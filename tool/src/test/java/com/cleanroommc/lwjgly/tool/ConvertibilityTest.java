package com.cleanroommc.lwjgly.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Over-matching here is how a shim ends up quietly passing the wrong buffer.
 * The negative (refusal) cases matter more.
 */
class ConvertibilityTest {

    @Test
    void acceptsABufferViewOfTheSameBytes() {
        assertTrue(Convertibility.signatureConvertible("(ILjava/nio/ByteBuffer;)V", "(ILjava/nio/FloatBuffer;)V"));
    }

    @Test
    void acceptsStringWhereLwjgl3WidenedToCharSequence() {
        assertTrue(Convertibility.signatureConvertible("(Ljava/lang/String;)Z", "(Ljava/lang/CharSequence;)Z"));
    }

    @Test
    void refusesAnAddedParameter() {
        assertFalse(Convertibility.signatureConvertible("(IILjava/nio/ByteBuffer;)V", "(IIILjava/nio/ByteBuffer;)V"));
    }

    @Test
    void refusesAChangedReturnType() {
        assertFalse(Convertibility.signatureConvertible("(Ljava/lang/String;)Lorg/lwjgl/openal/ALCdevice;", "(Ljava/lang/CharSequence;)J"));
    }

    @Test
    void refusesBytesToText() {
        // Decoding encoded, NUL-terminated bytes means choosing a charset and a terminator
        assertFalse(Convertibility.signatureConvertible("(Ljava/nio/ByteBuffer;)Z", "(Ljava/lang/CharSequence;)Z"));
    }

    @Test
    void refusesTheReverseBufferDirection() {
        // Narrowing a FloatBuffer to a ByteBuffer is not the same operation and is not implied
        assertFalse(Convertibility.signatureConvertible("(ILjava/nio/FloatBuffer;)V", "(ILjava/nio/ByteBuffer;)V"));
    }
}
