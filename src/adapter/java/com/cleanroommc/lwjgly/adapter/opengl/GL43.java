package com.cleanroommc.lwjgly.adapter.opengl;

import com.cleanroommc.lwjgly.rt.DebugCallbacks;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.opengl.KHRDebugCallback;

public final class GL43 {

    public static void glDebugMessageCallback(KHRDebugCallback callback) {
        GLDebugMessageCallback previous = DebugCallbacks.core(callback);
        org.lwjgl.opengl.GL43.glDebugMessageCallback(DebugCallbacks.installedCore(), 0L);
        DebugCallbacks.release(previous);
    }

    private GL43() { }

}
