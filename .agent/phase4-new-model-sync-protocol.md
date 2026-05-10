# Phase 4 New Model Sync Protocol

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document follows `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

Phase 4 adds an OpenYSM-style multiplayer model sync path to the 1.7.10 mod without removing the existing MD5/AES sync path. After this work, a server can advertise OpenYSM raw models with hash pairs, the client can validate a local OpenYSM cache folder, request only missing models, receive missing model cache files in chunks, and register the downloaded model through the Phase 3 `RawYsmModelAdapter` bridge. A human can see this working by joining a dedicated server twice: the first join logs missing OpenYSM model downloads, and the second join logs cache hits.

The old protocol remains active as a fallback during this phase. That fallback keeps legacy folder models and legacy `.ysm` models available while the new protocol proves the OpenYSM hash/cache/chunk path.

## Progress

- [x] (2026-05-10 00:00+08:00) Read `README.md`, `.agent/PLANS.md`, and `.agent/phase3-client-registration-bridge.md` to confirm Phase 4 scope and the Gradle restriction.
- [x] (2026-05-10 00:00+08:00) Inspected existing network packets, legacy model cache flow, server model scanning, client registration, and already-ported OpenYSM crypto/cache utilities.
- [x] (2026-05-10 00:00+08:00) Added OpenYSM sync metadata and server cache generation alongside existing legacy cache files.
- [x] (2026-05-10 00:00+08:00) Appended new packet ids in `NetworkHandler` for `C2SModelSyncPayload17`, `S2CModelSyncPayload17`, `C2SVersionCheck17`, `S2CVersionCheck17`, and `C2SCompleteFeedback17`.
- [x] (2026-05-10 00:00+08:00) Implemented server and client sync state machines for packets 01, 02, 03, 04, and 05 using background work for crypto, file IO, and parsing.
- [x] (2026-05-10 00:00+08:00) Added server config fields for sync thread count, bandwidth limit, timeout, low-bandwidth mode, sound effect acceptance, and an additive protocol enable switch.
- [x] (2026-05-10 00:00+08:00) Added focused tests for new message round trips and stable client cache key derivation.
- [x] (2026-05-10 00:00+08:00) Recorded validation commands the user must run on the host.

## Surprises & Discoveries

- Observation: Phase 1 already ported the OpenYSM security and cache primitives needed for this phase.
  Evidence: `src/main/java/rip/ysm/security/YsmCrypt.java`, `YSMClientCache.java`, and `YSMByteBuf.java` exist and have tests in `src/test/java/rip/ysm/security/YsmFoundationTest.java`.
- Observation: Phase 3 already converts `RawYsmModel` into legacy `ModelData`.
  Evidence: `RawYsmModelAdapter.toLegacyModelData` is used by `OpenYsmFormat` before writing legacy cache files, and `ClientModelManager.registerAll` can register that `ModelData`.
- Observation: The existing client join event starts legacy sync itself, and the server login event also asks the client to sync.
  Evidence: `ClientEventHandler.onClientPlayerJoinWorld` calls `ClientModelManager.sendSyncModelMessage()`, while `CommonEventHandler.onPlayerLoggedIn` calls `ServerModelManager.sendRequestSyncModelMessage`.
- Observation: The server can generate OpenYSM server-cache files from the same `RawYsmModel` that Phase 3 uses for legacy `ModelData`.
  Evidence: `ModelCacheWriter.writeOpenYsm` serializes with `YSMBinarySerializer.serialize(raw, 32, true)`, encrypts with `YsmCrypt.encryptServerCache`, and stores `OpenYsmSyncInfo` in `ServerModelManager.OPEN_YSM_SYNC_INFO`.
- Observation: The new client payload handler must be serialized even though Forge packet receipt is ordered.
  Evidence: `S2CModelSyncPayload17.Handler` submits each payload to `ThreadTools.THREAD_POOL`; `OpenYsmModelSyncClient.processServerData` is synchronized so packet 05 chunks cannot race while updating `bytesReceived`.

## Decision Log

- Decision: Keep the legacy MD5/AES protocol active while adding the OpenYSM protocol.
  Rationale: Legacy-only models are still important, and Phase 4 acceptance explicitly says to preserve the old sync as fallback until the new path is stable. Running the new protocol additively lets OpenYSM cache behavior be tested without risking the Phase 3 bridge.
  Date/Author: 2026-05-10 / Codex
- Decision: Use a stable per-server OpenYSM client cache key derived from `server_index` instead of a fresh random key per connection.
  Rationale: OpenYSM client cache file names depend on the runtime cache key. A fresh key on every join would prevent second-join cache hits, which is part of the Phase 4 acceptance behavior.
  Date/Author: 2026-05-10 / Codex
- Decision: Use throttled chunk sending instead of OpenYSM's modern Netty outbound-buffer reliable send.
  Rationale: Minecraft 1.7.10 does not expose the same modern `Connection.channel().unsafe().outboundBuffer()` path used upstream. The README directs this phase to use chunk sizing, a queue, ack, or throttling instead.
  Date/Author: 2026-05-10 / Codex
- Decision: Send packet ids 14, 15, and 16 serverbound, and 17 and 18 clientbound.
  Rationale: Existing packet ids 0-13 and 93-96 are part of the current wire protocol. The new ids append after 13 and do not disturb the Bukkit/NPC ids in the 90s.
  Date/Author: 2026-05-10 / Codex
- Decision: Keep new protocol startup additive by sending `S2CVersionCheck17` before the existing `RequestSyncModel`.
  Rationale: This lets the new OpenYSM cache path run while the old path still loads legacy-only models and provides fallback registration during Phase 4.
  Date/Author: 2026-05-10 / Codex

## Outcomes & Retrospective

This pass implemented an additive OpenYSM protocol path. `OpenYsmFormat` now writes both the existing legacy encrypted `ModelData` cache and a new OpenYSM server cache file for bridgeable raw models. `NetworkHandler` registers the new version, payload, and completion packets without renumbering existing ids. `OpenYsmModelSyncServer` performs packets 01, 03, and 05, and `OpenYsmModelSyncClient` performs packets 02, 04, local cache validation, chunk assembly, client cache writing, raw model parsing, and registration through `RawYsmModelAdapter`.

Legacy sync remains active. This means Phase 4 can be tested by looking for OpenYSM cache hit/miss/download logs while legacy models still load through the established path.

## Context and Orientation

The existing sync begins with `ServerModelManager.sendRequestSyncModelMessage`, which sends `RequestSyncModel` to a client. The client responds with `SyncModelFiles`, listing file names under `config/ysmu/cache/client`. The server sends `SendModelPassword`, asks the client to load cache hits with `RequestLoadModel`, and sends missing encrypted legacy cache files with `SendModelFile`. The client decrypts each legacy cache file into `ModelData` and calls `ClientModelManager.registerAll`.

The OpenYSM sync path uses different cache identifiers. A model has two 64-bit hashes derived from the model content hash and the server key in `config/ysmu/cache/server_index`. The server cache file name is the two hashes formatted as lower-case 16-character hex values. The client cache file name is generated with `YSMClientCache.generateCacheFileName(hash1, hash2, clientKey)`, and the file contents can be verified by `YSMClientCache.verifyFileContent`.

The already-ported binary serializer, `YSMBinarySerializer`, can turn a `RawYsmModel` into the clear payload expected by `YsmCrypt.encryptServerCache`. The client can reverse that with `YsmCrypt.read`, parse the clear bytes with `YSMBinaryDeserializer`, and reuse `RawYsmModelAdapter.toLegacyModelData` for rendering.

## Plan of Work

First, create a small `OpenYsmSyncInfo` data class and teach `OpenYsmFormat` to write an OpenYSM server cache file whenever it successfully parses a raw OpenYSM model. This is additive to `ModelCacheWriter.write`, so the legacy cache remains unchanged. `ServerModelManager` will keep a map from model id to `OpenYsmSyncInfo`.

Second, add the new network packet classes and append their packet ids in `NetworkHandler`. The packet payload classes only move opaque byte arrays or a version/status string; they must not perform crypto or file IO directly in packet handlers.

Third, implement server-side state in a new sync helper. It starts packet 01 after a version check, handles packet 02 and packet 04 from the client, sends packet 03 model metadata, then streams packet 05 chunks from `config/ysmu/cache/server`. File reads, encryption, and chunk loops run through `ThreadTools.THREAD_POOL`. Chunk size and throttling come from `Config`.

Fourth, implement client-side state in a client helper. It handles packet 01, sends packet 02, handles packet 03 by checking `config/ysmu/cache/client/<folder>`, requests cache misses in packet 04, and handles packet 05 chunks by assembling, transcoding to client cache, parsing the raw model, and registering it on the Minecraft client thread.

Fifth, add or update tests that do not require Minecraft startup. The most useful tests are for OpenYSM cache metadata generation and packet helper behavior. Gradle commands still must be run by the user on the host, not from this sandbox.

## Concrete Steps

From repository root `D:\Code\YSMU`, edit the Java files described above. Do not run Gradle from the sandbox. The host verification commands for the user are:

    .\gradlew.bat test
    .\gradlew.bat runServer
    .\gradlew.bat runClient

For manual multiplayer verification, run one server and one client with an OpenYSM folder model under `config/ysmu/custom/<model>/ysm.json`. On the first join, the log should include OpenYSM sync cache misses and downloaded chunks. On the second join with the same `server_index`, the log should include OpenYSM cache hits for the same model hashes.

## Validation and Acceptance

Acceptance for this phase is that host `.\gradlew.bat test` passes, a dedicated server can send OpenYSM sync metadata to a client, the first client join downloads missing OpenYSM model cache files, and the second join validates the client cache without re-downloading those files. Legacy MD5/AES sync must still work, so legacy folder models continue to appear and render.

The connection state cleanup acceptance is that player logout removes server sync state and client logout clears client sync state. Logs should show clear failure messages for invalid cache, unsupported model parse, or timeout rather than crashing a packet handler.

## Idempotence and Recovery

The added server cache files are deterministic for the same raw model hash and `server_index` key. Re-running `ServerModelManager.reloadPacks()` rebuilds the same legacy and OpenYSM cache entries. If a cache file is invalid, deleting `config/ysmu/cache/client` forces the client to request it again. If the new protocol causes trouble, disabling it in config leaves the legacy `RequestSyncModel` path available.

## Artifacts and Notes

Local checks performed in this environment:

    git diff --check

This exited 0.

    rg -n "record\b|List\.of|Map\.of|Set\.of|Files\.readString|Files\.writeString|Objects\.requireNonNullElse|var " src\main\java\com\fox\ysmu\network\sync src\main\java\com\fox\ysmu\client\sync src\main\java\com\fox\ysmu\network\message src\main\java\com\fox\ysmu\model\format src\test\java\com\fox\ysmu\network\sync

This returned no matches. Gradle test/build verification remains a host-side task.

## Interfaces and Dependencies

The implementation will add these interfaces:

    com.fox.ysmu.model.format.OpenYsmSyncInfo
    com.fox.ysmu.network.sync.OpenYsmModelSyncServer.startSync(EntityPlayerMP)
    com.fox.ysmu.network.sync.OpenYsmModelSyncServer.handlePayload(UUID, byte[])
    com.fox.ysmu.client.sync.OpenYsmModelSyncClient.handlePayload(byte[])
    com.fox.ysmu.client.sync.OpenYsmModelSyncClient.clearConnectionState()

The new message classes will be:

    com.fox.ysmu.network.message.C2SModelSyncPayload17
    com.fox.ysmu.network.message.S2CModelSyncPayload17
    com.fox.ysmu.network.message.C2SVersionCheck17
    com.fox.ysmu.network.message.S2CVersionCheck17
    com.fox.ysmu.network.message.C2SCompleteFeedback17

Revision note, 2026-05-10: Created this ExecPlan after reading the README and Phase 3 plan, before implementing Phase 4, to make the additive OpenYSM sync protocol resumable from a single document.

Revision note, 2026-05-10: Updated this ExecPlan after implementation with packet ids, cache generation behavior, synchronized client payload handling, config fields, focused tests, and static-check evidence.
