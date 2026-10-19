package org.lwjgl.input;

import com.cleanroommc.lwjgly.LWJGLY;
import com.cleanroommc.lwjgly.spi.WindowBridge;
import org.lwjgl.LWJGLException;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLMouse;

/**
 * LWJGL 2 mouse API backed by SDL events.
 * Converts Y origin, button order, and wheel units to LWJGL 2 conventions.
 */
public class Mouse {

    public static final int EVENT_SIZE = 22;

    /** LWJGL 2 wheel units per notch. */
    private static final int WHEEL_DELTA = 120;

    /** Number of buttons tracked by Cleanroom. */
    private static final int BUTTON_COUNT = 8;

    private static WindowBridge.MouseEvent current;
    private static boolean clipToWindow;
    /** Last cursor installed through {@link #setNativeCursor}. */
    private static Cursor currentCursor;
    /** Fractional SDL motion carried into the next integer LWJGL 2 reading. */
    private static float deltaXCarry;
    private static float deltaYCarry;
    private static float wheelCarry;
    private static float eventDeltaXCarry;
    private static float eventDeltaYCarry;
    private static float eventWheelCarry;

    public static void create() throws LWJGLException { }

    public static boolean isCreated() {
        return LWJGLY.hasWindowBridge();
    }

    public static void destroy() { }

    public static void poll() { }

    public static boolean isButtonDown(int button) {
        int sdlButton = toSdlButton(button);
        return sdlButton != 0 && LWJGLY.windowBridge().mouseButtonDown(sdlButton);
    }

    public static String getButtonName(int button) {
        return button >= 0 && button < BUTTON_COUNT ? "BUTTON" + button : null;
    }

    public static int getButtonIndex(String buttonName) {
        if (buttonName != null && buttonName.startsWith("BUTTON")) {
            try {
                return Integer.parseInt(buttonName.substring("BUTTON".length()));
            } catch (NumberFormatException ignored) {
                // Unknown names map to -1
            }
        }
        return -1;
    }

    public static boolean next() {
        WindowBridge.MouseEvent event = LWJGLY.windowBridge().nextMouseEvent();
        if (event == null) {
            return false;
        }
        current = event;
        return true;
    }

    /** Returns the event button, or -1 for motion and wheel events. */
    public static int getEventButton() {
        return current == null || current.button() == 0 ? -1 : toLwjglButton(current.button());
    }

    public static boolean getEventButtonState() {
        return current != null && current.pressed();
    }

    public static int getEventDX() {
        if (current == null) {
            return 0;
        }
        float delta = current.dx() + eventDeltaXCarry;
        int whole = (int) delta;
        eventDeltaXCarry = delta - whole;
        return whole;
    }

    public static int getEventDY() {
        if (current == null) {
            return 0;
        }
        float delta = -current.dy() + eventDeltaYCarry;
        int whole = (int) delta;
        eventDeltaYCarry = delta - whole;
        return whole;
    }

    public static int getEventX() {
        return current == null ? 0 : clipX((int) current.x());
    }

    public static int getEventY() {
        return current == null ? 0 : clipY(flipY(current.y()));
    }

    public static int getEventDWheel() {
        if (current == null) {
            return 0;
        }
        float delta = current.wheel() * WHEEL_DELTA + eventWheelCarry;
        int whole = (int) delta;
        eventWheelCarry = delta - whole;
        return whole;
    }

    public static long getEventNanoseconds() {
        return current == null ? 0L : current.timestampNs();
    }

    public static int getX() {
        return LWJGLY.hasWindowBridge() ? clipX((int) LWJGLY.windowBridge().mouseX()) : 0;
    }

    public static int getY() {
        return LWJGLY.hasWindowBridge() ? clipY(flipY(LWJGLY.windowBridge().mouseY())) : 0;
    }

    public static int getDX() {
        if (!LWJGLY.hasWindowBridge()) {
            return 0;
        }
        float delta = LWJGLY.windowBridge().takeMouseDeltaX() + deltaXCarry;
        int whole = (int) delta;
        deltaXCarry = delta - whole;
        return whole;
    }

