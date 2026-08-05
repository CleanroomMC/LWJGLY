package com.cleanroommc.lwjgly.adapter.opengl;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class AMDPerformanceMonitor {

    public static String glGetPerfMonitorGroupStringAMD(int group, int bufSize) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer length = stack.mallocInt(1);
            ByteBuffer groupString = stack.malloc(bufSize);
            org.lwjgl.opengl.AMDPerformanceMonitor.glGetPerfMonitorGroupStringAMD(group, length, groupString);
            return MemoryUtil.memASCII(groupString, length.get(0));
        }
    }

    public static String glGetPerfMonitorCounterStringAMD(int group, int counter, int bufSize) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer length = stack.mallocInt(1);
            ByteBuffer counterString = stack.malloc(bufSize);
            org.lwjgl.opengl.AMDPerformanceMonitor.glGetPerfMonitorCounterStringAMD(group, counter, length, counterString);
            return MemoryUtil.memASCII(counterString, length.get(0));
        }
    }

    public static void glSelectPerfMonitorCountersAMD(int monitor, boolean enable, int group, int counter) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            org.lwjgl.opengl.AMDPerformanceMonitor.glSelectPerfMonitorCountersAMD(monitor, enable, group, stack.ints(counter));
        }
    }

    public static int glGetPerfMonitorCounterDataAMD(int monitor, int pname) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer data = stack.mallocInt(1);
            org.lwjgl.opengl.AMDPerformanceMonitor.glGetPerfMonitorCounterDataAMD(monitor, pname, data, null);
            return data.get(0);
        }
    }

    private AMDPerformanceMonitor() { }

}
