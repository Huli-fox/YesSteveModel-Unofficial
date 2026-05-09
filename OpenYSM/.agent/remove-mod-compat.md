# Remove optional mod compatibility code

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document is maintained according to `.agent/PLANS.md` in the repository root. It is self-contained so a future agent or developer can continue the work after context compaction.

## Purpose / Big Picture

OpenYSM currently contains many optional compatibility integrations. In this plan, "compatibility integration" means code or data that lets OpenYSM and a separate mod run independently, but adds extra behavior when both are loaded. The target branch is dedicated to porting OpenYSM to older Minecraft versions where those partner mods are not available, so the branch should stop compiling against, loading, detecting, warning about, rendering for, or exposing MoLang/controller APIs for those optional mods.

After this change, OpenYSM should build using only Minecraft Forge, its own source, and libraries that are still part of the core mod. Loading additional mods should not activate OpenYSM-specific extra behavior. Built-in model packs should no longer ship animation files or tags whose only purpose is partner-mod integration.

## Progress

- [x] (2026-05-09 Asia/Shanghai) Read `.agent/PLANS.md` and created this ExecPlan for the compatibility removal.
- [x] (2026-05-09 Asia/Shanghai) Identified primary integration roots: `src/main/java/com/elfmcys/yesstevemodel/client/compat`, optional mixins under `src/main/java/com/elfmcys/yesstevemodel/mixin/client/parcool` and `src/main/java/com/elfmcys/yesstevemodel/mixin/client/create`, references from client setup, animation predicates, MoLang bindings, renderers, capabilities, resources, `build.gradle`, and `mods.toml`.
- [x] (2026-05-09 Asia/Shanghai) Removed optional partner mod dependency declarations from `build.gradle` and removed the optional TACZ dependency block from `mods.toml`.
- [x] (2026-05-09 Asia/Shanghai) Simplified `ClientSetupEvent` so it no longer initializes compatibility modules or emits partner-mod compatibility warnings.
- [x] (2026-05-09 Asia/Shanghai) Simplified `MixinTweaker` so it no longer imports, initializes, or returns Create/ParCool compatibility mixins.
- [x] (2026-05-09 Asia/Shanghai) Removed Java references to `client.compat` from core code, replacing partner-mod paths with neutral vanilla behavior where needed.
- [x] (2026-05-09 Asia/Shanghai) Deleted optional Create/ParCool accessor mixins from `src/main/java/com/elfmcys/yesstevemodel/mixin/client`.
- [x] (2026-05-09 Asia/Shanghai) Deleted the `src/main/java/com/elfmcys/yesstevemodel/client/compat` package after active references were removed.
- [x] (2026-05-09 Asia/Shanghai) Removed MoLang functions and bindings that only exist for optional mods, including Curios-specific functions, TLM binding, `mod_version`, and `first_person_mod_hide`.
- [x] (2026-05-09 Asia/Shanghai) Removed built-in model pack compatibility resources and loader references for partner-specific animation channels and item tags.
- [x] (2026-05-09 Asia/Shanghai) Searched for remaining partner-mod package names, mod ids, and compatibility classes; active Java/resource references are gone, with only intentional historical/source comments left in vendored or third-party-derived code.
- [x] (2026-05-09 Asia/Shanghai) Prepared the final validation request for the user to run Gradle commands, because repository instructions say coding agents should not run build/run commands for code changes.

## Surprises & Discoveries

- Observation: Compatibility behavior is spread beyond the `client/compat` package.
  Evidence: `rg` found imports from `client.compat` in `ClientSetupEvent`, animation predicates, `CtrlBinding`, `YSMBinding`, renderers, texture mapping, geckolib-derived classes, network packets, `PlayerCapability`, and GUI files.
- Observation: Some compatibility is resource-driven.
  Evidence: built-in animation files include names such as `tac.animation.json`, `carryon.animation.json`, `parcool.animation.json`, `slashblade.animation.json`, `swem.animation.json`, and `im.animation.json`; `src/main/resources/data/yes_steve_model/tags/items/slashblade.json` references SlashBlade item ids.
- Observation: Dynamic optional mixins are supplied by code rather than listed in `yes_steve_model.mixins.json`.
  Evidence: `MixinTweaker.getMixins()` conditionally returns `client.parcool.*` and `client.create.PlayerSkyhookRendererAccessor`.
- Observation: One remaining resource integration used the `immersive_aircraft` mod id with an underscore and was not caught by the first `immersiveaircraft` search term.
  Evidence: `wine_fox/17_mini/ysm.json` referenced `immersive_aircraft:biplane`, and `wine_fox/17_mini/animations/main.animation.json` contained `vehicle$immersive_aircraft:biplane`; both references and the dedicated plane model/animation files were removed.

