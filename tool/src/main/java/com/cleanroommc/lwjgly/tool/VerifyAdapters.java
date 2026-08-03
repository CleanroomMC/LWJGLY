package com.cleanroommc.lwjgly.tool;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class VerifyAdapters {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("usage: VerifyAdapters <adapter-classes-dir>");
        }
        Path root = Paths.get(args[0]);
        if (!Files.isDirectory(root)) {
            System.out.println("no adapters to verify");
            return;
        }

        List<String> problems = new ArrayList<>();
        int checked = 0;
        try (var walk = Files.walk(root)) {
            for (Path file : walk.filter(p -> p.toString().endsWith(".class")).toList()) {
                ClassNode node = new ClassNode();
                try (InputStream in = Files.newInputStream(file)) {
                    new ClassReader(in).accept(node, 0);
                }
                problems.addAll(check(node));
                checked++;
            }
        }

        if (!problems.isEmpty()) {
            problems.forEach(System.err::println);
            throw new IllegalStateException(problems.size() + " adapter problem(s). Merge relies on these rules");
        }
        System.out.println("adapters verified: " + checked);
    }

    private static List<String> check(ClassNode node) {
        List<String> problems = new ArrayList<>();
        if (!node.fields.isEmpty()) {
            problems.add(node.name + " declares " + node.fields.size()
                    + " field(s); the merge copies methods only, move state to com.cleanroommc.lwjgly.rt");
        }
        if (node.version < Opcodes.V1_7) {
            problems.add(node.name + " is class file version " + node.version
                    + ", too old to carry stack map frames; the merge does not compute them");
        }
        for (MethodNode method : node.methods) {
            String where = node.name + "." + method.name + method.desc;
            if (method.name.equals("<clinit>")) {
                problems.add(where + ": a static initialiser cannot be merged");
            }
            if ((method.access & Opcodes.ACC_NATIVE) != 0) {
                problems.add(where + " is native - there is no body to copy");
            }
            if ((method.access & Opcodes.ACC_ABSTRACT) != 0) {
                problems.add(where + " is abstract - an adapter must carry a body");
            }
            if (method.name.equals("<init>")
                    && (method.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0
                    && !chainsToOwnClass(node, method)) {
                problems.add(where + " is a constructor that calls super() - the merge only carries"
                        + " constructors chaining to this(...), so this one would be silently dropped");
            }
        }
        return problems;
    }

    private static boolean chainsToOwnClass(ClassNode node, MethodNode constructor) {
        for (var insn : constructor.instructions) {
            if (insn instanceof MethodInsnNode call && call.name.equals("<init>")) {
                return call.owner.equals(node.name);
            }
        }
        return false;
    }
}
