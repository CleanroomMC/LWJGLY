package com.cleanroommc.lwjgly.tool;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class ScanUsage {

    private static final String MIXIN_ANNOTATION = "Lorg/spongepowered/asm/mixin/Mixin;";

    public static void main(String[] args) throws IOException {
        if (args.length != 6) {
            throw new IllegalArgumentException("usage: ScanUsage <lwjgl3-cp> <shim-classes-dirs> "
                    + "<adapter-classes-dir> <adapter-map> <scan-paths> <report>");
        }
        ApiIndex lwjgl3 = ApiIndex.read(args[0]);
        Built built = Built.read(args[1], args[2], Paths.get(args[3]));
        List<Path> scanPaths = ApiIndex.split(args[4]);
        Path report = Paths.get(args[5]);

        Map<String, Set<String>> callersByReference = new TreeMap<>();
        Map<String, Integer> referenceCounts = new TreeMap<>();
        Map<String, Set<String>> mixinTargets = new TreeMap<>();
        Map<String, Integer> classesScanned = new LinkedHashMap<>();

        for (Path path : scanPaths) {
            String source = path.getFileName().toString();
            classesScanned.merge(source, scan(path, source, callersByReference, referenceCounts, mixinTargets), Integer::sum);
        }

        Map<Outcome, List<String>> byOutcome = new TreeMap<>();
        for (String reference : referenceCounts.keySet()) {
            byOutcome.computeIfAbsent(built.classify(reference, lwjgl3), outcome -> new ArrayList<>()).add(reference);
        }

        Files.createDirectories(report.getParent());
        try (BufferedWriter out = Files.newBufferedWriter(report)) {
            write(out, byOutcome, callersByReference, referenceCounts, mixinTargets, classesScanned);
        }

        System.out.println("scanned " + classesScanned.values().stream().mapToInt(Integer::intValue).sum()
                + " classes from " + classesScanned.size() + " source(s), "
                + referenceCounts.size() + " distinct org.lwjgl references");
        for (Outcome outcome : Outcome.values()) {
            List<String> references = byOutcome.getOrDefault(outcome, List.of());
            if (!references.isEmpty()) {
                System.out.println("  " + outcome + ": " + references.size());
            }
        }
        System.out.println("wrote " + report);
    }

    private static void write(BufferedWriter out, Map<Outcome, List<String>> byOutcome,
                              Map<String, Set<String>> callers, Map<String, Integer> counts,
                              Map<String, Set<String>> mixinTargets,
                              Map<String, Integer> classesScanned) throws IOException {
        out.write("What real code would hit\n");
        out.write("========================\n\n");
        out.write("Scanned:\n");
        for (Map.Entry<String, Integer> entry : classesScanned.entrySet()) {
            out.write("  " + entry.getValue() + " classes  " + entry.getKey() + "\n");
        }
        out.write("\n");

        for (Outcome outcome : Outcome.values()) {
            List<String> references = new ArrayList<>(byOutcome.getOrDefault(outcome, List.of()));
            out.write("== " + outcome + " (" + references.size() + "): " + outcome.description + "\n");
            if (outcome == Outcome.LWJGL3 || outcome == Outcome.MERGED) {
                out.write("\n");
                continue;
            }
            references.sort(Comparator.comparingInt((String r) -> -counts.getOrDefault(r, 0))
                    .thenComparing(r -> r));
            for (String reference : references) {
                Set<String> from = callers.getOrDefault(reference, Set.of());
                out.write("  " + reference + "  (" + counts.get(reference) + " call site(s))\n");

                for (String caller : from.stream().limit(3).toList()) {
                    out.write("      " + caller + "\n");
                }
                if (from.size() > 3) {
                    out.write("      ... and " + (from.size() - 3) + " more\n");
                }
            }
            out.write("\n");
        }

        out.write("== Mixins that touch org.lwjgl (" + mixinTargets.size() + ")\n");
        out.write("As Cleanroom's Minecraft code moves off the LWJGL 2 API, a mixin pinned to a method\n");
        out.write("whose signature mentions one of these types has to move with it.\n\n");
        if (mixinTargets.isEmpty()) {
            out.write("  none\n");
        }
        for (Map.Entry<String, Set<String>> entry : mixinTargets.entrySet()) {
            out.write("  " + entry.getKey() + "\n");
            for (String target : entry.getValue()) {
                out.write("      -> " + target + "\n");
            }
        }
    }

    private static int scan(Path path, String source, Map<String, Set<String>> callers,
                            Map<String, Integer> counts, Map<String, Set<String>> mixinTargets)
            throws IOException {
        int scanned = 0;
        if (Files.isDirectory(path)) {
            try (var walk = Files.walk(path)) {
                for (Path file : walk.filter(p -> p.toString().endsWith(".class")).toList()) {
                    try (InputStream in = Files.newInputStream(file)) {
                        scanned += accept(in, source, callers, counts, mixinTargets) ? 1 : 0;
                    }
                }
            }
            return scanned;
        }
        if (!path.getFileName().toString().endsWith(".jar")) {
            return 0;
        }
        try (JarFile jar = new JarFile(path.toFile())) {
            for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
                JarEntry entry = e.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }
                try (InputStream in = jar.getInputStream(entry)) {
                    scanned += accept(in, source, callers, counts, mixinTargets) ? 1 : 0;
                } catch (IllegalArgumentException unsupportedClassFile) {

                }
            }
        }
        return scanned;
    }

    private static boolean accept(InputStream in, String source, Map<String, Set<String>> callers,
                                  Map<String, Integer> counts, Map<String, Set<String>> mixinTargets)
            throws IOException {
        ClassNode node = new ClassNode();
        new ClassReader(in).accept(node, 0);

        Set<String> found = new LinkedHashSet<>();
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode call && call.owner.startsWith("org/lwjgl/")) {
                    found.add(call.owner + "." + call.name + call.desc);
                } else if (insn instanceof FieldInsnNode access && access.owner.startsWith("org/lwjgl/")) {
                    found.add(access.owner + "." + access.name + ":" + access.desc);
                }
            }
        }
        for (String reference : found) {
            counts.merge(reference, 1, Integer::sum);
            callers.computeIfAbsent(reference, r -> new TreeSet<>())
                    .add(source + " :: " + node.name.replace('/', '.'));
        }

        if (!found.isEmpty()) {
            Set<String> targets = mixinTargets(node);
            if (targets != null) {
                mixinTargets.put(source + " :: " + node.name.replace('/', '.'), targets);
            }
        }
        return true;
    }

    private static Set<String> mixinTargets(ClassNode node) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (node.visibleAnnotations != null) {
            annotations.addAll(node.visibleAnnotations);
        }
        if (node.invisibleAnnotations != null) {
            annotations.addAll(node.invisibleAnnotations);
        }
        for (AnnotationNode annotation : annotations) {
            if (!MIXIN_ANNOTATION.equals(annotation.desc)) {
                continue;
            }
            Set<String> targets = new TreeSet<>();
            List<Object> values = annotation.values;
            for (int i = 0; values != null && i + 1 < values.size(); i += 2) {
                Object value = values.get(i + 1);
                if (value instanceof List<?> list) {
                    for (Object element : list) {
                        if (element instanceof Type type) {
                            targets.add(type.getClassName());
                        } else if (element instanceof String string) {
                            targets.add(string);
                        }
                    }
                }
            }
            return targets;
        }
        return null;
    }

}
