package com.cleanroommc.lwjgly.adapter.opengl;

import org.lwjgl.system.MemoryStack;

public final class ARBDrawBuffers {

    public static void glDrawBuffersARB(int buffer) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            org.lwjgl.opengl.ARBDrawBuffers.glDrawBuffersARB(stack.ints(buffer));
        }
    }

    private ARBDrawBuffers() { }

}
