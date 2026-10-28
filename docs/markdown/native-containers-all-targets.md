# Native container port: Core 0.1.6 and Compatibility 0.1.3

The automatic container host and native-template API introduced on 1.20.1 are available on all twelve supported targets. Core is **0.1.6** everywhere. Compatibility remains **0.1.3**, and Controlify remains **0.1.1**; both companions require the matching rebuilt Core 0.1.6 artifact.

## Implemented upstream coverage

Availability was checked against upstream Modrinth release metadata on 2026-10-26. The review records 38 available mod/target pairs for these eight integrations. An absent upstream release is not advertised as support.

| Integration | 1.20.1 Fabric | 1.20.1 Forge | 1.21.1 Fabric | 1.21.1 NeoForge | 1.21.11 / 26.1.2 / 26.2 / 26.3 Fabric and NeoForge |
| --- | --- | --- | --- | --- | --- |
| Iron Chests: Restocked | 5.0.2 | 5.0.2 | — | — | — |
| Animal Feeding Trough | 1.1.0 | 1.1.0 | 1.1.2 | 1.1.2 | 1.2.0 on 1.21.11; 1.2.1 on 26.x |
| Oxidized | 1.8.2 | — | 1.8.4 | — | — |
| Brick & Mortar | 1.2.1 | 1.2.2 | 1.3.1 | 1.3.1 | — |
| Expanded Delight | 0.3.2 | — | — | 0.1.4 | — |
| Crabber's Delight | — | 1.2.3 | — | 1.3.1 | — |
| Nether Chested | 8.0.4 | 8.0.4 | 21.1.1 | 21.1.1 | 21.11.0 / 26.1.1 / 26.2.0 / 26.3.0 respectively |
| Inmis | 2.7.2 | — | 2.8.2 | — | — |

Exact release IDs, artifact hashes, optional dependency pins and unavailable reasons are maintained in [coverage-targets.json](../../compatibility/coverage-targets.json). Upstream mods and their own dependencies remain separate installations.

## Architecture and behavior

Core owns structural menu admission, authenticated native-menu negotiation, native opening-data transport, reconstruction, rendering and input capture, source lifetime checks, storage inference and fallback to the original screen. No integration-specific mod names or third-party imports were added to Core. Existing registered API windows retain priority.

All targets expose the same native-template contracts: retain/move/hide source regions, exterior fragments, native or standard slot backgrounds, slot/widget anchors, static JSON profiles, and the in-game inspector. See [the API guide](native-window-templates-1.20.1.md) and [automatic hosting guide](automatic-native-containers-1.20.1.md). Their historical filenames are retained to preserve links.

Compatibility consumes this public API to fit the approved compact layouts to the upstream menus. Machine crops retain decorative frames and controls; the Brick & Mortar preview stays outside the top border. Storage sizes follow real slot grids. Nether Chested retains large counts with standard cell backgrounds. Even Inmis's three-slot Baby Backpack is explicitly classified as storage for sorting.

The 1.21.1/1.21.11 Nether Chested compatibility bridge forwards only Core-owned synchronizers through upstream's supported setter. It identifies them through the additive public `DesktopSynchronization.isDesktopSynchronizer(...)` predicate, without importing Core internals. If an automatic menu cannot provide an initial authoritative snapshot, Core releases the attempted session and lets the original opener continue.

### Version-specific adaptation

- 1.20.1 and 1.21.1 use immediate native render capture with the appropriate vertex, matrix and recipe-button APIs.
- 1.21.11 and 26.x capture deferred GUI states, preserve render layers, and replay clipped transformed elements, items, text and supported vanilla picture-in-picture states.
- Those newer targets invoke the native background and foreground phases separately, retaining artwork for Core-only automatic windows as well as templates. Whole-screen dim/blur effects remain suppressed inside the hosted region.
- Fabric extended-menu codecs and NeoForge's original extra-data writer are preserved. Opening payloads, template capabilities, readiness acknowledgements and client fallback requests are bounded and session-validated.
- Slot/tooltip/background suppression is scoped to native capture. Ordinary API definitions and the rest of the game's screens do not enter that path.
- Ordinary container persistence includes menu identity, so a replacement block/menu does not inherit an unrelated grid size. Profiles and inspector exports use the actual target Minecraft version.

## Build and focused validation

Run `./gradlew build` (Windows: `.\gradlew.bat build`). Production output is separated into `dist/core`, `dist/compatibility` and `dist/controlify`; individual module builds remain available. Companions are rebuilt for their exact Core dependency pins without embedding Core.

The root checks compile the public-API-only native-template example on all twelve targets, enforce module boundaries and coverage, and verify automatic-host Minecraft member references, mixin selectors, accessors/invokers and embedded invocation descriptors against each target's actual compiler inputs. These are build/linkage checks, not gameplay results.

The [original 1.20.1 layout report](native-container-compact-validation.md) documents the earlier approved design pass separately.

### Executed runtime checks

The following twelve target runs each passed the focused baseline: a native Nether Chested window, authoritative pickup/placement of a 512-item stack, all four window walls, the API Feeding Trough window, pickup/return of 32 wheat, and session closure after source destruction. Evidence is stored under `build/native-all-targets/gameplay/<target>-<run>/`, with observations, screenshots, loaded-mod versions and staged production artifact hashes.

