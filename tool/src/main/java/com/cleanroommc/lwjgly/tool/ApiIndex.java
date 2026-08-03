package com.cleanroommc.lwjgly.tool;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class ApiIndex implements Opcodes {

    private final Map<String, ClassNode> classes = new TreeMap<>();

    public static ApiIndex read(String classpath) throws IOException {
        ApiIndex index = new ApiIndex();
        for (Path jar : split(classpath)) {
            if (!jar.getFileName().toString().endsWith(".jar")) {
                continue;
            }
            try (JarFile jarFile = new JarFile(jar.toFile())) {
                for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {
                    JarEntry entry = e.nextElement();
                    String name = entry.getName();
                    if (!name.endsWith(".class") || name.startsWith("META-INF/")) {
                        continue;
                    }
                    try (InputStream in = jarFile.getInputStream(entry)) {
                        ClassNode node = new ClassNode();
                        new ClassReader(in).accept(node, ClassReader.SKIP_DEBUG);
                        index.classes.putIfAbsent(node.name, node);
                    }
                }
            }
        }
        return index;
    }

    public static List<Path> split(String classpath) {
        List<Path> paths = new ArrayList<>();
        for (String element : classpath.split(File.pathSeparator)) {
            if (!element.isEmpty()) {
                paths.add(Paths.get(element));
            }
        }
        return paths;
    }

    // TODO: Evaluate if ACC_PROTECTED is callable, are LWJGL 2 classes extensible?
    public static boolean isCallable(int access) {
        return (access & ACC_PUBLIC) != 0 && (access & ACC_SYNTHETIC) == 0;
    }

    public static boolean isNameable(ClassNode node) {
        return node != null && (node.access & ACC_PUBLIC) != 0 && node.name.indexOf('$') < 0;
    }

    public static boolean isInlinedConstant(FieldNode field) {
        return field.value != null && (field.access & ACC_STATIC) != 0 && (field.access & ACC_FINAL) != 0;
    }

    private ApiIndex() { }

    public Map<String, ClassNode> classes() {
        return classes;
    }

    public ClassNode get(String internalName) {
        return classes.get(internalName);
    }

    public boolean has(String internalName) {
        return classes.containsKey(internalName);
    }

}
