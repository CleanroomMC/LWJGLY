package com.cleanroommc.lwjgly.tool;

import java.util.List;
import java.util.Map;

public final class Delta {

    private Delta() {
    }

    public enum Bucket {

        IDENTICAL,
        INJECT,
        SHIM;

    }

    public enum ShimSource {

        VENDOR,
        REWRITE;

    }

    public enum Fix {

        AUTO,
        AUTO_RENAMED,
        MANUAL,
        NO_TARGET;

        public boolean isAutomatic() {
            return this == AUTO || this == AUTO_RENAMED;
        }

        public boolean isImplementable() {
            return this != NO_TARGET;
        }

    }

    public record Argument(Conversion conversion, int source) { }

    public record Target(String name, String desc, List<Argument> arguments) {

        @Override
        public String toString() {
            return name + desc;
        }

    }

    public record MethodDelta(String name, String desc, Fix fix, Target target, List<String> candidates) { }

    public record FieldDelta(String name, String desc, boolean inlinedConstant) { }

    public record ClassDelta(String internalName, Bucket bucket, ShimSource shimSource, List<String> rejections,
                             List<MethodDelta> methods, List<FieldDelta> fields) {

        public boolean hasWork() {
            return bucket != Bucket.IDENTICAL;
        }

        public long countMethods(Fix fix) {
            return methods.stream().filter(m -> m.fix() == fix).count();
        }

    }

    public record Report(Map<String, ClassDelta> classes, String lwjgl2Classpath, String lwjgl3Classpath) { }

}
