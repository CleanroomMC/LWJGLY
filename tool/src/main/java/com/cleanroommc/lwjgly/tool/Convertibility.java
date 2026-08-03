package com.cleanroommc.lwjgly.tool;

import org.objectweb.asm.Type;

import java.util.Set;

public final class Convertibility {

    private static final String BYTE_BUFFER = "java/nio/ByteBuffer";
    private static final Set<String> BUFFER_VIEWS = Set.of(
            "java/nio/FloatBuffer",
            "java/nio/IntBuffer",
            "java/nio/ShortBuffer",
            "java/nio/DoubleBuffer",
            "java/nio/LongBuffer"
    );

    public static boolean parameterConvertible(Type from2, Type to3) {
        return parameterConvertible(from2, to3, PointerHandles.none());
    }

    public static boolean parameterConvertible(Type from2, Type to3, PointerHandles handles) {
        if (from2.equals(to3)) {
            return true;
        }
        if (from2.getSort() == Type.OBJECT && to3.getSort() == Type.LONG && handles.isHandle(from2.getInternalName())) {
            return true;
        }
        if (from2.getSort() != Type.OBJECT || to3.getSort() != Type.OBJECT) {
            return false;
        }
        String source = from2.getInternalName();
        String target = to3.getInternalName();
        if (BYTE_BUFFER.equals(source) && BUFFER_VIEWS.contains(target)) {
            return true;
        }
        return "java/lang/String".equals(source) && "java/lang/CharSequence".equals(target);
    }

    public static boolean returnCompatible(Type from2, Type to3) {
        return returnCompatible(from2, to3, PointerHandles.none());
    }

    public static boolean returnCompatible(Type from2, Type to3, PointerHandles handles) {
        if (from2.equals(to3)) {
            return true;
        }
        return from2.getSort() == Type.OBJECT && to3.getSort() == Type.LONG && handles.canWrap(from2.getInternalName());
    }

    public static boolean signatureConvertibleAsHandles(String lwjgl2Desc, String lwjgl3Desc, PointerHandles handles) {
        Type from = Type.getMethodType(lwjgl2Desc);
        Type to = Type.getMethodType(lwjgl3Desc);
        Type[] fromArgs = from.getArgumentTypes();
        Type[] toArgs = to.getArgumentTypes();
        if (fromArgs.length != toArgs.length) {
            return false;
        }
        if (!returnCompatible(from.getReturnType(), to.getReturnType(), handles)) {
            return false;
        }
        for (int i = 0; i < fromArgs.length; i++) {
            if (!fromArgs[i].equals(toArgs[i]) && !isHandleUnwrap(fromArgs[i], toArgs[i], handles)) {
                return false;
            }
        }
        return true;
    }

    public static boolean signatureConvertible(String lwjgl2Desc, String lwjgl3Desc) {
        return signatureConvertible(lwjgl2Desc, lwjgl3Desc, PointerHandles.none());
    }

    public static boolean signatureConvertible(String lwjgl2Desc, String lwjgl3Desc, PointerHandles handles) {
        Type from = Type.getMethodType(lwjgl2Desc);
        Type to = Type.getMethodType(lwjgl3Desc);
        Type[] fromArgs = from.getArgumentTypes();
        Type[] toArgs = to.getArgumentTypes();
        if (fromArgs.length != toArgs.length) {
            return false;
        }
        if (!returnCompatible(from.getReturnType(), to.getReturnType(), handles)) {
            return false;
        }
        for (int i = 0; i < fromArgs.length; i++) {
            if (!parameterConvertible(fromArgs[i], toArgs[i], handles)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHandleUnwrap(Type from2, Type to3, PointerHandles handles) {
        return from2.getSort() == Type.OBJECT && to3.getSort() == Type.LONG && handles.isHandle(from2.getInternalName());
    }

    private Convertibility() { }
}
