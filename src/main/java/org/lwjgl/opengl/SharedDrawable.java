package org.lwjgl.opengl;

import org.lwjgl.LWJGLException;

/** LWJGL 2's shared drawable over a context the window bridge creates. */
public final class SharedDrawable extends DrawableGL {

    /**
     * Shares with the host window's context.
     * Every drawable LWJGLY hands out belongs to that share group,
     * so {@code drawable} only needs to be non-null.
     */
    public SharedDrawable(Drawable drawable) throws LWJGLException {
        if (drawable == null) {
            throw new NullPointerException("drawable cannot be null");
        }
        this.context = ContextGL.create();
    }

    /** LWJGL 2 throws here too. */
    @Override
    public ContextGL createSharedContext() {
        throw new UnsupportedOperationException();
    }

}
