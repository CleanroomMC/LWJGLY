package org.lwjgl.opengl;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Provides LWJGL 2 capabilities for the current LWJGL 3 context. */
public class GLContext {

    private static final Map<GLCapabilities, ContextCapabilities> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Returns capabilities cached for the current GLCapabilities instance.
     *
     * @throws IllegalStateException if this thread has no current GL context
     */
    public static ContextCapabilities getCapabilities() {
        GLCapabilities capabilities = GL.getCapabilities();
        ContextCapabilities existing = CACHE.get(capabilities);
        if (existing != null) {
            return existing;
        }
        // Avoid computeIfAbsent re-entry, a duplicate immutable value is harmless
        ContextCapabilities built = new ContextCapabilities(capabilities);
        CACHE.put(capabilities, built);
        return built;
    }

    /** Does nothing because Cleanroom owns context binding and capability setup. */
    public static void useContext(Object context) {
        throw new UnsupportedOperationException("org.lwjgl.opengl.GLContext.useContext: the window owns the GL context under Cleanroom. See build/lwjgly/PROBLEMS.md");
    }

    public static void useContext(Object context, boolean forwardCompatible) {
        useContext(context);
    }

    /** GL is loaded in demand in LWJGL 3, thus this is no-op. */
    public static void loadOpenGLLibrary() { }

    public static void unloadOpenGLLibrary() { }

}
