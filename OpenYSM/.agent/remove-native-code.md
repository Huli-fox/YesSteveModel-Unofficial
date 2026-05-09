# Remove disabled Native subsystem

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This plan follows `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

OpenYSM currently has a disabled C++/JNI build path, plus Java code that still tries to model optional native loading and contains unreachable native rendering and compression branches. After this change, the mod should build and start without any native library extraction or `System.load` path. Model loading, model sync, export, audio decoding, and rendering should continue to use the existing Java implementations.

## Progress

- [x] (2026-05-08 22:58 CST) Located the disabled Gradle `compileNative` task, root `CMakeLists.txt`, `jni/`, `src/main/native/`, `YSMParser` submodule, Java JNI wrappers in `com.ysm.parser`, and Java runtime references to `NativeLibLoader`.
- [x] (2026-05-08 23:12 CST) Removed native build inputs and submodule metadata: `.gitmodules`, root `CMakeLists.txt`, `jni/`, `src/main/native/`, `YSMParser`, the Gradle `compileNative` task, and generated native resource wiring.
- [x] (2026-05-08 23:20 CST) Removed Java native library loader and JNI wrappers, then forced compression, model mapping, and mod initialization onto the existing Java paths.
- [x] (2026-05-08 23:25 CST) Renamed or removed leftover native-only rendering/cache hooks: `NativeModelRenderer` became `GeoMeshRenderer`, `GeoModel` no longer declares native cache methods, and `NativeAudioDecoder` became `OpusAudioDecoder`.
- [x] (2026-05-08 23:30 CST) Searched for remaining project-owned native references and documented validation for the user to run.

## Surprises & Discoveries

- Observation: `NativeLibLoader.init()` currently sets the mod as available even when no native library is loaded, so most availability checks are effectively unrelated to native success.
  Evidence: `NativeLibLoader.init()` calls `available = true` after attempting extraction/loading.
- Observation: `NativeAudioDecoder` was not JNI code despite its name; it used the Java `org.concentus.OpusDecoder` and Ogg parser classes.
  Evidence: it was renamed to `OpusAudioDecoder` because it imports `org.concentus.OpusDecoder` and contains no `native` methods or `System.load`.
- Observation: Several `ServerModelManager.native*` methods are pure Java implementations of model loading, sync, and export.
  Evidence: `nativeLoadModels`, `nativeSyncModels`, `nativeSendModelData`, and `nativeExportModel` contain Java file IO, crypto, and packet code, not JNI declarations.

## Decision Log

- Decision: Remove the native build artifacts and JNI wrappers rather than leaving disabled task stubs.
  Rationale: The requested outcome is to remove Native-related code, and the build path is already disabled and unused.
  Date/Author: 2026-05-08 / Codex
- Decision: Keep Java-only audio decoding functionality even though the class name includes `Native`.
  Rationale: It is active pure Java functionality for Opus playback, not a native dependency. It can be renamed later if desired, but deleting it would remove working audio behavior.
  Date/Author: 2026-05-08 / Codex
- Decision: Preserve `YesSteveModel.isAvailable()` as an always-true compatibility facade instead of removing all availability checks in this pass.
  Rationale: Many event and mixin guards call this method. Making it independent of native loading removes the native dependency with much lower risk than broad behavioral churn.
  Date/Author: 2026-05-08 / Codex
- Decision: Rename `NativeModelRenderer` to `GeoMeshRenderer` and delete the unreachable native rendering branch instead of keeping a disabled flag.
  Rationale: Rendering already always used the Java path. Removing the branch and native cache declarations eliminates JNI linkage without changing rendered output.
  Date/Author: 2026-05-08 / Codex

## Outcomes & Retrospective

Complete. The build no longer defines native compilation or generated native resource packaging. The JNI wrapper package and native loader were removed. Rendering, YSM zstd compression, audio decoding, model loading, sync, and export now route through Java-only implementations. Validation was not run here because repository guidance says agents should request the user to run build commands for code changes.

## Context and Orientation

The native subsystem consists of several parts. `build.gradle` defines a disabled `compileNative` task that would build `ysm-core` into `src/generated/resources/META-INF/native`. The root `CMakeLists.txt` builds that `ysm-core` library from `src/main/native/render.cpp` and the `YSMParser` submodule. The `jni/` directory contains copied JNI headers. Java classes `src/main/java/com/elfmcys/yesstevemodel/NativeLibLoader.java`, `src/main/java/com/ysm/parser/YSMParser.java`, and `src/main/java/com/ysm/parser/YSMNative.java` load or declare native methods. Client rendering has an unreachable native branch in `src/main/java/com/elfmcys/yesstevemodel/geckolib3/geo/NativeModelRenderer.java` and native cache declarations in `GeoModel.java`.

Several names contained `native` but were not native dependencies. `ServerModelManager.nativeLoadModels` and related methods were Java implementations that replaced earlier native callbacks; they were renamed to `loadModelDefinitions`, `syncModels`, `handleModelSyncPayload`, and `exportModel`. `NativeAudioDecoder` was also Java-only and was renamed to `OpusAudioDecoder`.

## Plan of Work

The work was completed in that order: the disabled Gradle task, generated native resource source set, root CMake build file, JNI header directory, C++ source directory, and `YSMParser` submodule entry were deleted. Java JNI wrappers and `NativeLibLoader` were removed, and `YesSteveModel` now initializes configuration directly and reports the mod as available without loading a library. Native rendering branches and native cache declarations were removed, so rendering always uses the Java renderer. Pure Java callers that referenced `NativeLibLoader` or `YSMNative` now use Java code directly.

## Concrete Steps

From `D:\Code\OpenYSM`, inspect references with `rg`, edit files using `apply_patch`, and remove native-only directories after verifying their resolved paths are under the repository root. Do not run Gradle commands in this environment because `AGENTS.md` instructs agents not to run build commands for code changes.

## Validation and Acceptance

The user should run `.\gradlew.bat compileJava` from `D:\Code\OpenYSM`. Success means Java compilation no longer depends on `NativeLibLoader`, `com.ysm.parser`, native methods, CMake, or generated native resources. For broader packaging confidence after native resource removal, the user should also run `.\gradlew.bat build`.

## Idempotence and Recovery

The edits are source-level removals and renames. Re-running the search commands is safe. Directory deletion is limited to the resolved repository-owned native paths `jni`, `src/main/native`, and `YSMParser`. If a deletion is too broad, restore the path from version control.

## Artifacts and Notes

Important initial search hits include `build.gradle` lines defining `compileNative`, `CMakeLists.txt` lines building `ysm-core`, `NativeLibLoader.java`, `com/ysm/parser`, `rip/ysm/algorithms/YsmZstd.java`, `YSMClientMapper.java`, `GeoModel.java`, and `NativeModelRenderer.java`.

## Interfaces and Dependencies

At completion, no Java class should declare `native` methods for project-owned code, no code should call `System.load` or `System.loadLibrary` for YSM native libraries, and Gradle should not register or depend on `compileNative`. Existing Java public methods such as `YesSteveModel.isAvailable()` may remain as compatibility methods, but they must not depend on a native loader.
