package org.lwjgl.opengl;

import org.lwjgl.PointerWrapperAbstract;

public final class AMDDebugOutputCallback extends PointerWrapperAbstract {

    private final Handler handler;

    public AMDDebugOutputCallback() {
        this((id, category, severity, message) -> System.err.println("[LWJGL] AMD_debug_output message " + id + ": " + message));
    }

    public AMDDebugOutputCallback(Handler handler) {
        super(1L);
        this.handler = handler;
    }

    Handler getHandler() {
        return handler;
    }

    @FunctionalInterface
    public interface Handler {

        void handleMessage(int id, int category, int severity, String message);

    }

}
