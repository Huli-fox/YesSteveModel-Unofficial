# AGENTS.md

## Project Snapshot

YSMU is a Minecraft Forge 1.7.10 mod that ports Yes Steve Model/OpenYSM-style player models back to 1.7.10. The mod id is `ysmu`, the root package is `com.fox.ysmu`, and the Forge entry point is `src/main/java/com/fox/ysmu/ysmu.java`.

The build uses the GTNH Gradle convention plugin through `settings.gradle.kts` and `build.gradle.kts`. `gradle.properties` targets Minecraft `1.7.10`, Forge `10.13.4.1614`, MCP stable `12`, enables Mixins, enables Jabel modern Java syntax while still targeting JVM 8, and shades/relocates Jackson. Runtime/development dependencies are declared in `dependencies.gradle`.

## ExecPlans

When writing complex features or significant refactors, use an ExecPlan (as described in .agent/PLANS.md) from design to implementation.

## Repository Layout

- `src/main/java/com/fox/ysmu`: project-specific mod code.
- `src/main/java/com/fox/ysmu/client`: client-only rendering, GUI, keybinds, animation predicates, texture/model registration, and upload state.
- `src/main/java/com/fox/ysmu/model`: server-side model discovery, built-in model copying, cache generation, and folder/`.ysm` model format handling.
- `src/main/java/com/fox/ysmu/network`: Forge `SimpleNetworkWrapper` setup and packet classes.
- `src/main/java/com/fox/ysmu/eep`: 1.7.10 `IExtendedEntityProperties` state for selected model/texture, active animation, and starred models.
- `src/main/java/com/fox/ysmu/event`: GTNHLib event subscribers for common player sync and client rendering events.
- `src/main/java/com/fox/ysmu/compat`: optional-mod compatibility wrappers. Keep Backhand, Et Futurum, and similar direct calls behind these wrappers.
- `src/main/java/com/fox/ysmu/mixin`: Mixins only. `gradle.properties` restricts Mixins to package `com.fox.ysmu.mixin`.
- `src/main/java/software/bernie`, `src/main/java/com/eliotlash`, and `src/main/java/net/geckominecraft`: vendored/ported GeckoLib, Molang/math, and legacy adapter code. Treat these as third-party compatibility code and keep edits narrow.
- `src/main/resources/assets/ysmu/custom`: built-in model assets copied into `config/ysmu/custom` during reload.
- `src/main/resources/assets/ysmu/lang`: `en_US.lang` and `zh_CN.lang`. Keep new translation keys in sync.
- `src/main/resources/mixins.ysmu.json`: Mixin config. Currently only `MixinItemRenderer` is listed as a client Mixin.
- `src/main/resources/META-INF/*_at.cfg`: access transformers for Minecraft/GeckoLib internals.
- `tools/convert_new_ysm.py` and `tools/convert.md`: conversion utility and documentation for newer OpenYSM-style model directories.

## Runtime Flow

Startup begins in `ysmu.java`. `CommonProxy.preInit` loads `Config`, calls `ServerModelManager.reloadPacks()`, and logs the version. `CommonProxy.init` registers network packets through `NetworkHandler.init()`. `ClientProxy.init` additionally registers animation states/Molang variables, the custom player renderer, and key bindings.

`ServerModelManager.reloadPacks()` creates `config/ysmu`, `custom`, `export`, and `cache` directories, force-copies built-in models into `config/ysmu/custom`, initializes `cache/server/PASSWORD`, and rebuilds encrypted server cache files for both folder models and `.ysm` files.

Model sync starts with the server sending `RequestSyncModel`. The client replies with cached MD5 names via `SyncModelFiles`. The server sends an encrypted password (`SendModelPassword`), asks the client to load cache hits (`RequestLoadModel`), and sends missing cache files (`SendModelFile`). The client decrypts and registers models through `ClientModelManager.registerAll()`.

