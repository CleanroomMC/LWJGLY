package com.cleanroommc.lwjgly.tool;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class GenerateShims {

    private static final String CONTEXT_CAPABILITIES = "org/lwjgl/opengl/ContextCapabilities";
    private static final String GL_CAPABILITIES = "org/lwjgl/opengl/GLCapabilities";

    public static void main(String[] args) throws IOException {
        if (args.length != 6) {
            throw new IllegalArgumentException("usage: GenerateShims <lwjgl2-cp> <lwjgl3-cp> "
                    + "<out-src-dir> <out-class-dir> <handwritten-src-dir> <native-calls.md>");
        }
        ApiIndex lwjgl2 = ApiIndex.read(args[0]);
        ApiIndex lwjgl3 = ApiIndex.read(args[1]);
        Path srcDir = Paths.get(args[2]);
        Path classDir = Paths.get(args[3]);
        Path handWritten = Paths.get(args[4]);
        Path nativeCalls = Paths.get(args[5]);

        Map<String, Delta.ClassDelta> deltas = ApiDiff.diff(lwjgl2, lwjgl3);
        clean(srcDir);
        clean(classDir);

        Set<String> handWrittenNames = handWritten(handWritten);
        Set<String> vendored = new TreeSet<>();
        List<String> placeheld = new ArrayList<>();
        List<String> skippedInternal = new ArrayList<>();
        List<String> skippedMembers = new ArrayList<>();

        Set<String> shipped = new TreeSet<>(handWrittenNames);
        for (Delta.ClassDelta delta : deltas.values()) {
            if (delta.bucket() != Delta.Bucket.SHIM) {
                continue;
            }
            if (delta.shimSource() == Delta.ShimSource.VENDOR || ApiIndex.isNameable(lwjgl2.get(delta.internalName()))) {
                shipped.add(delta.internalName());
            }
        }
        requiredTypes(shipped, lwjgl2, lwjgl3, deltas);

        int constantInterfaces = 0;
        for (String name : handWrittenNames) {
            ClassNode node = lwjgl2.get(name);
            if (node != null && writeConstants(srcDir, node)) {
                constantInterfaces++;
            }
        }

        for (Delta.ClassDelta delta : deltas.values()) {
            if (delta.bucket() != Delta.Bucket.SHIM || handWrittenNames.contains(delta.internalName())) {
                continue;
            }
            if (delta.shimSource() == Delta.ShimSource.VENDOR) {
                vendored.add(delta.internalName());
            } else if (!shipped.contains(delta.internalName())) {
                skippedInternal.add(delta.internalName());
            } else {
                placeheld.add(delta.internalName());
            }
        }

        Map<String, List<Ctor>> generatedConstructors = new TreeMap<>();
        for (String name : placeheld) {
            generatedConstructors.put(name, constructorShapes(lwjgl2.get(name), shipped, lwjgl3));
        }
        for (String name : placeheld) {
            if (CONTEXT_CAPABILITIES.equals(name)) {
                writeContextCapabilities(srcDir, lwjgl2.get(name), lwjgl2, lwjgl3, handWrittenNames);
            } else {
                writePlaceholder(srcDir, lwjgl2, lwjgl3, lwjgl2.get(name), shipped, skippedMembers, generatedConstructors);
            }
        }

        copyVerbatim(args[0], vendored, classDir);
        writeNativeCalls(nativeCalls, lwjgl2);

        System.out.println("Shims: " + vendored.size() + " vendored verbatim, "
                + placeheld.size() + " placeholders, " + handWrittenNames.size() + " handwritten ("
                + constantInterfaces + " with generated constants), "
                + skippedInternal.size() + " unnameable classes skipped");
        if (!skippedMembers.isEmpty()) {
            System.out.println("Members omitted, their types were never on the 1.12.2 classpath ("
                    + skippedMembers.size() + "):");
            skippedMembers.stream().limit(20).forEach(m -> System.out.println("  " + m));
        }
    }

    private static void writeNativeCalls(Path file, ApiIndex lwjgl2) throws IOException {
        Map<String, List<MethodNode>> byClass = new TreeMap<>();
        int total = 0;
        int callable = 0;
        for (ClassNode node : lwjgl2.classes().values()) {
            if (!node.name.startsWith("org/lwjgl/")) {
                continue;
            }
            List<MethodNode> natives = node.methods.stream()
                    .filter(m -> (m.access & Opcodes.ACC_NATIVE) != 0)
                    .sorted(Comparator.comparing((MethodNode m) -> m.name).thenComparing(m -> m.desc))
                    .toList();
            if (natives.isEmpty()) {
                continue;
            }
            byClass.put(node.name, natives);
            total += natives.size();
            callable += (int) natives.stream().filter(m -> ApiIndex.isCallable(m.access)).count();
        }

        Files.createDirectories(file.getParent());
        try (BufferedWriter out = Files.newBufferedWriter(file)) {
            out.write("""
                    # Native Calls
                    
                    Generated by `./gradlew generateShims`. Do not edit by hand.
                    
                    Every `native` method LWJGL 2 declared.
                    LWJGLY ships none, and LWJGL 3's natives export a different set of symbols.
                    A shim declaring one would fail with `UnsatisfiedLinkError`.
                    
                    Marked `callable` where LWJGL 2 declared it public or protected.
                    """);
            out.write("# " + total + " native method(s) across " + byClass.size() + " class(es); " + callable + " were callable.\n");
            for (Map.Entry<String, List<MethodNode>> entry : byClass.entrySet()) {
                out.write("\n" + entry.getKey().replace('/', '.') + "\n");
                for (MethodNode method : entry.getValue()) {
                    out.write("    " + method.name + method.desc + (ApiIndex.isCallable(method.access) ? "    callable" : "") + "\n");
                }
            }
        }
        System.out.println("Native calls: " + total + " across " + byClass.size() + " class(es), " + callable + " callable; wrote " + file);
    }

    private static void copyVerbatim(String classpath, Set<String> wanted, Path outDir) throws IOException {
        if (wanted.isEmpty()) {
            return;
        }
        int copied = 0;
        for (Path jar : ApiIndex.split(classpath)) {
            if (!jar.getFileName().toString().endsWith(".jar")) {
                continue;
            }
            try (JarFile jarFile = new JarFile(jar.toFile())) {
                for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements(); ) {
                    JarEntry entry = e.nextElement();
                    if (!entry.getName().endsWith(".class")) {
                        continue;
                    }
                    String internalName = entry.getName().substring(0, entry.getName().length() - ".class".length());
                    if (!wanted.contains(internalName)) {
                        continue;
                    }
                    Path target = outDir.resolve(entry.getName());
                    Files.createDirectories(target.getParent());
                    try (InputStream in = jarFile.getInputStream(entry)) {
                        Files.copy(in, target);
                    }
                    copied++;
                }
            }
        }
        if (copied != wanted.size()) {
            throw new IllegalStateException("Expected to vendor " + wanted.size() + " classes, copied " + copied);
        }
    }

    private static void requiredTypes(Set<String> shipped, ApiIndex lwjgl2, ApiIndex lwjgl3, Map<String, Delta.ClassDelta> deltas) {
        Deque<String> pending = new ArrayDeque<>(shipped);
        while (!pending.isEmpty()) {
            ClassNode node = lwjgl2.get(pending.poll());
            if (node == null) {
                continue;
            }
            Set<String> named = new TreeSet<>();
            addIfObject(named, node.superName);
            node.interfaces.forEach(i -> addIfObject(named, i));
            for (MethodNode method : node.methods) {
                if (ApiIndex.isCallable(method.access) && (method.access & Opcodes.ACC_NATIVE) == 0) {
                    Type type = Type.getMethodType(method.desc);
                    for (Type argument : type.getArgumentTypes()) {
                        addIfObject(named, argument);
                    }
                    addIfObject(named, type.getReturnType());
                }
            }
            for (FieldNode field : node.fields) {
                if (ApiIndex.isCallable(field.access)) {
                    addIfObject(named, Type.getType(field.desc));
                }
            }
            for (String required : named) {
                Delta.ClassDelta delta = deltas.get(required);
                if (shipped.contains(required) || lwjgl3.has(required) || required.indexOf('$') >= 0 ||
                        delta == null || delta.bucket() != Delta.Bucket.SHIM) {
                    continue;
                }
                shipped.add(required);
                pending.add(required);
            }
        }
    }

    private static void addIfObject(Set<String> types, Type type) {
        while (type.getSort() == Type.ARRAY) {
            type = type.getElementType();
        }
        if (type.getSort() == Type.OBJECT) {
            addIfObject(types, type.getInternalName());
        }
    }

    private static void addIfObject(Set<String> types, String internalName) {
        if (internalName != null && internalName.startsWith("org/lwjgl/")) {
            types.add(internalName);
        }
    }

    private static boolean representable(String desc, Set<String> shipped, ApiIndex lwjgl3) {
        for (Type type : Type.getMethodType(desc).getArgumentTypes()) {
            if (!representable(type, shipped, lwjgl3)) {
                return false;
            }
        }
        return representable(Type.getMethodType(desc).getReturnType(), shipped, lwjgl3);
    }

    private static boolean representable(Type type, Set<String> shipped, ApiIndex lwjgl3) {
        while (type.getSort() == Type.ARRAY) {
            type = type.getElementType();
        }
        if (type.getSort() != Type.OBJECT) {
            return true;
        }
        String name = type.getInternalName();

        return !name.startsWith("org/lwjgl/") || shipped.contains(name) || lwjgl3.has(name);
    }

    private static boolean writeConstants(Path srcDir, ClassNode node) throws IOException {
        List<FieldNode> constants = new ArrayList<>();
        for (FieldNode field : node.fields) {
            if (ApiIndex.isCallable(field.access) && ApiIndex.isInlinedConstant(field)) {
                constants.add(field);
            }
        }
        if (constants.isEmpty()) {
            return false;
        }
        String name = simpleName(node.name) + "Constants";
        Path file = srcDir.resolve(node.name + "Constants.java");
        Files.createDirectories(file.getParent());
        try (BufferedWriter out = Files.newBufferedWriter(file)) {
            out.write("package " + packageOf(node.name) + ";\n\n");
            out.write("public interface " + name + " {\n");
            for (FieldNode constant : constants) {
                out.write("\n    " + Type.getType(constant.desc).getClassName() + " " + constant.name
                        + " = " + literal(constant.value, constant.desc) + ";\n");
            }
            out.write("}\n");
        }
        return true;
    }

    private static void writeContextCapabilities(Path srcDir, ClassNode node, ApiIndex lwjgl2, ApiIndex lwjgl3,
                                                 Set<String> handWrittenNames) throws IOException {
        ClassNode glCapabilities = lwjgl3.get(GL_CAPABILITIES);
        if (glCapabilities == null) {
            throw new IllegalStateException("LWJGL 3 has no " + GL_CAPABILITIES + " to copy capabilities from");
        }
        Set<String> available = new TreeSet<>();
        for (FieldNode field : glCapabilities.fields) {
            if (field.desc.equals("Z") && ApiIndex.isCallable(field.access)) {
                available.add(field.name);
            }
        }

        List<String> fields = new ArrayList<>();
        List<String> unmatched = new ArrayList<>();
        for (FieldNode field : node.fields) {
            if (field.desc.equals("Z") && ApiIndex.isCallable(field.access)) {
                fields.add(field.name);
                if (!available.contains(field.name)) {
                    unmatched.add(field.name);
                }
            }
        }

        CapabilityFlags.Split split = CapabilityFlags.of(lwjgl2, lwjgl3, handWrittenNames::contains);
        Set<String> fromDriver = new TreeSet<>(split.fromDriver());

        Path file = srcDir.resolve(node.name + ".java");
        Files.createDirectories(file.getParent());
        try (BufferedWriter out = Files.newBufferedWriter(file)) {
            out.write("package org.lwjgl.opengl;\n\n");
            out.write("public class ContextCapabilities {\n");
            for (String field : fields) {
                out.write("\n    public final boolean " + field + ";\n");
            }
            out.write("\n    ContextCapabilities(GLCapabilities capabilities) {\n");
            if (!fromDriver.isEmpty()) {
                out.write("        java.util.Set<String> driver = DriverExtensions.of(capabilities);\n");
            }
            for (String field : fields) {
                String source;
                if (available.contains(field)) {
                    source = "capabilities." + field;
                } else if (fromDriver.contains(field)) {
                    source = "driver.contains(\"" + field + "\")";
                } else {
                    source = "false";
                }
                out.write("        this." + field + " = " + source + ";\n");
            }
            out.write("    }\n}\n");
        }
        if (!unmatched.isEmpty()) {
            System.out.println("ContextCapabilities: " + unmatched.size() + " of " + fields.size()
                    + " flags have no LWJGL 3 counterpart, " + fromDriver.size()
                    + " read from the driver, " + (unmatched.size() - fromDriver.size()) + " stay false");
        }
    }

    private static List<Ctor> constructorShapes(ClassNode node, Set<String> shipped, ApiIndex lwjgl3) {
        List<Ctor> shapes = new ArrayList<>();
        for (MethodNode method : node.methods) {
            if (method.name.equals("<init>") && ApiIndex.isCallable(method.access) && representable(method.desc, shipped, lwjgl3)) {
                shapes.add(new Ctor(Type.getMethodType(method.desc).getArgumentTypes(), representableExceptions(method.exceptions, shipped, lwjgl3)));
            }
        }
        if (shapes.isEmpty()) {
            shapes.add(new Ctor(new Type[0], List.of()));
        }
        return shapes;
    }

    private static List<String> representableExceptions(List<String> exceptions, Set<String> shipped, ApiIndex lwjgl3) {
        List<String> thrown = new ArrayList<>();
        if (exceptions != null) {
            for (String exception : exceptions) {

                if (representable(Type.getObjectType(exception), shipped, lwjgl3)) {
                    thrown.add(exception.replace('/', '.'));
                }
            }
        }
        return thrown;
    }

    private static void writePlaceholder(Path srcDir, ApiIndex lwjgl2, ApiIndex lwjgl3, ClassNode node,
                                         Set<String> shipped, List<String> skippedMembers,
                                         Map<String, List<Ctor>> generatedConstructors) throws IOException {
        if ((node.access & Opcodes.ACC_ANNOTATION) != 0 || (node.access & Opcodes.ACC_ENUM) != 0) {
            throw new IllegalStateException("Cannot generate a placeholder for enum/annotation " + node.name);
        }
        boolean isInterface = (node.access & Opcodes.ACC_INTERFACE) != 0;
        String packageName = packageOf(node.name);
        String simpleName = simpleName(node.name);
        Path file = srcDir.resolve(node.name + ".java");
        Files.createDirectories(file.getParent());

        try (BufferedWriter out = Files.newBufferedWriter(file)) {
            out.write("package " + packageName + ";\n\n");

            out.write("public ");
            if (!isInterface && (node.access & Opcodes.ACC_ABSTRACT) != 0) {
                out.write("abstract ");
            }
            out.write(isInterface ? "interface " : "class ");
            out.write(simpleName);

            String superName = node.superName;
            if (!isInterface && superName != null && !"java/lang/Object".equals(superName) &&
                    representable(Type.getObjectType(superName), shipped, lwjgl3)) {
                out.write(" extends " + superName.replace('/', '.'));
            }
            List<String> interfaces = node.interfaces.stream()
                    .filter(i -> representable(Type.getObjectType(i), shipped, lwjgl3))
                    .map(i -> i.replace('/', '.'))
                    .toList();
            if (!interfaces.isEmpty()) {
                out.write((isInterface ? " extends " : " implements ") + String.join(", ", interfaces));
            }
            out.write(" {\n");

            for (FieldNode field : node.fields) {
                if (!ApiIndex.isCallable(field.access)) {
                    continue;
                }
                if (!representable(Type.getType(field.desc), shipped, lwjgl3)) {
                    skippedMembers.add(node.name + "." + field.name + " : " + field.desc);
                    continue;
                }
                writeField(out, field, isInterface);
            }
            boolean wroteConstructor = false;
            for (MethodNode method : node.methods) {
                if (!ApiIndex.isCallable(method.access)) {
                    continue;
                }
                if ((method.access & Opcodes.ACC_NATIVE) != 0) {
                    continue;
                }
                if (!representable(method.desc, shipped, lwjgl3)) {
                    skippedMembers.add(node.name + "." + method.name + method.desc);
                    continue;
                }
                writeMethod(out, lwjgl2, lwjgl3, node, method, isInterface, generatedConstructors, shipped);
                wroteConstructor |= method.name.equals("<init>");
            }

            if (!isInterface && !wroteConstructor) {
                SuperCall superCall = superCall(lwjgl2, lwjgl3, node, generatedConstructors, shipped);
                if (superCall != null) {
                    String thrown = superCall.exceptions().isEmpty()
                            ? "" : " throws " + String.join(", ", superCall.exceptions());
                    out.write("\n    protected " + simpleName(node.name) + "()" + thrown + " {\n        "
                            + superCall.code() + ";\n    }\n");
                }
            }
            out.write("}\n");
        }
    }

    private static void writeField(BufferedWriter out, FieldNode field, boolean isInterface) throws IOException {
        String type = Type.getType(field.desc).getClassName();
        out.write("\n    ");
        if (!isInterface) {
            out.write(((field.access & Opcodes.ACC_PROTECTED) != 0 ? "protected " : "public "));
            if ((field.access & Opcodes.ACC_STATIC) != 0) {
                out.write("static ");
            }
            if ((field.access & Opcodes.ACC_FINAL) != 0 && field.value != null) {
                out.write("final ");
            }
        }
        out.write(type + " " + field.name);
        if (field.value != null) {
            out.write(" = " + literal(field.value, field.desc));
        } else if (isInterface) {
            out.write(" = " + defaultValue(Type.getType(field.desc)));
        }
        out.write(";\n");
    }

    private static void writeMethod(BufferedWriter out, ApiIndex lwjgl2, ApiIndex lwjgl3, ClassNode owner,
                                    MethodNode method, boolean isInterface, Map<String, List<Ctor>> generatedConstructors,
                                    Set<String> shipped) throws IOException {
        Type type = Type.getMethodType(method.desc);
        Type[] params = type.getArgumentTypes();
        boolean isConstructor = method.name.equals("<init>");
        boolean isAbstract = (method.access & Opcodes.ACC_ABSTRACT) != 0;

        out.write("\n    ");
        out.write((method.access & Opcodes.ACC_PROTECTED) != 0 ? "protected " : "public ");
        if ((method.access & Opcodes.ACC_STATIC) != 0) {
            out.write("static ");
        }
        if (isAbstract && !isInterface) {
            out.write("abstract ");
        }
        if (isConstructor) {
            out.write(simpleName(owner.name));
        } else {
            out.write(type.getReturnType().getClassName() + " " + method.name);
        }
        out.write("(");
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                out.write(", ");
            }
            out.write(params[i].getClassName() + " p" + i);
        }
        out.write(")");

        SuperCall superCall = isConstructor ? superCall(lwjgl2, lwjgl3, owner, generatedConstructors, shipped) : null;

        List<String> thrown = new ArrayList<>(representableExceptions(method.exceptions, shipped, lwjgl3));
        if (superCall != null) {
            superCall.exceptions().stream().filter(e -> !thrown.contains(e)).forEach(thrown::add);
        }
        if (!thrown.isEmpty()) {
            out.write(" throws " + String.join(", ", thrown));
        }

        if (isAbstract || (isInterface && (method.access & Opcodes.ACC_STATIC) == 0)) {
            out.write(";\n");
            return;
        }
        out.write(" {\n");
        if (superCall != null) {
            out.write("        " + superCall.code() + ";\n");
        }
        out.write("        throw new UnsupportedOperationException(\""
                + owner.name.replace('/', '.') + "." + (isConstructor ? "<init>" : method.name)
                + " is not implemented by LWJGLY. See build/lwjgly/PROBLEMS.md\");\n    }\n");
    }

    private static SuperCall superCall(ApiIndex lwjgl2, ApiIndex lwjgl3, ClassNode owner,
                                       Map<String, List<Ctor>> generatedConstructors, Set<String> shipped) {
        if (owner.superName == null || "java/lang/Object".equals(owner.superName) ||
                !representable(Type.getObjectType(owner.superName), shipped, lwjgl3)) {
            return null;
        }
        List<Ctor> constructors = generatedConstructors.get(owner.superName);
        if (constructors == null) {
            ClassNode superClass = lwjgl2.has(owner.superName) ? lwjgl2.get(owner.superName) : lwjgl3.get(owner.superName);
            constructors = superClass != null ? fromBytecode(superClass) : fromReflection(owner.superName);
        }

        Ctor chosen = null;
        for (Ctor candidate : constructors) {
            if (candidate.params().length == 0 && candidate.exceptions().isEmpty()) {
                return null;
            }
            if (chosen == null || candidate.params().length < chosen.params().length) {
                chosen = candidate;
            }
        }
        if (chosen == null) {
            return null;
        }
        List<String> arguments = new ArrayList<>();
        for (Type param : chosen.params()) {
            arguments.add("(" + param.getClassName() + ") " + defaultValue(param));
        }
        return new SuperCall("super(" + String.join(", ", arguments) + ")", chosen.exceptions());
    }

    private static List<Ctor> fromBytecode(ClassNode superClass) {
        List<Ctor> constructors = new ArrayList<>();
        for (MethodNode candidate : superClass.methods) {
            if (candidate.name.equals("<init>") && (candidate.access & Opcodes.ACC_PRIVATE) == 0) {
                List<String> thrown = new ArrayList<>();
                if (candidate.exceptions != null) {
                    candidate.exceptions.forEach(e -> thrown.add(e.replace('/', '.')));
                }
                constructors.add(new Ctor(Type.getMethodType(candidate.desc).getArgumentTypes(), thrown));
            }
        }
        return constructors;
    }

    private static List<Ctor> fromReflection(String internalName) {
        List<Ctor> constructors = new ArrayList<>();
        try {
            Class<?> type = Class.forName(internalName.replace('/', '.'));
            for (Constructor<?> candidate : type.getDeclaredConstructors()) {
                if (Modifier.isPrivate(candidate.getModifiers())) {
                    continue;
                }
                List<String> thrown = new ArrayList<>();
                for (Class<?> exception : candidate.getExceptionTypes()) {
                    thrown.add(exception.getName());
                }
                constructors.add(new Ctor(
                        Type.getArgumentTypes(Type.getConstructorDescriptor(candidate)), thrown));
            }
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) { }
        return constructors;
    }

    private static String literal(Object value, String desc) {
        if (value instanceof String s) {
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        return switch (desc) {
            case "J" -> value + "L";
            case "F" -> value + "f";
            case "Z" -> Boolean.toString(((Number) value).intValue() != 0);
            case "C" -> "(char) " + value;
            case "B" -> "(byte) " + value;
            case "S" -> "(short) " + value;
            default -> value.toString();
        };
    }

    private static String defaultValue(Type type) {
        return switch (type.getSort()) {
            case Type.OBJECT, Type.ARRAY -> "null";
            case Type.BOOLEAN -> "false";
            case Type.FLOAT -> "0F";
            case Type.LONG -> "0L";
            case Type.DOUBLE -> "0D";
            default -> "0";
        };
    }

    private static Set<String> handWritten(Path root) throws IOException {
        Set<String> names = new TreeSet<>();
        if (!Files.isDirectory(root)) {
            return names;
        }
        try (var walk = Files.walk(root)) {
            for (Path path : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                String relative = root.relativize(path).toString().replace('\\', '/');
                if (relative.startsWith("org/lwjgl/")) {
                    names.add(relative.substring(0, relative.length() - ".java".length()));
                }
            }
        }
        return names;
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

    private static String packageOf(String internalName) {
        return internalName.substring(0, internalName.lastIndexOf('/')).replace('/', '.');
    }

    private static String simpleName(String internalName) {
        return internalName.substring(internalName.lastIndexOf('/') + 1);
    }

    private GenerateShims() { }

    private record Ctor(Type[] params, List<String> exceptions) { }

    private record SuperCall(String code, List<String> exceptions) { }

}
