# Remove authorized model support

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document is maintained according to `.agent/PLANS.md` in the repository root. It is self-contained so a future agent or developer can continue the work after context compaction.

## Purpose / Big Picture

OpenYSM currently has an authorized model feature. In this repository, "authorized model" means a model placed under the server runtime folder `config/yes_steve_model/auth`, tracked in a per-player Forge capability, synchronized to clients with a dedicated packet, shown as a separate GUI category, and blocked unless the player has that model in their authorized set. The user no longer needs this feature.

After this change, model selection should treat all loaded server models as ordinary selectable models. There should be no authorized-model capability, no authorized-model sync packet, no "Auth Models" GUI tab, no per-model lock overlay, and no server-side validation that rejects a model because it came from an authorization-only list. The existing star/favorite model feature must remain intact.

## Progress

- [x] (2026-05-09 Asia/Shanghai) Read `.agent/PLANS.md` and created this ExecPlan for the authorized model removal.
- [x] (2026-05-09 Asia/Shanghai) Identified main roots: `AuthModelsCapability`, `AuthModelsCapabilityProvider`, `S2CSyncAuthModelsPacket`, `CapabilityEvent`, `CommonEvent`, `NetworkHandler`, `C2SVersionCheckPacket`, `C2SRequestSwitchModelPacket`, `ServerModelManager`, `ModelLoadResult`, `ServerModelData`, `ClientModelManager`, `ModelAssemblyFactory`, `ModelDisplayAssets`, `PlayerModelScreen`, `ModelButton`, and language keys under `assets/yes_steve_model/lang`.
- [x] (2026-05-09 Asia/Shanghai) Removed authorized-model capability classes and all capability registration, attach, clone, join, and sync code.
- [x] (2026-05-09 Asia/Shanghai) Removed the authorized-model network packet and simplified version-check and model-switch packet handling.
- [x] (2026-05-09 Asia/Shanghai) Removed server-side authorized folder scanning, authorized model id tracking, and authorized checks from model validation.
- [x] (2026-05-09 Asia/Shanghai) Removed client-side authorized flags from model sync, model assembly, model display assets, and GUI model buttons.
- [x] (2026-05-09 Asia/Shanghai) Removed authorized-model language keys and ran targeted searches for remaining active references.
- [x] (2026-05-09 Asia/Shanghai) Prepared the final validation request for the user to run repository-appropriate Gradle commands, because AGENTS.md says coding agents should not run build commands for code changes.

## Surprises & Discoveries

- Observation: The authorized GUI tab does not show only authorized models.
  Evidence: `PlayerModelScreen.refreshModelList()` adds every non-authorized model plus every authorized model the capability contains, and `ModelButton` receives an `isAuthLocked` flag to block unauthorized selections.
- Observation: The model sync index includes an authorized-model flag per model.
  Evidence: `ServerModelManager.sendPacket03()` writes `model.isAuth() ? 1 : 0`, and `ClientModelManager.handlePacket03()` reads it into `ServerModelContext.isAuth`.
- Observation: The `auth` runtime folder is created and scanned as a model source.
  Evidence: `ServerModelManager.reloadPacks()` creates `AUTH`; `loadModelDefinitions()` scans packs and models from `AUTH` separately from `BUILT` and `CUSTOM`.
- Observation: Some remaining strings that contain "auth" are ordinary author metadata, not authorization code.
  Evidence: broader searches find `authors`, `AuthorInfo`, and model metadata localization keys; the targeted search for `AuthModels`, `S2CSyncAuthModels`, `AUTH_MODELS`, `auth_model`, `auth_models`, `need_auth`, `own_models`, `isAuthModel`, `setAuthModel`, `getAuthModels`, `requestPlayerAuth`, and standalone `isAuth` has no active source or language hits.
- Observation: Bulk language-file edits can introduce a UTF-8 BOM on Windows PowerShell.
  Evidence: after the key removal, `en_us.json` started with bytes `EF BB BF`; the language JSON files were rewritten as UTF-8 without BOM and parsed successfully.

## Decision Log

- Decision: Remove the authorization-only runtime folder from active loading and pack scanning, but do not delete existing user files on disk.
  Rationale: The user asked to remove the feature. Stop using `config/yes_steve_model/auth` in code, while respecting repository guidance not to delete user runtime data.
  Date/Author: 2026-05-09 / Codex