    public static int getDY() {
        if (!LWJGLY.hasWindowBridge()) {
            return 0;
        }
        float delta = -LWJGLY.windowBridge().takeMouseDeltaY() + deltaYCarry;
        int whole = (int) delta;
        deltaYCarry = delta - whole;
        return whole;
    }

    public static int getDWheel() {
        if (!LWJGLY.hasWindowBridge()) {
            return 0;
        }
        float delta = LWJGLY.windowBridge().takeMouseWheel() * WHEEL_DELTA + wheelCarry;
        int whole = (int) delta;
        wheelCarry = delta - whole;
        return whole;
    }

    public static int getButtonCount() {
        return BUTTON_COUNT;
    }

    public static boolean hasWheel() {
        return true;
    }

    public static boolean isGrabbed() {
        return LWJGLY.hasWindowBridge() && LWJGLY.windowBridge().mouseGrabbed();
    }

    public static void setGrabbed(boolean grabbed) {
        LWJGLY.windowBridge().grabMouse(grabbed);
    }

    public static void setCursorPosition(int x, int y) {
        LWJGLY.windowBridge().mousePosition(x, LWJGLY.windowBridge().height() - 1 - y);
    }

    public static boolean isClipMouseCoordinatesToWindow() {
        return clipToWindow;
    }

    public static void setClipMouseCoordinatesToWindow(boolean clip) {
        clipToWindow = clip;
    }

    /** Advances an animated cursor when its next frame is due. */
    public static void updateCursor() {
        Cursor cursor = currentCursor;
        if (cursor != null && cursor.hasTimedOut()) {
            cursor.nextCursor();
            cursor.setTimeout();
            SDLMouse.SDL_SetCursor(cursor.handle());
        }
    }

    public static boolean isInsideWindow() {
        return LWJGLY.windowBridge().mouseInside();
    }

    public static Cursor getNativeCursor() {
        return currentCursor;
    }

    /**
     * Installs a cursor, or restores the system cursor for null.
     *
     * @return the previously installed cursor
     */
    public static Cursor setNativeCursor(Cursor cursor) throws LWJGLException {
        Cursor previous = currentCursor;
        currentCursor = cursor;
        if (cursor == null) {
            // SDL restores the default by setting the default cursor again
            SDLMouse.SDL_SetCursor(SDLMouse.SDL_GetDefaultCursor());
        } else {
            cursor.setTimeout();
            if (!SDLMouse.SDL_SetCursor(cursor.handle())) {
                currentCursor = previous;
                throw new LWJGLException("SDL_SetCursor failed: " + SDLError.SDL_GetError());
            }
        }
        return previous;
    }

    private static int clipX(int x) {
        return clipToWindow ? Math.min(LWJGLY.windowBridge().width() - 1, Math.max(0, x)) : x;
    }

    private static int clipY(int y) {
        return clipToWindow ? Math.min(LWJGLY.windowBridge().height() - 1, Math.max(0, y)) : y;
    }

    private static int flipY(float sdlY) {
        return LWJGLY.windowBridge().height() - 1 - (int) sdlY;
    }

    private static int toSdlButton(int lwjglButton) {
        return switch (lwjglButton) {
            case 0 -> SDLMouse.SDL_BUTTON_LEFT;
            case 1 -> SDLMouse.SDL_BUTTON_RIGHT;
            case 2 -> SDLMouse.SDL_BUTTON_MIDDLE;
            // Later buttons use the same order with a one-based SDL index
            default -> lwjglButton >= 0 && lwjglButton < BUTTON_COUNT ? lwjglButton + 1 : 0;
        };
    }

    private static int toLwjglButton(int sdlButton) {
        if (sdlButton == SDLMouse.SDL_BUTTON_LEFT) {
            return 0;
        }
        if (sdlButton == SDLMouse.SDL_BUTTON_RIGHT) {
            return 1;
        }
        if (sdlButton == SDLMouse.SDL_BUTTON_MIDDLE) {
            return 2;
        }
        return sdlButton - 1;
    }

    protected Mouse() { }

}
