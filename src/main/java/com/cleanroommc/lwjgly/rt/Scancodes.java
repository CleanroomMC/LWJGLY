package com.cleanroommc.lwjgly.rt;

import org.lwjgl.input.KeyboardConstants;
import org.lwjgl.sdl.SDLScancode;

/** Translates between SDL scancodes and LWJGL 2 DirectInput key codes. */
public final class Scancodes {

    private static final int MAX_SDL_SCANCODE = 512;
    private static final int[] SDL_TO_LWJGL = new int[MAX_SDL_SCANCODE];
    private static final int[] LWJGL_TO_SDL = new int[256];

    static {
        // LWJGL 2 digit codes follow the top row, so 0 comes after 9
        map(SDLScancode.SDL_SCANCODE_A, KeyboardConstants.KEY_A);
        map(SDLScancode.SDL_SCANCODE_B, KeyboardConstants.KEY_B);
        map(SDLScancode.SDL_SCANCODE_C, KeyboardConstants.KEY_C);
        map(SDLScancode.SDL_SCANCODE_D, KeyboardConstants.KEY_D);
        map(SDLScancode.SDL_SCANCODE_E, KeyboardConstants.KEY_E);
        map(SDLScancode.SDL_SCANCODE_F, KeyboardConstants.KEY_F);
        map(SDLScancode.SDL_SCANCODE_G, KeyboardConstants.KEY_G);
        map(SDLScancode.SDL_SCANCODE_H, KeyboardConstants.KEY_H);
        map(SDLScancode.SDL_SCANCODE_I, KeyboardConstants.KEY_I);
        map(SDLScancode.SDL_SCANCODE_J, KeyboardConstants.KEY_J);
        map(SDLScancode.SDL_SCANCODE_K, KeyboardConstants.KEY_K);
        map(SDLScancode.SDL_SCANCODE_L, KeyboardConstants.KEY_L);
        map(SDLScancode.SDL_SCANCODE_M, KeyboardConstants.KEY_M);
        map(SDLScancode.SDL_SCANCODE_N, KeyboardConstants.KEY_N);
        map(SDLScancode.SDL_SCANCODE_O, KeyboardConstants.KEY_O);
        map(SDLScancode.SDL_SCANCODE_P, KeyboardConstants.KEY_P);
        map(SDLScancode.SDL_SCANCODE_Q, KeyboardConstants.KEY_Q);
        map(SDLScancode.SDL_SCANCODE_R, KeyboardConstants.KEY_R);
        map(SDLScancode.SDL_SCANCODE_S, KeyboardConstants.KEY_S);
        map(SDLScancode.SDL_SCANCODE_T, KeyboardConstants.KEY_T);
        map(SDLScancode.SDL_SCANCODE_U, KeyboardConstants.KEY_U);
        map(SDLScancode.SDL_SCANCODE_V, KeyboardConstants.KEY_V);
        map(SDLScancode.SDL_SCANCODE_W, KeyboardConstants.KEY_W);
        map(SDLScancode.SDL_SCANCODE_X, KeyboardConstants.KEY_X);
        map(SDLScancode.SDL_SCANCODE_Y, KeyboardConstants.KEY_Y);
        map(SDLScancode.SDL_SCANCODE_Z, KeyboardConstants.KEY_Z);

        map(SDLScancode.SDL_SCANCODE_1, KeyboardConstants.KEY_1);
        map(SDLScancode.SDL_SCANCODE_2, KeyboardConstants.KEY_2);
        map(SDLScancode.SDL_SCANCODE_3, KeyboardConstants.KEY_3);
        map(SDLScancode.SDL_SCANCODE_4, KeyboardConstants.KEY_4);
        map(SDLScancode.SDL_SCANCODE_5, KeyboardConstants.KEY_5);
        map(SDLScancode.SDL_SCANCODE_6, KeyboardConstants.KEY_6);
        map(SDLScancode.SDL_SCANCODE_7, KeyboardConstants.KEY_7);
        map(SDLScancode.SDL_SCANCODE_8, KeyboardConstants.KEY_8);
        map(SDLScancode.SDL_SCANCODE_9, KeyboardConstants.KEY_9);
        map(SDLScancode.SDL_SCANCODE_0, KeyboardConstants.KEY_0);

        map(SDLScancode.SDL_SCANCODE_RETURN, KeyboardConstants.KEY_RETURN);
        map(SDLScancode.SDL_SCANCODE_ESCAPE, KeyboardConstants.KEY_ESCAPE);
        map(SDLScancode.SDL_SCANCODE_BACKSPACE, KeyboardConstants.KEY_BACK);
        map(SDLScancode.SDL_SCANCODE_TAB, KeyboardConstants.KEY_TAB);
        map(SDLScancode.SDL_SCANCODE_SPACE, KeyboardConstants.KEY_SPACE);
        map(SDLScancode.SDL_SCANCODE_MINUS, KeyboardConstants.KEY_MINUS);
        map(SDLScancode.SDL_SCANCODE_EQUALS, KeyboardConstants.KEY_EQUALS);
        map(SDLScancode.SDL_SCANCODE_LEFTBRACKET, KeyboardConstants.KEY_LBRACKET);
        map(SDLScancode.SDL_SCANCODE_RIGHTBRACKET, KeyboardConstants.KEY_RBRACKET);
        map(SDLScancode.SDL_SCANCODE_BACKSLASH, KeyboardConstants.KEY_BACKSLASH);
        map(SDLScancode.SDL_SCANCODE_SEMICOLON, KeyboardConstants.KEY_SEMICOLON);
        map(SDLScancode.SDL_SCANCODE_APOSTROPHE, KeyboardConstants.KEY_APOSTROPHE);
        map(SDLScancode.SDL_SCANCODE_GRAVE, KeyboardConstants.KEY_GRAVE);
        map(SDLScancode.SDL_SCANCODE_COMMA, KeyboardConstants.KEY_COMMA);
        map(SDLScancode.SDL_SCANCODE_PERIOD, KeyboardConstants.KEY_PERIOD);
        map(SDLScancode.SDL_SCANCODE_SLASH, KeyboardConstants.KEY_SLASH);
        map(SDLScancode.SDL_SCANCODE_CAPSLOCK, KeyboardConstants.KEY_CAPITAL);

        map(SDLScancode.SDL_SCANCODE_F1, KeyboardConstants.KEY_F1);
        map(SDLScancode.SDL_SCANCODE_F2, KeyboardConstants.KEY_F2);
        map(SDLScancode.SDL_SCANCODE_F3, KeyboardConstants.KEY_F3);
        map(SDLScancode.SDL_SCANCODE_F4, KeyboardConstants.KEY_F4);
        map(SDLScancode.SDL_SCANCODE_F5, KeyboardConstants.KEY_F5);
        map(SDLScancode.SDL_SCANCODE_F6, KeyboardConstants.KEY_F6);
        map(SDLScancode.SDL_SCANCODE_F7, KeyboardConstants.KEY_F7);
        map(SDLScancode.SDL_SCANCODE_F8, KeyboardConstants.KEY_F8);
        map(SDLScancode.SDL_SCANCODE_F9, KeyboardConstants.KEY_F9);
        map(SDLScancode.SDL_SCANCODE_F10, KeyboardConstants.KEY_F10);
        map(SDLScancode.SDL_SCANCODE_F11, KeyboardConstants.KEY_F11);
        map(SDLScancode.SDL_SCANCODE_F12, KeyboardConstants.KEY_F12);
        map(SDLScancode.SDL_SCANCODE_F13, KeyboardConstants.KEY_F13);
        map(SDLScancode.SDL_SCANCODE_F14, KeyboardConstants.KEY_F14);
        map(SDLScancode.SDL_SCANCODE_F15, KeyboardConstants.KEY_F15);

        map(SDLScancode.SDL_SCANCODE_PRINTSCREEN, KeyboardConstants.KEY_SYSRQ);
        map(SDLScancode.SDL_SCANCODE_SCROLLLOCK, KeyboardConstants.KEY_SCROLL);
        map(SDLScancode.SDL_SCANCODE_PAUSE, KeyboardConstants.KEY_PAUSE);
        map(SDLScancode.SDL_SCANCODE_INSERT, KeyboardConstants.KEY_INSERT);
        map(SDLScancode.SDL_SCANCODE_HOME, KeyboardConstants.KEY_HOME);
        map(SDLScancode.SDL_SCANCODE_PAGEUP, KeyboardConstants.KEY_PRIOR);
        map(SDLScancode.SDL_SCANCODE_DELETE, KeyboardConstants.KEY_DELETE);
        map(SDLScancode.SDL_SCANCODE_END, KeyboardConstants.KEY_END);
        map(SDLScancode.SDL_SCANCODE_PAGEDOWN, KeyboardConstants.KEY_NEXT);
        map(SDLScancode.SDL_SCANCODE_RIGHT, KeyboardConstants.KEY_RIGHT);
        map(SDLScancode.SDL_SCANCODE_LEFT, KeyboardConstants.KEY_LEFT);
        map(SDLScancode.SDL_SCANCODE_DOWN, KeyboardConstants.KEY_DOWN);
        map(SDLScancode.SDL_SCANCODE_UP, KeyboardConstants.KEY_UP);

        map(SDLScancode.SDL_SCANCODE_NUMLOCKCLEAR, KeyboardConstants.KEY_NUMLOCK);
        map(SDLScancode.SDL_SCANCODE_KP_DIVIDE, KeyboardConstants.KEY_DIVIDE);
        map(SDLScancode.SDL_SCANCODE_KP_MULTIPLY, KeyboardConstants.KEY_MULTIPLY);
        map(SDLScancode.SDL_SCANCODE_KP_MINUS, KeyboardConstants.KEY_SUBTRACT);
        map(SDLScancode.SDL_SCANCODE_KP_PLUS, KeyboardConstants.KEY_ADD);
        map(SDLScancode.SDL_SCANCODE_KP_ENTER, KeyboardConstants.KEY_NUMPADENTER);
        map(SDLScancode.SDL_SCANCODE_KP_1, KeyboardConstants.KEY_NUMPAD1);
        map(SDLScancode.SDL_SCANCODE_KP_2, KeyboardConstants.KEY_NUMPAD2);
        map(SDLScancode.SDL_SCANCODE_KP_3, KeyboardConstants.KEY_NUMPAD3);
        map(SDLScancode.SDL_SCANCODE_KP_4, KeyboardConstants.KEY_NUMPAD4);
        map(SDLScancode.SDL_SCANCODE_KP_5, KeyboardConstants.KEY_NUMPAD5);
        map(SDLScancode.SDL_SCANCODE_KP_6, KeyboardConstants.KEY_NUMPAD6);
        map(SDLScancode.SDL_SCANCODE_KP_7, KeyboardConstants.KEY_NUMPAD7);
        map(SDLScancode.SDL_SCANCODE_KP_8, KeyboardConstants.KEY_NUMPAD8);
        map(SDLScancode.SDL_SCANCODE_KP_9, KeyboardConstants.KEY_NUMPAD9);
        map(SDLScancode.SDL_SCANCODE_KP_0, KeyboardConstants.KEY_NUMPAD0);
        map(SDLScancode.SDL_SCANCODE_KP_PERIOD, KeyboardConstants.KEY_DECIMAL);
        map(SDLScancode.SDL_SCANCODE_KP_EQUALS, KeyboardConstants.KEY_NUMPADEQUALS);
        map(SDLScancode.SDL_SCANCODE_KP_COMMA, KeyboardConstants.KEY_NUMPADCOMMA);

        // LWJGL 2 calls the Windows or Command key META. SDL calls it GUI
        map(SDLScancode.SDL_SCANCODE_LCTRL, KeyboardConstants.KEY_LCONTROL);
        map(SDLScancode.SDL_SCANCODE_LSHIFT, KeyboardConstants.KEY_LSHIFT);
        map(SDLScancode.SDL_SCANCODE_LALT, KeyboardConstants.KEY_LMENU);
        map(SDLScancode.SDL_SCANCODE_LGUI, KeyboardConstants.KEY_LMETA);
        map(SDLScancode.SDL_SCANCODE_RCTRL, KeyboardConstants.KEY_RCONTROL);
        map(SDLScancode.SDL_SCANCODE_RSHIFT, KeyboardConstants.KEY_RSHIFT);
        map(SDLScancode.SDL_SCANCODE_RALT, KeyboardConstants.KEY_RMENU);
        map(SDLScancode.SDL_SCANCODE_RGUI, KeyboardConstants.KEY_RMETA);
        map(SDLScancode.SDL_SCANCODE_APPLICATION, KeyboardConstants.KEY_APPS);
        map(SDLScancode.SDL_SCANCODE_POWER, KeyboardConstants.KEY_POWER);
        map(SDLScancode.SDL_SCANCODE_SLEEP, KeyboardConstants.KEY_SLEEP);
    }

    /** LWJGL 2's key code for an SDL scancode, or {@code KEY_NONE} for keys LWJGL 2 never had. */
    public static int toLwjgl(int sdlScancode) {
        return sdlScancode >= 0 && sdlScancode < MAX_SDL_SCANCODE ? SDL_TO_LWJGL[sdlScancode] : KeyboardConstants.KEY_NONE;
    }

    /** The SDL scancode for an LWJGL 2 key code, or 0 when there is no equivalent. */
    public static int toSdl(int lwjglKey) {
        return lwjglKey >= 0 && lwjglKey < LWJGL_TO_SDL.length ? LWJGL_TO_SDL[lwjglKey] : 0;
    }

    private static void map(int sdlScancode, int lwjglKey) {
        SDL_TO_LWJGL[sdlScancode] = lwjglKey;
        LWJGL_TO_SDL[lwjglKey] = sdlScancode;
    }

    private Scancodes() { }

}
