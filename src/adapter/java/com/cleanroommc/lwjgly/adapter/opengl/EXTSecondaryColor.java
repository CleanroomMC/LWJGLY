package com.cleanroommc.lwjgly.adapter.opengl;

import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

public final class EXTSecondaryColor {

    public static void glSecondaryColorPointerEXT(int size, boolean unsigned, int stride, ByteBuffer buffer) {
        org.lwjgl.opengl.EXTSecondaryColor.glSecondaryColorPointerEXT(size, unsigned ? GL11.GL_UNSIGNED_BYTE : GL11.GL_BYTE, stride, buffer);
    }

    private EXTSecondaryColor() { }

}
