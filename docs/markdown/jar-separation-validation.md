# JAR separation validation — 2026-10-11

Implemented on `Jar-Seperation-Experiment`. The existing six-version,
twelve-target Prism matrix now produces an independent core and optional
compatibility extension for every target. Core project paths, mod identifier,
version `0.1.4`, configuration files and desktop wire protocol remain stable.

## Architecture and migrations

- Core contains inventory expansion, vanilla desktop screens, networking,
  persistence, configuration, extension discovery and the supported API.
- The extension contains Tom's Simple Storage; JEI, REI and EMI providers;
  Fabric Mod Menu; and the supported Forge/NeoForge Sophisticated Core,
  Backpacks and Storage integrations. Their original packages remain grouped
  by mod inside `compatibility/versions`.
- Extension metadata requires the matching core. Integration targets remain
  optional. Conditional Sophisticated mixins and recipe-browser plugin
  entrypoints belong to the extension. Core has no integration bootstrap,
  target-mod dependency or reference to the extension.
- Extension compilers receive only core's API development JAR. The final
  compiler audit inspected **751 classpath entries** across all twelve
  extension targets and found no accessible core implementation classes.
  Production bytecode and resource checks verified all 24 release JARs.
- Added reusable APIs for native quick-move policy, native menu/session
  transitions and payload routing, item-source protection, synchronizer
  preservation, storage-slot registration, lifecycle/diagnostics, configuration
  factories, integration help and supported theme assets. The browser contract
  now supports arbitrary external providers. Legacy graphics/input wrappers
  were moved into public `api.*` packages; legacy consumers must update imports
  and recompile. See the [API extension guide](jar-separation.md).
- Corrected duplicate Sophisticated menu binding registration discovered by
  live testing, and updated the Forge extension's mixin refmap identity. Tom's
  server registration supports repeated world starts; integration registrations
  participate in rollback rather than leaving partial core registrations.

## Produced artifacts

The ordinary `.\gradlew.bat build` completed successfully without `CI=true`.
It generated **12 core JARs and 12 compatibility JARs**, selected from each
loader's exact production/remapping task output. No sources, development,
intermediate or nested core JARs enter distribution.

```text
dist/core/salts_inventory_update-<Minecraft>-<Loader>-0.1.4.jar
dist/compatibility/salts_inventory_compat-<Minecraft>-<Loader>-0.1.4.jar
```

| Minecraft | Loader labels in filenames |
| --- | --- |
| 1.20.1 | Fabric, Forge |
| 1.21.1 | Fabric, NeoForge |
| 1.21.11 | Fabric, NeoForge |
| 26.1.2 | Fabric, NeoForge |
| 26.2 | Fabric, NeoForge |
| 26.3 | Fabric, NeoForge |

Both directories contain `SHA256SUMS.txt`; all 24 hashes were independently
recomputed and matched. `dist` is cleared before a root release build and
synchronized from production outputs. The legacy `build/release/0.1.4` bundle
continues to contain core artifacts only. Individual module builds still work.

## Executed checks

- Full standard root build: **passed**.
- Existing shared/common JUnit suite, six mapped-game core regression suites
  and six extension initialization suites: **601 tests, zero failures, zero
  errors, zero skipped**. This includes provider fallback/priority/removal,
  registration rollback, availability failure isolation, absent optional
  implementations and unrelated integration initialization after a failure.
- Public UI/API examples compiled on all twelve core loader/version targets.
- Source feature parity, source boundary checks, compiler classpath audit,
  production bytecode boundary checks, metadata/resource/loader isolation and
  release verification: **passed**.
- The generated public signature reference contains **86 public types** and
  its synchronization check passed. Markdown/HTML API guide synchronization
  validated the repository links.
- The Forge extension's declared refmap exists in its production JAR. Core
  artifacts contain no moved integrations; extension artifacts contain no
  bundled core classes or nested JARs.

## Live Minecraft smoke runs

These were actual development client launches. The harness checks startup,
keybindings, configuration, vanilla screens, core API definitions, packet
contracts, text editing and inventory expansion limits. Optional integrations
also receive the registration checks described below.

