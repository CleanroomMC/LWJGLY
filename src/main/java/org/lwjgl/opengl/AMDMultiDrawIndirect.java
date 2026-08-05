package org.lwjgl.opengl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/** Forwards AMD_multi_draw_indirect to its GL 4.3 core spelling. */
public class AMDMultiDrawIndirect {

    public static void glMultiDrawArraysIndirectAMD(int mode, ByteBuffer indirect, int primcount, int stride) {
        GL43.glMultiDrawArraysIndirect(mode, indirect, primcount, stride);
    }

    public static void glMultiDrawArraysIndirectAMD(int mode, IntBuffer indirect, int primcount, int stride) {
        GL43.glMultiDrawArraysIndirect(mode, indirect, primcount, stride);
    }

    public static void glMultiDrawArraysIndirectAMD(int mode, long indirect_buffer_offset, int primcount, int stride) {
        GL43.glMultiDrawArraysIndirect(mode, indirect_buffer_offset, primcount, stride);
    }

    public static void glMultiDrawElementsIndirectAMD(int mode, int type, ByteBuffer indirect, int primcount, int stride) {
        GL43.glMultiDrawElementsIndirect(mode, type, indirect, primcount, stride);
    }

    public static void glMultiDrawElementsIndirectAMD(int mode, int type, IntBuffer indirect, int primcount, int stride) {
        GL43.glMultiDrawElementsIndirect(mode, type, indirect, primcount, stride);
    }

    public static void glMultiDrawElementsIndirectAMD(int mode, int type, long indirect_buffer_offset, int primcount, int stride) {
        GL43.glMultiDrawElementsIndirect(mode, type, indirect_buffer_offset, primcount, stride);
    }

}
