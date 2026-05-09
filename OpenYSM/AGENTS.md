# AGENTS.md

This file gives repository-specific guidance for coding agents working on
OpenYSM. Follow it in addition to any higher-priority instructions.

## ExecPlans

When writing complex features or significant refactors, use an ExecPlan (as
described in `.agent/PLANS.md`) from design to implementation.

ExecPlans are not needed for small, localized fixes or documentation-only
changes. If an ExecPlan is needed, read `.agent/PLANS.md` first and keep the
plan self-contained and up to date while implementing.

## Project Snapshot

OpenYSM is a Minecraft Forge mod for Minecraft 1.20.1. It is based on Yes Steve
Model 2.6.5 and implements custom player, projectile, vehicle, animation, model
pack, audio, network sync, and compatibility features.

Important identifiers:

- Mod id: `yes_steve_model`
- Java package: `com.elfmcys.yesstevemodel`
- Version in Gradle and `mods.toml`: `2.6.5-forge+mc1.20.1`
- Network protocol version in `NetworkHandler`: `2.6.0`
- Java toolchain: Java 17
- Gradle wrapper: Gradle 8.8
- Forge: `net.minecraftforge:forge:1.20.1-47.4.0`
- Mappings: official Mojang mappings for `1.20.1`

## Build And Run

Use the Gradle wrapper from the repository root.

Common Windows commands:

- `.\gradlew.bat compileJava`
- `.\gradlew.bat build`
- `.\gradlew.bat runClient`
- `.\gradlew.bat runServer`
- `.\gradlew.bat runClient2`

Common Unix-like equivalents:

- `./gradlew compileJava`
- `./gradlew build`
- `./gradlew runClient`
- `./gradlew runServer`
- `./gradlew runClient2`

There is no checked-in `src/test` source set at the time of this writing. For
code changes, DO NOT run commands; request the user to run commands and provide
feedback.

Local mod compat dependencies are provided as jars in `libs/` through a `flatDir`
repository.

## Repository Map

Top-level files and directories:

- `build.gradle` configures ForgeGradle, Mixin, JarJar, local `libs/`
  dependencies, run configurations, and Java 17.
- `settings.gradle` configures plugin repositories and Foojay toolchain
  resolution.
- `gradle.properties` sets `org.gradle.jvmargs=-Xmx3G` and disables the Gradle
  daemon.
- `src/main/java` contains mod Java sources.
- `src/main/resources` contains Forge metadata, Mixin config, lang files,
  sounds, item tags, and built-in YSM model packs.
- `libs` contains compile-only and implementation jars for optional mod
  integrations.
- `run/`, `build/`, `.gradle/`, and IDE folders are generated/local and should
  not be committed.

Primary Java package areas:

- `YesSteveModel.java`: Forge mod entry point, mod constants, Gson, config
  registration, and Android launcher detection for UI behavior.
- `event/`: common/server/client registration events, capability attachment,
  command registration, login/logout sync hooks, server startup model loading.
- `network/NetworkHandler.java`: Forge `SimpleChannel`, packet ids, protocol
  version, send helpers, channel-version validation.
- `network/message/`: C2S and S2C packet types.
- `model/ServerModelManager.java`: server model pack folders, built-in
  extraction, blacklist processing, cache files, sync handshake, export, auth
  model state, and model validation.
- `client/ClientModelManager.java`: client model cache, server handshake,
  model parsing, default model loading, GUI callbacks, texture upload cleanup,
  and sync state.
- `resource/`: folder/binary YSM deserializers, serializer, client mapper, raw
  model POJOs, and model metadata classes.
- `client/model/`: runtime model assembly and processing pipeline.
- `client/renderer/`, `client/entity/`, `client/texture/`, `client/upload/`:
  render path, texture upload, resource lifetime, and entity model wrappers.
- `client/gui/` and `client/input/`: model selection/config/debug GUI and key
  mappings.
- `client/compat/`: optional integrations for Curios, FirstPerson, Oculus,
  Parcool, Create, TACZ, Superb Warfare, Touhou Little Maid, Better Combat,
  Carry On, SlashBlade, SWEM, Jade/TOP, and other mods represented in `libs/`.
- `capability/`: Forge capabilities for player model info, auth/star models,
  projectile/vehicle model state, and client-side animatable entities.
- `mixin/`: core and client Mixins. Extra Create and Parcool accessors are
  added dynamically by `mixin/plugin/MixinTweaker.java`.
- `geckolib3/`: vendored/customized GeckoLib-style animation and rendering
  code used by this mod.
- `molang/` and `client/animation/molang/`: MoLang parser/runtime, bindings,
  animation states, conditions, and functions.
- `audio/`: Ogg/Vorbis/Opus playback and cache support.
- `org/concentus`, `org/gagravarr`, and `rip/ysm`: vendored or ported support
  code for audio, security, zstd, algorithms, and legacy compatibility.

## Resources And Model Packs

Important resource paths:

