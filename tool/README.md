# LWJGLY Tooling

The `tool` module analyzes LWJGL 2 and LWJGL 3 bytecode, generates compatibility
classes, and verifies the result that LWJGLY ships. Build-time only.

Use the Gradle tasks in the repository root.
The root build supplies the LWJGL classpaths, generated-source directories, adapter map, and report paths in the required order.

## Design

`ApiIndex` reads jars and class directories with ASM and retains their `ClassNode` models.

`ApiDiff` classifies every externally usable LWJGL 2 class as:

- `IDENTICAL`: LWJGL 3 already provides the class and all relevant members.
- `INJECT`: The class exists in both versions, but LWJGL 3 is missing members. Those members are supplied by generated shims and/or adapters merged into the LWJGL 3 class at class-load time.
- `SHIM`: The class exists only in LWJGL 2. LWJGLY ships it as another `org.lwjgl` class, either by copying safe LWJGL 2 bytecode, compiling a generated replacement, or using a handwritten adapter.

For a missing method, the diff searches the target class and its hierarchy first.
Trying the same method name, then `Naming` applies the LWJGL 2-to-3 typed suffix rules used by OpenGL bindings.
A candidate is accepted only when the `CallPlan` maps every target argument.

The supported mappings are deliberately conservative:
- Pass-through values
- Buffer views
- Raw addresses
- Same-width retyped views
- Unwrapped pointer handles
- Buffer-derived byte counts
- OpenGL element-type constants

Anything else becomes a manual decision or ultimately a placeholder that throws an `UnsupportedOperationException`.

`NativeTypes` reads LWJGL 3's parameter-level `@NativeType` annotations.
They resolve ambiguous inserted arguments and allows `VerifyConversions` check that generated raw memory conversions land on the expected C pointer and element type.

`PointerHandles`only unwraps LWJGL 2 types that implement the stateless `PointerWrapper` contract. Stateful handle objects require handwritten bridges.

`Built` reads
- Compiled shim classes
- Vendored classes
- Compiled adapters
- Adapter map

And follows superclasses and interfaces to recognize placeholders from bytecode whose body only throws.

`VerifyCoverage`, `ProblemsDoc`, and `ScanUsage` uses this built view so a handwritten implementation always overrides.

## Pipeline

After importing or refreshing the Gradle project in an IDE, run `setup` to
generate the shim and adapter sources and package vendored LWJGL 2 classes.
This lets code analysis resolve compatibility types without running the full build.

`setup` prepares IDE inputs but does not run verification.

Run the full pipeline from the repository root:

```text
./gradlew build
```

On Windows:

```text
.\gradlew.bat build
```

The main tasks are:

| Task                | Purpose                                                                                                                                                                                                                                                                          | Main output                                                                                           |
|---------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------|
| `checkSplitPackage` | Rejects sealed or signed LWJGL 3 jars, either would make the second `org.lwjgl` package illegal.                                                                                                                                                                                 | `{buildDir}/lwjgly/split-package.txt`                                                                 |
| `apiDiff`           | Compares the two APIs and records every class/member decision. It only needs the dependency jars making it the quickest diagnostic.                                                                                                                                              | `{buildDir}/lwjgly/deltas.json`, `{buildDir}/lwjgly/delta-report.txt`                                 |
| `generateShims`     | Handles LWJGL 2-only classes by copying self-contained class files or generating exact-signature throwing placeholders. Adapters suppress their generated counterpart. Constants may be emitted through generated interfaces. Native methods are documented instead of declared. | `{buildDir}/generated/shim-src`, `{buildDir}/generated/vendored`, `{buildDir}/lwjgly/native_calls.md` |
| `generateAdapters`  | Emits generated method shims for members missing from classes shared by both APIs and indexes handwritten adapters. Automatic methods delegate through the recorded call plan. Unsupported methods throw.                                                                        | `{buildDir}/generated/adapter-src`, `{buildDir}/lwjgly/adapter-map.txt`                               |
| `packageAdapters`   | Packages adapters and generated method shims as resources and writes the target index consumed by the runtime transformer.                                                                                                                                                       | `{buildDir}/generated/adapter-resources/lwjgly`                                                       |
| `verifyAdapters`    | Enforces the assumptions used by method merging.                                                                                                                                                                                                                                 | Build failure on violation                                                                            |
| `verifyConversions` | Checks generated address, view, retype, handle, size, and GL-type conversions against LWJGL 3 native signatures.                                                                                                                                                                 | `{buildDir}/lwjgly/conversions.txt`                                                                   |
| `verifyCoverage`    | Ensures every callable LWJGL 2 member resolves through LWJGL 3, a generated shim, or an adapter.                                                                                                                                                                                 | `{buildDir}/lwjgly/coverage-gaps.txt`                                                                 |
| `problemsDoc`       | Describes throwing implementations and unsupported capability flags(!) from the classes that were actually built.                                                                                                                                                                | `{buildDir}/lwjgly/PROBLEMS.md`                                                                       |
| `scanUsage`         | Scans selected application/mod jars and classifies their real `org.lwjgl` references. This is adhoc and is not part of `check`.                                                                                                                                                  | `{buildDir}/lwjgly/usage-report.txt`                                                                  |

