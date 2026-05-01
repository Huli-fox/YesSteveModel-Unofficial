# Clean Up the YesSteveModel 1.7.10 Port Without Changing Core Behavior

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document must be maintained in accordance with `PLANS.md` at the repository root. It is self-contained: a contributor should be able to start from this file and the current working tree, without relying on prior chat history.

## Purpose / Big Picture

This project is a Minecraft Java Edition Forge 1.7.10 mod that replaces player models with custom Geckolib-style models. The main feature already works, but the port mixes high-version concepts, 1.7.10 compatibility code, networking, encryption, rendering, and animation state in ways that make future fixes risky. The purpose of this work is to make the code easier to read and safer to modify while preserving the existing user-visible behavior: a player can start the game, load default and custom models, use `/ysm reload`, switch model and texture in the GUI, see first-person hands, and synchronize models in multiplayer.

The work is successful when the code is organized into clearer responsibilities, the most misleading hard-coded return values are either fixed or explicitly documented, and the mod can still be built and smoke-tested. Because this is primarily internal cleanup, the observable proof is that the same workflows still work after each milestone, and the documented authorization-model behavior is no longer contradicted by the code.

## Progress

- [x] (2026-05-01 14:36 +08:00) Read `PLANS.md` and created this initial ExecPlan for the cleanup work.
- [x] (2026-05-01 14:43 +08:00) Established the current build baseline. `.\gradlew.bat build` needs access outside the workspace for the Gradle wrapper cache; after escalation it reached Gradle but failed because the Gradle process was running on Java 8 while `shadowJar` uses a dependency compiled for Java 11. A follow-up attempt to run Gradle with `E:\Java\Java21` was not authorized, so runtime smoke tests were not started from this baseline.
- [x] (2026-05-01 14:50 +08:00) Clarified and fixed the authorization-model fields and methods in source: `ModelData.isAuth()` now returns stored data, `ServerModelInfo.needAuth` now reflects the constructor argument, `ExtendedAuthModels.containModel()` checks the saved set, folder models under `auth` are cached as auth models, unauthorized `SetModelAndTexture` requests reset to the default model, and the model GUI marks unauthorized auth models as locked.
- [x] (2026-05-01 14:50 +08:00) User reported `BUILD SUCCESSFUL` for the authorization-model milestone.
- [x] (2026-05-01 14:52 +08:00) Refactored model loading and cache creation responsibilities. `ServerModelManager.reloadPacks()` now delegates to named private steps, and `FolderFormat` plus `YsmFormat` now share `ModelCacheWriter` for encryption, MD5 naming, and writing cache files under `cache/server`.
- [x] (2026-05-01 14:52 +08:00) User reported `BUILD SUCCESSFUL` for the model-loading/cache-creation milestone.
- [x] (2026-05-01 14:56 +08:00) Organized network packet ids and thread-boundary documentation. `NetworkHandler` now uses named ids grouped by serverbound, clientbound, and NPC compatibility messages, and model sync packets now include short comments for password ordering, large file sending, cached model decryption, client-thread registration, and delayed remote-player EEP application.
- [x] (2026-05-01 14:56 +08:00) User reported `BUILD SUCCESSFUL` for the network packet organization milestone.
- [x] (2026-05-01 15:00 +08:00) Split common player event logic and made the motion `DataWatcher` update readable. `CommonEventHandler` now delegates EEP registration, clone copying, tracking sync, join-world auth/star/model sync, dirty model broadcasts, and tracking-point construction to named helpers. `updateData(EntityPlayer)` now computes a fresh byte from current ground/flying state and updates watcher id `28` at most once per tick.
- [x] (2026-05-01 15:00 +08:00) User reported `BUILD SUCCESSFUL` for the common event cleanup milestone.
- [x] (2026-05-01 15:03 +08:00) Split client model registration and first-person hand rendering into smaller helper methods. `ClientModelManager.registerAll()` now reads as deriving a model id, recording auth state, registering geometry, registering animations, and registering textures. `ClientEventHandler.onRenderHand()` now delegates custom-hand eligibility, arm model lookup, cached animatable preparation, first-person matrix setup, main-hand transforms, right-arm drawing, and OpenGL state restoration to named helpers.
- [x] (2026-05-01 15:03 +08:00) User reported `BUILD SUCCESSFUL` for the client model/rendering cleanup milestone.
- [x] (2026-05-01 15:08 +08:00) Organized animation registration, Molang variable setup, and compatibility boundaries. `AnimationRegister` now groups animation state registration by high-priority states, riding/flying states, damage/jump/sneak states, movement states, and idle fallback; Molang variable registration and value assignment are split into named helpers; the duplicate `query.is_on_ground` assignment was removed. `AnimationManager` now centralizes repeated conditional animation lookups for hold, swing, use, and armor controllers. Compat scanning found only the expected Backhand optional render marker outside `BackhandCompat`, and that marker is now documented.
- [x] (2026-05-01 15:08 +08:00) User reported `BUILD SUCCESSFUL` for the animation/compat cleanup milestone.
- [x] (2026-05-01 15:09 +08:00) Updated `AGENTS.md` to reflect the now-enforced authorization model behavior, the fixed motion watcher byte update, and the documented Backhand optional marker exception.
- [x] (2026-05-01 15:09 +08:00) Ran static diff hygiene check with `git diff --check`; it produced no output.
- [x] (2026-05-01 15:28 +08:00) User reported `OK` for the final runtime smoke-test request. Treat this as the requested `runClient` smoke workflows passing; no separate multiplayer transcript was provided.

