package com.cleanroommc.lwjgly.merge;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Merges an adapter's methods into an LWJGL 3 class.
 *
 * <p>This allows linkage for LWJGL 2's API to be in the {@code org.lwjgl} namespace.
 * An adapter is an ordinary class, compiled against LWJGL 3, carrying only the methods LWJGL 3
 * lacks, merging rewrites its own name to the target's so that self-references land, then appends
 * the methods the target does not already declare.
 *
 * <p>Two rules keeping transformations safe:
 *
 * <ul>
 *   <li>{@code COMPUTE_MAXS}, never {@code COMPUTE_FRAMES}. Frame computation resolves common
 *       supertypes, which needs a class loader leading to transformer deadlocks. Adapter methods
 *       already carry javac's frames and {@link ClassRemapper} rewrites the types in them.</li>
 *   <li>Adapters declare no fields and no {@code <clinit>}. State lives in
 *       {@code com.cleanroommc.lwjgly.rt}, which the merged bodies call. Without that rule a merge
 *       would have to splice two static initialisers together and reconcile field layouts.</li>
 * </ul>
 */
public final class ClassMerger {

    /**
     * Private adapter helpers are prefixed with these on merge, just in case of collisions.
     */
    public static final String HELPER_PREFIX = "lwjgly$";

    public static final class MergeException extends RuntimeException {

        public MergeException(String message) {
            super(message);
        }

    }

    public static byte[] merge(byte[] targetBytes, byte[] adapterBytes) {
        ClassReader targetReader = new ClassReader(targetBytes);
        ClassNode target = new ClassNode();
        targetReader.accept(target, 0);

        ClassNode adapter = new ClassNode();
        new ClassReader(adapterBytes).accept(adapter, 0);

        merge(target, adapter);

        ClassWriter writer = new ClassWriter(targetReader, ClassWriter.COMPUTE_MAXS);
        target.accept(writer);
        return writer.toByteArray();
    }

    public static void merge(ClassNode target, ClassNode adapterClass) {
        reject(adapterClass);

        Map<String, MethodNode> existing = new HashMap<>();
        for (MethodNode method : target.methods) {
            existing.put(key(method.name, method.desc), method);
        }

        ClassNode adapter = rename(adapterClass, target.name);

        for (MethodNode method : adapter.methods) {
            if (method.name.equals("<clinit>")) {
                continue;
            }
            if (method.name.equals("<init>") && !chainsToOwnClass(method, target.name)) {
                continue;
            }
            MethodNode clash = existing.get(key(method.name, method.desc));
            if (clash != null) {
                if (!isReachable(clash) && isReachable(method)) {
                    // The signature exists but nothing outside org.lwjgl can call it
                    // See: org.lwjgl.openal.AL.destroy() (public in LWJGL 2, package-private in LWJGL 3)
                    target.methods.remove(clash);
                } else {
                    continue;
                }
            }
            existing.put(key(method.name, method.desc), method);
            target.methods.add(method);
        }
    }

    /**
     * Whether an adapter constructor can be merged.
     *
     * <p>Only those that chain to another constructor of the same class with {@code this(...)} whose
     * owner had just been renamed, so it lands on a constructor the target really declares.
     * An adapter's {@code this(...)} may name a signature LWJGL 3 already has or one the merge is adding alongside it.
     *
     * <p>Everything else is skipped, and the case that matters is the private no-argument
     * constructor javac gives every generated shim. Its body is {@code super()} on
     * {@code Object}, copied into a target whose superclass is not {@code Object},
     * would cause {@code VerifyError} at load.
     */
    private static boolean chainsToOwnClass(MethodNode constructor, String targetName) {
        for (AbstractInsnNode insn : constructor.instructions) {
            if (insn instanceof MethodInsnNode call && call.name.equals("<init>")) {
                return call.owner.equals(targetName);
            }
        }
        return false;
    }

    /**
     * Rewrites the adapter's references to itself, and mangles private helpers.
     * Both in one pass; {@link SimpleRemapper} keys method renames by owner.
     */
    private static ClassNode rename(ClassNode adapter, String targetName) {
        Map<String, String> mapping = new HashMap<>();
        mapping.put(adapter.name, targetName);
        for (MethodNode method : adapter.methods) {
            if (method.name.equals("<init>") || method.name.equals("<clinit>")) {
                continue;
            }
            if (!isReachable(method)) {
                mapping.put(adapter.name + "." + method.name + method.desc, HELPER_PREFIX + method.name);
            }
        }
        ClassNode renamed = new ClassNode();
        adapter.accept(new ClassRemapper(renamed, new SimpleRemapper(mapping)));
        return renamed;
    }

    private static boolean isReachable(MethodNode method) {
        return (method.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0;
    }

    private static void reject(ClassNode adapter) {
        if (!adapter.fields.isEmpty()) {
            throw new MergeException(adapter.name + " declares fields, adapter state belongs in com.cleanroommc.lwjgly.rt.");
        }
        for (MethodNode method : adapter.methods) {
            if (method.name.equals("<clinit>")) {
                throw new MergeException(adapter.name + " declares a static initialiser, which cannot be merged.");
            }
            if ((method.access & Opcodes.ACC_NATIVE) != 0) {
                throw new MergeException(adapter.name + "." + method.name + " is native, there is nothing to copy.");
            }
            if ((method.access & Opcodes.ACC_ABSTRACT) != 0) {
                throw new MergeException(adapter.name + "." + method.name + " is abstract, an adapter must carry a body.");
            }
        }
    }

    private static String key(String name, String desc) {
        return name + desc;
    }

    private ClassMerger() { }

}
