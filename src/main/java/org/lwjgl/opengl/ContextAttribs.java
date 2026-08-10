package org.lwjgl.opengl;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLUtil;

import java.nio.IntBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

/** LWJGL 2's immutable OpenGL context creation attributes. */
public final class ContextAttribs {

    public static final int CONTEXT_MAJOR_VERSION_ARB = 0x2091;
    public static final int CONTEXT_MINOR_VERSION_ARB = 0x2092;

    public static final int CONTEXT_PROFILE_MASK_ARB = 0x9126;
    public static final int CONTEXT_CORE_PROFILE_BIT_ARB = 0x00000001;
    public static final int CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB = 0x00000002;
    public static final int CONTEXT_ES2_PROFILE_BIT_EXT = 0x00000004;

    public static final int CONTEXT_FLAGS_ARB = 0x2094;
    public static final int CONTEXT_DEBUG_BIT_ARB = 0x0001;
    public static final int CONTEXT_FORWARD_COMPATIBLE_BIT_ARB = 0x0002;
    public static final int CONTEXT_ROBUST_ACCESS_BIT_ARB = 0x00000004;
    public static final int CONTEXT_RESET_ISOLATION_BIT_ARB = 0x00000008;

    public static final int CONTEXT_RESET_NOTIFICATION_STRATEGY_ARB = 0x8256;
    public static final int NO_RESET_NOTIFICATION_ARB = 0x8261;
    public static final int LOSE_CONTEXT_ON_RESET_ARB = 0x8252;

    // LWJGL 2's misspelling
    public static final int CONTEXT_RELEASE_BEHABIOR_ARB = 0x2097;
    public static final int CONTEXT_RELEASE_BEHAVIOR_NONE_ARB = 0x0000;
    public static final int CONTEXT_RELEASE_BEHAVIOR_FLUSH_ARB = 0x2098;

    public static final int CONTEXT_LAYER_PLANE_ARB = 0x2093;

    private int majorVersion;
    private int minorVersion;
    private int profileMask;
    private int contextFlags;
    private int contextResetNotificationStrategy = NO_RESET_NOTIFICATION_ARB;
    private int contextReleaseBehavior = CONTEXT_RELEASE_BEHAVIOR_FLUSH_ARB;
    private int layerPlane;

    public ContextAttribs() {
        this(1, 0);
    }

    public ContextAttribs(int majorVersion, int minorVersion) {
        this(majorVersion, minorVersion, 0, 0);
    }

    public ContextAttribs(int majorVersion, int minorVersion, int profileMask) {
        // Preserve LWJGL 2's constructor behavior (including its historical parameter ordering).
        this(majorVersion, minorVersion, 0, profileMask);
    }

    public ContextAttribs(int majorVersion, int minorVersion, int profileMask, int contextFlags) {
        if (majorVersion < 0 || majorVersion > 4 || minorVersion < 0 || (majorVersion == 4 && minorVersion > 5)
                || (majorVersion == 3 && minorVersion > 3) || (majorVersion == 2 && minorVersion > 1)
                || (majorVersion == 1 && minorVersion > 5)) {
            throw new IllegalArgumentException("Invalid OpenGL version specified: " + majorVersion + '.' + minorVersion);
        }
        if (LWJGLUtil.CHECKS) {
            if (Integer.bitCount(profileMask) > 1 || profileMask > CONTEXT_ES2_PROFILE_BIT_EXT) {
                throw new IllegalArgumentException("Invalid profile mask specified: " + Integer.toBinaryString(profileMask));
            }
            if (contextFlags > 0xF) {
                throw new IllegalArgumentException("Invalid context flags specified: " + Integer.toBinaryString(profileMask));
            }
        }
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.profileMask = profileMask;
        this.contextFlags = contextFlags;
    }

