package org.lwjgl.util;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.DisplayMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Filters and selects LWJGL 2 display modes. */
public final class Display {

    /** Returns modes within the supplied bounds. A bound of -1 is ignored. */
    public static DisplayMode[] getAvailableDisplayModes(int minWidth, int minHeight, int maxWidth, int maxHeight,
                                                         int minBPP, int maxBPP, int minFreq, int maxFreq) throws LWJGLException {
        List<DisplayMode> matches = new ArrayList<>();
        for (DisplayMode mode : org.lwjgl.opengl.Display.getAvailableDisplayModes()) {
            if (beyond(mode.getWidth(), minWidth, maxWidth) ||
                    beyond(mode.getHeight(), minHeight, maxHeight) ||
                    beyond(mode.getBitsPerPixel(), minBPP, maxBPP)) {
                continue;
            }
            if (mode.getFrequency() != 0 && beyond(mode.getFrequency(), minFreq, maxFreq)) {
                continue;
            }
            matches.add(mode);
        }
        return matches.toArray(new DisplayMode[0]);
    }

    /**
     * Sorts {@code modes} by the stated preference and sets the first one that works.
     *
     * <p>Entries use {@code "width"} for ascending order, {@code "-width"} for descending order,
     * or {@code "width=1024"} to sort by distance from a preferred value.
     *
     * <p>Earlier entries take precedence and ties use the next entry.
     *
     * @throws NoSuchFieldException if a name is not one of the four
     * @throws Exception            if every mode was rejected
     */
    public static DisplayMode setDisplayMode(DisplayMode[] modes, String[] order) throws Exception {
        Preference[] preferences = new Preference[order.length];
        for (int i = 0; i < order.length; i++) {
            preferences[i] = Preference.parse(order[i]);
        }

        DisplayMode[] sorted = modes.clone();
        Arrays.sort(sorted, comparator(preferences));

        for (DisplayMode mode : sorted) {
            try {
                org.lwjgl.opengl.Display.setDisplayMode(mode);
                return mode;
            } catch (Exception rejected) {
                // Try the next mode and fail only after all candidates are rejected
            }
        }
        throw new Exception("Failed to set display mode.");
    }

    private static boolean beyond(int value, int min, int max) {
        return (min != -1 && value < min) || (max != -1 && value > max);
    }

    private static Comparator<DisplayMode> comparator(Preference[] preferences) {
        return (left, right) -> {
            for (Preference preference : preferences) {
                int result = preference.compare(left, right);
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        };
    }

    /** Parses one display-mode preference entry. */
    private record Preference(String property, int direction, int target, boolean hasTarget) {

        private static Preference parse(String spec) throws NoSuchFieldException {
            int equals = spec.indexOf('=');
            Preference parsed;
            if (equals > 0) {
                parsed = new Preference(spec.substring(0, equals), 0, Integer.parseInt(spec.substring(equals + 1)), true);
            } else if (spec.charAt(0) == '-') {
                parsed = new Preference(spec.substring(1), -1, 0, false);
            } else {
                parsed = new Preference(spec, 1, 0, false);
            }
            parsed.valueOf(new DisplayMode(0, 0)); // Rejects an unknown name before any sorting starts
            return parsed;
        }

        private int valueOf(DisplayMode mode) throws NoSuchFieldException {
            return switch (property) {
                case "width" -> mode.getWidth();
                case "height" -> mode.getHeight();
                case "bpp" -> mode.getBitsPerPixel();
                case "freq" -> mode.getFrequency();
                default -> throw new NoSuchFieldException(property);
            };
        }

        private int compare(DisplayMode left, DisplayMode right) {
            int a, b;
            try {
                a = valueOf(left);
                b = valueOf(right);
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException(e); // parse() already rejected this
            }
            if (a == b) {
                return 0;
            }
            if (hasTarget) {
                if (a == target) {
                    return -1;
                }
                if (b == target) {
                    return 1;
                }
                return Integer.compare(Math.abs(a - target), Math.abs(b - target));
            }
            return a < b ? direction : -direction;
        }
    }

    private Display() { }

}
