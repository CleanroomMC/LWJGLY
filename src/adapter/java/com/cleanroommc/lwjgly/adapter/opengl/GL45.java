package com.cleanroommc.lwjgly.adapter.opengl;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL15;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class GL45 {

    public static void glVertexArrayVertexBuffers(int vaobj, int first, int count, IntBuffer buffers, PointerBuffer offsets, IntBuffer strides) {
        org.lwjgl.opengl.GL45.nglVertexArrayVertexBuffers(vaobj, first, count, MemoryUtil.memAddressSafe(buffers),
                MemoryUtil.memAddressSafe(offsets), MemoryUtil.memAddressSafe(strides));
    }

    public static void glClearNamedFramebufferfi(int framebuffer, int buffer, float depth, int stencil) {
        org.lwjgl.opengl.GL45.glClearNamedFramebufferfi(framebuffer, buffer, 0, depth, stencil);
    }

    public static ByteBuffer glGetNamedBufferPointer(int buffer, int pname) {
        long address = org.lwjgl.opengl.GL45.glGetNamedBufferPointer(buffer, pname);
        if (address == MemoryUtil.NULL) {
            return null;
        }
        return MemoryUtil.memByteBufferSafe(address, org.lwjgl.opengl.GL45.glGetNamedBufferParameteri(buffer, GL15.GL_BUFFER_SIZE));
    }

    private GL45() { }

}
