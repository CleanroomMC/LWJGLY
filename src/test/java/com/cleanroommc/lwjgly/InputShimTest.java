package com.cleanroommc.lwjgly;

import com.cleanroommc.lwjgly.rt.Scancodes;
import com.cleanroommc.lwjgly.spi.WindowBridge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLScancode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SDL to LWJGL 2 input conversions.
 */
class InputShimTest {

    private MockWindow bridge;

    @BeforeEach
    void installBridge() {
        bridge = new MockWindow();
        LWJGLY.setWindowBridge(bridge);
        // Drain events left by an earlier test
        while (Keyboard.next()) { }
    }

    @Test
    void unmappedScancodesAreNoneRatherThanWrong() {
        assertEquals(Keyboard.KEY_NONE, Scancodes.toLwjgl(SDLScancode.SDL_SCANCODE_COUNT - 1));
        assertEquals(Keyboard.KEY_NONE, Scancodes.toLwjgl(0));
    }

    @Test
    void keyEvent() {
        Keyboard.enableRepeatEvents(true);
        bridge.pushKey(new WindowBridge.KeyEvent(SDLScancode.SDL_SCANCODE_W, 0, true, false, 'w', 1234L));

        assertTrue(Keyboard.next());
        assertEquals(Keyboard.KEY_W, Keyboard.getEventKey());
        assertTrue(Keyboard.getEventKeyState());
        assertEquals('w', Keyboard.getEventCharacter());
        assertEquals(1234L, Keyboard.getEventNanoseconds());
        assertFalse(Keyboard.isRepeatEvent());

        assertFalse(Keyboard.next());
    }

    @Test
    void repeatEventsAreFilteredWhenDisabled() {
        bridge.pushKey(new WindowBridge.KeyEvent(SDLScancode.SDL_SCANCODE_A, 0, true, true, 'a', 1L));
        bridge.pushKey(new WindowBridge.KeyEvent(SDLScancode.SDL_SCANCODE_B, 0, true, false, 'b', 2L));

        Keyboard.enableRepeatEvents(false);
        assertFalse(Keyboard.areRepeatEventsEnabled());
        assertTrue(Keyboard.next());
        assertEquals(Keyboard.KEY_B, Keyboard.getEventKey(), "The repeat should have been skipped");
        assertFalse(Keyboard.next());
    }

    @Test
    void keyStateAsksInSdlScancodes() {
        bridge.downScancode = SDLScancode.SDL_SCANCODE_W;
        assertTrue(Keyboard.isKeyDown(Keyboard.KEY_W));
        assertFalse(Keyboard.isKeyDown(Keyboard.KEY_A));
    }

    @Test
    void keyNames() {
        assertEquals("W", Keyboard.getKeyName(Keyboard.KEY_W));
        assertEquals(Keyboard.KEY_W, Keyboard.getKeyIndex("W"));
        assertEquals("NONE", Keyboard.getKeyName(0xFFFF));
        assertEquals(Keyboard.KEY_NONE, Keyboard.getKeyIndex("NOT_A_KEY"));
    }
    @Test
    void mouseYIsFlippedToLwjglOrigin() {
        bridge.height = 480;
        bridge.mouseY = 0; // SDL: top edge
        assertEquals(479, Mouse.getY(), "The top of the window is LWJGL 2's highest Y");
        bridge.mouseY = 479; // SDL: bottom edge
        assertEquals(0, Mouse.getY());
    }

    @Test
    void mouseDeltaYIsFlipped() {
        bridge.deltaY = 5; // SDL: moved down
        assertEquals(-5, Mouse.getDY(), "Moving down is negative in LWJGL 2");
    }

    /**
     * SDL and LWJGL 2 use different middle/right ordering.
     */
    @Test
    void buttonsAreRenumbered() {
        bridge.downButton = SDLMouse.SDL_BUTTON_RIGHT;
        assertTrue(Mouse.isButtonDown(1), "LWJGL 2's button 1 is the right button");
        assertFalse(Mouse.isButtonDown(2));
        bridge.downButton = SDLMouse.SDL_BUTTON_MIDDLE;
        assertTrue(Mouse.isButtonDown(2), "LWJGL 2's button 2 is the middle button");
    }

