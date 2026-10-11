# API redesign for 0.1.4

The authoritative objective is the user goal in the attached `goal-objective.md`.
This checklist tracks implementation and evidence; it does not reduce that scope.

## Required outcome

- Optional integrations discovered by Salt on all twelve supported loader/version targets.
  External mod startup must work without Salt; client providers must remain unloaded on servers.
- Rich container and client-only windows, with explicit lifecycle, input, focus, layers,
  clipping, native widget access, constraints, virtual content, and external interaction regions.
- Correct per-session inventory actions and custom server payloads, preserving existing
  authority, slot rules, cursor reconciliation, and multi-window behavior.
- Convenient registration and helpers, explicit conflicts and failure diagnostics,
  unsupported-screen fallback, and updated internal integrations.
- No old public API aliases, deprecation bridges, or legacy consumer adapters.
- Compilable simple-machine, advanced-terminal, and client-only examples; external-style
  absence/presence and dedicated-server tests; all twelve builds and appropriate existing checks.
- Current documentation, dependency instructions, 0.1.4 metadata and changelog.

## Work and evidence

- [x] Read the goal and confirm clean baseline `4616148` (0.1.3).
- [x] Inspect per-version API, existing Tom's/Sophisticated integrations, loader shims,
  lifecycle dispatch, current networking, release tasks, and test setup.
- [x] Loader-neutral descriptor discovery, common/client initialization and diagnostics.
- [x] Registration redesign and removal of old public aliases.
- [x] Rich UI contracts and desktop engine integration.
- [x] Client-only window hosting with no inventory mutation privileges.
- [x] Developer helpers and migration of internal integrations.
- [x] External-style compilable fixtures and meaningful behavioral tests.
- [x] Documentation and release metadata.
- [x] All-target compilation, tests, runtime proof and completion audit (limits below).

## Architecture observations

The existing `DesktopWindowDefinition<T extends AbstractContainerMenu, S>` exposes useful
render/input hooks but assumes a live menu. `InventoryDesktopScreen` owns window geometry,
focus, gestures and sessions. Keep those established inventory guarantees while separating
UI composition from inventory capabilities. Loader shims already expose common lifecycle
events: client-start and server-start can run provider registration after mod registries exist.
The current registry combines client and server methods; this needs separation so common
provider registration does not resolve client API classes on dedicated servers.

All supported versions are 1.20.1, 1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3. Fabric supports
all six; Forge supports 1.20.1; NeoForge supports the other five.

## Verification evidence

Commands use `CI=true`, which selects the supported NeoForge binary-patch build path.

- `./gradlew build -I functional-tests/sort-tests.gradle --continue --console=plain`
  passed for all 12 loader/version targets. `build/api-0.1.4-final-build.log` records
  the result, all-target example compilation, source parity and release bundle validation.
- The common suite passed 45 tests. The six actual-Minecraft headless suites passed
  302 tests (50 / 51 / 50 / 50 / 50 / 51), with zero failures or errors.
  XML results are under each project's `build/test-results/test`.
- API tests exercise capture negotiation, failed-registration rollback, bounded
  handshake packets, custom slot ownership, client-only action denial, independent
  handles, disposal reentrancy, keyboard ownership, modal routing and external regions.
- The externally compiled fixture starts with Salt absent from its runtime classpath,
  registers common/client providers once with Salt present, and runs common discovery
  with a classloader that rejects client API, native GUI and client provider resolution.
- Machine, virtual terminal and native client-only example sources compile on all 12
  real loader/version development classpaths via `verifyApiExamples`.
- `Run-FunctionalMatrix.ps1 -Runtime -TimeoutSeconds 180` completed all 12
  clients with passing summaries: 55 assertions on each 1.20.1 loader and 57 on
  each other target (680 assertions total). Complete logs are preserved in
  `build/api-0.1.4-runtime-results`; the driver log is `build/api-0.1.4-runtime.log`.
  26.3 Fabric reported a native Windows process exit error (0xC0000005) after
  its passing summary. A direct retry with normal Minecraft shutdown reproduced
  it after all 57 assertions passed (`26.3-fabric-retry.log`). The matrix driver
  accepts assertion summaries, so this is a runtime exit limitation, not a clean
  `runClient` task result. Windows Application Error events identify the faulting
  module as `jvm.dll` from Microsoft JDK 25.0.2. This does not establish its cause.
- The normal post-harness build passed again: all 12 release jars, no test harness,
  examples or removed API classes, matching loader metadata and SHA256SUMS.txt.
  Final rebuild, removal of the recheck harness, artifact audit and diff checks passed.

## Practical limits

The optional fixture proves classloading and phase isolation; it is not a distributable
Minecraft mod pack. The UI examples are compilable integration sources whose ordinary
menu registration and real storage backend remain the adopting mod's responsibility.
The virtual catalog demonstrates authenticated selection/synchronization, not fabricated
withdrawals. Dedicated-server safety proof currently uses client-class resolution traps.
Headless routing tests and client startup checks do not establish visual correctness for
all third-party UI frameworks, native narration/IME behavior, or every storage backend.
Each adopting integration still needs an in-game pass with its real permissions and UI.
There are no old-public-API compatibility adapters. Nothing has been published.
