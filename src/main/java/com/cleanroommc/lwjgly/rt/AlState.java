package com.cleanroommc.lwjgly.rt;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/** Holds the LWJGL 2 OpenAL lifecycle state used by merged adapters. */
public final class AlState {

    private static boolean libraryLoaded;
    private static long device;
    private static long context;

    /** Loads OpenAL once, including callers that bypass {@code AL.create()}. */
    public static synchronized void ensureLoaded() {
        if (libraryLoaded) {
            return;
        }
        try {
            ALC.create();
        } catch (IllegalStateException alreadyCreated) {
            // AL may initialize ALC before this state sees it, treat that as success
        }
        libraryLoaded = true;
    }

    /** Binds capabilities for a newly opened device. */
    public static synchronized void deviceOpened(long handle) {
        if (handle != 0L) {
            ALC.setCapabilities(ALC.createCapabilities(handle));
        }
    }

    public static synchronized boolean isCreated() {
        return context != 0L;
    }

    public static synchronized long device() {
        return device;
    }

    public static synchronized long context() {
        return context;
    }

    /** Runs LWJGL 2's create sequence on the default device. */
    public static synchronized void create(String deviceName, int frequency, int refresh, boolean synchronized_) {
        if (context != 0L) {
            return; // LWJGL 2 threw here, but a second call is not worth failing game startup
        }
        ensureLoaded();
        device = deviceName == null ? ALC10.alcOpenDevice((ByteBuffer) null) : ALC10.alcOpenDevice(deviceName);
        if (device == 0L) {
            throw new IllegalStateException("Could not open the OpenAL device" + (deviceName == null ? "" : " '" + deviceName + "'."));
        }
        ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
        ALC.setCapabilities(alcCapabilities);

        // Zero requests the driver defaults used by paulscode
        context = ALC10.alcCreateContext(device, attributes(frequency, refresh, synchronized_));
        if (context == 0L) {
            ALC10.alcCloseDevice(device);
            device = 0L;
            throw new IllegalStateException("Could not create the OpenAL context.");
        }
        ALC10.alcMakeContextCurrent(context);
        AL.setCurrentProcess(AL.createCapabilities(alcCapabilities));
    }

    /** Destroys LWJGL 2 state and clears LWJGL 3 process capabilities. */
    public static synchronized void destroy() {
        AL.setCurrentProcess(null);
        if (context != 0L) {
            ALC10.alcMakeContextCurrent(0L);
            ALC10.alcDestroyContext(context);
            context = 0L;
        }
        if (device != 0L) {
            ALC10.alcCloseDevice(device);
            device = 0L;
        }
    }

    private static IntBuffer attributes(int frequency, int refresh, boolean synchronized_) {
        if (frequency == 0 && refresh == 0 && !synchronized_) {
            return null;
        }
        IntBuffer attributes = org.lwjgl.BufferUtils.createIntBuffer(7);
        return attributes.put(ALC10.ALC_FREQUENCY)
                .put(frequency)
                .put(ALC10.ALC_REFRESH)
                .put(refresh)
                .put(ALC10.ALC_SYNC)
                .put(synchronized_ ? ALC10.ALC_TRUE : ALC10.ALC_FALSE)
                .put(0)
                .flip();
    }

    private AlState() { }

}
