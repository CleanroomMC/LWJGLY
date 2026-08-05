package org.lwjgl.openal;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/** LWJGL 2 ALC_EXT_EFX names forwarded to LWJGL 3 EXTEfx. */
public final class EFX10 implements EFX10Constants {

    public static void alGenAuxiliaryEffectSlots(IntBuffer effectSlots) {
        EXTEfx.alGenAuxiliaryEffectSlots(effectSlots);
    }

    public static int alGenAuxiliaryEffectSlots() {
        return EXTEfx.alGenAuxiliaryEffectSlots();
    }

    public static void alDeleteAuxiliaryEffectSlots(IntBuffer effectSlots) {
        EXTEfx.alDeleteAuxiliaryEffectSlots(effectSlots);
    }

    public static void alDeleteAuxiliaryEffectSlots(int effectSlot) {
        EXTEfx.alDeleteAuxiliaryEffectSlots(effectSlot);
    }

    public static boolean alIsAuxiliaryEffectSlot(int effectSlot) {
        return EXTEfx.alIsAuxiliaryEffectSlot(effectSlot);
    }

    public static void alAuxiliaryEffectSloti(int effectSlot, int param, int value) {
        EXTEfx.alAuxiliaryEffectSloti(effectSlot, param, value);
    }

    public static void alAuxiliaryEffectSlot(int effectSlot, int param, IntBuffer values) {
        EXTEfx.alAuxiliaryEffectSlotiv(effectSlot, param, values);
    }

    public static void alAuxiliaryEffectSlotf(int effectSlot, int param, float value) {
        EXTEfx.alAuxiliaryEffectSlotf(effectSlot, param, value);
    }

    public static void alAuxiliaryEffectSlot(int effectSlot, int param, FloatBuffer values) {
        EXTEfx.alAuxiliaryEffectSlotfv(effectSlot, param, values);
    }

    public static int alGetAuxiliaryEffectSloti(int effectSlot, int param) {
        return EXTEfx.alGetAuxiliaryEffectSloti(effectSlot, param);
    }

    public static void alGetAuxiliaryEffectSlot(int effectSlot, int param, IntBuffer values) {
        EXTEfx.alGetAuxiliaryEffectSlotiv(effectSlot, param, values);
    }

    public static float alGetAuxiliaryEffectSlotf(int effectSlot, int param) {
        return EXTEfx.alGetAuxiliaryEffectSlotf(effectSlot, param);
    }

    public static void alGetAuxiliaryEffectSlot(int effectSlot, int param, FloatBuffer values) {
        EXTEfx.alGetAuxiliaryEffectSlotfv(effectSlot, param, values);
    }

    public static void alGenEffects(IntBuffer effects) {
        EXTEfx.alGenEffects(effects);
    }

    public static int alGenEffects() {
        return EXTEfx.alGenEffects();
    }

    public static void alDeleteEffects(IntBuffer effects) {
        EXTEfx.alDeleteEffects(effects);
    }

    public static void alDeleteEffects(int effect) {
        EXTEfx.alDeleteEffects(effect);
    }

    public static boolean alIsEffect(int effect) {
        return EXTEfx.alIsEffect(effect);
    }

    public static void alEffecti(int effect, int param, int value) {
        EXTEfx.alEffecti(effect, param, value);
    }

    public static void alEffect(int effect, int param, IntBuffer values) {
        EXTEfx.alEffectiv(effect, param, values);
    }

    public static void alEffectf(int effect, int param, float value) {
        EXTEfx.alEffectf(effect, param, value);
    }

    public static void alEffect(int effect, int param, FloatBuffer values) {
        EXTEfx.alEffectfv(effect, param, values);
    }

    public static int alGetEffecti(int effect, int param) {
        return EXTEfx.alGetEffecti(effect, param);
    }

    public static void alGetEffect(int effect, int param, IntBuffer values) {
        EXTEfx.alGetEffectiv(effect, param, values);
    }

    public static float alGetEffectf(int effect, int param) {
        return EXTEfx.alGetEffectf(effect, param);
    }

    public static void alGetEffect(int effect, int param, FloatBuffer values) {
        EXTEfx.alGetEffectfv(effect, param, values);
    }

    public static void alGenFilters(IntBuffer filters) {
        EXTEfx.alGenFilters(filters);
    }

    public static int alGenFilters() {
        return EXTEfx.alGenFilters();
    }

    public static void alDeleteFilters(IntBuffer filters) {
        EXTEfx.alDeleteFilters(filters);
    }

    public static void alDeleteFilters(int filter) {
        EXTEfx.alDeleteFilters(filter);
    }

    public static boolean alIsFilter(int filter) {
        return EXTEfx.alIsFilter(filter);
    }

    public static void alFilteri(int filter, int param, int value) {
        EXTEfx.alFilteri(filter, param, value);
    }

    public static void alFilter(int filter, int param, IntBuffer values) {
        EXTEfx.alFilteriv(filter, param, values);
    }

    public static void alFilterf(int filter, int param, float value) {
        EXTEfx.alFilterf(filter, param, value);
    }

    public static void alFilter(int filter, int param, FloatBuffer values) {
        EXTEfx.alFilterfv(filter, param, values);
    }

    public static int alGetFilteri(int filter, int param) {
        return EXTEfx.alGetFilteri(filter, param);
    }

    public static void alGetFilter(int filter, int param, IntBuffer values) {
        EXTEfx.alGetFilteriv(filter, param, values);
    }

    public static float alGetFilterf(int filter, int param) {
        return EXTEfx.alGetFilterf(filter, param);
    }

    public static void alGetFilter(int filter, int param, FloatBuffer values) {
        EXTEfx.alGetFilterfv(filter, param, values);
    }

    private EFX10() { }

}
