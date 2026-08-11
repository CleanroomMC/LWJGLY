package com.cleanroommc.lwjgly;

import com.cleanroommc.lwjgly.spi.WindowBridge;

/**
 * There is exactly one of these to set. Cleanroom calls
 * {@link #setWindowBridge(WindowBridge)} once its window exists and before any mod code runs;
 * {@code Display}, {@code Keyboard} and {@code Mouse} read it from there.
 */
public final class LWJGLY {

    private static volatile WindowBridge windowBridge;

    public static void setWindowBridge(WindowBridge bridge) {
        windowBridge = bridge;
    }

    public static WindowBridge windowBridge() {
        WindowBridge bridge = windowBridge;
        if (bridge == null) {
            throw new IllegalStateException("LWJGLY has no window bridge:" +
                    "the host must call LWJGLY.setWindowBridge(...) after " +
                    "creating its window and before any code touching org.lwjgl.opengl.Display, " +
                    "org.lwjgl.input.Keyboard or org.lwjgl.input.Mouse runs.");
        }
        return bridge;
    }

    /** Whether a bridge has been installed. Lets a shim answer "not created" instead of throwing. */
    public static boolean hasWindowBridge() {
        return windowBridge != null;
    }

    private LWJGLY() { }

}
