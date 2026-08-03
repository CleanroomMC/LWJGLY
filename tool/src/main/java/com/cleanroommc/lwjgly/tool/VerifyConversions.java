package com.cleanroommc.lwjgly.tool;

import org.objectweb.asm.Type;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class VerifyConversions {

    private static final Map<String, Integer> POINTEE_WIDTH = Map.ofEntries(
            Map.entry("GLbyte", 1), Map.entry("GLubyte", 1), Map.entry("GLchar", 1),
            Map.entry("GLboolean", 1), Map.entry("void", 0),
            Map.entry("GLshort", 2), Map.entry("GLushort", 2), Map.entry("GLhalf", 2),
            Map.entry("GLint", 4), Map.entry("GLuint", 4), Map.entry("GLsizei", 4),
            Map.entry("GLenum", 4), Map.entry("GLfloat", 4), Map.entry("GLclampf", 4),
            Map.entry("GLfixed", 4), Map.entry("GLbitfield", 4),
            Map.entry("GLdouble", 8), Map.entry("GLclampd", 8),
            Map.entry("GLint64", 8), Map.entry("GLuint64", 8),
            Map.entry("GLintptr", 8), Map.entry("GLsizeiptr", 8), Map.entry("GLsync", 8),
            Map.entry("ALint", 4), Map.entry("ALuint", 4), Map.entry("ALenum", 4),
            Map.entry("ALfloat", 4), Map.entry("ALsizei", 4), Map.entry("ALboolean", 1),
            Map.entry("ALdouble", 8), Map.entry("ALCint", 4), Map.entry("ALCenum", 4));

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            throw new IllegalArgumentException("usage: VerifyConversions <lwjgl2-cp> <lwjgl3-cp> <report>");
        }
        ApiIndex lwjgl2 = ApiIndex.read(args[0]);
        ApiIndex lwjgl3 = ApiIndex.read(args[1]);
        Path report = Paths.get(args[2]);
        PointerHandles handles = PointerHandles.in(lwjgl2);
        Map<String, Delta.ClassDelta> deltas = ApiDiff.diff(lwjgl2, lwjgl3);

        List<String> problems = new ArrayList<>();
        Map<Conversion, Integer> counts = new TreeMap<>();
        List<String> lines = new ArrayList<>();

        for (Delta.ClassDelta delta : deltas.values()) {
            if (delta.bucket() != Delta.Bucket.INJECT) {
                continue;
            }
            for (Delta.MethodDelta method : delta.methods()) {
                if (!method.fix().isAutomatic() || method.target() == null
                        || method.target().arguments() == null) {
                    continue;
                }
                Type[] from = Type.getMethodType(method.desc()).getArgumentTypes();
                Type[] to = Type.getMethodType(method.target().desc()).getArgumentTypes();
                NativeTypes nativeTypes = NativeTypes.of(
                        Resolution.findMethod(lwjgl3, delta.internalName(),
                                method.target().name(), method.target().desc()),
                        to.length);
                List<Delta.Argument> plan = method.target().arguments();

                for (int i = 0; i < plan.size(); i++) {
                    Delta.Argument argument = plan.get(i);
                    counts.merge(argument.conversion(), 1, Integer::sum);
                    String where = delta.internalName().replace('/', '.') + "." + method.name()
                            + method.desc() + " -> " + method.target() + " argument " + i;
                    String problem = check(argument, from[argument.source()], to[i], nativeTypes, i, handles);
                    if (problem != null) {
                        problems.add(where + ": " + problem);
                    } else if (argument.conversion() != Conversion.PASS) {
                        lines.add(where + ": " + argument.conversion() + " from "
                                + simple(from[argument.source()]) + " -> @NativeType(\""
                                + nativeTypes.at(i) + "\")");
                    }
                }
            }
        }

        Files.createDirectories(report.getParent());
        Files.write(report, lines);

        counts.forEach((conversion, n) -> System.out.println("  " + conversion + ": " + n));
        if (!problems.isEmpty()) {
            problems.forEach(System.err::println);
            throw new IllegalStateException(problems.size()
                    + " conversion(s) disagree with LWJGL 3's own @NativeType; see the messages above");
        }
        System.out.println("conversions verified: " + lines.size()
                + " non-trivial argument(s), " + problems.size() + " problem(s); wrote " + report);
    }

    private static String check(Delta.Argument argument, Type from, Type to, NativeTypes nativeTypes,
                                int parameter, PointerHandles handles) {
        String declared = nativeTypes.at(parameter);
        switch (argument.conversion()) {
            case PASS, VIEW, BYTE_COUNT -> {

                return null;
            }
            case GL_TYPE -> {

                return NativeTypes.GL_ENUM.equals(declared) ? null
                        : "the element type enum landed on @NativeType(\"" + declared + "\"), not GLenum";
            }
            case HANDLE -> {
                if (nativeTypes.isScalar(parameter)) {
                    return simple(from) + " is a pointer, but LWJGL 3 wants @NativeType(\"" + declared
                            + "\") here -- the parameters do not line up";
                }
                String expected = PointerHandles.cType(from.getInternalName());
                if (expected != null && declared != null
                        && !expected.equals(declared) && !declared.endsWith("*")) {
                    return simple(from) + " stands for " + expected
                            + ", but LWJGL 3 declares @NativeType(\"" + declared + "\")";
                }
                return null;
            }
            case ADDRESS, BYTE_VIEW, RETYPE -> {
                if (nativeTypes.isScalar(parameter)) {
                    return simple(from) + " is passed as memory, but LWJGL 3 wants @NativeType(\""
                            + declared + "\") here -- the parameters do not line up";
                }

                Type effective = argument.conversion() == Conversion.RETYPE ? to : from;
                return widthProblem(from, effective, declared);
            }
            default -> {
                return null;
            }
        }
    }

    private static String widthProblem(Type original, Type effective, String declared) {
        if (declared == null || !declared.endsWith("*")) {
            return null;
        }
        String pointee = declared.substring(0, declared.length() - 1).trim();
        if (pointee.startsWith("const ")) {
            pointee = pointee.substring(6).trim();
        }
        if (pointee.endsWith(" const")) {
            pointee = pointee.substring(0, pointee.length() - 6).trim();
        }
        Integer width = POINTEE_WIDTH.get(pointee);
        if (width == null || width == 0 || !CallPlan.isBuffer(effective)) {
            return null;
        }
        int actual = CallPlan.elementWidth(effective);
        if (actual == width) {
            return null;
        }
        return simple(original) + " has " + actual + "-byte elements, but LWJGL 3 declares @NativeType(\""
                + declared + "\") - " + pointee + " is " + width + " bytes";
    }

    private static String simple(Type type) {
        String name = type.getClassName();
        return name.substring(name.lastIndexOf('.') + 1);
    }
}
