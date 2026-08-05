package org.lwjgl.openal;

/** Converts LWJGL 2 ALC wrappers to and from LWJGL 3 handles. */
public final class ALCBridge {

    public static ALCdevice device(long handle) {
        return handle == 0L ? null : new ALCdevice(handle);
    }

    public static long handle(ALCdevice device) {
        return device == null ? 0L : device.device;
    }

    public static ALCcontext context(long handle) {
        return handle == 0L ? null : new ALCcontext(handle);
    }

    public static long handle(ALCcontext context) {
        return context == null ? 0L : context.context;
    }

    private ALCBridge() { }

}
