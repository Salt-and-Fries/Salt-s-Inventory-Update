# Four-wall bounds and Spatial GUI movement control

Historical boundary and 26.3 Fabric results. See [the port validation](SPATIAL-PORTS-VALIDATION.md) for the subsequent twelve-target spatial rollout.

Focused validation on October 21, 2026, on `Spatial-GUI-support-test`. Core remains version **0.1.5**. This follow-up supersedes the earlier expanding-canvas design and visual acceptance claims in the historical Spatial GUI PDFs. It is not a complete functional or gameplay suite.

## Implemented scope

**All twelve Core targets:** ordinary windows remain within all four desktop edges during dragging, resizing, saved-state recovery and resolution/GUI-scale changes. Bottom dragging constrains the entire window rather than only its title bar. Later layout refreshes preserve a window placed against a wall instead of adding an eight-pixel inset. Initial automatic placement can still leave its normal spacing.

Viewport recovery delivers API `resized`/`moved` callbacks when it changes geometry. Those callbacks see the current viewport environment, allowing consumers to update cached child layouts. Existing native `PRESERVE`/`ADAPT` surface policies retain their contract; this change does not rescale original third-party artwork or add public API methods.

The six version-specific `InventoryDesktopScreen` source trees are shared by the corresponding Fabric and Forge/NeoForge builds. No compatibility-specific code was added to these shared paths.

**26.3 Fabric Spatial GUI experiment only:** the logical desktop is fixed to the normal GUI viewport's dimensions/aspect ratio. Moving a window cannot grow the canvas or progressively reduce its scale. The complete spatial panel still fits beside the avatar. A wall-aligned window reaches the corresponding physical screen edge in the settled normal presentation.

Movement control animates the camera to first person and expands the panel into the normal screen-aligned desktop, then restores the spatial presentation on release. Window IDs, positions, sizes and sessions are preserved across this transition. Camera, panel, curvature and FOV use the same elapsed-time movement transition. The native mouse wrapper runs outside Spatial GUI's coordinate remapping, keeping relative camera input separate from projected desktop input. Alt/remapped bindings and Hold/Toggle remain supported.

Spatial changes are isolated under `core-integrations/spatial-gui/`. Other targets contain no Spatial GUI integration. There are no network-protocol or API-version changes. Compatibility 0.1.2 and Controlify 0.1.1 are unchanged.

## Final executed gameplay profiles

All four profiles below finished with **zero failures**. Counts include assertions, stage completion, informational geometry records and captures; they are not counts of independent test cases.

| Target / configuration | Run ID | Records | Code source |
| --- | --- | ---: | --- |
| 26.3 Fabric, Core without Spatial GUI | `core-walls-final` | 53 | Packaged production Core JAR |
| 26.3 Fabric, Core + Spatial GUI 1.8 | `spatial-walls-combined-final-r2` | 186 | Packaged production Core JAR |
| 1.20.1 Forge, Core | `window-walls-release` | 53 | Development classes with separate opt-in QA sources |
| 1.21.11 NeoForge, Core | `window-walls-release` | 53 | Development classes with separate opt-in QA sources |

Both packaged Fabric profiles logged this identical production Core SHA-256:

```text
ef3e6a1bc1b5b51ea2522897591594204373b0a343d99d08d781ea26c69f1d01
```

Boundary checks dispatch the screen's actual title-drag callbacks toward each wall, assert complete presentation bounds, and require the frame to reach the exact edge. Repeated bottom/right drags do not change the canvas. Oversized resize requests and GUI scales 2/3 exercise viewport recovery. A public-API client-only view verifies accepted resizing, full bounds, resize notification and the current viewport inside that notification.

The combined spatial profile also checks:

- Fixed canvas dimensions and scale throughout dragging and movement control.
- Intermediate camera-to-eye distances during entry and return, rather than only an animation-state flag. Recorded entry samples progress from about 2.08 blocks to effectively zero.
- Unchanged window geometry across Hold/release, native relative-delta callbacks turning the player, and a settled camera at the player's eye.
- Normal-plane pointer alignment with the physical viewport, default Alt, remapped P, Hold/Toggle, native HUD restoration and held-slot selection.
- Real chest pickup/place with authoritative integrated-server counts, curved-panel item interaction, disabled/re-enabled Spatial GUI and clean close.
- Reachable world use, player-eye obstruction, range limits and outside-drop precedence/counts.

