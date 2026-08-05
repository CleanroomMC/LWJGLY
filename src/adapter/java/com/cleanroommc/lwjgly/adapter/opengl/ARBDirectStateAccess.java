package com.cleanroommc.lwjgly.adapter.opengl;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL15;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class ARBDirectStateAccess {

    public static void glVertexArrayVertexBuffers(int vaobj, int first, int count, IntBuffer buffers,
                                                  PointerBuffer offsets, IntBuffer strides) {
        org.lwjgl.opengl.ARBDirectStateAccess.nglVertexArrayVertexBuffers(vaobj, first, count,
                MemoryUtil.memAddressSafe(buffers), MemoryUtil.memAddressSafe(offsets), MemoryUtil.memAddressSafe(strides));
    }

    public static void glClearNamedFramebufferfi(int framebuffer, int buffer, float depth, int stencil) {
        org.lwjgl.opengl.ARBDirectStateAccess.glClearNamedFramebufferfi(framebuffer, buffer, 0, depth, stencil);
    }

    public static ByteBuffer glGetNamedBufferPointer(int buffer, int pname) {
        long address = org.lwjgl.opengl.ARBDirectStateAccess.glGetNamedBufferPointer(buffer, pname);
        if (address == MemoryUtil.NULL) {
            return null;
        }
        return MemoryUtil.memByteBufferSafe(address, org.lwjgl.opengl.ARBDirectStateAccess.glGetNamedBufferParameteri(buffer, GL15.GL_BUFFER_SIZE));
    }

    private ARBDirectStateAccess() { }

}
