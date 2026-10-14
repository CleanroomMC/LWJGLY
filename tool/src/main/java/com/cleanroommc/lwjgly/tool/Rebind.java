package com.cleanroommc.lwjgly.tool;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * Rebinds LWJGL 2's GL extension classes that LWJGL 3 dropped, from LWJGL 2's own bytecode.
 *
 * <p>Each {@code native} entry point becomes a call through {@code org.lwjgl.system.JNI}, or through
 * libffi in {@code LegacyGL} where no overload matches. Function pointers are read from
 * {@code ContextCapabilities} fields that the generator adds, and LWJGL 2's internal helpers
 * are redirected to {@code LegacyGL}.
 */
final class Rebind implements Opcodes {

    static final String CAPS = "org/lwjgl/opengl/ContextCapabilities";
    private static final String LEGACY = "org/lwjgl/opengl/LegacyGL";
    private static final String REFERENCES = LEGACY + "$References";
    private static final String JNI = "org/lwjgl/system/JNI";
    private static final String BYTE_BUFFER = "Ljava/nio/ByteBuffer;";
    private static final Map<String, String> HELPERS = Map.of(
            "org/lwjgl/opengl/APIUtil", LEGACY,
            "org/lwjgl/opengl/GLChecks", LEGACY,
            "org/lwjgl/opengl/StateTracker", LEGACY,
            "org/lwjgl/opengl/References", REFERENCES
    );

    /** @param functions every function pointer field the rebound classes read from {@code ContextCapabilities} */
    record Result(Map<String, byte[]> classes, Set<String> functions, Map<String, String> rejected) { }

    static Result of(Collection<String> candidates, ApiIndex lwjgl2, ApiIndex lwjgl3,
                     Map<String, Delta.ClassDelta> deltas, Set<String> handWritten) {
        Map<String, String> jni = jniByDescriptor(lwjgl3);
        Set<String> flags = new HashSet<>();
        for (FieldNode field : lwjgl2.get(CAPS).fields) {
            if (field.desc.equals("Z") && ApiIndex.isCallable(field.access)) {
                flags.add(field.name);
            }
        }

        Map<String, ClassNode> rebound = new TreeMap<>();
        Map<String, String> rejected = new TreeMap<>();
        for (String name : candidates) {
            ClassNode node = remapped(lwjgl2.get(name));
            String failure = bindNatives(node, jni);
            if (failure == null) {
                rebound.put(name, node);
            } else {
                rejected.put(name, failure);
            }
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (Iterator<Map.Entry<String, ClassNode>> it = rebound.entrySet().iterator(); it.hasNext(); ) {
                ClassNode node = it.next().getValue();
                String failure = unresolved(node, rebound.keySet(), flags, lwjgl3, deltas, handWritten);
                if (failure != null) {
                    rejected.put(node.name, failure);
                    it.remove();
                    changed = true;
                }
            }
        }

        Map<String, byte[]> classes = new TreeMap<>();
        Set<String> functions = new TreeSet<>();
        for (ClassNode node : rebound.values()) {
            for (MethodNode method : node.methods) {
                for (AbstractInsnNode insn : method.instructions) {
                    if (insn instanceof FieldInsnNode field && field.owner.equals(CAPS) && field.desc.equals("J")) {
                        functions.add(field.name);
                    }
                }
            }
            // LWJGL 2 targets class version 49, so no stack map frames need computing
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            classes.put(node.name, writer.toByteArray());
        }
        return new Result(classes, functions, rejected);
    }

    private static ClassNode remapped(ClassNode original) {
        ClassNode copy = new ClassNode();
        original.accept(new ClassRemapper(copy, new SimpleRemapper(HELPERS)));
        return copy;
    }

    private static Map<String, String> jniByDescriptor(ApiIndex lwjgl3) {
        Map<String, String> byDescriptor = new HashMap<>();
        for (MethodNode method : lwjgl3.get(JNI).methods) {
            // N is the C long, 32 bits on Windows, which would truncate a pointer or GLint64
            if ((method.access & ACC_STATIC) != 0 && ApiIndex.isCallable(method.access) && method.name.indexOf('N') < 0) {
                byDescriptor.putIfAbsent(method.desc, method.name);
            }
        }
        return byDescriptor;
    }

