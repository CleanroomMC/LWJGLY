package com.cleanroommc.lwjgly.tool;

import org.objectweb.asm.tree.ClassNode;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class PointerHandles {

    public static final String POINTER_WRAPPER = "org/lwjgl/PointerWrapper";

    private static final Map<String, String> C_TYPES = Map.of(
            "org/lwjgl/opengl/GLSync", "GLsync",
            "org/lwjgl/opencl/CLContext", "cl_context",
            "org/lwjgl/opencl/CLEvent", "cl_event",
            "org/lwjgl/opencl/CLMem", "cl_mem",
            "org/lwjgl/opencl/CLKernel", "cl_kernel",
            "org/lwjgl/opencl/CLProgram", "cl_program",
            "org/lwjgl/opencl/CLCommandQueue", "cl_command_queue",
            "org/lwjgl/opencl/CLDevice", "cl_device_id",
            "org/lwjgl/opencl/CLPlatform", "cl_platform_id",
            "org/lwjgl/opencl/CLSampler", "cl_sampler"
    );

    private static final Map<String, String> FACTORIES = Map.of("org/lwjgl/opengl/GLSync", "org.lwjgl.opengl.GLSyncBridge.wrap");

    public static PointerHandles in(ApiIndex lwjgl2) {
        Set<String> found = new LinkedHashSet<>();
        for (ClassNode node : lwjgl2.classes().values()) {
            if (implementsWrapper(lwjgl2, node.name)) {
                found.add(node.name);
            }
        }
        return new PointerHandles(found);
    }

    public static PointerHandles none() {
        return new PointerHandles(Set.of());
    }

    private static boolean implementsWrapper(ApiIndex index, String internalName) {
        if (POINTER_WRAPPER.equals(internalName)) {
            return true;
        }
        ClassNode node = index.get(internalName);
        if (node == null) {
            return false;
        }
        for (String itf : node.interfaces) {
            if (implementsWrapper(index, itf)) {
                return true;
            }
        }
        return node.superName != null && implementsWrapper(index, node.superName);
    }

    public static String factory(String internalName) {
        return FACTORIES.get(internalName);
    }

    public static String cType(String internalName) {
        return C_TYPES.get(internalName);
    }

    private final Set<String> types;

    private PointerHandles(Set<String> types) {
        this.types = types;
    }

    public boolean isHandle(String internalName) {
        return types.contains(internalName);
    }

    public boolean canWrap(String internalName) {
        return isHandle(internalName) && FACTORIES.containsKey(internalName);
    }

    public Set<String> types() {
        return types;
    }

}