## Actual computer-use observations

The final packaged spatial run also received actual native mouse/key input through the Computer Use skill. A barrel window was dragged toward the spatial bottom/right walls. Physical Alt Toggle expanded it into a normal first-person window touching the physical right/bottom edges; returning restored its spatial placement. The title controls and full container body stayed visible.

Raw F2 captures are in:

```text
build/gameplay-qa/packaged-core/26.3-fabric-spatial-walls-combined-final-r2/screenshots/
  manual-spatial-edge.png
  manual-normal-edge.png
```

![Final spatial wall placement](../build/gameplay-qa/packaged-core/26.3-fabric-spatial-walls-combined-final-r2/screenshots/manual-spatial-edge.png)

![The same container at the normal desktop walls](../build/gameplay-qa/packaged-core/26.3-fabric-spatial-walls-combined-final-r2/screenshots/manual-normal-edge.png)

**Physical-look coverage limit:** Computer Use's absolute drag input did not generate relative mouse look even after the desktop was closed. The F3 facing values stayed unchanged in ordinary gameplay as well. Sustained physical Alt + relative mouse look therefore remains a manual coverage limit. The automated Hold checks use the QA-only key-poll seam and supply relative deltas through Minecraft's real `MouseHandler.onMove`; production control state, camera, mouse handling and rendering still execute. These are not represented as a complete physical-key/mouse test.

## Build and artifacts

The Core-only release collection, release metadata/content verification, non-Fabric package isolation, checksums and cross-version source-feature parity passed:

```powershell
.\gradlew.bat collectModJars verifyReleaseBundle verifyNonFabricModJars writeReleaseChecksums sourceFeatureParity --console=plain
python tools/verify-module-boundaries.py --sources --jars
git diff --check
```

The twelve rebuilt production Core JARs are in `dist/core/`, with the release mirror in `build/release/0.1.5/`. Both folders have updated `SHA256SUMS.txt`. The staged 26.3 Fabric file matches the packaged gameplay hash above. All twelve Core distribution files changed against this task's starting manifest; all 24 Compatibility/Controlify distribution files remain byte-identical. No QA or upstream Spatial GUI classes are bundled in Core.

Artifact hashes and comparisons are recorded in `build/spatial-gui-analysis/walls-artifact-verification.json`. Build/audit logs are `walls-core-release-final.log` and `walls-boundary-audit.log` in the same directory. Source-feature checks now also recognize the existing HUD layout API, current help text and separate Fabric-only HUD mixin registration; the old checks had stale assumptions about those earlier changes.

## Reproduction and fixture corrections

Run the focused packaged configurations:

```powershell
.\functional-tests\scripts\Run-SpatialGuiQa.ps1 -Walls -Mode absent -RunId core-walls-review
.\functional-tests\scripts\Run-SpatialGuiQa.ps1 -Walls -WithRegression -Manual -RunId spatial-walls-review
# After the manual review, create the run output directory's continue file.
```

For the other loaders, set `SIU_QA_WINDOW_WALLS_ONLY=true` and invoke `Run-GameplayQa.ps1 -Scenario core -IntegrationOnly` with the desired `-Minecraft`, `-Loader` and a fresh `-RunId`. Clear that environment variable afterward. Result JSON and launch logs live under `build/gameplay-qa/`; each run uses a disposable world.

Intermediate runs are retained but are not counted as final passing evidence. They include a manual-pause timeout, older-Java fixture compilation errors, a fixed-size API fixture that initially rejected the requested resize, and the real eight-pixel post-drag inset corrected in Core. A combined run also exposed an inverse-projection fixture solver stuck on upstream's clamped miss coordinates. The corrected solver rejects edge-sentinel plateaus and searches interior, on-monitor hits; the repeated sequence passed real server-backed pickup/place after camera movement. No production ray-mapping workaround was added to hide that fixture failure.

## Remaining limits

Only the three representative Minecraft/loader configurations in the table received gameplay checks; all twelve Core targets were compiled, packaged and source-checked. No complete gameplay suite, fresh dedicated-server session, controller presentation, shader/camera-mod matrix, every native container, extreme viewport/FOV matrix or long multiplayer session was run. Earlier dedicated-server results belong to earlier artifacts. Current transaction checks used a real integrated server. Very small views or author-owned native surfaces larger than their viewport still require their documented adaptation policy. No claim of universal bug-free modpack behavior is made.
