package com.cleanroommc.lwjgly.adapter.opengl;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public final class GL11 {

    public static void glColorPointer(int size, boolean unsigned, int stride, ByteBuffer buffer) {
        org.lwjgl.opengl.GL11.glColorPointer(size, unsigned ? org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE : org.lwjgl.opengl.GL11.GL_BYTE, stride, buffer);
    }

    public static ByteBuffer glGetPointer(int pname, long resultSize) {
        return MemoryUtil.memByteBufferSafe(org.lwjgl.opengl.GL11.glGetPointer(pname), (int) resultSize);
    }

    private GL11() { }

}
