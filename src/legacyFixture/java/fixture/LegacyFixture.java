package fixture;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.MemoryUtil;
import org.lwjgl.Sys;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCdevice;
import org.lwjgl.openal.EFX10;
import org.lwjgl.openal.EFXUtil;
import org.lwjgl.openal.Util;
import org.lwjgl.opengl.AMDDebugOutput;
import org.lwjgl.opengl.AMDDebugOutputCallback;
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.ARBDebugOutputCallback;
import org.lwjgl.opengl.ARBImaging;
import org.lwjgl.opengl.ARBRobustness;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBSync;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL44;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.GLSync;
import org.lwjgl.opengl.KHRDebug;
import org.lwjgl.opengl.KHRDebugCallback;
import org.lwjgl.util.Timer;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Class with typical LWJGL 2 usages.
 *
 * <p>Its job is to be a source of javac-emitted references to the LWJGL 2 API.
 * {@code LegacyLinkTest} reads them straight out of this class file and resolves every one against the merged runtime.
 */
public final class LegacyFixture {

    public static void display() throws Exception {
        Display.setTitle("LegacyFixture");
        Display.setDisplayMode(new DisplayMode(854, 480));
        Display.create();
        Display.setVSyncEnabled(true);
        Display.setResizable(true);
        Display.update();
        Display.sync(60);
        Display.swapBuffers();
        Display.processMessages();
        boolean unused = Display.isCloseRequested() | Display.isActive() | Display.isVisible() | Display.wasResized() | Display.isCreated() | Display.isFullscreen();
        int size = Display.getWidth() + Display.getHeight();
        float scale = Display.getPixelScaleFactor();
        DisplayMode[] modes = Display.getAvailableDisplayModes();
        DisplayMode desktop = Display.getDesktopDisplayMode();
        Display.setIcon(new ByteBuffer[]{BufferUtils.createByteBuffer(4)});
        Display.destroy();
    }

    public static void input() throws Exception {
        Keyboard.create();
        Keyboard.enableRepeatEvents(true);
        while (Keyboard.next()) {
            int key = Keyboard.getEventKey();
            boolean pressed = Keyboard.getEventKeyState();
            char character = Keyboard.getEventCharacter();
            long when = Keyboard.getEventNanoseconds();
            boolean repeat = Keyboard.isRepeatEvent();
        }
        boolean forward = Keyboard.isKeyDown(Keyboard.KEY_W);
        String name = Keyboard.getKeyName(Keyboard.KEY_ESCAPE);
        int index = Keyboard.getKeyIndex("SPACE");
        int queued = Keyboard.getNumKeyboardEvents();
        Mouse.create();
        while (Mouse.next()) {
            int button = Mouse.getEventButton();
            boolean state = Mouse.getEventButtonState();
            int position = Mouse.getEventX() + Mouse.getEventY();
            int delta = Mouse.getEventDX() + Mouse.getEventDY();
            int wheel = Mouse.getEventDWheel();
        }
        Mouse.setGrabbed(true);
        Mouse.setCursorPosition(1, 1);
        int movement = Mouse.getDX() + Mouse.getDY() + Mouse.getDWheel() + Mouse.getX() + Mouse.getY();
        boolean down = Mouse.isButtonDown(0) | Mouse.isGrabbed() | Mouse.isInsideWindow() | Mouse.hasWheel();
    }

