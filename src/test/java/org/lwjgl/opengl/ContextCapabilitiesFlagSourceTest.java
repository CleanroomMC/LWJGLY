package org.lwjgl.opengl;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies whether each capability flag comes from the driver or a constant.
 */
class ContextCapabilitiesFlagSourceTest {

    // Token-only extensions are safe to report from the driver string
    private static final Set<String> EXPECTED_FROM_DRIVER = Set.of(
            "GL_ATI_texture_float",
            "GL_EXT_texture_env_dot3",
            "GL_SGIS_texture_lod",
            "GL_EXT_texture_lod_bias",
            "GL_ARB_robustness_isolation"
    );

    // These extensions have working delegating shims
    private static final Set<String> EXPECTED_DELEGATED = Set.of(
            "GL_EXT_draw_range_elements",
            "GL_EXT_fog_coord",
            "GL_ATI_separate_stencil",
            "GL_AMD_multi_draw_indirect"
    );

    // These extensions are rebound from LWJGL 2's bytecode, so their methods work
    private static final Set<String> EXPECTED_REBOUND = Set.of(
            "GL_ATI_vertex_streams",
            "GL_EXT_vertex_shader",
            "GL_NV_register_combiners",
            "GL_ATI_fragment_shader",
            "GL_NV_vertex_program"
    );

    private static MethodNode constructor() throws IOException {
        ClassNode node = new ClassNode();
        try (InputStream in = ContextCapabilities.class.getResourceAsStream("ContextCapabilities.class")) {
            assertNotNull(in, "ContextCapabilities is not on the test classpath");
            new ClassReader(in).accept(node, 0);
        }
        return node.methods.stream()
                .filter(m -> m.name.equals("<init>"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("ContextCapabilities has no constructor"));
    }

    private static Set<String> lookedUpInDriver(MethodNode constructor) {
        Set<String> names = new TreeSet<>();
        for (AbstractInsnNode insn : constructor.instructions) {
            if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof String name && name.startsWith("GL_")) {
                names.add(name);
            }
        }
        return names;
    }

    @Test
    void readTokenOnlyExtensionsFromTheDriver() throws IOException {
        Set<String> fromDriver = lookedUpInDriver(constructor());
        for (String flag : EXPECTED_FROM_DRIVER) {
            assertTrue(fromDriver.contains(flag), flag + " has no delegate methods and should be read from the driver's extension string, not hardcoded false");
        }
    }

    @Test
    void readDelegatedExtensionsFromTheDriver() throws IOException {
        Set<String> fromDriver = lookedUpInDriver(constructor());
        for (String flag : EXPECTED_DELEGATED) {
            assertTrue(fromDriver.contains(flag), flag + " forwards to LWJGL 3's spelling of the same calls, so the driver's answer is one the shim can keep");
        }
    }

    @Test
    void readReboundExtensionsFromTheDriver() throws IOException {
        Set<String> fromDriver = lookedUpInDriver(constructor());
        for (String flag : EXPECTED_REBOUND) {
            assertTrue(fromDriver.contains(flag), flag + " is rebound from LWJGL 2's bytecode, so the driver's answer is one the shim can keep");
        }
    }
}
