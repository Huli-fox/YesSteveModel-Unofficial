# Phase 0 Baseline and Samples

Date: 2026-05-09

This note records the Phase 0 baseline from `README.md`: preserve the current 1.7.10 behavior, pick OpenYSM model samples for future acceptance, and verify that the temporary conversion script can degrade those samples to the legacy YSMU folder layout before native `ysm.json` support exists.

## Current 1.7.10 Baseline

Startup still enters through `src/main/java/com/fox/ysmu/ysmu.java`. `CommonProxy.preInit` calls `ServerModelManager.reloadPacks()`, which creates `config/ysmu/custom`, `config/ysmu/export`, `config/ysmu/cache/server`, and `config/ysmu/cache/client`, force-copies built-in models into `custom`, initializes `cache/server/PASSWORD`, and rebuilds cache data from `custom`.

Legacy folder models are scanned by `src/main/java/com/fox/ysmu/model/format/FolderFormat.java`. A valid folder model must contain non-empty `main.json`, non-empty `arm.json`, and at least one `.png`. Missing `main.animation.json`, `arm.animation.json`, and `extra.animation.json` fall back to the built-in default animation files under `config/ysmu/custom/default`.

Legacy `.ysm` files are scanned by `src/main/java/com/fox/ysmu/model/format/YsmFormat.java`. A valid archive must contain `main.json`, `arm.json`, and at least one `.png`. Missing animation files also fall back to the built-in default animation files.

The built-in resource baseline under `src/main/resources/assets/ysmu/custom` currently includes:

- `alex`
- `default`
- `default_boy`
- `qingluka`
- `steve`
- `wine_fox`

Model sync remains on the existing MD5/AES path: `RequestSyncModel` asks the client for cached MD5 names, `SyncModelFiles` compares them on the server, `SendModelPassword` sends the password before `RequestLoadModel` asks the client to decrypt cached hits, and `SendModelFile` transfers misses. The relevant registrations are in `src/main/java/com/fox/ysmu/network/NetworkHandler.java`.

Client rendering still cancels vanilla `RenderPlayerEvent.Pre` in `src/main/java/com/fox/ysmu/client/ClientEventHandler.java` and delegates to `src/main/java/com/fox/ysmu/client/renderer/CustomPlayerRenderer.java`. First-person hand rendering still goes through `RenderHandEvent` unless Angelica uses the shader hand renderer path.

## OpenYSM Samples

The OpenYSM built-in sample root is `OpenYSM/src/main/resources/assets/yes_steve_model/builtin`.

Chosen acceptance samples:

- `default`: default package with main/arm models, main/arm/extra/fp-arm animations, two player textures, GUI images, projectile resources, and vehicle resources.
- `misc/1_alex`: player-only `ysm.json` package with main/arm models, one main animation, and one PNG texture.
- `misc/4_default_controllers`: package with player models, main/arm animations, one PNG texture, `animation_controllers`, and configurable extra animation metadata.

Additional built-in samples also verified:

- `misc/2_steve`
- `misc/3_default_boy`

## Conversion Dry Run

Command run from repository root:

    python tools\convert_new_ysm.py OpenYSM\src\main\resources\assets\yes_steve_model\builtin --recursive --dry-run

Result after updating `tools/convert_new_ysm.py` to treat missing animation files as optional:

    [DRY] ...\builtin\default -> ...\builtin\ysmu_convert\default (2 texture(s))
    [DRY] ...\builtin\misc\1_alex -> ...\builtin\ysmu_convert\1_alex (1 texture(s))
    [DRY] ...\builtin\misc\2_steve -> ...\builtin\ysmu_convert\2_steve (1 texture(s))
    [DRY] ...\builtin\misc\3_default_boy -> ...\builtin\ysmu_convert\3_default_boy (2 texture(s))
    [DRY] ...\builtin\misc\4_default_controllers -> ...\builtin\ysmu_convert\4_default_controllers (1 texture(s))
    done: 5 converted, 0 failed. output: ...\builtin\ysmu_convert

No conversion output was written because `--dry-run` was used.

## Phase 0 Adjustment

Initial dry-run failed for `misc/1_alex`, `misc/2_steve`, `misc/3_default_boy`, and `misc/4_default_controllers` because the conversion script required animation files that are optional in the legacy runtime. The script now omits missing optional animation files from converted output, allowing `FolderFormat` and `YsmFormat` to apply their existing default animation fallback. `tools/convert.md` was updated to document this behavior.

## Manual Verification Still Required

Per repository instructions, Gradle commands were not run in this environment. To complete the runtime evidence for Phase 0, run these from the repository root on the host machine and save the output plus screenshots:

    .\gradlew.bat build
    .\gradlew.bat runClient

In `runClient`, verify that the current release can load legacy folder models from `config/ysmu/custom`, display the default model, use at least one legacy `.ysm` if available, sync models in a multiplayer or integrated-server flow, and render first-person hands without regressing the existing Angelica-compatible path.

Manual Verification PASSED. --User, 2026-05-09
