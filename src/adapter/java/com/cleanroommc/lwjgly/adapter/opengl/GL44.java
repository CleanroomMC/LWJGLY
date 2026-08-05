package com.cleanroommc.lwjgly.adapter.opengl;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

public final class GL44 {

    public static void glBindBuffersRange(int target, int first, int count, IntBuffer buffers, PointerBuffer offsets, PointerBuffer sizes) {
        org.lwjgl.opengl.GL44.nglBindBuffersRange(target, first, count,
                MemoryUtil.memAddressSafe(buffers),
                MemoryUtil.memAddressSafe(offsets),
                MemoryUtil.memAddressSafe(sizes));
    }

    public static void glBindVertexBuffers(int first, int count, IntBuffer buffers, PointerBuffer offsets, IntBuffer strides) {
        org.lwjgl.opengl.GL44.nglBindVertexBuffers(first, count,
                MemoryUtil.memAddressSafe(buffers),
                MemoryUtil.memAddressSafe(offsets),
                MemoryUtil.memAddressSafe(strides));
    }

    private GL44() { }

}
