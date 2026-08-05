package com.cleanroommc.lwjgly.adapter.opengl;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;

import java.nio.ByteBuffer;

public final class EXTDirectStateAccess {

    public static void glTextureParameterIEXT(int texture, int target, int pname, int param) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            org.lwjgl.opengl.EXTDirectStateAccess.nglTextureParameterIivEXT(texture, target, pname, MemoryUtil.memAddress(stack.ints(param)));
        }
    }

    public static void glTextureParameterIuEXT(int texture, int target, int pname, int param) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            org.lwjgl.opengl.EXTDirectStateAccess.nglTextureParameterIuivEXT(texture, target, pname, MemoryUtil.memAddress(stack.ints(param)));
        }
    }

    public static void glMultiTexParameterIEXT(int texunit, int target, int pname, int param) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            org.lwjgl.opengl.EXTDirectStateAccess.nglMultiTexParameterIivEXT(texunit, target, pname, MemoryUtil.memAddress(stack.ints(param)));
        }
    }

    public static void glMultiTexParameterIuEXT(int texunit, int target, int pname, int param) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            org.lwjgl.opengl.EXTDirectStateAccess.nglMultiTexParameterIuivEXT(texunit, target, pname, MemoryUtil.memAddress(stack.ints(param)));
        }
    }

    public static ByteBuffer glGetNamedBufferPointerEXT(int buffer, int pname) {
        long function = GL.getFunctionProvider().getFunctionAddress("glGetNamedBufferPointervEXT");
        if (function == MemoryUtil.NULL) {
            throw new IllegalStateException("EXT_direct_state_access: glGetNamedBufferPointervEXT is not available on this driver");
        }
        long address;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long params = stack.ncalloc(Pointer.POINTER_SIZE, 1, Pointer.POINTER_SIZE);
            JNI.callPV(buffer, pname, params, function);
            address = MemoryUtil.memGetAddress(params);
        }
        if (address == MemoryUtil.NULL) {
            return null;
        }
        int size = org.lwjgl.opengl.EXTDirectStateAccess.glGetNamedBufferParameteriEXT(buffer, GL15.GL_BUFFER_SIZE);
        return MemoryUtil.memByteBufferSafe(address, size);
    }

    public static ByteBuffer glGetPointerEXT(int pname, int index, long resultSize) {
        return MemoryUtil.memByteBufferSafe(org.lwjgl.opengl.EXTDirectStateAccess.glGetPointeriEXT(pname, index), (int) resultSize);
    }

    public static ByteBuffer glGetPointerIndexedEXT(int pname, int index, long resultSize) {
        return MemoryUtil.memByteBufferSafe(
                org.lwjgl.opengl.EXTDirectStateAccess.glGetPointerIndexedEXT(pname, index), (int) resultSize
        );
    }

    public static ByteBuffer glGetVertexArrayPointerEXT(int vaobj, int pname, long resultSize) {
        return MemoryUtil.memByteBufferSafe(
                org.lwjgl.opengl.EXTDirectStateAccess.glGetVertexArrayPointerEXT(vaobj, pname), (int) resultSize
        );
    }

    public static ByteBuffer glGetVertexArrayPointerEXT(int vaobj, int index, int pname, long resultSize) {
        return MemoryUtil.memByteBufferSafe(
                org.lwjgl.opengl.EXTDirectStateAccess.glGetVertexArrayPointeriEXT(vaobj, index, pname), (int) resultSize
        );
    }

    public static void glNamedProgramStringEXT(int program, int target, int format, CharSequence string) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            org.lwjgl.opengl.EXTDirectStateAccess.glNamedProgramStringEXT(program, target, format, stack.ASCII(string, false));
        }
    }

    public static String glGetNamedProgramStringEXT(int program, int target, int pname) {
        int length = org.lwjgl.opengl.EXTDirectStateAccess.glGetNamedProgramiEXT(program, target,
                org.lwjgl.opengl.ARBVertexProgram.GL_PROGRAM_LENGTH_ARB);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer source = stack.malloc(length);
            org.lwjgl.opengl.EXTDirectStateAccess.glGetNamedProgramStringEXT(program, target, pname, source);
            return MemoryUtil.memASCII(source);
        }
    }

    private EXTDirectStateAccess() { }

}
