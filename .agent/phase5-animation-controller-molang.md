# Phase 5 Animation Controllers and Molang

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document follows `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

Phase 5 makes OpenYSM animation controller files useful in the 1.7.10 client without replacing the existing GeckoLib renderer. After this work, an OpenYSM folder or binary model that contains `animation_controllers` keeps those controller definitions through the server cache and client registration path. The client can evaluate a practical first subset of controller states and transition expressions for the player model, so controller-driven idle, movement, hold, use, and swing animations can coexist with the older fixed `AnimationManager` fallback.

The user-visible proof is a model under `config/ysmu/custom/<model>/ysm.json` that references a controller JSON file. After joining a world, the client log should say the model registered OpenYSM controllers, and the model should still animate through idle/walk/run/sneak/use/swing paths. If a controller expression cannot be evaluated, rendering must fall back or continue safely instead of crashing the render thread.

## Progress

- [x] (2026-05-10 08:50+08:00) Read `README.md`, `.agent/PLANS.md`, and `.agent/phase4-new-model-sync-protocol.md` to confirm Phase 5 scope and validation limits.
- [x] (2026-05-10 08:50+08:00) Inspected `AnimationRegister`, `AnimationManager`, `ConditionManager`, `CustomPlayerEntity`, `ClientModelManager`, `RawYsmModel`, `YSMFolderDeserializer`, and OpenYSM controller examples.
- [x] (2026-05-10 09:20+08:00) Preserved OpenYSM animation controller files when `RawYsmModelAdapter` converts raw models into legacy `ModelData`.
- [x] (2026-05-10 09:20+08:00) Registered preserved controller files in the client beside normal GeckoLib animation files.
- [x] (2026-05-10 09:20+08:00) Added a lightweight player controller runtime that evaluates controller state transitions for existing 1.7.10 GeckoLib `AnimationController` slots.
- [x] (2026-05-10 09:20+08:00) Extended basic state/query mappings for `climb`, `climbing`, ladder, fishing, and riptide-safe variables.
- [x] (2026-05-10 09:20+08:00) Ran non-Gradle static checks and recorded host-side Gradle commands for the user.
- [x] (2026-05-10 09:35+08:00) Fixed malformed controller JSON in the Phase 5 unit-test fixture after host `.\gradlew.bat test` reported `JsonSyntaxException`.
- [x] (2026-05-10 09:45+08:00) Fixed `misc/4_default_controllers` regressions where non-sword block clicks entered sword attack states, sword draw animations replayed, and fully drawn bows replayed their draw animation.
- [x] (2026-05-10 10:59+08:00) Rechecked OpenYSM's `PlayerAnimationController`, `ConditionSwing`, `ConditionUse`, `ConditionHold`, `InnerClassify`, and `misc/4_default_controllers` after manual testing exposed sword-combo and bow-use regressions.
- [x] (2026-05-10 10:59+08:00) Added exact OpenYSM `player.pre_*` and `player.post_*` controller slots in the 1.7.10 player animatable instead of folding them into the old hold/swing/use fallback controllers.
- [x] (2026-05-10 10:59+08:00) Ported the OpenYSM item inner-classifier idea for 1.7.10 items so `swing:sword`, `hold_mainhand:bow`, `use_mainhand:bow`, and related category animation names are detected.
- [x] (2026-05-10 10:59+08:00) Fixed controller expression `ctrl.idle` so it is false during run, walk, sneak, jump, swim, ladder, sleep, hurt, and similar non-idle states.
- [x] (2026-05-10 11:13+08:00) Fixed a remaining sword attack restart when `ctrl.idle` changes mid-attack by letting the OpenYSM state switch animation entries while preserving the state's elapsed tick.
- [x] (2026-05-10 11:13+08:00) Fixed controller processing order by preserving GeckoLib controller registration order, so pre-parallel blink layers do not randomly run after and override bow-use eyelid transforms.
- [x] (2026-05-10 12:20+08:00) Inspected OpenYSM `PhysicsManager`, first/second-order physics functions, bone accessor functions, and built-in hair/chest animation examples.
- [x] (2026-05-10 12:20+08:00) Added `ysm.first_order`, `ysm.second_order`, `ysm.bone_rot(...).x/y/z`, `ysm.bone_pos(...).x/y/z`, and `ysm.bone_scale(...).x/y/z` support to the existing GeckoLib Molang path.
- [x] (2026-05-10 12:20+08:00) Scoped runtime `v.*` variables and physics state by player, model id, and animation id, and wired animation timeline instructions so OpenYSM physics helper variables can update during playback.
- [x] (2026-05-10 12:20+08:00) Added a focused parser unit test for OpenYSM physics function strings, bone-vector accessor rewriting, and timeline statement splitting.
- [x] (2026-05-10 12:20+08:00) Ran non-Gradle static checks for the physics pass.
- [x] (2026-05-10 12:40+08:00) Rechecked physics behavior against OpenYSM after manual feedback and fixed timeline-before-bone evaluation, short-loop remaining event execution, deterministic active bone application order, and abbreviated sync footer parsing.
- [ ] Run host Gradle verification and a manual `4_default_controllers` client pass after these OpenYSM-alignment fixes.

## Surprises & Discoveries

- Observation: Phase 2 already parses controller files into `RawYsmModel.RawAnimationControllerFile`, including state animations, transitions, `on_entry`, `on_exit`, `sound_effects`, and `blend_transition`.
  Evidence: `src/main/java/com/fox/ysmu/model/resource/YSMFolderDeserializer.java` has `parseAnimationControllers` and `parseControllerState`.
- Observation: Phase 3/4 bridge currently drops controller files when converting raw models to legacy `ModelData`.
  Evidence: `RawYsmModelAdapter.toLegacyModelData` only writes model, texture, and animation JSON maps; it does not copy `raw.mainEntity.animationControllerFiles`.
- Observation: The existing GeckoLib controller class supports one animation queue per controller and transition lengths, but not OpenYSM's full parallel blend graph.
  Evidence: `software.bernie.geckolib3.core.controller.AnimationController` exposes `transitionLengthTicks` and `setAnimation(AnimationBuilder)`, while `AnimationBuilder` queues named animations serially.
- Observation: OpenYSM's built-in `misc/4_default_controllers` sample has controllers named `player.pre_parallel_0`, `player.parallel_0`, `player.hold_offhand`, `player.post_swing`, and `player.post_hold`.
  Evidence: `OpenYSM/src/main/resources/assets/yes_steve_model/builtin/misc/4_default_controllers/controller/main_controllers.json`.
- Observation: The old `ModelData` envelope can carry controller JSON without a wire-format change.
  Evidence: `EncryptTools` serializes animation entries as a string-keyed byte map; Phase 5 uses keys prefixed with `__ysm_controller__`.
- Observation: The first Phase 5 test fixture had one extra closing brace in `controllerJson()`.
  Evidence: Host `.\gradlew.bat test` failed `YsmResourceFormatTest.folderDeserializerReadsOpenYsmFolderAndBridgeablePlayerFiles()` with `JsonSyntaxException`; removing the extra final brace leaves the root object closed by the existing `}}}}` suffix.
- Observation: The `misc/4_default_controllers` `player.post_swing` controller uses `v.swing_sword` for sword combo transitions, but the first runtime shim set that variable for every swing.
  Evidence: `OpenYsmPlayerControllerRuntime.prepareFrameVariables` unconditionally wrote `swing_sword=1` when `player.isSwingInProgress`; the controller file transitions from `default` to `attack1` on `v.swing_sword`.
- Observation: `hold_on_last_frame` was parsed, but the 1.7.10 GeckoLib controller treated it like a repeating loop.
  Evidence: `ILoopType.EDefaultLoopTypes.HOLD_ON_LAST_FRAME` reports `isRepeatingAfterEnd() == true`, and `AnimationController.processCurrentAnimation` reset the tick for every repeating animation.
- Observation: OpenYSM does not treat `post_swing`, `post_hold`, and `post_use` as aliases for the coded `swing`, `hold_mainhand`, or `use` predicates. They are separate slot controllers installed around the coded predicates.
  Evidence: `OpenYSM/src/main/java/com/elfmcys/yesstevemodel/geckolib3/core/controller/controllers/PlayerAnimationController.java` registers `pre_hold`, `post_hold`, `pre_swing`, `post_swing`, `pre_use`, and `post_use` through `registerSlotController`, while `swing` uses `ItemHoldAnimationPredicate` and `use` uses `InteractionHandAnimationPredicate`.
- Observation: The sample sword-combo trigger is carried by an animation timeline, not by the post-swing controller itself. In the 1.7.10 bridge, that means the fallback swing predicate must be able to select `swing:sword`.
  Evidence: `OpenYSM/src/main/resources/assets/yes_steve_model/builtin/misc/4_default_controllers/animations/arm.animation.json` defines `swing:sword` with a timeline setting `v.swing_sword=1;`; `controller/main_controllers.json` reads that variable in `player.post_swing`.
- Observation: `ctrl.idle` must be a derived state meaning no other movement or body state is active. Returning true for unknown or default controller names makes `sword_idle_attack_*` win even while the player is running or sneaking.
  Evidence: `player.post_swing` chooses `sword_attack_01` on `!ctrl.idle` and `sword_idle_attack_01` on `ctrl.idle`; manual testing showed idle attack variants while moving before the evaluator excluded non-idle states.
- Observation: Bow draw on the player depends on conditional use classification, not only on the item renderer. OpenYSM's coded `use` predicate plays `use_mainhand:bow` or `use_offhand:bow` when the held item is classified as a bow.
  Evidence: `InteractionHandAnimationPredicate` checks `ConditionUse.doTest`, and `ConditionUse.doExtraTest` calls `InnerClassify.doClassifyTest`; the sample animation file contains `use_mainhand:bow` and `use_offhand:bow`.
- Observation: The bow "aiming with one eye closed" effect is not produced by `ysm.is_close_eyes`. That variable still only covers sleeping and periodic blinking; the bow pose drives the eyelid and eyebrow bones directly from `use_mainhand:bow` or `use_offhand:bow`.
  Evidence: `OpenYSM/src/main/java/com/elfmcys/yesstevemodel/client/animation/molang/YSMBinding.java` implements `is_close_eyes` as sleep-or-blink, while `OpenYSM/src/main/resources/assets/yes_steve_model/builtin/misc/4_default_controllers/animations/arm.animation.json` animates `RightEyelid`/`RightEyebrow` for `use_mainhand:bow` and `LeftEyelid`/`LeftEyebrow` for `use_offhand:bow`.
- Observation: The 1.7.10 GeckoLib `AnimationData` stored controllers in a `HashMap`, which loses the registration order that `CustomPlayerEntity.registerControllers` relies on for layer priority.
  Evidence: `AnimationData.getAnimationControllers().values()` is the order used by `AnimationProcessor`; replacing the map with `LinkedHashMap` keeps `pre_parallel_*` before `use_controller`, matching the intended registration order.
- Observation: OpenYSM's hair and chest physics are mostly normal Molang keyframe expressions that call `ysm.second_order(...)`, while some hair chains depend on timeline assignments such as `v.HP_x=...` and bone reads such as `ysm.bone_rot('BackHairA1').x`.
  Evidence: `OpenYSM/src/main/resources/assets/yes_steve_model/builtin/default/animations/main.animation.json` and `misc/4_default_controllers/animations/main.animation.json`.
- Observation: OpenYSM stores physics values per entity and uses the first string argument as the stable key. A newly created first/second-order value returns the current input for that evaluation, then later frames return the simulated value.
  Evidence: `OpenYSM/src/main/java/com/elfmcys/yesstevemodel/client/animation/molang/PhysicsManager.java`, `FirstOrderFunction.java`, and `SecondOrderFunction.java`.
- Observation: The current mclib parser rejects quoted strings before function parsing, so OpenYSM string-keyed functions cannot be represented directly.
  Evidence: `com.eliotlash.mclib.math.MathBuilder.breakdown` only accepts word, digit, operator, dot, comma, and parenthesis characters.
- Observation: OpenYSM executes animation timeline instructions before sampling bone animation points. The 1.7.10 port was executing custom instruction keyframes after building bone queues, so same-frame helpers such as `v.HP_x` were stale while hair/chest physics expressions were evaluated.
  Evidence: `OpenYSM/.../AnimationControllerInstance.process` calls `executeTimelineEvents` before `processRunningAnimation`, while `software.bernie.geckolib3.core.controller.AnimationController.processCurrentAnimation` previously processed bone keyframes first.
- Observation: OpenYSM runs remaining timeline events before looping an animation and then rewinds to the modulo tick. The port reset executed keyframes immediately, so the `Hair_Physics` event at `0.0101` could be skipped on normal frame deltas.
  Evidence: OpenYSM `AnimationControllerInstance.process` calls `executeRemainingEvents` before resetting the loop; `Hair_Physics` in `misc/4_default_controllers` stores previous hair root rotation at `0.0101`.
- Observation: Chain hair physics depends on applying active bone transforms in animation order. Iterating `HashMap.values()` can apply `BackHairC1` before the `BackHairB1` rotation it reads through `ysm.bone_rot(...)`.
  Evidence: OpenYSM builds `activeBoneAnimationQueues` from `currentAnimation.boneAnimations`; this port previously iterated `AnimationController.boneAnimationQueues`, a `HashMap`.
- Observation: The sync footer writer emits a shortened footer when `footer.version == 65535`: version, zero flag, and time only. The parser always tried to read `rand`, `extra`, and `unkInt2`, causing EOF on synced folder models.
  Evidence: `YSMBinarySerializer.writeFooter` skips strings when `unkInt1 == 0`; the reported stack traces ended in `YSMByteBuf.readVarLong` after parsing the model body.

## Decision Log

- Decision: Keep the existing Molang parser and add a focused controller-expression evaluator instead of importing the full OpenYSM Molang runtime in this phase.
  Rationale: README explicitly says to choose one Molang strategy and not replace parser and renderer in the same stage. The current GeckoLib parser is already wired into animation keyframes, while controller transitions need a small boolean subset first.
  Date/Author: 2026-05-10 / Codex
- Decision: Store controller JSON in `ModelData.getAnimation()` with a reserved `__ysm_controller__` prefix.
  Rationale: The legacy encrypted model cache and network protocol already move the animation map end-to-end. A reserved key is additive, keeps old clients able to ignore the entry as a bad animation JSON, and avoids changing the wire format.
  Date/Author: 2026-05-10 / Codex
- Decision: Map OpenYSM controller names onto the existing 1.7.10 player controller slots instead of registering a new dynamic controller graph.
  Rationale: `CustomPlayerEntity` already has stable controllers for main, hold, use, swing, cap, armor, and parallel slots. Reusing them preserves current rendering behavior and makes fallback straightforward.
  Date/Author: 2026-05-10 / Codex
- Decision: Treat `sound_effects` as parsed data but do not play audio in Phase 5.
  Rationale: README places full audio support in Phase 8. This phase must keep controller state data without pulling audio cache/playback into the render predicate path.
  Date/Author: 2026-05-10 / Codex
- Decision: Let conditional hold/use animations keep the loop mode declared by their animation JSON instead of forcing `LOOP` in `AnimationManager`.
  Rationale: OpenYSM uses `hold_on_last_frame` for draw-sword and draw-bow style animations. Forcing `LOOP` causes those one-time transitions to replay while the item is held or used.
  Date/Author: 2026-05-10 / Codex
- Decision: Register exact `player.pre_main`, `player.post_main`, `player.pre_hold`, `player.post_hold`, `player.pre_swing`, `player.post_swing`, `player.pre_use`, and `player.post_use` GeckoLib controllers in `CustomPlayerEntity`.
  Rationale: OpenYSM layers these as separate slots. Mapping `player.post_swing` into `swing_controller` or `player.post_hold` into `hold_mainhand_controller` changes priority and causes controller JSON to replace coded fallback behavior instead of layering with it.
  Date/Author: 2026-05-10 / Codex
- Decision: Keep a small 1.7.10 shim that raises `v.swing_sword` on a new sword swing for `player.post_swing`, but only as a compatibility bridge until timeline instruction execution is complete.
  Rationale: OpenYSM normally sets that variable from the `swing:sword` animation timeline. This port does not yet execute animation timeline instructions, so the shim preserves the sample combo behavior while being scoped to sword swings and new swing edges.
  Date/Author: 2026-05-10 / Codex
- Decision: Port OpenYSM's `InnerClassify` category matching into the 1.7.10 condition classes instead of using registry-name substring matching for known categories.
  Rationale: OpenYSM treats categories such as sword, bow, shield, spear, and throwable potion as item classes or tags. Substring matching can misclassify modded items and cannot discover `use_mainhand:bow` or `swing:sword` unless those names are stored as conditional tests.
  Date/Author: 2026-05-10 / Codex
- Decision: Allow conditional animation entries to change inside one controller state, but preserve the state's elapsed tick when swapping the GeckoLib animation.
  Rationale: The first follow-up cached `sword_idle_attack_01` for the whole `attack1` state, which stopped replay but kept the idle attack overlay active after the player started walking. OpenYSM visibly changes to the moving attack entry while keeping the attack progress. The 1.7.10 bridge now switches to the newly selected entry through a GeckoLib helper that keeps `tickOffset` aligned to the OpenYSM state elapsed time.
  Date/Author: 2026-05-10 / Codex
- Decision: Preserve GeckoLib controller registration order with `LinkedHashMap`.
  Rationale: The player controller registration comment and OpenYSM's list-based `AnimationData` both depend on deterministic layer order. This also lets bow-use eyelid transforms run after the pre-parallel blink layer.
  Date/Author: 2026-05-10 / Codex
- Decision: Keep using the existing mclib-based Molang parser and rewrite OpenYSM string literals into interned numeric ids before `MathBuilder.breakdown`.
  Rationale: This enables `ysm.second_order('胸部垂直', ...)` and bone-name arguments without replacing the parser in Phase 5.
  Date/Author: 2026-05-10 / Codex
- Decision: Store `v.*` values and physics simulations in a per-render-frame runtime keyed by player UUID, model `ResourceLocation`, and animation `ResourceLocation`.
  Rationale: OpenYSM variables used by physics are entity-local. Leaving them in the global parser map would let one rendered player leak hair/chest state into another.
  Date/Author: 2026-05-10 / Codex
- Decision: Execute GeckoLib custom instruction keyframes as Molang statements for every player controller.
  Rationale: OpenYSM uses animation `timeline` entries for helper variables such as sword-combo and hair-chain deltas. Without executing those instructions, the physics expressions have no current `v.*` inputs.
  Date/Author: 2026-05-10 / Codex
- Decision: Move keyframe event execution ahead of bone expression sampling and process remaining events before loop rewind.
  Rationale: This matches OpenYSM's ordering and is required for physics helper variables and very short loop animations such as `Hair_Physics`.
  Date/Author: 2026-05-10 / Codex
- Decision: Apply only active bone animation queues in animation JSON order.
  Rationale: OpenYSM chain physics reads previous bones by name during the same controller pass. Stable animation order keeps source bones current before dependent bones evaluate.
  Date/Author: 2026-05-10 / Codex
- Decision: Treat absent or abbreviated OpenYSM sync footers as valid.
  Rationale: Folder-derived sync payloads can use the serializer's `65535,0,time` footer form, and older cache payloads may have no footer at all. Footer absence should not make an otherwise parsed bridgeable model fail registration.
  Date/Author: 2026-05-10 / Codex

## Outcomes & Retrospective

This pass implemented a first controller/Molang runtime slice. `RawYsmModelAdapter` now preserves folder controller JSON and synthesizes controller JSON for binary raw controller structures. `ClientModelManager` separates controller resources from normal animation files, stores them in `OpenYsmAnimationControllerRegistry`, and clears controller and Molang runtime state when model caches clear. `OpenYsmPlayerControllerRuntime` maps common OpenYSM player controller names to the existing GeckoLib controller slots and evaluates state transitions with a focused expression evaluator.

The result is intentionally additive. If a controller is absent, selects a missing animation, or hits an unsupported expression/function, the existing fixed `AnimationManager` predicates continue to provide old behavior. Full OpenYSM parser/runtime replacement, audio playback, and mesh renderer changes remain future work.

After manual testing with `misc/4_default_controllers`, this plan also fixed several controller-runtime regressions. The 1.7.10 player now has exact OpenYSM pre/post slot controllers so `player.post_swing` and `player.post_hold` layer like OpenYSM instead of replacing old fallback predicates. The sword-combo bridge now only raises `v.swing_sword` on new sword swing edges, `ctrl.idle` no longer remains true during movement states, conditional swing animations play once, and conditional use animations such as `use_mainhand:bow` can be found through an OpenYSM-style item classifier. Controller states now can switch conditional animation entries without resetting their elapsed time, so changing movement state during a sword attack does not replay the attack and does not keep the idle attack overlay over walking. GeckoLib controller iteration now preserves registration order, allowing bow-use eyelid transforms to layer after the pre-parallel blink controller. The GeckoLib controller now treats `HOLD_ON_LAST_FRAME` as a held final frame rather than a replaying loop, and `AnimationManager` no longer forces conditional hold/use animations to `LOOP`.

The physics follow-up adds the OpenYSM first-order and second-order Molang functions, bone rotation/position/scale accessors, scoped `v.*` variables, and timeline Molang instruction execution. This covers built-in hair, breast/chest, eye, elytra, and chained-hair secondary-motion expressions that are expressed through `ysm.second_order(...)` and timeline helper variables.

The follow-up after manual feedback tightened OpenYSM alignment further. Timeline instructions now execute before bone expressions are sampled, looping animations execute remaining timeline events before rewinding, active bone queues apply in animation order instead of hash-map order, and the first-order filter now uses OpenYSM's exact update equation. Sync footer parsing now accepts both full and abbreviated footer forms, fixing the reported EOF warnings for synced models such as `4_default_controllers`.

## Context and Orientation

This mod runs on Minecraft Forge 1.7.10 and uses a ported GeckoLib runtime. `CustomPlayerEntity.registerControllers` creates a fixed set of GeckoLib animation controllers such as `main_controller`, `hold_mainhand_controller`, `hold_offhand_controller`, `swing_controller`, `use_controller`, and several parallel controllers. Each controller calls a predicate in `AnimationManager`, and that predicate decides which named animation to play.

OpenYSM controller JSON is different. A controller file has an `animation_controllers` object. Each controller contains states. Each state can list animations, transitions to other states guarded by Molang expressions, `on_entry` and `on_exit` expressions, sound effects, and blend transition settings. In this phase, "Molang expression" means the small expression language used by model files to read values such as `query.is_on_ground`, `q.all_animations_finished`, `ctrl.idle`, `ctrl.hold('mainhand', ':sword')`, or `ysm.is_fishing`.

`RawYsmModelAdapter` is the bridge from OpenYSM raw models to the old `ModelData` format. `ClientModelManager.registerAll` receives `ModelData`, registers geometry, textures, and animations into `GeckoLibCache`, and asks `ConditionManager` to classify conditional animation names. These are the two main integration points for this phase.

## Plan of Work

First, update `src/main/java/com/fox/ysmu/model/resource/RawYsmModelAdapter.java` so it copies every player animation controller file into the legacy animation map with a reserved key. If the raw controller came from an OpenYSM folder, preserve its original `sourceJson`; if it came from a binary `.ysm`, synthesize equivalent controller JSON from the already parsed `RawYsmModel.RawAnimationController` structures.

Second, add client-side controller support classes under `src/main/java/com/fox/ysmu/client/animation/controller`. These classes will parse controller JSON, store definitions by model animation `ResourceLocation`, map OpenYSM controller names to the existing controller slots, evaluate common boolean expressions, and maintain per-player controller state keyed by player UUID and model id.

Third, update `ClientModelManager.registerAnimations` to separate normal animation JSON from controller JSON. Normal animations continue to register exactly as before. Controller JSON registers through the new registry. `ClientModelManager.clearRuntimeModelCaches` must also clear controller definitions and per-player state.

Fourth, update `AnimationManager` so each predicate gives the OpenYSM controller runtime first chance for the current GeckoLib controller. If the runtime has no matching controller, cannot select an animation, or encounters an expression error, the existing fixed predicates remain the fallback.

Fifth, expand `AnimationRegister` and `EtfuturumCompat` with safe 1.7.10 mappings for Phase 5's state and query names. `climb` and `climbing` map to ladder behavior. `riptide` and `ysm.is_riptide` use a compatibility method that returns false unless an optional mod exposes an equivalent method. Backhand remains the only offhand source.

## Concrete Steps

From repository root `D:\Code\YSMU`, edit the Java files above. Do not run Gradle from the sandbox. The host verification commands for the user are:

    .\gradlew.bat test
    .\gradlew.bat runClient

For manual client verification, put an OpenYSM folder model with `files.player.animation_controllers` under `config/ysmu/custom/<model>`, start `runClient`, select that model, and exercise idle, walk, run, sneak, item use, and swing. The client log should include a line showing controller registration for the model, and render-thread crashes from malformed expressions are unacceptable.

## Validation and Acceptance

Acceptance for this phase is that host `.\gradlew.bat test` passes, controller JSON survives through the raw-to-legacy bridge, client registration stores controller definitions, and the player renderer can evaluate basic controller transitions without breaking old models. Old legacy folder models with only `main.animation.json`, `arm.animation.json`, and `extra.animation.json` must continue to animate through the existing `AnimationManager` fallback.

The expression subset accepted in this milestone includes boolean operators, parentheses, numeric comparisons, `query.` and `q.` aliases for basic player queries, `ysm.` values already available in 1.7.10, controller state variables such as `ctrl.idle`, and simple hand functions `ctrl.hold`, `ctrl.use`, and `ctrl.swing`. Unsupported controller functions should evaluate false with a warning at most once per expression. Keyframe Molang expressions also accept OpenYSM physics functions `ysm.first_order`, `ysm.second_order`, and bone accessors `ysm.bone_rot`, `ysm.bone_pos`/`ysm.bone_position`, and `ysm.bone_scale`.

## Idempotence and Recovery

The reserved controller entries are deterministic for a given raw model. Re-running `ServerModelManager.reloadPacks()` rebuilds the same cache payload. If controller evaluation causes trouble, removing or renaming the model's `animation_controllers` entry leaves the older fixed animation predicates in place. Runtime state is in memory only and is cleared when client model caches are cleared.

## Artifacts and Notes

Static-check evidence will be recorded after implementation. Gradle build and runtime verification remain host-side tasks because this repository's Gradle wrapper needs host cache and network access.

Local checks performed in this environment:

    git diff --check

This exited 0.

    rg -n "record\b|List\.of|Map\.of|Set\.of|Files\.readString|Files\.writeString|Objects\.requireNonNullElse|var " src\main\java\com\fox\ysmu\client\animation\controller src\main\java\com\fox\ysmu\model\resource\YsmControllerResources.java src\main\java\com\fox\ysmu\model\resource\RawYsmModelAdapter.java src\main\java\com\fox\ysmu\client\ClientModelManager.java src\main\java\com\fox\ysmu\client\animation\AnimationManager.java src\main\java\com\fox\ysmu\client\animation\AnimationRegister.java src\main\java\com\fox\ysmu\compat\EtfuturumCompat.java

This returned no matches. Gradle test/build verification remains a host-side task.

Additional local checks after the OpenYSM-alignment fixes:

    git diff --check

This exited 0.

    rg -n "record\b|List\.of|Map\.of|Set\.of|Files\.readString|Files\.writeString|Objects\.requireNonNullElse|var " src\main\java\com\fox\ysmu\client\animation\AnimationManager.java src\main\java\com\fox\ysmu\client\animation\condition\ConditionalHold.java src\main\java\com\fox\ysmu\client\animation\condition\ConditionalSwing.java src\main\java\com\fox\ysmu\client\animation\condition\ConditionalUse.java src\main\java\com\fox\ysmu\client\animation\condition\InnerClassify.java src\main\java\com\fox\ysmu\client\animation\controller\OpenYsmControllerExpressionEvaluator.java src\main\java\com\fox\ysmu\client\animation\controller\OpenYsmPlayerControllerRuntime.java src\main\java\com\fox\ysmu\client\entity\CustomPlayerEntity.java src\main\java\com\fox\ysmu\util\ControllerUtils.java

This returned no matches, so these edits did not introduce obvious Java 9+ library calls or Java syntax that would violate the JVM 8 target.

Additional local checks after the mid-attack and bow-eye follow-up:

    git diff --check

This exited 0.

    rg -n "record\b|List\.of|Map\.of|Set\.of|Files\.readString|Files\.writeString|Objects\.requireNonNullElse|var " src\main\java\com\fox\ysmu\client\animation\controller\OpenYsmPlayerControllerRuntime.java src\main\java\software\bernie\geckolib3\core\controller\AnimationController.java src\main\java\software\bernie\geckolib3\core\manager\AnimationData.java

This returned no matches.

Additional local checks after the physics-function follow-up:

    git diff --check

This exited 0.

    rg -n "record\b|List\.of|Map\.of|Set\.of|Files\.readString|Files\.writeString|Objects\.requireNonNullElse|var " src/main/java/com/fox/ysmu/client/animation/molang src/main/java/com/fox/ysmu/client/ClientModelManager.java src/main/java/com/fox/ysmu/client/entity/CustomPlayerEntity.java src/main/java/com/fox/ysmu/client/model/CustomPlayerModel.java src/main/java/software/bernie/geckolib3/core/molang src/main/java/software/bernie/geckolib3/core/controller/AnimationController.java src/main/java/software/bernie/geckolib3/util/json/JsonAnimationUtils.java

This only matched existing Guava `ImmutableSet.of(...)` calls in `JsonAnimationUtils`, not Java 9+ collection factories introduced by this pass.

    rg -n "[ \t]$" src/main/java/com/fox/ysmu/client/animation/molang src/main/java/software/bernie/geckolib3/core/molang/MolangStringPool.java src/main/java/software/bernie/geckolib3/core/molang/ScopedMolangVariable.java src/main/java/software/bernie/geckolib3/core/molang/functions/BonePosition.java src/main/java/software/bernie/geckolib3/core/molang/functions/BoneRotation.java src/main/java/software/bernie/geckolib3/core/molang/functions/BoneScale.java src/main/java/software/bernie/geckolib3/core/molang/functions/FirstOrder.java src/main/java/software/bernie/geckolib3/core/molang/functions/SecondOrder.java src/test/java/software/bernie/geckolib3/core/molang/MolangParserOpenYsmPhysicsTest.java

This returned no matches.

Additional local checks after the OpenYSM physics-order and sync-footer follow-up:

    git diff --check

This exited 0.

    rg -n "[ \t]$" src/main/java/com/fox/ysmu/client/animation/molang src/main/java/software/bernie/geckolib3/core/controller/AnimationController.java src/main/java/software/bernie/geckolib3/core/processor/AnimationProcessor.java src/main/java/com/fox/ysmu/model/resource/YSMBinaryDeserializer.java src/test/java/software/bernie/geckolib3/core/molang/MolangParserOpenYsmPhysicsTest.java src/test/java/com/fox/ysmu/model/resource/YsmResourceFormatTest.java

This returned no matches.

    rg -n "record\b|List\.of|Map\.of|Set\.of|Files\.readString|Files\.writeString|Objects\.requireNonNullElse|var " src/main/java/com/fox/ysmu/client/animation/molang src/main/java/software/bernie/geckolib3/core/controller/AnimationController.java src/main/java/software/bernie/geckolib3/core/processor/AnimationProcessor.java src/main/java/com/fox/ysmu/model/resource/YSMBinaryDeserializer.java src/test/java/software/bernie/geckolib3/core/molang/MolangParserOpenYsmPhysicsTest.java src/test/java/com/fox/ysmu/model/resource/YsmResourceFormatTest.java

This returned no matches.

## Interfaces and Dependencies

The implementation will add these client-side interfaces:

    com.fox.ysmu.client.animation.controller.OpenYsmAnimationControllerRegistry.register(ResourceLocation, Iterable<byte[]>)
    com.fox.ysmu.client.animation.controller.OpenYsmAnimationControllerRegistry.clear()
    com.fox.ysmu.client.animation.controller.OpenYsmPlayerControllerRuntime.tryApply(AnimationEvent<CustomPlayerEntity>)

The existing interfaces remain stable:

    com.fox.ysmu.model.resource.RawYsmModelAdapter.toLegacyModelData(RawYsmModel, String)
    com.fox.ysmu.client.ClientModelManager.registerAll(ModelData)
    com.fox.ysmu.client.animation.AnimationManager predicate methods

Revision note, 2026-05-10: Created this ExecPlan after reading the README and previous Phase 4 plan, before implementing Phase 5, to make the controller/Molang scope resumable from a single document.

Revision note, 2026-05-10: Updated this ExecPlan after implementation with controller resource preservation, client registry/runtime behavior, expression subset, compatibility-state additions, and static-check evidence.

Revision note, 2026-05-10: Recorded the host test failure caused by malformed test fixture JSON and the corresponding fixture correction.

Revision note, 2026-05-10: Recorded the `misc/4_default_controllers` manual-test regressions and the fixes for sword-only post-swing variables, swing fallback, `hold_on_last_frame`, and conditional hold/use loop handling.

Revision note, 2026-05-10: Updated the plan after comparing the failing sword and bow behavior against OpenYSM's real controller installer and condition classifiers. The important correction is that OpenYSM pre/post controllers are independent slots, while `swing:sword` and `use_mainhand:bow` are conditional animation names discovered by the coded swing/use predicates.

Revision note, 2026-05-10: Recorded the follow-up fixes for mid-attack movement restarts and bow aiming eye closure. The implementation now preserves elapsed state time when a conditional animation entry changes and preserves GeckoLib controller registration order.

Revision note, 2026-05-10: Recorded the physics-function follow-up. The implementation now supports OpenYSM first/second-order physics functions, bone accessors, scoped `v.*` runtime values, timeline Molang instruction execution for hair/chest secondary motion, and a focused parser regression test.

Revision note, 2026-05-10: Recorded the manual-feedback follow-up. The implementation now matches OpenYSM's timeline-before-bone ordering, short-loop remaining event execution, active bone queue ordering, and abbreviated sync footer parsing.