- `src/main/resources/META-INF/mods.toml`
- `src/main/resources/yes_steve_model.mixins.json`
- `src/main/resources/assets/yes_steve_model/lang/`
- `src/main/resources/assets/yes_steve_model/builtin/`
- `src/main/resources/assets/yes_steve_model/sounds.json`
- `src/main/resources/data/yes_steve_model/tags/items/`

Built-in model groups currently include `default` and `misc`.
Model packs use files such as `ysm-pack.json`, `ysm.json`, textures,
animations, controllers, avatars, sounds, and per-pack lang files. When adding
or changing built-in packs, keep metadata, icons, translations, texture names,
and model ids consistent with the loader expectations in `ServerModelManager`,
`YSMFolderDeserializer`, and `YSMClientMapper`.

At runtime, server model data is rooted under `config/yes_steve_model`:

- `built`: extracted built-in models; cleared and recreated on each game start.
- `custom`: user-provided models.
- `auth`: authorized-model data.
- `export`: exported `.ysm` files.
- `cache/server`: server-side model cache payloads.
- `cache/client`: client-side model cache payloads.
- `blacklist.txt`: optional regex rules for skipping built-in model extraction.

Do not make code changes that delete or overwrite user `custom`, `auth`,
`export`, or cache data unless the task explicitly requires it.

## Coding Guidelines

Prefer existing local patterns over new abstractions. This codebase contains
many decompiled or ported sections, so keep edits tightly scoped and avoid
style-only rewrites in unrelated files.

Use Java 17 language features conservatively. Compile encoding is UTF-8. Many
comments and lang/resource files contain Chinese text; preserve existing
encoding and do not "fix" unrelated text while changing behavior.

Client-only Minecraft classes must stay behind client-side boundaries. Use
`Dist.CLIENT`, `@OnlyIn(Dist.CLIENT)`, client event subscribers, or guarded
access patterns already present in the codebase. Always consider dedicated
server safety before importing or referencing `net.minecraft.client.*`.

Render and texture work must respect the Minecraft render thread. Existing code
uses `Minecraft.getInstance().execute(...)`, `submit(...)`, and
`RenderSystem.assertOnGameThread()` around client state and texture changes.
Follow those patterns instead of mutating render state from packet, Netty, or
worker threads.

Server-side state changes that affect players should run on the server thread
when appropriate. The existing code uses `MinecraftServer.execute(...)` and
`ServerLifecycleHooks.getCurrentServer()` to cross from worker callbacks back to
server state.

Use `YSMThreadPool` for existing asynchronous model/cache work. Avoid blocking
the game thread or Netty packet handlers with model parsing, file IO, or large
network transfers.

Network packet changes need extra care:

- Register packets in `NetworkHandler.init()` with unique numeric ids.
- Keep C2S/S2C directions explicit.
- Only change `NetworkHandler.VERSION` when the protocol is intentionally
  incompatible.
- Preserve the channel-version handshake behavior used by
  `S2CVersionCheckPacket`, `C2SVersionCheckPacket`, and
  `NetworkHandler.isConnectionValid(...)`.
- Make packet handlers enqueue work on the correct side/thread when touching
  world, player, render, or capability state.

Capability changes need registration, attachment, serialization/copy behavior,
and sync behavior considered together. Start with `event/CommonEvent.java`,
`event/CapabilityEvent.java`, and the relevant provider/capability pair.

Command changes should follow the existing root commands:

- `/ysm` for server/common commands.
- `/ysmclient` for current-client debug commands.
- `/openysm cache dump` for OpenYSM client cache export/debug.

Server-affecting commands normally use permission checks through
`YSMMessageFormatter.hasCommandPermission(...)`. Client-only command roots guard
against non-local players with `YSMMessageFormatter.isCurrentClientPlayer(...)`.

Mixin changes require updating both Java classes and
`yes_steve_model.mixins.json`, unless they are dynamically supplied by
`MixinTweaker`. Keep dynamic optional-mod accessors conditional on the relevant
compat loader checks.

Compatibility integrations should stay optional. Do not make an optional
integration mandatory unless the Gradle dependency and `mods.toml` dependency
are intentionally changed. Most jars in `libs/` are `compileOnly`; runtime
presence is detected with `ModList` or `LoadingModList`.

Avoid editing vendored or third-party-derived code unless the task is
specifically about it:

- `org/concentus`
- `org/gagravarr`
- `rip/ysm`
- `geckolib3`

## Validation Guidance

Choose validation based on the touched area:

- Java-only logic: run `.\gradlew.bat compileJava`.
- Forge metadata, resources, Mixin config, JarJar, or packaging: run
  `.\gradlew.bat build`.
- Client rendering, GUI, keybinds, texture upload, model assembly, or
  client-only compat: run `.\gradlew.bat runClient`.
- Server model loading, commands, capabilities, auth models, or packet sync:
  run `.\gradlew.bat runServer` and at least one client.
- Multiplayer sync or tracking behavior: run `runServer`, `runClient`, and
  `runClient2` when feasible.

Do not run the command. Instead, request the user to perform the build test
according to the guidance and report the results.
