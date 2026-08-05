package com.cleanroommc.lwjgly.adapter.opengl;

import com.cleanroommc.lwjgly.rt.SizeTypePair;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class NVTransformFeedback {

    public static int glGetActiveVaryingSizeNV(int program, int index) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer size = stack.mallocInt(1);
            IntBuffer type = stack.mallocInt(1);
            ByteBuffer name = stack.malloc(1);
            org.lwjgl.opengl.NVTransformFeedback.nglGetActiveVaryingNV(program, index, 1, 0L, MemoryUtil.memAddress(size),
                    MemoryUtil.memAddress(type), MemoryUtil.memAddress(name));
            return size.get(0);
        }
    }

    public static int glGetActiveVaryingTypeNV(int program, int index) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer size = stack.mallocInt(1);
            IntBuffer type = stack.mallocInt(1);
            ByteBuffer name = stack.malloc(1);
            org.lwjgl.opengl.NVTransformFeedback.nglGetActiveVaryingNV(program, index, 1, 0L, MemoryUtil.memAddress(size),
                    MemoryUtil.memAddress(type), MemoryUtil.memAddress(name));
            return type.get(0);
        }
    }

    public static String glGetActiveVaryingNV(int program, int index, int bufSize, IntBuffer sizeType) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer length = stack.mallocInt(1);
            ByteBuffer name = stack.malloc(bufSize);
            org.lwjgl.opengl.NVTransformFeedback.glGetActiveVaryingNV(program, index, length, SizeTypePair.size(sizeType), SizeTypePair.type(sizeType), name);
            return MemoryUtil.memASCII(name, length.get(0));
        }
    }

    public static String glGetActiveVaryingNV(int program, int index, int bufSize) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer length = stack.mallocInt(1);
            ByteBuffer name = stack.malloc(bufSize);
            org.lwjgl.opengl.NVTransformFeedback.glGetActiveVaryingNV(program, index, length, stack.mallocInt(1), stack.mallocInt(1), name);
            return MemoryUtil.memASCII(name, length.get(0));
        }
    }

    private NVTransformFeedback() { }

}
