package com.cleanroommc.lwjgly.tool;

import org.objectweb.asm.Type;

final class JavaNames {

    static String type(Type type) {
        return type.getClassName().replace('$', '.');
    }

    static String className(String internalName) {
        return internalName.replace('/', '.').replace('$', '.');
    }

    static String simpleName(String internalName) {
        int separator = Math.max(internalName.lastIndexOf('/'), internalName.lastIndexOf('$'));
        return internalName.substring(separator + 1);
    }

    private JavaNames() { }

}
