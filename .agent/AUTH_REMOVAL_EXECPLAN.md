# Remove Authorization Models

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document follows `PLANS.md` in the repository root. It is self-contained so a future contributor can continue the authorization removal without relying on prior chat context.

## Purpose / Big Picture

The mod currently has a separate "authorization model" path: models can be placed under `config/ysmu/auth`, serialized with an auth flag, synced to clients as locked models, and blocked on the server unless a player owns a matching `ExtendedAuthModels` entry. For a small private server this adds persistence, packets, UI states, and model-cache metadata without useful gameplay value.

After this change, every custom model is just a normal selectable model. Server startup and `/ysm reload` scan only `config/ysmu/custom`. The player model screen no longer hides or locks models by authorization. The model sync packet format no longer stores auth metadata, and the cache package version is bumped so stale cache files are naturally ignored and regenerated.

## Progress

- [x] (2026-05-01 20:54 +08:00) Read `PLANS.md` and surveyed current auth references across source, resources, and documentation.
- [x] (2026-05-01 20:54 +08:00) Decided the removal should delete the auth model package flag and bump the encrypted model version, rather than leaving a dead always-false field.
- [x] (2026-05-01 21:00 +08:00) Removed auth directories, model metadata, and encrypted package auth fields from server/client model loading.
- [x] (2026-05-01 21:00 +08:00) Removed auth EEP registration, copying, validation, and clientbound auth sync packet.
- [x] (2026-05-01 21:00 +08:00) Simplified player model GUI buttons so there is no locked model state or authorization category.
- [x] (2026-05-01 21:00 +08:00) Removed localization and repository guidance that described active auth model behavior.
- [x] (2026-05-01 21:01 +08:00) Ran static source searches and `git diff --check`; Gradle build remains a user-run validation step because repository instructions say build/test validation is user-run in this sandbox.

## Surprises & Discoveries

- Observation: The auth packet id is clientbound id `6`, but packet ids are documented as wire protocol values that should not be renumbered.
  Evidence: `src/main/java/com/fox/ysmu/network/NetworkHandler.java` says "Add new ids; do not renumber existing ones" and registers `SyncAuthModels` with `CLIENTBOUND_SYNC_AUTH_MODELS = 6`.

- Observation: Auth state is serialized inside the encrypted model payload, not only in side metadata.
  Evidence: `src/main/java/com/fox/ysmu/data/EncryptTools.java` calls `writeBoolean(tmp, data.isAuth())` after writing the model id, and `decryptModel` reads it back before map metadata.

- Observation: After implementation, sensitive auth-code searches outside this plan and the previous historical cleanup plan no longer find live references.
  Evidence: `rg -n "config/ysmu/auth|ExtendedAuthModels|SyncAuthModels|AUTH_MODELS|ModelData\\.isAuth|ServerModelInfo\\.needAuth|need_auth|授权" . -g "!AUTH_REMOVAL_EXECPLAN.md" -g "!CODE_CLEANUP_EXECPLAN.md"` exited with no matches.

- Observation: `EncryptTools.VERSION` gates both encrypted model packages and the `PASSWORD` file, so a model package version bump also makes old password files invalid.
  Evidence: `ServerModelManager.initPassword()` read `PASSWORD` with `EncryptTools.readPassword(...)`, and `readPassword` checked the same `VERSION` constant used by `decryptModel`.

## Decision Log

- Decision: Do not scan or create `config/ysmu/auth` after this change. Existing files in that directory are not deleted automatically; users should move any models they still want into `config/ysmu/custom`.
  Rationale: The request is to remove authorization behavior, and silently merging an old auth directory into the normal model set would keep the old feature shape alive. Avoiding deletion is safer for user files.
  Date/Author: 2026-05-01 / Codex

- Decision: Remove the auth boolean from `ModelData`, `ServerModelInfo`, and the encrypted model payload, and bump `EncryptTools.VERSION` from `1` to `2`.
  Rationale: Leaving an always-false auth field would preserve dead code and invite future confusion. A version bump makes old client/server cache files fail version checks and be regenerated through the normal sync flow.
  Date/Author: 2026-05-01 / Codex

- Decision: Remove the `SyncAuthModels` registration while leaving later packet ids unchanged.
  Rationale: Packet ids are protocol-visible. Leaving a hole at id `6` avoids accidental incompatibility from renumbering existing packets.
  Date/Author: 2026-05-01 / Codex

- Decision: Make `EncryptTools.readPassword` report validity and rewrite `PASSWORD` when the existing file has the old version.
  Rationale: Without rewriting, a version-1 `PASSWORD` file would remain on disk after the version bump, and clients would receive a password blob that `decryptPassword` rejects.
  Date/Author: 2026-05-01 / Codex

