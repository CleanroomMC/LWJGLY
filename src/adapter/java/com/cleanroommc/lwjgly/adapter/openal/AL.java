package com.cleanroommc.lwjgly.adapter.openal;

import com.cleanroommc.lwjgly.rt.AlState;
import org.lwjgl.openal.ALCBridge;
import org.lwjgl.openal.ALCcontext;
import org.lwjgl.openal.ALCdevice;

public final class AL {

    public static void create() {
        AlState.create(null, 44100, 60, false);
    }

    public static void create(String deviceArguments, int contextFrequency, int contextRefresh, boolean contextSynchronized) {
        AlState.create(deviceArguments, contextFrequency, contextRefresh, contextSynchronized);
    }

    public static void create(String deviceArguments, int contextFrequency, int contextRefresh, boolean contextSynchronized, boolean openDevice) {
        AlState.create(deviceArguments, contextFrequency, contextRefresh, contextSynchronized);
    }

    public static void destroy() {
        AlState.destroy();
    }

    public static boolean isCreated() {
        return AlState.isCreated();
    }

    public static ALCdevice getDevice() {
        return ALCBridge.device(AlState.device());
    }

    public static ALCcontext getContext() {
        return ALCBridge.context(AlState.context());
    }

    private AL() { }

}
