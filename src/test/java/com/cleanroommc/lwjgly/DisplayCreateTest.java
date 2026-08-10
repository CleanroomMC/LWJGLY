package com.cleanroommc.lwjgly;

import com.cleanroommc.lwjgly.spi.WindowBridge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.LWJGLException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.Drawable;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.opengl.PixelFormatLWJGL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayCreateTest {

    private MockWindow bridge;

    @BeforeEach
    void installBridge() {
        bridge = new MockWindow();
        LWJGLY.setWindowBridge(bridge);
    }

    @Test
    void defaultCreateRequestsDefaultLwjgl2PixelFormat() throws Exception {
        Display.create();

        WindowBridge.PixelFormatRequest pixel = bridge.contextRequest.pixelFormat();
        assertEquals(0, pixel.alphaBits());
        assertEquals(8, pixel.depthBits());
        assertEquals(0, pixel.stencilBits());
        assertEquals(0, pixel.samples());
        assertNull(bridge.contextRequest.attributes());
    }

    @Test
    void defaultContextValuesRemainUnspecifiedCreationAttributes() throws Exception {
        Display.create(new PixelFormat(), new ContextAttribs());

        WindowBridge.ContextAttributesRequest context = bridge.contextRequest.attributes();
        assertEquals(WindowBridge.ContextProfile.DEFAULT, context.profile());
        assertEquals(WindowBridge.ResetNotification.DEFAULT, context.resetNotification());
        assertEquals(WindowBridge.ReleaseBehavior.DEFAULT, context.releaseBehavior());
    }

    @Test
    void translateRequestsWithoutSdlConstants() throws Exception {
        PixelFormat pixelFormat = new PixelFormat(24, 8, 24, 8, 4, 1,
                48, 16, true)
                .withCoverageSamples(2, 4)
                .withFloatingPointPacked(true)
                .withSRGB(true);
        ContextAttribs attribs = new ContextAttribs(4, 5)
                .withProfileCompatibility(true)
                .withDebug(true)
                .withForwardCompatible(true)
                .withRobustAccess(true)
                .withContextResetIsolation(true)
                .withLoseContextOnReset(true)
                .withContextReleaseBehavior(ContextAttribs.CONTEXT_RELEASE_BEHAVIOR_NONE_ARB);

        Display.create(pixelFormat, attribs);

        WindowBridge.PixelFormatRequest pixel = bridge.contextRequest.pixelFormat();
        assertEquals(24, pixel.bitsPerPixel());
        assertEquals(8, pixel.alphaBits());
        assertEquals(24, pixel.depthBits());
        assertEquals(8, pixel.stencilBits());
        assertEquals(4, pixel.samples());
        assertEquals(2, pixel.colorSamples());
        assertEquals(1, pixel.auxiliaryBuffers());
        assertEquals(48, pixel.accumulationBitsPerPixel());
        assertEquals(16, pixel.accumulationAlphaBits());
        assertTrue(pixel.stereo());
        assertFalse(pixel.floatingPoint());
        assertTrue(pixel.floatingPointPacked());
        assertTrue(pixel.sRGB());

        WindowBridge.ContextAttributesRequest context = bridge.contextRequest.attributes();
        assertEquals(4, context.majorVersion());
        assertEquals(5, context.minorVersion());
        assertEquals(WindowBridge.ContextProfile.COMPATIBILITY, context.profile());
        assertTrue(context.debug());
        assertTrue(context.forwardCompatible());
        assertTrue(context.robustAccess());
        assertTrue(context.resetIsolation());
        assertEquals(WindowBridge.ResetNotification.LOSE_CONTEXT, context.resetNotification());
        assertEquals(WindowBridge.ReleaseBehavior.NONE, context.releaseBehavior());
        assertEquals(0, context.layerPlane());
    }

    @Test
    void rejectionReleasesContextAndThrowsLwjglException() {
        bridge.contextResult = WindowBridge.ContextResult.rejected("needs 8 stencil bits", bridge.windowHandle, bridge.contextHandle);

        LWJGLException failure = assertThrows(LWJGLException.class, () -> Display.create(new PixelFormat().withStencilBits(8)));

        assertEquals("needs 8 stencil bits", failure.getMessage());
        assertFalse(bridge.current);
    }

    @Test
    void rejectMismatchedWindowHandles() {
        bridge.currentWindowHandle = 0xDEADL;

        LWJGLException failure = assertThrows(LWJGLException.class, Display::create);

        assertTrue(failure.getMessage().contains("does not match"));
        assertFalse(bridge.current);
    }

    @Test
    void rejectMissingContext() {
        bridge.contextHandle = 0L;

        LWJGLException failure = assertThrows(LWJGLException.class, Display::create);

        assertTrue(failure.getMessage().contains("no current OpenGL context"));
    }

    @Test
    void rejectMissingCapabilities() {
        bridge.contextResult = new WindowBridge.ContextResult(true, false, bridge.windowHandle, bridge.contextHandle, null);

        LWJGLException failure = assertThrows(LWJGLException.class, Display::create);

        assertTrue(failure.getMessage().contains("capabilities were not bound"));
        assertFalse(bridge.current);
    }

    @Test
    void rejectZeroWindowHandle() {
        bridge.windowHandle = 0L;

        LWJGLException failure = assertThrows(LWJGLException.class, Display::create);

        assertTrue(failure.getMessage().contains("no SDL window handle"));
        assertNull(bridge.contextRequest);
    }

    @Test
    void rejectSharedDrawable() {
        Drawable shared = new Drawable() {
            @Override public boolean isCurrent() { return false; }
            @Override public void makeCurrent() { }
            @Override public void releaseContext() { }
            @Override public void destroy() { }
            @Override public void setCLSharingProperties(PointerBuffer properties) { }
        };

        LWJGLException failure = assertThrows(LWJGLException.class, () -> Display.create(new PixelFormat(), shared));

        assertTrue(failure.getMessage().contains("Context sharing"));
        assertNull(bridge.contextRequest);
    }

    @Test
    void rejectUnknownPixelFormat() {
        PixelFormatLWJGL unknown = new PixelFormatLWJGL() { };

        LWJGLException failure = assertThrows(LWJGLException.class, () -> Display.create(unknown));

        assertTrue(failure.getMessage().contains("Unsupported PixelFormatLWJGL"));
        assertNull(bridge.contextRequest);
    }

}
