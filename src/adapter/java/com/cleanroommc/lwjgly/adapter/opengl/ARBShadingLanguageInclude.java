package com.cleanroommc.lwjgly.adapter.opengl;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class ARBShadingLanguageInclude {

    public static void glCompileShaderIncludeARB(int shader, int count, ByteBuffer path) {
        org.lwjgl.opengl.ARBShadingLanguageInclude.nglCompileShaderIncludeARB(shader, count, MemoryUtil.memAddress(path), 0L);
    }

    public static void glCompileShaderIncludeARB(int shader, CharSequence[] path) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer paths = stack.mallocPointer(path.length);
            IntBuffer lengths = stack.mallocInt(path.length);
            for (int i = 0; i < path.length; i++) {
                ByteBuffer encoded = stack.ASCII(path[i], false);
                paths.put(i, MemoryUtil.memAddress(encoded));
                lengths.put(i, encoded.remaining());
            }
            org.lwjgl.opengl.ARBShadingLanguageInclude.glCompileShaderIncludeARB(shader, paths, lengths);
        }
    }

    private ARBShadingLanguageInclude() { }

}
