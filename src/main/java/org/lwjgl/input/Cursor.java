package org.lwjgl.input;

import org.lwjgl.LWJGLException;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLPixels;
import org.lwjgl.sdl.SDLSurface;
import org.lwjgl.sdl.SDL_Surface;

import java.nio.IntBuffer;

/**
 * LWJGL 2 cursor API backed by SDL.
 * Converts bottom-left cursor images and hotspots to SDL's top-left origin.
 */
public class Cursor {

    /** 1-bit transparency. */
    public static final int CURSOR_ONE_BIT_TRANSPARENCY = 1;

    /** 8-bit alpha. */
    public static final int CURSOR_8_BIT_ALPHA = 2;

    /** Animated cursors. */
    public static final int CURSOR_ANIMATION = 4;

    /** One SDL cursor frame and its display duration. */
    private record Frame(long cursor, long surface, long delayMillis) { }

    private final Frame[] frames;

    private int index;
    private long timeout;
    private boolean destroyed;

    /**
     * Builds a cursor and copies the supplied image and delay data.
     *
     * @param width     image width in pixels
     * @param height    image height in pixels
     * @param xHotspot  hotspot x, from the left
     * @param yHotspot  hotspot y, from the <em>bottom</em>
     * @param numImages how many frames follow one another in {@code images}
     * @param images    ARGB pixels, {@code numImages} frames of {@code width * height}, bottom row first
     * @param delays    per-frame delay in milliseconds, or null when {@code numImages} is 1
     */
    public Cursor(int width, int height, int xHotspot, int yHotspot, int numImages, IntBuffer images, IntBuffer delays) throws LWJGLException {
        if (numImages < 1) {
            throw new IllegalArgumentException("There must be at least 1 cursor image");
        }
        int pixels = width * height;
        if (images.remaining() < pixels * numImages) {
            throw new IllegalArgumentException("Not enough pixels in the cursor image");
        }
        if (numImages > 1 && (delays == null || delays.remaining() < numImages)) {
            throw new IllegalArgumentException("Animated cursors need one delay per image");
        }

        frames = new Frame[numImages];
        try {
            for (int frame = 0; frame < numImages; frame++) {
                frames[frame] = build(width, height, xHotspot, yHotspot, images, images.position() + frame * pixels,
                        delays == null ? 0 : delays.get(delays.position() + frame));
            }
        } catch (LWJGLException | RuntimeException e) {
            // Release SDL objects from a partially built animation
            release();
            throw e;
        }
        timeout = System.currentTimeMillis() + frames[0].delayMillis();
    }

    private static Frame build(int width, int height, int xHotspot, int yHotspot, IntBuffer images, int offset, long delay) throws LWJGLException {
        SDL_Surface surface = SDLSurface.SDL_CreateSurface(width, height, SDLPixels.SDL_PIXELFORMAT_ARGB8888);
        if (surface == null) {
            throw new LWJGLException("SDL_CreateSurface failed: " + SDLError.SDL_GetError());
        }
        long surfaceHandle = surface.address();
        try {
            if (!SDLSurface.SDL_LockSurface(surface)) {
                throw new LWJGLException("SDL_LockSurface failed: " + SDLError.SDL_GetError());
            }
            try {
                // Use the surface pitch because SDL may pad rows
                IntBuffer target = surface.pixels().asIntBuffer();
                int rowStride = surface.pitch() / Integer.BYTES;
                for (int row = 0; row < height; row++) {
                    int sourceRow = height - 1 - row; // LWJGL 2's bottom-up order into SDL's top-down
                    for (int column = 0; column < width; column++) {
                        target.put(row * rowStride + column, images.get(offset + sourceRow * width + column));
                    }
                }
            } finally {
                SDLSurface.SDL_UnlockSurface(surface);
            }

            int hotspotX = Math.clamp(xHotspot, 0, width - 1);
            int hotspotY = Math.clamp(height - 1 - yHotspot, 0, height - 1);
            long cursor = SDLMouse.SDL_CreateColorCursor(surface, hotspotX, hotspotY);
            if (cursor == 0L) {
                throw new LWJGLException("SDL_CreateColorCursor failed: " + SDLError.SDL_GetError());
            }
            return new Frame(cursor, surfaceHandle, delay);
        } catch (LWJGLException | RuntimeException e) {
            SDLSurface.SDL_DestroySurface(surface);
            throw e;
        }
    }

    /** Returns LWJGL 2's minimum cursor size. */
    public static int getMinCursorSize() {
        return 8;
    }

    /** Returns LWJGL 2's maximum cursor size. */
    public static int getMaxCursorSize() {
        return 64;
    }

    /** Returns supported cursor features. SDL does not animate cursors itself. */
    public static int getCapabilities() {
        return CURSOR_ONE_BIT_TRANSPARENCY | CURSOR_8_BIT_ALPHA;
    }

    /** Returns the current SDL cursor frame. */
    long handle() {
        checkValid();
        return frames[index].cursor();
    }

    private void checkValid() {
        if (destroyed) {
            throw new IllegalStateException("The cursor is destroyed");
        }
    }

    /** Frees SDL objects after restoring the system cursor when necessary. */
    public void destroy() {
        if (destroyed) {
            return;
        }
        if (Mouse.getNativeCursor() == this) {
            try {
                Mouse.setNativeCursor(null);
            } catch (LWJGLException e) {
                // Continue releasing SDL objects if cursor restoration fails
            }
        }
        release();
        destroyed = true;
    }

    private void release() {
        for (int i = 0; i < frames.length; i++) {
            Frame frame = frames[i];
            if (frame == null) {
                continue;
            }
            SDLMouse.SDL_DestroyCursor(frame.cursor());
            SDLSurface.nSDL_DestroySurface(frame.surface());
            frames[i] = null;
        }
    }

    /** Starts timing the current frame. */
    protected void setTimeout() {
        checkValid();
        timeout = System.currentTimeMillis() + frames[index].delayMillis();
    }

    /** Returns whether an animated cursor frame is due to advance. */
    protected boolean hasTimedOut() {
        checkValid();
        return frames.length > 1 && timeout < System.currentTimeMillis();
    }

    /** Advances to the next frame and wraps at the end. */
    protected void nextCursor() {
        checkValid();
        index = (index + 1) % frames.length;
    }

}