    public static void immediateModeGl() {
        FloatBuffer floats = BufferUtils.createFloatBuffer(16);
        IntBuffer ints = BufferUtils.createIntBuffer(16);
        GL11.glEnable(GL11.GL_FOG);
        GL11.glFog(GL11.GL_FOG_COLOR, floats);
        GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, floats);
        GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, floats);
        GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE, floats);
        GL11.glTexEnv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_COLOR, floats);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, floats);
        GL11.glGetInteger(GL11.GL_VIEWPORT, ints);
        GL11.glGetTexParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, ints);
        GL11.glLoadMatrix(floats);
        GL11.glMultMatrix(floats);
        GL11.glGenTextures(ints);
        GL11.glDeleteTextures(ints);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3f(0f, 0f, 0f);
        GL11.glEnd();
    }

    public static void shaders() {
        IntBuffer ints = BufferUtils.createIntBuffer(16);
        FloatBuffer floats = BufferUtils.createFloatBuffer(16);
        int program = GL20.glCreateProgram();
        GL20.glGetProgram(program, GL20.GL_LINK_STATUS, ints);
        GL20.glUniformMatrix4(0, false, floats);
        GL20.glUniform4(0, floats);
        int arbProgram = ARBShaderObjects.glCreateProgramObjectARB();
        ARBShaderObjects.glGetObjectParameterARB(arbProgram, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB, floats);
        GL15.glGenBuffers(ints);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, floats, GL15.GL_STATIC_DRAW);
        GL15.glGetBufferParameter(GL15.GL_ARRAY_BUFFER, GL15.GL_BUFFER_SIZE, ints);
        EXTFramebufferObject.glGenFramebuffersEXT(ints);
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
    }

    public static boolean capabilities() {
        ContextCapabilities caps = GLContext.getCapabilities();
        return caps.OpenGL21 && caps.GL_ARB_multitexture && caps.GL_EXT_framebuffer_object && caps.OpenGL30 && caps.GL_ARB_vertex_buffer_object;
    }

    public static void utilities() {
        Matrix4f matrix = new Matrix4f();
        matrix.setIdentity();
        Matrix4f.mul(matrix, matrix, matrix);
        Vector3f vector = new Vector3f(1f, 2f, 3f);
        vector.normalise();
        float length = vector.length();
        GLU.gluPerspective(70f, 1.5f, 0.05f, 1000f);
        GLU.gluLookAt(0f, 0f, 0f, 1f, 1f, 1f, 0f, 1f, 0f);
        String error = GLU.gluErrorString(0);
        long time = Sys.getTime();
        long resolution = Sys.getTimerResolution();
        String version = Sys.getVersion();
    }

    public static void syncObjects() {
        GLSync sync = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        boolean live = GL32.glIsSync(sync) && sync.isValid();
        int waited = GL32.glClientWaitSync(sync, GL32.GL_SYNC_FLUSH_COMMANDS_BIT, 0L);
        GL32.glWaitSync(sync, 0, GL32.GL_TIMEOUT_IGNORED);
        int status = GL32.glGetSynci(sync, GL32.GL_SYNC_STATUS);
        int deprecated = GL32.glGetSync(sync, GL32.GL_SYNC_STATUS);
        GL32.glGetSync(sync, GL32.GL_SYNC_STATUS, null, BufferUtils.createIntBuffer(1));
        GL32.glDeleteSync(sync);
        GLSync arb = ARBSync.glFenceSync(ARBSync.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        ARBSync.glWaitSync(arb, 0, ARBSync.GL_TIMEOUT_IGNORED);
        int arbStatus = ARBSync.glGetSynci(arb, ARBSync.GL_SYNC_STATUS);
        ARBSync.glDeleteSync(arb);
    }

    public static void pointerLabels() {
        GLSync sync = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        KHRDebug.glObjectPtrLabel(sync, "fence");
        String label = KHRDebug.glGetObjectPtrLabel(sync, 64);
        GL43.glObjectPtrLabel(sync, "fence");
        GL43.glGetObjectPtrLabel(sync, BufferUtils.createIntBuffer(1), BufferUtils.createByteBuffer(64));
        GL32.glDeleteSync(sync);
    }

    public static void typedBuffers() {
        GL11.glVertexPointer(3, 0, BufferUtils.createDoubleBuffer(9));
        GL11.glColorPointer(4, 0, BufferUtils.createFloatBuffer(16));
        GL11.glNormalPointer(0, BufferUtils.createIntBuffer(9));
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 2, 2, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, BufferUtils.createIntBuffer(4));
        GL13.glCompressedTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 2, 2, 0, BufferUtils.createByteBuffer(16));
        ARBImaging.glColorTable(ARBImaging.GL_COLOR_TABLE, GL11.GL_RGBA, 256, GL11.GL_RGBA, GL11.GL_FLOAT, BufferUtils.createDoubleBuffer(1024));
        ARBRobustness.glReadnPixelsARB(0, 0, 1, 1, GL11.GL_RGBA, GL11.GL_FLOAT, BufferUtils.createFloatBuffer(4));
        GL44.glBufferStorage(GL15.GL_ARRAY_BUFFER, BufferUtils.createLongBuffer(4), 0);
    }

    public static void unsignedPointerOverloads() {
        GL11.glColorPointer(4, true, 0, BufferUtils.createByteBuffer(16));
        GL14.glSecondaryColorPointer(3, false, 0, BufferUtils.createByteBuffer(12));
        GL20.glVertexAttribPointer(0, 4, true, false, 0, BufferUtils.createByteBuffer(16));
        GL20.glVertexAttribPointer(1, 4, false, true, 0, BufferUtils.createShortBuffer(8));
        GL20.glVertexAttribPointer(2, 4, true, false, 0, BufferUtils.createIntBuffer(4));
        ARBVertexShader.glVertexAttribPointerARB(3, 4, false, false, 0, BufferUtils.createIntBuffer(4));
    }

    public static void queriesReturningObjects() {
        ByteBuffer pointer = GL11.glGetPointer(GL11.GL_VERTEX_ARRAY_POINTER, 16);
        ByteBuffer mapped = GL15.glGetBufferPointer(GL15.GL_ARRAY_BUFFER, GL15.GL_BUFFER_MAP_POINTER);
        ByteBuffer attrib = GL20.glGetVertexAttribPointer(0, GL20.GL_VERTEX_ATTRIB_ARRAY_POINTER, 16);
        IntBuffer sizeType = BufferUtils.createIntBuffer(2);
        String uniform = GL20.glGetActiveUniform(0, 0, 64, sizeType);
        String uniformOnly = GL20.glGetActiveUniform(0, 0, 64);
        String attribName = GL20.glGetActiveAttrib(0, 0, 64, sizeType);
        String attribOnly = GL20.glGetActiveAttrib(0, 0, 64);
        String arbAttrib = ARBVertexShader.glGetActiveAttribARB(0, 0, 64, sizeType);
    }

    public static void pointerBufferChaining() {
        PointerBuffer buffer = BufferUtils.createPointerBuffer(4);
        PointerBuffer other = BufferUtils.createPointerBuffer(2);
        buffer.position(1).limit(3).mark();
        buffer.rewind().clear().flip();
        PointerBuffer slice = buffer.slice();
        PointerBuffer copy = buffer.duplicate();
        buffer.clear().put(other).compact().reset();
    }

    public static void debugCallbacks() {
        GL43.glDebugMessageCallback(new KHRDebugCallback());
        KHRDebug.glDebugMessageCallback(new KHRDebugCallback((source, type, id, severity, message) -> { }));
        ARBDebugOutput.glDebugMessageCallbackARB(new ARBDebugOutputCallback());
        AMDDebugOutput.glDebugMessageCallbackAMD(new AMDDebugOutputCallback());
    }

    public static void sound() throws Exception {
        AL.create();
        ALCdevice device = AL.getDevice();
        String devices = ALC10.alcGetString(device, ALC10.ALC_DEVICE_SPECIFIER);
        int error = ALC10.alcGetError(device);
        IntBuffer sources = BufferUtils.createIntBuffer(1);
        AL10.alGenSources(sources);
        AL10.alSourcePlay(sources.get(0));
        AL10.alSourceStop(sources);
        AL10.alListener(AL10.AL_POSITION, BufferUtils.createFloatBuffer(3));
        AL10.alDeleteSources(sources);
        AL.destroy();
    }

    public static void soundEffects() {
        boolean efx = EFXUtil.isEfxSupported() && EFXUtil.isEffectSupported(EFX10.AL_EFFECT_REVERB) && EFXUtil.isFilterSupported(EFX10.AL_FILTER_LOWPASS);
        IntBuffer ints = BufferUtils.createIntBuffer(1);
        FloatBuffer floats = BufferUtils.createFloatBuffer(1);
        int slot = EFX10.alGenAuxiliaryEffectSlots();
        EFX10.alGenAuxiliaryEffectSlots(ints);
        EFX10.alAuxiliaryEffectSloti(slot, EFX10.AL_EFFECTSLOT_EFFECT, EFX10.AL_EFFECTSLOT_NULL);
        EFX10.alAuxiliaryEffectSlotf(slot, EFX10.AL_EFFECTSLOT_GAIN, 1f);
        EFX10.alAuxiliaryEffectSlot(slot, EFX10.AL_EFFECTSLOT_EFFECT, ints);
        EFX10.alAuxiliaryEffectSlot(slot, EFX10.AL_EFFECTSLOT_GAIN, floats);
        EFX10.alGetAuxiliaryEffectSlot(slot, EFX10.AL_EFFECTSLOT_EFFECT, ints);
        EFX10.alGetAuxiliaryEffectSlot(slot, EFX10.AL_EFFECTSLOT_GAIN, floats);
        int slotEffect = EFX10.alGetAuxiliaryEffectSloti(slot, EFX10.AL_EFFECTSLOT_EFFECT);
        float slotGain = EFX10.alGetAuxiliaryEffectSlotf(slot, EFX10.AL_EFFECTSLOT_GAIN);
        boolean isSlot = EFX10.alIsAuxiliaryEffectSlot(slot);
        int effect = EFX10.alGenEffects();
        EFX10.alGenEffects(ints);
        EFX10.alEffecti(effect, EFX10.AL_EFFECT_TYPE, EFX10.AL_EFFECT_REVERB);
        EFX10.alEffectf(effect, EFX10.AL_REVERB_DECAY_TIME, 1.49f);
        EFX10.alEffect(effect, EFX10.AL_EFFECT_TYPE, ints);
        EFX10.alEffect(effect, EFX10.AL_REVERB_DECAY_TIME, floats);
        EFX10.alGetEffect(effect, EFX10.AL_EFFECT_TYPE, ints);
        EFX10.alGetEffect(effect, EFX10.AL_REVERB_DECAY_TIME, floats);
        int effectType = EFX10.alGetEffecti(effect, EFX10.AL_EFFECT_TYPE);
        float decay = EFX10.alGetEffectf(effect, EFX10.AL_REVERB_DECAY_TIME);
        boolean isEffect = EFX10.alIsEffect(effect);
        int filter = EFX10.alGenFilters();
        EFX10.alGenFilters(ints);
        EFX10.alFilteri(filter, EFX10.AL_FILTER_TYPE, EFX10.AL_FILTER_LOWPASS);
        EFX10.alFilterf(filter, EFX10.AL_LOWPASS_GAIN, 1f);
        EFX10.alFilter(filter, EFX10.AL_FILTER_TYPE, ints);
        EFX10.alFilter(filter, EFX10.AL_LOWPASS_GAIN, floats);
        EFX10.alGetFilter(filter, EFX10.AL_FILTER_TYPE, ints);
        EFX10.alGetFilter(filter, EFX10.AL_LOWPASS_GAIN, floats);
        int filterType = EFX10.alGetFilteri(filter, EFX10.AL_FILTER_TYPE);
        float gain = EFX10.alGetFilterf(filter, EFX10.AL_LOWPASS_GAIN);
        boolean isFilter = EFX10.alIsFilter(filter);
        EFX10.alDeleteEffects(effect);
        EFX10.alDeleteEffects(ints);
        EFX10.alDeleteFilters(filter);
        EFX10.alDeleteFilters(ints);
        EFX10.alDeleteAuxiliaryEffectSlots(slot);
        EFX10.alDeleteAuxiliaryEffectSlots(ints);
    }

    public static void bufferAddresses() {
        ByteBuffer bytes = BufferUtils.createByteBuffer(16);
        IntBuffer ints = BufferUtils.createIntBuffer(4);
        PointerBuffer pointers = BufferUtils.createPointerBuffer(2);
        long raw = MemoryUtil.getAddress0(bytes) + MemoryUtil.getAddress0(pointers)
                + MemoryUtil.getAddress0Safe(bytes) + MemoryUtil.getAddress0Safe(pointers);
        long positioned = MemoryUtil.getAddress(bytes) + MemoryUtil.getAddress(bytes, 4)
                + MemoryUtil.getAddress(ints) + MemoryUtil.getAddress(ints, 1)
                + MemoryUtil.getAddress(pointers) + MemoryUtil.getAddress(pointers, 1);
        long safe = MemoryUtil.getAddressSafe(bytes) + MemoryUtil.getAddressSafe(ints, 1)
                + MemoryUtil.getAddressSafe(pointers);
        ByteBuffer ascii = MemoryUtil.encodeASCII("fixture");
        ByteBuffer utf8 = MemoryUtil.encodeUTF8("fixture");
        ByteBuffer utf16 = MemoryUtil.encodeUTF16("fixture");
        String back = MemoryUtil.decodeASCII(ascii) + MemoryUtil.decodeUTF8(utf8) + MemoryUtil.decodeUTF16(utf16);
    }

    public static void timingAndModes() throws Exception {
        Timer.tick();
        Timer timer = new Timer();
        timer.set(0.5f);
        timer.pause();
        boolean paused = timer.isPaused();
        timer.resume();
        timer.reset();
        float seconds = timer.getTime();
        DisplayMode[] modes = org.lwjgl.util.Display.getAvailableDisplayModes(640, 480, -1, -1, 16, -1, -1, -1);
        DisplayMode chosen = org.lwjgl.util.Display.setDisplayMode(modes, new String[] {"width=1024", "-height", "freq"});
    }

    public static void cursors() throws Exception {
        int size = Cursor.getMinCursorSize();
        int max = Cursor.getMaxCursorSize();
        int caps = Cursor.getCapabilities();
        boolean alpha = (caps & Cursor.CURSOR_8_BIT_ALPHA) != 0 && (caps & Cursor.CURSOR_ONE_BIT_TRANSPARENCY) != 0 && (caps & Cursor.CURSOR_ANIMATION) != 0;
        IntBuffer images = BufferUtils.createIntBuffer(16 * 16 * 2);
        IntBuffer delays = BufferUtils.createIntBuffer(2);
        Cursor cursor = new Cursor(16, 16, 0, 15, 2, images, delays);
        Cursor previous = Mouse.setNativeCursor(cursor);
        Cursor current = Mouse.getNativeCursor();
        Mouse.updateCursor();
        Mouse.setNativeCursor(null);
        cursor.destroy();
    }

    public static void soundErrorChecks() throws Exception {
        Util.checkALError();
        ALCdevice device = AL.getDevice();
        Util.checkALCError(device);
        Util.checkALCValidDevice(device);
        Util.checkALCValidContext(ALC10.alcGetCurrentContext());
    }

    private LegacyFixture() { }

}
