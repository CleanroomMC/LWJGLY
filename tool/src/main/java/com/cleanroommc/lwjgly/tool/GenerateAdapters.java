package com.cleanroommc.lwjgly.tool;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class GenerateAdapters {

    private static final String ADAPTER_PACKAGE = "com.cleanroommc.lwjgly.adapter";

    public static void main(String[] args) throws IOException {
        if (args.length != 5) {
            throw new IllegalArgumentException("usage: GenerateAdapters <lwjgl2-cp> <lwjgl3-cp> <out-src-dir> <handwritten-src-dir> <adapter-map>");
        }
        ApiIndex lwjgl2 = ApiIndex.read(args[0]);
        ApiIndex lwjgl3 = ApiIndex.read(args[1]);
        Path outDir = Paths.get(args[2]);
        Path handWritten = Paths.get(args[3]);
        Path adapterMap = Paths.get(args[4]);

        Map<String, Delta.ClassDelta> deltas = ApiDiff.diff(lwjgl2, lwjgl3);
        PointerHandles handles = PointerHandles.in(lwjgl2);

        clean(outDir);
        Files.createDirectories(outDir);

        Map<String, String> emitted = new LinkedHashMap<>();
        List<String> handWrittenAdapters = findHandWritten(handWritten);
        for (String adapterInternal : handWrittenAdapters) {
            emitted.put(adapterInternal, targetName(adapterInternal));
        }

        List<String> skipped = new ArrayList<>();
        int delegating = 0;
        int throwing = 0;

        for (Delta.ClassDelta delta : deltas.values()) {
            if (delta.bucket() != Delta.Bucket.INJECT) {
                continue;
            }
            String shimInternal = generatedShimName(delta.internalName());

            List<Generated> methods = new ArrayList<>();
            for (Delta.MethodDelta method : delta.methods()) {
                MethodNode source = Resolution.findMethod(lwjgl2, delta.internalName(), method.name(), method.desc());
                if (source == null || method.name().startsWith("<")) {

                    skipped.add(delta.internalName() + "." + method.name() + method.desc() + " (constructor)");
                    continue;
                }
                methods.add(generate(delta.internalName(), method, source, handles));
            }
            if (methods.isEmpty()) {
                continue;
            }
            delegating += (int) methods.stream().filter(Generated::delegates).count();
            throwing += (int) methods.stream().filter(m -> !m.delegates()).count();
            write(outDir, shimInternal, delta.internalName(), methods);
            emitted.put(shimInternal, delta.internalName());
        }

        Files.createDirectories(adapterMap.getParent());
        try (BufferedWriter out = Files.newBufferedWriter(adapterMap)) {
            out.write("# adapter or generated shim internal name = LWJGL 3 class it merges into, in merge order\n");
            for (Map.Entry<String, String> entry : emitted.entrySet()) {
                out.write(entry.getKey() + "=" + entry.getValue() + "\n");
            }
        }

        System.out.println("merge inputs: " + emitted.size() + " classes ("
                + handWrittenAdapters.size() + " adapters, " + (emitted.size() - handWrittenAdapters.size()) + " generated shims), "
                + delegating + " delegating methods, " + throwing + " throwing placeholders");
        if (!skipped.isEmpty()) {
            System.out.println("not generated (" + skipped.size() + "):");
            skipped.forEach(s -> System.out.println("  " + s));
        }
    }

    static String generatedShimName(String target) {
        return ADAPTER_PACKAGE.replace('.', '/') + "/generated/" + target.substring("org/lwjgl/".length());
    }

    static String targetName(String adapterInternal) {
        return "org/lwjgl/" + adapterInternal.substring(ADAPTER_PACKAGE.length() + 1);
    }

    private static Generated generate(String owner, Delta.MethodDelta delta, MethodNode source,
                                      PointerHandles handles) {
        boolean isStatic = (source.access & Opcodes.ACC_STATIC) != 0;
        Type type = Type.getMethodType(delta.desc());
        Type[] params = type.getArgumentTypes();

        StringBuilder sb = new StringBuilder();
        sb.append("\n    public ").append(isStatic ? "static " : "")
                .append(type.getReturnType().getClassName()).append(' ').append(delta.name()).append('(');
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(params[i].getClassName()).append(" p").append(i);
        }
        sb.append(") {\n");

        boolean delegates = delta.fix().isAutomatic() && isStatic;
        if (!delegates) {
            sb.append("        throw new UnsupportedOperationException(\"")
                    .append(escape(owner.replace('/', '.') + "." + delta.name() + delta.desc()))
                    .append(" is not implemented by LWJGLY: see build/lwjgly/PROBLEMS.md\");\n");
        } else {
            Type target = Type.getMethodType(delta.target().desc());
            Type[] targetParams = target.getArgumentTypes();
            Type returnType = type.getReturnType();

            String wrap = returnType.equals(target.getReturnType()) ? null : PointerHandles.factory(returnType.getInternalName());
            sb.append("        ").append(returnType.getSort() == Type.VOID ? "" : "return ");
            if (wrap != null) {
                sb.append(wrap).append('(');
            }
            sb.append(owner.replace('/', '.')).append('.').append(delta.target().name()).append('(');
            List<Delta.Argument> arguments = delta.target().arguments();
            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                Delta.Argument argument = arguments.get(i);
                sb.append(render(argument, params[argument.source()], targetParams[i]));
            }
            sb.append(')');
            if (wrap != null) {
                sb.append(')');
            }
            sb.append(";\n");
        }
        sb.append("    }\n");
        return new Generated(sb.toString(), delegates);
    }

    private static String render(Delta.Argument argument, Type from, Type to) {
        String p = "p" + argument.source();
        return switch (argument.conversion()) {
            case PASS -> p;
            case VIEW -> p + ".slice().order(" + p + ".order())." + viewMethod(to) + "()";
            case HANDLE -> p + ".getPointer()";
            case ADDRESS -> "org.lwjgl.system.MemoryUtil.memAddress(" + p + ")";
            case BYTE_VIEW -> "org.lwjgl.system.MemoryUtil.memByteBuffer(" + p + ")";
            case RETYPE -> "org.lwjgl.system.MemoryUtil." + retypeMethod(to) +
                    "(org.lwjgl.system.MemoryUtil.memAddress(" + p + "), " + p + ".remaining())";
            case BYTE_COUNT -> p + ".remaining() * " + CallPlan.elementWidth(from);
            case GL_TYPE -> "org.lwjgl.opengl.GL11." + CallPlan.glType(from);
        };
    }

    private static String viewMethod(Type to) {
        return switch (to.getClassName()) {
            case "java.nio.FloatBuffer" -> "asFloatBuffer";
            case "java.nio.IntBuffer" -> "asIntBuffer";
            case "java.nio.ShortBuffer" -> "asShortBuffer";
            case "java.nio.DoubleBuffer" -> "asDoubleBuffer";
            case "java.nio.LongBuffer" -> "asLongBuffer";
            case "java.nio.CharBuffer" -> "asCharBuffer";
            default -> throw new IllegalStateException("No buffer view for " + to.getClassName());
        };
    }

    private static String retypeMethod(Type to) {
        return switch (to.getClassName()) {
            case "java.nio.IntBuffer" -> "memIntBuffer";
            case "java.nio.FloatBuffer" -> "memFloatBuffer";
            case "java.nio.ShortBuffer" -> "memShortBuffer";
            case "java.nio.LongBuffer" -> "memLongBuffer";
            case "java.nio.DoubleBuffer" -> "memDoubleBuffer";
            default -> throw new IllegalStateException("No retype for " + to.getClassName());
        };
    }

    private static void write(Path outDir, String adapterInternal, String target, List<Generated> methods) throws IOException {
        Path file = sourceFile(outDir, adapterInternal);
        Files.createDirectories(file.getParent());
        String packageName = adapterInternal.substring(0, adapterInternal.lastIndexOf('/')).replace('/', '.');
        String simpleName = adapterInternal.substring(adapterInternal.lastIndexOf('/') + 1);

        try (BufferedWriter out = Files.newBufferedWriter(file)) {
            out.write("package " + packageName + ";\n\n");
            out.write("public final class " + simpleName + " {\n");
            for (Generated method : methods) {
                out.write(method.source());
            }
            out.write("\n    private " + simpleName + "() {\n    }\n}\n");
        }
    }

    private static void clean(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    private static Path sourceFile(Path root, String internalName) {
        return root.resolve(internalName + ".java");
    }

    private static List<String> findHandWritten(Path root) throws IOException {
        List<String> adapters = new ArrayList<>();
        if (!Files.isDirectory(root)) {
            return adapters;
        }
        String prefix = ADAPTER_PACKAGE.replace('.', '/') + "/";
        try (var walk = Files.walk(root)) {
            for (Path file : walk.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
                String relative = root.relativize(file).toString().replace('\\', '/');
                String internalName = relative.substring(0, relative.length() - ".java".length());
                if (internalName.startsWith(prefix) && !internalName.startsWith(prefix + "generated/")) {
                    adapters.add(internalName);
                }
            }
        }
        return adapters;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private GenerateAdapters() { }

    private record Generated(String source, boolean delegates) { }

}
