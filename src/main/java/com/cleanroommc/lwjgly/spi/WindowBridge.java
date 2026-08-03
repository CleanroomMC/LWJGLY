package com.cleanroommc.lwjgly.spi;

/**
 * Cleanroom already has an SDL3 window, GL context and input pump.
 * LWJGLY does not open its own and must not.
 * Two pumps would split every event stream between them,
 * and on macOS the second one would be calling Cocoa off the main thread.
 * So the dependency is inverted. LWJGLY declares this interface, Cleanroom implements it over its {@code Window},
 * and {@link com.cleanroommc.lwjgly.LWJGLY#setWindowBridge} hands it over before the game starts.
 *
 * <p>Everything here is a read or a request, no polls other than {@link #pump()}.
 *
 * <p>Coordinates and buttons are SDL's: origin top-left, button 1 left / 2 middle / 3 right, wheel
 * positive away from the user. The shims convert to LWJGL 2's conventions (origin bottom-left, its
 * own button order) as that conversion is part of LWJGL 2 compatibility.
 */
public interface WindowBridge {

    long handle();

    int width();

    int height();

    String title();

    void title(String title);

    boolean closeRequested();

    boolean focused();

    /** True once per resize, consuming the flag: LWJGL 2's {@code Display.wasResized()}. */
    boolean consumeResized();

    boolean fullscreen();

    void fullscreen(boolean fullscreen);

    void vsync(boolean vsync);

    /**
     * Makes the GL context current on the calling thread.
     * The implementation must bind LWJGL 3's capabilities at the same time ({@code GL.setCapabilities()}).
     * Since LWJGL 3 resolves entry points per thread and would disagree with SDL about which context is current.
     */
    void makeCurrent();

    void releaseContext();

    void swapBuffers();

    /**
     * Drains the OS event queue into the state and queues below.
     *
     * <p>Two pumps do not double events, instead it splits.
     * Half the input disappears into whichever consumer polled first.
     */
    void pump();

    /** The next queued key event, or null when the queue is empty. */
    KeyEvent nextKeyEvent();

    int queuedKeyEvents();

    boolean keyDown(int sdlScancode);

    void textInput(boolean enabled);

    /** The next queued mouse event. Null when the queue is empty. */
    MouseEvent nextMouseEvent();

    int queuedMouseEvents();

    boolean mouseButtonDown(int sdlButton);

    float mouseX();

    float mouseY();

    /** Movement since the last call. Zero afterwards. */
    float takeMouseDeltaX();

    float takeMouseDeltaY();

    float takeMouseWheel();

    void mousePosition(float x, float y);

    void grabMouse(boolean grab);

    boolean mouseGrabbed();

    /**
     * A key event as SDL reported it. {@code character} is 0 when the key produced no text.
     *
     * @param timestampNs SDL's timestamp, which LWJGL 2 exposes as {@code Keyboard.getEventNanoseconds()}
     */
    record KeyEvent(int scancode, int key, boolean pressed, boolean repeat, char character, long timestampNs) { }

    /** A mouse event as SDL reported it. {@code button} is 0 for motion and wheel events. */
    record MouseEvent(int button, boolean pressed, float x, float y, float dx, float dy, float wheel, long timestampNs) { }

}
