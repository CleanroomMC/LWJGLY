package org.lwjgl;

import org.lwjgl.system.Pointer;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * LWJGL 2 address and string helpers backed by LWJGL 3 MemoryUtil.
 * {@code address0} methods ignore buffer position, and encoded strings use GC'd storage.
 */
public final class MemoryUtil {

    /*
      The buffer address without its position
    */

    public static long getAddress0(Buffer buffer) {
        return org.lwjgl.system.MemoryUtil.memAddress0(buffer);
    }

    public static long getAddress0Safe(Buffer buffer) {
        return buffer == null ? 0L : getAddress0(buffer);
    }

    public static long getAddress0(PointerBuffer buffer) {
        return buffer.address0();
    }

    public static long getAddress0Safe(PointerBuffer buffer) {
        return buffer == null ? 0L : buffer.address0();
    }

    /*
      The current or indexed element address
    */

    public static long getAddress(ByteBuffer buffer) {
        return org.lwjgl.system.MemoryUtil.memAddress(buffer);
    }

    public static long getAddress(ByteBuffer buffer, int position) {
        return org.lwjgl.system.MemoryUtil.memAddress(buffer, position);
    }

    public static long getAddress(ShortBuffer buffer) {
        return org.lwjgl.system.MemoryUtil.memAddress(buffer);
    }

    public static long getAddress(ShortBuffer buffer, int position) {
        return org.lwjgl.system.MemoryUtil.memAddress(buffer, position);
    }

    public static long getAddress(CharBuffer buffer) {
        return org.lwjgl.system.MemoryUtil.memAddress(buffer);
    }

    public static long getAddress(CharBuffer buffer, int position) {
        return org.lwjgl.system.MemoryUtil.memAddress(buffer, position);
    }

    public static long getAddress(IntBuffer buffer) {
        return org.lwjgl.system.MemoryUtil.memAddress(buffer);
    }

    public static long getAddress(IntBuffer buffer, int position) {
        return org.lwjgl.system.MemoryUtil.memAddress(buffer, position);
    }

    public static long getAddress(FloatBuffer buffer) {
        return org.lwjgl.system.MemoryUtil.memAddress(buffer);
    }

    public static long getAddress(FloatBuffer buffer, int position) {
        return org.lwjgl.system.MemoryUtil.memAddress(buffer, position);
    }

    public static long getAddress(LongBuffer buffer) {
        return org.lwjgl.system.MemoryUtil.memAddress(buffer);
    }

    public static long getAddress(LongBuffer buffer, int position) {
        return org.lwjgl.system.MemoryUtil.memAddress(buffer, position);
    }

    public static long getAddress(DoubleBuffer buffer) {
        return org.lwjgl.system.MemoryUtil.memAddress(buffer);
    }

    public static long getAddress(DoubleBuffer buffer, int position) {
        return org.lwjgl.system.MemoryUtil.memAddress(buffer, position);
    }

    public static long getAddress(PointerBuffer buffer) {
        return getAddress(buffer, buffer.position());
    }

    public static long getAddress(PointerBuffer buffer, int position) {
        return buffer.address0() + (long) position * Pointer.POINTER_SIZE;
    }

    /*
      Null-safe forms used for optional out-parameters
    */

    public static long getAddressSafe(ByteBuffer buffer) {
        return buffer == null ? 0L : getAddress(buffer);
    }

    public static long getAddressSafe(ByteBuffer buffer, int position) {
        return buffer == null ? 0L : getAddress(buffer, position);
    }

    public static long getAddressSafe(ShortBuffer buffer) {
        return buffer == null ? 0L : getAddress(buffer);
    }

    public static long getAddressSafe(ShortBuffer buffer, int position) {
        return buffer == null ? 0L : getAddress(buffer, position);
    }

    public static long getAddressSafe(CharBuffer buffer) {
        return buffer == null ? 0L : getAddress(buffer);
    }

    public static long getAddressSafe(CharBuffer buffer, int position) {
        return buffer == null ? 0L : getAddress(buffer, position);
    }

    public static long getAddressSafe(IntBuffer buffer) {
        return buffer == null ? 0L : getAddress(buffer);
    }

    public static long getAddressSafe(IntBuffer buffer, int position) {
        return buffer == null ? 0L : getAddress(buffer, position);
    }

    public static long getAddressSafe(FloatBuffer buffer) {
        return buffer == null ? 0L : getAddress(buffer);
    }

    public static long getAddressSafe(FloatBuffer buffer, int position) {
        return buffer == null ? 0L : getAddress(buffer, position);
    }

    public static long getAddressSafe(LongBuffer buffer) {
        return buffer == null ? 0L : getAddress(buffer);
    }

    public static long getAddressSafe(LongBuffer buffer, int position) {
        return buffer == null ? 0L : getAddress(buffer, position);
    }

    public static long getAddressSafe(DoubleBuffer buffer) {
        return buffer == null ? 0L : getAddress(buffer);
    }

    public static long getAddressSafe(DoubleBuffer buffer, int position) {
        return buffer == null ? 0L : getAddress(buffer, position);
    }

    public static long getAddressSafe(PointerBuffer buffer) {
        return buffer == null ? 0L : getAddress(buffer);
    }

    public static long getAddressSafe(PointerBuffer buffer, int position) {
        return buffer == null ? 0L : getAddress(buffer, position);
    }

    /*
      Encoded buffers include a trailing null and start at position zero
    */

    public static ByteBuffer encodeASCII(CharSequence text) {
        if (text == null) {
            return null;
        }
        ByteBuffer target = BufferUtils.createByteBuffer(org.lwjgl.system.MemoryUtil.memLengthASCII(text, true));
        org.lwjgl.system.MemoryUtil.memASCII(text, true, target);
        return target;
    }

    public static ByteBuffer encodeUTF8(CharSequence text) {
        if (text == null) {
            return null;
        }
        ByteBuffer target = BufferUtils.createByteBuffer(org.lwjgl.system.MemoryUtil.memLengthUTF8(text, true));
        org.lwjgl.system.MemoryUtil.memUTF8(text, true, target);
        return target;
    }

    public static ByteBuffer encodeUTF16(CharSequence text) {
        if (text == null) {
            return null;
        }
        ByteBuffer target = BufferUtils.createByteBuffer(org.lwjgl.system.MemoryUtil.memLengthUTF16(text, true));
        org.lwjgl.system.MemoryUtil.memUTF16(text, true, target);
        return target;
    }

    public static String decodeASCII(ByteBuffer buffer) {
        return org.lwjgl.system.MemoryUtil.memASCII(buffer);
    }

    public static String decodeUTF8(ByteBuffer buffer) {
        return org.lwjgl.system.MemoryUtil.memUTF8(buffer);
    }

    public static String decodeUTF16(ByteBuffer buffer) {
        return org.lwjgl.system.MemoryUtil.memUTF16(buffer);
    }

    private MemoryUtil() { }

}