    private ContextAttribs(ContextAttribs other) {
        majorVersion = other.majorVersion;
        minorVersion = other.minorVersion;
        profileMask = other.profileMask;
        contextFlags = other.contextFlags;
        contextResetNotificationStrategy = other.contextResetNotificationStrategy;
        contextReleaseBehavior = other.contextReleaseBehavior;
        layerPlane = other.layerPlane;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public int getProfileMask() {
        return profileMask;
    }

    public boolean isProfileCore() {
        return hasMask(CONTEXT_CORE_PROFILE_BIT_ARB);
    }

    public boolean isProfileCompatibility() {
        return hasMask(CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB);
    }

    public boolean isProfileES() {
        return hasMask(CONTEXT_ES2_PROFILE_BIT_EXT);
    }

    public int getContextFlags() {
        return contextFlags;
    }

    public boolean isDebug() {
        return hasFlag(CONTEXT_DEBUG_BIT_ARB);
    }

    public boolean isForwardCompatible() {
        return hasFlag(CONTEXT_FORWARD_COMPATIBLE_BIT_ARB);
    }

    public boolean isRobustAccess() {
        return hasFlag(CONTEXT_ROBUST_ACCESS_BIT_ARB);
    }

    public boolean isContextResetIsolation() {
        return hasFlag(CONTEXT_RESET_ISOLATION_BIT_ARB);
    }

    public int getContextResetNotificationStrategy() {
        return contextResetNotificationStrategy;
    }

    @Deprecated
    public boolean isLoseContextOnReset() {
        return contextResetNotificationStrategy == LOSE_CONTEXT_ON_RESET_ARB;
    }

    public int getContextReleaseBehavior() {
        return contextReleaseBehavior;
    }

    public int getLayerPlane() {
        return layerPlane;
    }

    public ContextAttribs withProfileCore(boolean enabled) {
        requireProfiles();
        return toggleMask(CONTEXT_CORE_PROFILE_BIT_ARB, enabled);
    }

    public ContextAttribs withProfileCompatibility(boolean enabled) {
        requireProfiles();
        return toggleMask(CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB, enabled);
    }

    public ContextAttribs withProfileES(boolean enabled) {
        if (majorVersion != 2 || minorVersion != 0) {
            throw new IllegalArgumentException("The OpenGL ES profile is only supported on OpenGL version 2.0.");
        }
        return toggleMask(CONTEXT_ES2_PROFILE_BIT_EXT, enabled);
    }

    public ContextAttribs withDebug(boolean enabled) {
        return toggleFlag(CONTEXT_DEBUG_BIT_ARB, enabled);
    }

    public ContextAttribs withForwardCompatible(boolean enabled) {
        return toggleFlag(CONTEXT_FORWARD_COMPATIBLE_BIT_ARB, enabled);
    }

    public ContextAttribs withRobustAccess(boolean enabled) {
        return toggleFlag(CONTEXT_ROBUST_ACCESS_BIT_ARB, enabled);
    }

    public ContextAttribs withContextResetIsolation(boolean enabled) {
        return toggleFlag(CONTEXT_RESET_ISOLATION_BIT_ARB, enabled);
    }

    public ContextAttribs withResetNotificationStrategy(int strategy) {
        if (strategy == contextResetNotificationStrategy) {
            return this;
        }
        if (LWJGLUtil.CHECKS && strategy != NO_RESET_NOTIFICATION_ARB && strategy != LOSE_CONTEXT_ON_RESET_ARB) {
            throw new IllegalArgumentException("Invalid context reset notification strategy specified: 0x" + LWJGLUtil.toHexString(strategy));
        }
        ContextAttribs copy = new ContextAttribs(this);
        copy.contextResetNotificationStrategy = strategy;
        return copy;
    }

    @Deprecated
    public ContextAttribs withLoseContextOnReset(boolean loseContextOnReset) {
        return withResetNotificationStrategy(loseContextOnReset ? LOSE_CONTEXT_ON_RESET_ARB : NO_RESET_NOTIFICATION_ARB);
    }

    public ContextAttribs withContextReleaseBehavior(int behavior) {
        if (behavior == contextReleaseBehavior) {
            return this;
        }
        if (LWJGLUtil.CHECKS && behavior != CONTEXT_RELEASE_BEHAVIOR_FLUSH_ARB && behavior != CONTEXT_RELEASE_BEHAVIOR_NONE_ARB) {
            throw new IllegalArgumentException("Invalid context release behavior specified: 0x" + LWJGLUtil.toHexString(behavior));
        }
        ContextAttribs copy = new ContextAttribs(this);
        copy.contextReleaseBehavior = behavior;
        return copy;
    }

    public ContextAttribs withLayer(int layerPlane) {
        if (LWJGLUtil.getPlatform() != LWJGLUtil.PLATFORM_WINDOWS) {
            throw new IllegalArgumentException("The CONTEXT_LAYER_PLANE_ARB attribute is supported only on the Windows platform.");
        }
        if (layerPlane == this.layerPlane) {
            return this;
        }
        if (layerPlane < 0) {
            throw new IllegalArgumentException("Invalid layer plane specified: " + layerPlane);
        }
        ContextAttribs copy = new ContextAttribs(this);
        copy.layerPlane = layerPlane;
        return copy;
    }

    IntBuffer getAttribList() {
        if (LWJGLUtil.getPlatform() == LWJGLUtil.PLATFORM_MACOSX) {
            return null;
        }
        Map<Integer, Integer> values = new LinkedHashMap<>(8);
        if (majorVersion != 1 || minorVersion != 0) {
            values.put(CONTEXT_MAJOR_VERSION_ARB, majorVersion);
            values.put(CONTEXT_MINOR_VERSION_ARB, minorVersion);
        }
        if (contextFlags != 0) {
            values.put(CONTEXT_FLAGS_ARB, contextFlags);
        }
        if (profileMask != 0) {
            values.put(CONTEXT_PROFILE_MASK_ARB, profileMask);
        }
        if (contextResetNotificationStrategy != NO_RESET_NOTIFICATION_ARB) {
            values.put(CONTEXT_RESET_NOTIFICATION_STRATEGY_ARB, contextResetNotificationStrategy);
        }
        if (contextReleaseBehavior != CONTEXT_RELEASE_BEHAVIOR_FLUSH_ARB) {
            values.put(CONTEXT_RELEASE_BEHABIOR_ARB, contextReleaseBehavior);
        }
        if (layerPlane != 0) {
            values.put(CONTEXT_LAYER_PLANE_ARB, layerPlane);
        }
        if (values.isEmpty()) {
            return null;
        }
        IntBuffer attributes = BufferUtils.createIntBuffer(values.size() * 2 + 1);
        values.forEach((key, value) -> attributes.put(key).put(value));
        return attributes.put(0).flip();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(48)
                .append("ContextAttribs: Version=")
                .append(majorVersion)
                .append('.')
                .append(minorVersion);
        if (profileMask != 0) {
            result.append(", Profile=");
            if (isProfileCore()) {
                result.append("CORE");
            } else if (isProfileCompatibility()) {
                result.append("COMPATIBLITY");
            } else if (isProfileES()) {
                result.append("ES2");
            } else {
                result.append("*unknown*");
            }
        }
        appendFlag(result, CONTEXT_DEBUG_BIT_ARB, "DEBUG");
        appendFlag(result, CONTEXT_FORWARD_COMPATIBLE_BIT_ARB, "FORWARD_COMPATIBLE");
        appendFlag(result, CONTEXT_ROBUST_ACCESS_BIT_ARB, "ROBUST_ACCESS");
        appendFlag(result, CONTEXT_RESET_ISOLATION_BIT_ARB, "RESET_ISOLATION");
        if (contextResetNotificationStrategy != NO_RESET_NOTIFICATION_ARB) {
            result.append(", LOSE_CONTEXT_ON_RESET");
        }
        if (contextReleaseBehavior != CONTEXT_RELEASE_BEHAVIOR_FLUSH_ARB) {
            result.append(", RELEASE_BEHAVIOR_NONE");
        }
        if (layerPlane != 0) {
            result.append(", Layer=").append(layerPlane);
        }
        return result.toString();
    }

    private void requireProfiles() {
        if (majorVersion < 3 || (majorVersion == 3 && minorVersion < 2)) {
            throw new IllegalArgumentException("Profiles are only supported on OpenGL version 3.2 or higher.");
        }
    }

    private boolean hasMask(int mask) {
        return profileMask == mask;
    }

    private boolean hasFlag(int flag) {
        return (contextFlags & flag) != 0;
    }

    private ContextAttribs toggleMask(int mask, boolean enabled) {
        if (enabled == hasMask(mask)) {
            return this;
        }
        ContextAttribs copy = new ContextAttribs(this);
        copy.profileMask = enabled ? mask : 0;
        return copy;
    }

    private ContextAttribs toggleFlag(int flag, boolean enabled) {
        if (enabled == hasFlag(flag)) {
            return this;
        }
        ContextAttribs copy = new ContextAttribs(this);
        copy.contextFlags ^= flag;
        return copy;
    }

    private void appendFlag(StringBuilder result, int flag, String name) {
        if (hasFlag(flag)) {
            result.append(", ").append(name);
        }
    }
}
