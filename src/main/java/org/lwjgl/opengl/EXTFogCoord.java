package org.lwjgl.opengl;

import org.lwjgl.system.MemoryUtil;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

/** Forwards EXT_fog_coord to GL 1.4 and restores buffer-derived type values. */
public class EXTFogCoord {

    public static final int GL_FOG_COORDINATE_SOURCE_EXT = 33872;
    public static final int GL_FOG_COORDINATE_EXT = 33873;
    public static final int GL_FRAGMENT_DEPTH_EXT = 33874;
    public static final int GL_CURRENT_FOG_COORDINATE_EXT = 33875;
    public static final int GL_FOG_COORDINATE_ARRAY_TYPE_EXT = 33876;
    public static final int GL_FOG_COORDINATE_ARRAY_STRIDE_EXT = 33877;
    public static final int GL_FOG_COORDINATE_ARRAY_POINTER_EXT = 33878;
    public static final int GL_FOG_COORDINATE_ARRAY_EXT = 33879;

    public static void glFogCoordfEXT(float coord) {
        GL14.glFogCoordf(coord);
    }

    public static void glFogCoorddEXT(double coord) {
        GL14.glFogCoordd(coord);
    }

    public static void glFogCoordPointerEXT(int stride, FloatBuffer data) {
        GL14.glFogCoordPointer(GL11.GL_FLOAT, stride, data);
    }

    public static void glFogCoordPointerEXT(int stride, DoubleBuffer data) {
        if (!data.isDirect()) {
            throw new IllegalArgumentException("glFogCoordPointerEXT needs a direct DoubleBuffer");
        }
        GL14.nglFogCoordPointer(GL11.GL_DOUBLE, stride, MemoryUtil.memAddress(data));
    }

    public static void glFogCoordPointerEXT(int type, int stride, long data_buffer_offset) {
        GL14.glFogCoordPointer(type, stride, data_buffer_offset);
    }

}
