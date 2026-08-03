package com.cleanroommc.lwjgly.tool;

public enum Outcome {

    GAP("Would not link, `NoSuchMethodError` or `NoSuchFieldError` would happen"),
    THROWS("Links, but the body only throws: see `build/lwjgly/PROBLEMS.md`"),
    SHIM("Served by a org.lwjgl shim"),
    MERGED("Served by an adapter"),
    LWJGL3("LWJGL 3 already has it, nothing to do");

    final String description;

    Outcome(String description) {
        this.description = description;
    }

    public boolean works() {
        return this == SHIM || this == MERGED || this == LWJGL3;
    }

}