    private static String bindNatives(ClassNode node, Map<String, String> jni) {
        for (MethodNode method : node.methods) {
            if ((method.access & ACC_NATIVE) == 0) {
                continue;
            }
            if ((method.access & ACC_STATIC) == 0) {
                return "declares instance native method " + method.name + method.desc;
            }
            Type[] args = Type.getArgumentTypes(method.desc);
            if (args.length == 0 || args[args.length - 1] != Type.LONG_TYPE) {
                return "native method " + method.name + method.desc + " takes no function pointer";
            }
            InsnList body = body(method.desc, args, jni);
            if (body == null) {
                return "native method " + method.name + method.desc + " has no call shape LWJGLY can bind";
            }
            method.access &= ~ACC_NATIVE;
            method.instructions = body;
        }
        return null;
    }

    private static InsnList body(String desc, Type[] args, Map<String, String> jni) {
        Type returns = Type.getReturnType(desc);
        int last = args.length - 1;
        InsnList body = new InsnList();
        if (returns.getDescriptor().equals(BYTE_BUFFER)) {
            if (args.length >= 3 && args[last - 1].getDescriptor().equals(BYTE_BUFFER) && args[last - 2] == Type.LONG_TYPE) {
                // (..., result_size, old_buffer, function): the GL call returns the mapped address
                Type[] call = Arrays.copyOf(args, last - 1);
                call[call.length - 1] = Type.LONG_TYPE;
                String callDesc = Type.getMethodDescriptor(Type.LONG_TYPE, call);
                String name = jni.get(callDesc);
                if (name == null || !primitive(call)) {
                    return null;
                }
                int[] slots = slots(args);
                for (int i = 0; i < last - 2; i++) {
                    body.add(new VarInsnNode(args[i].getOpcode(ILOAD), slots[i]));
                }
                body.add(new VarInsnNode(LLOAD, slots[last]));
                body.add(new MethodInsnNode(INVOKESTATIC, JNI, name, callDesc, false));
                body.add(new VarInsnNode(LLOAD, slots[last - 2]));
                body.add(new VarInsnNode(ALOAD, slots[last - 1]));
                body.add(new MethodInsnNode(INVOKESTATIC, LEGACY, "mapped", "(JJ" + BYTE_BUFFER + ")" + BYTE_BUFFER, false));
                body.add(new InsnNode(ARETURN));
                return body;
            }
            if (desc.equals("(IIJJ)" + BYTE_BUFFER)) {
                // (target, pname, result_size, function): the GL call writes the address through void**
                loadAll(body, args);
                body.add(new MethodInsnNode(INVOKESTATIC, LEGACY, "pointer", desc, false));
                body.add(new InsnNode(ARETURN));
                return body;
            }
            return null;
        }
        if (!primitive(args) || returns.getSort() > Type.DOUBLE) {
            return null;
        }
        String name = jni.get(desc);
        if (name != null) {
            loadAll(body, args);
            body.add(new MethodInsnNode(INVOKESTATIC, JNI, name, desc, false));
            body.add(new InsnNode(returns.getOpcode(IRETURN)));
            return body;
        }
        foreignCall(body, desc, args, returns);
        return body;
    }

