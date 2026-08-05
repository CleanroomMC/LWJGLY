package org.lwjgl.opengl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Reads driver extension names for capability flags absent from LWJGL 3. */
final class DriverExtensions {

    static Set<String> of(GLCapabilities capabilities) {
        try {
            return capabilities.OpenGL30 ? indexed() : flat();
        } catch (Throwable ignored) {
            return Collections.emptySet();
        }
    }

    private static Set<String> indexed() {
        int count = GL11.glGetInteger(GL30.GL_NUM_EXTENSIONS);
        if (count <= 0) {
            return Collections.emptySet();
        }
        Set<String> names = new HashSet<>(count * 2);
        for (int i = 0; i < count; i++) {
            String name = GL30.glGetStringi(GL11.GL_EXTENSIONS, i);
            if (name != null) {
                names.add(name);
            }
        }
        return names;
    }

    private static Set<String> flat() {
        String blob = GL11.glGetString(GL11.GL_EXTENSIONS);
        if (blob == null || blob.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> names = new HashSet<>();
        for (String name : blob.split(" ")) {
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }

    private DriverExtensions() { }

}