## Outcomes & Retrospective

The implementation has removed the active authorization path from source and resources. `git diff --check` passes, and source/resource searches only report unrelated model author metadata for broad `auth` patterns. Gradle validation remains user-run per repository instructions.

## Context and Orientation

This repository is a Minecraft Forge 1.7.10 mod. Forge 1.7.10 uses `IExtendedEntityProperties`, abbreviated as EEP, to attach custom saved data to players. The authorization feature currently uses an EEP named `ExtendedAuthModels` to store which locked models a player may use.

The server model loader is `src/main/java/com/fox/ysmu/model/ServerModelManager.java`. It creates runtime folders under `config/ysmu`, copies built-in models into `config/ysmu/custom`, scans model files, writes encrypted cache files under `config/ysmu/cache/server`, and asks clients to sync missing files. The current code also creates and scans `config/ysmu/auth`.

Folder models are parsed by `src/main/java/com/fox/ysmu/model/format/FolderFormat.java`; `.ysm` archive models are parsed by `src/main/java/com/fox/ysmu/model/format/YsmFormat.java`. Both currently detect whether the root path is `AUTH`, then pass `true` as an auth flag into `ModelData`.

`src/main/java/com/fox/ysmu/data/ModelData.java` is the in-memory model package. `src/main/java/com/fox/ysmu/model/format/ServerModelInfo.java` is metadata stored in `ServerModelManager.CACHE_NAME_INFO`. `src/main/java/com/fox/ysmu/data/EncryptTools.java` serializes `ModelData` into the binary encrypted model package and deserializes it on clients.

Player sync is split across `src/main/java/com/fox/ysmu/event/CommonEventHandler.java`, `src/main/java/com/fox/ysmu/network/NetworkHandler.java`, and message classes under `src/main/java/com/fox/ysmu/network/message`. The auth-specific message is `SyncAuthModels`, and the server selection gate is in `SetModelAndTexture`.

The client player model selection UI is `src/main/java/com/fox/ysmu/client/gui/PlayerModelScreen.java`, and individual model tiles are `src/main/java/com/fox/ysmu/client/gui/button/ModelButton.java`. They use `ClientModelManager.AUTH_MODELS` and `ExtendedAuthModels` to hide or lock models. After this change, the screen should show all normal models and only keep the existing star filter.

## Plan of Work

First, update the model package layer. In `ServerModelManager`, remove the `AUTH` path and `AUTH_MODELS` set, stop creating the auth directory, and stop scanning it. In `FolderFormat` and `YsmFormat`, remove the `rootPath.equals(AUTH)` branches and make cache helpers construct `ModelData` without an auth flag. In `ModelData` and `ServerModelInfo`, delete auth fields and accessors. In `EncryptTools`, bump `VERSION` to `0x00_00_00_02`, remove auth boolean writes and reads from model payloads, and delete the now-unused boolean helpers if they become unused. Update `ClientModelManager` to stop tracking `AUTH_MODELS`, stop recording auth state, and call `FolderFormat.getModelData(ServerModelManager.CUSTOM, "default")`.

Second, remove player authorization state and packets. Delete `src/main/java/com/fox/ysmu/eep/ExtendedAuthModels.java` and `src/main/java/com/fox/ysmu/network/message/SyncAuthModels.java`. In `CommonEventHandler`, stop registering/copying auth EEPs and remove join-world auth sync/reset helpers. In `CommonProxy` and `ClientProxy`, remove `handleAuthModels` and its imports. In `NetworkHandler`, remove clientbound id constant `CLIENTBOUND_SYNC_AUTH_MODELS` and its registration, while leaving later packet ids unchanged. In `SetModelAndTexture`, set the selected model and texture directly when `ExtendedModelInfo` exists.

Third, clean client-facing UI. In `PlayerModelScreen`, remove the `AUTH` category, the dead action case for button id `4`, and all `ExtendedAuthModels` checks. Add `ModelButton` instances directly with the normal model info. In `ModelButton`, remove the `needAuth` constructor parameter, field, disabled-click guard, dark overlay, and hover suppression. The button should always select the model.

Fourth, remove stale auth text and guidance. Remove auth-related localization keys from `src/main/resources/assets/ysmu/lang/zh_CN.lang` and `src/main/resources/assets/ysmu/lang/en_US.lang`, but keep `gui.yes_steve_model.model.authors` because it describes model creators, not authorization. Update `AGENTS.md` so future maintainers see that `config/ysmu/custom` is the only runtime model input folder and that EEP additions should follow `ExtendedModelInfo` or `ExtendedStarModels`.

