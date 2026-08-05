package com.cleanroommc.lwjgly.adapter.opengl;

import org.lwjgl.system.MemoryUtil;

import java.nio.Buffer;

public final class APPLETextureRange {

    public static Buffer glGetTexParameterPointervAPPLE(int target, int pname, long result_size) {
        if (result_size < 0 || result_size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("result_size out of range for a Buffer: " + result_size);
        }
        long address = org.lwjgl.opengl.APPLETextureRange.glGetTexParameterPointervAPPLE(target, pname);
        if (address == MemoryUtil.NULL) {
            return null;
        }
        return MemoryUtil.memByteBufferSafe(address, (int) result_size);
    }

    private APPLETextureRange() { }

}
