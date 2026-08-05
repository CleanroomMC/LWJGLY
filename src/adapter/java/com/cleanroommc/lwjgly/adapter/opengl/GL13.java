package com.cleanroommc.lwjgly.adapter.opengl;

public final class GL13 {

    public static void glCompressedTexImage1D(int target, int level, int internalformat, int width, int border, int imageSize) {
        org.lwjgl.opengl.GL13.nglCompressedTexImage1D(target, level, internalformat, width, border, imageSize, 0L);
    }

    public static void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, int imageSize) {
        org.lwjgl.opengl.GL13.nglCompressedTexImage2D(target, level, internalformat, width, height, border, imageSize, 0L);
    }

    public static void glCompressedTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int imageSize) {
        org.lwjgl.opengl.GL13.nglCompressedTexImage3D(target, level, internalformat, width, height, depth, border, imageSize, 0L);
    }

    private GL13() { }

}