Finally, search for remaining auth references with a source/resource `rg` command. Expected remaining matches are unrelated English words like `authors`, `authorList`, and copyright `AUTHORS`. Run `git diff --check`. Because `AGENTS.md` says Gradle builds and tests are user-run in this sandbox, ask the user to run `.\gradlew.bat build` after the source diff is clean.

## Concrete Steps

Work from repository root `D:\Code\YesSteveModel-Unofficial`.

Use these search commands during implementation:

    rg -n "Auth|auth|need_auth|needAuth|SyncAuth|ExtendedAuth|AUTH_MODELS|auth_model" src/main/java src/main/resources AGENTS.md README.md
    rg -n "new ModelData|getModelData\\(|new ServerModelInfo|isAuth|isNeedAuth" src/main/java/com/fox/ysmu

After edits, run:

    git diff --check

The project-specific Gradle validation is:

    .\gradlew.bat build

Do not repeatedly run Gradle from Codex in this sandbox; ask the user to run it and paste the result, matching the repository guidance.

## Validation and Acceptance

Static acceptance is that there are no source references to `ExtendedAuthModels`, `SyncAuthModels`, `AUTH_MODELS`, `ServerModelManager.AUTH`, `ModelData.isAuth`, `ServerModelInfo.isNeedAuth`, or `need_auth`. The only remaining `auth`-like matches should be unrelated terms such as model `authors`, Gradle/Maven authentication comments, or copyright text.

Build acceptance is that the user runs `.\gradlew.bat build` from `D:\Code\YesSteveModel-Unofficial` and reports `BUILD SUCCESSFUL`.

Runtime acceptance is that after starting a server or integrated client and running `/ysm reload`, models placed under `config/ysmu/custom` sync to clients and appear in the player model screen as selectable tiles. No locked tile overlay should appear. A model that used to be in `config/ysmu/auth` should not load until it is moved to `config/ysmu/custom`.

## Idempotence and Recovery

These edits are safe to repeat because they remove code paths and leave user model files untouched. If old cache files remain under `config/ysmu/cache/client` or `config/ysmu/cache/server`, the version bump makes old version-1 model packages fail the version check and the server will regenerate version-2 packages on reload. If a user still needs a model from the old `auth` directory, move that folder or `.ysm` file manually into `config/ysmu/custom` and run `/ysm reload`.

If compilation fails because a deleted class is still imported, search for the class name and remove the stale import or logic. If model sync fails because a version-1 cache file is reused unexpectedly, delete the runtime `config/ysmu/cache` directory and run `/ysm reload`; this removes only generated cache files, not original models.

## Artifacts and Notes

Initial auth survey found these implementation anchors:

    src/main/java/com/fox/ysmu/eep/ExtendedAuthModels.java
    src/main/java/com/fox/ysmu/network/message/SyncAuthModels.java
    src/main/java/com/fox/ysmu/model/ServerModelManager.java AUTH and AUTH_MODELS
    src/main/java/com/fox/ysmu/data/EncryptTools.java writeBoolean/readBoolean for isAuth
    src/main/java/com/fox/ysmu/client/gui/PlayerModelScreen.java Category.AUTH and locked ModelButton state

The packet id hole at `6` is intentional after removal; do not renumber ids `8`, `10`, `11`, or `12`.

## Interfaces and Dependencies

At the end of this plan, `ModelData` must expose this constructor and no auth accessor:

    public ModelData(String modelId, Type type, Map<String, byte[]> geo, Map<String, byte[]> texture, Map<String, byte[]> animation)

`ServerModelInfo` must expose this constructor and no auth accessor:

    public ServerModelInfo(Set<String> textures, Type type)

`FolderFormat` must expose this default-model helper:

    public static ModelData getModelData(Path rootPath, String modelId) throws IOException

`YsmFormat` can keep its model data helper private, but it must no longer accept or pass an auth flag.

The network channel must no longer register `SyncAuthModels`. Existing non-auth messages retain their current ids.

## Change Notes

2026-05-01 / Codex: Created the initial plan after surveying current authorization references. The plan removes the auth feature instead of bypassing it, and records the cache version bump required by the encrypted payload change.

2026-05-01 / Codex: Updated progress after removing the server/client auth model code paths, the auth EEP and sync packet, GUI lock state, and stale localization keys. Sensitive auth-code search is now clean outside this plan and the previous historical cleanup plan. Added the password-file regeneration fix required by the package version bump.