    /** Packs every argument into a {@code long[]} for {@code LegacyGL.ffi}, which reads them back by {@code desc}. */
    private static void foreignCall(InsnList body, String desc, Type[] args, Type returns) {
        int[] slots = slots(args);
        int last = args.length - 1;
        body.add(new VarInsnNode(LLOAD, slots[last]));
        body.add(new LdcInsnNode(desc));
        body.add(new IntInsnNode(BIPUSH, last));
        body.add(new IntInsnNode(NEWARRAY, T_LONG));
        for (int i = 0; i < last; i++) {
            body.add(new InsnNode(DUP));
            body.add(new IntInsnNode(BIPUSH, i));
            body.add(new VarInsnNode(args[i].getOpcode(ILOAD), slots[i]));
            switch (args[i].getSort()) {
                case Type.LONG -> { }
                case Type.FLOAT -> {
                    body.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "floatToRawIntBits", "(F)I", false));
                    body.add(new InsnNode(I2L));
                }
                case Type.DOUBLE ->
                        body.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "doubleToRawLongBits", "(D)J", false));
                default -> body.add(new InsnNode(I2L));
            }
            body.add(new InsnNode(LASTORE));
        }
        body.add(new MethodInsnNode(INVOKESTATIC, LEGACY, "ffi", "(JLjava/lang/String;[J)J", false));
        switch (returns.getSort()) {
            case Type.VOID -> {
                body.add(new InsnNode(POP2));
                body.add(new InsnNode(RETURN));
            }
            case Type.LONG -> body.add(new InsnNode(LRETURN));
            case Type.DOUBLE -> {
                body.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "longBitsToDouble", "(J)D", false));
                body.add(new InsnNode(DRETURN));
            }
            case Type.FLOAT -> {
                body.add(new InsnNode(L2I));
                body.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "intBitsToFloat", "(I)F", false));
                body.add(new InsnNode(FRETURN));
            }
            default -> {
                body.add(new InsnNode(L2I));
                switch (returns.getSort()) {
                    case Type.BYTE -> body.add(new InsnNode(I2B));
                    case Type.SHORT -> body.add(new InsnNode(I2S));
                    case Type.CHAR -> body.add(new InsnNode(I2C));
                    default -> { }
                }
                body.add(new InsnNode(IRETURN));
            }
        }
    }

    private static void loadAll(InsnList body, Type[] args) {
        int[] slots = slots(args);
        for (int i = 0; i < args.length; i++) {
            body.add(new VarInsnNode(args[i].getOpcode(ILOAD), slots[i]));
        }
    }

    private static int[] slots(Type[] args) {
        int[] slots = new int[args.length];
        int slot = 0;
        for (int i = 0; i < args.length; i++) {
            slots[i] = slot;
            slot += args[i].getSize();
        }
        return slots;
    }

    private static boolean primitive(Type[] types) {
        for (Type type : types) {
            if (type.getSort() > Type.DOUBLE) {
                return false;
            }
        }
        return true;
    }

    private static String unresolved(ClassNode node, Set<String> rebound, Set<String> flags, ApiIndex lwjgl3,
                                      Map<String, Delta.ClassDelta> deltas, Set<String> handWritten) {
        if (node.superName != null && node.superName.startsWith("org/lwjgl/") && !rebound.contains(node.superName)
                && !lwjgl3.has(node.superName)) {
            return "extends " + node.superName + ", which is not rebound";
        }
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                String owner;
                String name;
                String desc;
                boolean field;
                if (insn instanceof MethodInsnNode call) {
                    owner = call.owner;
                    name = call.name;
                    desc = call.desc;
                    field = false;
                } else if (insn instanceof FieldInsnNode access) {
                    owner = access.owner;
                    name = access.name;
                    desc = access.desc;
                    field = true;
                } else {
                    continue;
                }
                if (!owner.startsWith("org/lwjgl/") || available(owner, name, desc, field, node.name, rebound, flags, lwjgl3, deltas, handWritten)) {
                    continue;
                }
                return "references " + owner + "." + name + (field ? " : " : "") + desc;
            }
        }
        return null;
    }

    private static boolean available(String owner, String name, String desc, boolean field, String self,
                                     Set<String> rebound, Set<String> flags, ApiIndex lwjgl3,
                                     Map<String, Delta.ClassDelta> deltas, Set<String> handWritten) {
        if (owner.equals(self) || rebound.contains(owner) || owner.equals(LEGACY) || owner.equals(REFERENCES)
                || handWritten.contains(owner)) {
            return true;
        }
        if (owner.equals(CAPS)) {
            return field && (desc.equals("J") || desc.equals("Z") && flags.contains(name));
        }
        if (field ? Resolution.findField(lwjgl3, owner, name, desc) != null : Resolution.hasMethod(lwjgl3, owner, name, desc)) {
            return true;
        }
        Delta.ClassDelta delta = deltas.get(owner);
        if (delta == null) {
            return false;
        }
        if (delta.bucket() == Delta.Bucket.SHIM) {
            return delta.shimSource() == Delta.ShimSource.VENDOR;
        }
        return !field && delta.bucket() == Delta.Bucket.INJECT && delta.methods().stream()
                .anyMatch(m -> m.name().equals(name) && m.desc().equals(desc) && m.fix().isImplementable());
    }

    private Rebind() { }

}
