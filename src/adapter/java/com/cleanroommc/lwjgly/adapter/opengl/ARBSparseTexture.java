package com.cleanroommc.lwjgly.adapter.opengl;

public final class ARBSparseTexture {

    public static void glTexturePageCommitmentEXT(int texture, int target, int level, int xoffset, int yoffset,
                                                  int zoffset, int width, int height, int depth, boolean commit) {
        org.lwjgl.opengl.ARBSparseTexture.glTexturePageCommitmentEXT(texture, level, xoffset, yoffset, zoffset, width, height, depth, commit);
    }

    private ARBSparseTexture() { }

}
