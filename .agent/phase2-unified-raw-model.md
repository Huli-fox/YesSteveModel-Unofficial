# Phase 2 Unified Raw Model Representation

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document follows `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

Phase 2 lets the 1.7.10 server understand OpenYSM-style model packages before the client rendering bridge exists. After this work, the server can scan `config/ysmu/custom/<model>/ysm.json`, read the referenced player model, textures, animations, metadata, languages, functions, sounds, vehicles, and projectiles into a rendering-independent `RawYsmModel`, then bridge player folder models that still use Gecko JSON and PNG textures into the existing encrypted legacy cache. A user can see the behavior by putting an OpenYSM folder model under `config/ysmu/custom`, starting the server, and seeing it appear in the same server model list as legacy folder models.

This phase also adds binary `.ysm` format-32 parsing and serialization support. Because the current 1.7.10 client can only render legacy Gecko JSON through `ClientModelManager`, binary `.ysm` files are parsed and indexed as raw models but are not converted into legacy render data unless they contain bridgeable folder JSON source. This avoids pretending that baked binary geometry can safely render through the old path before Phase 3.

## Progress

- [x] (2026-05-09 19:22+08:00) Read `README.md`, `.agent/PLANS.md`, and `.agent/phase1-format-security.md` to confirm Phase 2 scope and Gradle restrictions.
- [x] (2026-05-09 19:42+08:00) Added JVM 8-compatible raw model classes and OpenYSM folder/binary deserializers under `src/main/java/com/fox/ysmu/model/resource`.
- [x] (2026-05-09 19:45+08:00) Added a bridge from raw OpenYSM folder models to current `ModelData` when the package references player `main` and `arm` Gecko JSON plus PNG textures.
- [x] (2026-05-09 19:49+08:00) Updated `ServerModelManager.reloadPacks()` to create `built`, `server_index`, and `blacklist.txt`, scan built/custom for OpenYSM packages, and keep legacy folder/legacy `.ysm` fallback.
- [x] (2026-05-09 19:55+08:00) Added focused tests for folder parsing, folder-to-legacy bridge, and format-32 binary round trip without launching Minecraft.
- [x] (2026-05-09 19:58+08:00) Recorded local non-Gradle verification and host Gradle commands the user must run.

## Surprises & Discoveries

- Observation: The existing runtime cache format still expects `ModelData` containing named maps for model JSON, texture bytes, and animation JSON.
  Evidence: `EncryptTools.assembleEncryptModels` writes `data.getModel()`, `data.getTexture()`, and `data.getAnimation()` maps, and `ClientModelManager.registerGeo` parses geometry bytes with the current Gecko JSON `Converter`.
- Observation: OpenYSM folder parsing uses Java 17 syntax, JOML transforms, and optional ImageStream decoders that are not appropriate for this phase.
  Evidence: `OpenYSM/.../YSMFolderDeserializer.java` contains a `record ImageMeta`, switch expressions, JOML imports, and `rip.ysm.imagestream` imports.
- Observation: Scanning old `.ysm` files through the new binary parser would produce noisy warnings before the legacy `.ysm` scanner gets its chance.
  Evidence: Old YSM archives start with `YSGP` as a big-endian legacy header, while OpenYSM encrypted files from Phase 1 start with the byte prefix `EF BB BF 59 53 47 50`. `OpenYsmFormat` now checks that prefix before decrypting.

## Decision Log

- Decision: Port the raw model representation under `com.fox.ysmu.model.resource` instead of `com.elfmcys.yesstevemodel.resource`.
  Rationale: The README says the 1.7.10 mod should keep `com.fox.ysmu` as the root for project-specific code while vendored low-level `rip.ysm` code may remain outside it.
  Date/Author: 2026-05-09 / Codex
- Decision: Bridge only folder models with raw Gecko JSON and PNG player textures into the existing legacy cache during Phase 2.
  Rationale: Current client rendering parses JSON geometry and PNG textures. Binary `.ysm` stores baked face data, so converting it into the legacy renderer belongs to Phase 3.
  Date/Author: 2026-05-09 / Codex
- Decision: The new server scan only caches directories that contain `ysm.json`; old top-level `main.json`/`arm.json` folders continue through `FolderFormat`.
  Rationale: `YSMFolderDeserializer` still supports legacy fallback for tests and future raw tooling, but running it over every legacy folder in `ServerModelManager` would duplicate cache work and increase behavioral risk.
  Date/Author: 2026-05-09 / Codex

## Outcomes & Retrospective

Phase 2 now has a raw resource layer, folder deserializer, format-32 binary serializer/deserializer, a folder-to-legacy bridge, and server scan integration. OpenYSM folder models under `config/ysmu/custom/<name>/ysm.json` are parsed into `RAW_MODEL_INFO`; if their player model references bridgeable Gecko JSON and PNG textures, they are also written into the existing encrypted server cache and listed in `CACHE_NAME_INFO`.

Binary OpenYSM `.ysm` files are detected by their encrypted OpenYSM prefix, decrypted through `YsmCrypt`, and parsed as format 32. They are stored as raw models, but most will not enter the legacy render cache because they do not contain source Gecko JSON. That is intentional until Phase 3 adds a client bridge or mesh renderer.

Gradle was not run because repository instructions forbid sandboxed Gradle execution. The remaining acceptance step is for the user to run `.\gradlew.bat test` and `.\gradlew.bat runServer` from the repository root.

## Context and Orientation

The existing 1.7.10 server model scan starts in `src/main/java/com/fox/ysmu/model/ServerModelManager.java`. It creates `config/ysmu/custom`, copies built-in legacy models into that directory, initializes an AES password file, then calls `YsmFormat.cacheAllModels` for old encrypted `.ysm` archives and `FolderFormat.cacheAllModels` for old folder models containing `main.json`, `arm.json`, and at least one `.png`.

The current multiplayer sync path stores cached server files under `config/ysmu/cache/server`. The `CACHE_NAME_INFO` map uses an internal model id, generated by `ModelIdUtil.getInternalModelId`, as the key. Unsafe disk names are encoded to a lower-case resource-safe id so they can become a Minecraft 1.7.10 `ResourceLocation`.

An OpenYSM folder model is a directory containing `ysm.json`. The JSON points to resources such as `files.player.model.main`, `files.player.model.arm`, `files.player.texture`, `files.player.animation`, and `files.player.animation_controllers`. It can also contain metadata, properties, language JSON files, `.molang` function files, `.ogg` sound files, vehicles, and projectiles. In this phase, these are represented in memory by `RawYsmModel`, not rendered directly.

## Plan of Work

Create `src/main/java/com/fox/ysmu/model/resource/pojo/RawYsmModel.java` as a JVM 8-compatible data holder adapted from OpenYSM. Keep public fields for simple mapper-style use and add `sourceJson` or `sourceData` fields for folder resources that the current legacy bridge can reuse.

Create `YSMFolderDeserializer` in `src/main/java/com/fox/ysmu/model/resource`. It should accept a directory path, read `ysm.json` when present, or fall back to the old folder layout when absent. It should validate that the player has `main` and `arm` model JSON and at least one PNG texture. It should parse metadata and properties, read player animations and controllers, collect language/function/sound resources, and collect vehicle/projectile references. It should use JDK 8 APIs and Gson, not Java 17 records or `Files.readString`.

Create `YSMBinaryDeserializer` and `YSMBinarySerializer` in the same package by adapting the OpenYSM format-32 implementation to Java 8. They must use the Phase 1 `rip.ysm.security.YSMByteBuf` and avoid stdout/stderr logging. Formats older than 32 may be recognized but should fail clearly unless the parser can safely handle them.

Create a small adapter, `RawYsmModelAdapter`, that converts a folder-backed `RawYsmModel` to current `ModelData` when possible. It should name model entries `main` and `arm`, texture entries with their file names including `.png`, and animation entries `main`, `arm`, and `extra`. Missing animation files should use the same default animation fallbacks as `FolderFormat`.

Update `ServerModelManager` to create `BUILT`, `CACHE_SERVER_INDEX_FILE`, and `blacklist.txt`, initialize a 56-byte OpenYSM server key in `server_index`, scan `BUILT` and `CUSTOM` for OpenYSM folder models and binary `.ysm`, and then run the legacy scanners. Built-in legacy model copying remains unchanged in this phase so current users keep the same defaults.

## Concrete Steps

From repository root `D:\Code\YSMU`, edit the Java files described above. Do not run Gradle from the sandbox. The host verification commands for the user are:

    .\gradlew.bat test
    .\gradlew.bat runServer

During manual server verification, place an OpenYSM folder under `config/ysmu/custom/<name>` with a `ysm.json` that references player `models/main.json`, `models/arm.json`, and at least one `textures/*.png`. Start the server and check the log for a successful scan rather than an invalid-model warning.

## Validation and Acceptance

Acceptance for Phase 2 is that `.\gradlew.bat test` passes on the host and server startup can scan both a legacy folder model and an OpenYSM `ysm.json` folder model. The OpenYSM folder model should be cached under `config/ysmu/cache/server` and appear in `ServerModelManager.CACHE_NAME_INFO` using `ModelIdUtil.getInternalModelId` for the disk name. Invalid OpenYSM folders should log a clear reason such as missing player main model, missing arm model, or no PNG player texture.

Binary `.ysm` acceptance is narrower in this phase: a format-32 encrypted `.ysm` can be decrypted with `YsmCrypt.decryptYsmFile`, parsed into `RawYsmModel`, and reserialized for cache-format tests. It does not have to render through the current client until Phase 3.

## Idempotence and Recovery

The server scan is additive and can be run repeatedly. `config/ysmu/built` and `config/ysmu/custom` are created if absent. `server_index` keeps its existing server key if it is valid; if it is missing or invalid, a new key is generated. The legacy `PASSWORD` file remains untouched except for the existing `initPassword` behavior.

If an OpenYSM folder fails to bridge, the scanner logs the failure and continues with other models. Legacy `FolderFormat` and `YsmFormat` are still called after the new scanner so old model loading remains available.

## Artifacts and Notes

Phase 1 added `rip.ysm.security.YSMByteBuf` and `YsmCrypt`, which this phase reuses for binary format work.

Local checks performed in this environment:

    rg -n "TODO|System\.out|System\.err|printStackTrace|record\b|case .*->|Files\.readString|Files\.writeString|List\.of|Map\.of|Set\.of|Objects\.requireNonNullElse|com\.elfmcys|rip\.ysm\.imagestream|org\.joml|var " src\main\java\com\fox\ysmu\model\resource src\main\java\com\fox\ysmu\model\format\OpenYsmFormat.java src\test\java\com\fox\ysmu\model\resource

This returned no matches. `git diff --check` also exited 0. Gradle test/build verification remains a host-side task.

## Interfaces and Dependencies

At the end of this phase, these public classes should exist:

    src/main/java/com/fox/ysmu/model/resource/pojo/RawYsmModel.java
    src/main/java/com/fox/ysmu/model/resource/YSMFolderDeserializer.java
    src/main/java/com/fox/ysmu/model/resource/YSMBinaryDeserializer.java
    src/main/java/com/fox/ysmu/model/resource/YSMBinarySerializer.java
    src/main/java/com/fox/ysmu/model/resource/RawYsmModelAdapter.java

`YSMFolderDeserializer` should expose:

    public YSMFolderDeserializer(Path sourcePath) throws IOException
    public RawYsmModel deserialize() throws IOException
    public static boolean isModelFolder(Path dir)

`RawYsmModelAdapter` should expose:

    public static ModelData toLegacyModelData(RawYsmModel raw, String modelId) throws IOException

Revision note, 2026-05-09: Created this ExecPlan after reading the README and Phase 1 plan, before implementing Phase 2, to make the work resumable from a single document.

Revision note, 2026-05-09: Updated this ExecPlan after implementation with completed progress, the binary/legacy scan decision, and local static-check evidence.
