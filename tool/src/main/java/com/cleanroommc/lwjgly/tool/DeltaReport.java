package com.cleanroommc.lwjgly.tool;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

public final class DeltaReport {

    public static void write(Path file, Map<String, Delta.ClassDelta> deltas) throws IOException {
        Files.createDirectories(file.getParent());
        try (BufferedWriter out = Files.newBufferedWriter(file)) {
            out.write(summary(deltas));
            out.write("\n\n");

            section(out, "SHIM/VENDOR: LWJGL 2-only, self-contained, vendor the source as-is",
                    deltas, d -> d.bucket() == Delta.Bucket.SHIM && d.shimSource() == Delta.ShimSource.VENDOR,
                    d -> "");

            section(out, "SHIM/REWRITE: LWJGL 2-only, body depends on internals LWJGL 3 dropped",
                    deltas, d -> d.bucket() == Delta.Bucket.SHIM && d.shimSource() == Delta.ShimSource.REWRITE,
                    d -> d.rejections().stream().map(r -> "\n      - " + r).collect(Collectors.joining()));

            section(out, "INJECT: shared class, members to merge into LWJGL 3's own",
                    deltas, d -> d.bucket() == Delta.Bucket.INJECT,
                    DeltaReport::methodLines);

            out.write("\nIDENTICAL: nothing to do (" + count(deltas, d -> d.bucket() == Delta.Bucket.IDENTICAL) + " classes)\n");
        }
    }

    public static String summary(Map<String, Delta.ClassDelta> deltas) {
        long identical = count(deltas, d -> d.bucket() == Delta.Bucket.IDENTICAL);
        long inject = count(deltas, d -> d.bucket() == Delta.Bucket.INJECT);
        long vendor = count(deltas, d -> d.bucket() == Delta.Bucket.SHIM && d.shimSource() == Delta.ShimSource.VENDOR);
        long rewrite = count(deltas, d -> d.bucket() == Delta.Bucket.SHIM && d.shimSource() == Delta.ShimSource.REWRITE);

        long auto = sum(deltas, d -> d.countMethods(Delta.Fix.AUTO));
        long autoRenamed = sum(deltas, d -> d.countMethods(Delta.Fix.AUTO_RENAMED));
        long manual = sum(deltas, d -> d.countMethods(Delta.Fix.MANUAL));
        long noTarget = sum(deltas, d -> d.countMethods(Delta.Fix.NO_TARGET));

        long liveFields = countFields(deltas, f -> !f.inlinedConstant());
        long inlinedFields = countFields(deltas, Delta.FieldDelta::inlinedConstant);

        return """
                LWJGL 2 => LWJGL 3 API Diff
                  %d Classes in Scope
                
                  Classes
                    Identical                   : %d
                    Inject into LWJGL 3         : %d
                    Shim (Vendor)               : %d
                    Shim (Adapter)              : %d
                
                  Members missing from LWJGL 3
                    Auto (Identical Name)       : %d
                    Auto (GL spelling)          : %d
                    Manual methods              : %d
                    No target whatsoever        : %d
                    Fields (Need shim)          : %d
                    Fields (javac inlined)      : %d (No call site survives)
                """.formatted(deltas.size(), identical, inject, vendor, rewrite, auto, autoRenamed, manual, noTarget, liveFields, inlinedFields);
    }

    private static String methodLines(Delta.ClassDelta delta) {
        StringBuilder sb = new StringBuilder();
        for (Delta.MethodDelta method : delta.methods()) {
            sb.append("\n      [").append(method.fix()).append("] ").append(method.name()).append(method.desc());
            if (method.target() != null) {
                sb.append("\n          -> ").append(method.target());
            } else if (!method.candidates().isEmpty()) {
                sb.append("\n          candidates: ").append(String.join(", ", method.candidates()));
            }
        }
        for (Delta.FieldDelta field : delta.fields()) {
            if (!field.inlinedConstant()) {
                sb.append("\n      [FIELD] ").append(field.name()).append(" : ").append(field.desc());
            }
        }
        return sb.toString();
    }

    private static void section(BufferedWriter out, String title, Map<String, Delta.ClassDelta> deltas,
                                Predicate<Delta.ClassDelta> filter, Function<Delta.ClassDelta, String> detail) throws IOException {
        List<Delta.ClassDelta> matching = deltas.values().stream().filter(filter).toList();
        out.write( title + " (" + matching.size() + ")\n");
        for (Delta.ClassDelta delta : matching) {
            out.write("  " + delta.internalName().replace('/', '.') + detail.apply(delta) + "\n");
        }
        out.write("\n");
    }

    private static long count(Map<String, Delta.ClassDelta> deltas, Predicate<Delta.ClassDelta> filter) {
        return deltas.values().stream().filter(filter).count();
    }

    private static long sum(Map<String, Delta.ClassDelta> deltas, ToLongFunction<Delta.ClassDelta> function) {
        return deltas.values().stream().mapToLong(function).sum();
    }

    private static long countFields(Map<String, Delta.ClassDelta> deltas, Predicate<Delta.FieldDelta> filter) {
        return deltas.values().stream().flatMap(d -> d.fields().stream()).filter(filter).count();
    }

    private DeltaReport() { }

}
