package org.lwjgl.opengl;

import org.junit.jupiter.api.Test;
import org.lwjgl.sdl.SDL;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The GL extension classes the generator rebinds from LWJGL 2's bytecode. */
class RebindTest {

    private static final Set<String> CHECKED = Set.of(
            "org/lwjgl/opengl/LegacyGL", "org/lwjgl/opengl/LegacyGL$References",
            "org/lwjgl/system/JNI", "org/lwjgl/opengl/ContextCapabilities"
    );

    @Test
    void everyRewrittenReferenceExists() throws Exception {
        List<String> missing = new ArrayList<>();
        int rebound = 0;
        for (ClassNode node : opengl()) {
            boolean touched = false;
            for (MethodNode method : node.methods) {
                for (AbstractInsnNode insn : method.instructions) {
                    if (insn instanceof MethodInsnNode call && CHECKED.contains(call.owner)) {
                        touched = true;
                        if (!hasMethod(call.owner, call.name, call.desc)) {
                            missing.add(node.name + " -> " + call.owner + "." + call.name + call.desc);
                        }
                    } else if (insn instanceof FieldInsnNode access && CHECKED.contains(access.owner)) {
                        touched |= !access.owner.equals("org/lwjgl/opengl/ContextCapabilities");
                        if (!hasField(access.owner, access.name, access.desc)) {
                            missing.add(node.name + " -> " + access.owner + "." + access.name + " : " + access.desc);
                        }
                    }
                }
            }
            if (touched && !node.name.startsWith("org/lwjgl/opengl/LegacyGL")) {
                rebound++;
            }
        }
        assertTrue(rebound > 20, "Expected the rebound extension classes on the classpath, found " + rebound);
        assertEquals(List.of(), missing);
    }

    @Test
    void libffiRoundTripsEachArgumentKind() {
        long abs = SDL.getLibrary().getFunctionAddress("SDL_abs");
        long fabsf = SDL.getLibrary().getFunctionAddress("SDL_fabsf");
        long fabs = SDL.getLibrary().getFunctionAddress("SDL_fabs");

        assertEquals(5, (int) LegacyGL.ffi(abs, "(IJ)I", new long[] { -5 }));
        assertEquals(2.5F, Float.intBitsToFloat((int) LegacyGL.ffi(fabsf, "(FJ)F", new long[] { Float.floatToRawIntBits(-2.5F) })));
        assertEquals(0.25D, Double.longBitsToDouble(LegacyGL.ffi(fabs, "(DJ)D", new long[] { Double.doubleToRawLongBits(-0.25D) })));
    }

    private static boolean hasMethod(String owner, String name, String desc) throws ClassNotFoundException {
        if (name.equals("<init>")) {
            return Stream.of(load(owner).getDeclaredConstructors()).anyMatch(c -> Type.getConstructorDescriptor(c).equals(desc));
        }
        for (Class<?> type = load(owner); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getName().equals(name) && Type.getMethodDescriptor(method).equals(desc)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasField(String owner, String name, String desc) throws ClassNotFoundException {
        for (Field field : load(owner).getDeclaredFields()) {
            if (field.getName().equals(name) && Type.getDescriptor(field.getType()).equals(desc)) {
                return true;
            }
        }
        return false;
    }

    private static Class<?> load(String internalName) throws ClassNotFoundException {
        return Class.forName(internalName.replace('/', '.'), false, RebindTest.class.getClassLoader());
    }

    private static List<ClassNode> opengl() throws IOException {
        List<ClassNode> nodes = new ArrayList<>();
        Enumeration<URL> roots = RebindTest.class.getClassLoader().getResources("org/lwjgl/opengl");
        while (roots.hasMoreElements()) {
            URL root = roots.nextElement();
            if (!root.getProtocol().equals("file")) {
                continue;
            }
            try (Stream<Path> files = Files.list(Paths.get(root.getPath()))) {
                for (Path file : files.filter(p -> p.toString().endsWith(".class")).toList()) {
                    ClassNode node = new ClassNode();
                    try (InputStream in = Files.newInputStream(file)) {
                        new ClassReader(in).accept(node, 0);
                    }
                    nodes.add(node);
                }
            }
        }
        return nodes;
    }

}
