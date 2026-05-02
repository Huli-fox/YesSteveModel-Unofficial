# Remove In-Game Model Repair and Support Arbitrary Model Names

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document follows `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

Players should be able to place model folders or `.ysm` files in `config/ysmu/custom` whose names contain uppercase letters, Chinese characters, spaces, and other filesystem-supported symbols, then reload the mod and see those models in the model GUI without renaming them. The mod should no longer try to convert or "fix" old Yes Steve Model package formats while the game is running; version conversion belongs in external tools.

The implementation keeps disk names and UI display names human-readable, but converts unsafe model names to stable internal resource IDs before registering models with Minecraft and GeckoLib. Existing built-in IDs such as `default`, `steve`, and `alex` remain unchanged so existing saves and configs keep working.

## Progress

- [x] (2026-05-02 08:10+08:00) Read the project plan rules and located the in-game repair flow in `src/main/java/com/fox/ysmu/compat/YsmConverter.java` and `src/main/java/com/fox/ysmu/client/gui/PlayerModelScreen.java`.
- [x] (2026-05-02 08:10+08:00) Identified model-name validation gates in `FolderFormat`, `YsmFormat`, `YesModelUtils`, and `/ysm reload` diagnostics.
- [x] (2026-05-02 08:13+08:00) Added `ModelIdUtil.getInternalModelId` and display-name decoding helpers using `_name_` plus lowercase UTF-8 hex.
- [x] (2026-05-02 08:15+08:00) Updated folder and `.ysm` scanning so arbitrary disk basenames are accepted and encoded before entering `ModelData`.
- [x] (2026-05-02 08:16+08:00) Removed the repair button, `PlayerModelScreen.fix()`, `YsmConverter.java`, tinypinyin dependencies, and repair-only language keys.
- [x] (2026-05-02 08:17+08:00) Updated model screen display, sorting, and search to use decoded display names.
- [x] (2026-05-02 08:18+08:00) Ran static repository searches and `git diff --check`; no converter references remain in source/resources/dependencies and no whitespace errors were reported.
- [ ] Ask the user to run `.\gradlew.bat build` because project instructions say Gradle verification should be performed by the user in this sandbox.

## Surprises & Discoveries

- Observation: `Utils.isValidResourceLocation` only constructs a `ResourceLocation`, and the current scan paths reject names before any model data is parsed. This means unsupported names are currently skipped silently in `FolderFormat` and `YsmFormat`.
  Evidence: `FolderFormat.cacheAllModels` and `YsmFormat.cacheAllModels` both `continue` when `isValidResourceLocation` fails.

- Observation: the repair GUI renames `.ysm` files and converts folder models in-place, including deleting and replacing directories.
  Evidence: `PlayerModelScreen.fix()` calls `YsmConverter.convertAndReplace(folder, customDir)` and `YsmConverter` recursively deletes source and destination directories after conversion.

- Observation: the open-model-folder help text still told users that folders and `.ysm` files must use lowercase English/numeric names.
  Evidence: both language files had `gui.yes_steve_model.open_model_folder.tips` text with that restriction, so the text was updated with the new supported name classes.

## Decision Log

- Decision: Use a reversible internal ID for unsafe model names instead of renaming files or allowing raw unsafe names into `ResourceLocation`.
  Rationale: Minecraft and GeckoLib use `ResourceLocation` as registry/cache keys, while the user-visible requirement is about disk and UI names. A reversible ID preserves display names without requiring a protocol field or cache format bump.
  Date/Author: 2026-05-02 / Codex

- Decision: Keep already-safe lowercase IDs unchanged unless they start with the reserved encoded prefix.
  Rationale: This preserves built-in model IDs and existing saved selections while avoiding collisions with encoded names.
  Date/Author: 2026-05-02 / Codex

- Decision: Remove in-game conversion entirely rather than moving it behind another command or hidden button.
  Rationale: The user explicitly wants compatibility conversion handled by external scripts, not by the mod during gameplay.
  Date/Author: 2026-05-02 / Codex

- Decision: Limit folder-model scanning to immediate children of `config/ysmu/custom`.
  Rationale: The project convention is `custom/*` for model packs, and using the actual `File` path avoids the previous recursive scan's basename ambiguity when nested support directories such as `models` or `textures` exist inside a pack.
  Date/Author: 2026-05-02 / Codex

## Outcomes & Retrospective

Code changes are complete. The mod no longer has an in-game repair/conversion path, and arbitrary model folder or `.ysm` basenames are mapped to legal internal IDs without renaming files. UI display/search uses decoded names so players see the original model name. Gradle build validation is pending user execution per repository instructions.

## Context and Orientation

The mod scans custom models from `config/ysmu/custom`. Folder models are loaded by `src/main/java/com/fox/ysmu/model/format/FolderFormat.java`; `.ysm` packages are loaded by `src/main/java/com/fox/ysmu/model/format/YsmFormat.java` and unpacked by `src/main/java/com/fox/ysmu/util/YesModelUtils.java`.

A `ResourceLocation` is Minecraft's namespaced identifier type, formatted like `ysmu:default` or `ysmu:default/main`. It is used throughout the client renderer, GeckoLib caches, network messages, and player extended properties. The current implementation uses the folder name or `.ysm` basename directly as the resource path, so names with uppercase letters, Chinese characters, spaces, or punctuation are either rejected by validation or risk becoming invalid resource keys.

The model picker GUI is `src/main/java/com/fox/ysmu/client/gui/PlayerModelScreen.java`. It currently shows `modelId.getResourcePath()` directly and includes a "fix" button that runs conversion code in `YsmConverter`. That conversion code depends on `com.github.promeg:tinypinyin` from `dependencies.gradle`.

## Plan of Work

First, add helper methods to `src/main/java/com/fox/ysmu/util/ModelIdUtil.java`. The helper will treat names matching `[a-z0-9._-]+` as safe internal IDs, except for the reserved prefix `_name_`. Any other name will be encoded as `_name_` followed by lowercase hexadecimal UTF-8 bytes. Decoding will reverse that representation for display.

Second, update server-side scanning. `FolderFormat.cacheAllModels` will scan immediate child directories, keep the filesystem directory name for reading files, and pass the encoded model ID into `ModelData`. `YsmFormat.cacheAllModels` will stop rejecting `.ysm` basenames and will encode the basename before creating `ModelData`. `YesModelUtils.input` and `YesModelUtils.export` will stop rejecting files or folders solely because the model name is not a valid resource path.

Third, remove the repair feature. Delete `YsmConverter.java`, remove the `tinypinyin` dependencies, remove the repair button and `fix()` method from `PlayerModelScreen`, and remove language strings used only by that feature.

Fourth, update UI display and search. `PlayerModelScreen` and `ModelButton` will use `ModelIdUtil.getModelDisplayName` so encoded IDs appear as their original names. Sorting and search will also use display names.

## Concrete Steps

From repository root `d:\Code\YesSteveModel-Unofficial`, use repository search commands to confirm all converter references are gone:

    rg -n "YsmConverter|tinypinyin|gui\\.yes_steve_model\\.fix|model\\.compat" src dependencies.gradle src/main/resources/assets/ysmu/lang

The command should produce no matches after implementation.

Use repository search to confirm model-name resource validation is not applied to folder or `.ysm` basenames:

    rg -n "isValidResourceLocation\\(dirName\\)|isValidResourceLocation\\(modelId\\)|isValidResourceLocation\\(fileName\\)" src/main/java/com/fox/ysmu

The command should not show the old model basename validation in `FolderFormat`, `YsmFormat`, or `YesModelUtils.input`.

The following local verification commands were run from `d:\Code\YesSteveModel-Unofficial`:

    rg -n "YsmConverter|tinypinyin|gui\\.yes_steve_model\\.fix|model\\.compat|model\\.reload\\.error\\.dir_name" src dependencies.gradle src/main/resources/assets/ysmu/lang
    rg -n "isValidResourceLocation\\(dirName\\)|isValidResourceLocation\\(modelId\\)|isValidResourceLocation\\(fileName\\)" src/main/java/com/fox/ysmu
    git diff --check

The first two searches produced no matches, and `git diff --check` produced no output.

## Validation and Acceptance

Because this project instruction says the sandbox may not be able to access the local Gradle cache, the code agent should not repeatedly run Gradle. After code edits, ask the user to run this from `d:\Code\YesSteveModel-Unofficial`:

    .\gradlew.bat build

Expected result: Gradle completes successfully.

Manual acceptance after a successful build: place a folder model named `测试 Model #1` in `config/ysmu/custom` with `main.json`, `arm.json`, and a `.png`, or place a valid `.ysm` file named `测试 Model #1.ysm`. Start the client or server, run `/ysm reload`, open the model screen, and verify the model appears as `测试 Model #1`. The model should render, selection should sync to the server, and reopening the model screen should still show the decoded name rather than the internal `_name_...` ID.

The old repair feature is accepted as removed when the model screen no longer has a repair icon/button, the converter class is gone, and there are no `YsmConverter` references.

## Idempotence and Recovery

The ID encoding is deterministic: the same disk name always maps to the same internal ID, and running `/ysm reload` repeatedly will not rename files or modify model folders. If two models use exactly the same disk basename in folder and `.ysm` form, they still conflict at the model ID level, which matches the old behavior for identical model names.

If a change fails to compile, revert only the lines changed for this task and keep user edits intact. Do not delete model files or generated config/cache directories as part of this work.

## Artifacts and Notes

The reserved encoded prefix is `_name_`. For example, the disk name `测试 Model #1` becomes an internal resource path beginning with `_name_` followed by UTF-8 bytes encoded as lowercase hex. Safe existing names such as `default` and `wine_fox` remain unchanged.

## Interfaces and Dependencies

At the end of the change, `src/main/java/com/fox/ysmu/util/ModelIdUtil.java` will provide these common helpers:

    public static String getInternalModelId(String modelName)
    public static String getModelDisplayName(ResourceLocation modelId)
    public static String getModelDisplayName(String modelPath)

`FolderFormat` and `YsmFormat` will create `ModelData` with the internal ID returned by `getInternalModelId`. UI code will call `getModelDisplayName` when showing or searching model IDs.

Revision note, 2026-05-02 08:10+08:00: Created the plan after source reconnaissance to guide implementation and record the resource-ID encoding decision.

Revision note, 2026-05-02 08:18+08:00: Updated the plan after implementation to record completed edits, the immediate-child folder scanning decision, and local static verification evidence.