- Decision: Preserve the star/favorite model capability and GUI category.
  Rationale: Star models are a separate user-facing favorite feature and are not part of authorization or access control.
  Date/Author: 2026-05-09 / Codex
- Decision: Bump the custom network protocol version when removing the authorized flag from the encrypted model sync index.
  Rationale: The server-to-client model sync payload layout changes when one integer is removed from each model entry. Repository guidance says to change `NetworkHandler.VERSION` when packet formats become intentionally incompatible.
  Date/Author: 2026-05-09 / Codex

## Outcomes & Retrospective

Active removal is complete. The source tree no longer contains the authorized-model capability, sync packet, server authorization folder scanning, per-model authorization bit, GUI authorization tab, lock overlay, or authorization language keys. The custom network protocol version is now `2.6.2` because the encrypted model sync index no longer includes the authorization flag. The agent parsed the language JSON files locally to confirm the mechanical key removal kept valid JSON and rewrote them as UTF-8 without BOM, but did not run Gradle build commands because repository instructions require the user to run build commands for code changes.

## Context and Orientation

This is a Minecraft Forge 1.20.1 mod. The mod id is `yes_steve_model`, and the main Java package is `com.elfmcys.yesstevemodel`.

Forge capabilities are attachable data containers used by this mod to store player model state and star/favorite model data. The authorized model capability lives in `src/main/java/com/elfmcys/yesstevemodel/capability/AuthModelsCapability.java` and `src/main/java/com/elfmcys/yesstevemodel/capability/AuthModelsCapabilityProvider.java`. It is registered in `CommonEvent.onRegisterCapabilities()` and attached/copied/synchronized in `CapabilityEvent`.

Network packets are registered in `src/main/java/com/elfmcys/yesstevemodel/network/NetworkHandler.java`. The authorized model sync packet is `S2CSyncAuthModelsPacket`, registered with message id `6`. `C2SVersionCheckPacket` currently sends authorized models to the client after version negotiation, and `C2SRequestSwitchModelPacket` currently rejects switching to an authorized model when the sender does not have the capability entry.

Server model loading is centralized in `src/main/java/com/elfmcys/yesstevemodel/model/ServerModelManager.java`. It has runtime folders under `config/yes_steve_model`: `built` for extracted built-in models, `custom` for user models, `auth` for authorization-gated models, `export` for exports, and `cache` for sync cache files. The target behavior is to keep `built`, `custom`, `export`, and `cache`, but stop creating, scanning, or assigning special meaning to `auth`.

The encrypted server-to-client model sync index is written in `ServerModelManager.sendPacket03()` and read in `ClientModelManager.handlePacket03()`. Each model entry currently contains hashes, model id, authorized flag, custom-skin flag, and format version. Removing the authorized flag requires changing both writer and reader consistently and bumping `NetworkHandler.VERSION`.

The player model selection screen is `src/main/java/com/elfmcys/yesstevemodel/client/gui/PlayerModelScreen.java`. It has categories `ALL`, `AUTH`, and `STAR`. The target behavior is to keep `ALL` and `STAR`, remove `AUTH`, and build every `ModelButton` as selectable. `ModelButton` currently uses a misleading field named `isStarred` to mean the model is locked by authorization; this should be removed or renamed away by deleting the lock behavior.

## Plan of Work

First, remove the capability layer. Delete `AuthModelsCapability.java` and `AuthModelsCapabilityProvider.java`. In `CommonEvent`, stop registering `AuthModelsCapability`. In `CapabilityEvent`, remove the `AUTH_MODELS_CAP` resource key, the player attach block for authorized models, clone-copy logic for authorized models, join-level synchronization of authorized models, and the private `getAuthModelsCap()` helper.

Second, remove the explicit authorized sync packet. Delete `S2CSyncAuthModelsPacket.java`. In `NetworkHandler.init()`, remove message id `6`. In `C2SVersionCheckPacket`, remove the authorized sync send and keep model validation, mandatory reset, animation stop, star model sync, and model sync request. In `C2SRequestSwitchModelPacket`, validate only that the requested model exists and that the requested texture belongs to that model.

