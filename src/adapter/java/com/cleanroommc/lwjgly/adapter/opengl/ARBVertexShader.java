package com.cleanroommc.lwjgly.adapter.opengl;

import com.cleanroommc.lwjgly.rt.SizeTypePair;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public final class ARBVertexShader {

    public static int glGetActiveAttribSizeARB(int programObj, int index) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer size = stack.mallocInt(1);
            IntBuffer type = stack.mallocInt(1);
            ByteBuffer name = stack.malloc(1);
            org.lwjgl.opengl.ARBVertexShader.nglGetActiveAttribARB(programObj, index, 1, 0L, MemoryUtil.memAddress(size),
                    MemoryUtil.memAddress(type), MemoryUtil.memAddress(name));
            return size.get(0);
        }
    }

    public static int glGetActiveAttribTypeARB(int programObj, int index) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer size = stack.mallocInt(1);
            IntBuffer type = stack.mallocInt(1);
            ByteBuffer name = stack.malloc(1);
            org.lwjgl.opengl.ARBVertexShader.nglGetActiveAttribARB(programObj, index, 1, 0L, MemoryUtil.memAddress(size),
                    MemoryUtil.memAddress(type), MemoryUtil.memAddress(name));
            return type.get(0);
        }
    }

    public static String glGetActiveAttribARB(int programObj, int index, int maxLength, IntBuffer sizeType) {
        return org.lwjgl.opengl.ARBVertexShader.glGetActiveAttribARB(programObj, index, maxLength, SizeTypePair.size(sizeType), SizeTypePair.type(sizeType));
    }

    public static String glGetActiveAttribARB(int programObj, int index, int maxLength) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            return org.lwjgl.opengl.ARBVertexShader.glGetActiveAttribARB(programObj, index, maxLength, stack.mallocInt(1), stack.mallocInt(1));
        }
    }

    public static void glVertexAttribPointerARB(int index, int size, boolean unsigned, boolean normalized, int stride, ByteBuffer buffer) {
        org.lwjgl.opengl.ARBVertexShader.glVertexAttribPointerARB(index, size, unsigned ? GL11.GL_UNSIGNED_BYTE : GL11.GL_BYTE, normalized, stride, buffer);
    }

    public static void glVertexAttribPointerARB(int index, int size, boolean unsigned, boolean normalized, int stride, ShortBuffer buffer) {
        org.lwjgl.opengl.ARBVertexShader.glVertexAttribPointerARB(index, size, unsigned ? GL11.GL_UNSIGNED_SHORT : GL11.GL_SHORT, normalized, stride, buffer);
    }

    public static void glVertexAttribPointerARB(int index, int size, boolean unsigned, boolean normalized, int stride, IntBuffer buffer) {
        org.lwjgl.opengl.ARBVertexShader.glVertexAttribPointerARB(index, size, unsigned ? GL11.GL_UNSIGNED_INT : GL11.GL_INT, normalized, stride, buffer);
    }

    public static ByteBuffer glGetVertexAttribPointerARB(int index, int pname, long resultSize) {
        return MemoryUtil.memByteBufferSafe(org.lwjgl.opengl.ARBVertexShader.glGetVertexAttribPointerARB(index, pname), (int) resultSize);
    }

    private ARBVertexShader() { }

}
