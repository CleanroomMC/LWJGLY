package com.cleanroommc.lwjgly.adapter.opengl;

import org.lwjgl.opengl.EXTDrawBuffers2;

import java.nio.IntBuffer;

public final class ARBViewportArray {

    public static void glGetIntegerIndexedEXT(int target, int index, IntBuffer data) {
        EXTDrawBuffers2.glGetIntegerIndexedvEXT(target, index, data);
    }

    public static int glGetIntegerIndexedEXT(int target, int index) {
        return EXTDrawBuffers2.glGetIntegerIndexedEXT(target, index);
    }

    public static void glEnableIndexedEXT(int target, int index) {
        EXTDrawBuffers2.glEnableIndexedEXT(target, index);
    }

    public static void glDisableIndexedEXT(int target, int index) {
        EXTDrawBuffers2.glDisableIndexedEXT(target, index);
    }

    public static boolean glIsEnabledIndexedEXT(int target, int index) {
        return EXTDrawBuffers2.glIsEnabledIndexedEXT(target, index);
    }

    private ARBViewportArray() { }

}
