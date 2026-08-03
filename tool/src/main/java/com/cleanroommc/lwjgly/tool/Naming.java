package com.cleanroommc.lwjgly.tool;

import org.objectweb.asm.Type;

import java.util.*;

public final class Naming {

    private static final Map<String, List<String>> SUFFIXES = new LinkedHashMap<>();
    private static final Map<Character, List<String>> RETURN_SUFFIXES = new LinkedHashMap<>();
    private static final Set<String> VENDOR_TAGS = Set.of(
            "ARB", "EXT", "NV", "NVX", "AMD", "ATI", "APPLE", "SGIS", "SGIX", "SGI", "SUN", "SUNX", "IBM", "INTEL",
            "KHR", "OES", "MESA", "HP", "PGI", "REND", "S3", "WIN", "3DFX", "INGR", "OML", "GREMEDY", "AMDX"
    );
    private static final List<String> INDEXED = List.of("i_v", "i", "v");

    static {
        SUFFIXES.put("java/nio/FloatBuffer", List.of("fv", "f"));
        SUFFIXES.put("java/nio/IntBuffer", List.of("iv", "i", "uiv", "ui"));
        SUFFIXES.put("java/nio/DoubleBuffer", List.of("dv", "d"));
        SUFFIXES.put("java/nio/ShortBuffer", List.of("sv", "s", "usv", "us", "hv", "h"));
        SUFFIXES.put("java/nio/ByteBuffer", List.of("bv", "b", "ubv", "ub"));
        SUFFIXES.put("java/nio/LongBuffer", List.of("i64v", "ui64v", "i64", "ui64"));

        RETURN_SUFFIXES.put('J', List.of("i64", "ui64"));
        RETURN_SUFFIXES.put('I', List.of("i", "ui"));
        RETURN_SUFFIXES.put('S', List.of("s", "us"));
        RETURN_SUFFIXES.put('B', List.of("b", "ub"));
        RETURN_SUFFIXES.put('F', List.of("f"));
        RETURN_SUFFIXES.put('D', List.of("d"));
    }

    public static List<String> candidateNames(String lwjgl2Name, String lwjgl2Desc) {
        String tag = trailingVendorTag(lwjgl2Name);
        String withoutTag = tag == null ? lwjgl2Name : lwjgl2Name.substring(0, lwjgl2Name.length() - tag.length());
        boolean unsigned = withoutTag.endsWith("u");
        String base = unsigned ? withoutTag.substring(0, withoutTag.length() - 1) : withoutTag;

        List<String> names = new ArrayList<>();
        String bufferType = lastBufferParameter(lwjgl2Desc);

        List<String> suffixes = bufferType == null ?
                RETURN_SUFFIXES.getOrDefault(returnTypeKey(lwjgl2Desc), List.of()) :
                SUFFIXES.getOrDefault(bufferType, List.of());
        List<String> applicable = new ArrayList<>();
        for (String suffix : suffixes) {
            if (unsigned != suffix.startsWith("u")) {
                continue;
            }
            applicable.add(suffix);
            add(names, base, suffix, tag);
        }
        if (!unsigned) {
            for (String suffix : INDEXED) {
                add(names, base, suffix, tag);
            }
        }

        for (String suffix : applicable) {
            for (String indexed : INDEXED) {
                add(names, base, suffix + indexed, tag);
            }
        }
        return names;
    }

    private static void add(List<String> names, String base, String suffix, String tag) {
        String name = tag == null ? base + suffix : base + suffix + tag;
        if (!names.contains(name)) {
            names.add(name);
        }
    }

    private static char returnTypeKey(String desc) {
        Type returnType = Type.getMethodType(desc).getReturnType();
        return switch (returnType.getSort()) {
            case Type.LONG -> 'J';
            case Type.INT -> 'I';
            case Type.SHORT -> 'S';
            case Type.BYTE -> 'B';
            case Type.FLOAT -> 'F';
            case Type.DOUBLE -> 'D';
            default -> 0;
        };
    }

    private static String trailingVendorTag(String name) {
        for (String tag : VENDOR_TAGS) {
            if (name.endsWith(tag) && name.length() > tag.length()) {
                return tag;
            }
        }
        return null;
    }

    private static String lastBufferParameter(String desc) {
        Type[] args = Type.getMethodType(desc).getArgumentTypes();
        for (int i = args.length - 1; i >= 0; i--) {
            if (args[i].getSort() == Type.OBJECT && SUFFIXES.containsKey(args[i].getInternalName())) {
                return args[i].getInternalName();
            }
        }
        return null;
    }

    private Naming() { }

}
