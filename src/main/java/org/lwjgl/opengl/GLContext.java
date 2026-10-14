package org.lwjgl.opengl;

import org.lwjgl.LWJGLException;

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

    public static synchronized void useContext(Object context) throws LWJGLException {
        useContext(context, false);
    }

    /**
     * Loads capabilities for the context current on this thread, or clears them for {@code null}.
     * A context made current through LWJGLY already has its own, so this only acts on one made current natively.
     */
    public static synchronized void useContext(Object context, boolean forwardCompatible) throws LWJGLException {
        if (context == null) {
            GL.setCapabilities(null);
        } else if (!Display.isCurrent()) {
            GL.createCapabilities(forwardCompatible);
        }
    }

    /** GL is loaded in demand in LWJGL 3, thus this is no-op. */
    public static void loadOpenGLLibrary() { }

    public static void unloadOpenGLLibrary() { }

}
