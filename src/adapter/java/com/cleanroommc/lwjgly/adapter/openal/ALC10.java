package com.cleanroommc.lwjgly.adapter.openal;

import com.cleanroommc.lwjgly.rt.AlState;
import org.lwjgl.openal.ALCBridge;
import org.lwjgl.openal.ALCcontext;
import org.lwjgl.openal.ALCdevice;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class ALC10 {

    public static ALCdevice alcOpenDevice(String deviceName) {
        AlState.ensureLoaded();
        long handle = deviceName == null
                ? org.lwjgl.openal.ALC10.alcOpenDevice((ByteBuffer) null)
                : org.lwjgl.openal.ALC10.alcOpenDevice(deviceName);
        AlState.deviceOpened(handle);
        return ALCBridge.device(handle);
    }

    public static boolean alcCloseDevice(ALCdevice device) {
        return org.lwjgl.openal.ALC10.alcCloseDevice(ALCBridge.handle(device));
    }

    public static ALCcontext alcCreateContext(ALCdevice device, IntBuffer attributes) {
        return ALCBridge.context(org.lwjgl.openal.ALC10.alcCreateContext(ALCBridge.handle(device), attributes));
    }

    public static int alcMakeContextCurrent(ALCcontext context) {
        return org.lwjgl.openal.ALC10.alcMakeContextCurrent(ALCBridge.handle(context))
                ? org.lwjgl.openal.ALC10.ALC_TRUE
                : org.lwjgl.openal.ALC10.ALC_FALSE;
    }

    public static void alcProcessContext(ALCcontext context) {
        org.lwjgl.openal.ALC10.alcProcessContext(ALCBridge.handle(context));
    }

    public static void alcSuspendContext(ALCcontext context) {
        org.lwjgl.openal.ALC10.alcSuspendContext(ALCBridge.handle(context));
    }

    public static void alcDestroyContext(ALCcontext context) {
        org.lwjgl.openal.ALC10.alcDestroyContext(ALCBridge.handle(context));
    }

    public static ALCcontext alcGetCurrentContext() {
        return ALCBridge.context(org.lwjgl.openal.ALC10.alcGetCurrentContext());
    }

    public static ALCdevice alcGetContextsDevice(ALCcontext context) {
        return ALCBridge.device(org.lwjgl.openal.ALC10.alcGetContextsDevice(ALCBridge.handle(context)));
    }

    public static int alcGetError(ALCdevice device) {
        return org.lwjgl.openal.ALC10.alcGetError(ALCBridge.handle(device));
    }

    public static boolean alcIsExtensionPresent(ALCdevice device, String extension) {
        return org.lwjgl.openal.ALC10.alcIsExtensionPresent(ALCBridge.handle(device), extension);
    }

    public static int alcGetEnumValue(ALCdevice device, String enumName) {
        return org.lwjgl.openal.ALC10.alcGetEnumValue(ALCBridge.handle(device), enumName);
    }

    public static String alcGetString(ALCdevice device, int token) {
        return org.lwjgl.openal.ALC10.alcGetString(ALCBridge.handle(device), token);
    }

    public static void alcGetInteger(ALCdevice device, int token, IntBuffer destination) {
        org.lwjgl.openal.ALC10.alcGetIntegerv(ALCBridge.handle(device), token, destination);
    }

    private ALC10() { }

}
