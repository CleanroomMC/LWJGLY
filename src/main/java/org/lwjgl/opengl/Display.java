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
        create(new PixelFormat());
    }

    public static void create(PixelFormat pixelFormat) throws LWJGLException {
        create(pixelFormat, null, null);
    }

    public static void create(PixelFormat pixelFormat, Drawable sharedDrawable) throws LWJGLException {
        create(pixelFormat, sharedDrawable, null);
    }

    public static void create(PixelFormat pixelFormat, ContextAttribs attribs) throws LWJGLException {
        create(pixelFormat, null, attribs);
    }

    public static void create(PixelFormat pixelFormat, Drawable sharedDrawable, ContextAttribs attribs) throws LWJGLException {
        if (pixelFormat == null) {
            throw new NullPointerException("pixelFormat cannot be null");
        }
        if (sharedDrawable != null) {
            throw new LWJGLException("Context sharing cannot be added to Cleanroom's already-created SDL context");
        }
        adoptContext(new WindowBridge.ContextRequest(pixelFormatRequest(pixelFormat), contextRequest(attribs)));
    }

    public static void create(PixelFormatLWJGL pixelFormat) throws LWJGLException {
        create(pixelFormat, null);
    }

    public static void create(PixelFormatLWJGL pixelFormat, Drawable sharedDrawable) throws LWJGLException {
        if (pixelFormat == null) {
            throw new NullPointerException("pixelFormat cannot be null");
        }
        if (pixelFormat instanceof PixelFormat desktopFormat) {
            create(desktopFormat, sharedDrawable);
            return;
        }
        throw new LWJGLException("Unsupported PixelFormatLWJGL implementation: " + pixelFormat.getClass().getName() +
                " - OpenGL ES display creation is not bridged");
    }

    private static WindowBridge.PixelFormatRequest pixelFormatRequest(PixelFormat pixelFormat) {
        return new WindowBridge.PixelFormatRequest(
                pixelFormat.getBitsPerPixel(),
                pixelFormat.getAlphaBits(),
                pixelFormat.getDepthBits(),
                pixelFormat.getStencilBits(),
                pixelFormat.getSamples(),
                pixelFormat.getColorSamples(),
                pixelFormat.getAuxBuffers(),
                pixelFormat.getAccumulationBitsPerPixel(),
                pixelFormat.getAccumulationAlpha(),
                pixelFormat.isStereo(),
                pixelFormat.isFloatingPoint(),
                pixelFormat.isFloatingPointPacked(),
                pixelFormat.isSRGB()
        );
    }

    private static WindowBridge.ContextAttributesRequest contextRequest(ContextAttribs attribs) throws LWJGLException {
        if (attribs == null) {
            return null;
        }
        int knownFlags = ContextAttribs.CONTEXT_DEBUG_BIT_ARB
                | ContextAttribs.CONTEXT_FORWARD_COMPATIBLE_BIT_ARB
                | ContextAttribs.CONTEXT_ROBUST_ACCESS_BIT_ARB
                | ContextAttribs.CONTEXT_RESET_ISOLATION_BIT_ARB;
        if ((attribs.getContextFlags() & ~knownFlags) != 0) {
            throw new LWJGLException("Unsupported OpenGL context flags: 0x" + Integer.toHexString(attribs.getContextFlags() & ~knownFlags));
        }
        WindowBridge.ContextProfile profile;
        if (attribs.getProfileMask() == 0) {
            profile = WindowBridge.ContextProfile.DEFAULT;
        } else if (attribs.isProfileCore()) {
            profile = WindowBridge.ContextProfile.CORE;
        } else if (attribs.isProfileCompatibility()) {
            profile = WindowBridge.ContextProfile.COMPATIBILITY;
        } else if (attribs.isProfileES()) {
            profile = WindowBridge.ContextProfile.ES;
        } else {
            throw new LWJGLException("Unsupported OpenGL context profile mask: 0x" + Integer.toHexString(attribs.getProfileMask()));
        }

        WindowBridge.ResetNotification resetNotification;
        if (attribs.getContextResetNotificationStrategy() == ContextAttribs.NO_RESET_NOTIFICATION_ARB) {
            resetNotification = WindowBridge.ResetNotification.DEFAULT;
        } else if (attribs.getContextResetNotificationStrategy() == ContextAttribs.LOSE_CONTEXT_ON_RESET_ARB) {
            resetNotification = WindowBridge.ResetNotification.LOSE_CONTEXT;
        } else {
            throw new LWJGLException("Unsupported context reset notification strategy: 0x" + Integer.toHexString(attribs.getContextResetNotificationStrategy()));
        }

        WindowBridge.ReleaseBehavior releaseBehavior;
        if (attribs.getContextReleaseBehavior() == ContextAttribs.CONTEXT_RELEASE_BEHAVIOR_FLUSH_ARB) {
            releaseBehavior = WindowBridge.ReleaseBehavior.DEFAULT;
        } else if (attribs.getContextReleaseBehavior() == ContextAttribs.CONTEXT_RELEASE_BEHAVIOR_NONE_ARB) {
            releaseBehavior = WindowBridge.ReleaseBehavior.NONE;
        } else {
            throw new LWJGLException("Unsupported context release behavior: 0x" + Integer.toHexString(attribs.getContextReleaseBehavior()));
        }

        return new WindowBridge.ContextAttributesRequest(
                attribs.getMajorVersion(),
                attribs.getMinorVersion(),
                profile,
                attribs.isDebug(),
                attribs.isForwardCompatible(),
                attribs.isRobustAccess(),
                attribs.isContextResetIsolation(),
                resetNotification,
                releaseBehavior,
                attribs.getLayerPlane()
        );
    }

    private static void adoptContext(WindowBridge.ContextRequest request) throws LWJGLException {
        WindowBridge bridge = LWJGLY.windowBridge();
        long expectedWindow = bridge.handle();
        if (expectedWindow == 0L) {
            throw new LWJGLException("The window bridge has no SDL window handle");
        }

        final WindowBridge.ContextResult result;
        try {
            result = bridge.adoptContext(request);
        } catch (RuntimeException failure) {
            throw new LWJGLException("Window bridge failed to adopt its SDL OpenGL context", failure);
        }
        if (result == null) {
            throw new LWJGLException("Window bridge returned no context adoption result");
        }

        boolean contextWasMadeCurrent = result.currentContextHandle() != 0L;
        if (!result.accepted()) {
            failContextAdoption(bridge, result.message(), null, contextWasMadeCurrent);
        }
        if (result.currentWindowHandle() != expectedWindow) {
            failContextAdoption(bridge,
                    "The current SDL OpenGL window does not match the window bridge (expected 0x" + Long.toHexString(expectedWindow) + ", got 0x" + Long.toHexString(result.currentWindowHandle()) + ')',
                    null, contextWasMadeCurrent);
        }
        if (!contextWasMadeCurrent) {
            failContextAdoption(bridge, "SDL has no current OpenGL context after adoption", null, false);
        }

        if (!result.capabilitiesBound()) {
            failContextAdoption(bridge, "LWJGL 3 GL capabilities were not bound during context adoption",
                    null, true);
        }
    }

    private static void failContextAdoption(WindowBridge bridge, String message, Throwable cause,
                                            boolean releaseContext) throws LWJGLException {
        LWJGLException failure = cause == null ? new LWJGLException(message) : new LWJGLException(message, cause);
        if (releaseContext) {
            try {
                bridge.releaseContext();
            } catch (RuntimeException releaseFailure) {
                failure.addSuppressed(releaseFailure);
            }
        }
        throw failure;
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
