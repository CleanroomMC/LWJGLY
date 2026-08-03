package com.cleanroommc.lwjgly.tool;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NamingTest {

    private static List<String> names(String lwjgl2Name, String desc) {
        return Naming.candidateNames(lwjgl2Name, desc);
    }

    @Test
    void dropsTheTypeSuffixBack() {
        assertTrue(names("glFog", "(ILjava/nio/FloatBuffer;)V").contains("glFogfv"));
        assertTrue(names("glFog", "(ILjava/nio/IntBuffer;)V").contains("glFogiv"));
        assertTrue(names("glGetTexParameter", "(IILjava/nio/IntBuffer;)V").contains("glGetTexParameteriv"));
        assertTrue(names("glGetMaterial", "(IILjava/nio/FloatBuffer;)V").contains("glGetMaterialfv"));
    }

    @Test
    void offerAdditionalScalarSpelling() {
        // glLoadMatrix(FloatBuffer) is glLoadMatrixf, not glLoadMatrixfv
        assertTrue(names("glLoadMatrix", "(Ljava/nio/FloatBuffer;)V").contains("glLoadMatrixf"));
        assertTrue(names("glMultMatrix", "(Ljava/nio/DoubleBuffer;)V").contains("glMultMatrixd"));
    }

    @Test
    void keepsTheVendorTagLast() {
        // glGetObjectParameterARBfv exists nowhere, the type letters go before the tag
        List<String> arb = names("glGetObjectParameterARB", "(IILjava/nio/FloatBuffer;)V");
        assertTrue(arb.contains("glGetObjectParameterfvARB"));
        assertFalse(arb.contains("glGetObjectParameterARBfv"));
    }

    @Test
    void readsTrailingUAsPartOfTheSuffix() {
        // LWJGL 2's glPixelMapu(int, ShortBuffer) is GL's glPixelMapusv
        assertTrue(names("glPixelMapu", "(ILjava/nio/ShortBuffer;)V").contains("glPixelMapusv"));
        assertTrue(names("glGetPixelMapu", "(ILjava/nio/IntBuffer;)V").contains("glGetPixelMapuiv"));
    }

    @Test
    void neverMixesSignedAndUnsignedSpellings() {
        assertFalse(names("glPixelMapu", "(ILjava/nio/ShortBuffer;)V").contains("glPixelMapusv".replace("usv", "sv")));
        assertFalse(names("glPixelMap", "(ILjava/nio/FloatBuffer;)V").contains("glPixelMapufv"));
    }

    @Test
    void offersIndexedSpellings() {
        // Here: 'i' means "by index", not "int", so it applies whatever the element type is
        assertTrue(names("glGetInteger", "(IILjava/nio/IntBuffer;)V").contains("glGetIntegeri_v"));
        assertTrue(names("glGetFloat", "(IILjava/nio/FloatBuffer;)V").contains("glGetFloati_v"));
        assertTrue(names("glGetInteger", "(II)I").contains("glGetIntegeri"));
    }

    @Test
    void readsTheTypeOffTheReturnWhenThereIsNoBuffer() {
        // glGetQueryObjectu(int, int) returns the value rather than filling a buffer
        // Type letters are only recoverable from the return type
        // GL spells it glGetQueryObjectui64
        assertTrue(names("glGetQueryObjectu", "(II)J").contains("glGetQueryObjectui64"));
        assertTrue(names("glGetQueryObject", "(II)J").contains("glGetQueryObjecti64"));
        assertTrue(names("glGetUniformSubroutineu", "(II)I").contains("glGetUniformSubroutineui"));
        // Vendor tag still goes last
        assertTrue(names("glGetQueryObjectEXT", "(II)J").contains("glGetQueryObjecti64EXT"));
        assertTrue(names("glGetQueryObjectuEXT", "(II)J").contains("glGetQueryObjectui64EXT"));
    }

    @Test
    void composesTypeLettersWithIndexedSpelling() {
        // glGetIntegerui64i_vNV needs both rules at once: 'ui64' from the LongBuffer and 'i_v' for the index
        // Neither alone produces it.
        assertTrue(names("glGetIntegeruNV", "(IILjava/nio/LongBuffer;)V")
                .contains("glGetIntegerui64i_vNV"));
    }

    @Test
    void offersHalfFloatSpelling() {
        // NV_half_float carries halves in a ShortBuffer
        // glVertexAttribs1NV is glVertexAttribs1hvNV
        assertTrue(names("glVertexAttribs1NV", "(ILjava/nio/ShortBuffer;)V").contains("glVertexAttribs1hvNV"));
        // The genuine short spelling still comes first
        List<String> candidates = names("glVertexAttribs1NV", "(ILjava/nio/ShortBuffer;)V");
        assertTrue(candidates.indexOf("glVertexAttribs1svNV") < candidates.indexOf("glVertexAttribs1hvNV"));
    }

    @Test
    void proposesNothingWhenRulesDoNotFit() {
        // No buffer and no index shape: the rule should NOT invent a name
        // Three universal spellings are all it may offer
        assertTrue(names("glFoo", "()V").size() <= 3);
    }
}
