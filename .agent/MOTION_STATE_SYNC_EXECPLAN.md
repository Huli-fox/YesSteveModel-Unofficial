# Replace Player Metadata Motion Sync

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This plan follows `PLANS.md` in the repository root. The goal is to remove the custom use of Minecraft 1.7.10 player metadata, also called `DataWatcher` slots, for synchronizing whether a player is on the ground or flying.

## Purpose / Big Picture

Yes Steve Model uses player ground and flying state to pick animations such as `jump`, `walk`, `run`, and `fly`. The current implementation stores those two booleans in player `DataWatcher` id `28`. In Minecraft 1.7.10, `DataWatcher` ids are a limited shared namespace on the entity, so another mod using the same id can overwrite or misread this mod's data. After this change, the same animation decisions are driven by a YSMU-owned network packet and client cache, so no player metadata slot is consumed.

The behavior should be visible in multiplayer: remote players still switch between ground, jump, and flight animations, but the code no longer calls `addObject`, `getWatchableObjectByte`, or `updateObject` for this custom state.

## Progress

- [x] (2026-05-01 15:52 +08:00) Read `PLANS.md`, `CommonEventHandler`, `AnimationRegister`, and `NetworkHandler`.
- [x] (2026-05-01 15:52 +08:00) Identified the unsafe metadata path: server tick writes `DataWatcher` id `28`; client animation predicates read the same id for remote players.
- [x] (2026-05-01 15:58 +08:00) Added `PlayerMotionState` for packing on-ground and flying booleans into a byte without binding it to player metadata.
- [x] (2026-05-01 15:58 +08:00) Added `RemotePlayerMotionStates` client cache keyed by player UUID and `SyncPlayerMotionState` clientbound message.
- [x] (2026-05-01 16:00 +08:00) Registered the new message with clientbound packet id `11`, sent current state on start tracking, and sent dirty changes from server tick.
- [x] (2026-05-01 16:00 +08:00) Changed animation predicates and Molang query values to read the client cache for remote players, with local-player direct reads preserved.
- [x] (2026-05-01 16:00 +08:00) Removed custom DataWatcher registration and access for this feature.
- [x] (2026-05-01 16:06 +08:00) Ran source-level checks: old motion DataWatcher search has no hits under `src/main/java/com/fox/ysmu`, and `git diff --check` passes.
- [ ] User should run `.\gradlew.bat build` and report the result, because project build/test commands are user-run in this sandbox.

## Surprises & Discoveries

- Observation: The project already has a note saying `CommonEventHandler.updateData()` writes `DataWatcher` id `28` once per tick to avoid flag overwrite, but that still leaves a cross-mod id collision risk.
  Evidence: `CommonEventHandler` declares `MOTION_DATAWATCHER_ID = 28`, `ON_GROUND = 0x01`, and `IS_FLYING = 0x02`; `AnimationRegister` reads the byte for non-local players.

- Observation: `SimpleNetworkWrapper` in the generated Minecraft sources supports `sendToDimension(IMessage, int)`.
  Evidence: `build/rfg/minecraft-src/java/cpw/mods/fml/common/network/simpleimpl/SimpleNetworkWrapper.java` contains `public void sendToDimension(IMessage message, int dimensionId)`.

- Observation: The sandbox could not run Gradle because the wrapper needed access to an external Gradle distribution lock file.
  Evidence: `.\gradlew.bat build` failed with `java.io.FileNotFoundException: E:\JetBrains\cache\.gradle\wrapper\dists\gradle-8.13-bin\...\gradle-8.13-bin.zip.lck (拒绝访问。)`. The user instructed that builds and tests are user-run for this workspace.

## Decision Log

- Decision: Replace `DataWatcher` storage with an explicit YSMU clientbound packet rather than another entity metadata field.
  Rationale: The packet id namespace belongs to this mod's `SimpleNetworkWrapper`, so it avoids collisions with other mods' player metadata slots.
  Date/Author: 2026-05-01 / Codex

- Decision: Key the client cache by player UUID instead of entity id.
  Rationale: The animation code already has an `EntityPlayer`, so UUID lookup is direct and less vulnerable to stale entity id reuse after world transitions.
  Date/Author: 2026-05-01 / Codex

- Decision: Send updates only when the packed state changes, plus send the current state when a client starts tracking a player.
  Rationale: This keeps network traffic low while giving newly tracking clients an immediate baseline even if the player is standing still and no dirty tick occurs.
  Date/Author: 2026-05-01 / Codex

- Decision: Broadcast dirty state changes to all clients in the same dimension, while keeping start-tracking direct sync for immediate baselines.
  Rationale: A 64-block radius can be narrower than the server's player tracking range. The packet is only a UUID plus one byte and is sent only on state changes, so same-dimension delivery is a pragmatic way to avoid missed updates without using entity metadata.
  Date/Author: 2026-05-01 / Codex

## Outcomes & Retrospective

Implementation is complete at the source level. The motion state no longer uses player metadata; it is packed by `PlayerMotionState`, sent by `SyncPlayerMotionState`, cached by `RemotePlayerMotionStates`, and read by `AnimationRegister`. Build validation remains pending user execution because Gradle access is outside this sandbox.

## Context and Orientation

`src/main/java/com/fox/ysmu/event/CommonEventHandler.java` is the server-side event hub. It registers player extended properties, sends model info when a player starts tracking another player, and runs player tick logic. The unsafe code is in `registerPlayerProperties`, which calls `player.getDataWatcher().addObject(28, ...)`, and `updateData`, which writes the packed byte each server tick.

