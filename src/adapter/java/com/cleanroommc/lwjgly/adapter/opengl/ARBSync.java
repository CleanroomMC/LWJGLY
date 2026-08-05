package com.cleanroommc.lwjgly.adapter.opengl;

import org.lwjgl.opengl.GLSync;

public final class ARBSync {

    public static int glGetSync(GLSync sync, int pname) {
        return org.lwjgl.opengl.ARBSync.glGetSynci(sync.getPointer(), pname, null);
    }

    public static int glGetSynci(GLSync sync, int pname) {
        return org.lwjgl.opengl.ARBSync.glGetSynci(sync.getPointer(), pname, null);
    }

    private ARBSync() { }

}
