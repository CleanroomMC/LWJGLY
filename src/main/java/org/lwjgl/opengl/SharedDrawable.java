package org.lwjgl.opengl;

import com.cleanroommc.lwjgly.LWJGLY;
import com.cleanroommc.lwjgly.spi.WindowBridge;
import org.lwjgl.LWJGLException;
import org.lwjgl.PointerBuffer;

/** LWJGL 2's shared drawable over a context the window bridge creates. */
public final class SharedDrawable extends DrawableGL {

    private final WindowBridge.SharedContext context;

    /**
     * Shares with the host window's context.
     * Every drawable LWJGLY hands out belongs to that share group,
     * so {@code drawable} only needs to be non-null.
     */
    public SharedDrawable(Drawable drawable) throws LWJGLException {
        if (drawable == null) {
            throw new NullPointerException("drawable cannot be null");
        }
        try {
            this.context = LWJGLY.windowBridge().createSharedContext();
        } catch (RuntimeException e) {
            throw new LWJGLException("Could not create a shared GL context", e);
        }
    }

    public ContextGL createSharedContext() {
        throw new UnsupportedOperationException("org.lwjgl.opengl.SharedDrawable.createSharedContext is not implemented by LWJGLY");
    }

    @Override
    public boolean isCurrent() {
        return this.context.isCurrent();
    }

    @Override
    public void makeCurrent() {
        this.context.makeCurrent();
    }

    @Override
    public void releaseContext() {
        this.context.release();
    }

    @Override
    public void destroy() {
        this.context.close();
    }

    @Override
    public void setCLSharingProperties(PointerBuffer properties) throws LWJGLException {
        throw new LWJGLException("OpenCL context sharing is not supported");
    }

}
