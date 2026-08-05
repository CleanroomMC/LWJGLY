package org.lwjgl.opengl;

import com.cleanroommc.lwjgly.LWJGLY;
import com.cleanroommc.lwjgly.spi.WindowBridge;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.nio.ByteBuffer;

/** LWJGL 2 display API over the window and GL context owned by Cleanroom. */
public class Display {

    private static long nextFrameNanos;

    public static void create() throws LWJGLException {
        LWJGLY.windowBridge().makeCurrent();
    }

    public static void create(PixelFormat pixelFormat) throws LWJGLException {
        create();
    }

    public static void create(PixelFormat pixelFormat, Drawable sharedDrawable) throws LWJGLException {
        create();
    }

    public static void create(PixelFormat pixelFormat, ContextAttribs attribs) throws LWJGLException {
        create();
    }

    public static void create(PixelFormat pixelFormat, Drawable sharedDrawable, ContextAttribs attribs) throws LWJGLException {
        create();
    }

    public static void create(PixelFormatLWJGL pixelFormat) throws LWJGLException {
        create();
    }

    public static void create(PixelFormatLWJGL pixelFormat, Drawable sharedDrawable) throws LWJGLException {
        create();
    }

    public static boolean isCreated() {
        return LWJGLY.hasWindowBridge();
    }

    public static void destroy() {
        LWJGLY.windowBridge().releaseContext();
    }

    public static void update() {
        update(true);
    }

    public static void update(boolean processMessages) {
        WindowBridge bridge = LWJGLY.windowBridge();
        bridge.swapBuffers();
        if (processMessages) {
            bridge.pump();
        }
        Mouse.updateCursor();
    }

    public static void swapBuffers() throws LWJGLException {
        LWJGLY.windowBridge().swapBuffers();
    }

    public static void processMessages() {
        LWJGLY.windowBridge().pump();
    }

    public static void sync(int fps) {
        if (fps <= 0) {
            return;
        }
        long frame = 1_000_000_000L / fps;
        long now = System.nanoTime();
        if (nextFrameNanos == 0L || now - nextFrameNanos > frame * 4) {
            nextFrameNanos = now;
        }
        nextFrameNanos += frame;
        long remaining = nextFrameNanos - System.nanoTime();
        while (remaining > 0) {
            if (remaining > 1_500_000L) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            } else {
                Thread.yield();
            }
            remaining = nextFrameNanos - System.nanoTime();
        }
    }

    public static void makeCurrent() throws LWJGLException {
        LWJGLY.windowBridge().makeCurrent();
    }

    public static void releaseContext() throws LWJGLException {
        LWJGLY.windowBridge().releaseContext();
    }

    /** Returns whether this thread has LWJGL 3 GL capabilities. */
    public static boolean isCurrent() throws LWJGLException {
        try {
            return GL.getCapabilities() != null;
        } catch (IllegalStateException noCapabilities) {
            // getCapabilities() may throw after the threadlocal binding is cleared
            return false;
        }
    }

    /** Always null. LWJGL 2's {@code Drawable} has no LWJGL 3 counterpart, see build/lwjgly/PROBLEMS.md. */
    public static Drawable getDrawable() {
        return null;
    }

    public static String getTitle() {
        return LWJGLY.windowBridge().title();
    }

    public static void setTitle(String title) {
        LWJGLY.windowBridge().title(title);
    }

    public static int getWidth() {
        return LWJGLY.windowBridge().width();
    }

    public static int getHeight() {
        return LWJGLY.windowBridge().height();
    }

    public static int getX() {
        return 0;
    }

    public static int getY() {
        return 0;
    }

    public static boolean isCloseRequested() {
        return LWJGLY.windowBridge().closeRequested();
    }

    public static boolean isActive() {
        return LWJGLY.windowBridge().focused();
    }

    public static boolean wasResized() {
        return LWJGLY.windowBridge().consumeResized();
    }

    public static boolean isFullscreen() {
        return LWJGLY.windowBridge().fullscreen();
    }

    public static void setFullscreen(boolean fullscreen) throws LWJGLException {
        LWJGLY.windowBridge().fullscreen(fullscreen);
    }

    public static void setVSyncEnabled(boolean enabled) {
        LWJGLY.windowBridge().vsync(enabled);
    }

    public static void setSwapInterval(int interval) {
        LWJGLY.windowBridge().vsync(interval > 0);
    }

    /** Returns true until the bridge tracks minimize and expose events. */
    public static boolean isVisible() {
        return true;
    }

    /** Always false: SDL has no equivalent of LWJGL 2's damage flag. */
    public static boolean isDirty() {
        return false;
    }

    public static DisplayMode getDisplayMode() {
        WindowBridge bridge = LWJGLY.windowBridge();
        return new DisplayMode(bridge.width(), bridge.height());
    }

    public static DisplayMode getDesktopDisplayMode() {
        return getDisplayMode();
    }

    public static DisplayMode[] getAvailableDisplayModes() throws LWJGLException {
        return new DisplayMode[] { getDisplayMode() };
    }

    public static void setDisplayMode(DisplayMode mode) throws LWJGLException {
        if (mode != null) {
            LWJGLY.windowBridge().fullscreen(mode.isFullscreenCapable() && Display.isFullscreen());
        }
    }

    public static void setDisplayModeAndFullscreen(DisplayMode mode) throws LWJGLException {
        if (mode != null) {
            LWJGLY.windowBridge().fullscreen(mode.isFullscreenCapable());
        }
    }

    // Accepted and ignored settings

    /** Gamma, brightness and contrast. SDL 3 dropped the gamma ramp API. See build/lwjgly/PROBLEMS.md. */
    public static void setDisplayConfiguration(float gamma, float brightness, float contrast) throws LWJGLException { }

    /** The clear colour before the first frame. Nothing observes it for the one frame it would last. */
    public static void setInitialBackground(float red, float green, float blue) { }

    /** Returns zero until the bridge supports SDL window icons. */
    public static int setIcon(ByteBuffer[] icons) {
        return 0;
    }

    /** Stores no resizable state until the bridge exposes it. */
    public static void setResizable(boolean resizable) { }

    public static boolean isResizable() {
        return false;
    }

    public static void setLocation(int x, int y) { }

    public static String getAdapter() {
        return null;
    }

    public static String getVersion() {
        return null;
    }

    /** Returns 1 until the bridge exposes a shared HiDPI scale. */
    public static float getPixelScaleFactor() {
        return 1.0F;
    }

    public static Canvas getParent() {
        return null;
    }

    /** Embedding GL in an AWT canvas does not apply to an SDL-windowed game. */
    public static void setParent(Canvas parent) throws LWJGLException {
        throw new UnsupportedOperationException("org.lwjgl.opengl.Display.setParent: the game is not AWT-hosted, see build/lwjgly/PROBLEMS.md");
    }

    protected Display() { }

}
