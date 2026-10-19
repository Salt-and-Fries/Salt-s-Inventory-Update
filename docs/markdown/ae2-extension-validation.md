# AE2 extension validation — 2026-10-12

Implemented on `Jar-Seperation-Experiment`, version `0.1.4`. AE2 compatibility
is an optional consumer of Core's public API and of an unmodified upstream AE2
JAR. Core remains independent. All twelve compatibility artifacts contain the
shared fork additions; six have native adapters for available upstream releases.

## Architectural changes and migrated behavior

Core retains the inventory framework, vanilla windows, persistence, networking
and the public API. The extension owns AE2 desktop views, native adapters,
menu reconstruction and lifetime routing, authenticated actions, toolbox
companions, desktop controls, screen navigation and CPU synchronization.
Existing Tom's Storage, JEI, REI, EMI, Mod Menu and Sophisticated integrations
remain in their separate extension packages.

Compatibility compiles against an API-only Core JAR. Source, encoded-delta and
production-bytecode audits reject implementation access and reverse dependency.
Neither artifact embeds Core or upstream AE2. Native patches target AE2 only,
retain its installed implementation and are applied after full contract checks.
Missing or unsupported AE2 leaves unrelated integrations available.

The API additions supporting the split are documented in
[the architecture guide](jar-separation.md#public-extension-points) and
[the desktop API guide](desktop_window_api.md). They include loader-neutral
lifecycle/transactional registration, recipe-backend registration, native menu
capture/transitions, cursor access, item-source protection, native quick-move
policy, synchronization preservation, supported theme assets, configuration/help
factories, bounded payloads and native drawing/input adapters. They are general
extension points, available to any third-party consumer. No AE2-specific Core
extension point was added for the version-specific UI clarification.

The reference source coverage audit accounts for all 89 changed Java files:
60 native source edits, 16 rehomed helpers, 12 excluded QA sources/edits and one
localization generator. All 42 desktop documents and 59 added language keys are
packaged with provenance and LGPL notices. The native 26.3 delta comparison
verifies all 61 class contracts against the fork; its only explicit metadata
adaptation transports four CPU fields through the public session channel instead
of extending ordinary AE2 `GuiSync` updates.

## Version-specific UI and native support

The accepted baseline is each installed AE2 version's own design and features,
with the fork's desktop integration layered on top. Native artwork, controls,
rendering and packet codecs come from that version. AE2 15 retains its native
knife/condenser positions, 97-pixel catalog footer, larger sky chest and combined
chest menu. Later ordinary AE2 features are not backported solely to match 26.3.

| Minecraft / loader | Native AE2 input | Full atlas coverage |
| --- | --- | --- |
| 1.20.1 Fabric | 15.4.11 | PASS: 40 fixtures, 39 native menus, 76 presentations |
| 1.20.1 Forge | 15.4.11 | PASS: 40 fixtures, 39 native menus, 76 presentations |
| 1.21.1 NeoForge | 19.2.18 | PASS: 40 native menus, 90 presentations |
| 26.1.2 NeoForge | 26.1.13-beta | PASS: 40 native menus, 90 presentations |
| 26.2 NeoForge | 26.2.1-alpha | PASS: 40 native menus, 90 presentations |
| 26.3 NeoForge | 26.3.1-alpha | PASS: 40 native menus, 90 presentations |

The legacy manifests now enable release activation. Fresh checks without
`SIU_AE2_LEGACY_QA` passed the Forge 1.20.1 full atlas, Fabric 1.20.1 catalog
focus and NeoForge 1.21.1 four-menu gameplay profile. Release audits reject
staged adapters; regeneration requires renewed review before activation.

Atlas coverage verifies named views, uniqueness, image/geometry files, native
menu counts and actual upstream subview class availability. AE2 15 lacks twelve
key-type selector/return presentations and two separate chest-settings views;
these are native feature differences, not missing supported views. Twenty-five
standalone coverage checks accepted reviewed captures and rejected deliberate
duplicates, missing views/geometry and incorrect upstream feature profiles.

## Produced artifacts and build results

The normal `.\gradlew.bat build` passed in **2m 4s** with 293 tasks (40 executed).
It produced twelve Core and twelve compatibility production JARs:

```text
dist/core/salts_inventory_update-<Minecraft>-<Loader>-0.1.4.jar
dist/compatibility/salts_inventory_compat-<Minecraft>-<Loader>-0.1.4.jar
```

Examples:

```text
dist/core/salts_inventory_update-26.3-NeoForge-0.1.4.jar
dist/compatibility/salts_inventory_compat-26.3-NeoForge-0.1.4.jar
```

Each output directory contains `SHA256SUMS.txt`; all 24 hashes were independently
verified. Root builds clear owned distribution outputs and select exact remapped
production archives. Development, sources, ABI, QA and nested dependency JARs
are excluded. Individual module builds remain available.

Executed checks:

- **848 JUnit tests:** zero failures, errors or skipped tests.
- **14 portable instruction-merge regressions:** passed.
- Public API examples, source parity, source/compiler/archive boundaries,
  native symbol/stack checks and metadata/resource audits: passed.
- Six native four-menu gameplay profiles: 312 records per AE2 15 loader and
  319 per newer target, with zero failures.
- Initial CPU state delivery and two real job/cancel cycles on all six native
  targets: passed, including server cancellation and client idle resynchronization.
- Fresh AE2 15 full atlases: both loaders passed all 76 native presentations.
- Earlier Core-only gameplay passed on all twelve targets. Forge 1.20.1 also
  passed 271 Core-only records and 281 extension-without-AE2 records after the
  generic cursor-session lookup fix.

Machine-readable evidence is in
`build/ae2-port/validation-final-version-specific-release.json`,
`final-distribution-audit.json`, `native-cpu-lifecycle-matrix.json`,
`legacy-version-specific-ui-review.json` and `fork-java-source-coverage.json`.
The root log is `build/ae2-port/build-version-specific-ae2-release.log`;
scoped runtime records and captures are under `build/gameplay-qa`.

The final integrity check confirms the reference HEAD and clean status,
all 4,862 tracked-file hashes, and its built JAR hash are unchanged.
The reference was never built or modified by this migration.

## Remaining limits

As of the reviewed upstream release matrix, no unmodified AE2 input exists for
Fabric 1.21.1/1.21.11/26.1.2/26.2/26.3 or NeoForge 1.21.11. Their extension JARs
contain the shared additions, but native AE2 support is unavailable and is not
claimed. A future upstream release requires a reviewed native adapter.

AE2 runtime checks here use development game launchers with unmodified upstream
AE2 artifacts. Current production archives passed bytecode/resource/remapping
audits; fresh installed-launcher AE2 gameplay, dedicated servers, multiplayer,
latency/reconnect/dimension changes and arbitrary modpacks remain unverified.
Earlier packaged Core/pair gameplay evidence is recorded separately in
[the separation validation report](jar-separation-validation.md).

Cross-version raw pixel identity is not required or claimed. Native art and
renderer behavior vary, and screenshots contain live/animated data and world
background. The recorded assertions establish covered behavior and native
layout integrity, not an exhaustive guarantee for every interaction.
