# Mod Compatibility (Not Generated)

Mods that reach LWJGL 2 through reflection rather than a call site, so nothing in the build can see them since:

- `scanUsage` walks bytecode references
- `verifyCoverage` works from an index that holds only public and protected members

Mods can pass all three and fail at runtime.

Note: documentation below this can be AI-generated, I am not going to manually comb through every mod on CF/Modrinth and every GitHub repository...

## Vivecraft

`src/org/vivecraft/utils/InputInjector.java`. Injects synthetic keyboard and mouse events by driving LWJGL 2's platform implementation directly.

- `Display.display_impl`: fails first; our `Display` has one field, `nextFrameNanos`
- `WindowsDisplay.keyboard`, `WindowsDisplay.mouse`: `Class.forName` on `WindowsDisplay` does
  succeed, since it declares natives and ships as a placeholder, but a placeholder carries only
  public and protected members.
- `WindowsMouse.putMouseEventWithCoords`, `handleMouseScrolled`
- `LinuxMouse.handleButtonPress`, and the Linux display and keyboard equivalents

Crashes rather than degrades: every entry point rethrows `ReflectiveOperationException` as
`RuntimeException`, and `isSupported()` tests only the platform, never whether the reflection
resolved. On Windows it reports true and throws on the first injected keypress.

## FarPlaneTwo

`gl/opengl-lwjgl2/.../GLAPILWJGL2.java` is its LWJGL 2 backend. Resolves GL entry points itself and calls them through its own JNI layer.

- `GLContext.getFunctionAddress(String)`: package-private, the wrapper over LWJGL 2's native `ngetFunctionAddress(J)J`.
  Our `GLContext` shim declares five members and this is not one.
  Read in a `static {}` block, so it surfaces as `ExceptionInInitializerError`.
- `APIUtil.getBufferByte/Int/Long/Float/Double`
- `StateTracker.getReferences`, `StateTracker$VAOState` and its `elementArrayBuffer`
- `ContextCapabilities.tracker`
- `FastIntMap.put/get`

Works: `findGetter(ContextCapabilities.class, "OpenGL45", boolean.class)` public field on the shim, and the version flags are not among the always-false ones.

Not necessarily live: this is a separate backend module, and FP2 has an LWJGL 3 one to select instead.
