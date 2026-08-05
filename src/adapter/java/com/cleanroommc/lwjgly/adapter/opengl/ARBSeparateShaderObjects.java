package com.cleanroommc.lwjgly.adapter.opengl;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public final class ARBSeparateShaderObjects {

    public static int glCreateShaderProgram(int type, ByteBuffer[] strings) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer sources = stack.mallocPointer(strings.length);
            for (int i = 0; i < strings.length; i++) {
                sources.put(i, MemoryUtil.memAddress(strings[i]));
            }
            return org.lwjgl.opengl.ARBSeparateShaderObjects.glCreateShaderProgramv(type, sources);
        }
    }

    private ARBSeparateShaderObjects() { }

}
