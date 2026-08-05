package com.cleanroommc.lwjgly.adapter.opengl;

import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

public final class GL14 {

    public static void glSecondaryColorPointer(int size, boolean unsigned, int stride, ByteBuffer buffer) {
        org.lwjgl.opengl.GL14.glSecondaryColorPointer(size, unsigned ? GL11.GL_UNSIGNED_BYTE : GL11.GL_BYTE, stride, buffer);
    }

    private GL14() { }

}
