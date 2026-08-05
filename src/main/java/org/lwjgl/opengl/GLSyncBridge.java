package org.lwjgl.opengl;

/** Wraps LWJGL 3 sync handles in LWJGL 2 GLSync objects. */
public final class GLSyncBridge {

    /** Wraps a handle, including zero, which LWJGL 2 represents as an invalid GLSync. */
    public static GLSync wrap(long handle) {
        return new GLSync(handle);
    }

    private GLSyncBridge() { }

}
