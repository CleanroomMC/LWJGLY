package com.cleanroommc.lwjgly.tool;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaNamesTest {

    @Test
    void rendersNestedTypesAsJavaSourceNames() {
        assertEquals("org.lwjgl.opengl.KHRDebugCallback.Handler", JavaNames.type(Type.getObjectType("org/lwjgl/opengl/KHRDebugCallback$Handler")));
        assertEquals("org.lwjgl.opengl.KHRDebugCallback.Handler[][]", JavaNames.type(Type.getType("[[Lorg/lwjgl/opengl/KHRDebugCallback$Handler;")));
    }

    @Test
    void findsTheSimpleNameOfNestedTypes() {
        assertEquals("Handler", JavaNames.simpleName("org/lwjgl/opengl/KHRDebugCallback$Handler"));
    }

}
