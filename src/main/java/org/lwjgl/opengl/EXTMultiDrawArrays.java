package org.lwjgl.opengl;

import java.nio.IntBuffer;

/** Forwards EXT_multi_draw_arrays to its GL 1.4 core spelling. */
public class EXTMultiDrawArrays {

    public static void glMultiDrawArraysEXT(int mode, IntBuffer piFirst, IntBuffer piCount) {
        GL14.glMultiDrawArrays(mode, piFirst, piCount);
    }

}