| Scenario | Executed result |
| --- | --- |
| Fabric 1.21.1, core only, isolated directory with no integration targets | PASS: 57 checks; loader list contained core and Fabric dependencies, without the extension |
| Fabric 1.21.1, core and extension, isolated directory without targets | PASS: 57 checks |
| Fabric 1.21.1, core without extension, unrelated JEI already present in the existing development instance | PASS: 57 checks |
| Fabric 1.21.1, extension with JEI 19.25.1.332 and EMI 1.1.24 | PASS: 59 checks, including selected/available EMI provider |
| Fabric 1.21.1, extension with REI 16.0.799 and Mod Menu 11.0.4 | PASS: 60 checks, including REI provider selection/availability and the core's Mod Menu configuration factory |
| Fabric 1.21.1, extension with Tom's Storage 2.4.2 | PASS: 58 checks, including all eight client menu registrations |
| NeoForge 1.21.1, extension with Sophisticated Core/Backpacks/Storage | PASS: 65 checks, including two backpack and four storage client menus and server support for all six |
| Forge 1.20.1, extension with Sophisticated Core/Backpacks/Storage | PASS: 63 checks, including the same six client/server integration registrations |

Runtime logs are saved under `build/separation-runtime-*.log`; the final build
log is `build/separation-final-build.log`. These local logs and generated
artifacts are intentionally ignored by Git. Runtime test harness classes were
excluded from the final production build and verified absent in release JARs.

## Initial smoke-phase limits

The initial smoke runs did **not** exercise in-world interactions. The subsequent
gameplay validation below supersedes that limitation for its listed scenarios.
Dedicated-server, multiplayer and exhaustive operation coverage remain separate
work; startup registration alone is not evidence for those scenarios.

Existing integration implementations were retained and now consume the public
API. Their compilation, optional loading and registration were verified as
reported above; complete gameplay or packaged-runtime validation was not claimed
at that initial stage.
Earlier manual validation records predate this refactor and are not evidence
that those scenarios passed with the new artifacts.


## Subsequent in-world gameplay validation

The disposable-world harness and reproducible commands are documented in
[GAMEPLAY-QA.md](../../functional-tests/GAMEPLAY-QA.md). These are actual game
sessions with menu capture, desktop mouse callbacks, authoritative server
assertions and native framebuffer screenshots.

- Core-only gameplay passed on all twelve supported Minecraft/loader targets.
- Fabric 1.21.1 gameplay passed with the extension and no integration targets;
  separate JEI, EMI, REI/Mod Menu and Tom's Storage profiles passed.
- Sophisticated Core/Backpacks/Storage gameplay passed on Forge 1.20.1 and
  NeoForge 1.21.1, including backpack gestures, source locking, crafting upgrade,
  settings navigation, storage screens and resource-pack reload.
- Actual production Fabric 1.21.1 core-only and production-pair launches use
  a separate QA mod and exclude project implementation directories. Their final
  evidence is recorded in `build/packaged-core-qa.log` and
  `build/packaged-extension-qa.log`.

The subsequent final normal root build passed with **613 tests, zero failures,
zero errors and zero skipped**. It regenerated all 24 production archives,
passed the API compiler-classpath and production-bytecode boundary audits,
and verified the release bundle. These counts supersede the earlier 601-test
smoke-phase build.

Gameplay found and corrected four runtime problems: missing legacy 1.21.1
expansion capability state; Tom's terminal cursor commits; late NeoForge resource
listener registration; and 1.20.1 full menu snapshots omitting required opening
data after the first snapshot. The shared cursor is exposed through supported
session-context API methods. Forge development launches also explicitly register
the core's separate mixin configuration.

The final report generator reads JSON results, launcher summary markers, JUnit
XML and production hashes into `output/pdf/salts-inventory-core-and-compatibility-qa.pdf`
and its evidence manifest. Final normal-build logs are in
`build/gameplay-final-build.log`; failed reproductions are retained under
`build/gameplay-qa/bugs/`.

The tested interactions are not an exhaustive guarantee of zero bugs. Every
native recipe/upgrade/action, saved-world reload, dedicated-server/network latency,
multiplayer/reconnect/dimension transitions, other integration target versions and
arbitrary mod combinations still need their own scenarios. Menu opening/rendering
and a specific successful interaction must be distinguished when interpreting
coverage. Production gameplay here uses Fabric's development game launcher,
not an installed consumer launcher.

## AE2 extension completion — 2026-10-12

The [AE2 validation report](ae2-extension-validation.md) records the subsequent
native integration work, version-specific UI policy, enabled legacy adapters,
848-test root build, 24 refreshed production archives, runtime scenarios and
remaining upstream/runtime limits. These totals supersede the earlier build
counts above.