## Surprises & Discoveries

- Observation: The repository currently has no `src/test` directory, even though JUnit dependencies are configured in `dependencies.gradle`.
  Evidence: Running `rg --files src\test` failed with “系统找不到指定的文件”.

- Observation: Several methods currently contradict the apparent authorization-model design.
  Evidence: `src/main/java/com/fox/ysmu/data/ModelData.java` has `isAuth()` returning `false`; `src/main/java/com/fox/ysmu/model/format/ServerModelInfo.java` accepts `needAuth` but assigns `this.needAuth = false`; `src/main/java/com/fox/ysmu/eep/ExtendedAuthModels.java` has `containModel(ResourceLocation modelId)` returning `true`.

- Observation: Remote-player ground and flying state is implemented through a 1.7.10 `DataWatcher` byte at id `28`.
  Evidence: `src/main/java/com/fox/ysmu/event/CommonEventHandler.java` declares `MOTION_DATAWATCHER_ID = 28`, `ON_GROUND = 0x01`, and `IS_FLYING = 0x02`.

- Observation: The baseline build is blocked by the Java version used for the Gradle process, not by source compilation.
  Evidence: `.\gradlew.bat build` first failed in the sandbox with `FileNotFoundException: E:\JetBrains\cache\.gradle\wrapper\...\gradle-8.13-bin.zip.lck (拒绝访问。)`. After running outside the sandbox, it failed at `:shadowJar` with `org/vafer/jdependency/Clazzpath has been compiled by a more recent version of the Java Runtime (class file version 55.0), this version of the Java Runtime only recognizes class file versions up to 52.0`.

- Observation: The previous motion watcher update could lose a simultaneous state change because the second branch was based on `oldData`, not the byte written by the first branch.
  Evidence: `CommonEventHandler.updateData()` previously called `updateObject()` separately for `ON_GROUND` and `IS_FLYING`. If both changed in the same server tick, the second computed byte reused `oldData`. The new code computes both flags into `newData` first and calls `updateObject()` once if needed.

- Observation: `AnimationRegister.setParserValue()` assigned `query.is_on_ground` twice with the same lambda.
  Evidence: Before the animation cleanup, `src/main/java/com/fox/ysmu/client/animation/AnimationRegister.java` contained two adjacent `parser.setValue("query.is_on_ground", () -> MolangUtils.booleanToFloat(isPlayerOnGround(player)))` calls. After cleanup there is one registration and one runtime value assignment.

- Observation: The only Backhand type reference outside `compat/BackhandCompat.java` is the optional render opt-out marker on `CustomPlayerItemInHandLayer`; Et Futurum classes are only referenced inside `compat/EtfuturumCompat.java`.
  Evidence: `rg -n 'xonin\.backhand|ganymedes01\.etfuturum|IOffhandRenderOptOut' src\main\java\com\fox\ysmu` reports Backhand API calls in `BackhandCompat`, Et Futurum API calls in `EtfuturumCompat`, and `xonin.backhand.compat.IOffhandRenderOptOut` in `client/renderer/layer/CustomPlayerItemInHandLayer.java`.

## Decision Log

- Decision: Treat this cleanup as a sequence of small behavior-preserving milestones, except for the authorization-model hard-coded return values, which may need a focused behavior correction.
  Rationale: The project couples model files, network synchronization, client rendering, and animation state. Changing several of those at once would make regressions hard to diagnose.
  Date/Author: 2026-05-01 / Codex

- Decision: Do not begin by formatting or rewriting the vendored or adapted libraries under `software/bernie/geckolib3`, `com/eliotlash/mclib`, or `net/geckominecraft`.
  Rationale: Those packages are compatibility code for Forge 1.7.10 and may intentionally differ from upstream high-version code. Wide formatting or mechanical rewrites would create noisy diffs without directly improving mod behavior.
  Date/Author: 2026-05-01 / Codex

- Decision: Use `.\gradlew.bat build` as the main automated verification command on Windows, and use `.\gradlew.bat runClient` plus targeted manual checks for rendering and multiplayer-sensitive behavior.
  Rationale: This is a Forge 1.7.10 mod with little or no automated test coverage. Rendering, model selection, and model synchronization need runtime observation.
  Date/Author: 2026-05-01 / Codex

- Decision: Continue cleanup after recording the baseline build blocker, but do not claim build verification until Gradle can be run on a newer JDK.
  Rationale: The failure happens before useful project compilation evidence is available and a Java 21 retry was not authorized. The plan already allows continuing with recorded baseline failures as long as later outcomes state the limitation.
  Date/Author: 2026-05-01 / Codex

- Decision: Codex will not run build or test commands after the user clarified that sandbox effects make local validation unreliable; instead, Codex will stop at validation points and ask the user to run the exact command.
  Rationale: Build and runtime smoke-test results need to reflect the user's actual environment, not the restricted tool sandbox.
  Date/Author: 2026-05-01 / Codex

- Decision: Preserve `MOTION_DATAWATCHER_ID = 28` and only change how its byte value is computed.
  Rationale: Clients and servers must agree on the watcher id. The cleanup goal is to remove confusing update semantics, not to migrate protocol state or investigate mod conflicts.
  Date/Author: 2026-05-01 / Codex