## Generated Shims and Adapters

- A vendorable shim has no native methods and no bytecode dependency on an unavailable LWJGL 2 type. The original compiled class is copied unchanged.
- A non-vendorable but externally nameable class gets generated Java source that preserves callable signatures and throws `UnsupportedOperationException` for unimplemented behavior.
- Package-private and nested implementation details do not have placeholders generated for them because of reflection, although required signature types and otherwise vendorable bytecode can still be included.
- Native declarations are omitted because LWJGLY does not ship the LWJGL 2 natives that implement them.
- Shims are generated compatibility classes.
  - Adapters are their handwritten counterparts.
  - A target may have both an adapter and a generated method shim, and their order in the  adapter map is significant
    - Adapters are merged first
    - The runtime skips members already present, handwritten method wins without duplicating the rest of the generated class.

- Adapters must satisfy these rules:
  - No fields or static initializer. The runtime merge copies methods only.
  - No `native` or `abstract` methods.
  - A visible constructor must chain to another constructor in the same adapter.
  - Stack-map frames must already exist. The transformer computes maximum stack  sizes but deliberately does not load classes to recompute frames.

- Put persistent adapter state in `com.cleanroommc.lwjgly.rt`, not in the adapter. Adapters are packaged as resources rather than ordinary application classes so the transformer can parse them with ASM without loading them before an OpenGL context exists.

## Reports and Failure Policy

Read `build/lwjgly/delta-report.txt` before implementing a shim or adapter.
It shows whether the tool found an exact target, a renamed target, only incompatible candidates, or no target.

The coverage promise is linkability.
A member may intentionally link to a throwing placeholder, but it must not disappear and cause a `NoSuchMethodError` or`NoSuchFieldError`.
Truly unavoidable missing declarations belong in `../scope/known-gaps.txt`.
`verifyCoverage` fails for a new unlisted gap and reports stale entries when a gap has been closed.

`build/lwjgly/PROBLEMS.md` is generated after compilation because it describes runtime behavior and not generator intent.
Capability flags follow the same rule:
- An extension string can only produce `true` when the extension has no callable methods or its methods are actually serviced.
- Otherwise, reporting support would route callers into methods that throw.

To prioritize remaining work against real-world situations, run:

```text
./gradlew scanUsage -Pjars="path/to/software.jar;path/to/minecraft_mod.jar"
```

The scanner accepts the platform classpath separator (normally `;`).
It reads jars or class directories and counts each distinct LWJGL reference and its callers, classifies it as a gap, throwing implementation, shipped shim, merged adapter, or existing LWJGL 3 member.
It also records Mixin targets to expose compatibility risks outside direct LWJGL calls.

## Changing the Tool

Keep automatic rules deterministic and narrower than the set of calls that merely look plausible.
The reference behavior is LWJGL 2's own bytecode.
If a conversion needs a choice about length, encoding, ownership, lifetime, or state, implement it in an adapter instead of broadening `Convertibility` or `CallPlan`.

After changing matching, conversions, generation, adapter structure (or really just about any part of the tooling), run:

```text
./gradlew :tool:test check
```

**Inspect the regenerated delta, conversion, coverage, problem, and native-call reports before committing their changes.**
