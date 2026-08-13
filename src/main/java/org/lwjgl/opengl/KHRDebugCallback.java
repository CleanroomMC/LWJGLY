package org.lwjgl.opengl;

import org.lwjgl.PointerWrapperAbstract;

public final class KHRDebugCallback extends PointerWrapperAbstract {

    private final Handler handler;

    public KHRDebugCallback() {
        this((source, type, id, severity, message) -> System.err.println("[LWJGL] KHR_debug message " + id + ": " + message));
    }

    public KHRDebugCallback(Handler handler) {
        super(1L);
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
