package com.cleanroommc.lwjgly.tool;

import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CallPlan {

    private static final String BYTE_BUFFER = "java/nio/ByteBuffer";
    private static final Map<String, Integer> ELEMENT_WIDTH = Map.of(
            "java/nio/ByteBuffer", 1,
            "java/nio/ShortBuffer", 2,
            "java/nio/CharBuffer", 2,
            "java/nio/IntBuffer", 4,
            "java/nio/FloatBuffer", 4,
            "java/nio/LongBuffer", 8,
            "java/nio/DoubleBuffer", 8);
    private static final Map<String, String> GL_TYPES = Map.of(
            "java/nio/ByteBuffer", "GL_UNSIGNED_BYTE",
            "java/nio/ShortBuffer", "GL_SHORT",
            "java/nio/IntBuffer", "GL_INT",
            "java/nio/FloatBuffer", "GL_FLOAT",
            "java/nio/DoubleBuffer", "GL_DOUBLE");
    private static final int NOTHING_INSERTED = -1;

    public static boolean isBuffer(Type type) {
        return type.getSort() == Type.OBJECT && ELEMENT_WIDTH.containsKey(type.getInternalName());
    }

    public static int elementWidth(Type buffer) {
        return ELEMENT_WIDTH.get(buffer.getInternalName());
    }

    public static String glType(Type buffer) {
        return GL_TYPES.get(buffer.getInternalName());
    }

    public static List<Delta.Argument> of(String lwjgl2Desc, String lwjgl3Desc, PointerHandles handles,
                                          boolean allowLossy, NativeTypes nativeTypes) {
        Type from = Type.getMethodType(lwjgl2Desc);
        Type to = Type.getMethodType(lwjgl3Desc);
        if (!Convertibility.returnCompatible(from.getReturnType(), to.getReturnType(), handles)) {
            return null;
        }
        Type[] fromArgs = from.getArgumentTypes();
        Type[] toArgs = to.getArgumentTypes();

        if (fromArgs.length == toArgs.length) {
            return direct(fromArgs, toArgs, handles, allowLossy, nativeTypes, NOTHING_INSERTED);
        }
        if (allowLossy && toArgs.length == fromArgs.length + 1) {
            List<Delta.Argument> plan = withByteCount(fromArgs, toArgs, handles, nativeTypes);
            return plan != null ? plan : withGlType(fromArgs, toArgs, handles, nativeTypes);
        }
        return null;
    }

    private static List<Delta.Argument> direct(Type[] fromArgs, Type[] toArgs, PointerHandles handles,
                                               boolean allowLossy, NativeTypes nativeTypes, int insertedAt) {
        List<Delta.Argument> plan = new ArrayList<>();
        for (int i = 0; i < fromArgs.length; i++) {
            int declared = insertedAt == NOTHING_INSERTED || i < insertedAt ? i : i + 1;
            Conversion conversion =
                    conversionOf(fromArgs[i], toArgs[i], handles, allowLossy, nativeTypes, declared);
            if (conversion == null) {
                return null;
            }
            plan.add(new Delta.Argument(conversion, i));
        }
        return plan;
    }

    private static Conversion conversionOf(Type from, Type to, PointerHandles handles, boolean allowLossy,
                                           NativeTypes nativeTypes, int parameter) {
        if (from.equals(to)) {
            return Conversion.PASS;
        }
        if (from.getSort() == Type.OBJECT && to.getSort() == Type.LONG
                && handles.isHandle(from.getInternalName())) {
            return Conversion.HANDLE;
        }

        boolean scalarTarget = nativeTypes.isScalar(parameter);
        if (from.getSort() == Type.OBJECT && to.getSort() == Type.OBJECT) {
            String source = from.getInternalName();
            String target = to.getInternalName();
            if ("java/lang/String".equals(source) && "java/lang/CharSequence".equals(target)) {
                return Conversion.PASS;
            }
            if (BYTE_BUFFER.equals(source) && ELEMENT_WIDTH.containsKey(target)) {
                return Conversion.VIEW;
            }
            if (!allowLossy || scalarTarget || !isBuffer(from) || !isBuffer(to)) {
                return null;
            }
            if (BYTE_BUFFER.equals(target)) {
                return Conversion.BYTE_VIEW;
            }

            return ELEMENT_WIDTH.get(source).equals(ELEMENT_WIDTH.get(target)) ? Conversion.RETYPE : null;
        }
        if (allowLossy && !scalarTarget && isBuffer(from) && to.getSort() == Type.LONG) {
            return Conversion.ADDRESS;
        }
        return null;
    }

    private static List<Delta.Argument> withByteCount(Type[] fromArgs, Type[] toArgs,
                                                      PointerHandles handles, NativeTypes nativeTypes) {
        for (int b = 0; b < fromArgs.length; b++) {
            if (!isBuffer(fromArgs[b])) {
                continue;
            }
            if (b + 1 >= toArgs.length || toArgs[b + 1].getSort() != Type.LONG) {
                continue;
            }
            if (toArgs[b].getSort() == Type.LONG) {

                String size = nativeTypes.at(b);
                String address = nativeTypes.at(b + 1);
                if (!nativeTypes.isScalar(b) || size == null || address == null || !address.endsWith("*")) {
                    continue;
                }
            } else if (toArgs[b].getSort() != Type.INT) {
                continue;
            }
            List<Delta.Argument> plan = copyAround(fromArgs, toArgs, handles, b, nativeTypes);
            if (plan == null) {
                continue;
            }

            plan.add(b, new Delta.Argument(Conversion.BYTE_COUNT, b));
            plan.set(b + 1, new Delta.Argument(Conversion.ADDRESS, b));
            return plan;
        }
        return null;
    }

    private static List<Delta.Argument> withGlType(Type[] fromArgs, Type[] toArgs,
                                                   PointerHandles handles, NativeTypes nativeTypes) {
        int enumAt = -1;
        for (int t = 0; t < toArgs.length; t++) {
            if (toArgs[t].getSort() == Type.INT && nativeTypes.isEnum(t)) {
                if (enumAt >= 0) {
                    return null;
                }
                enumAt = t;
            }
        }
        if (enumAt < 0) {
            return null;
        }

        Type[] remaining = new Type[toArgs.length - 1];
        System.arraycopy(toArgs, 0, remaining, 0, enumAt);
        System.arraycopy(toArgs, enumAt + 1, remaining, enumAt, toArgs.length - enumAt - 1);

        List<Delta.Argument> plan = direct(fromArgs, remaining, handles, true, nativeTypes, enumAt);
        if (plan == null) {
            return null;
        }

        for (int i = 0; i < fromArgs.length; i++) {
            if (isBuffer(fromArgs[i]) && glType(fromArgs[i]) != null) {
                plan.add(enumAt, new Delta.Argument(Conversion.GL_TYPE, i));
                return plan;
            }
        }
        return null;
    }

    private static List<Delta.Argument> copyAround(Type[] fromArgs, Type[] toArgs, PointerHandles handles,
                                                   int expanded, NativeTypes nativeTypes) {
        List<Delta.Argument> plan = new ArrayList<>();
        for (int i = 0; i < fromArgs.length; i++) {
            if (i == expanded) {
                plan.add(new Delta.Argument(Conversion.PASS, i));
                continue;
            }
            int declared = i <= expanded ? i : i + 1;
            Conversion conversion = conversionOf(
                    fromArgs[i], toArgs[declared], handles, true, nativeTypes, declared);
            if (conversion == null) {
                return null;
            }
            plan.add(new Delta.Argument(conversion, i));
        }
        return plan;
    }

    private CallPlan() { }

}
