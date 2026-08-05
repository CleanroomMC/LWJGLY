package org.lwjgl;

import org.junit.jupiter.api.Test;
import org.lwjgl.system.Pointer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks address offsets and encoder allocation ownership.
 */
class MemoryUtilShimTest {

    @Test
    void getAddressCountsPositionAndGetAddress0DoesNot() {
        ByteBuffer bytes = BufferUtils.createByteBuffer(16);
        long base = MemoryUtil.getAddress0(bytes);
        assertEquals(base, MemoryUtil.getAddress(bytes), "At position 0 the two agree");
        bytes.position(4);
        assertEquals(base, MemoryUtil.getAddress0(bytes), "getAddress0 must ignore the position");
        assertEquals(base + 4, MemoryUtil.getAddress(bytes), "getAddress must add it");
        assertEquals(base + 8, MemoryUtil.getAddress(bytes, 8), "And the explicit index is absolute");
    }

    @Test
    void typedBuffersScaleOffsetByElementSize() {
        IntBuffer ints = BufferUtils.createIntBuffer(8);
        long base = MemoryUtil.getAddress0(ints);
        ints.position(2);
        assertEquals(base, MemoryUtil.getAddress0(ints));
        assertEquals(base + 2 * Integer.BYTES, MemoryUtil.getAddress(ints));
        assertEquals(base + 3 * Integer.BYTES, MemoryUtil.getAddress(ints, 3));
    }

    @Test
    void pointerBuffersScaleOffsetByPointerSize() {
        PointerBuffer pointers = BufferUtils.createPointerBuffer(4);
        long base = MemoryUtil.getAddress0(pointers);
        pointers.position(1);
        assertEquals(base, MemoryUtil.getAddress0(pointers));
        assertEquals(base + Pointer.POINTER_SIZE, MemoryUtil.getAddress(pointers));
        assertEquals(base + 2L * Pointer.POINTER_SIZE, MemoryUtil.getAddress(pointers, 2));
    }

    @Test
    void safeVariantsAnswerZeroForNull() {
        assertEquals(0L, MemoryUtil.getAddressSafe((ByteBuffer) null));
        assertEquals(0L, MemoryUtil.getAddressSafe((IntBuffer) null, 3));
        assertEquals(0L, MemoryUtil.getAddressSafe((PointerBuffer) null));
        assertEquals(0L, MemoryUtil.getAddress0Safe((java.nio.Buffer) null));
        assertEquals(0L, MemoryUtil.getAddress0Safe((PointerBuffer) null));
    }

    @Test
    void encodersReturnGarbageCollectedMemory() {
        ByteBuffer encoded = MemoryUtil.encodeASCII("abc");
        assertTrue(encoded.isDirect());
        // BufferUtils allocations have a Cleaner but memAlloc buffers do not
        ByteBuffer reference = BufferUtils.createByteBuffer(4);
        assertEquals(reference.getClass(), encoded.getClass(), "encodeASCII should allocate the way BufferUtils does, not with memAlloc");
    }

    @Test
    void encodersNullTerminateAndDecodersRoundTrip() {
        ByteBuffer ascii = MemoryUtil.encodeASCII("abc");
        assertEquals(0, ascii.position());
        assertEquals(4, ascii.limit(), "Three characters and a terminating null");
        assertEquals(0, ascii.get(3));
        assertEquals("abc", MemoryUtil.decodeASCII(ascii.duplicate().limit(3)));
        ByteBuffer utf8 = MemoryUtil.encodeUTF8("hé");
        assertEquals(4, utf8.limit(), "The e-acute takes two bytes, plus the null");
        assertEquals("hé", MemoryUtil.decodeUTF8(utf8.duplicate().limit(3)));
        ByteBuffer utf16 = MemoryUtil.encodeUTF16("ab");
        assertEquals(6, utf16.limit(), "Two code units and a null, two bytes each");
        assertEquals("ab", MemoryUtil.decodeUTF16(utf16.duplicate().limit(4)));
    }

    @Test
    void encodingNullGivesNull() {
        assertNull(MemoryUtil.encodeASCII(null));
        assertNull(MemoryUtil.encodeUTF8(null));
        assertNull(MemoryUtil.encodeUTF16(null));
    }

    @Test
    void eachEncodeAllocatesOwnBuffer() {
        ByteBuffer first = MemoryUtil.encodeASCII("abc");
        ByteBuffer second = MemoryUtil.encodeASCII("xyz");
        assertNotEquals(MemoryUtil.getAddress0(first), MemoryUtil.getAddress0(second));
        assertEquals('a', first.get(0));
    }

}