    @Test
    void mouseEventButtonsComeBackRenumbered() {
        bridge.pushMouse(new WindowBridge.MouseEvent(SDLMouse.SDL_BUTTON_RIGHT, true, 10, 20, 0, 0, 0, 7L));
        assertTrue(Mouse.next());
        assertEquals(1, Mouse.getEventButton());
        assertTrue(Mouse.getEventButtonState());
        assertEquals(10, Mouse.getEventX());
        assertEquals(459, Mouse.getEventY());
    }

    @Test
    void mouseEventsReportNoButton() {
        bridge.pushMouse(new WindowBridge.MouseEvent(0, false, 1, 2, 3, 4, 0, 9L));
        assertTrue(Mouse.next());
        assertEquals(-1, Mouse.getEventButton(), "LWJGL 2 reports -1 for a non-button event");
        assertEquals(-4, Mouse.getEventDY());
    }

    /**
     * One SDL notch is 120 LWJGL 2 wheel units.
     */
    @Test
    void wheelIsScaledToWindowsNotchUnits() {
        bridge.wheel = 1;
        assertEquals(120, Mouse.getDWheel());

        bridge.pushMouse(new WindowBridge.MouseEvent(0, false, 0, 0, 0, 0, -2, 1L));
        assertTrue(Mouse.next());
        assertEquals(-240, Mouse.getEventDWheel());
    }

    @Test
    void fractionalMovementIsCarriedAndNotDropped() {
        int total = 0;
        for (int frame = 0; frame < 10; frame++) {
            bridge.deltaX = 0.3F;
            total += Mouse.getDX();
        }
        assertTrue(total >= 2, "Sub-pixel movement vanished instead of accumulating: " + total);
        assertTrue(Math.abs(total - 3) <= 1, "Movement was invented rather than carried: " + total);
    }

    /**
     * Trackpads and high-resolution wheels produce fractional notches.
     */
    @Test
    void fractionalWheelIsCarriedAndNotDropped() {
        bridge.wheel = 0.25F;
        assertEquals(30, Mouse.getDWheel());
        bridge.wheel = 0.25F;
        assertEquals(30, Mouse.getDWheel());
    }

    @Test
    void keyRepeatStartsOffAsItDidInLwjgl2() {
        assertFalse(Keyboard.areRepeatEventsEnabled());
    }

    @Test
    void cursorPositionIsSetInSdlCoordinates() {
        bridge.height = 480;
        Mouse.setCursorPosition(100, 400);
        assertEquals(100F, bridge.mouseX);
        assertEquals(79F, bridge.mouseY);
    }

    @Test
    void updatePresentsTheFrameAndDrainsInput() {
        Display.update();
        assertEquals(1, bridge.swaps);
        assertEquals(1, bridge.pumps);
        Display.update(false);
        assertEquals(2, bridge.swaps);
        assertEquals(1, bridge.pumps, "update(false) must not pump");
    }

    @Test
    void displayAdoptsTheHostWindowAndNotToOpenOne() throws Exception {
        Display.create();
        assertTrue(bridge.current, "create() should bind the host's context");
        Display.destroy();
        assertFalse(bridge.current, "destroy() should release the context");
        assertTrue(Display.isCreated(), "destroy() must not close the host's window");
    }

    @Test
    void displayIsNotCurrentWhenLwjglCapabilitiesAreCleared() throws Exception {
        GL.setCapabilities(null);
        assertFalse(Display.isCurrent());
    }

    @Test
    void resizedIsConsumedOnce() {
        bridge.resized = true;
        assertTrue(Display.wasResized());
        assertFalse(Display.wasResized());
    }

    @Test
    void syncReturnsImmediatelyForNonPositiveRates() {
        long start = System.nanoTime();
        Display.sync(0);
        Display.sync(-1);
        assertTrue(System.nanoTime() - start < 100_000_000L, "sync(0) should not sleep");
    }

}
