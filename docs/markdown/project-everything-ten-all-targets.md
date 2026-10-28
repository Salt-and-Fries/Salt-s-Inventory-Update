# Project Everything: second batch across Minecraft targets

Core **0.1.6** and Compatibility **0.1.3** use the same public native-template API on every supported target. Integrations select the original release's artwork and real menu slots, retaining native animations, processing, item rules and controls. Controlify remains **0.1.1**.

The [1.20.1 report](project-everything-ten-1.20.1.md) covers the earlier Fabric/Forge work. The [release inventory](../../compatibility/native-windows-all-targets.json) pins the post-1.20.1 releases, file hashes and screen classes checked on October 27, 2026. Availability means an upstream release explicitly declares that exact Minecraft version and loader.

| Integration | 1.21.1 | 1.21.11 | 26.1.2 | 26.2 | 26.3 |
| --- | --- | --- | --- | --- | --- |
| Bartering Station | Fabric, NeoForge | Fabric, NeoForge | Fabric, NeoForge | Fabric, NeoForge | Fabric, NeoForge |
| Alloy Forgery | Fabric | — | Fabric | — | — |
| Fabric Seasons: Extras | Fabric | — | — | — | — |
| Alloyed (standalone 3.x) | Fabric, NeoForge | — | Fabric, NeoForge | Fabric | Fabric |
| Farmer's Delight / Refabricated | Fabric, NeoForge | Fabric | Fabric | Fabric | Fabric |
| Miner's Delight | NeoForge | — | — | — | — |
| Better Archeology | Fabric, NeoForge | Fabric, NeoForge | Fabric, NeoForge | Fabric, NeoForge | Fabric, NeoForge |
| Illager Invasion | Fabric, NeoForge | Fabric, NeoForge | Fabric, NeoForge | Fabric, NeoForge | — |
| Galosphere | Fabric, NeoForge | — | — | — | — |
| Sawmill | Fabric, NeoForge | — | Fabric, NeoForge | — | — |

These are 50 available integration/target combinations. Missing combinations are not advertised as supported ports. Upstream dependencies remain separate optional downloads.

## Release-specific behavior

- Cooking and copper pots keep their recipe-book toggle inside a compact extra column in the frame. The original recipe component remains responsible for its recipes and selection behavior.
- Identifying tables use the shared parchment frame and brown controls. Fossil sockets retain their original gray beveled plate. Slot ownership handles player-first menus and the newer identifying menu's overlapping native entries without renumbering them.
- Bartering Station's newer item/button rendering is captured directly; the older 1.20.1 custom model renderer is not reused on these releases.
- Alloyed 3.x owns an independent forge screen, unlike the legacy 1.20.1 Create add-on. The compact window retains its nine inputs, fuel, output, flame and progress arrow.
- Alloyed's newer optional mechanical-crafting knife recipe is loaded only with Alloyed, Farmer's Delight and Create present. Its original ingredients and result are preserved. This avoids invalid serializer failures, including world-loading failure on 26.3 when Create is absent.
- Alloyed 26.1.2 registers that screen as `alloyed:oven`, while its block remains `alloyed:forge`. Both reviewed menu identifiers are recognized. Miner's Delight 1.21.1 uses `minersdelight`, unlike its legacy `miners_delight` namespace.
- Alloy Forgery's lava gauge remains outside the frame. Its 26.1.2 layout also retains the native slot-lock control. The 1.21.1 Owo OpenGL bridge is loader-specific; newer Owo uses the deferred renderer.
- Alloy Forgery 26.1.2 attaches its original Owo menu to the correct player and sends Owo's property bytes to its owning SIU window. Slot-lock clicks and hotbar-key enabling use the original upstream packet. Other Owo menus and ordinary native screens retain their original transport.
- Seasons: Extras uses the public menu-button channel for each detached heater/chiller, with the original upstream server behavior and pressed indicators.
- Sawmill keeps both widths, native recipe tiles, search and scrollbar. Standalone sign editors remain native screens; cabinets use normal storage windows.

Core's generalized cursor fix preserves in-flight menu state when native menus broadcast from inside a click. Recipe-book placement accounts for the occupied frame and exterior regions. Extended-menu opening recognizes both generations of Fabric's public stream-codec getter and decodes typed opening data before invoking a factory. No Core integration imports were added; Compatibility continues to use only the public API.

`DesktopSessions.activeNetworkMenu(ServerPlayer)` is an additive public observation hook available on every supported Core target. It returns the vanilla menu model only inside that player's scoped desktop server callback, and an empty `Optional` elsewhere. Together with `activeNetworkSessionId` and `sendSessionPayload`, it lets a normal API consumer scope a library's custom synchronization to the correct detached window. It does not register an explicit window handler or change automatic-window eligibility.

## Reproducing focused checks

Prepare an exact upstream fixture with:

```powershell
python tools/prepare-project-everything-ports.py --minecraft 1.21.1 --loader fabric --output build/project-everything-ten-port/fixture-1.21.1-fabric.json
```

