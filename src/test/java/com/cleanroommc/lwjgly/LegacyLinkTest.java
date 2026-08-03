package com.cleanroommc.lwjgly;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Takes a class compiled against real LWJGL 2 and resolves every reference it makes against the runtime LWJGLY produces.
 *
 * <p>This is the closest thing to running a mod that a test can be.
 * The references are not written by hand: they are read out of {@code LegacyMod}'s class exactly as javac emitted,
 * which means the test cannot accidentally agree with the shims about a signature.
 *
 * <p>What fails here is precisely what would reach the end user as {@code NoSuchMethodError} or {@code NoSuchFieldError}.
 */
class LegacyLinkTest {

    @Test
    void makeSureShimsAreNotInvolved() throws Exception {
        Path fixtures = Paths.get(System.getProperty("fixtures.dir"));
        Path legacyFixture = fixtures.resolve("fixture/LegacyFixture.class");
        assertTrue(Files.isRegularFile(legacyFixture));

        ClassNode node = new ClassNode();
        try (InputStream in = Files.newInputStream(legacyFixture)) {
            new ClassReader(in).accept(node, 0);
        }

        assertTrue(references(node).stream().anyMatch(r -> r.startsWith("org/lwjgl/opengl/GL11.glFog(")), "The fixture should reference LWJGL 2's glFog overload, which LWJGL 3 does not declare");
        assertFalse(references(node).stream().anyMatch(r -> r.startsWith("com/cleanroommc/")), "The fixture must not reference LWJGLY");
    }

    @Test
    void theResolverActuallyDetectsAMissingMember() {
        MergedRuntime runtime = new MergedRuntime();
        assertNotNull(runtime.resolve("org/lwjgl/opengl/GL11.glThisDoesNotExist()V"));
        assertNotNull(runtime.resolve("org/lwjgl/opengl/GL11.glFog(ILjava/nio/DoubleBuffer;)V"), "Wrong buffer types should not resolve");
        assertNotNull(runtime.resolve("org/lwjgl/opengl/ContextCapabilities.NotAFlag:Z"));
        assertNotNull(runtime.resolve("class org/lwjgl/opengl/NoSuchClass"));
        assertNull(runtime.resolve("org/lwjgl/opengl/GL11.glFog(ILjava/nio/FloatBuffer;)V"), "Merged adapter method");
        assertNull(runtime.resolve("org/lwjgl/opengl/Display.getWidth()I"), "Shipped shim");
        assertNull(runtime.resolve("org/lwjgl/opengl/GL11.glEnable(I)V"), "LWJGL 3's own");
    }

    private static List<String> references(ClassNode node) {
        List<String> references = new ArrayList<>();
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode call && call.owner.startsWith("org/lwjgl/")) {
                    references.add(call.owner + "." + call.name + call.desc);
                } else if (insn instanceof FieldInsnNode access && access.owner.startsWith("org/lwjgl/")) {
                    references.add(access.owner + "." + access.name + ":" + access.desc);
                } else if (insn instanceof TypeInsnNode type && type.desc.startsWith("org/lwjgl/")) {
                    references.add("class " + type.desc);
                }
            }
        }
        return references;
    }

    /**
     * Loads {@code org.lwjgl} through the transformer, exactly as Cleanroom's classloader would
     */
    private static final class MergedRuntime extends ClassLoader {

        private final Map<String, Class<?>> defined = new HashMap<>();

        MergedRuntime() {
            super(LegacyLinkTest.class.getClassLoader());
        }

        String resolve(String reference) {
            try {
                if (reference.startsWith("class ")) {
                    loadClass(reference.substring("class ".length()).replace('/', '.'));
                    return null;
                }
                int colon = reference.indexOf(':');
                int split = reference.lastIndexOf('.', colon >= 0 ? colon : reference.indexOf('('));
                String owner = reference.substring(0, split).replace('/', '.');
                Class<?> type = loadClass(owner);
                if (colon >= 0) {
                    return findField(type, reference.substring(split + 1, colon),
                            Type.getType(reference.substring(colon + 1)))
                            ? null : "missing field " + reference;
                }
                int paren = reference.indexOf('(');
                return findMethod(type, reference.substring(split + 1, paren),
                        Type.getMethodType(reference.substring(paren)))
                        ? null : "missing method " + reference;
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                return "missing class for " + reference + " (" + e.getMessage() + ")";
            }
        }

        private boolean findField(Class<?> type, String name, Type expected) {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                for (Field field : current.getDeclaredFields()) {
                    if (field.getName().equals(name) && descriptorOf(field.getType()).equals(expected)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean findMethod(Class<?> type, String name, Type expected) {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                if (name.equals("<init>")) {
                    for (Constructor<?> constructor : current.getDeclaredConstructors()) {
                        if (matches(constructor, expected, void.class)) {
                            return true;
                        }
                    }
                } else {
                    for (Method method : current.getDeclaredMethods()) {
                        if (method.getName().equals(name) && matches(method, expected, method.getReturnType())) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private boolean matches(Executable executable, Type expected, Class<?> returnType) {
            Type[] wanted = expected.getArgumentTypes();
            Class<?>[] actual = executable.getParameterTypes();
            if (wanted.length != actual.length) {
                return false;
            }
            for (int i = 0; i < wanted.length; i++) {
                if (!descriptorOf(actual[i]).equals(wanted[i])) {
                    return false;
                }
            }
            return descriptorOf(returnType).equals(expected.getReturnType());
        }

        private static Type descriptorOf(Class<?> type) {
            return Type.getType(type);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith("org.lwjgl.")) {
                return super.loadClass(name, resolve);
            }
            Class<?> existing = defined.get(name);
            if (existing != null) {
                return existing;
            }
            String internalName = name.replace('.', '/');
            byte[] bytes = read("/" + internalName + ".class");
            if (bytes == null) {
                throw new ClassNotFoundException(name);
            }
            if (LWJGLYTransformer.handles(internalName)) {
                bytes = LWJGLYTransformer.transform(internalName, bytes);
            }
            Class<?> type = defineClass(name, bytes, 0, bytes.length);
            defined.put(name, type);
            return type;
        }

        private byte[] read(String resource) {
            try (InputStream in = LegacyLinkTest.class.getResourceAsStream(resource)) {
                if (in == null) {
                    return null;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int count;
                while ((count = in.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
                return out.toByteArray();
            } catch (IOException e) {
                return null;
            }
        }
    }
}
