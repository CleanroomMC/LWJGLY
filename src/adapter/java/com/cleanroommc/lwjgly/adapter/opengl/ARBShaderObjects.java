package com.cleanroommc.lwjgly.adapter.opengl;

import com.cleanroommc.lwjgly.rt.SizeTypePair;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class ARBShaderObjects {

    public static void glShaderSourceARB(int shader, ByteBuffer string) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer strings = stack.pointers(MemoryUtil.memAddress(string));
            IntBuffer lengths = stack.ints(string.remaining());
            org.lwjgl.opengl.ARBShaderObjects.glShaderSourceARB(shader, strings, lengths);
        }
    }

    public static int glGetActiveUniformSizeARB(int programObj, int index) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer size = stack.mallocInt(1);
            IntBuffer type = stack.mallocInt(1);
            ByteBuffer name = stack.malloc(1);
            org.lwjgl.opengl.ARBShaderObjects.nglGetActiveUniformARB(programObj, index, 1, 0L, MemoryUtil.memAddress(size),
                    MemoryUtil.memAddress(type), MemoryUtil.memAddress(name));
            return size.get(0);
        }
    }

    public static int glGetActiveUniformTypeARB(int programObj, int index) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer size = stack.mallocInt(1);
            IntBuffer type = stack.mallocInt(1);
            ByteBuffer name = stack.malloc(1);
            org.lwjgl.opengl.ARBShaderObjects.nglGetActiveUniformARB(programObj, index, 1, 0L, MemoryUtil.memAddress(size),
                    MemoryUtil.memAddress(type), MemoryUtil.memAddress(name));
            return type.get(0);
        }
    }

    public static String glGetActiveUniformARB(int programObj, int index, int maxLength, IntBuffer sizeType) {
        return org.lwjgl.opengl.ARBShaderObjects.glGetActiveUniformARB(programObj, index, maxLength, SizeTypePair.size(sizeType), SizeTypePair.type(sizeType));
    }

    public static String glGetActiveUniformARB(int programObj, int index, int maxLength) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            return org.lwjgl.opengl.ARBShaderObjects.glGetActiveUniformARB(programObj, index, maxLength, stack.mallocInt(1), stack.mallocInt(1));
        }
    }

    public static float glGetObjectParameterfARB(int obj, int pname) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            java.nio.FloatBuffer params = stack.mallocFloat(1);
            org.lwjgl.opengl.ARBShaderObjects.glGetObjectParameterfvARB(obj, pname, params);
            return params.get(0);
        }
    }

    private ARBShaderObjects() { }

}
