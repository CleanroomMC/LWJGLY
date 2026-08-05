package com.cleanroommc.lwjgly.adapter.opengl;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public final class GL41 {

    public static int glCreateShaderProgram(int type, ByteBuffer[] strings) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer sources = stack.mallocPointer(strings.length);
            for (int i = 0; i < strings.length; i++) {
                sources.put(i, MemoryUtil.memAddress(strings[i]));
            }
            return org.lwjgl.opengl.GL41.glCreateShaderProgramv(type, sources);
        }
    }

    public static void glVertexAttribLPointer(int index, int size, int stride, long pointerBufferOffset) {
        org.lwjgl.opengl.GL41.glVertexAttribLPointer(index, size, GL11.GL_DOUBLE, stride, pointerBufferOffset);
    }

    private GL41() { }

}
