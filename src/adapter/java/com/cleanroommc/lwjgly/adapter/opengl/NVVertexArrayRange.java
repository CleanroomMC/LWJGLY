package com.cleanroommc.lwjgly.adapter.opengl;

import org.lwjgl.opengl.GL;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;

import java.nio.ByteBuffer;

public final class NVVertexArrayRange {

    public static ByteBuffer glAllocateMemoryNV(int size, float readFrequency, float writeFrequency, float priority) {
        long function = Platform.get() == Platform.WINDOWS ? GL.getCapabilitiesWGL().wglAllocateMemoryNV : GL.getFunctionProvider().getFunctionAddress("glXAllocateMemoryNV");
        long address = JNI.callP(size, readFrequency, writeFrequency, priority, check(function, "AllocateMemoryNV"));
        return MemoryUtil.memByteBufferSafe(address, size);
    }

    public static void glFreeMemoryNV(ByteBuffer pointer) {
        long function = Platform.get() == Platform.WINDOWS ? GL.getCapabilitiesWGL().wglFreeMemoryNV : GL.getFunctionProvider().getFunctionAddress("glXFreeMemoryNV");
        JNI.callPV(MemoryUtil.memAddress(pointer), check(function, "FreeMemoryNV"));
    }

    private static long check(long function, String name) {
        if (function == MemoryUtil.NULL) {
            throw new IllegalStateException("NV_vertex_array_range: " +
                    (Platform.get() == Platform.WINDOWS ? "wgl" : "glX") + name + " is not available on this driver");
        }
        return function;
    }

    private NVVertexArrayRange() { }

}
