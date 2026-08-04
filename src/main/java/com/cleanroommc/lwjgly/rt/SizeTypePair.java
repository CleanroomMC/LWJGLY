package com.cleanroommc.lwjgly.rt;

import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

/**
 * Aliases LWJGL 2's shared size/type buffer as two one-element views. The source buffer must be direct.
 */
public final class SizeTypePair {

    public static IntBuffer size(IntBuffer sizeType) {
        return MemoryUtil.memIntBuffer(MemoryUtil.memAddress(sizeType), 1);
    }

    public static IntBuffer type(IntBuffer sizeType) {
        return MemoryUtil.memIntBuffer(MemoryUtil.memAddress(sizeType, sizeType.position() + 1), 1);
    }

    private SizeTypePair() { }

}
