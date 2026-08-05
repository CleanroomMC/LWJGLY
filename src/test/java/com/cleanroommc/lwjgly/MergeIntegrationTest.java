package com.cleanroommc.lwjgly;

import com.cleanroommc.lwjgly.merge.ClassMerger;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Merge tests against the real LWJGL 3 classes.
 */
class MergeIntegrationTest {

    private static final String GL11 = "org/lwjgl/opengl/GL11";

    @Test
    void mergedGl11PassesVerification() throws IOException {
        byte[] merged = LWJGLYTransformer.transform(GL11, original(GL11));
        StringWriter problems = new StringWriter();
        CheckClassAdapter.verify(new ClassReader(merged), false, new PrintWriter(problems));
        assertEquals("", problems.toString(), "Merged GL11 failed bytecode verification");
    }

    @Test
    void mergedGl11CarriesTheLwjgl2Signatures() throws IOException {
        Map<String, MethodNode> before = methods(original(GL11));
        Map<String, MethodNode> after = methods(LWJGLYTransformer.transform(GL11, original(GL11)));
        assertTrue(after.containsKey("glFog(ILjava/nio/FloatBuffer;)V"), "LWJGL 2's glFog(int, FloatBuffer) is missing after the merge");
        assertTrue(after.containsKey("glGetInteger(ILjava/nio/IntBuffer;)V"), "LWJGL 2's glGetInteger(int, IntBuffer) is missing after the merge");
        for (Map.Entry<String, MethodNode> entry : before.entrySet()) {
            assertTrue(after.containsKey(entry.getKey()), "The merge dropped " + entry.getKey());
        }
        assertTrue(after.size() > before.size(), "The merge added nothing");
    }

    @Test
    void theMergedClassDefinesAndItsMembersAreReachable() throws Exception {
        Class<?> gl11 = new MergedLoader().loadClass("org.lwjgl.opengl.GL11");
        Method glFog = gl11.getMethod("glFog", int.class, FloatBuffer.class);
        assertTrue(Modifier.isStatic(glFog.getModifiers()));
        assertTrue(Modifier.isPublic(glFog.getModifiers()));
        assertNotNull(gl11.getMethod("glGetInteger", int.class, IntBuffer.class));
    }

    @Test
    void theBodyDelegatesToGlsOwnSpelling() throws IOException {
        MethodNode glFog = methods(LWJGLYTransformer.transform(GL11, original(GL11))).get("glFog(ILjava/nio/FloatBuffer;)V");
        assertNotNull(glFog, "glFog was not merged");
        String called = null;
        for (AbstractInsnNode insn : glFog.instructions) {
            if (insn instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESTATIC) {
                called = call.owner + "." + call.name + call.desc;
            }
        }
        assertEquals("org/lwjgl/opengl/GL11.glFogfv(ILjava/nio/FloatBuffer;)V", called, "glFog should delegate to GL's own name for the same call");
    }

    @Test
    void handWrittenAdaptersWinGeneratedShims() throws IOException {
        Map<String, MethodNode> merged = methods(LWJGLYTransformer.transform("org/lwjgl/opengl/GL20", original("org/lwjgl/opengl/GL20")));
        for (String member : new String[] {"glGetActiveUniformSize(II)I", "glGetActiveUniformType(II)I",
                "glGetActiveAttribSize(II)I", "glGetActiveAttribType(II)I", "glShaderSource(ILjava/nio/ByteBuffer;)V"}) {
            MethodNode method = merged.get(member);
            assertNotNull(method, member + " was not merged into GL20");
            assertFalse(throwsOnly(method), member + " got the generated placeholder, not the body");
        }
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

    private static Map<String, MethodNode> methods(byte[] classBytes) {
        ClassNode node = new ClassNode();
        new ClassReader(classBytes).accept(node, 0);
        Map<String, MethodNode> methods = new HashMap<>();
        for (MethodNode method : node.methods) {
            methods.put(method.name + method.desc, method);
        }
        return methods;
    }

    private static byte[] original(String internalName) throws IOException {
        try (InputStream in = MergeIntegrationTest.class.getResourceAsStream("/" + internalName + ".class")) {
            assertNotNull(in, internalName + " is not on the test classpath");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    private static final class MergedLoader extends ClassLoader {

        private final Map<String, Class<?>> defined = new HashMap<>();

        MergedLoader() {
            super(MergeIntegrationTest.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith("org.lwjgl.")) {
                return super.loadClass(name, resolve);
            }
            Class<?> existing = defined.get(name);
            if (existing != null) {
                return existing;
            }
            String internalName = name.replace('.', '/');
            try {
                byte[] bytes = original(internalName);
                if (LWJGLYTransformer.handles(internalName)) {
                    bytes = LWJGLYTransformer.transform(internalName, bytes);
                }
                Class<?> type = defineClass(name, bytes, 0, bytes.length);
                defined.put(name, type);
                return type;
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }

}
