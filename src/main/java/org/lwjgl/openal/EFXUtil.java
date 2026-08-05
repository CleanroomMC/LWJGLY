package org.lwjgl.openal;

import com.cleanroommc.lwjgly.rt.AlState;

/** Probes ALC_EXT_EFX effect and filter support. */
public final class EFXUtil {

    private static final int EFFECT = 1111;
    private static final int FILTER = 2222;

    private EFXUtil() {
    }

    /** Returns whether the current device reports ALC_EXT_EFX. */
    public static boolean isEfxSupported() {
        return AlState.isCreated() && ALC10.alcIsExtensionPresent(AlState.device(), EFX10.ALC_EXT_EFX_NAME);
    }

    /**
     * Whether the driver implements this effect type, e.g. {@code AL_EFFECT_REVERB}.
     *
     * @throws IllegalArgumentException if {@code effectType} is not an EFX 1.0 effect
     * @throws OpenALException          if the test ran out of memory, or OpenAL is not created
     */
    public static boolean isEffectSupported(int effectType) {
        switch (effectType) {
            case EFX10.AL_EFFECT_NULL:
            case EFX10.AL_EFFECT_EAXREVERB:
            case EFX10.AL_EFFECT_REVERB:
            case EFX10.AL_EFFECT_CHORUS:
            case EFX10.AL_EFFECT_DISTORTION:
            case EFX10.AL_EFFECT_ECHO:
            case EFX10.AL_EFFECT_FLANGER:
            case EFX10.AL_EFFECT_FREQUENCY_SHIFTER:
            case EFX10.AL_EFFECT_VOCAL_MORPHER:
            case EFX10.AL_EFFECT_PITCH_SHIFTER:
            case EFX10.AL_EFFECT_RING_MODULATOR:
            case EFX10.AL_EFFECT_AUTOWAH:
            case EFX10.AL_EFFECT_COMPRESSOR:
            case EFX10.AL_EFFECT_EQUALIZER:
                break;
            default:
                throw new IllegalArgumentException("Unknown or invalid effect type: " + effectType);
        }
        return testSupport(EFFECT, effectType);
    }

    /**
     * Whether the driver implements this filter type, e.g. {@code AL_FILTER_LOWPASS}.
     *
     * @throws IllegalArgumentException if {@code filterType} is not an EFX 1.0 filter
     * @throws OpenALException          if the test ran out of memory, or OpenAL is not created
     */
    public static boolean isFilterSupported(int filterType) {
        switch (filterType) {
            case EFX10.AL_FILTER_NULL:
            case EFX10.AL_FILTER_LOWPASS:
            case EFX10.AL_FILTER_HIGHPASS:
            case EFX10.AL_FILTER_BANDPASS:
                break;
            default:
                throw new IllegalArgumentException("Unknown or invalid filter type: " + filterType);
        }
        return testSupport(FILTER, filterType);
    }

    /** Creates a temporary object and tests whether OpenAL accepts the requested type. */
    private static boolean testSupport(int objectType, int typeValue) {
        if (!isEfxSupported()) {
            return false;
        }
        AL10.alGetError();
        int object = objectType == EFFECT ? EFX10.alGenEffects() : EFX10.alGenFilters();
        int genError = AL10.alGetError();
        if (genError != AL10.AL_NO_ERROR) {
            if (genError == AL10.AL_OUT_OF_MEMORY) {
                // Allocation failure does not say whether the type is supported
                throw new OpenALException(AL10.alGetString(genError));
            }
            return false;
        }
        AL10.alGetError();
        if (objectType == EFFECT) {
            EFX10.alEffecti(object, EFX10.AL_EFFECT_TYPE, typeValue);
        } else {
            EFX10.alFilteri(object, EFX10.AL_FILTER_TYPE, typeValue);
        }
        boolean supported = AL10.alGetError() == AL10.AL_NO_ERROR;
        if (objectType == EFFECT) {
            EFX10.alDeleteEffects(object);
        } else {
            EFX10.alDeleteFilters(object);
        }
        AL10.alGetError();
        return supported;
    }
}
