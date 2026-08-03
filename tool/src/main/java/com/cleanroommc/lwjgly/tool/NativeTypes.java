package com.cleanroommc.lwjgly.tool;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.Set;

public final class NativeTypes {

    public static final String GL_ENUM = "GLenum";
    private static final String NATIVE_TYPE = "Lorg/lwjgl/system/NativeType;";
    private static final Set<String> SCALARS = Set.of(
            GL_ENUM, "GLbitfield", "GLint", "GLuint", "GLsizei", "GLboolean", "GLfloat", "GLdouble",
            "GLshort", "GLushort", "GLbyte", "GLubyte", "GLfixed", "GLclampf", "GLclampd",
            "GLint64", "GLuint64", "GLintptr", "GLsizeiptr", "GLhandleARB",
            "ALenum", "ALint", "ALuint", "ALfloat", "ALdouble", "ALsizei", "ALboolean", "ALCenum",
            "cl_uint", "cl_int", "cl_ulong", "cl_long", "cl_bitfield", "size_t",
            "int", "unsigned int", "long", "float", "double", "char", "short"
    );

    public static NativeTypes of(MethodNode method, int parameterCount) {
        String[] types = new String[parameterCount];
        if (method != null && method.visibleParameterAnnotations != null) {
            for (int i = 0; i < parameterCount && i < method.visibleParameterAnnotations.length; i++) {
                types[i] = value(method.visibleParameterAnnotations[i]);
            }
        }
        return new NativeTypes(types);
    }

    public static NativeTypes unknown(int parameterCount) {
        return new NativeTypes(new String[parameterCount]);
    }

    private static String value(List<AnnotationNode> annotations) {
        if (annotations == null) {
            return null;
        }
        for (AnnotationNode annotation : annotations) {
            if (!NATIVE_TYPE.equals(annotation.desc) || annotation.values == null) {
                continue;
            }
            for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                if ("value".equals(annotation.values.get(i)) && annotation.values.get(i + 1) instanceof String s) {
                    return s;
                }
            }
        }
        return null;
    }

    private final String[] byParameter;

    private NativeTypes(String[] byParameter) {
        this.byParameter = byParameter;
    }

    public String at(int parameter) {
        return parameter >= 0 && parameter < byParameter.length ? byParameter[parameter] : null;
    }

    public boolean isEnum(int parameter) {
        return GL_ENUM.equals(at(parameter));
    }

    public boolean isScalar(int parameter) {
        String type = at(parameter);
        return type != null && SCALARS.contains(type);
    }

}
