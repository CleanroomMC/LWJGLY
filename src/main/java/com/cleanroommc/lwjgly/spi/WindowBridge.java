package com.cleanroommc.lwjgly.spi;

import java.util.Objects;

/**
 * Connects LWJGLY's LWJGL 2 compatibility APIs to the window, OpenGL context and input pump owned by the host.
 *
 * <p>LWJGLY does not create a second window or poll SDL independently.
 * Two event pumps would divide the event stream between consumers,
 * and on macOS a second pump could call Cocoa from the wrong thread.
 * Instead, the host implements this interface over its existing window and installs the
 * implementation with {@link com.cleanroommc.lwjgly.LWJGLY#setWindowBridge(WindowBridge)}.
 *
 * <p>Everything here is a read or a request, no polls other than {@link #pump()}.
 *
 * <p>Coordinates and buttons are SDL's: origin top-left, button 1 left / 2 middle / 3 right, wheel
 * <ul>
 *     <li>Origin: Top-left</li>
 *     <li>Button 1: Left</li>
 *     <li>Button 2: Middle</li>
 *     <li>Button 3: Right</li>
 *     <li>Wheel: {@code +} away from the user</li>
 * </ul>
 * The shims convert to LWJGL 2's conventions (origin bottom-left, its own button order) as that conversion is part of LWJGL 2 compatibility.
 */
public interface WindowBridge {

    /**
     * Returns the native SDL window handle owned by the host.
     *
     * @return the {@code SDL_Window} pointer encoded as a {@code long}, or {@code 0} if unavailable
     */
    long handle();

    /**
     * Returns the current width of the window's client area in SDL coordinate units.
     *
     * @return the current client-area width
     */
    int width();

    /**
     * Returns the current height of the window's client area in SDL coordinate units.
     *
     * @return the current client-area height
     */
    int height();

    /**
     * Returns the window title currently known to the host.
     *
     * @return the current title
     */
    String title();

    /**
     * Requests that the host change the window title.
     *
     * @param title the new title
     */
    void title(String title);

    /**
     * Reports whether the user or window system has requested that the window close.
     *
     * @return {@code true} if a close request is pending
     */
    boolean closeRequested();

    /**
     * Reports whether the host window currently has input focus.
     *
     * @return {@code true} when the window is focused
     */
    boolean focused();

    /**
     * Consumes the host's resize notification.
     *
     * @return {@code true} once after one or more resizes, then {@code false} until another resize
     */
    boolean consumeResized();

    /**
     * Reports whether the host window is currently fullscreen.
     *
     * @return {@code true} when fullscreen
     */
    boolean fullscreen();

    /**
     * Requests a transition between windowed and fullscreen presentation.
     *
     * @param fullscreen {@code true} to enter fullscreen, {@code false} to return to windowed mode
     */
    void fullscreen(boolean fullscreen);

    /**
     * Enables or disables synchronization of buffer swaps to the display refresh.
     *
     * @param vsync {@code true} to enable vertical synchronization
     */
    void vsync(boolean vsync);

    /**
     * Adopts the host's existing OpenGL context for an LWJGL 2 {@code Display.create} request.
     *
     * <p>The implementation must make the context current, bind LWJGL 3's capabilities and validate
     * the request against the actual context. An SDL implementation should use {@code SDL_GL_GetAttribute}
     * as setting attributes here is too late because SDL requires them before creating the OpenGL window and context.
     *
     * <p>On both acceptance and rejection, return the handles reported by
     * {@code SDL_GL_GetCurrentWindow()} and {@code SDL_GL_GetCurrentContext()}.
     * A rejected context that was made current will be released by {@code Display}.
     *
     * @param request the framebuffer and context requirements translated from LWJGL 2
     * @return the validation result and the handles that SDL reports as current
     */
    ContextResult adoptContext(ContextRequest request);

    /**
     * Makes the host's GL context current on the calling thread.
     *
     * <p>The implementation must bind LWJGL 3's capabilities at the same time with {@code GL.setCapabilities(...)}.
     * LWJGL 3 resolves entry points per thread and would otherwise disagree with SDL about which context is current.
     */
    void makeCurrent();

