package com.cleanroommc.lwjgly.tool;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class VerifyCoverage {

    public static void main(String[] args) throws IOException {
        if (args.length != 7) {
            throw new IllegalArgumentException("usage: VerifyCoverage <lwjgl2-cp> <lwjgl3-cp> "
                    + "<shim-classes-dirs> <adapter-classes-dir> <adapter-map> <known-gaps> <report>");
        }
        ApiIndex lwjgl2 = ApiIndex.read(args[0]);
        ApiIndex lwjgl3 = ApiIndex.read(args[1]);
        Map<String, ClassNode> shipped = readDirectories(args[2]);
        Map<String, ClassNode> adapters = readDirectories(args[3]);
        Map<String, String> adapterMap = readAdapterMap(Paths.get(args[4]));
        Path knownGapsFile = Paths.get(args[5]);
        Set<String> knownGaps = readKnownGaps(knownGapsFile);

        Map<String, List<ClassNode>> adaptersByTarget = new LinkedHashMap<>();
        adapterMap.forEach((adapter, target) -> {
            ClassNode node = adapters.get(adapter);
            if (node != null) {
                adaptersByTarget.computeIfAbsent(target, t -> new ArrayList<>()).add(node);
            }
        });

        List<String> gaps = new ArrayList<>();
        for (ClassNode lwjgl2Class : lwjgl2.classes().values()) {
            if (!lwjgl2Class.name.startsWith("org/lwjgl/") || !ApiIndex.isNameable(lwjgl2Class)) {
                continue;
            }
            check(lwjgl2Class, lwjgl3, shipped, adaptersByTarget, gaps);
        }

        Path report = Paths.get(args[6]);
        Files.createDirectories(report.getParent());
        Files.write(report, gaps);

        List<String> unexpected = gaps.stream().filter(gap -> !knownGaps.contains(gap)).toList();
        List<String> stale = knownGaps.stream().filter(gap -> !gaps.contains(gap)).toList();

        System.out.println("coverage: " + gaps.size() + " gap(s), " + knownGaps.size() + " of them known");
        if (!stale.isEmpty()) {
            System.out.println("known gaps that are now covered, remove them from " + knownGapsFile.getFileName() + ":");
            stale.forEach(gap -> System.out.println("  " + gap));
        }
        if (!unexpected.isEmpty()) {
            System.err.println("Coverage gate FAILED: " + unexpected.size() + " member(s) a mod could call would not link.");
            unexpected.stream().limit(50).forEach(gap -> System.err.println("  " + gap));
            if (unexpected.size() > 50) {
                System.err.println("  ... and " + (unexpected.size() - 50) + " more");
            }
            System.err.println("Either provide them, or add them to " + knownGapsFile + " with a comment saying why they cannot be.");
            throw new IllegalStateException(unexpected.size() + " uncovered member(s)");
        }
    }

    private static void check(ClassNode lwjgl2Class, ApiIndex lwjgl3, Map<String, ClassNode> shipped,
                              Map<String, List<ClassNode>> adaptersByTarget, List<String> gaps) {
        String name = lwjgl2Class.name;
        boolean inLwjgl3 = lwjgl3.has(name);
        ClassNode ourClass = shipped.get(name);

        if (!inLwjgl3 && ourClass == null) {
            gaps.add("class " + name);
            return;
        }

        List<ClassNode> classAdapters = adaptersByTarget.getOrDefault(name, List.of());
        for (MethodNode method : lwjgl2Class.methods) {
            if (!ApiIndex.isCallable(method.access) || method.name.equals("<clinit>")) {
                continue;
            }
            if (covered(name, method.name, method.desc, lwjgl3, ourClass, classAdapters, shipped)) {
                continue;
            }
            gaps.add(name + "." + method.name + method.desc);
        }
        for (FieldNode field : lwjgl2Class.fields) {
            if (!ApiIndex.isCallable(field.access) || ApiIndex.isInlinedConstant(field)) {

                continue;
            }
            if (fieldCovered(name, field, lwjgl3, ourClass, shipped)) {
                continue;
            }
            gaps.add(name + "." + field.name + " : " + field.desc);
        }
    }

    private static boolean covered(String owner, String name, String desc, ApiIndex lwjgl3,
                                   ClassNode ourClass, List<ClassNode> adapters, Map<String, ClassNode> shipped) {
        if (lwjgl3.has(owner) && Resolution.hasMethod(lwjgl3, owner, name, desc)) {
            return true;
        }
        for (ClassNode adapter : adapters) {
            if (declares(adapter, name, desc)) {
                return true;
            }
        }
        if (ourClass != null && (declares(ourClass, name, desc) || inherited(ourClass, name, desc, shipped, lwjgl3))) {
            return true;
        }
        return false;
    }

    private static boolean fieldCovered(String owner, FieldNode field, ApiIndex lwjgl3,
                                        ClassNode ourClass, Map<String, ClassNode> shipped) {
        if (lwjgl3.has(owner) && Resolution.findField(lwjgl3, owner, field.name, field.desc) != null) {
            return true;
        }
        if (ourClass == null) {
            return false;
        }
        for (ClassNode node = ourClass; node != null; node = shipped.get(node.superName)) {
            for (FieldNode candidate : node.fields) {
                if (candidate.name.equals(field.name) && candidate.desc.equals(field.desc)) {
                    return true;
                }
            }

            for (String itf : node.interfaces) {
                ClassNode parent = shipped.get(itf);
                if (parent != null) {
                    for (FieldNode candidate : parent.fields) {
                        if (candidate.name.equals(field.name) && candidate.desc.equals(field.desc)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean declares(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) {
                return true;
            }
        }
        return false;
    }

    private static boolean inherited(ClassNode node, String name, String desc,
                                     Map<String, ClassNode> shipped, ApiIndex lwjgl3) {
        String superName = node.superName;
        while (superName != null) {
            ClassNode parent = shipped.get(superName);
            if (parent == null) {
                return lwjgl3.has(superName) && Resolution.hasMethod(lwjgl3, superName, name, desc);
            }
            if (declares(parent, name, desc)) {
                return true;
            }
            superName = parent.superName;
        }
        return false;
    }

    private static Map<String, ClassNode> readDirectories(String pathList) throws IOException {
        Map<String, ClassNode> classes = new LinkedHashMap<>();
        for (Path root : ApiIndex.split(pathList)) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (var walk = Files.walk(root)) {
                for (Path file : walk.filter(p -> p.toString().endsWith(".class")).toList()) {
                    ClassNode node = new ClassNode();
                    try (InputStream in = Files.newInputStream(file)) {
                        new ClassReader(in).accept(node, ClassReader.SKIP_CODE);
                    }
                    classes.putIfAbsent(node.name, node);
                }
            }
        }
        return classes;
    }

    private static Map<String, String> readAdapterMap(Path file) throws IOException {
        Map<String, String> map = new LinkedHashMap<>();
        if (!Files.isRegularFile(file)) {
            return map;
        }
        for (String line : Files.readAllLines(file)) {
            if (!line.isBlank() && !line.startsWith("#")) {
                int equals = line.indexOf('=');
                map.put(line.substring(0, equals), line.substring(equals + 1));
            }
        }
        return map;
    }

    private static Set<String> readKnownGaps(Path file) throws IOException {
        Set<String> gaps = new TreeSet<>();
        if (!Files.isRegularFile(file)) {
            return gaps;
        }
        for (String line : Files.readAllLines(file)) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                gaps.add(trimmed);
            }
        }
        return gaps;
    }
}
