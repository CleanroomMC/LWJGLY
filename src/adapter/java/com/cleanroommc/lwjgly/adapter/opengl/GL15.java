package com.cleanroommc.lwjgly.adapter.opengl;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public final class GL15 {

    public static ByteBuffer glGetBufferPointer(int target, int pname) {
        return MemoryUtil.memByteBufferSafe(org.lwjgl.opengl.GL15.glGetBufferPointer(target, pname),
                org.lwjgl.opengl.GL15.glGetBufferParameteri(target, org.lwjgl.opengl.GL15.GL_BUFFER_SIZE));
    }

    private GL15() { }

}
