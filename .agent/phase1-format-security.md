# Phase 1 Format and Security Foundations

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document follows `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

Phase 1 makes this 1.7.10 Forge mod understand the low-level OpenYSM data primitives without changing rendering or model selection behavior. After this work, the repository contains JVM 8-compatible ports of the OpenYSM byte buffer, hash, XChaCha20, modified YSM zstd, cache naming, and cache/file encryption helpers. A contributor can prove the foundation works by running the JUnit tests for varint encoding, hash vectors, zstd round trips, encrypted `.ysm` file round trips, and server/client cache transcode.

## Progress

- [x] (2026-05-09 19:02+08:00) Read `README.md`, `.agent/PLANS.md`, and `.agent/phase0-baseline.md` to confirm Phase 1 scope and repository constraints.
- [x] (2026-05-09 19:12+08:00) Ported `rip.ysm.algorithms`, `rip.ysm.zstd`, and `rip.ysm.security` into `src/main/java` with Java 8-compatible syntax adjustments.
- [x] (2026-05-09 19:12+08:00) Removed standalone debug output from the ported foundation classes and made packet integrity failure throw instead of writing to stderr.
- [x] (2026-05-09 19:12+08:00) Added focused JUnit 5 tests under `src/test/java` for byte buffer primitives, hash/cache naming, modified zstd, `.ysm` file encryption, and cache encryption/transcode.
- [x] (2026-05-09 19:17+08:00) Performed available non-Gradle checks and recorded that Gradle verification must be run by the user.

## Surprises & Discoveries

- Observation: OpenYSM's low-level `rip.ysm` sources are mostly independent of Minecraft/Forge classes.
  Evidence: `rg` found only `YSMByteBuf`/`YsmCrypt` requiring Netty `ByteBuf`, while the algorithm and zstd packages depend on JDK APIs and `sun.misc.Unsafe`.
- Observation: The source copy still contains Java 17 syntax in Phase 1 scope.
  Evidence: `YsmCrypt` declares `public record EncryptedPacket`, and `rip.ysm.algorithms.YsmZstd` uses switch expressions with `->`.
- Observation: The algorithm and zstd packages compile with Java 8 source/target settings outside Gradle.
  Evidence: `javac -source 8 -target 8 -encoding UTF-8 -d build/codex-phase1-check <algorithm and zstd files>` exited 0. The only output was the expected `sun.misc.Unsafe` internal API warnings from the vendored zstd implementation and the standard bootclasspath warning from using a newer JDK with `-source 8`.

## Decision Log

- Decision: Keep the upstream package names `rip.ysm.*` for the Phase 1 foundation instead of relocating them under `com.fox.ysmu`.
  Rationale: Later OpenYSM imports already reference these package names, and the repository already carries third-party packages outside `com.fox.ysmu`. Keeping names stable reduces future migration churn while still keeping the code isolated from rendering.
  Date/Author: 2026-05-09 / Codex
- Decision: Do not integrate these classes into `ServerModelManager` or network handlers in Phase 1.
  Rationale: The README defines Phase 1 as pure format/security groundwork and defers model scanning and protocol changes to later stages. Keeping this phase test-only avoids changing current runtime behavior.
  Date/Author: 2026-05-09 / Codex
- Decision: Convert packet integrity mismatch in `YsmCrypt.decrypt` from stderr logging to an exception.
  Rationale: The foundation package should not write to standard output or error, and a packet with a mismatched signature should not be consumed as valid decrypted data.
  Date/Author: 2026-05-09 / Codex

## Outcomes & Retrospective

Phase 1 foundation code is now present but not wired into runtime model loading or networking. The port added 46 main Java source files under `src/main/java/rip/ysm` and one JUnit test class under `src/test/java/rip/ysm/security/YsmFoundationTest.java`. The test class contains eight tests covering byte buffer primitives, garbage headers, CityHash, modified zstd, cache filenames, encrypted `.ysm` files, cache transcode, and encrypted packet integrity.

Gradle was not run in this environment because the repository instructions forbid sandboxed Gradle execution. The remaining acceptance step is for the user to run `.\gradlew.bat test` from the repository root and confirm the new tests pass.

## Context and Orientation

The current mod code lives mainly under `src/main/java/com/fox/ysmu`. Existing legacy YSM folder and old `.ysm` loading use `src/main/java/com/fox/ysmu/model/format/FolderFormat.java`, `src/main/java/com/fox/ysmu/model/format/YsmFormat.java`, and helpers under `src/main/java/com/fox/ysmu/util`. OpenYSM's Phase 1 primitives live under `OpenYSM/src/main/java/rip/ysm`.

`YSMByteBuf` is a small wrapper around Netty `ByteBuf` that reads and writes OpenYSM little-endian values, variable-length integers, byte arrays, and UTF-8 strings. A variable-length integer stores seven data bits per byte and uses the high bit to signal that another byte follows.

`CityHash`, `MT19937`, and `XChaCha20` are deterministic algorithms used by OpenYSM to derive cache hashes and encrypt payloads. Deterministic means the same input and key must always produce the same output.

`YsmZstd` is OpenYSM's modified zstd framing layer. It uses normal zstd compression internally, then obfuscates block headers. Decompression first reverses the header obfuscation and then runs the standard zstd decoder included in `rip.ysm.zstd`.

`YsmCrypt` combines these primitives to encrypt `.ysm` files, server cache files, client cache files, and encrypted network packets. `YSMClientCache` converts model hash pairs into obfuscated local cache filenames and verifies cache file signatures.

## Plan of Work

Copy the upstream `rip.ysm.algorithms`, `rip.ysm.zstd`, and `rip.ysm.security` packages into `src/main/java/rip/ysm`. The legacy package is not copied in this phase because this repository already has legacy AES, deflate, byte integer, and old `.ysm` utilities under `com.fox.ysmu.util`, and the upstream legacy copy imports modern `net.minecraft.resources.ResourceLocation`.

After copying, edit the ported classes so they remain valid for a JVM 8 runtime. Replace `YsmCrypt.EncryptedPacket` from a Java record with a final nested class exposing `data()` and `nextKey()` accessors so future OpenYSM-style call sites still read naturally. Replace switch expressions in `rip.ysm.algorithms.YsmZstd` with ordinary switch statements. Remove noisy `System.out.println` and `printStackTrace` behavior from `YSMClientCache` because this foundation package should not write to standard output during cache scans.

Add JUnit 5 tests under `src/test/java/rip/ysm`. The tests should avoid Minecraft startup and only exercise JDK/Netty-backed primitives. They should cover little-endian and varint behavior in `YSMByteBuf`, the known CityHash vector already present in the upstream source, modified zstd round trips, cache filename generation and UUID decoding, `.ysm` file encrypt/decrypt round trips, server cache signature verification, server-to-client cache transcode, and encrypted packet next-key behavior.

## Concrete Steps

From repository root `D:\Code\YSMU`, copy the relevant source packages, then apply Java 8 and logging edits. Add JUnit tests. Do not run Gradle in this environment; repository instructions require the user to run Gradle on the host.

The exact host verification command for this phase is:

    .\gradlew.bat test

Expected result after this phase is a successful JUnit run including the new `rip.ysm` tests.

Checks performed in this environment:

    rg -n "record\b|Files\.readString|Files\.writeString|List\.of|Map\.of|Set\.of|Objects\.requireNonNullElse|VarHandle|System\.out|System\.err|printStackTrace" src\main\java\rip\ysm src\test\java\rip\ysm -S

This returned no matches. A Java 8 syntax-level compile of `src/main/java/rip/ysm/algorithms` and `src/main/java/rip/ysm/zstd` succeeded with expected `sun.misc.Unsafe` warnings. The temporary compile output directory `build/codex-phase1-check` was removed afterwards.

## Validation and Acceptance

Acceptance for Phase 1 is that `.\gradlew.bat test` passes on the host machine. The new tests must demonstrate that `YSMByteBuf` round-trips representative varints, strings, byte arrays, and little-endian numeric values; `CityHash` produces the known hash vector; `YsmZstd.compress` followed by `YsmZstd.decompress` returns the original bytes; `YSMClientCache.generateCacheFileName` produces a 40-character hexadecimal name that decodes to the original hash pair; and `YsmCrypt` can decrypt data it encrypted for both `.ysm` files and cache files.

The runtime behavior of legacy folders, legacy `.ysm` archives, rendering, and current network sync should be unchanged in this phase because none of the new classes are wired into those paths yet.

## Idempotence and Recovery

The copy step is additive into `src/main/java/rip/ysm`. Re-running it before local edits would overwrite the JVM 8 adaptations, so after the initial copy future updates should use normal diffs. If a copied class causes compilation trouble, compare it against the original under `OpenYSM/src/main/java/rip/ysm` and keep only the minimal Java 8 adaptation needed.

The tests write only to JUnit-managed temporary directories. They should be safe to run repeatedly and should not create or modify files under `config/ysmu`.

## Artifacts and Notes

Phase 0 has already been recorded in `.agent/phase0-baseline.md`, including user-provided manual verification. Phase 1 builds on that baseline without changing current runtime integration points.

## Interfaces and Dependencies

At the end of this phase, these public classes should exist:

    src/main/java/rip/ysm/security/YSMByteBuf.java
    src/main/java/rip/ysm/security/YsmCrypt.java
    src/main/java/rip/ysm/security/YSMClientCache.java
    src/main/java/rip/ysm/algorithms/CityHash.java
    src/main/java/rip/ysm/algorithms/MT19937.java
    src/main/java/rip/ysm/algorithms/XChaCha20.java
    src/main/java/rip/ysm/algorithms/YsmZstd.java
    src/main/java/rip/ysm/zstd/*.java

`YsmCrypt.EncryptedPacket` must provide:

    public byte[] data()
    public byte[] nextKey()

The tests require the existing JUnit 5 dependencies declared in `dependencies.gradle`. No new external dependency should be added for Phase 1.

Revision note, 2026-05-09: Created this ExecPlan before implementation so Phase 1 can be resumed from a self-contained document.

Revision note, 2026-05-09: Updated the ExecPlan after implementation with actual progress, the packet integrity decision, test coverage, and local verification evidence.
