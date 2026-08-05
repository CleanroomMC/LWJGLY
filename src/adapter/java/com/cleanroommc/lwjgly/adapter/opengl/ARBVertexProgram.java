package com.cleanroommc.lwjgly.adapter.opengl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public final class ARBVertexProgram {

    public static void glVertexAttribPointerARB(int index, int size, boolean unsigned, boolean normalized, int stride, ByteBuffer buffer) {
        org.lwjgl.opengl.ARBVertexProgram.glVertexAttribPointerARB(index, size, unsigned ? GL11.GL_UNSIGNED_BYTE : GL11.GL_BYTE, normalized, stride, buffer);
    }

    public static void glVertexAttribPointerARB(int index, int size, boolean unsigned, boolean normalized, int stride, ShortBuffer buffer) {
        org.lwjgl.opengl.ARBVertexProgram.glVertexAttribPointerARB(index, size, unsigned ? GL11.GL_UNSIGNED_SHORT : GL11.GL_SHORT, normalized, stride, buffer);
    }

    public static void glVertexAttribPointerARB(int index, int size, boolean unsigned, boolean normalized, int stride, IntBuffer buffer) {
        org.lwjgl.opengl.ARBVertexProgram.glVertexAttribPointerARB(index, size, unsigned ? GL11.GL_UNSIGNED_INT : GL11.GL_INT, normalized, stride, buffer);
    }

    public static ByteBuffer glGetVertexAttribPointerARB(int index, int pname, long resultSize) {
        return MemoryUtil.memByteBufferSafe(org.lwjgl.opengl.ARBVertexProgram.glGetVertexAttribPointerARB(index, pname), (int) resultSize);
    }

    private ARBVertexProgram() { }

}