## Decision Log

- Decision: Treat compile dependencies, runtime mod detection, dynamic mixins, MoLang/controller APIs, render hooks, capability hooks, GUI entries, and built-in compatibility animation resources as in-scope for removal.
  Rationale: The user's goal is a porting branch that no longer considers optional partner mods. Keeping any of these pieces would preserve behavior that only exists when another mod is loaded.
  Date/Author: 2026-05-09 / Codex
- Decision: Keep Forge/Minecraft mod metadata and core libraries, and do not delete binary jars from `libs/` unless they are proven to be unnecessary repository artifacts after source cleanup.
  Rationale: The explicit request is to remove compatibility code. Removing large checked-in jars is a separate repository hygiene decision and may make review noisier. Removing references from `build.gradle` is enough to stop compiling against them.
  Date/Author: 2026-05-09 / Codex
- Decision: Bump `NetworkHandler.VERSION` from `2.6.0` to `2.6.1` after removing the Touhou Little Maid-only `entityId` field from `C2SPlayAnimationPacket`.
  Rationale: The packet wire format changed intentionally when deleting compatibility-only animation roulette behavior for non-player entities. Repository guidance says to change the protocol version when packet formats become incompatible.
  Date/Author: 2026-05-09 / Codex

## Outcomes & Retrospective

Active cleanup is complete. The source tree no longer contains optional partner mod dependencies, dynamic optional mixin registration, the `client/compat` package, partner-only MoLang/controller APIs, partner-only GUI/render/capability hooks, or built-in model resources that create partner-mod-specific OpenYSM behavior. Gradle validation was not run by the agent because repository instructions require the user to run build commands.

## Context and Orientation

This is a Minecraft Forge 1.20.1 mod. The mod id is `yes_steve_model`, and the main Java package is `com.elfmcys.yesstevemodel`.

The main compatibility package is `src/main/java/com/elfmcys/yesstevemodel/client/compat`. It contains integrations for mods such as Curios, FirstPerson, Oculus, ParCool, Create, TACZ, Superb Warfare, Touhou Little Maid, Better Combat, Carry On, SlashBlade, SWEM, Jade, The One Probe, Simple Planes, Simple Hats, Sophisticated Backpacks, Cosmetic Armor Reworked, Elytra Slot, Immersive Melodies, Immersive Aircraft, Iron's Spells, Real Camera, OptiFine detection, Accelerated Rendering, and Player Animator.

Compile-time partner mod jars are referenced in `build.gradle` through the local `libs` flatDir repository. The repository also contains optional metadata in `src/main/resources/META-INF/mods.toml`, currently including a non-mandatory TACZ dependency.

Dynamic optional mixins are handled by `src/main/java/com/elfmcys/yesstevemodel/mixin/plugin/MixinTweaker.java`. These mixins live under `src/main/java/com/elfmcys/yesstevemodel/mixin/client/parcool` and `src/main/java/com/elfmcys/yesstevemodel/mixin/client/create`.

Core code calls compatibility classes from several areas: client setup registers compatibility initialization, animation predicates branch into partner-mod states, MoLang bindings expose partner-mod query functions, render layers hide or replace partner-mod equipment, and some packet/gui code supports Touhou Little Maid entities. These references must be replaced with vanilla-only behavior before deleting the compatibility package.

Built-in model resources in `src/main/resources/assets/yes_steve_model/builtin` include partner-mod-specific animation files and animation names. `YSMFolderDeserializer` also maps partner-specific animation suffixes to numeric slots. These resources and mappings should be removed when their only purpose is optional integration.

## Plan of Work

First, remove build and metadata references to optional partner mods. In `build.gradle`, delete all `compileOnly fg.deobf('libs:...')` or `implementation fg.deobf('libs:...')` entries that refer to partner mods. Keep Minecraft Forge, ImageStream, MixinExtras, and Sponge Mixin processor dependencies. In `mods.toml`, remove optional partner dependency blocks such as TACZ.

Second, simplify client startup. In `ClientSetupEvent`, keep OpenYSM's own animation registration, key registration, overlays, and OpenGL capability check. Remove all partner compatibility imports, initialization calls, incompatible-mod warnings, and helper methods that only exist to warn about other mods.

Third, remove dynamic optional mixins. `MixinTweaker` should no longer import or initialize compatibility classes and should return `null` from `getMixins()`. Delete the ParCool and Create accessor mixin Java files because they refer to external packages that will no longer be dependencies.

Fourth, update core code that currently calls compatibility classes. Replace partner-specific behavior with neutral behavior: a removed partner-mod predicate should return false, a partner-mod texture type should become a local OpenYSM enum or a simple vanilla texture path, partner-mod equipment helpers should use vanilla equipment slots only, and partner-mod animation handlers should fall back to existing vanilla animation evaluation. Delete GUI screens or buttons that are exclusively for partner entities.

