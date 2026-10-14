package org.lwjgl.opengl;

import com.cleanroommc.lwjgly.LWJGLY;
import org.lwjgl.LWJGLException;
import org.lwjgl.PointerBuffer;

/** LWJGL 2's base for drawables that own a context, which here shares objects with the host's. */
abstract class DrawableGL implements DrawableLWJGL {

    ContextGL context;
    private PixelFormat pixelFormat;

    /** LWJGL 2 throws here too, a drawable only takes a pixel format with its context attributes. */
    @Override
    public void setPixelFormat(PixelFormatLWJGL pixelFormat) throws LWJGLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPixelFormat(PixelFormatLWJGL pixelFormat, ContextAttribs attribs) throws LWJGLException {
        this.pixelFormat = (PixelFormat) pixelFormat;
    }

    @Override
    public PixelFormatLWJGL getPixelFormat() {
        return pixelFormat;
    }

    @Override
    public ContextGL getContext() {
        return context;
    }

    @Override
    public ContextGL createSharedContext() throws LWJGLException {
        checkDestroyed();
        return ContextGL.create();
    }

    @Override
    public void checkGLError() {
        Util.checkGLError();
    }

    /** Applies to whichever context is current, as LWJGL 2's did. */
    @Override
    public void setSwapInterval(int value) {
        LWJGLY.windowBridge().vsync(value != 0);
    }

    /** The hidden surface behind the context has nothing to present. */
    @Override
    public void swapBuffers() throws LWJGLException { }

    @Override
    public void initContext(float r, float g, float b) {
        GL11.glClearColor(r, g, b, 0.0F);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    @Override
    public boolean isCurrent() throws LWJGLException {
        checkDestroyed();
        return context.isCurrent();
    }

    @Override
    public void makeCurrent() throws LWJGLException {
        checkDestroyed();
        context.makeCurrent();
    }

    @Override
    public void releaseContext() throws LWJGLException {
        checkDestroyed();
        if (context.isCurrent()) {
            context.releaseCurrent();
        }
    }

    @Override
    public void destroy() {
        if (context == null) {
            return;
        }
        try {
            releaseContext();
        } catch (LWJGLException ignored) {
            // The context is destroyed below either way
        }
        context.destroy();
        context = null;
    }

    @Override
    public void setCLSharingProperties(PointerBuffer properties) throws LWJGLException {
        throw new LWJGLException("OpenCL context sharing is not supported");
    }

    protected final void checkDestroyed() {
        if (context == null) {
            throw new IllegalStateException("Cannot operate on destroyed drawable");
        }
    }

}
