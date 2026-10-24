# Spatial GUI ports — Core 0.1.5

Historical port results. The subsequent [config-menu fix](CONFIG-MENU-VALIDATION.md) refreshes the six Fabric artifacts; Spatial GUI implementation classes are unchanged.

Date: 2026-10-22. Scope: the optional Spatial GUI integration and the presentation, input and HUD behavior affected by porting it. This is focused QA, not a complete functional/gameplay suite.

## Availability and implementation

The [official Spatial GUI versions page](https://modrinth.com/mod/spatial-gui/versions) and a fresh Modrinth API response confirmed Spatial GUI 1.8 files for every existing Core target: Fabric and Forge on Minecraft 1.20.1; Fabric and NeoForge on 1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3. The immutable release IDs, filenames and upstream SHA-512 hashes are in [the target manifest](../gradle/spatial-gui-targets.json). All test fixtures are unmodified upstream files, verified against those hashes.

The [shared integration](../core-integrations/spatial-gui/src/main/java/com/salts_inventory_update/spatial/) remains the source of truth for panel fitting, fixed aspect ratio, input ownership and reversible camera/surface transitions. [The source generator](../tools/generate-spatial-sources.py) adapts renamed Minecraft APIs, legacy camera setup, mouse callback signatures, graphics wrappers, player rendering and native HUD composition. Forge/NeoForge capture their hotbar overlay/layer group without drawing unrelated layers into the panel. Earlier Minecraft versions cache the renderer's actual final FOV for projection and cursor agreement.

Porting exposed a GLFW input-ordering bug: Spatial GUI could remap mouse coordinates before Core's movement-control bypass ran. The shared wrapper now executes outside that remapping, and Spatial GUI's frame update cannot replace native mouse positions during gameplay control. This preserves the normal relative camera path on the older targets as well as 26.3.

Activation remains optional and client-only. Fabric checks its mod metadata; Forge/NeoForge check early native loading metadata. The plugin itself does not reference Minecraft client or Spatial GUI classes. No public API or network-protocol changes were made. Compatibility and Controlify require no Spatial GUI changes.

## Focused checks

The per-target command is:

```powershell
.\functional-tests\scripts\Run-SpatialGuiQa.ps1 -Minecraft <version> -Loader <loader> -Transitions -WithRegression -RunId spatial-port-release-r1
```

Fabric launches the production Core JAR with a separate disposable QA mod. Forge/NeoForge use the existing source-classpath gameplay launcher with QA-only instrumentation. Neither fixture includes the Compatibility or Controlify extension. The harness creates a disposable Survival integrated-server world and exercises the real Core session and item-transfer paths.

Coverage includes:

- Inventory opening, Alt entry/release, rapid reversal, closing, interrupted opening, reopening during closing and closing with movement control held, for third person, first person and curved first person.
- Hold and Toggle movement control, rebound movement keys, native mouse camera rotation, gameplay attack/use, and return to the spatial desktop around the player's new facing.
- Closing the last window, persistent workspace hide/restore, multiple container/player windows, both hotbar placements, held-slot locking and temporary native HUD restoration.
- Rendered-slot/projected-hit agreement, real server-authoritative pickup/place, curved UI clicks, out-of-panel handling, outside dropping, world reach and eye-to-target obstruction.
- Actual submitted panel-mesh measurements and during/settled screenshots. The probe rejects non-finite corners and flags steps above `max(0.06 NDC, elapsedSeconds * 8 NDC/s)`. This detects large jumps; it does not prove mathematical continuity or subjective smoothness at every frame rate.

The QA-only held-key seam supplies physical-key polling state for Hold. Toggle and mouse tests invoke native callbacks. Production movement, camera and render logic remain in use.

Representative settled screenshots were visually inspected: Forge 1.20.1's detached HUD, NeoForge 1.21.1's third-person workspace, Fabric 1.21.11's normal movement desktop, NeoForge 26.1.2's detached HUD and NeoForge 26.3's curved first-person workspace. Every target captured during/settled screenshots and frame measurements; not every image was manually reviewed.

The deterministic presentation test passed **13 assertions on each of Java 17, 21 and 25**, covering projected opening motion, normal viewport alignment, reversal, closing placement, reopening and independent sessions. All twelve production source variants compiled. Source feature parity and the source boundary audit also passed.

## Target results

All twelve combined profiles passed. Record counts include assertions, stage completions and diagnostic records; they are not counts of independent test cases.

| Minecraft | Loader | Records | Failures | Result |
| --- | --- | ---: | ---: | --- |
| 1.20.1 | Fabric | 336 | 0 | PASS |
| 1.20.1 | Forge | 332 | 0 | PASS |
| 1.21.1 | Fabric | 332 | 0 | PASS |
| 1.21.1 | NeoForge | 336 | 0 | PASS |
| 1.21.11 | Fabric | 336 | 0 | PASS |
| 1.21.11 | NeoForge | 336 | 0 | PASS |
| 26.1.2 | Fabric | 336 | 0 | PASS |
| 26.1.2 | NeoForge | 336 | 0 | PASS |
| 26.2 | Fabric | 336 | 0 | PASS |
| 26.2 | NeoForge | 336 | 0 | PASS |
| 26.3 | Fabric | 335 | 0 | PASS |
| 26.3 | NeoForge | 335 | 0 | PASS |

All targets ran the 34 transition cases plus the affected interaction profile. The first Forge 1.20.1 and Fabric 1.21.1 runs preceded four supplementary normal-screen alignment assertions; remaining GLFW targets include them. The SDL targets emit one fewer native-input diagnostic record.

Fabric runtime checks used Loader 0.19.5, as the pinned Spatial GUI files require at least 0.19.3. NeoForge 1.21.1 used 21.1.240 to meet Spatial GUI's requirement; Core's ordinary compile/release baseline is unchanged. Java 17 was used for 1.20.1, Java 21 for 1.21.1/1.21.11 and Java 25 for 26.x.

Core-only launches without Spatial GUI also passed on **1.20.1 Forge, 1.21.1 NeoForge and 26.3 Fabric**, with 21 records and zero failures each. They check absence detection, ordinary desktop input, server-authoritative pickup/return and session cleanup. These are representative checks across the three loader families, not twelve separate absence runs.

## Production artifacts

The normal root command, **`.\gradlew.bat build --console=plain`**, passed, including source archives, production remapping/reobfuscation, release metadata, API examples, compiler boundaries and the existing static module checks. The generated sources/resources carry their producer task dependency, so source archives are also reliable in a clean root build. Native QA build outputs were cleared before production compilation.

All twelve updated Core 0.1.5 JARs are in [dist/core](../dist/core/), using `salts_inventory_update-<Minecraft>-<Fabric|Forge|NeoForge>-0.1.5.jar`. Their [SHA-256 checksums](../dist/core/SHA256SUMS.txt) and identical [versioned release mirrors](../build/release/0.1.5/) were verified.

The [artifact comparison](../build/spatial-gui-analysis/port/artifact-verification.json) confirms exactly **12 changed Core JARs and 24 byte-identical Compatibility/Controlify JARs** relative to the pre-port distribution. All final Fabric class files and non-loader resources are byte-identical to their gameplay fixtures. For the five older Fabric targets, only `fabric.mod.json`'s loader minimum and the manifest's loader/mixin build-version attributes differ: QA used Loader 0.19.5, while production retains Core's ordinary minimum. The 26.3 Fabric release is the exact tested JAR. This comparison checks ZIP entry bytes, rather than assuming that recompilation produced equivalent code.

Production boundary checks passed for all 12 Core and 12 Compatibility JARs. Every Core artifact registers the optional client-only plugin and mixin configuration; none embeds Spatial GUI or contains QA classes, QA mixin metadata, QA refmap entries or the obsolete canvas wrapper. Forge 1.20.1's reobfuscated artifact contains the required camera, options, mouse, HUD, avatar and FOV mappings. Core has no required Spatial GUI/YACL/Mod Menu dependency; release loader minimums retain their existing values. Compatibility's API-only compiler and bytecode boundaries remain intact.

## Evidence

- [Availability response](../build/spatial-gui-analysis/port/modrinth-versions-live.json)
- [Matrix driver status](../build/spatial-gui-analysis/port/matrix-status.log)
- [Fabric production-JAR results](../build/gameplay-qa/packaged-core/)
- [Forge/NeoForge source-classpath results](../build/gameplay-qa/spatial-ports/)
- [Source and runtime signature inspection](../build/spatial-gui-analysis/port/)
- [Production Core artifacts](../dist/core/)
- [Successful standard root build](../build/spatial-gui-analysis/port/production-build.log)
- [Production source/JAR boundary audit](../build/spatial-gui-analysis/port/production-boundaries.log)
- [Artifact and gameplay-fixture comparison](../build/spatial-gui-analysis/port/artifact-verification.json)

## Coverage limits

A sustained physical Alt-plus-mouse session is not covered by the held-key seam. The Native tests are source-classpath tests, rather than packaged-JAR client launches. Test JAR contents and mappings are checked separately before distribution. Creative/Spectator combinations, other camera mods, shaders, extreme custom transform/animation settings, arbitrary frame rates, multiplayer latency and every possible external API window are outside this port's focused coverage. The earlier 26.3 Fabric [Core-only dedicated-server checks](SPATIAL-GUI-VALIDATION.md) remain historical evidence; a twelve-target dedicated-server matrix is not implied.
