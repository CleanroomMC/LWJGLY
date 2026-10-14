package org.lwjgl.opengl;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libffi.FFICIF;
import org.lwjgl.system.libffi.FFIType;
import org.lwjgl.system.libffi.LibFFI;

import java.nio.*;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Backs the GL extension classes the generator rebinds from LWJGL 2's bytecode.
 * Their calls into LWJGL 2's {@code APIUtil}, {@code GLChecks} and {@code StateTracker} land here,
 * along with native calls no {@code JNI} overload matches.
 */
final class LegacyGL {

    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);
    private static final Map<ContextCapabilities, References> REFERENCES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<String, Call> CALLS = new ConcurrentHashMap<>();

    static ByteBuffer getBufferByte(ContextCapabilities caps, int size) {
        Scratch scratch = SCRATCH.get();
        ByteBuffer buffer = scratch.bytes;
        if (buffer.capacity() >= size) {
            buffer.clear();
            return buffer;
        }
        int capacity = buffer.capacity() << 1;
        while (capacity < size) {
            capacity <<= 1;
        }
        return scratch.bytes = BufferUtils.createByteBuffer(capacity);
    }

    static IntBuffer getBufferInt(ContextCapabilities caps) {
        return SCRATCH.get().ints;
    }

    static LongBuffer getBufferLong(ContextCapabilities caps) {
        return SCRATCH.get().longs;
    }

    static FloatBuffer getBufferFloat(ContextCapabilities caps) {
        return SCRATCH.get().floats;
    }

    static DoubleBuffer getBufferDouble(ContextCapabilities caps) {
        return SCRATCH.get().doubles;
    }

    static long getInt(ContextCapabilities caps, int value) {
        return MemoryUtil.memAddress0(getBufferInt(caps).put(0, value));
    }

    static long getBuffer(ContextCapabilities caps, CharSequence string) {
        ByteBuffer buffer = encode(getBufferByte(caps, string.length()), string);
        buffer.flip();
        return MemoryUtil.memAddress0(buffer);
    }

    static long getBufferNT(ContextCapabilities caps, CharSequence string) {
        ByteBuffer buffer = encode(getBufferByte(caps, string.length() + 1), string);
        buffer.put((byte) 0);
        buffer.flip();
        return MemoryUtil.memAddress0(buffer);
    }

    static String getString(ContextCapabilities caps, ByteBuffer buffer) {
        int length = buffer.remaining();
        Scratch scratch = SCRATCH.get();
        if (scratch.chars.length < length) {
            scratch.chars = new char[Math.max(length, scratch.chars.length << 1)];
        }
        for (int i = 0; i < length; i++) {
            scratch.chars[i] = (char) buffer.get(buffer.position() + i);
        }
        return new String(scratch.chars, 0, length);
    }

    private static ByteBuffer encode(ByteBuffer buffer, CharSequence string) {
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            buffer.put(LWJGLUtil.DEBUG && c >= 128 ? 0x1A : (byte) c);
        }
        return buffer;
    }

    /** LWJGL 2 tracked the binding itself, through hooks LWJGL 3 does not have, so the driver is asked. */
    static void ensureArrayVBOdisabled(ContextCapabilities caps) {
        if (LWJGLUtil.CHECKS && GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING) != 0) {
            throw new OpenGLException("Cannot use Buffers when Array Buffer Object is enabled");
        }
    }

    static void ensureArrayVBOenabled(ContextCapabilities caps) {
        if (LWJGLUtil.CHECKS && GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING) == 0) {
            throw new OpenGLException("Cannot use offsets when Array Buffer Object is disabled");
        }
    }

    static int calculateImageStorage(Buffer buffer, int format, int type, int width, int height, int depth) {
        if (!LWJGLUtil.CHECKS) {
            return 0;
        }
        int elementShift = buffer instanceof ByteBuffer ? 0
                : buffer instanceof ShortBuffer || buffer instanceof CharBuffer ? 1
                : buffer instanceof IntBuffer || buffer instanceof FloatBuffer ? 2 : 3;
        return bytesPerPixel(format, type) * width * height * depth >> elementShift;
    }

    private static int bytesPerPixel(int format, int type) {
        int element = switch (type) {
            case GL11.GL_BYTE, GL11.GL_UNSIGNED_BYTE -> 1;
            case GL11.GL_SHORT, GL11.GL_UNSIGNED_SHORT -> 2;
            case GL11.GL_INT, GL11.GL_UNSIGNED_INT, GL11.GL_FLOAT -> 4;
            default -> 0;
        };
        int elements = switch (format) {
            case GL11.GL_ALPHA, GL11.GL_LUMINANCE -> 1;
            case GL11.GL_LUMINANCE_ALPHA -> 2;
            case GL11.GL_RGB, GL12.GL_BGR -> 3;
            case GL11.GL_RGBA, GL12.GL_BGRA, EXTABGR.GL_ABGR_EXT -> 4;
            default -> 0;
        };
        return element * elements;
    }

    static References getReferences(ContextCapabilities caps) {
        return REFERENCES.computeIfAbsent(caps, References::new);
    }

    /** Nothing to track, the checks above read the binding from the driver. */
    static void bindBuffer(ContextCapabilities caps, int target, int buffer) { }

    static ByteBuffer mapped(long address, long size, ByteBuffer old) {
        if (old != null && address != 0L && MemoryUtil.memAddress0(old) == address && old.capacity() == size) {
            return old;
        }
        return MemoryUtil.memByteBufferSafe(address, (int) size);
    }

    static ByteBuffer pointer(int target, int pname, long size, long function) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer address = stack.mallocPointer(1);
            JNI.invokePV(target, pname, MemoryUtil.memAddress(address), function);
            return mapped(address.get(0), size, null);
        }
    }

    /**
     * Calls {@code function} through libffi.
     *
     * @param desc the LWJGL 2 native's descriptor, its trailing {@code J} being {@code function} itself
     * @param args every other argument, as {@code Rebind} packed them into longs
     */
    static long ffi(long function, String desc, long[] args) {
        Call call = CALLS.computeIfAbsent(desc, Call::new);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Every LWJGL target is little-endian, so a narrower argument is the low bytes of its slot
            LongBuffer slots = stack.longs(args);
            PointerBuffer values = stack.mallocPointer(args.length);
            for (int i = 0; i < args.length; i++) {
                values.put(i, MemoryUtil.memAddress(slots) + (long) i * Long.BYTES);
            }
            ByteBuffer result = stack.calloc(Long.BYTES);
            LibFFI.ffi_call(call.cif, function, result, values);
            return call.returns == 'F' ? result.getInt(0) & 0xFFFFFFFFL : result.getLong(0);
        }
    }

    private static FFIType type(char descriptor) {
        return switch (descriptor) {
            case 'V' -> LibFFI.ffi_type_void;
            case 'Z' -> LibFFI.ffi_type_uint8;
            case 'B' -> LibFFI.ffi_type_sint8;
            case 'C' -> LibFFI.ffi_type_uint16;
            case 'S' -> LibFFI.ffi_type_sint16;
            case 'I' -> LibFFI.ffi_type_sint32;
            case 'J' -> LibFFI.ffi_type_sint64;
            case 'F' -> LibFFI.ffi_type_float;
            case 'D' -> LibFFI.ffi_type_double;
            default -> throw new IllegalArgumentException("No libffi type for descriptor " + descriptor);
        };
    }

    private LegacyGL() { }

    /** Buffers LWJGL 2 kept alive while GL held a client-side pointer into them. */
    static final class References {

        final Buffer[] glVertexAttribPointer_buffer;
        Buffer EXT_vertex_shader_glVariantPointerEXT_pAddr;
        Buffer EXT_vertex_weighting_glVertexWeightPointerEXT_pPointer;

        References(ContextCapabilities caps) {
            int attributes = caps.OpenGL20 || caps.GL_ARB_vertex_shader ? GL11.glGetInteger(GL20.GL_MAX_VERTEX_ATTRIBS) : 0;
            this.glVertexAttribPointer_buffer = new Buffer[attributes];
        }

    }

    private static final class Scratch {

        char[] chars = new char[256];
        ByteBuffer bytes = BufferUtils.createByteBuffer(256);
        final IntBuffer ints = BufferUtils.createIntBuffer(32);
        final LongBuffer longs = BufferUtils.createLongBuffer(32);
        final FloatBuffer floats = BufferUtils.createFloatBuffer(32);
        final DoubleBuffer doubles = BufferUtils.createDoubleBuffer(32);

    }

    /** A libffi call interface for one native descriptor, kept for the life of the process. */
    private static final class Call {

        final FFICIF cif = FFICIF.calloc();
        final char returns;

        Call(String desc) {
            String params = desc.substring(1, desc.indexOf(')') - 1);
            this.returns = desc.charAt(desc.length() - 1);
            PointerBuffer types = MemoryUtil.memAllocPointer(params.length());
            for (int i = 0; i < params.length(); i++) {
                types.put(i, type(params.charAt(i)));
            }
            if (LibFFI.ffi_prep_cif(cif, LibFFI.FFI_DEFAULT_ABI, type(returns), types) != LibFFI.FFI_OK) {
                throw new IllegalStateException("libffi could not prepare a call for " + desc);
            }
        }

    }

}
