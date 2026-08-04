package org.lwjgl;

import org.lwjgl.sdl.SDLMessageBox;
import org.lwjgl.sdl.SDLMisc;
import org.lwjgl.system.Pointer;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;

/** LWJGL 2 system helpers backed by Java and SDL. */
public class Sys {

    private static final long NANOSECONDS_PER_SECOND = 1_000_000_000L;

    // TODO: report LWJGL 3 or LWJGL 2?
    public static String getVersion() {
        return "2.9.4";
    }

    public static void initialize() { }

    public static boolean is64Bit() {
        return Pointer.POINTER_SIZE == 8;
    }

    public static long getTimerResolution() {
        return NANOSECONDS_PER_SECOND;
    }

    public static long getTime() {
        return System.nanoTime();
    }

    /** Shows a modal SDL message box. */
    public static void alert(String title, String message) {
        SDLMessageBox.SDL_ShowSimpleMessageBox(SDLMessageBox.SDL_MESSAGEBOX_ERROR, title, message, 0L);
    }

    public static boolean openURL(String url) {
        return SDLMisc.SDL_OpenURL(url);
    }

    /** Returns the clipboard text, or null when the clipboard is unavailable. */
    public static String getClipboard() {
        try {
            Object contents = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            return contents instanceof String text ? text : null;
        } catch (Exception e) {
            // The clipboard is unavailable or the JVM is headless
            return null;
        }
    }

    protected Sys() { }

}