    /**
     * Releases the host's GL context from the calling thread and clears that thread's LWJGL 3 capability binding.
     */
    void releaseContext();

    /**
     * Presents the current back buffer using the host window.
     *
     * <p>The host context must be current on the calling thread.
     */
    void swapBuffers();

    /**
     * Drains the OS event queue into the state and queues below.
     *
     * <p>This is the only method in this interface that may poll the platform event source.
     * Calling a second independent pump does not duplicate events,
     * but removes them before the other consumer can observe them.
     */
    void pump();

    /**
     * Removes and returns the oldest queued key event.
     *
     * @return the next event, or {@code null} when the queue is empty
     */
    KeyEvent nextKeyEvent();

    /**
     * Returns the number of key events available to {@link #nextKeyEvent()} without pumping.
     *
     * @return the queued key-event count
     */
    int queuedKeyEvents();

    /**
     * Tests the host's current keyboard state using an SDL scancode.
     *
     * @param sdlScancode an SDL scancode, not an LWJGL 2 key code or SDL keycode
     * @return {@code true} if the physical key is currently held
     */
    boolean keyDown(int sdlScancode);

    /**
     * Enables or disables the host's platform text-input service.
     *
     * <p>Enabling this service allows composed text to be associated with queued key events.
     * It must not create a second event pump.
     *
     * @param enabled {@code true} to accept text input
     */
    void textInput(boolean enabled);

    /**
     * Removes and returns the oldest queued mouse event.
     *
     * @return the next event, or {@code null} when the queue is empty
     */
    MouseEvent nextMouseEvent();

    /**
     * Returns the number of mouse events available to {@link #nextMouseEvent()} without pumping.
     *
     * @return the queued mouse-event count
     */
    int queuedMouseEvents();

    /**
     * Tests the host's current mouse-button state using SDL's one-based button numbering.
     *
     * @param sdlButton the SDL button number
     * @return {@code true} if the button is currently held
     */
    boolean mouseButtonDown(int sdlButton);

    /**
     * Returns the current horizontal pointer coordinate in SDL's top-left-origin coordinate system.
     *
     * @return the pointer's x coordinate
     */
    float mouseX();

    /**
     * Returns the current vertical pointer coordinate in SDL's top-left-origin coordinate system.
     *
     * @return the pointer's y coordinate
     */
    float mouseY();

    /**
     * Returns and clears accumulated horizontal mouse motion.
     *
     * @return motion since the previous call, or zero if no motion accumulated
     */
    float takeMouseDeltaX();

    /**
     * Returns and clears accumulated vertical mouse motion in SDL coordinates, where positive is
     * downward.
     *
     * @return motion since the previous call, or zero if no motion accumulated
     */
    float takeMouseDeltaY();

    /**
     * Returns and clears accumulated vertical wheel motion in SDL units.
     *
     * @return wheel motion since the previous call, positive away from the user
     */
    float takeMouseWheel();

    /**
     * Warps the pointer to a position in SDL's top-left-origin coordinate system.
     *
     * @param x the target x coordinate
     * @param y the target y coordinate
     */
    void mousePosition(float x, float y);

    /**
     * Enables or disables the host's grabbed/relative pointer mode.
     *
     * @param grab {@code true} to grab the pointer
     */
    void grabMouse(boolean grab);

    /**
     * Reports whether the host currently has the pointer grabbed.
     *
     * @return {@code true} when pointer grabbing is active
     */
    boolean mouseGrabbed();

    /**
     * A keyboard transition and any text character associated with it by the host.
     *
     * <p>The physical {@code scancode} drives LWJGL 2 key mapping.
     * The layout-dependent {@code key} is retained for hosts and diagnostics.
     * {@code character} is {@code '\0'} when the event produced no text.
     *
     * @param scancode the SDL scancode identifying the physical key
     * @param key the SDL keycode after applying the active keyboard layout
     * @param pressed {@code true} for a key press, {@code false} for a release
     * @param repeat {@code true} when this is an automatically repeated press
     * @param character the UTF-16 character associated with the event, or {@code '\0'}
     * @param timestampNs SDL's timestamp, which LWJGL 2 exposes as {@code Keyboard.getEventNanoseconds()}
     */
    record KeyEvent(int scancode, int key, boolean pressed, boolean repeat, char character, long timestampNs) { }

