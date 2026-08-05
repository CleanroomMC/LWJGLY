package org.lwjgl.opengl;

import org.lwjgl.PointerWrapperAbstract;

public final class ARBDebugOutputCallback extends PointerWrapperAbstract {

    private final Handler handler;

    public ARBDebugOutputCallback() {
        this((source, type, id, severity, message) -> System.err.println("[LWJGL] ARB_debug_output message " + id + ": " + message));
    }

    public ARBDebugOutputCallback(Handler handler) {
        this.handler = handler;
    }

    Handler getHandler() {
        return handler;
    }

    @FunctionalInterface
    public interface Handler {

        void handleMessage(int source, int type, int id, int severity, String message);

    }

}
