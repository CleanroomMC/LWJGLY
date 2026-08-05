package com.cleanroommc.lwjgly.adapter.opengl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class INTELMapTexture {

    public static ByteBuffer glMapTexture2DINTEL(int texture, int level, long length, int access, IntBuffer stride, IntBuffer layout, ByteBuffer oldBuffer) {
        return org.lwjgl.opengl.INTELMapTexture.glMapTexture2DINTEL(texture, level, access, stride, layout, length, oldBuffer);
    }

    private INTELMapTexture() { }

}
