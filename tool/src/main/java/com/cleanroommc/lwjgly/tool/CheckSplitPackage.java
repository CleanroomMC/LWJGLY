package com.cleanroommc.lwjgly.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public final class CheckSplitPackage {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: CheckSplitPackage <lwjgl3-classpath> <report>");
        }
        List<Path> jars = classpath(args[0]);
        Path report = Paths.get(args[1]);

        List<String> failures = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        lines.add("split-package gate: can LWJGLY add classes to org.lwjgl.** ?");
        lines.add("");

        for (Path jar : jars) {
            if (!Files.isRegularFile(jar) || !jar.getFileName().toString().endsWith(".jar")) {
                continue;
            }
            try (JarFile jarFile = new JarFile(jar.toFile())) {
                lines.add(jar.getFileName().toString());
                lines.addAll(inspect(jar, jarFile, failures));
                lines.add("");
            }
        }

        Files.createDirectories(report.getParent());
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(report))) {
            lines.forEach(out::println);
            if (failures.isEmpty()) {
                out.println("PASS - org.lwjgl is neither sealed nor signed. Split-package shims are legal.");
            } else {
                out.println("FAIL");
                failures.forEach(f -> out.println("  " + f));
            }
        }

        lines.forEach(System.out::println);
        if (!failures.isEmpty()) {
            System.err.println("Split-package gate FAILED. Bucket A cannot ship org.lwjgl.** classes:");
            failures.forEach(f -> System.err.println("  " + f));
            throw new IllegalStateException("LWJGL 3 seals or signs org.lwjgl: see " + report);
        }
        System.out.println("PASS - org.lwjgl is neither sealed nor signed.");
    }

    private static List<String> inspect(Path jar, JarFile jarFile, List<String> failures) throws IOException {
        List<String> lines = new ArrayList<>();
        Manifest manifest = jarFile.getManifest();

        if (manifest == null) {
            lines.add("  no manifest");
        } else {
            String globalSealed = manifest.getMainAttributes().getValue(Attributes.Name.SEALED);
            lines.add("  Sealed (main): " + (globalSealed == null ? "absent" : globalSealed));
            if ("true".equalsIgnoreCase(globalSealed)) {
                failures.add(jar.getFileName() + ": whole jar is sealed");
            }
            manifest.getEntries().forEach((name, attrs) -> {
                String sealed = attrs.getValue(Attributes.Name.SEALED);
                if ("true".equalsIgnoreCase(sealed) && name.startsWith("org/lwjgl")) {
                    failures.add(jar.getFileName() + ": package entry '" + name + "' is sealed");
                    lines.add("  Sealed (entry): " + name);
                }
            });
        }

        List<String> signatureFiles = new ArrayList<>();
        for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {
            String name = e.nextElement().getName().toUpperCase(Locale.ROOT);
            if (name.startsWith("META-INF/") && (name.endsWith(".SF") || name.endsWith(".DSA") || name.endsWith(".RSA") || name.endsWith(".EC"))) {
                signatureFiles.add(name);
            }
        }
        lines.add("  signed: " + (signatureFiles.isEmpty() ? "no" : signatureFiles));
        if (!signatureFiles.isEmpty()) {
            failures.add(jar.getFileName() + ": jar is signed " + signatureFiles);
        }
        return lines;
    }

    private static List<Path> classpath(String raw) {
        List<Path> paths = new ArrayList<>();
        for (String element : raw.split(java.io.File.pathSeparator)) {
            if (!element.isEmpty()) {
                paths.add(Paths.get(element));
            }
        }
        if (paths.isEmpty()) {
            throw new UncheckedIOException(new IOException("empty LWJGL 3 classpath"));
        }
        return paths;
    }

    private CheckSplitPackage() { }

}
