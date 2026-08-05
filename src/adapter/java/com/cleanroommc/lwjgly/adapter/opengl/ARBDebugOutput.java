package com.cleanroommc.lwjgly.adapter.opengl;

import com.cleanroommc.lwjgly.rt.DebugCallbacks;
import org.lwjgl.opengl.ARBDebugOutputCallback;
import org.lwjgl.opengl.GLDebugMessageARBCallback;

public final class ARBDebugOutput {

    public static void glDebugMessageCallbackARB(ARBDebugOutputCallback callback) {
        GLDebugMessageARBCallback previous = DebugCallbacks.arb(callback);
        org.lwjgl.opengl.ARBDebugOutput.glDebugMessageCallbackARB(DebugCallbacks.installedArb(), 0L);
        DebugCallbacks.release(previous);
    }

    private ARBDebugOutput() { }

}