Third, simplify server model loading and validation. Remove the `AUTH` path constant, the `AUTH_MODELS` set, and `getAuthModels()`. Stop creating `config/yes_steve_model/auth`. Stop scanning `AUTH` for packs and models. Remove `authIds` collection from `loadModelDefinitions()` and remove the `isAuth` parameter from `scanDirectoryModels()`, `processAndCacheModel()`, and `mapToDataClass()`. Update `ModelLoadResult` so it only carries success, error message, and model definitions. Update `ServerModelData` so it no longer stores or exposes `isAuth`. In `validatePlayerModel()`, remove authorized capability cleanup and reject only missing models or invalid textures.

Fourth, remove authorized state from client model loading and GUI metadata. In `ClientModelManager`, remove `ServerModelContext.isAuth`, stop reading the authorized flag from `handlePacket03()`, and remove the `isAuth` parameter from `parseAndLoadModel()`, `onModelDataReceived()`, and `processModelData()`. In `ModelAssemblyFactory`, remove the `isAuth` parameter from `buildAssembly()` and `buildTextureRegistry()`. In `ModelDisplayAssets`, remove `isAuthModel()` and `setAuthModel()`. In `PlayerModelScreen`, remove authorized imports, the `AUTH` category, the auth tab button, and auth capability reads. In `ModelButton`, remove the lock parameter and lock-only disabled overlay/click guards while leaving star/favorite rendering intact.

Fifth, remove user-facing language keys that only describe authorized models: `gui.yes_steve_model.auth_models`, `message.yes_steve_model.model.set.need_auth`, `message.yes_steve_model.model.need_auth`, and `commands.yes_steve_model.auth_model.*`. Then run targeted `rg` searches for authorized-model class names, packet names, language keys, and server path constants. Ignore ordinary author metadata such as `authors`.

## Concrete Steps

Work from `D:\Code\OpenYSM`.

Use targeted searches instead of broad `auth` searches, because many ordinary model metadata fields use "author":

    rg -n "AuthModels|S2CSyncAuthModels|AUTH_MODELS|auth_model|auth_models|need_auth|own_models|isAuthModel|setAuthModel|requestPlayerAuth|getAuthModels" src/main/java src/main/resources

After removing the network sync index field, update both ends together:

    ServerModelManager.sendPacket03(): remove the `model.isAuth()` write.
    ClientModelManager.handlePacket03(): remove the matching read and constructor argument.

Do not run Gradle build commands as the agent. At the end, tell the user to run:

    .\gradlew.bat compileJava

If compilation succeeds and the user wants packaging validation, tell them to run:

    .\gradlew.bat build

## Validation and Acceptance

Acceptance is reached when the active Java source has no references to `AuthModelsCapability`, `AuthModelsCapabilityProvider`, `S2CSyncAuthModelsPacket`, `AUTH_MODELS`, `own_models`, `auth_model`, `auth_models`, `need_auth`, `isAuthModel`, `setAuthModel`, or `getAuthModels`. The player model GUI should still show all models and the star/favorite tab, but it should no longer show an authorized-model tab or any authorization lock overlay. The server should load built-in and custom models, validate selected textures, and synchronize model data to clients without carrying any authorized-model bit.

The user should validate by running `.\gradlew.bat compileJava` from `D:\Code\OpenYSM`. Expected success is a completed Java compile with no missing symbol errors for deleted authorized-model classes or packets. Because the network sync payload layout changes, a client and server from different protocol versions should not be treated as compatible.

## Idempotence and Recovery

The edits are code-level removals and can be repeated safely if the working tree is checked between passes. Do not delete files under the runtime `run/` directory or any user's existing `config/yes_steve_model/auth` data; the feature removal simply stops using that folder. If a deleted symbol turns out to be needed for the independent star/favorite feature, restore only the star-related code and record the reason in this plan.

## Artifacts and Notes

Initial search evidence:

    Auth capability roots: capability/AuthModelsCapability.java and capability/AuthModelsCapabilityProvider.java.
    Network root: network/message/S2CSyncAuthModelsPacket.java registered as message id 6.
    Server roots: ServerModelManager.AUTH, AUTH_MODELS, and per-model isAuth flags in ServerModelData.
    Client roots: PlayerModelScreen AUTH category, ModelDisplayAssets.isAuthModel, and ModelButton lock handling.
