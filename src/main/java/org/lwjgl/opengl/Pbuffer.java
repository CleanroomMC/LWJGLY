package org.lwjgl.opengl;

import org.lwjgl.LWJGLException;

/**
 * LWJGL 2's off-screen drawable, as a framebuffer object in a context the window bridge shares with the host's.
 *
 * <p>Platform pbuffers are gone from SDL, and a hidden window's pixels fail the ownership test on some drivers.
 * Render to texture was a WGL-only extension, so {@link #getCapabilities()} withholds it.
 */
public final class Pbuffer extends DrawableGL implements PbufferConstants {

    private final int width;
    private final int height;
    private final PixelFormat format;
    private int framebuffer;
    private int colorBuffer;
    private int depthBuffer;

    public Pbuffer(int width, int height, PixelFormat pixelFormat, Drawable sharedDrawable) throws LWJGLException {
        this(width, height, pixelFormat, null, sharedDrawable, null);
    }

    public Pbuffer(int width, int height, PixelFormat pixelFormat, RenderTexture renderTexture, Drawable sharedDrawable)
            throws LWJGLException {
        this(width, height, pixelFormat, renderTexture, sharedDrawable, null);
    }

    /** Every context LWJGLY creates shares with the host's, so {@code sharedDrawable} and {@code attribs} change nothing. */
    public Pbuffer(int width, int height, PixelFormat pixelFormat, RenderTexture renderTexture, Drawable sharedDrawable,
                   ContextAttribs attribs) throws LWJGLException {
        if (pixelFormat == null) {
            throw new NullPointerException("Pixel format must be non-null");
        }
        if (renderTexture != null) {
            throw new LWJGLException("Render to texture is not supported");
        }
        this.width = width;
        this.height = height;
        this.format = pixelFormat;
        setPixelFormat(pixelFormat, attribs);
        this.context = ContextGL.create();
    }

    public static int getCapabilities() {
        try {
            GLCapabilities capabilities = GL.getCapabilities();
            return capabilities.OpenGL30 || capabilities.GL_ARB_framebuffer_object ? PBUFFER_SUPPORTED : 0;
        } catch (IllegalStateException noContext) {
            return 0;
        }
    }

    /** A framebuffer object is never lost the way a platform pbuffer was on a mode switch. */
    public synchronized boolean isBufferLost() {
        return false;
    }

    /** The attributes only steer render to texture, which no pbuffer here has. */
    public synchronized void setAttrib(int attrib, int value) { }

    public synchronized void bindTexImage(int buffer) { }

    public synchronized void releaseTexImage(int buffer) { }

    public synchronized int getWidth() {
        return width;
    }

    public synchronized int getHeight() {
        return height;
    }

    @Override
    public void makeCurrent() throws LWJGLException {
        super.makeCurrent();
        if (framebuffer == 0) {
            createFramebuffer();
        }
    }

    @Override
    public void destroy() {
        if (context != null && colorBuffer != 0) {
            // Renderbuffers are shared, so any current context in the group can delete them; the FBO dies with ours
            try {
                if (!Display.isCurrent()) {
                    context.makeCurrent();
                }
                GL30.glDeleteRenderbuffers(colorBuffer);
                if (depthBuffer != 0) {
                    GL30.glDeleteRenderbuffers(depthBuffer);
                }
            } catch (LWJGLException ignored) {
                // Leaks the renderbuffers, the context still goes below
            }
        }
        super.destroy();
    }

    private void createFramebuffer() throws LWJGLException {
        GLCapabilities capabilities = GL.getCapabilities();
        if (!capabilities.OpenGL30 && !capabilities.GL_ARB_framebuffer_object) {
            throw new LWJGLException("Pbuffer needs framebuffer objects, which this context lacks");
        }
        framebuffer = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);

        colorBuffer = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, colorBuffer);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, format.isFloatingPoint() ? GL30.GL_RGBA32F : GL11.GL_RGBA8, width, height);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_RENDERBUFFER, colorBuffer);

        if (format.getDepthBits() > 0 || format.getStencilBits() > 0) {
            depthBuffer = GL30.glGenRenderbuffers();
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthBuffer);
            GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH24_STENCIL8, width, height);
            GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER, depthBuffer);
        }
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);

        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new LWJGLException("Pbuffer framebuffer is incomplete: 0x" + Integer.toHexString(status));
        }
        // A new context's viewport and scissor box start at its drawable's size, the hidden surface's here
        GL11.glViewport(0, 0, width, height);
        GL11.glScissor(0, 0, width, height);
    }

}
