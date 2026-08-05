package org.lwjgl.opengl;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks promoted extension shims without invoking the GL driver.
 */
class DelegatingExtensionShimTest {

    @Test
    void shimsActuallyDelegates() throws IOException {
        for (String simpleName : List.of("AMDMultiDrawIndirect", "EXTDrawRangeElements", "EXTMultiDrawArrays",
                "EXTFogCoord", "ATIDrawBuffers", "ATISeparateStencil")) {
            ClassNode node = read(simpleName);
            int checked = 0;
            for (MethodNode m : node.methods) {
                if ((m.access & Opcodes.ACC_PUBLIC) == 0 || m.name.charAt(0) == '<') {
                    continue;
                }
                boolean forwards = calls(m).stream().anyMatch(call -> call.owner.startsWith("org/lwjgl/opengl/GL"));
                assertTrue(forwards, simpleName + "." + m.name + m.desc + " does not forward to LWJGL 3");
                checked++;
            }
            assertTrue(checked > 0, simpleName + " declares no public methods");
        }
    }

    /**
     * ATI sets both faces in one call, so the shim must issue front and back calls.
     */
    @Test
    void stencilFuncSeparateSetsFollowSpec() throws IOException {
        MethodNode shim = method(read("ATISeparateStencil"), "glStencilFuncSeparateATI", "(IIII)V");
        List<MethodInsnNode> forwarded = calls(shim).stream()
                .filter(call -> call.name.equals("glStencilFuncSeparate"))
                .toList();
        assertEquals(2, forwarded.size(), "ATI sets both faces in one call, so the GL 2.0 translation needs one call per face");
        List<Integer> faces = new ArrayList<>();
        for (AbstractInsnNode insn : shim.instructions) {
            if (!(insn instanceof MethodInsnNode call) || !call.name.equals("glStencilFuncSeparate")) {
                continue;
            }
            AbstractInsnNode face = insn;
            for (int i = 0; i < 4 && face != null; face = face.getPrevious()) {
                if (face.getOpcode() != -1) {
                    i++;
                }
            }
            assertNotNull(face, "could not find the face argument");
            faces.add(constant(face));
        }
        assertEquals(List.of(GL11.GL_FRONT, GL11.GL_BACK), faces, "Front then back, matching the order glStencilFuncSeparateATI takes its arguments");
    }

    private static int constant(AbstractInsnNode insn) {
        if (insn instanceof IntInsnNode push) {
            return push.operand;
        }
        if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Integer value) {
            return value;
        }
        throw new AssertionError("not an int constant: opcode " + insn.getOpcode());
    }

    private static ClassNode read(String simpleName) throws IOException {
        ClassNode node = new ClassNode();
        try (InputStream in = DelegatingExtensionShimTest.class.getResourceAsStream(simpleName + ".class")) {
            assertNotNull(in, simpleName + " is not on the test classpath");
            new ClassReader(in).accept(node, 0);
        }
        return node;
    }

    private static MethodNode method(ClassNode node, String name, String desc) {
        return node.methods.stream()
                .filter(m -> m.name.equals(name) && (desc == null || m.desc.equals(desc)))
                .findFirst()
                .orElseThrow(() -> new AssertionError(node.name + " has no " + name));
    }

    private static List<MethodInsnNode> calls(MethodNode method) {
        List<MethodInsnNode> found = new ArrayList<>();
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof MethodInsnNode call) {
                found.add(call);
            }
        }
        return found;
    }

}