- Decision: Keep `IOffhandRenderOptOut` implemented directly on `CustomPlayerItemInHandLayer`, but document why this direct Backhand type is allowed.
  Rationale: The optional interface is a marker used by Backhand's renderer to avoid duplicate layer rendering. Moving it behind `BackhandCompat` would not work because Java interface implementation is part of the class declaration. Actual Backhand behavior calls remain isolated in `BackhandCompat`.
  Date/Author: 2026-05-01 / Codex

## Outcomes & Retrospective

Baseline outcome, 2026-05-01 14:43 +08:00: the initial build could not complete because Gradle was launched with Java 8 and `shadowJar` needs a dependency compiled for Java 11. The machine has `E:\Java\Java21`, but the attempt to run the wrapper with that `JAVA_HOME` was not authorized. Runtime smoke tests were not started because there is no successful baseline build in this environment yet. This is an environment blocker rather than a source-code result.

Authorization milestone outcome, 2026-05-01 14:50 +08:00: source changes now enforce the existing authorization design rather than bypassing it. Auth model metadata is preserved through server cache creation and encrypted model data, the client can mark locked auth models, and the server resets an unauthorized selection to `ysmu:default` with texture `ysmu:default/default.png`. The user ran the build and reported `BUILD SUCCESSFUL`, so this milestone is accepted.

Model loading/cache milestone outcome, 2026-05-01 14:52 +08:00: source code now separates `reloadPacks()` into named steps and centralizes encrypted cache file writing in `src/main/java/com/fox/ysmu/model/format/ModelCacheWriter.java`. The supported inputs and cache format are intended to remain unchanged. The user ran the build and reported `BUILD SUCCESSFUL`, so this milestone is accepted.

Network milestone outcome, 2026-05-01 14:56 +08:00: packet ids are now named constants instead of scattered numeric literals, registration is split by direction, and the model-sync message comments document the current ordering/thread constraints without changing serialization. The user ran the build and reported `BUILD SUCCESSFUL`, so this milestone is accepted.

Common event milestone outcome, 2026-05-01 15:00 +08:00: source code now separates common player event responsibilities into named helpers and fixes the readable computation of the synchronized motion byte while keeping watcher id `28` unchanged. The user ran the build and reported `BUILD SUCCESSFUL`, so this milestone is accepted.

Client model/rendering milestone outcome, 2026-05-01 15:03 +08:00: source code now makes client model registration steps explicit and splits first-person custom-hand rendering into named helpers without changing the intended OpenGL operation order. Geometry, animation, and default-model failures now include resource context in the log before printing the stack trace. The user ran the build and reported `BUILD SUCCESSFUL`, so this milestone is accepted.

Animation/compat milestone outcome, 2026-05-01 15:08 +08:00: source code now organizes animation state registration and Molang value setup into smaller named helpers, removes a duplicate `query.is_on_ground` assignment, and centralizes repeated conditional animation lookup code in `AnimationManager`. Optional mod API boundaries remain intact; the one direct Backhand optional marker is documented. The user ran the build and reported `BUILD SUCCESSFUL`, so this milestone is accepted.

Final source cleanup outcome, 2026-05-01 15:09 +08:00: all planned source cleanup milestones are implemented. The user reported `BUILD SUCCESSFUL` after each source milestone from authorization through animation/compat cleanup. `AGENTS.md` now matches the changed authorization and motion watcher behavior. Codex ran `git diff --check` and it produced no output.

Final validation outcome, 2026-05-01 15:28 +08:00: the user reported `OK` after the final runtime smoke-test request. This plan treats the requested client smoke checks as passing: default model loading, model GUI opening, model/texture switching, first-person empty-hand arm rendering, and integrated-world `/ysm reload` did not reveal a reported blocker. No separate multiplayer smoke-test transcript was provided, so multiplayer synchronization remains residual risk beyond the repeated successful builds and client smoke report.

## Context and Orientation

The repository root is `d:\Code\YesSteveModel-Unofficial`. The mod id is `ysmu`, and the Forge entry class is `src/main/java/com/fox/ysmu/ysmu.java`. A Forge mod is a Java archive loaded by Minecraft Forge; in this project it targets Minecraft `1.7.10` and Forge `10.13.4.1614`.

