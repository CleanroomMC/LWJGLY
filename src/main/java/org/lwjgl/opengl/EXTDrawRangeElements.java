package org.lwjgl.opengl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/** Forwards EXT_draw_range_elements to its GL 1.2 core spelling. */
public class EXTDrawRangeElements {

    public static final int GL_MAX_ELEMENTS_VERTICES_EXT = 33000;
    public static final int GL_MAX_ELEMENTS_INDICES_EXT = 33001;

    public static void glDrawRangeElementsEXT(int mode, int start, int end, ByteBuffer indices) {
        GL12.glDrawRangeElements(mode, start, end, indices);
    }

    public static void glDrawRangeElementsEXT(int mode, int start, int end, IntBuffer indices) {
        GL12.glDrawRangeElements(mode, start, end, indices);
    }

    public static void glDrawRangeElementsEXT(int mode, int start, int end, ShortBuffer indices) {
        GL12.glDrawRangeElements(mode, start, end, indices);
    }

    public static void glDrawRangeElementsEXT(int mode, int start, int end, int count, int type, long buffer_offset) {
        GL12.glDrawRangeElements(mode, start, end, count, type, buffer_offset);
    }

}
