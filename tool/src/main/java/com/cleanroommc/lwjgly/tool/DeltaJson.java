package com.cleanroommc.lwjgly.tool;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class DeltaJson {

    public static void write(Path file, Map<String, Delta.ClassDelta> deltas, String lwjgl2Classpath, String lwjgl3Classpath) throws IOException {
        try (BufferedWriter out = Files.newBufferedWriter(file)) {
            out.write("{\n");
            out.write("  \"lwjgl2\": " + quote(lwjgl2Classpath) + ",\n");
            out.write("  \"lwjgl3\": " + quote(lwjgl3Classpath) + ",\n");
            out.write("  \"classes\": [\n");
            boolean firstClass = true;
            for (Delta.ClassDelta delta : deltas.values()) {
                if (!firstClass) {
                    out.write(",\n");
                }
                firstClass = false;
                writeClass(out, delta);
            }
            out.write("\n  ]\n}\n");
        }
    }

    private static void writeClass(BufferedWriter out, Delta.ClassDelta delta) throws IOException {
        out.write("    {\n");
        out.write("      \"name\": " + quote(delta.internalName()) + ",\n");
        out.write("      \"bucket\": " + quote(delta.bucket().name()) + ",\n");
        if (delta.shimSource() != null) {
            out.write("      \"shimSource\": " + quote(delta.shimSource().name()) + ",\n");
        }
        if (!delta.rejections().isEmpty()) {
            out.write("      \"rejections\": " + array(delta.rejections()) + ",\n");
        }
        out.write("      \"methods\": [");
        boolean first = true;
        for (Delta.MethodDelta method : delta.methods()) {
            if (!first) {
                out.write(",");
            }
            first = false;
            out.write("\n        {\"name\": " + quote(method.name())
                    + ", \"desc\": " + quote(method.desc())
                    + ", \"fix\": " + quote(method.fix().name())
                    + (method.target() == null ? "" : ", \"targetName\": " + quote(method.target().name())
                                                      + ", \"targetDesc\": " + quote(method.target().desc()))
                    + ", \"candidates\": " + array(method.candidates()) + "}");
        }
        out.write(delta.methods().isEmpty() ? "]," : "\n      ],\n");
        out.write("      \"fields\": [");
        first = true;
        for (Delta.FieldDelta field : delta.fields()) {
            if (!first) {
                out.write(",");
            }
            first = false;
            out.write("\n        {\"name\": " + quote(field.name())
                    + ", \"desc\": " + quote(field.desc())
                    + ", \"inlinedConstant\": " + field.inlinedConstant() + "}");
        }
        out.write(delta.fields().isEmpty() ? "]\n" : "\n      ]\n");
        out.write("    }");
    }

    private static String array(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(quote(values.get(i)));
        }
        return sb.append(']').toString();
    }

    private static String quote(String value) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    private DeltaJson() { }

}
