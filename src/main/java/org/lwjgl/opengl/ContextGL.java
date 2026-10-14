package org.lwjgl.opengl;

import com.cleanroommc.lwjgly.LWJGLY;
import com.cleanroommc.lwjgly.spi.WindowBridge;
import org.lwjgl.LWJGLException;

/** LWJGL 2's GL context, over one the window bridge creates in the host's share group. */
final class ContextGL implements Context {

    private final WindowBridge.SharedContext context;

    private ContextGL(WindowBridge.SharedContext context) {
        this.context = context;
    }

    static ContextGL create() throws LWJGLException {
        try {
            return new ContextGL(LWJGLY.windowBridge().createSharedContext());
        } catch (RuntimeException e) {
            throw new LWJGLException("Could not create a shared GL context", e);
        }
    }

    @Override
    public boolean isCurrent() {
        return context.isCurrent();
    }

    @Override
    public void makeCurrent() throws LWJGLException {
        try {
            context.makeCurrent();
        } catch (RuntimeException e) {
            throw new LWJGLException("Could not make the shared GL context current", e);
        }
    }

    @Override
    public void releaseCurrent() throws LWJGLException {
        try {
            context.release();
        } catch (RuntimeException e) {
            throw new LWJGLException("Could not release the shared GL context", e);
        }
    }

    /** LWJGL 2 only detached the drawable on macOS, where SDL keeps the surface attached instead. */
    @Override
    public void releaseDrawable() { }

    void destroy() {
        context.close();
    }

}
