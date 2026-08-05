package org.lwjgl.opengl;

/** Reads handlers from LWJGL 2 debug callbacks without reusing their native trampolines. */
public final class DebugCallbackBridge {

    public static KHRDebugCallback.Handler handler(KHRDebugCallback callback) {
        return callback.getHandler();
    }

    public static ARBDebugOutputCallback.Handler handler(ARBDebugOutputCallback callback) {
        return callback.getHandler();
    }

    public static AMDDebugOutputCallback.Handler handler(AMDDebugOutputCallback callback) {
        return callback.getHandler();
    }

    private DebugCallbackBridge() { }

}