Client rendering cancels vanilla `RenderPlayerEvent.Pre` in `ClientEventHandler` and delegates to `CustomPlayerRenderer`. `CustomPlayerRenderer` chooses model/texture state from `ExtendedModelInfo` or NPC overrides, posts `SpecialPlayerRenderEvent`, then renders through the GeckoLib replacement renderer. First-person hand rendering is split between `RenderHandEvent` and the Angelica-specific `MixinItemRenderer` path.

## Model and Resource Rules

Folder models live under `config/ysmu/custom/<model name>` and must include `main.json`, `arm.json`, and at least one `.png`. Optional animation files are `main.animation.json`, `arm.animation.json`, and `extra.animation.json`; missing animation files fall back to the built-in default animations.

`.ysm` files in `config/ysmu/custom` are also scanned. Only files containing `main.json`, `arm.json`, and at least one `.png` are cached.

`ModelIdUtil` normalizes model names for `ResourceLocation`. Safe ids match `[a-z0-9._-]+`; unsafe names are encoded as `_name_` plus UTF-8 hex. Use `ModelIdUtil` helpers instead of hand-building model, main, arm, or texture ids.

Built-in model assets in `src/main/resources/assets/ysmu/custom` are copied into the runtime config directory on reload. Be careful when changing these assets because the runtime reload path intentionally overwrites the built-in copies.

## Network and Threading Rules

Packet ids in `NetworkHandler` are part of the wire protocol. Add new ids when needed; do not renumber existing ids. Keep packet side registration explicit and consistent with the handler behavior.

Do not perform heavy file IO, encryption/decryption, or model parsing directly on a packet handler path. Existing code uses `ThreadTools.THREAD_POOL` for background work and `Minecraft.func_152344_a(...)` when client-side model or texture registration must return to the client thread.

The model password must be available before cached model files can decrypt. Preserve the `SendModelPassword` before `RequestLoadModel` relationship and the retry behavior in `RequestLoadModel`.

## Compatibility Notes

Runtime prerequisites from the README are UniMixins and GTNHLib. Development/runtime extras include NotEnoughItems, Nashorn, Angelica, Backhand, Et Futurum, Jackson, and JUnit as declared in `dependencies.gradle`.

Use `@EventBusSubscriber` from GTNHLib for event subscribers following the existing pattern. Client-only subscribers should specify `side = Side.CLIENT`.

Use `BackhandCompat`, `EtfuturumCompat`, and `AngelicaCompat` rather than scattering optional-mod API calls through core logic. Keep compatibility checks resilient when the optional mod is absent.

## Coding Conventions

Follow `.editorconfig`: UTF-8, LF line endings, 4-space Java indentation, 2-space Markdown/JSON/YAML indentation, final newline, and no trailing whitespace except in `.lang` files.

On Windows PowerShell, read UTF-8 files with `-Encoding UTF8`; otherwise Chinese README, comments, and lang files may display as mojibake.

Modern Java syntax is enabled by Jabel, and the code already uses pattern variables. The produced mod still targets JVM 8, so avoid Java 9+ library APIs unless the project already provides or shades them.

Preserve existing public names and legacy casing, including the lowercase `ysmu` mod class. Avoid broad rewrites in vendored GeckoLib/Molang code unless the task specifically requires it.

When adding user-facing text, update both `en_US.lang` and `zh_CN.lang`. When adding config fields, update `Config`, the relevant GUI screen if applicable, and translation keys.

When adding model animation states, register names and priorities through `AnimationRegister`/`AnimationManager`, and ensure `ConditionManager.addTest` can classify conditional animation names.

## Gradle and Verification

Do not run Gradle commands from the sandbox. This environment cannot reliably execute the wrapper because Gradle needs host cache/network access outside the workspace. When a change needs build, test, or run verification, ask the user to execute the exact command and paste the output.

Useful commands for the user to run from the repository root:

- `.\gradlew.bat build`
- `.\gradlew.bat test`
- `.\gradlew.bat runClient`
- `.\gradlew.bat runServer`

There are currently no `src/test` Java test sources, although JUnit 5 dependencies are configured. CI delegates build/test and tagged releases to reusable GTNH workflows in `.github/workflows`.
