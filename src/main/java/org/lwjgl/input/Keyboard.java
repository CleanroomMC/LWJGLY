package org.lwjgl.input;

import com.cleanroommc.lwjgly.LWJGLY;
import com.cleanroommc.lwjgly.rt.Scancodes;
import com.cleanroommc.lwjgly.spi.WindowBridge;
import org.lwjgl.LWJGLException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/** LWJGL 2 keyboard API backed by Cleanroom's SDL event queue. */
public class Keyboard implements KeyboardConstants {

    public static final int EVENT_SIZE = 4 + 1 + 4 + 8 + 1;
    public static final int CHAR_NONE = '\0';
    public static final int KEYBOARD_SIZE = 256;

    private static final String[] KEY_NAMES = new String[KEYBOARD_SIZE];
    private static final Map<String, Integer> KEY_INDICES = new HashMap<>();

    private static WindowBridge.KeyEvent current;
    private static boolean repeatEvents;

    static {
        for (Field field : Keyboard.class.getFields()) {
            if (field.getType() == int.class && field.getName().startsWith("KEY_") && Modifier.isStatic(field.getModifiers())) {
                try {
                    int value = field.getInt(null);
                    String name = field.getName().substring("KEY_".length());
                    // Keep the first name for aliased key codes
                    if (value >= 0 && value < KEYBOARD_SIZE && KEY_NAMES[value] == null) {
                        KEY_NAMES[value] = name;
                    }
                    KEY_INDICES.putIfAbsent(name, value);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Cannot read " + field.getName(), e);
                }
            }
        }
    }

    public static void create() throws LWJGLException { }

    public static boolean isCreated() {
        return LWJGLY.hasWindowBridge();
    }

    public static void destroy() { }

    public static void poll() { }

    public static boolean isKeyDown(int key) {
        int scancode = Scancodes.toSdl(key);
        return scancode != 0 && LWJGLY.windowBridge().keyDown(scancode);
    }

    public static String getKeyName(int key) {
        if (key < 0 || key >= KEYBOARD_SIZE) {
            return "NONE";
        }
        String name = KEY_NAMES[key];
        return name == null ? "NONE" : name;
    }

    public static int getKeyIndex(String keyName) {
        return KEY_INDICES.getOrDefault(keyName, KEY_NONE);
    }

    public static int getNumKeyboardEvents() {
        return LWJGLY.windowBridge().queuedKeyEvents();
    }

    public static boolean next() {
        WindowBridge bridge = LWJGLY.windowBridge();
        while (true) {
            WindowBridge.KeyEvent event = bridge.nextKeyEvent();
            if (event == null) {
                current = null;
                return false;
            }
            if (event.repeat() && !repeatEvents) {
                continue;
            }
            current = event;
            return true;
        }
    }

    public static void enableRepeatEvents(boolean enable) {
        repeatEvents = enable;
    }

    public static boolean areRepeatEventsEnabled() {
        return repeatEvents;
    }

    public static int getKeyCount() {
        return KEYBOARD_SIZE;
    }

    public static char getEventCharacter() {
        return current == null ? CHAR_NONE : current.character();
    }

    public static int getEventKey() {
        return current == null ? KEY_NONE : Scancodes.toLwjgl(current.scancode());
    }

    public static boolean getEventKeyState() {
        return current != null && current.pressed();
    }

    public static long getEventNanoseconds() {
        return current == null ? 0L : current.timestampNs();
    }

    public static boolean isRepeatEvent() {
        return current != null && current.repeat();
    }

    protected Keyboard() { }

}
