# LWJGLY

LWJGL 2's API => into an LWJGL 3 runtime.

Primary reason why this exists is to allow mods written against LWJGL 2 to keep working in Cleanroom, and to provide a better experience for developers to not see LWJGL 2 classes in their classpaths.

## How it Works

Every LWJGL 2 class falls into one of two cases:

**The class exists only in LWJGL 2**: `Display`, `GLContext`, `ContextCapabilities`, the maths types etc.
It ships as an ordinary class in `org.lwjgl.**` from LWJGLY's jar, alongside LWJGL 3's own.
Legal as LWJGL 3 neither seals nor signs the package, which `checkSplitPackage` confirms on every build.
Where the class is self-contained, LWJGL 2's *compiled class file is copied verbatim* so it stays in the package and doesn't need rewriting.

**The class exists in both, and LWJGL 2 has members LWJGL 3 dropped**: `GL11`, the `ARB*` extensions, `AL10` etc.
There cannot be a second `org.lwjgl.opengl.GL11`, so the missing methods are merged into LWJGL 3's own class as it loads, from generated *shims/adapters*:
  - Shim: class compiled against LWJGL 3 carrying only those methods, with bodies that delegate
  - Adapters: handwritten shims that supply methods that need explicit policy or state corrections and changes

Most of the second case turns out to be a spelling difference.
As GL names its typed entry points `glFogfv`, `glGetTexParameteriv` and LWJGL 3 chooses to keep to those names.
While LWJGL 2 dropped the type letters and overloaded instead. Reversing that rule accounts for *981 of the 1154 missing methods*.

The rule has three refinements beyond the plain suffix, each worth one line in `Naming` and each paying for a handful of methods that would otherwise need writing.
The type letters can live in the return rather than in a parameter (`glGetQueryObjectu(int, int)` returning `long` is `glGetQueryObjectui64`).
They can compose with GL's indexed spelling (`glGetIntegerui64i_vNV`) and `NV_half_float` carries halves in a `ShortBuffer`, so `hv` can be attributed to short-typed descriptors.
The last rule is tried after genuine short spellings, so a real short method is never ignored.

## Artifacts and Usages

`com.cleanroommc:lwjgly`: runtime portion, shims and adapters.
`com.cleanroommc:lwjgly-api`: compile portion, bridges.

```groovy
repositories {
    maven {
        name 'CleanroomMC Maven'
        url 'https://maven.cleanroommc.com'
    }
}

dependencies {
    compileOnly 'com.cleanroommc:lwjgly-api:1.0.0'
    runtimeOnly 'com.cleanroommc:lwjgly:1.0.0'
}
```

### Class Transformer

```java
byte[] transformed = LWJGLYTransformer.handles(name) ? LWJGLYTransformer.transform(name, bytes) : bytes;
```

### Window Bridge

`Display`, `Keyboard` and `Mouse` do not open anything of their own.
They read the window Cleanroom already has.

Implement `com.cleanroommc.lwjgly.spi.WindowBridge` over the SDL implementation and hand it over once, before any mod code runs:

*In Cleanroom's Case:*

```java
LWJGLY.setWindowBridge(new CleanroomWindowBridge(com.cleanroommc.client.input.Window.main()));
```

`WindowBridge.makeCurrent()` must bind LWJGL 3's capabilities as well as SDL's context.

`SDL_GL_MakeCurrent` and `GL.setCapabilities()` together.

LWJGL 3 resolves GL entry points per thread, doing one without the other leaves the two disagreeing about which context is live.

## Building and Contributing

See: `tool/README.md`

## Licence

LWJGLY vendors compiled classes from LWJGL 2, which is BSD-3-Clause.
The notice ships in `META-INF/licenses/LWJGL2-LICENSE.txt` and inside the package at `org/lwjgl/LICENSE.txt`.
