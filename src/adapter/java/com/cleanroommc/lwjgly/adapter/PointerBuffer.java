package com.cleanroommc.lwjgly.adapter;

import com.cleanroommc.lwjgly.rt.PointerBufferSupport;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class PointerBuffer {

    public static boolean is64Bit() {
        return Pointer.BITS64;
    }

    public static int getPointerSize() {
        return Pointer.POINTER_SIZE;
    }

    // Lets javac resolve constructor chaining
    private PointerBuffer(long address, ByteBuffer container, int mark, int position, int limit, int capacity) { }

    public PointerBuffer(int capacity) {
        this(BufferUtils.createByteBuffer(capacity * Pointer.POINTER_SIZE));
    }

    public PointerBuffer(ByteBuffer source) {
        this(MemoryUtil.memAddress(source), source, -1, 0, source.remaining() / Pointer.POINTER_SIZE, source.remaining() / Pointer.POINTER_SIZE);
    }

    public ByteBuffer getBuffer() {
        return PointerBufferSupport.getBuffer(this);
    }

    public int positionByte() {
        return PointerBufferSupport.positionByte(this);
    }

    public int remainingByte() {
        return PointerBufferSupport.remainingByte(this);
    }

    public ByteOrder order() {
        return PointerBufferSupport.order(this);
    }

    public boolean isReadOnly() {
        return PointerBufferSupport.isReadOnly(this);
    }

    public org.lwjgl.PointerBuffer asReadOnlyBuffer() {
        return PointerBufferSupport.asReadOnlyBuffer(this);
    }

    protected org.lwjgl.PointerBuffer newInstance(ByteBuffer source) {
        return PointerBufferSupport.newInstance(this, source);
    }

    public org.lwjgl.PointerBuffer position(int newPosition) {
        return PointerBufferSupport.position(this, newPosition);
    }

    public org.lwjgl.PointerBuffer limit(int newLimit) {
        return PointerBufferSupport.limit(this, newLimit);
    }

    public org.lwjgl.PointerBuffer mark() {
        return PointerBufferSupport.mark(this);
    }

    public org.lwjgl.PointerBuffer reset() {
        return PointerBufferSupport.reset(this);
    }

    public org.lwjgl.PointerBuffer clear() {
        return PointerBufferSupport.clear(this);
    }

    public org.lwjgl.PointerBuffer flip() {
        return PointerBufferSupport.flip(this);
    }

    public org.lwjgl.PointerBuffer rewind() {
        return PointerBufferSupport.rewind(this);
    }

    public org.lwjgl.PointerBuffer compact() {
        return PointerBufferSupport.compact(this);
    }

    public org.lwjgl.PointerBuffer slice() {
        return PointerBufferSupport.slice(this);
    }

    public org.lwjgl.PointerBuffer duplicate() {
        return PointerBufferSupport.duplicate(this);
    }

    public org.lwjgl.PointerBuffer put(org.lwjgl.PointerBuffer source) {
        return PointerBufferSupport.put(this, source);
    }

}
