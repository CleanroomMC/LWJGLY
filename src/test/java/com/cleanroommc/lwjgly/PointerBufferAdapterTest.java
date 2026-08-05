package com.cleanroommc.lwjgly;

import com.cleanroommc.lwjgly.rt.PointerBufferSupport;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.Pointer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies PointerBuffer instance bridges, constructors, and pointer arithmetic.
 */
class PointerBufferAdapterTest {

    private static final String POINTER_BUFFER = "org/lwjgl/PointerBuffer";

    @Test
    void mergedPointerBufferPassesVerification() throws IOException {
        byte[] merged = LWJGLYTransformer.transform(POINTER_BUFFER, original(POINTER_BUFFER));
        StringWriter problems = new StringWriter();
        CheckClassAdapter.verify(new ClassReader(merged), false, new PrintWriter(problems));
        assertEquals("", problems.toString(), "Merged PointerBuffer failed bytecode verification");
    }

    @Test
    void mergedPointerBufferCarriesTheLwjgl2Accessors() throws IOException {
        Map<String, MethodNode> after = methods(LWJGLYTransformer.transform(POINTER_BUFFER, original(POINTER_BUFFER)));
        assertTrue(after.containsKey("getBuffer()Ljava/nio/ByteBuffer;"));
        assertTrue(after.containsKey("positionByte()I"));
        assertTrue(after.containsKey("remainingByte()I"));
        assertTrue(after.containsKey("order()Ljava/nio/ByteOrder;"));
        assertTrue(after.containsKey("isReadOnly()Z"));
        assertTrue(after.containsKey("asReadOnlyBuffer()Lorg/lwjgl/PointerBuffer;"));
        assertTrue(after.containsKey("newInstance(Ljava/nio/ByteBuffer;)Lorg/lwjgl/PointerBuffer;"));
        assertTrue(after.containsKey("is64Bit()Z"));
        assertTrue(after.containsKey("getPointerSize()I"));
    }

    @Test
    void mergedPointerBufferCarriesTheChainableOperationsWithLwjgl2sReturnType() throws IOException {
        Map<String, MethodNode> after = methods(LWJGLYTransformer.transform(POINTER_BUFFER, original(POINTER_BUFFER)));
        for (String member : new String[]{"position(I)Lorg/lwjgl/PointerBuffer;", "limit(I)Lorg/lwjgl/PointerBuffer;",
                "mark()Lorg/lwjgl/PointerBuffer;", "reset()Lorg/lwjgl/PointerBuffer;", "clear()Lorg/lwjgl/PointerBuffer;",
                "flip()Lorg/lwjgl/PointerBuffer;", "rewind()Lorg/lwjgl/PointerBuffer;", "compact()Lorg/lwjgl/PointerBuffer;",
                "slice()Lorg/lwjgl/PointerBuffer;", "duplicate()Lorg/lwjgl/PointerBuffer;", "put(Lorg/lwjgl/PointerBuffer;)Lorg/lwjgl/PointerBuffer;"}) {
            assertTrue(after.containsKey(member), "The merge did not add " + member);
        }
    }

    @Test
    void mergedPointerBufferCarriesLwjgl2sConstructors() throws IOException {
        Map<String, MethodNode> after = methods(LWJGLYTransformer.transform(POINTER_BUFFER, original(POINTER_BUFFER)));
        assertTrue(after.containsKey("<init>(I)V"), "PointerBuffer(int) was not merged");
        assertTrue(after.containsKey("<init>(Ljava/nio/ByteBuffer;)V"), "PointerBuffer(ByteBuffer) was not merged");
        assertFalse(after.containsKey("<init>()V"), "An adapter's own no-argument constructor was merged; its super() call names Object");
    }

    @Test
    void byteBufferConstructorChainsToLwjgl3sOwn() throws IOException {
        MethodNode constructor = methods(LWJGLYTransformer.transform(POINTER_BUFFER, original(POINTER_BUFFER))).get("<init>(Ljava/nio/ByteBuffer;)V");
        assertNotNull(constructor, "PointerBuffer(ByteBuffer) was not merged");
        String chained = null;
        for (AbstractInsnNode insn : constructor.instructions) {
            if (insn instanceof MethodInsnNode call && call.name.equals("<init>")) {
                chained = call.owner + "." + call.name + call.desc;
                break;
            }
        }
        assertEquals("org/lwjgl/PointerBuffer.<init>(JLjava/nio/ByteBuffer;IIII)V", chained, "The constructor should chain to LWJGL 3's own, not to the adapter's stand-in");
    }

