package com.cleanroommc.lwjgly.adapter.opengl;

import org.lwjgl.opengl.GLSync;

public final class GL32 {

    public static int glGetSync(GLSync sync, int pname) {
        return org.lwjgl.opengl.GL32.glGetSynci(sync.getPointer(), pname, null);
    }

    public static int glGetSynci(GLSync sync, int pname) {
        return org.lwjgl.opengl.GL32.glGetSynci(sync.getPointer(), pname, null);
    }

    private GL32() { }

}
