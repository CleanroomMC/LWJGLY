package org.lwjgl.opengl;

/**
 * LWJGL 2's immutable description of the minimum framebuffer properties requested by a drawable.
 */
public final class PixelFormat implements PixelFormatLWJGL {

    private static void requireNonNegative(String property, int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Invalid number of " + property + " specified: " + value);
        }
    }

    private int bpp;
    private int alpha;
    private int depth;
    private int stencil;
    private int samples;
    private int colorSamples;
    private int numAuxBuffers;
    private int accumBpp;
    private int accumAlpha;
    private boolean stereo;
    private boolean floatingPoint;
    private boolean floatingPointPacked;
    private boolean sRGB;

    public PixelFormat() {
        this(0, 8, 0);
    }

    public PixelFormat(int alpha, int depth, int stencil) {
        this(alpha, depth, stencil, 0);
    }

    public PixelFormat(int alpha, int depth, int stencil, int samples) {
        this(0, alpha, depth, stencil, samples);
    }

    public PixelFormat(int bpp, int alpha, int depth, int stencil, int samples) {
        this(bpp, alpha, depth, stencil, samples, 0, 0, 0, false);
    }

    public PixelFormat(int bpp, int alpha, int depth, int stencil, int samples, int numAuxBuffers, int accumBpp,
                       int accumAlpha, boolean stereo) {
        this(bpp, alpha, depth, stencil, samples, numAuxBuffers, accumBpp, accumAlpha, stereo, false);
    }

    public PixelFormat(int bpp, int alpha, int depth, int stencil, int samples, int numAuxBuffers, int accumBpp,
                       int accumAlpha, boolean stereo, boolean floatingPoint) {
        this.bpp = bpp;
        this.alpha = alpha;
        this.depth = depth;
        this.stencil = stencil;
        this.samples = samples;
        this.numAuxBuffers = numAuxBuffers;
        this.accumBpp = accumBpp;
        this.accumAlpha = accumAlpha;
        this.stereo = stereo;
        this.floatingPoint = floatingPoint;
    }

    private PixelFormat(PixelFormat other) {
        bpp = other.bpp;
        alpha = other.alpha;
        depth = other.depth;
        stencil = other.stencil;
        samples = other.samples;
        colorSamples = other.colorSamples;
        numAuxBuffers = other.numAuxBuffers;
        accumBpp = other.accumBpp;
        accumAlpha = other.accumAlpha;
        stereo = other.stereo;
        floatingPoint = other.floatingPoint;
        floatingPointPacked = other.floatingPointPacked;
        sRGB = other.sRGB;
    }

    public int getBitsPerPixel() {
        return bpp;
    }

    public PixelFormat withBitsPerPixel(int bpp) {
        requireNonNegative("bits per pixel", bpp);
        PixelFormat copy = new PixelFormat(this);
        copy.bpp = bpp;
        return copy;
    }

    public int getAlphaBits() {
        return alpha;
    }

    public PixelFormat withAlphaBits(int alpha) {
        requireNonNegative("alpha bits", alpha);
        PixelFormat copy = new PixelFormat(this);
        copy.alpha = alpha;
        return copy;
    }

    public int getDepthBits() {
        return depth;
    }

    public PixelFormat withDepthBits(int depth) {
        requireNonNegative("depth bits", depth);
        PixelFormat copy = new PixelFormat(this);
        copy.depth = depth;
        return copy;
    }

    public int getStencilBits() {
        return stencil;
    }

    public PixelFormat withStencilBits(int stencil) {
        requireNonNegative("stencil bits", stencil);
        PixelFormat copy = new PixelFormat(this);
        copy.stencil = stencil;
        return copy;
    }

    public int getSamples() {
        return samples;
    }

    public PixelFormat withSamples(int samples) {
        requireNonNegative("samples", samples);
        PixelFormat copy = new PixelFormat(this);
        copy.samples = samples;
        return copy;
    }

    public PixelFormat withCoverageSamples(int colorSamples) {
        return withCoverageSamples(colorSamples, samples);
    }

    public PixelFormat withCoverageSamples(int colorSamples, int coverageSamples) {
        if (coverageSamples < 0 || colorSamples < 0 || (coverageSamples == 0 && colorSamples > 0) || coverageSamples < colorSamples) {
            throw new IllegalArgumentException("Invalid number of coverage samples specified: " + coverageSamples + " - " + colorSamples);
        }
        PixelFormat copy = new PixelFormat(this);
        copy.samples = coverageSamples;
        copy.colorSamples = colorSamples;
        return copy;
    }

    public int getAuxBuffers() {
        return numAuxBuffers;
    }

    public PixelFormat withAuxBuffers(int numAuxBuffers) {
        requireNonNegative("auxiliary buffers", numAuxBuffers);
        PixelFormat copy = new PixelFormat(this);
        copy.numAuxBuffers = numAuxBuffers;
        return copy;
    }

    public int getAccumulationBitsPerPixel() {
        return accumBpp;
    }

    public PixelFormat withAccumulationBitsPerPixel(int accumBpp) {
        requireNonNegative("accumulation bits per pixel", accumBpp);
        PixelFormat copy = new PixelFormat(this);
        copy.accumBpp = accumBpp;
        return copy;
    }

    public int getAccumulationAlpha() {
        return accumAlpha;
    }

    public PixelFormat withAccumulationAlpha(int accumAlpha) {
        requireNonNegative("accumulation alpha bits", accumAlpha);
        PixelFormat copy = new PixelFormat(this);
        copy.accumAlpha = accumAlpha;
        return copy;
    }

    public boolean isStereo() {
        return stereo;
    }

    public PixelFormat withStereo(boolean stereo) {
        PixelFormat copy = new PixelFormat(this);
        copy.stereo = stereo;
        return copy;
    }

    public boolean isFloatingPoint() {
        return floatingPoint;
    }

    public PixelFormat withFloatingPoint(boolean floatingPoint) {
        PixelFormat copy = new PixelFormat(this);
        copy.floatingPoint = floatingPoint;
        if (floatingPoint) {
            copy.floatingPointPacked = false;
        }
        return copy;
    }

    public PixelFormat withFloatingPointPacked(boolean floatingPointPacked) {
        PixelFormat copy = new PixelFormat(this);
        copy.floatingPointPacked = floatingPointPacked;
        if (floatingPointPacked) {
            copy.floatingPoint = false;
        }
        return copy;
    }

    public boolean isSRGB() {
        return sRGB;
    }

    public PixelFormat withSRGB(boolean sRGB) {
        PixelFormat copy = new PixelFormat(this);
        copy.sRGB = sRGB;
        return copy;
    }

    // LWJGL 2 did not expose these getters, but Display needs the stored values for bridge translation
    int getColorSamples() {
        return colorSamples;
    }

    boolean isFloatingPointPacked() {
        return floatingPointPacked;
    }

}
