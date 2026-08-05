package com.cleanroommc.lwjgly.adapter.opengl;

import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public final class ARBVertexBlend {

    public static void glWeightPointerARB(int size, boolean unsigned, int stride, ByteBuffer buffer) {
        org.lwjgl.opengl.ARBVertexBlend.glWeightPointerARB(size, unsigned ? GL11.GL_UNSIGNED_BYTE : GL11.GL_BYTE, stride, buffer);
    }

    public static void glWeightPointerARB(int size, boolean unsigned, int stride, ShortBuffer buffer) {
        org.lwjgl.opengl.ARBVertexBlend.glWeightPointerARB(size, unsigned ? GL11.GL_UNSIGNED_SHORT : GL11.GL_SHORT, stride, buffer);
    }

    public static void glWeightPointerARB(int size, boolean unsigned, int stride, IntBuffer buffer) {
        org.lwjgl.opengl.ARBVertexBlend.glWeightPointerARB(size, unsigned ? GL11.GL_UNSIGNED_INT : GL11.GL_INT, stride, buffer);
    }

    private ARBVertexBlend() { }

}
