package org.lwjgl.opengl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayValueObjectsTest {

    @Test
    void pixelFormatHasLwjgl2DefaultsAndImmutableWithers() {
        PixelFormat original = new PixelFormat();
        PixelFormat changed = original.withDepthBits(24).withSamples(4).withSRGB(true);

        assertEquals(0, original.getAlphaBits());
        assertEquals(8, original.getDepthBits());
        assertEquals(0, original.getStencilBits());
        assertEquals(8, original.getDepthBits());
        assertEquals(24, changed.getDepthBits());
        assertEquals(4, changed.getSamples());
        assertTrue(changed.isSRGB());
        assertNotSame(original, changed);
        assertThrows(IllegalArgumentException.class, () -> original.withSamples(-1));
    }

    @Test
    void floatingPointModesRemainMutuallyExclusive() {
        PixelFormat floating = new PixelFormat().withFloatingPoint(true);
        PixelFormat packed = floating.withFloatingPointPacked(true);

        assertTrue(floating.isFloatingPoint());
        assertFalse(floating.isFloatingPointPacked());
        assertFalse(packed.isFloatingPoint());
        assertTrue(packed.isFloatingPointPacked());
    }

    @Test
    void contextAttributesHaveLwjgl2DefaultsAndImmutableWithers() {
        ContextAttribs original = new ContextAttribs(3, 3);
        ContextAttribs changed = original.withProfileCore(true).withDebug(true);

        assertEquals(3, original.getMajorVersion());
        assertEquals(3, original.getMinorVersion());
        assertEquals(0, original.getProfileMask());
        assertFalse(original.isDebug());
        assertTrue(changed.isProfileCore());
        assertTrue(changed.isDebug());
        assertNotSame(original, changed);
    }

    @Test
    void requireSameProfilesAsLwjgl2() {
        assertThrows(IllegalArgumentException.class, () -> new ContextAttribs(3, 1).withProfileCore(true));
        assertThrows(IllegalArgumentException.class, () -> new ContextAttribs(3, 3).withProfileES(true));
    }
}