    /**
     * A mouse event expressed in SDL's coordinate and button conventions.
     *
     * <p>Absolute coordinates use a top-left origin. Motion deltas are positive right and down.
     * Wheel motion is positive away from the user. {@code button} is {@code 0} for motion and wheel events,
     * and {@code pressed} is meaningful only for button events.
     *
     * @param button the one-based SDL button number, or {@code 0} for a non-button event
     * @param pressed {@code true} for a button press, {@code false} for a release or non-button event
     * @param x the pointer's x coordinate when the event occurred
     * @param y the pointer's y coordinate when the event occurred
     * @param dx horizontal motion carried by the event
     * @param dy vertical motion carried by the event
     * @param wheel vertical wheel motion carried by the event
     * @param timestampNs SDL's event timestamp in nanoseconds
     */
    record MouseEvent(int button, boolean pressed, float x, float y, float dx, float dy, float wheel, long timestampNs) { }

    /**
     * SDL-independent minimum framebuffer properties requested by LWJGL 2.
     *
     * <p>SDL mappings are:
     * <ul>
     *     <li>alpha => {@code SDL_GL_ALPHA_SIZE}</li>
     *     <li>depth => {@code SDL_GL_DEPTH_SIZE}</li>
     *     <li>stencil => {@code SDL_GL_STENCIL_SIZE}</li>
     *     <li>samples => {@code SDL_GL_MULTISAMPLEBUFFERS} and {@code SDL_GL_MULTISAMPLESAMPLES}</li>
     *     <li>floating point => {@code SDL_GL_FLOATBUFFERS}</li>
     *     <li>sRGB => {@code SDL_GL_FRAMEBUFFER_SRGB_CAPABLE}</li>
     * </ul>
     *
     * <p>SDL has no portable equivalents for auxiliary buffers, NV coverage color samples or packed floating point.
     * A non-default unsupported request must be rejected instead of silently accepted.
     * LWJGL 2 ignored bits-per-pixel for {@code Display.create}, but it is retained here for diagnostics.
     *
     * @param bitsPerPixel requested color-buffer size - retained for diagnostics
     * @param alphaBits minimum alpha-channel bit depth
     * @param depthBits minimum depth-buffer bit depth
     * @param stencilBits minimum stencil-buffer bit depth
     * @param samples minimum multisample count, or {@code 0} when multisampling is not required
     * @param colorSamples minimum NV coverage color-sample count
     * @param auxiliaryBuffers minimum number of auxiliary buffers
     * @param accumulationBitsPerPixel minimum total RGB accumulation-buffer bit depth
     * @param accumulationAlphaBits minimum accumulation-buffer alpha bit depth
     * @param stereo whether stereoscopic buffers are required
     * @param floatingPoint whether a floating-point color buffer is required
     * @param floatingPointPacked whether a packed floating-point color buffer is required
     * @param sRGB whether an sRGB-capable framebuffer is required
     */
    record PixelFormatRequest(
            int bitsPerPixel,
            int alphaBits,
            int depthBits,
            int stencilBits,
            int samples,
            int colorSamples,
            int auxiliaryBuffers,
            int accumulationBitsPerPixel,
            int accumulationAlphaBits,
            boolean stereo,
            boolean floatingPoint,
            boolean floatingPointPacked,
            boolean sRGB
    ) { }

