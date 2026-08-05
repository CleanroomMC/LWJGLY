package com.cleanroommc.lwjgly.adapter.opengl;

import org.lwjgl.opengl.EXTDrawBuffers2;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class NVExplicitMultisample {

    public static void glGetBooleanIndexedEXT(int pname, int index, ByteBuffer data) {
        EXTDrawBuffers2.glGetBooleanIndexedvEXT(pname, index, data);
    }

    public static boolean glGetBooleanIndexedEXT(int pname, int index) {
        return EXTDrawBuffers2.glGetBooleanIndexedEXT(pname, index);
    }

    public static void glGetIntegerIndexedEXT(int pname, int index, IntBuffer data) {
        EXTDrawBuffers2.glGetIntegerIndexedvEXT(pname, index, data);
    }

    public static int glGetIntegerIndexedEXT(int pname, int index) {
        return EXTDrawBuffers2.glGetIntegerIndexedEXT(pname, index);
    }

    private NVExplicitMultisample() { }

}
