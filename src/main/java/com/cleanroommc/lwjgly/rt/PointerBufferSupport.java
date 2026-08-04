package com.cleanroommc.lwjgly.rt;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Implements LWJGL 2 PointerBuffer members on top of LWJGL 3.
 * Adapter receivers arrive as Object because the merger rewrites their owner after compilation.
 */
public final class PointerBufferSupport {

    public static ByteBuffer getBuffer(Object self) {
        PointerBuffer buffer = i(self);
        return MemoryUtil.memByteBuffer(buffer.address0(), buffer.capacity() * Pointer.POINTER_SIZE).order(ByteOrder.nativeOrder());
    }

    public static int positionByte(Object self) {
        return i(self).position() * Pointer.POINTER_SIZE;
    }

    public static int remainingByte(Object self) {
        return i(self).remaining() * Pointer.POINTER_SIZE;
    }

    public static ByteOrder order(Object self) {
        return ByteOrder.nativeOrder();
    }

    public static PointerBuffer asReadOnlyBuffer(Object self) {
        return i(self).duplicate();
    }

    public static boolean isReadOnly(Object self) {
        return false;
    }

    public static PointerBuffer newInstance(Object self, ByteBuffer source) {
        return PointerBuffer.create(source);
    }

    public static PointerBuffer position(Object self, int newPosition) {
        return i(self).position(newPosition);
    }

    public static PointerBuffer limit(Object self, int newLimit) {
        return i(self).limit(newLimit);
    }

    public static PointerBuffer mark(Object self) {
        return i(self).mark();
    }

    public static PointerBuffer reset(Object self) {
        return i(self).reset();
    }

    public static PointerBuffer clear(Object self) {
        return i(self).clear();
    }

    public static PointerBuffer flip(Object self) {
        return i(self).flip();
    }

    public static PointerBuffer rewind(Object self) {
        return i(self).rewind();
    }

    public static PointerBuffer compact(Object self) {
        return i(self).compact();
    }

    public static PointerBuffer slice(Object self) {
        return i(self).slice();
    }

    public static PointerBuffer duplicate(Object self) {
        return i(self).duplicate();
    }

    public static PointerBuffer put(Object self, PointerBuffer source) {
        PointerBuffer buffer = i(self);
        int transferred = source.remaining();
        buffer.put(source);
        source.position(source.position() + transferred);
        return buffer;
    }

    private static PointerBuffer i(Object self) {
        return (PointerBuffer) self;
    }

    private PointerBufferSupport() { }

}