The build uses the GTNH RetroFuturaGradle convention plugin. The important build files are `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `dependencies.gradle`, and `repositories.gradle`. On Windows, use the Gradle wrapper script `.\gradlew.bat` from the repository root. The wrapper uses Gradle `8.13`. `gradle.properties` enables modern Java syntax through Jabel, but the mod still runs in the old Minecraft 1.7.10 ecosystem.

The project-specific source lives primarily under `src/main/java/com/fox/ysmu`. The `software/bernie/geckolib3` package is an adapted Geckolib implementation for 1.7.10. Geckolib is the animation and model runtime used to turn JSON model and animation files into rendered animated geometry. The `com/eliotlash/mclib` package contains math-expression helpers used by the animation stack. The `net/geckominecraft` package contains 1.7.10 compatibility shims such as a `GlStateManager` facade and resource adapter. Treat these three packages as embedded compatibility dependencies, not ordinary project business logic.

The mod stores and scans models under runtime folders rooted at `config/ysmu`. `config/ysmu/custom` contains normal custom models, `config/ysmu/auth` contains authorization-gated models, `config/ysmu/export` is for exported output, and `config/ysmu/cache` stores encrypted model cache files. These paths are declared in `src/main/java/com/fox/ysmu/model/ServerModelManager.java`.

An EEP, short for `IExtendedEntityProperties`, is Forge 1.7.10’s way to attach persistent custom data to an entity. This project uses EEP classes in `src/main/java/com/fox/ysmu/eep`: `ExtendedModelInfo` stores the selected model, selected texture, and requested animation; `ExtendedAuthModels` stores authorization-model ownership; `ExtendedStarModels` stores starred models.

A `DataWatcher` is Minecraft 1.7.10’s per-entity synchronized data table. This project writes a byte into watcher id `28` to tell remote clients whether a player is on the ground or flying, because the local client cannot reliably infer those states for other players in 1.7.10.

`ResourceLocation` is Minecraft’s namespaced identifier type. A model id such as `ysmu:default` becomes a main model id `ysmu:default/main`, an arm model id `ysmu:default/arm`, and texture ids such as `ysmu:default/default.png`. Use `src/main/java/com/fox/ysmu/util/ModelIdUtil.java` instead of manually concatenating these strings.

`SimpleNetworkWrapper` is Forge 1.7.10’s packet registration and sending API. Packet registration is centralized in `src/main/java/com/fox/ysmu/network/NetworkHandler.java`; message classes live under `src/main/java/com/fox/ysmu/network/message`.

Molang is the expression/query language used by Bedrock-style animation files. In this project, Molang query variables are registered and updated by `src/main/java/com/fox/ysmu/client/animation/AnimationRegister.java`.

Backhand and Et Futurum are optional compatibility mods. Backhand adds an offhand to 1.7.10, and Et Futurum backports later-version features such as elytra and spectator mode. Business logic should call `src/main/java/com/fox/ysmu/compat/BackhandCompat.java` and `src/main/java/com/fox/ysmu/compat/EtfuturumCompat.java` rather than calling those mod APIs directly.

## Plan of Work

Milestone 1 establishes a baseline. Before editing source code, run the project’s build and record whether it passes. Then run a client and verify the existing main workflows: the default model loads, the model GUI opens through the configured keybinding, `/ysm reload` works in an integrated server, switching model and texture still renders a player, and the first-person empty hand renders when the custom model is enabled. If multiplayer testing is available, start a server and one client, then verify that a second player joining triggers model synchronization. Record the results in `Progress` and `Outcomes & Retrospective`; if any workflow is already broken, mark it as a baseline defect rather than trying to fix it during this milestone.

Milestone 2 clarifies the authorization-model data path. Read `src/main/java/com/fox/ysmu/data/ModelData.java`, `src/main/java/com/fox/ysmu/model/format/ServerModelInfo.java`, `src/main/java/com/fox/ysmu/eep/ExtendedAuthModels.java`, `src/main/java/com/fox/ysmu/model/format/FolderFormat.java`, `src/main/java/com/fox/ysmu/model/format/YsmFormat.java`, and `src/main/java/com/fox/ysmu/network/message/SetModelAndTexture.java`. The goal is for `isAuth`, `needAuth`, and `containModel` to mean what their names say. If the intended behavior is to enforce auth models, then store the constructor’s `isAuth` value in `ModelData`, assign the constructor’s `needAuth` value in `ServerModelInfo`, and make `ExtendedAuthModels.containModel` delegate to the set. If the intended behavior is temporarily “all authorization checks are disabled,” then rename or document that state explicitly and add a central flag so the bypass is visible. Prefer enforcing the existing design because `ServerModelManager.AUTH_MODELS`, `auth` folder scanning, and `SetModelAndTexture` already imply that authorization is meant to work. After this milestone, an unauthorized player should be rejected or reset to the default model when attempting to select an auth model.

Milestone 3 refactors model scanning and cache creation without changing file format. In `src/main/java/com/fox/ysmu/model/ServerModelManager.java`, keep public constants and public entry points stable, especially `reloadPacks()`, `sendRequestSyncModelMessage(...)`, and the folder path constants. Split `reloadPacks()` into clearly named private steps such as directory creation, built-in model copying, password initialization, and model cache rebuilding. In `FolderFormat` and `YsmFormat`, reduce duplicate cache-writing logic by extracting a helper that accepts a `ModelData`, writes the encrypted cache file to `ServerModelManager.CACHE_SERVER`, assigns the MD5 into `ServerModelInfo`, and returns that info. Keep the observable supported inputs unchanged: folder models need `main.json`, `arm.json`, and at least one `.png`; `.ysm` files need the same logical contents; missing animation files fall back to default animation JSON files.

Milestone 4 documents and organizes the network protocol. In `src/main/java/com/fox/ysmu/network/NetworkHandler.java`, replace scattered numeric ids with named constants for every registered message, grouped by serverbound messages, clientbound messages, and Bukkit/NPC compatibility messages. A serverbound message is sent from client to server; a clientbound message is sent from server to client. For each message class touched under `src/main/java/com/fox/ysmu/network/message`, add short comments only where they explain protocol order or thread constraints. Keep serialization formats compatible unless a versioned migration is explicitly added. Important message flows are: `RequestSyncModel` asks the client to report cached MD5 names; `SyncModelFiles` sends that MD5 list to the server; `SendModelFile` sends either the encrypted password blob or model bytes to the client; `RequestLoadModel` tells the client to load an already cached model; `SyncModelInfo` synchronizes selected model and texture for an entity.

Milestone 5 splits common event responsibilities. In `src/main/java/com/fox/ysmu/event/CommonEventHandler.java`, separate player EEP registration, login model-sync request, tracking sync, join-world auth validation, dirty `ExtendedModelInfo` broadcast, and motion state bit updates into small helper methods. Make `updateData(EntityPlayer player)` compute a fresh `newData` byte from current state and call `updateObject` once if the byte changed. This avoids the current confusing pattern where `oldData` is reused across two separate bit updates. Do not change `MOTION_DATAWATCHER_ID` during this cleanup unless a conflict is proven and a migration plan is added, because clients and servers must agree on the id.

Milestone 6 makes client model registration and rendering easier to read. In `src/main/java/com/fox/ysmu/client/ClientModelManager.java`, keep the public cache maps stable for now, but make registration steps explicit: `registerAll` should read as “derive model id, record auth state, register geometry, register animations, register textures.” Error handling should identify which model or resource failed instead of printing stack traces without context. In `src/main/java/com/fox/ysmu/client/ClientEventHandler.java`, split `onRenderHand(RenderHandEvent event)` into helper methods for deciding whether custom hand rendering should run, preparing the `CustomPlayerEntity`, setting the projection/modelview matrices, applying swing/equip transforms, drawing the right arm bone, and restoring OpenGL state. OpenGL state means the current rendering settings such as matrix stack, blending, culling, depth buffer, lighting, and texture state. Every push should have a matching pop in the same logical helper or in a clearly documented caller.

Milestone 7 organizes animation and compatibility code. In `src/main/java/com/fox/ysmu/client/animation/AnimationRegister.java`, group registrations by high-priority state, riding/flying/swimming state, damage/jump/sneak state, movement state, and idle fallback. Remove duplicated variable assignment such as the repeated `query.is_on_ground` setter if verification shows it is truly redundant. In `AnimationManager`, keep controller names stable but add small private helpers when the same “get animation id, find conditional rule, play if present” pattern repeats. In `compat/BackhandCompat.java` and `compat/EtfuturumCompat.java`, keep optional-mod API calls isolated. If business logic directly imports Backhand or Et Futurum classes elsewhere, move that access behind the compat classes.

Milestone 8 performs final documentation and validation. Update `AGENTS.md` if the cleanup changes repository guidance, especially if authorization behavior, network ids, or model cache protocol changes. Run the build and the same smoke tests used for the baseline. Compare results against Milestone 1. Update this ExecPlan’s `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` with the final state.

## Concrete Steps

Start in the repository root:

    Set-Location d:\Code\YesSteveModel-Unofficial

Check the working tree before starting. The command should show only intentional documentation files or a clean tree. If other user changes exist, do not revert them; work around them or pause and record the conflict in this plan.

    git status --short

Run the baseline build. On Windows use:

    .\gradlew.bat build

If dependency resolution fails because the machine is offline or a Maven repository is unreachable, record the failure exactly in `Surprises & Discoveries`. If the build succeeds, record the successful task summary in `Artifacts and Notes`.

The 2026-05-01 baseline did not reach a successful build. The first sandboxed run failed because the Gradle wrapper cache lock file under `E:\JetBrains\cache\.gradle` could not be accessed. The escalated run entered Gradle but failed at `:shadowJar` because the wrapper used Java 8 and the dependency `org.vafer:jdependency` was compiled for Java 11. A retry with `E:\Java\Java21` was requested and denied, so source cleanup proceeds with this baseline limitation recorded.

Run the client for baseline smoke testing:

    .\gradlew.bat runClient

In the client, verify the default model appears, the model management GUI opens with the configured keybinding, and the first-person hand renders when the player is empty-handed and custom hands are enabled. In an integrated world, run:

    /ysm reload

The expected result is a chat message using the translation key behind `message.yes_steve_model.model.reload.info`, and the model list should refresh without crashing.

After each milestone, rerun:

    .\gradlew.bat build

Per user instruction from 2026-05-01, Codex does not run the build or test commands directly. At each validation point, Codex asks the user to run the command in their environment and records the result here.

For milestones that touch rendering, model registration, or network sync, also rerun `.\gradlew.bat runClient` and repeat the relevant manual checks. If a milestone touches multiplayer synchronization, start a server with:

    .\gradlew.bat runServer

Then connect a client and verify that model files are requested and loaded. The observable client-side result is that a player can still select a synchronized model and other players render with that selected model rather than falling back unexpectedly.

When committing the cleanup, prefer one commit per milestone so regressions can be bisected. Example commit scopes are:

    docs: add cleanup exec plan
    refactor: clarify auth model state
    refactor: split model cache loading
    refactor: name ysmu packet ids
    refactor: split player sync events
    refactor: split custom hand rendering
    refactor: organize animation conditions

## Validation and Acceptance

The minimum automated acceptance is that `.\gradlew.bat build` completes successfully after each source-code milestone. If the baseline build already fails before cleanup, the acceptance for each milestone is that it does not introduce new compile errors beyond the recorded baseline failure, and the final retrospective must state that the repository still has a pre-existing build problem.

The model-loading acceptance is that built-in models copied from `src/main/resources/assets/ysmu/custom` still appear under `config/ysmu/custom` after startup or `/ysm reload`, encrypted cache files still appear under `config/ysmu/cache/server`, and the client can still load models into `ClientModelManager.MODELS`.

The authorization acceptance is that normal models under `config/ysmu/custom` remain selectable, while models under `config/ysmu/auth` follow one explicit behavior. If enforcement is implemented, an unauthorized player cannot select an auth model through `SetModelAndTexture` and is reset to `ysmu:default` with texture `ysmu:default/default.png` when necessary. If enforcement remains intentionally disabled, there must be one clear flag or comment explaining that the bypass is temporary, and the misleading hard-coded methods must no longer appear as accidental bugs.

The network acceptance is that packet registration still happens once through `NetworkHandler.init()`, no packet id is duplicated, and the client-server model sync flow still works. The proof is either a multiplayer smoke test or, if multiplayer cannot be run, a successful integrated client run plus build verification and a recorded reason why multiplayer was not tested.

The rendering acceptance is that third-person player rendering still replaces the vanilla renderer, first-person empty-hand rendering still shows the custom right arm, Backhand offhand rendering does not recurse when Backhand is present, and disabling self model or self hands through `Config` still prevents the corresponding rendering.

The animation acceptance is that common animation states still play: idle while standing, walk while moving, run while sprinting, jump while airborne, sneak while sneaking, swing while attacking, use while using an item, and the extra animation controller still plays requested animations from `ExtendedModelInfo`.

## Idempotence and Recovery

The build and run commands are safe to repeat. `ServerModelManager.reloadPacks()` intentionally recreates or overwrites built-in model files under `config/ysmu/custom`, so do not use that directory as the only copy of a hand-edited model during testing. If a test model matters, keep a separate backup outside `config/ysmu` before running reload.

Do not delete `config/ysmu/cache` as part of cleanup unless a specific cache protocol change requires it. If a cache protocol version changes, document the reason in `Decision Log`, update `EncryptTools.VERSION`, and add a clear recovery step telling users to remove stale cache files only after backing up custom model sources.

If a source change causes rendering to break, revert only that milestone’s own edits. Do not run `git reset --hard` or revert unrelated files. If the working tree contains user edits in the same file, inspect the diff and preserve the user edits while backing out only the cleanup change.

If Gradle dependency resolution fails due to network or repository availability, do not change source code to work around it. Record the exact failing repository or dependency and continue only with checks that do not require dependency downloads.

## Artifacts and Notes

Initial repository observations from 2026-05-01:

    rg --files | rg -i "plan|exec"
    PLANS.md

    git status --short
    A  AGENTS.md
    A  PLANS.md

Baseline build attempt from 2026-05-01:

    .\gradlew.bat build
    Exception in thread "main" java.io.FileNotFoundException: E:\JetBrains\cache\.gradle\wrapper\dists\gradle-8.13-bin\...\gradle-8.13-bin.zip.lck (拒绝访问。)

    .\gradlew.bat build   # rerun outside the sandbox
    Execution failed for task ':shadowJar'.
    > org/vafer/jdependency/Clazzpath has been compiled by a more recent version of the Java Runtime (class file version 55.0), this version of the Java Runtime only recognizes class file versions up to 52.0

Authorization cleanup source summary from 2026-05-01:

    src/main/java/com/fox/ysmu/data/ModelData.java now stores the constructor's isAuth value and returns it from isAuth().
    src/main/java/com/fox/ysmu/model/format/ServerModelInfo.java now stores the constructor's needAuth value.
    src/main/java/com/fox/ysmu/eep/ExtendedAuthModels.java now checks authModels.contains(modelId).
    src/main/java/com/fox/ysmu/model/format/FolderFormat.java now passes true when caching folder models from config/ysmu/auth.
    src/main/java/com/fox/ysmu/network/message/SetModelAndTexture.java now resets unauthorized auth-model selections to ysmu:default.
    src/main/java/com/fox/ysmu/client/gui/PlayerModelScreen.java now marks unauthorized auth models as locked buttons.

Model loading/cache cleanup source summary from 2026-05-01:

    src/main/java/com/fox/ysmu/model/ServerModelManager.java reloadPacks() now calls clearModelCaches(), createConfigDirectories(), copyBuiltInModels(), initPassword(), and rebuildModelCaches().
    src/main/java/com/fox/ysmu/model/format/ModelCacheWriter.java was added as the single place that calls EncryptTools.assembleEncryptModels(), computes the uppercase MD5 name, writes to ServerModelManager.CACHE_SERVER, and returns ServerModelInfo.
    src/main/java/com/fox/ysmu/model/format/FolderFormat.java and src/main/java/com/fox/ysmu/model/format/YsmFormat.java now parse their respective formats and delegate cache writing to ModelCacheWriter.

Network cleanup source summary from 2026-05-01:

    src/main/java/com/fox/ysmu/network/NetworkHandler.java now declares named packet ids for serverbound packets, clientbound packets, and NPC compatibility packets. init() delegates to registerServerboundMessages(), registerClientboundMessages(), and initBukkit().
    src/main/java/com/fox/ysmu/network/message/SyncModelFiles.java now documents the RequestSyncModel -> SyncModelFiles -> SendModelFile/RequestLoadModel ordering and why large file sends are submitted to ThreadTools.THREAD_POOL.
    src/main/java/com/fox/ysmu/network/message/SendModelFile.java now documents the existing encrypted-password length check.
    src/main/java/com/fox/ysmu/network/message/RequestLoadModel.java now documents that decryption waits for the password and then uses Minecraft.func_152344_a for client-thread model registration.
    src/main/java/com/fox/ysmu/network/message/SyncModelInfo.java now documents the short wait for remote entities before applying synced EEP data.

Common event cleanup source summary from 2026-05-01:

    src/main/java/com/fox/ysmu/event/CommonEventHandler.java event methods now call helpers for requestModelSync(), registerPlayerProperties(), copyPlayerProperties(), syncTrackedPlayerModelInfo(), syncJoinedPlayerState(), syncAuthModelsAndValidateSelection(), syncStarModels(), broadcastDirtyModelInfo(), and getPlayerTrackingPoint().
    CommonEventHandler.updateData(EntityPlayer) now starts from the old watcher byte, applies ON_GROUND and IS_FLYING through setFlag(), and calls DataWatcher.updateObject(MOTION_DATAWATCHER_ID, newData) once only when the byte changed.

Client model/rendering cleanup source summary from 2026-05-01:

    src/main/java/com/fox/ysmu/client/ClientModelManager.java registerAll(ModelData) now delegates to getModelId(), recordAuthState(), registerGeometry(), registerModelAnimations(), and registerModelTextures().
    ClientModelManager now logs the affected geometry id, animation name, or default model load context before printing stack traces for those failures.
    src/main/java/com/fox/ysmu/client/ClientEventHandler.java onRenderHand(RenderHandEvent) now delegates to shouldRenderCustomHand(), getArmGeoModel(), getCustomHandPlayer(), renderCustomHand(), pushFirstPersonMatrices(), popFirstPersonMatrices(), renderMainHandArm(), applyEquipProgress(), setupHandLighting(), prepareHandRenderState(), applySwingTransform(), applyRightArmPlacement(), renderRightArmBone(), and restoreHandRenderState().
    The custom-hand render path still cancels the vanilla hand before checking EEP/model/renderer availability, matching the previous behavior after the same eligibility checks pass.

Animation/compat cleanup source summary from 2026-05-01:

    src/main/java/com/fox/ysmu/client/animation/AnimationRegister.java registerAnimationState() now delegates to registerHighPriorityStates(), registerRidingFlyingStates(), registerDamageJumpSneakStates(), registerMovementStates(), and registerIdleFallback().
    AnimationRegister.registerVariables() now delegates to registerQueryVariables() and registerYsmVariables(). setParserValue() now delegates to setEntityQueryValues(), setStateQueryValues(), setItemUseQueryValues(), setWorldQueryValues(), and setYsmValues().
    AnimationRegister no longer sets query.is_on_ground twice during runtime Molang value assignment.
    src/main/java/com/fox/ysmu/client/animation/AnimationManager.java now uses getAnimationId(), playLoopIfPresent(), findHoldAnimation(), findSwingAnimation(), findUseAnimation(), and findArmorAnimation() to remove repeated conditional lookup code while keeping controller names and fallback animation names stable.
    src/main/java/com/fox/ysmu/client/renderer/layer/CustomPlayerItemInHandLayer.java now documents why the direct Backhand optional marker remains in the class declaration.

Final documentation and static check summary from 2026-05-01:

    AGENTS.md now says auth models under config/ysmu/auth are enforced through ModelData.isAuth(), ServerModelInfo.needAuth, ExtendedAuthModels.containModel(), and SetModelAndTexture server-side reset behavior.
    AGENTS.md now says CommonEventHandler.updateData() computes a complete motion byte before updating DataWatcher id 28 once.
    AGENTS.md now documents that CustomPlayerItemInHandLayer's IOffhandRenderOptOut declaration is the allowed Backhand optional marker exception.
    git diff --check
    <no output>

Final user validation summary from 2026-05-01:

    After Codex requested runClient smoke testing for default model loading, model GUI opening, model/texture switching, first-person empty-hand rendering, and integrated-world /ysm reload, the user replied:
    OK

Relevant files already inspected while preparing this plan include:

    src/main/java/com/fox/ysmu/ysmu.java
    src/main/java/com/fox/ysmu/CommonProxy.java
    src/main/java/com/fox/ysmu/client/ClientProxy.java
    src/main/java/com/fox/ysmu/event/CommonEventHandler.java
    src/main/java/com/fox/ysmu/client/ClientEventHandler.java
    src/main/java/com/fox/ysmu/model/ServerModelManager.java
    src/main/java/com/fox/ysmu/model/format/FolderFormat.java
    src/main/java/com/fox/ysmu/model/format/YsmFormat.java
    src/main/java/com/fox/ysmu/network/NetworkHandler.java
    src/main/java/com/fox/ysmu/client/ClientModelManager.java
    src/main/java/com/fox/ysmu/client/renderer/CustomPlayerRenderer.java
    src/main/java/com/fox/ysmu/client/model/CustomPlayerModel.java
    src/main/java/com/fox/ysmu/client/entity/CustomPlayerEntity.java
    src/main/java/com/fox/ysmu/client/animation/AnimationRegister.java
    src/main/java/com/fox/ysmu/client/animation/AnimationManager.java
    src/main/java/com/fox/ysmu/data/EncryptTools.java
    src/main/java/com/fox/ysmu/data/ModelData.java
    src/main/java/com/fox/ysmu/eep/ExtendedModelInfo.java
    src/main/java/com/fox/ysmu/eep/ExtendedAuthModels.java
    src/main/java/com/fox/ysmu/eep/ExtendedStarModels.java

## Interfaces and Dependencies

At the end of this cleanup, the following public entry points should still exist unless a later decision log entry explains a deliberate change:

`src/main/java/com/fox/ysmu/ysmu.java` must remain the Forge mod entry point with `@Mod(modid = ysmu.MODID, version = Tags.VERSION, name = "ysmu", acceptedMinecraftVersions = "[1.7.10]")`.

`src/main/java/com/fox/ysmu/model/ServerModelManager.java` must keep these public paths and entry points:

    public static final Path FOLDER
    public static final Path CUSTOM
    public static final Path AUTH
    public static final Path EXPORT
    public static final Path CACHE
    public static final Path CACHE_SERVER
    public static final Path CACHE_CLIENT
    public static void reloadPacks()
    public static void sendRequestSyncModelMessage()
    public static void sendRequestSyncModelMessage(EntityPlayer player)
    public static void sendRequestSyncModelMessage(List<EntityPlayer> playerList)

`src/main/java/com/fox/ysmu/client/ClientModelManager.java` must keep the existing public cache maps until a separate migration plan replaces them:

    public static Map<ResourceLocation, List<ResourceLocation>> MODELS
    public static Map<ResourceLocation, Pair<Double, Double>> SCALE_INFO
    public static Map<ResourceLocation, List<IChatComponent>> EXTRA_INFO
    public static Map<ResourceLocation, String[]> EXTRA_ANIMATION_NAME
    public static void registerAll(ModelData data)
    public static void loadDefaultModel()
    public static void sendSyncModelMessage()

`src/main/java/com/fox/ysmu/network/NetworkHandler.java` must still create a `SimpleNetworkWrapper` named `ysmu_network`, register all currently supported message classes, and expose:

    public static final SimpleNetworkWrapper CHANNEL
    public static void init()
    public static void sendToClientPlayer(IMessage message, EntityPlayer player)

The EEP classes must remain retrievable by their existing names so saved data and event code keep working:

    ExtendedModelInfo.EXT_PROP_NAME = "ysmu_ModelInfo"
    ExtendedAuthModels.EXT_PROP_NAME = "ysmu_AuthModels"
    ExtendedStarModels.EXT_PROP_NAME = "ysmu_StarModels"

The optional mod boundaries must remain:

    BackhandCompat.getOffhandItem(EntityPlayer player)
    BackhandCompat.renderOffhand(float partialTicks)
    BackhandCompat.swingingArm(EntityPlayer player)
    EtfuturumCompat.isFallFlying(EntityPlayer entityPlayer)
    EtfuturumCompat.isSpectator(EntityPlayer entityPlayer)
    EtfuturumCompat.hasElytra(EntityPlayer entityPlayer)
    EtfuturumCompat.getElytraRot(EntityPlayer entityPlayer, String xyz)

The cleanup should not add a new mandatory runtime dependency. If a test-only dependency is added, it must be declared in `dependencies.gradle`, used by a test under `src/test`, and documented in this plan.

## Revision Notes

2026-05-01 / Codex: Initial ExecPlan created from the project cleanup analysis. The plan emphasizes baseline capture, narrow milestones, behavior-preserving refactors, and explicit validation because the mod’s model sync, rendering, and animation paths are tightly coupled.

2026-05-01 / Codex: Recorded the actual baseline build blocker and the decision to continue cleanup with the build limitation stated. This keeps the plan restartable for a future contributor who can authorize or configure a newer Gradle JDK.

2026-05-01 / Codex: Recorded the user instruction that builds and tests are user-run validation steps because sandbox behavior makes Codex-run Gradle results unreliable. Added the authorization cleanup source summary and pending validation item.

2026-05-01 / Codex: Recorded the model loading/cache cleanup source changes and the pending user-run build validation. This note exists because the cache protocol is sensitive: the refactor should change responsibility boundaries only, not encrypted bytes, MD5 naming rules, or supported model inputs.

2026-05-01 / Codex: Recorded the network cleanup source changes and pending user-run build validation. This note exists because packet ids are protocol state and must remain stable across clients and servers.

2026-05-01 / Codex: Recorded the common event cleanup source changes and pending user-run build validation. This note exists because player EEP sync and motion watcher updates cross the client/server boundary and need clear restartable context.

2026-05-01 / Codex: Recorded the client model/rendering cleanup source changes and pending user-run build validation. This note exists because first-person hand rendering depends on paired matrix/state operations and should be easy to audit after a restart.

2026-05-01 / Codex: Recorded the animation/compat cleanup source changes and pending user-run build validation. This note exists because animation priorities, controller names, and optional mod boundaries are user-visible through model animation behavior.

2026-05-01 / Codex: Recorded the user-reported animation/compat build success, final AGENTS.md documentation updates, and clean `git diff --check` result. Runtime smoke tests are still pending user execution because Codex is not running build or test commands in this sandboxed environment.

2026-05-01 / Codex: Recorded the user-reported `OK` for final runtime smoke testing and closed the remaining validation progress item. Multiplayer smoke testing was not separately evidenced, so it is recorded as residual risk rather than silently assumed.