| Target | Successful run |
| --- | --- |
| 1.20.1 Fabric | `port-final-02` |
| 1.20.1 Forge | `port-final-02` |
| 1.21.1 Fabric | `port-fabric-06` |
| 1.21.1 NeoForge | `port-neo-04` |
| 1.21.11 Fabric | `port-final-01` |
| 1.21.11 NeoForge | `port-final-01` |
| 26.1.2 Fabric | `port-final-01` |
| 26.1.2 NeoForge | `port-final-01` |
| 26.2 Fabric | `port-final-01` |
| 26.2 NeoForge | `port-final-01` |
| 26.3 Fabric | `port-modern-04` |
| 26.3 NeoForge | `port-final-01` |

Additional executed checks:

- 1.21.1 Fabric: Copper Kiln processing artwork and real mouse recipe-book open/close; Brick & Mortar's exterior preview open/close with a stable 139 x 88 body; native Baby Backpack pickup/placement. The subsequent `port-fabric-06` run verifies the explicit Baby storage classification and sort control. The 1.20.1 Fabric `port-final-02` run verifies Baby sorting too.
- 1.21.1 NeoForge `port-neo-04`: real block-use opening through the original extra-data writer, Juicer pickup/return, and the compact Crab Trap and Brick & Mortar layouts. Its newer Juicer retains its additional native recipe-book control and measures 143 x 94; the Crab Trap is 178 x 71 and Kiln body 139 x 88.
- 1.21.1 Fabric `core-only-final-01`: without Compatibility, an automatic menu that cannot supply an initial snapshot safely returns to Nether Chested's original screen.
- 26.3 Fabric `core-profile-final-01`: without Compatibility, automatic capture and 512-item transfers; rejection of a profile for another Minecraft version; local profile reload/selection; inspector entry. Computer Use exercised the actual Export button and Escape key, and the saved JSON identifies Minecraft 26.3 correctly.

The final background correction followed these initial matrix checks. It was then tested in `26.3-fabric-core-artwork-final-02` (Core-only native artwork, retained profile and 512-item transfers), `1.21.11-fabric-background-final-01` (Core-only native trough transfers), `1.21.11-neoforge-background-final-01` (Core-only transfers and deferred inspector/template rendering), and `26.3-fabric-background-final-01` (the Compatibility baseline repeated after the fix). Finally, `1.21.11-neoforge-template-native-final-02` registers an ordinary public-API consumer with native slot backgrounds enabled, verifies the original dark Nether Chested artwork, and transfers a 512-item stack through that template.

The matrix rows are focused feature checks, not a full gameplay suite or a claim that every final binary was individually retested after the last shared renderer correction. The final build performs compilation and native-symbol checks on all targets; the changed rendering path was exercised on the representative Fabric/NeoForge cases listed above.

### Issues corrected during the port

- Nether Chested 1.21.1/1.21.11 does not accept desktop synchronization through vanilla's setter; its optional bridge now preserves initial and subsequent slot/cursor updates. Core-only reconstruction can fall back safely.
- Crabber's Delight 1.21.1 renamed its menu ID. The compact layout now selects the actual current menu.
- The three-slot Baby Backpack needed explicit storage classification for its sort control, rather than loosening general machine-slot inference.
- Modern profile filters and exports must use the actual Minecraft target. Escape must use the native key event's semantic helper on SDL-based 26.3.
- Newer Minecraft separates background and foreground rendering. Both phases are now called for native capture/automatic hosting; the Core-only texture check caught an omission masked by Compatibility's standard slot backgrounds.

The 1.21.1 NeoForge fixture also logged upstream Expanded Delight recipe/loot diagnostics with the selected Farmer's Delight release. The executed port checks cover its UI and slot transfers, not every processing recipe; no upstream recipe data was changed.

### Release output

The successful root build produces twelve production JARs in each directory:

- `dist/core/salts_inventory_update-<minecraft>-<loader>-0.1.6.jar`
- `dist/compatibility/salts_inventory_compat-<minecraft>-<loader>-0.1.3.jar`
- `dist/controlify/salts_inventory_controlify-<minecraft>-<loader>-0.1.1.jar`

Each directory includes `SHA256SUMS.txt`. Build checks verify production remapping, versions, exact Core dependency pins, loader isolation, optional metadata, public-API-only consumer references and exclusion of QA/development/source artifacts. The Controlify rebuild changes dependency metadata; controller gameplay was not rerun as part of this container port.

Final verification: `.\gradlew.bat build` succeeded (`build/native-all-targets/root-build-05.log`), including `verifyNativeContainerSymbols` on all twelve targets, the public-API examples, source feature parity, coverage regression tests and production JAR boundary checks. The applicable JUnit XML reports contain **1,669 cases, zero failures/errors and zero skips**; Gradle reused unchanged test outputs rather than forcing an unrelated full rerun. The coverage audit reports 132 available and 96 explicitly unavailable integration cells across the entire extension, including the 38 Project Everything cells above.

The combined download is `dist/salts-inventory-all-latest.zip`. Final hashes and this port's runtime lineage are saved in `output/native-container-port/validation.json`.

## Scope limits

This is support for conventional container interfaces and the reviewed upstream releases. Arbitrary custom picture-in-picture state implementations, interfaces with unrelated screen-wide behavior, unusual tab/navigation systems, every resource pack and every custom upstream configuration are not automatically guaranteed. Dedicated remote multiplayer and combinations with every existing integration require their own focused coverage. No complete functional suite is claimed for this port.