The isolated fixture, `functional-tests/project-everything-ports/qa.init.gradle`, stages release builds and unmodified upstream downloads in disposable worlds. Fabric 1.21.1 and 1.21.11 use Loom's named development mapping of the release classes and upstream dependencies: Fabric Loader's runtime enum-extension remapper fails before SIU initializes with those upstream releases. The 26.x Fabric and NeoForge checks use staged production JARs. This distinction is recorded in each fixture's runtime/staging evidence. Its command driver is excluded from production artifacts. Tests concern affected native windows; they do not constitute the complete functional suite.

Root `gradlew.bat build` generates release artifacts in `dist/core`, `dist/compatibility` and `dist/controlify`. Individual module builds remain supported. A normal final build must follow QA builds so test runtime overrides do not become production metadata requirements.

## Focused gameplay results

All ten post-1.20.1 SIU targets completed their available second-batch native-window checks. Baselines and targeted follow-ups recorded **781 passing assertions** and **312 real-client screenshots**. These counts include repeated checks during follow-ups; they are not counts of distinct features.

| Minecraft | Loader | Baseline assertions | Follow-up assertions |
| --- | --- | ---: | ---: |
| 1.21.1 | fabric | 114 | 0 |
| 1.21.1 | neoforge | 84 | 45 |
| 1.21.11 | fabric | 54 | 16 |
| 1.21.11 | neoforge | 41 | 15 |
| 26.1.2 | fabric | 99 | 22 |
| 26.1.2 | neoforge | 71 | 0 |
| 26.2 | fabric | 75 | 0 |
| 26.2 | neoforge | 46 | 0 |
| 26.3 | fabric | 64 | 0 |
| 26.3 | neoforge | 35 | 0 |

Checks covered real block interaction, selection of the public native template, original input slots, server/client cursor agreement, pickup and placement, dragging, closing and reopening. Focused additional cases exercised pot recipe-book toggling after reopening, Alloyed bronze processing and output transfer, newer Bartering Station piglin highlighting and scoped closing, and Alloy Forgery locking, initial property snapshots and live fuel updates. Screenshot contact sheets were reviewed for all ten targets, including the parchment frame, compact slot layouts and exterior gauge.

Evidence is retained under `build/project-everything-ten-port/gameplay`, with successful matrix summaries named `matrix-ten-port-final-01.json`, `matrix-ten-port-final-05.json`, `matrix-ten-port-final-07.json`, `matrix-ten-followup-01.json`, `matrix-ten-head-01.json` and `matrix-ten-owo-01.json`. Earlier failed attempts remain separate; the counts above include only the successful runs.

### Coverage limits

This is focused affected-feature validation, not the complete functional/gameplay suite and not a claim that every recipe, resource pack, UI scale or modpack combination is bug-free. It uses disposable single-player worlds with an integrated server; dedicated-server and mixed remote-client scenarios were not exercised in this pass. The optional Alloyed recipe was checked structurally and with Create absent; its Create-present crafting path was not exercised. Raw remapped production-JAR gameplay launch on Fabric 1.21.1/1.21.11 remains unverified because of the upstream enum-extension remapper problem described above; those targets used named release classes.

## Automated regression coverage

All **65 selected JUnit tests** passed across the five newer Minecraft source targets: 35 layout/mapping tests, 15 native-cursor/scope tests and 15 opening-codec/factory tests. The same shared Java sources are compiled for both loaders. The tests exercise actual crop/input mapping, in-frame recipe controls, player-first fossil slots, exterior regions, the standalone Alloyed output border, scoped cursor broadcasts and typed opening data through both codec-getter generations.

Selected task/filter pairs were `:compatibility:<minecraft>:fabric:test --tests '*NativeMachineLayoutsTest'` and `:<minecraft>:fabric:test --tests '*NativeCursorBroadcastTest' --tests '*NativeOpeningCodecTest'`. XML results are retained in each target's `build/test-results/test`; the summary and log are in `build/project-everything-ten-port`.

Release packaging runs `gradlew.bat build -x test` without fixture/runtime overrides, following those selected tests. This rebuilds all production modules and runs the build's API, packaging and source-boundary checks without rerunning unrelated JUnit suites.

## Final release artifacts

The normal release build completed successfully on October 27, 2026: **424 Gradle tasks, 156 executed and 268 up to date**. Source/API compile boundaries, native renderer/mixin symbols on all twelve targets, public native-template examples, existing integration packaging, and the new 50-combination source/JAR coverage checks passed. A separate check verified all **63 shared textures** in processed resources and all **36 production JARs**.

Fresh artifacts are in `dist/core` (**12 × Core 0.1.6**), `dist/compatibility` (**12 × Compatibility 0.1.3**) and `dist/controlify` (**12 × Controlify 0.1.1**, unchanged functionality). The ten newer Compatibility targets contain these ports; the two 1.20.1 builds retain the earlier work. Core includes only generalized host/API changes; Compatibility still compiles without Core implementation classes.

`dist/salts-inventory-all-latest.zip` groups all 36 artifacts by Minecraft version and loader. Each distribution directory contains a verified `SHA256SUMS.txt`. Exact artifact hashes, test summaries and runtime qualifications are recorded in [the validation manifest](../../output/project-everything-ten-port/validation.json). Reviewed contact sheets are in `output/project-everything-ten-port/screenshots`. No development, source or QA JARs are included in the distribution.
