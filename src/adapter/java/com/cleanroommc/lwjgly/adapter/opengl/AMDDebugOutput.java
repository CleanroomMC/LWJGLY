package com.cleanroommc.lwjgly.adapter.opengl;

import com.cleanroommc.lwjgly.rt.DebugCallbacks;
import org.lwjgl.opengl.AMDDebugOutputCallback;
import org.lwjgl.opengl.GLDebugMessageAMDCallback;

public final class AMDDebugOutput {

    public static void glDebugMessageCallbackAMD(AMDDebugOutputCallback callback) {
        GLDebugMessageAMDCallback previous = DebugCallbacks.amd(callback);
        org.lwjgl.opengl.AMDDebugOutput.glDebugMessageCallbackAMD(DebugCallbacks.installedAmd(), 0L);
        DebugCallbacks.release(previous);
    }

    private AMDDebugOutput() { }

}
