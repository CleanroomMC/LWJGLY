package com.cleanroommc.lwjgly.adapter.opengl;

import org.lwjgl.opengl.GL11;

public final class EXTVertexAttrib64bit {

    public static void glVertexAttribLPointerEXT(int index, int size, int stride, long pointerBufferOffset) {
        org.lwjgl.opengl.EXTVertexAttrib64bit.glVertexAttribLPointerEXT(index, size, GL11.GL_DOUBLE, stride, pointerBufferOffset);
    }

    private EXTVertexAttrib64bit() { }

}
