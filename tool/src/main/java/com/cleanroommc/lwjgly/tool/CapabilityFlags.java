package com.cleanroommc.lwjgly.tool;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;

public final class CapabilityFlags {

    private static final String LWJGL2_CAPS = "org/lwjgl/opengl/ContextCapabilities";
    private static final String LWJGL3_CAPS = "org/lwjgl/opengl/GLCapabilities";
    private static final String EXTENSION_PACKAGE = "org/lwjgl/opengl/";

    public static Split of(ApiIndex lwjgl2, ApiIndex lwjgl3, Serviced serviced) {
        ClassNode lwjgl2Caps = lwjgl2.get(LWJGL2_CAPS);
        ClassNode lwjgl3Caps = lwjgl3.get(LWJGL3_CAPS);
        if (lwjgl2Caps == null || lwjgl3Caps == null) {
            return new Split(List.of(), Collections.emptySortedMap());
        }

        Set<String> available = new TreeSet<>();
        lwjgl3Caps.fields.stream()
                .filter(f -> f.desc.equals("Z"))
                .filter(f -> ApiIndex.isCallable(f.access))
                .map(f -> f.name)
                .forEach(available::add);

        Map<String, ClassNode> extensionClasses = extensionClasses(lwjgl2);
        List<String> fromDriver = new ArrayList<>();
        SortedMap<String, Blocked> alwaysFalse = new TreeMap<>();
        lwjgl2Caps.fields.stream()
                .filter(f -> f.desc.equals("Z"))
                .filter(f -> ApiIndex.isCallable(f.access))
                .map(f -> f.name)
                .filter(name -> !available.contains(name))
                .forEach(flag -> {
                    ClassNode extension = extensionClasses.get(normalise(flag));
                    int methods = extension == null ? 0 : callableMethods(extension);
                    if (methods == 0 || serviced.test(extension.name)) {
                        fromDriver.add(flag);
                    } else {
                        alwaysFalse.put(flag, new Blocked(simpleName(extension), methods));
                    }
                });
        return new Split(List.copyOf(fromDriver), Collections.unmodifiableSortedMap(alwaysFalse));
    }

    private static Map<String, ClassNode> extensionClasses(ApiIndex lwjgl2) {
        Map<String, ClassNode> byName = new TreeMap<>();
        for (Map.Entry<String, ClassNode> entry : lwjgl2.classes().entrySet()) {
            String internalName = entry.getKey();
            if (!internalName.startsWith(EXTENSION_PACKAGE)) {
                continue;
            }
            String simple = internalName.substring(EXTENSION_PACKAGE.length());
            if (simple.indexOf('/') >= 0 || simple.indexOf('$') >= 0) {
                continue;
            }
            byName.putIfAbsent(simple.replace("_", "").toLowerCase(Locale.ROOT), entry.getValue());
        }
        return byName;
    }

    private static String normalise(String flag) {
        String withoutPrefix = flag.startsWith("GL_") ? flag.substring(3) : flag;
        return withoutPrefix.replace("_", "").toLowerCase(Locale.ROOT);
    }

    private static int callableMethods(ClassNode node) {
        int count = 0;
        for (MethodNode method : node.methods) {
            if (ApiIndex.isCallable(method.access) && method.name.charAt(0) != '<') {
                count++;
            }
        }
        return count;
    }

    private static String simpleName(ClassNode node) {
        return node.name.substring(node.name.lastIndexOf('/') + 1);
    }

    private CapabilityFlags() { }

    @FunctionalInterface
    public interface Serviced {

        boolean test(String internalName);

    }

    public record Blocked(String className, int callableMethods) { }

    public record Split(List<String> fromDriver, SortedMap<String, Blocked> alwaysFalse) {

        public int total() {
            return fromDriver.size() + alwaysFalse.size();
        }

    }

}
