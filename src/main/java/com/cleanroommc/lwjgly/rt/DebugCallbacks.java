package com.cleanroommc.lwjgly.rt;

import org.lwjgl.opengl.AMDDebugOutputCallback;
import org.lwjgl.opengl.ARBDebugOutputCallback;
import org.lwjgl.opengl.DebugCallbackBridge;
import org.lwjgl.opengl.GLDebugMessageAMDCallback;
import org.lwjgl.opengl.GLDebugMessageARBCallback;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.opengl.KHRDebugCallback;

public final class DebugCallbacks {

    private static GLDebugMessageCallback core;
    private static GLDebugMessageARBCallback arb;
    private static GLDebugMessageAMDCallback amd;

    public static synchronized GLDebugMessageCallback core(KHRDebugCallback callback) {
        GLDebugMessageCallback previous = core;
        if (callback == null) {
            core = null;
        } else {
            KHRDebugCallback.Handler handler = DebugCallbackBridge.handler(callback);
            core = GLDebugMessageCallback.create((source, type, id, severity, length, message, userParam) ->
                            handler.handleMessage(source, type, id, severity, GLDebugMessageCallback.getMessage(length, message)));
        }
        return previous;
    }

    public static synchronized GLDebugMessageARBCallback arb(ARBDebugOutputCallback callback) {
        GLDebugMessageARBCallback previous = arb;
        if (callback == null) {
            arb = null;
        } else {
            ARBDebugOutputCallback.Handler handler = DebugCallbackBridge.handler(callback);
            arb = GLDebugMessageARBCallback.create((source, type, id, severity, length, message, userParam) ->
                            handler.handleMessage(source, type, id, severity, GLDebugMessageARBCallback.getMessage(length, message)));
        }
        return previous;
    }

    public static synchronized GLDebugMessageAMDCallback amd(AMDDebugOutputCallback callback) {
        GLDebugMessageAMDCallback previous = amd;
        if (callback == null) {
            amd = null;
        } else {
            AMDDebugOutputCallback.Handler handler = DebugCallbackBridge.handler(callback);
            amd = GLDebugMessageAMDCallback.create((id, category, severity, length, message, userParam) ->
                            handler.handleMessage(id, category, severity, GLDebugMessageAMDCallback.getMessage(length, message)));
        }
        return previous;
    }

    public static void release(org.lwjgl.system.Callback previous) {
        if (previous != null) {
            previous.free();
        }
    }

    public static synchronized GLDebugMessageCallback installedCore() {
        return core;
    }

    public static synchronized GLDebugMessageARBCallback installedArb() {
        return arb;
    }

    public static synchronized GLDebugMessageAMDCallback installedAmd() {
        return amd;
    }

    private DebugCallbacks() { }

}