Fifth, remove MoLang and controller bindings that only query partner mods. Delete Curios-specific functions under `client/animation/molang/functions/ysm/curios`. Remove `TLMBinding` and any registration of the `tlm` binding from `GeckoLibCache`. In `CtrlBinding` and `YSMBinding`, keep generic OpenYSM and vanilla Minecraft functions, but remove functions such as backpack, carryon, tacz, bettercombat, parcool, slashblade, swem, create, immersive_melodies, ironsspellbooks, and curios bindings.

Sixth, remove built-in compatibility resources. Delete partner-specific animation files from built-in model packs. Update `YSMFolderDeserializer` and `RawYsmModel` so the default animation channels no longer list partner-specific suffixes. Remove `data/yes_steve_model/tags/items/slashblade.json` and the `SLASHBLADE` constant from `ItemTagsConstants`.

Finally, run searches for `client.compat`, partner mod ids, and partner API package names. Because repository instructions forbid the agent from running Gradle validation commands for code changes, ask the user to run `.\gradlew.bat compileJava` from `D:\Code\OpenYSM` and report results. If resources or packaging changed significantly, ask for `.\gradlew.bat build` as a follow-up.

## Concrete Steps

Work from `D:\Code\OpenYSM`.

Use `rg` to locate compatibility references:

    rg -n "client\\.compat|compat\\.|curios|oculus|parcool|touhou|tacz|superb|swem|slashblade|jade|theoneprobe|firstperson|create|carryon|bettercombat|elytraslot|simpleplanes|simplehats|realcamera|cosmetic|sophisticated|immersive|ironsspellbooks|playeranimator" src/main/java src/main/resources build.gradle

After each deletion or refactor pass, repeat the search with narrower patterns. Resolve any active Java/resource references that preserve optional integration behavior.

Do not run Gradle build commands as the agent. At the end, tell the user to run:

    .\gradlew.bat compileJava

If compile succeeds and resource packaging was changed, tell the user to run:

    .\gradlew.bat build

## Validation and Acceptance

Acceptance is reached when these conditions are true:

The source no longer contains `src/main/java/com/elfmcys/yesstevemodel/client/compat` or imports from that package. The dynamic mixin plugin no longer returns partner-mod accessors, and the optional accessor mixin files are gone. `build.gradle` no longer declares compile or implementation dependencies on partner mod jars. `mods.toml` no longer declares optional partner dependencies. Built-in resources no longer ship partner-mod-specific animation files or item tags. Searches for known partner mod ids only find acceptable historical text in comments or files intentionally left out of the active build.

The user should validate by running `.\gradlew.bat compileJava`. Expected success is a completed Gradle compile with no Java compilation errors about missing compatibility classes or external mod packages. If packaging-sensitive resources were removed, the user should also run `.\gradlew.bat build` and expect the build to complete.

## Idempotence and Recovery

Deleting compatibility code and resources is safe to repeat if the working tree is checked before each pass. If a deleted file is discovered to contain core OpenYSM behavior rather than optional integration behavior, restore only that file or equivalent vanilla logic from version control and document the reason in this plan. Do not delete user runtime data under `run/` or `config/yes_steve_model`.

## Artifacts and Notes

Initial search evidence:

    build.gradle contains many local partner mod dependencies under `libs`.
    ClientSetupEvent imports and initializes compatibility classes for more than twenty partner mods.
    MixinTweaker dynamically adds ParCool and Create accessor mixins.
    YSMFolderDeserializer maps animation keys such as carryon, parcool, swem, slashblade, tlm, and immersive_melodies.
    Built-in resources include compatibility animation files such as tac.animation.json, carryon.animation.json, parcool.animation.json, slashblade.animation.json, swem.animation.json, and im.animation.json.

## Interfaces and Dependencies

At the end of the work, OpenYSM should depend on Forge, Minecraft, ImageStream, MixinExtras, Sponge Mixin annotation processing, and project-local source only. No Java type from a partner mod should be required by main source compilation. The `MixinTweaker` class should still implement `IMixinConfigPlugin` because `yes_steve_model.mixins.json` references it, but it should no longer supply optional partner mixins.

Revision note 2026-05-09 / Codex: Created the initial plan after repository survey. The plan records the removal scope and known compatibility roots so another worker can continue without prior conversation context.

Revision note 2026-05-09 / Codex: Updated the plan after completing resource cleanup and search validation. Remaining keyword hits are historical comments in `rip/ysm/security/YsmCrypt.java` and a source-provenance URL in `geckolib3/core/keyframe/bone/BoneKeyFrame.java`; they are not active compatibility behavior.
