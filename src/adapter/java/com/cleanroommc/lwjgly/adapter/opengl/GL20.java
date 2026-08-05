package com.cleanroommc.lwjgly.adapter.opengl;

import com.cleanroommc.lwjgly.rt.SizeTypePair;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public final class GL20 {

    public static void glShaderSource(int shader, ByteBuffer string) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer strings = stack.pointers(MemoryUtil.memAddress(string));
            IntBuffer lengths = stack.ints(string.remaining());
            org.lwjgl.opengl.GL20.glShaderSource(shader, strings, lengths);
        }
    }

    public static int glGetActiveUniformSize(int program, int index) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer size = stack.mallocInt(1);
            IntBuffer type = stack.mallocInt(1);
            ByteBuffer name = stack.malloc(1);
            org.lwjgl.opengl.GL20.nglGetActiveUniform(program, index, 1, 0L, MemoryUtil.memAddress(size),
                    MemoryUtil.memAddress(type), MemoryUtil.memAddress(name));
            return size.get(0);
        }
    }

    public static int glGetActiveUniformType(int program, int index) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer size = stack.mallocInt(1);
            IntBuffer type = stack.mallocInt(1);
            ByteBuffer name = stack.malloc(1);
            org.lwjgl.opengl.GL20.nglGetActiveUniform(program, index, 1, 0L, MemoryUtil.memAddress(size),
                    MemoryUtil.memAddress(type), MemoryUtil.memAddress(name));
            return type.get(0);
        }
    }

    public static int glGetActiveAttribSize(int program, int index) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer size = stack.mallocInt(1);
            IntBuffer type = stack.mallocInt(1);
            ByteBuffer name = stack.malloc(1);
            org.lwjgl.opengl.GL20.nglGetActiveAttrib(program, index, 1, 0L, MemoryUtil.memAddress(size),
                    MemoryUtil.memAddress(type), MemoryUtil.memAddress(name));
            return size.get(0);
        }
    }

    public static int glGetActiveAttribType(int program, int index) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer size = stack.mallocInt(1);
            IntBuffer type = stack.mallocInt(1);
            ByteBuffer name = stack.malloc(1);
            org.lwjgl.opengl.GL20.nglGetActiveAttrib(program, index, 1, 0L, MemoryUtil.memAddress(size),
                    MemoryUtil.memAddress(type), MemoryUtil.memAddress(name));
            return type.get(0);
        }
    }

    public static String glGetActiveUniform(int program, int index, int maxLength, IntBuffer sizeType) {
        return org.lwjgl.opengl.GL20.glGetActiveUniform(program, index, maxLength, SizeTypePair.size(sizeType), SizeTypePair.type(sizeType));
    }

    public static String glGetActiveUniform(int program, int index, int maxLength) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            return org.lwjgl.opengl.GL20.glGetActiveUniform(program, index, maxLength, stack.mallocInt(1), stack.mallocInt(1));
        }
    }

    public static String glGetActiveAttrib(int program, int index, int maxLength, IntBuffer sizeType) {
        return org.lwjgl.opengl.GL20.glGetActiveAttrib(program, index, maxLength, SizeTypePair.size(sizeType), SizeTypePair.type(sizeType));
    }

    public static String glGetActiveAttrib(int program, int index, int maxLength) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            return org.lwjgl.opengl.GL20.glGetActiveAttrib(program, index, maxLength, stack.mallocInt(1), stack.mallocInt(1));
        }
    }

    public static void glVertexAttribPointer(int index, int size, boolean unsigned, boolean normalized, int stride, ByteBuffer buffer) {
        org.lwjgl.opengl.GL20.glVertexAttribPointer(index, size, unsigned ? GL11.GL_UNSIGNED_BYTE : GL11.GL_BYTE, normalized, stride, buffer);
    }

    public static void glVertexAttribPointer(int index, int size, boolean unsigned, boolean normalized, int stride, ShortBuffer buffer) {
        org.lwjgl.opengl.GL20.glVertexAttribPointer(index, size, unsigned ? GL11.GL_UNSIGNED_SHORT : GL11.GL_SHORT, normalized, stride, buffer);
    }

    public static void glVertexAttribPointer(int index, int size, boolean unsigned, boolean normalized, int stride, IntBuffer buffer) {
        org.lwjgl.opengl.GL20.glVertexAttribPointer(index, size, unsigned ? GL11.GL_UNSIGNED_INT : GL11.GL_INT, normalized, stride, buffer);
    }

    public static ByteBuffer glGetVertexAttribPointer(int index, int pname, long resultSize) {
        return MemoryUtil.memByteBufferSafe(org.lwjgl.opengl.GL20.glGetVertexAttribPointer(index, pname), (int) resultSize);
    }

    private GL20() { }

}