`src/main/java/com/fox/ysmu/client/animation/AnimationRegister.java` is the client-side animation state registry. It decides whether remote players are on the ground or flying through `isPlayerOnGround` and `isPlayerFlying`. Those helpers currently read the same `DataWatcher` byte for every non-local player.

`src/main/java/com/fox/ysmu/network/NetworkHandler.java` owns the `SimpleNetworkWrapper` named `ysmu_network`. Each packet class must have a unique integer id and be registered for either `Side.CLIENT` or `Side.SERVER`.

In this plan, "packed state" means one byte where bit `0x01` means the server says the player is on the ground, and bit `0x02` means the server says the player is flying. The packed byte is still useful because it is compact, but it is no longer stored in entity metadata.

## Plan of Work

Create a common helper class under `src/main/java/com/fox/ysmu/data/` that defines the two bit flags and a method that packs `EntityPlayer.onGround` and `EntityPlayer.capabilities.isFlying` into one byte. This helper must not mention `DataWatcher`.

Create a client-only cache class under `src/main/java/com/fox/ysmu/client/animation/` that stores packed motion state by `UUID`. It should expose methods to update, clear, and query on-ground/flying state for an `EntityPlayer`, returning the player's direct fields as a fallback before the first packet arrives.

Create `src/main/java/com/fox/ysmu/network/message/SyncPlayerMotionState.java`. It should serialize a player's UUID and packed byte, and its client handler should update the cache.

Update `NetworkHandler` to assign a new clientbound packet id without renumbering existing packets. Packet id `11` is unused and fits between `10` and `12`, so use it for the new message.

Update `CommonEventHandler` so player construction no longer registers a `DataWatcher` object. Replace `updateData` with logic that computes the packed state, compares it with a server-side map of last sent states keyed by player UUID, and sends `SyncPlayerMotionState` to the player's current dimension when changed. Also send the current state in `onStartTracking` after model info is sent.

Update `AnimationRegister` to remove the static import from `CommonEventHandler` and to query the new client cache for remote-player on-ground and flying state.

Clear the client cache when the client player logs out, alongside the existing NPC data clear in `ClientEventHandler`.

## Concrete Steps

From repository root `D:\Code\YesSteveModel-Unofficial`, edit these files:

    src/main/java/com/fox/ysmu/data/PlayerMotionState.java
    src/main/java/com/fox/ysmu/client/animation/RemotePlayerMotionStates.java
    src/main/java/com/fox/ysmu/network/message/SyncPlayerMotionState.java
    src/main/java/com/fox/ysmu/network/NetworkHandler.java
    src/main/java/com/fox/ysmu/event/CommonEventHandler.java
    src/main/java/com/fox/ysmu/client/animation/AnimationRegister.java
    src/main/java/com/fox/ysmu/client/ClientEventHandler.java

After edits, ask the user to run:

    .\gradlew.bat build

In this workspace, build and test commands are user-run because the sandbox may not access the local Gradle cache.

## Validation and Acceptance

Ask the user to run `.\gradlew.bat build` from the repository root. The build should compile the new packet, helper, and animation references without errors. Search the Java sources for `MOTION_DATAWATCHER_ID`, `getWatchableObjectByte`, and `updateObject` in YSMU code; none should remain for this feature. This source search has already passed under `src/main/java/com/fox/ysmu`.

For a manual multiplayer check, start a server and two clients with this mod. Client A should observe Client B switching into `walk` or `run` on ground movement, `jump` when leaving the ground, and `fly` when creative flight is enabled. No custom player metadata slot should be registered by YSMU.

## Idempotence and Recovery

The edits are additive until the old `DataWatcher` path is removed. Running the build repeatedly is safe. If a packet id conflict appears, choose another unused id in `NetworkHandler` without changing any existing ids. If a client joins and sees a default state before the first packet, the cache fallback uses the vanilla local entity fields until the server packet arrives.

## Artifacts and Notes

Important expected source-level evidence after completion:

    rg -n "MOTION_DATAWATCHER_ID|getWatchableObjectByte|updateObject\\(" src/main/java/com/fox/ysmu

The command does not report the old motion-sync implementation. `git diff --check` also passes.

## Interfaces and Dependencies

`com.fox.ysmu.data.PlayerMotionState` must provide:

    public static final int ON_GROUND = 0x01;
    public static final int IS_FLYING = 0x02;
    public static byte pack(EntityPlayer player);
    public static boolean isOnGround(byte flags);
    public static boolean isFlying(byte flags);

`com.fox.ysmu.client.animation.RemotePlayerMotionStates` must provide:

    public static void update(UUID playerId, byte flags);
    public static void clear();
    public static boolean isOnGround(EntityPlayer player);
    public static boolean isFlying(EntityPlayer player);

`com.fox.ysmu.network.message.SyncPlayerMotionState` must be a clientbound `IMessage` containing two UUID longs and one byte of flags.

Revision note, 2026-05-01: Initial plan created before implementation to satisfy the repository ExecPlan requirement for significant refactors.

Revision note, 2026-05-01: Updated progress and decision log after implementing the packet/cache approach and changing dirty updates to same-dimension delivery.

Revision note, 2026-05-01: Updated validation status after source checks passed and the user clarified that Gradle build/test commands are user-run in this sandbox.
