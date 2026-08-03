package com.cleanroommc.lwjgly;

import com.cleanroommc.lwjgly.merge.ClassMerger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <pre>{@code
 * byte[] transformed = LWJGLYTransformer.handles(name) ? LWJGLYTransformer.transform(name, bytes) : bytes;
 * }</pre>
 *
 * <h2>What the caller has to guarantee</h2>
 *
 * <p><b>This must see the first load of each {@code org.lwjgl} class.</b>
 * The JVM forbids adding methods through {@code retransformClasses}, a class that loads before the hook is installed
 * can never be fixed afterwards.
 *
 * <p>{@link #handles(String)} is built to sit on the hot path of every class load.
 */
public final class LWJGLYTransformer {

    private static final String INDEX_RESOURCE = "/lwjgly/adapters.index";
    private static final String ADAPTER_ROOT = "/lwjgly/adapters/";
    /** Target internal name to the adapters that merge into it in order. Usually one except for those with adapters. */
    private static final Map<String, List<String>> ADAPTERS = loadIndex();

    public static boolean handles(String internalName) {
        return internalName != null
                && internalName.length() > 10
                && internalName.charAt(0) == 'o' // L
                && internalName.charAt(3) == '/' // O
                && internalName.charAt(9) == '/' // L
                && internalName.startsWith("org/lwjgl/")
                && ADAPTERS.containsKey(internalName);
    }

    /**
     * Returns {@code classBytes} with LWJGL 2's missing methods merged in, or the argument
     * unchanged if there is nothing to do.
     *
     * @throws ClassMerger.MergeException if an adapter is malformed, broken adapters are build defects.
     */
    public static byte[] transform(String internalName, byte[] classBytes) {
        List<String> resources = ADAPTERS.get(internalName);
        if (resources == null || classBytes == null) {
            return classBytes;
        }
        byte[] merged = classBytes;
        for (String resource : resources) {
            merged = ClassMerger.merge(merged, LWJGLYTransformer.readResource(resource));
        }
        return merged;
    }

    public static Set<String> targets() {
        return Set.copyOf(ADAPTERS.keySet());
    }

    /** Each line is {@code <target>=<adapter>,<adapter>...} in merge order. */
    private static Map<String, List<String>> loadIndex() {
        Map<String, List<String>> index = new HashMap<>();
        try (InputStream in = LWJGLYTransformer.class.getResourceAsStream(INDEX_RESOURCE)) {
            if (in == null) {
                return index;
            }
            for (String line : new String(readAll(in), StandardCharsets.UTF_8).split("\n")) {
                String entry = line.trim();
                if (entry.isEmpty() || entry.startsWith("#")) {
                    continue;
                }
                int equals = entry.indexOf('=');
                List<String> adapters = new ArrayList<>(2);
                for (String adapter : entry.substring(equals + 1).split(",")) {
                    adapters.add(ADAPTER_ROOT + adapter + ".class");
                }
                index.put(entry.substring(0, equals), adapters);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + INDEX_RESOURCE, e);
        }
        return index;
    }

    private static byte[] readResource(String resource) {
        try (InputStream in = LWJGLYTransformer.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Adapter index lists " + resource + ", but it is not in the jar");
            }
            return readAll(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + resource, e);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private LWJGLYTransformer() { }

}