    @Test
    void capacityConstructorChainsToTheByteBufferOne() throws IOException {
        MethodNode constructor = methods(LWJGLYTransformer.transform(POINTER_BUFFER, original(POINTER_BUFFER))).get("<init>(I)V");
        assertNotNull(constructor, "PointerBuffer(int) was not merged");
        String chained = null;
        for (AbstractInsnNode insn : constructor.instructions) {
            if (insn instanceof MethodInsnNode call && call.name.equals("<init>")) {
                chained = call.owner + "." + call.name + call.desc;
                break;
            }
        }
        assertEquals("org/lwjgl/PointerBuffer.<init>(Ljava/nio/ByteBuffer;)V", chained);
    }

    @Test
    void byteBufferConstructorCountsInPointers() throws IOException {
        MethodNode constructor = methods(LWJGLYTransformer.transform(POINTER_BUFFER, original(POINTER_BUFFER))).get("<init>(Ljava/nio/ByteBuffer;)V");
        assertNotNull(constructor, "PointerBuffer(ByteBuffer) was not merged");
        boolean divides = false;
        for (AbstractInsnNode insn : constructor.instructions) {
            if (insn.getOpcode() == Opcodes.IDIV) {
                divides = true;
                break;
            }
        }
        assertTrue(divides, "The remaining byte count must be divided by the pointer size");
    }

    @Test
    void chainableBridgesCallCustomBufferAndNotThemselves() throws IOException {
        Map<String, MethodNode> after = methods(LWJGLYTransformer.transform(POINTER_BUFFER, original(POINTER_BUFFER)));
        MethodNode position = after.get("position(I)Lorg/lwjgl/PointerBuffer;");
        assertNotNull(position, "Position bridge was not merged");
        boolean callsSupport = false;
        for (AbstractInsnNode insn : position.instructions) {
            if (insn instanceof MethodInsnNode call) {
                assertNotEquals("position(I)Lorg/lwjgl/PointerBuffer;", call.name + call.desc, "The bridge calls itself and would recurse without end");
                if (call.getOpcode() == Opcodes.INVOKESTATIC && call.owner.equals("com/cleanroommc/lwjgly/rt/PointerBufferSupport")) {
                    callsSupport = true;
                }
            }
        }
        assertTrue(callsSupport, "The bridge should hand off to PointerBufferSupport");
        MethodNode support = supportMethod("position");
        boolean reachesCustomBuffer = false;
        for (AbstractInsnNode insn : support.instructions) {
            if (insn instanceof MethodInsnNode call && call.name.equals("position") && call.desc.equals("(I)Lorg/lwjgl/system/CustomBuffer;")) {
                reachesCustomBuffer = true;
            }
        }
        assertTrue(reachesCustomBuffer, "PointerBufferSupport.position must call the CustomBuffer-returning signature");
    }

    @Test
    void chainableOperationsBehaveAsLwjgl2sDid() {
        PointerBuffer buffer = PointerBuffer.allocateDirect(4);
        buffer.put(0, 10L).put(1, 20L).put(2, 30L).put(3, 40L);
        assertSame(buffer, PointerBufferSupport.position(buffer, 1));
        assertEquals(1, buffer.position());
        assertSame(buffer, PointerBufferSupport.limit(buffer, 3));
        assertEquals(3, buffer.limit());
        assertSame(buffer, PointerBufferSupport.mark(buffer));
        assertSame(buffer, PointerBufferSupport.position(buffer, 2));
        assertSame(buffer, PointerBufferSupport.reset(buffer));
        assertEquals(1, buffer.position(), "Reset should return to the mark");
        assertSame(buffer, PointerBufferSupport.rewind(buffer));
        assertEquals(0, buffer.position());
        assertSame(buffer, PointerBufferSupport.flip(buffer));
        assertSame(buffer, PointerBufferSupport.clear(buffer));
        assertEquals(0, buffer.position());
        assertEquals(4, buffer.limit());
        PointerBuffer slice = PointerBufferSupport.slice(PointerBufferSupport.position(buffer, 2));
        assertNotSame(buffer, slice);
        assertEquals(2, slice.capacity(), "A slice spans position to limit");
        assertEquals(30L, slice.get(0));
        PointerBuffer copy = PointerBufferSupport.duplicate(buffer);
        assertNotSame(buffer, copy);
        assertEquals(4, copy.capacity(), "A duplicate spans the whole buffer");
        assertEquals(30L, copy.get(2));
        buffer.free();
    }