    /**
     * SDL-independent OpenGL context properties requested by LWJGL 2.
     *
     * <p>The version is a minimum, an explicit profile must match, and each requested flag must be present.
     * A {@code DEFAULT} reset-notification or release-behavior value means
     * LWJGL 2 emitted no creation attribute for that property.
     * The containing {@link ContextRequest} uses {@code null}
     * instead of this record when no {@code ContextAttribs} was supplied.
     *
     * @param majorVersion minimum OpenGL major version
     * @param minorVersion minimum OpenGL minor version within {@code majorVersion}
     * @param profile required OpenGL profile, or {@link ContextProfile#DEFAULT} for no requirement
     * @param debug whether the debug-context flag is required
     * @param forwardCompatible whether the forward-compatible flag is required
     * @param robustAccess whether the robust-access flag is required
     * @param resetIsolation whether the reset-isolation flag is required
     * @param resetNotification required reset-notification behavior
     * @param releaseBehavior required context-release behavior
     * @param layerPlane requested layer plane; nonzero values are unsupported by SDL
     */
    record ContextAttributesRequest(
            int majorVersion,
            int minorVersion,
            ContextProfile profile,
            boolean debug,
            boolean forwardCompatible,
            boolean robustAccess,
            boolean resetIsolation,
            ResetNotification resetNotification,
            ReleaseBehavior releaseBehavior,
            int layerPlane
    ) {

        public ContextAttributesRequest {
            Objects.requireNonNull(profile, "profile");
            Objects.requireNonNull(resetNotification, "resetNotification");
            Objects.requireNonNull(releaseBehavior, "releaseBehavior");
        }

    }

    /**
     * Complete set of requirements used when an LWJGL 2 display adopts the host context.
     *
     * @param pixelFormat framebuffer requirements, never {@code null}
     * @param attributes context requirements, or {@code null} when the caller supplied no explicit
     *                   LWJGL 2 context attributes
     */
    record ContextRequest(PixelFormatRequest pixelFormat, ContextAttributesRequest attributes) {

        public ContextRequest {
            Objects.requireNonNull(pixelFormat, "pixelFormat");
        }

    }

    /**
     * Result of adopting and validating the host context.
     * Handles are the current SDL handles and not merely the handles the bridge intended to bind.
     * {@code capabilitiesBound} asserts that {@code GL.setCapabilities(...)} has been completed for the calling thread.
     *
     * @param accepted whether the current context satisfies the complete request
     * @param capabilitiesBound whether LWJGL 3 capabilities are bound on the calling thread
     * @param currentWindowHandle the handle reported by {@code SDL_GL_GetCurrentWindow()}, or zero
     * @param currentContextHandle the handle reported by {@code SDL_GL_GetCurrentContext()}, or zero
     * @param message a useful rejection reason, or {@code null} when accepted
     */
    record ContextResult(boolean accepted, boolean capabilitiesBound, long currentWindowHandle, long currentContextHandle, String message) {

        public ContextResult {
            if (accepted && message != null) {
                throw new IllegalArgumentException("An accepted context cannot have a rejection message");
            }
            if (!accepted && (message == null || message.isBlank())) {
                throw new IllegalArgumentException("A rejected context needs a message");
            }
        }

        /**
         * Creates a successful result for a current context whose LWJGL 3 capabilities are bound.
         *
         * @param currentWindowHandle the actual current SDL window handle
         * @param currentContextHandle the actual current SDL OpenGL context handle
         * @return a successful adoption result
         */
        public static ContextResult accepted(long currentWindowHandle, long currentContextHandle) {
            return new ContextResult(true, true, currentWindowHandle, currentContextHandle, null);
        }

        /**
         * Creates a rejected result.
         *
         * @param message a nonblank explanation suitable for an {@code LWJGLException}
         * @param currentWindowHandle the actual current SDL window handle, or zero
         * @param currentContextHandle the actual current SDL OpenGL context handle, or zero
         * @return a rejected adoption result
         */
        public static ContextResult rejected(String message, long currentWindowHandle, long currentContextHandle) {
            return new ContextResult(false, false, currentWindowHandle, currentContextHandle, message);
        }

    }

    enum ContextProfile {

        /** No explicit profile requirement. */
        DEFAULT,
        /** Desktop OpenGL core profile. */
        CORE,
        /** Desktop OpenGL compatibility profile. */
        COMPATIBILITY,
        /** OpenGL ES profile. */
        ES;

    }

    /** Behavior requested when the graphics reset status indicates a lost context. */
    enum ResetNotification {

        /** No explicit reset-notification strategy. */
        DEFAULT,
        /** Lose the context and report a graphics-reset status after a reset. */
        LOSE_CONTEXT;

    }

    /** Behavior requested when a context is released from a thread. */
    enum ReleaseBehavior {

        /** No explicit release behavior. Use the implementation default (normally flush). */
        DEFAULT,
        /** Do not implicitly flush pending commands when releasing the context. */
        NONE;

    }

}
