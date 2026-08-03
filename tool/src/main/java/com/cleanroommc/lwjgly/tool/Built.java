package com.cleanroommc.lwjgly.tool;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public record Built(Map<String, ClassNode> shims, Map<String, List<ClassNode>> adaptersByTarget) {

    public static Built read(String shimDirs, String adapterDir, Path adapterMap) throws IOException {
        Map<String, ClassNode> shims = readClasses(ApiIndex.split(shimDirs));
        Map<String, ClassNode> adapters = readClasses(ApiIndex.split(adapterDir));

        Map<String, List<ClassNode>> byTarget = new LinkedHashMap<>();
        if (Files.isRegularFile(adapterMap)) {
            for (String line : Files.readAllLines(adapterMap)) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                int equals = line.indexOf('=');
                ClassNode adapter = adapters.get(line.substring(0, equals));
                if (adapter != null) {
                    byTarget.computeIfAbsent(line.substring(equals + 1), t -> new ArrayList<>()).add(adapter);
                }
            }
        }
        return new Built(shims, byTarget);
    }

    private static boolean declaresField(ClassNode node, String name, String desc) {
        if (node == null) {
            return false;
        }
        for (FieldNode field : node.fields) {
            if (field.name.equals(name) && field.desc.equals(desc)) {
                return true;
            }
        }
        return false;
    }

    private static MethodNode declared(ClassNode node, String name, String desc) {
        if (node == null) {
            return null;
        }
        for (MethodNode candidate : node.methods) {
            if (candidate.name.equals(name) && candidate.desc.equals(desc)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean throwsOnly(MethodNode method) {
        boolean throwsSomething = false;
        for (AbstractInsnNode insn : method.instructions) {
            int opcode = insn.getOpcode();
            if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
                return false;
            }
            if (opcode == Opcodes.ATHROW) {
                throwsSomething = true;
            }
        }
        return throwsSomething;
    }

    private static Map<String, ClassNode> readClasses(List<Path> roots) throws IOException {
        Map<String, ClassNode> classes = new LinkedHashMap<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(root)) {
                for (Path file : walk.filter(p -> p.toString().endsWith(".class")).toList()) {
                    ClassNode node = new ClassNode();
                    try (InputStream in = Files.newInputStream(file)) {
                        new ClassReader(in).accept(node, 0);
                    }
                    classes.putIfAbsent(node.name, node);
                }
            }
        }
        return classes;
    }

    public Outcome classify(String reference, ApiIndex lwjgl3) {
        int colon = reference.indexOf(':');
        int paren = reference.indexOf('(');
        int split = reference.lastIndexOf('.', colon >= 0 ? colon : paren);
        String owner = reference.substring(0, split);
        String name = reference.substring(split + 1, colon >= 0 ? colon : paren);
        String desc = reference.substring(colon >= 0 ? colon + 1 : paren);

        if (colon >= 0) {
            if (Resolution.findField(lwjgl3, owner, name, desc) != null) {
                return Outcome.LWJGL3;
            }
            return hasField(owner, name, desc) ? Outcome.SHIM : Outcome.GAP;
        }
        return classifyMethod(owner, name, desc, lwjgl3);
    }

    public Outcome classifyMethod(String owner, String name, String desc, ApiIndex lwjgl3) {
        if (Resolution.hasMethod(lwjgl3, owner, name, desc)) {
            return Outcome.LWJGL3;
        }
        for (ClassNode candidate : adaptersByTarget.getOrDefault(owner, List.of())) {
            MethodNode adapter = declared(candidate, name, desc);
            if (adapter != null) {
                return throwsOnly(adapter) ? Outcome.THROWS : Outcome.MERGED;
            }
        }
        MethodNode shim = shimMethod(owner, name, desc);
        if (shim != null) {
            return throwsOnly(shim) ? Outcome.THROWS : Outcome.SHIM;
        }
        return inheritedFromOutside(owner, name, desc) ? Outcome.SHIM : Outcome.GAP;
    }

    private boolean inheritedFromOutside(String owner, String name, String desc) {
        ClassNode node = shims.get(owner);
        while (node != null) {
            String superName = node.superName;
            if (superName == null) {
                return false;
            }
            if (!shims.containsKey(superName)) {
                try {
                    Class<?> type = Class.forName(superName.replace('/', '.'));
                    for (Method method : type.getMethods()) {
                        if (method.getName().equals(name) && Type.getMethodDescriptor(method).equals(desc)) {
                            return true;
                        }
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError ignored) { }
                return false;
            }
            node = shims.get(superName);
        }
        return false;
    }

    private boolean hasField(String owner, String name, String desc) {
        for (ClassNode node = shims.get(owner); node != null; node = shims.get(node.superName)) {
            if (declaresField(node, name, desc)) {
                return true;
            }
            for (String itf : node.interfaces) {
                if (declaresField(shims.get(itf), name, desc)) {
                    return true;
                }
            }
        }
        return false;
    }

    private MethodNode shimMethod(String owner, String name, String desc) {
        ClassNode node = shims.get(owner);
        if (node == null) {
            return null;
        }
        MethodNode found = declared(node, name, desc);
        if (found != null) {
            return found;
        }
        for (String itf : node.interfaces) {
            found = shimMethod(itf, name, desc);
            if (found != null) {
                return found;
            }
        }
        return node.superName == null ? null : shimMethod(node.superName, name, desc);
    }
}