    @Test
    void bulkPutCopiesPointersAndAdvancesBoth() {
        PointerBuffer source = PointerBuffer.allocateDirect(2);
        PointerBuffer target = PointerBuffer.allocateDirect(4);
        source.put(0, 111L).put(1, 222L);
        assertSame(target, PointerBufferSupport.put(target, source));
        assertEquals(2, target.position(), "The target advances by what was copied");
        assertEquals(2, source.position(), "And so does the source");
        assertEquals(111L, target.get(0));
        assertEquals(222L, target.get(1));
        source.free();
        target.free();
    }

    @Test
    void drainLoopTerminates() {
        PointerBuffer source = PointerBuffer.allocateDirect(3);
        PointerBuffer target = PointerBuffer.allocateDirect(3);
        source.put(0, 1L).put(1, 2L).put(2, 3L);
        int guard = 0;
        while (source.hasRemaining() && guard++ < 100) {
            PointerBufferSupport.put(target, source);
        }
        assertTrue(guard < 100, "The drain loop did not terminate: the source position never advanced");
        assertEquals(3, target.position());
        source.free();
        target.free();
    }

    private static MethodNode supportMethod(String name) throws IOException {
        ClassNode node = new ClassNode();
        try (InputStream in = PointerBufferAdapterTest.class.getClassLoader().getResourceAsStream("com/cleanroommc/lwjgly/rt/PointerBufferSupport.class")) {
            assertNotNull(in, "PointerBufferSupport.class not on the test classpath");
            new ClassReader(in).accept(node, 0);
        }
        for (MethodNode method : node.methods) {
            if (method.name.equals(name)) {
                return method;
            }
        }
        throw new AssertionError("PointerBufferSupport." + name + " not found");
    }

    @Test
    void instanceBodiesSurviveTheRename() throws IOException {
        MethodNode positionByte = methods(LWJGLYTransformer.transform(POINTER_BUFFER, original(POINTER_BUFFER))).get("positionByte()I");
        assertNotNull(positionByte, "positionByte was not merged");
        String called = null;
        for (AbstractInsnNode insn : positionByte.instructions) {
            if (insn instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESTATIC) {
                called = call.owner + "." + call.name + call.desc;
            }
        }
        assertEquals("com/cleanroommc/lwjgly/rt/PointerBufferSupport.positionByte(Ljava/lang/Object;)I", called);
    }

    @Test
    void mergingDoesNotDisturbLwjgl3Members() throws IOException {
        Map<String, MethodNode> before = methods(original(POINTER_BUFFER));
        Map<String, MethodNode> after = methods(LWJGLYTransformer.transform(POINTER_BUFFER, original(POINTER_BUFFER)));
        for (String member : before.keySet()) {
            assertTrue(after.containsKey(member), "The merge dropped " + member);
        }
    }

    @Test
    void byteOffsetsCountInPointers() {
        PointerBuffer buffer = PointerBuffer.allocateDirect(4);
        buffer.position(1);
        assertEquals(Pointer.POINTER_SIZE, PointerBufferSupport.positionByte(buffer));
        assertEquals(3 * Pointer.POINTER_SIZE, PointerBufferSupport.remainingByte(buffer));
        buffer.position(0);
        assertEquals(0, PointerBufferSupport.positionByte(buffer));
        assertEquals(4 * Pointer.POINTER_SIZE, PointerBufferSupport.remainingByte(buffer));
        buffer.free();
    }

    @Test
    void backingBufferAliasesSameMemory() {
        PointerBuffer buffer = PointerBuffer.allocateDirect(2);
        buffer.put(0, 0xCAFEBABEL);
        ByteBuffer bytes = PointerBufferSupport.getBuffer(buffer);
        assertEquals(2 * Pointer.POINTER_SIZE, bytes.capacity(), "Should span the whole buffer");
        assertEquals(ByteOrder.nativeOrder(), bytes.order());
        assertEquals(0xCAFEBABEL, Pointer.POINTER_SIZE == 8 ? bytes.getLong(0) : bytes.getInt(0) & 0xFFFFFFFFL);
        buffer.free();
    }

    @Test
    void readOnlyViewIsADuplicate() {
        PointerBuffer buffer = PointerBuffer.allocateDirect(2);
        buffer.put(0, 42L);
        PointerBuffer view = PointerBufferSupport.asReadOnlyBuffer(buffer);
        assertNotSame(buffer, view);
        assertEquals(42L, view.get(0), "The view has to see the same memory");
        // LWJGL 3 has no read-only CustomBuffer view
        assertFalse(PointerBufferSupport.isReadOnly(view));
        buffer.free();
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
        try (InputStream in = PointerBufferAdapterTest.class.getResourceAsStream("/" + internalName + ".class")) {
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

}
