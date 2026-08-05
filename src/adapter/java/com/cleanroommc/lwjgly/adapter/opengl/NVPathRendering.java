package com.cleanroommc.lwjgly.adapter.opengl;

public final class NVPathRendering {

    public static void glPathStencilDepthOffsetNV(float factor, int units) {
        org.lwjgl.opengl.NVPathRendering.glPathStencilDepthOffsetNV(factor, units);
    }

    private NVPathRendering() { }

}
