package org.lwjgl.openal;

import com.cleanroommc.lwjgly.rt.AlState;

/** LWJGL 2 AL and ALC error checks. */
public final class Util {

    public static void checkALCError(ALCdevice device) {
        int error = ALC10.alcGetError(ALCBridge.handle(device));
        if (error != ALC10.ALC_NO_ERROR) {
            throw new OpenALException(ALC10.alcGetString(AlState.device(), error));
        }
    }

    public static void checkALError() {
        int error = AL10.alGetError();
        if (error != AL10.AL_NO_ERROR) {
            throw new OpenALException(error);
        }
    }

    public static void checkALCValidDevice(ALCdevice device) {
        if (!device.isValid()) {
            throw new OpenALException("Invalid device: " + device);
        }
    }

    public static void checkALCValidContext(ALCcontext context) {
        if (!context.isValid()) {
            throw new OpenALException("Invalid context: " + context);
        }
    }

    private Util() { }

}
