# Phase 3 Client Registration Bridge

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document follows `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

Phase 3 makes OpenYSM folder models usable by the current 1.7.10 client renderer without porting OpenYSM's full ModelAssembly system. After this work, an OpenYSM folder under `config/ysmu/custom/<model>/ysm.json` that references player `models/main.json`, `models/arm.json`, PNG textures, and animation JSON can arrive at the client through the existing encrypted model cache and show its metadata, scale, textures, and extra animation labels through the current `ClientModelManager` maps.

The visible outcome is that OpenYSM built-in folder packages such as `builtin/default` and `builtin/misc/1_alex` can be copied into `config/ysmu/custom`, synchronized to the client, listed in the current model GUI, and rendered through the existing Gecko JSON path. Binary format-32 `.ysm` models are still parsed as `RawYsmModel`; this phase adds a conservative source-JSON fallback for raw models that still have bridgeable JSON or simple generated geometry, but it does not migrate OpenYSM's baked mesh renderer yet.

## Progress

- [x] (2026-05-09 19:59+08:00) Read `README.md`, `.agent/PLANS.md`, and `.agent/phase2-unified-raw-model.md` to confirm Phase 3 scope and the Gradle restriction.
- [x] (2026-05-09 19:59+08:00) Inspected client registration, legacy cache encryption, raw model adapter, and Gecko raw JSON types.
- [x] (2026-05-09 19:59+08:00) Injected OpenYSM metadata, scale, and extra animation labels into bridgeable Gecko main geometry in `RawYsmModelAdapter`.
- [x] (2026-05-09 19:59+08:00) Preserved PNG-only texture registration and added warnings for skipped JPEG/WebP/AVIF textures during the bridge.
- [x] (2026-05-09 19:59+08:00) Added focused tests for metadata/property injection, non-PNG skipping, and generated source-less geometry JSON.
- [x] (2026-05-09 19:59+08:00) Recorded local static checks and the host commands the user must run.
- [x] (2026-05-09 20:44+08:00) Investigated a runClient report where OpenYSM default and default boy reached server/client cache but did not appear in the model GUI.
- [x] (2026-05-09 20:44+08:00) Changed legacy model sync to send the encrypted password before cache-hit load requests and missing model files from one background sync task.
- [x] (2026-05-09 21:08+08:00) Added runtime INFO logs across OpenYSM scan/cache, legacy sync, client decrypt/register, and model GUI list rebuilding to locate the remaining missing-GUI-entry issue.
- [x] (2026-05-09 21:20+08:00) Used the new runtime logs to isolate OpenYSM GUI loss to client-side registration after geometry and before textures/MODELS were recorded.
- [x] (2026-05-09 21:20+08:00) Made client texture/MODELS registration run before animation registration, and made animation parse/condition failures log warnings instead of aborting the model entry.
- [x] (2026-05-09 21:37+08:00) Investigated purple/black texture output for OpenYSM default after the GUI entry fix.
- [x] (2026-05-09 21:37+08:00) Fixed legacy encrypted model decode to preserve serialized file order with `LinkedHashMap`, preventing multi-texture byte slices from being assigned to the wrong names.
- [x] (2026-05-09 21:40+08:00) Reduced expected OpenYSM animation parse failures from full Gson stack traces to single-line warnings.

## Surprises & Discoveries

- Observation: The current encrypted cache only serializes model JSON, texture bytes, and animation JSON maps.
  Evidence: `EncryptTools.encryptModel` writes `data.getModel()`, `data.getTexture()`, and `data.getAnimation()` only, and `EncryptTools.decryptModel` reconstructs `ModelData` with the same three maps.
- Observation: The current client already knows how to display scale, model info, and extra animation labels if they are present in the Gecko geometry description.
  Evidence: `ClientModelManager.registerGeo` reads `RawGeometryTree.properties.getHeightScale()`, `getWidthScale()`, and `getExtraInfo()` into `SCALE_INFO`, `EXTRA_INFO`, and `EXTRA_ANIMATION_NAME`.
- Observation: The OpenYSM built-in packages named in the Phase 3 acceptance target use Gecko `format_version` `1.12.0` for their player JSON.
  Evidence: `rg -n "format_version" OpenYSM\src\main\resources\assets\yes_steve_model\builtin\default\models OpenYSM\src\main\resources\assets\yes_steve_model\builtin\misc\1_alex\models` showed `1.12.0` for `default` and `misc/1_alex` player model files.
- Observation: Binary `.ysm` raw geometry stores baked face positions, while current 1.7.10 rendering consumes Gecko cube JSON.
  Evidence: `RawYsmModel.RawCube` stores `RawFace.positions/u/v/normal`, and `GeoBuilder.constructBone` only turns raw Gecko `Cube` entries into `GeoCube` objects.
- Observation: The reported OpenYSM folders were scanned and cached correctly, but cached models could still fail to reach the GUI promptly.
  Evidence: Decrypting `run/config/ysmu/cache/server` showed `_name_44656661756c74e696b0` and `_name_44656661756c7420626f79e696b0`; the same ids existed in `run/config/ysmu/cache/client`.
- Observation: `SyncModelFiles` intended to send `SendModelPassword` before `RequestLoadModel`, but the password packet was submitted asynchronously to the same shared `ThreadTools` pool used by client cache loading.
  Evidence: `SyncModelFiles.Handler.onMessage` called `sendPassword(sender)` and then `sendModelFiles(...)`; `sendPassword` only queued the packet on `ThreadTools.THREAD_POOL`, while cache-hit `RequestLoadModel` packets were sent immediately.
- Observation: The second run showed both OpenYSM models decrypting and starting client registration, but neither reached texture registration or the final `registered model` log.
  Evidence: `fml-client-latest.log` showed `YSM client registering model ysmu:_name_...` and `registered geometry .../main|arm` for both target ids, followed by other models; the GUI then logged `totalClientModels=6`.
- Observation: After the GUI entry fix, `Default新` registered both texture ids but `default.png` failed to upload because the cached bytes could not be decoded as an image.
  Evidence: `fml-client-latest.log` showed `Failed to register texture ysmu:_name_44656661756c74e696b0/default.png`, with a `NullPointerException` after `ImageIO.read(...)` returned null, while the source `textures/default.png` on disk had a valid PNG header.
- Observation: Legacy encrypted model decode stored file metadata in a `HashMap`, so names and lengths could be iterated in a different order than the serialized data bytes.
  Evidence: `EncryptTools.encryptModel` writes map file info and raw data in the source map order, but `readMapDataInfo` rebuilt the metadata with `Maps.newHashMap()`.

## Decision Log

- Decision: Carry OpenYSM metadata through the existing network protocol by injecting `ysm_height_scale`, `ysm_width_scale`, and `ysm_extra_info` into the main Gecko JSON before `ModelData` is encrypted.
  Rationale: Adding a fourth metadata payload to the legacy cache would change the existing wire/cache format. The current Gecko parser already supports these description fields, so JSON injection gives the client the needed data without packet id or cache layout changes.
  Date/Author: 2026-05-09 / Codex
- Decision: Keep binary `.ysm` rendering conservative in Phase 3 and avoid porting OpenYSM's baked mesh renderer.
  Rationale: OpenYSM binary geometry is stored as baked faces, while the current 1.7.10 renderer consumes legacy Gecko cubes. The README explicitly says to prefer minimal bridging first and only migrate the renderer when conversion loss blocks useful behavior. Folder models are the acceptance target for this phase.
  Date/Author: 2026-05-09 / Codex
- Decision: Generate a best-effort Gecko JSON fallback only when a raw geometry lacks original `sourceJson`.
  Rationale: Folder models keep their authoritative JSON and should render normally. Source-less binary raw geometry needs some path into the current cache; deriving boxes from baked face bounds is enough to keep parsing, metadata, and simple unrotated cubes working, while the limitation is explicit for complex baked meshes.
  Date/Author: 2026-05-09 / Codex
- Decision: Run the server's legacy model-sync response as one background task that sends `SendModelPassword` synchronously before cache-hit `RequestLoadModel` and missing `SendModelFile` packets.
  Rationale: The password is required before cache files can decrypt. Queueing the password packet separately could let client load tasks wait on a password packet that was still behind other work in the shared pool, especially in integrated-server testing.
  Date/Author: 2026-05-09 / Codex
- Decision: Register texture ids into `ClientModelManager.MODELS` before animation registration, and catch animation parse/condition failures per file/name.
  Rationale: Model selection is driven by `MODELS`. OpenYSM animation JSON may contain data that the legacy GeckoLib parser cannot consume yet, but that should not hide an otherwise converted model from the selection GUI.
  Date/Author: 2026-05-09 / Codex
- Decision: Use insertion-order maps while reading legacy encrypted model metadata, and assert full reads for names, lengths, and payload blocks.
  Rationale: The serialized payload stores raw file bytes as one contiguous stream whose boundaries are defined by the preceding metadata order. Losing that order can corrupt any multi-file section with different byte lengths.
  Date/Author: 2026-05-09 / Codex
- Decision: Roll back the changes to SyncModelFiles.java and remove the logs added in this task.
  Rationale: Manual test passed.
  Date/Author: 2026-05-09 / User

## Outcomes & Retrospective

Phase 3 now enriches the legacy bridge output instead of changing the network protocol. `RawYsmModelAdapter.toLegacyModelData` writes the main and arm geometry through a JSON bridge, injects `ysm_height_scale`, `ysm_width_scale`, and `ysm_extra_info` into the main geometry description, keeps PNG textures, and logs skipped non-PNG textures. The current `ClientModelManager.registerGeo` can read these fields without new packet ids or cache sections.

The raw source-less fallback can emit Gecko `format_version: 1.12.0` JSON from `RawYsmModel.RawGeometry`. It reconstructs bone names, parent names, pivots, rotations, and best-effort cube boxes from baked face bounds. This is intentionally not a full baked mesh renderer; complex binary models may still need a later renderer migration.

Follow-up runtime debugging found that the bridge output was present in both server and client cache, so the missing GUI entries were in the cache-load path rather than the scanner. `SyncModelFiles` now preserves the password-before-load relationship inside one background task, avoiding a race where cache-hit loads wait for a password packet that has not been sent yet.

After a second run still failed to show the models, runtime logging was added instead of guessing further. The next `runClient` log should show whether each target model reaches `OpenYsmFormat`, `CACHE_NAME_INFO`, `SyncModelFiles`, `RequestLoadModel`, `ClientModelManager.registerAll`, and finally `PlayerModelScreen.initGui`.

The runtime logs then showed the target OpenYSM models reaching `ClientModelManager.registerAll` and registering geometry, but not textures or final model entries. Client registration now records texture ids before animation parsing and logs animation failures as warnings, so unsupported OpenYSM animation details should no longer prevent the model from appearing in the selection GUI.

The subsequent purple/black texture report exposed a separate legacy cache container bug. `Default新` produced texture metadata in one order but decoded it through a `HashMap`, allowing `default.png` and `blue.png` byte ranges to be cut against the wrong names. The decoder now preserves metadata insertion order and a regression test covers multi-texture round-trip byte identity.

OpenYSM animation JSON remains only partially compatible with the legacy GeckoLib parser in this phase. Those parse failures are now concise warnings instead of full Gson stack traces, so runtime logs stay useful while the model still falls back to default animations.

## Context and Orientation

The current client registration entry point is `src/main/java/com/fox/ysmu/client/ClientModelManager.java`. `registerAll(ModelData data)` builds a root `ResourceLocation` from `data.getModelId()`, registers every model JSON in `data.getModel()`, merges all animation JSON in `data.getAnimation()` into one `AnimationFile`, and registers every PNG texture byte array in `data.getTexture()`.

`ClientModelManager.registerGeo` parses each JSON file with `software.bernie.geckolib3.geo.raw.pojo.Converter`, builds a `RawGeometryTree`, then stores the main model's scale and extra info in maps used by GUI and rendering code. A Gecko JSON `description` can contain `ysm_height_scale`, `ysm_width_scale`, and `ysm_extra_info`; the existing `ModelProperties` and `ExtraInfo` classes already expose those fields.

Phase 2 added `src/main/java/com/fox/ysmu/model/resource/RawYsmModelAdapter.java`. It converts a folder-backed `RawYsmModel` into legacy `ModelData` by copying `main` and `arm` source JSON, PNG textures, and main/arm/extra animation JSON. This phase extends that adapter so OpenYSM metadata and properties survive the conversion.

## Plan of Work

First, update `RawYsmModelAdapter` so `toLegacyModelData` writes enriched main model JSON instead of raw `sourceJson` bytes. The enriched JSON should preserve all existing geometry content, ensure the first `minecraft:geometry[0].description` object exists, then set `ysm_height_scale`, `ysm_width_scale`, and `ysm_extra_info`. `ysm_extra_info.name` comes from `raw.metadata.name`; `tips` comes from `raw.metadata.tips`; `authors` is an array of author names with role appended when present; `license` prefers `metadata.licenseType` and falls back to `metadata.licenseDescription`. The extra animation label array is eight slots for `extra0` through `extra7`, using language file keys like `properties.extra_animation.extra0` when present, then the `properties.extraAnimations` value when it is not blank and not a config reference, then the slot key.

Second, keep texture bridging PNG-only. The current `OuterFileTexture` path expects PNG bytes in practice. For each non-PNG `RawTexture` found in `raw.mainEntity.textures`, log a warning that the texture is skipped until the ImageStream path is wired into 1.7.10. This makes JPEG/WebP/AVIF degradation visible instead of silent.

Third, add a small generated-geometry fallback for raw models that have no `sourceJson`. The fallback should emit valid `format_version: 1.12.0` geometry JSON with description and bone names/pivots/rotations, but it should only be used when no source JSON exists. If a raw geometry contains baked faces, the fallback is enough to keep parsing and metadata registration deterministic but may not preserve full shape; that limitation must remain recorded in this plan.

Fourth, update tests in `src/test/java/com/fox/ysmu/model/resource/YsmResourceFormatTest.java` to prove the bridge injects scale, metadata, author/license info, extra animation labels, and skips non-PNG textures.

## Concrete Steps

From repository root `D:\Code\YSMU`, edit the Java files described above. Do not run Gradle from the sandbox. The host verification commands for the user are:

    .\gradlew.bat test
    .\gradlew.bat runClient

For manual client verification, copy `OpenYSM/src/main/resources/assets/yes_steve_model/builtin/default` to `run/config/ysmu/custom/openysm_default` or the equivalent runtime `config/ysmu/custom/openysm_default`, then start the client and open the YSMU model GUI. The model should be listed with its metadata and render with its PNG textures. Repeat with `OpenYSM/src/main/resources/assets/yes_steve_model/builtin/misc/1_alex`.

## Validation and Acceptance

Acceptance for this phase is that `.\gradlew.bat test` passes on the host and `.\gradlew.bat runClient` can show and render at least `builtin/default` and `builtin/misc/1_alex` copied as OpenYSM folders. The current legacy models under `assets/ysmu/custom` must still load, and converted script output must remain compatible because the legacy folder scanner is unchanged.

OpenYSM metadata acceptance is visible in the model GUI tooltip: name, tips, authors, and license should appear through `ClientModelManager.EXTRA_INFO` for the model's main geometry. Scale acceptance is that `CustomPlayerEntity.getHeightScale()` and `getWidthScale()` can read the injected scale through `ClientModelManager.SCALE_INFO`.

Non-PNG texture acceptance is that PNG textures register normally, while JPEG/WebP/AVIF texture entries are skipped with a warning instead of being silently added to `ModelData`.

## Idempotence and Recovery

The bridge is deterministic and can be run repeatedly because it derives client metadata from `RawYsmModel` every time the server cache is rebuilt. Rebuilding packs overwrites server cache files the same way Phase 2 already did. If the injected JSON is malformed, `ClientModelManager.registerGeo` logs the geometry id and continues with other models; the fix is to remove or repair the offending model folder, then reload packs or restart.

## Artifacts and Notes

Local checks performed in this environment:

    rg -n "record\b|case .*->|Files\.readString|Files\.writeString|List\.of|Map\.of|Set\.of|Objects\.requireNonNullElse|var " src\main\java\com\fox\ysmu\model\resource\RawYsmModelAdapter.java src\test\java\com\fox\ysmu\model\resource\YsmResourceFormatTest.java

This returned no matches. `git diff --check` exited 0. Gradle test/build verification remains a host-side task.

## Interfaces and Dependencies

`RawYsmModelAdapter` should continue to expose:

    public static boolean isBridgeable(RawYsmModel raw)
    public static ModelData toLegacyModelData(RawYsmModel raw, String modelId) throws IOException

The bridge relies on existing classes:

    software.bernie.geckolib3.geo.raw.pojo.ModelProperties
    software.bernie.geckolib3.geo.raw.pojo.ExtraInfo
    com.fox.ysmu.client.ClientModelManager

No new network packet ids or external dependencies are required for this phase.

Revision note, 2026-05-09: Created this ExecPlan after reading the README and Phase 2 plan, before implementing Phase 3, to make the client bridge work resumable from a single document.

Revision note, 2026-05-09: Updated this ExecPlan after implementation with completed progress, the metadata injection decision, source-less generated JSON behavior, and local static-check evidence.
