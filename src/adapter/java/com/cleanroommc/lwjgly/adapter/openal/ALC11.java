package com.cleanroommc.lwjgly.adapter.openal;

import com.cleanroommc.lwjgly.rt.AlState;
import org.lwjgl.openal.ALCBridge;
import org.lwjgl.openal.ALCdevice;

import java.nio.ByteBuffer;

public final class ALC11 {

    public static ALCdevice alcCaptureOpenDevice(String deviceName, int frequency, int format, int bufferSize) {
        AlState.ensureLoaded();
        long handle = deviceName == null
                ? org.lwjgl.openal.ALC11.alcCaptureOpenDevice((ByteBuffer) null, frequency, format, bufferSize)
                : org.lwjgl.openal.ALC11.alcCaptureOpenDevice(deviceName, frequency, format, bufferSize);
        AlState.deviceOpened(handle);
        return ALCBridge.device(handle);
    }

    public static boolean alcCaptureCloseDevice(ALCdevice device) {
        return org.lwjgl.openal.ALC11.alcCaptureCloseDevice(ALCBridge.handle(device));
    }

    public static void alcCaptureStart(ALCdevice device) {
        org.lwjgl.openal.ALC11.alcCaptureStart(ALCBridge.handle(device));
    }

    public static void alcCaptureStop(ALCdevice device) {
        org.lwjgl.openal.ALC11.alcCaptureStop(ALCBridge.handle(device));
    }

    public static void alcCaptureSamples(ALCdevice device, ByteBuffer buffer, int samples) {
        org.lwjgl.openal.ALC11.alcCaptureSamples(ALCBridge.handle(device), buffer, samples);
    }

    private ALC11() { }

}
