package com.cleanroommc.lwjgly.adapter;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

public final class BufferUtils {

    public static int getElementSizeExponent(Buffer buffer) {
        if (buffer instanceof ByteBuffer) {
            return 0;
        } else if (buffer instanceof ShortBuffer || buffer instanceof CharBuffer) {
            return 1;
        } else if (buffer instanceof FloatBuffer || buffer instanceof IntBuffer) {
            return 2;
        } else if (buffer instanceof LongBuffer || buffer instanceof DoubleBuffer) {
            return 3;
        }
        throw new IllegalStateException("Unsupported buffer type: " + buffer);
    }

    public static int getOffset(Buffer buffer) {
        return buffer.position() << getElementSizeExponent(buffer);
    }

    private BufferUtils() { }

}
