# Spatial GUI integration: focused validation

Historical 26.3 Fabric results. See [the port validation](SPATIAL-PORTS-VALIDATION.md) for the subsequent twelve-target rollout.

Tested on October 21, 2026, on Windows with Minecraft 26.3 Fabric, Core 0.1.5, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3 and Java 25. Scope is the optional Core integration on `Spatial-GUI-support-test`. This is focused validation of the affected features, not a complete functional or gameplay suite.

**Latest result:** [four-wall bounds and movement-control validation](WINDOW-BOUNDS-AND-SPATIAL-MOVEMENT-VALIDATION.md) supersedes the expanding-canvas design, artifact hashes and earlier visual acceptance claims below. These sections and PDFs remain a historical record, including dedicated-server checks on earlier artifacts.

## Upstream fixture and isolation

The client uses the unmodified published Spatial GUI 1.8 file, Modrinth version `8y9LJEzb`, together with YACL 3.9.7+26.3-fabric, Mod Menu 21.0.0 and Placeholder API 3.2.0+26.3. The Spatial GUI SHA-512 is:

```text
c6ddc8deab4189efc1868d83e69cd453c3e773eca2c67b5854e40284d9ccfa72ca4f9e67edfaed644aaed98e708a745dd09e4362340f308dd2c3ab2956ecd59c
```

Published 1.8 differs from the later upstream development API. The integration targets the released classes and configuration and gates its client mixins to the 1.8 version series. Supporting a later release requires separate validation.

`Run-SpatialGuiQa.ps1` excludes project implementation directories from the game classpath and loads the production Core JAR from an isolated mods directory. Test instrumentation is in a separate disposable QA mod. The localhost dedicated server runs production Core and the normal Fabric runtime, with no Spatial GUI, Compatibility, Controlify or QA mod. No production network protocol was changed.

## Executed gameplay checks

The final main focused run is `build/gameplay-qa/packaged-core/26.3-fabric-spatial-r8/results.json`: **96 passing result records, zero failures**. Records include both stage completion and individual assertions; this is not a count of 96 independent gameplay cases.

Coverage includes:

- Container and player windows sharing one third-person panel; native projected mouse pickup/place with authoritative integrated-server item state.
- First-person mouse, curved panel item interaction, and first-person crosshair presentation.
- Window Movement Control entering/exiting gameplay while retaining windows and server sessions; camera at the player's eye, cursor grab, native mouse deltas turning the actual player, and return to the desktop camera.
- Default Alt and remapped P Toggle bindings; the old binding is ignored after rebinding.
- Hidden inventory-window hotbar locking selection; number keys and wheel restoring held-slot selection during movement control.
- Spatial GUI disabled/re-enabled without losing the workspace; whole hotbar and its indicators on the detached desktop plane; clean close/session teardown.
- Physical-pointer world ray opening a reachable barrel in the same desktop; a player-eye obstruction preventing use despite the displaced camera; an out-of-reach target producing a miss.
- Out-of-panel carried-item right/left drops taking priority over world interaction, canonical server cursor counts changing 2 → 1 → 0, and no false edge slot or extra server menu.

Screenshots from this run are in its `screenshots/` directory, including `spatial-third-person-desktop.png`, `spatial-movement-gameplay-camera.png`, `spatial-first-person-curved.png`, `spatial-world-opened-barrel.png`, and `spatial-detached-hotbar-on-panel.png`.

Earlier computer-use checks in the running game also dragged and resized a window, transferred items between container/player windows, and physically toggled Alt between desktop and gameplay. Visual inspection confirmed the detached hotbar's hearts and hunger indicators share its tilted plane, with no flat duplicate.

## Hold controls and unit checks

The three existing `MovementControlStateTest` unit tests were freshly executed and passed. The XML is `versions/common/build/test-results/test/TEST-com.salts_inventory_update.client.MovementControlStateTest.xml`.

The final controls-only run, `build/gameplay-qa/packaged-core/26.3-fabric-spatial-controls-r2/results.json`, passed **54 result records with zero failures**. It includes sustained Alt Hold, a remapped P Hold binding, normal gameplay camera/HUD while held, and desktop restoration on release.

Sustained Hold coverage uses a QA-only `InputConstants.isKeyDown` polling seam in a real game. Production control state, camera, mouse grab and HUD code still execute normally. This distinguishes simulated held-key polling from physical keyboard testing. Short computer-use key chords did not produce an observable sustained Hold interval; extended physical-key Hold remains a manual coverage limit.

## Dedicated server

Core-only client: `build/gameplay-qa/packaged-core/26.3-fabric-spatial-remote-absent-r1/server-results.json` passed all seven client assertions plus the independent RCON inventory check. The server held one stack of 32 dirt in destination slot 10 and no source slot 9 stack after pickup/place. This also exercises optional-mixin startup with Spatial GUI and its client dependencies absent.

Spatial GUI client: `build/gameplay-qa/packaged-core/26.3-fabric-spatial-remote-present-final/server-results.json` passed all eight client assertions and the same independent RCON inventory check, including the active spatial panel assertion. Its screenshot is `screenshots/remote-spatial-desktop.png`. Both this run and the Core-only run used the final production Core hash on the client and server. The server requires no Spatial GUI installation and was stopped cleanly after the checks.

## Build and artifact verification

`:26.3:fabric:build -x test` passed, including the existing six compatibility-coverage regression tests, the source-boundary check and the target's packaging checks. Java unit testing was performed separately with the focused three-test movement-control command above. The log is `build/spatial-gui-analysis/final-module-build.log`.

`python tools/verify-module-boundaries.py --sources --jars` passed after staging the final artifact. It checks the existing Compatibility → public API boundary and production JAR contents, including that only 26.3 Fabric Core contains the isolated Spatial GUI classes/configuration, no upstream Spatial GUI classes are bundled, no required Spatial GUI dependency is added, upstream class references remain inside the isolated integration, and QA classes are absent.

The tested production JAR is `dist/core/salts_inventory_update-26.3-Fabric-0.1.5.jar`, also copied to the existing `build/release/0.1.5/` mirror. Its SHA-256 is:

```text
b8edfa2a06290138f9b541c805e46ef7a48886cb780fb6669692051dc298a5a6
```

This matches the production JAR logged by the main focused gameplay run, the final controls run, both final remote clients and the Core-only dedicated server. Comparing all 36 distribution JARs against the pre-change hash manifest found exactly one changed JAR, the target above; the other 35 were byte-identical. Both updated output directories have refreshed `SHA256SUMS.txt`. Evidence is in `build/spatial-gui-analysis/artifact-verification.json` and `final-boundary-audit.log`. `git diff --check` also passed.

## Reproducing the focused checks

From the repository root:

```powershell
.\functional-tests\scripts\Run-SpatialGuiQa.ps1 -Mode present -RunId spatial-review
.\functional-tests\scripts\Run-SpatialGuiQa.ps1 -Mode present -ControlsOnly -RunId spatial-controls-review
.\gradlew.bat :common:test --tests '*MovementControlStateTest' --rerun-tasks
.\gradlew.bat :26.3:fabric:build -x test
python tools/verify-module-boundaries.py --sources --jars
```

For the disposable dedicated-server fixture, launch with `-I functional-tests/spatial-server-qa.init.gradle :26.3:fabric:runSpatialQaServer`. In another shell start `python tools/verify-spatial-server.py --output <client-output-directory>`, then launch `Run-SpatialGuiQa.ps1 -Server` with the corresponding run ID. Repeat with `-Mode absent` to use a Core-only client. Stop the fixture with `python tools/verify-spatial-server.py --stop --output build/spatial-server-qa`. These fixtures bind only to localhost and are not deployment configuration.

## Remaining limits

Other Minecraft versions/loaders were not ported or gameplay-tested. Compatibility and Controlify JARs are unchanged. Combined Controlify presentation, shaders, unrelated camera/HUD mods, every Spatial GUI configuration permutation, every modded container, and long multiplayer stress sessions were not tested. Presentation animations were visually inspected during the focused runs, not measured frame by frame. Passing these checks does not establish that every mod combination is bug-free.

## Screenshot report follow-up

The requested visual report is generated at `output/pdf/Salts-Inventory-Spatial-GUI-Visual-Report.pdf` (ignored artifact, 30 pages). It includes real game screenshots, full-frame comparisons, a pixel crop of an observed visual concern, evidence labels, PDF bookmarks and coverage limits. `functional-tests/scripts/build-spatial-visual-report.py` is the reproducible builder; it uses ReportLab, Pillow and pypdf.

The fresh capture run `build/gameplay-qa/packaged-core/26.3-fabric-spatial-visual-report-r1/` recorded 123 passing assertion/stage records and 75 automatic framebuffer records. It also retained two fixture failures: the new scale probes called the obsolete `Minecraft.resizeDisplay` test method. The fixture was corrected to use the existing cross-version `resizeHudGui` helper. Only the scale/setup/cleanup profile was repeated as `spatial-visual-scale-r2`, with 31 total records and zero failures. The original visual run is not represented as a clean full-run pass.

Additional passing probes cover three-window overlap/focus, public-handle resizing, panel-edge hit rejection, explicit desktop exclusion, inventory/container category switches, first/third-person exclusions, automatic perspective detection and upstream HUD/hotbar hiding. Manual computer-use actions also dragged and resized an inventory window and moved the chest stack into the player window; four F2 captures were saved alongside the automatic captures. The production artifact SHA-256 remained unchanged.

**Visual concern:** the carried-item icon appears partially clipped near the chest edge in the pickup capture. The PDF preserves and enlarges that image on page 6. Transactions and canonical item counts passed; the rendering concern's cause and frequency remain unestablished and require a focused follow-up. No production fix was made during this screenshot/report task. Extreme window positions also visibly reach the camera frame boundary; those captures are retained.

Capture and build commands:

```powershell
.\functional-tests\scripts\Run-SpatialGuiQa.ps1 -VisualReport -Manual -RunId spatial-visual-report-r1
# During the manual pause, capture the desired UI actions, then create the output directory's continue file.
.\functional-tests\scripts\Run-SpatialGuiQa.ps1 -VisualReport -ScaleOnly -RunId spatial-visual-scale-r2
python functional-tests/scripts/build-spatial-visual-report.py
```

The builder currently expects the recorded initial fixture failures and their successful follow-up, so its evidence text cannot silently describe them as passing. The PDF was rendered with Poppler; all 30 pages were inspected in contact sheets, with full-size review of the dense reading-guide/evidence pages. These are screenshot-focused checks of the affected integration, not the complete functional suite.

## Problem investigation after visual review

The user's follow-up investigation produces `output/pdf/Salts-Inventory-Spatial-GUI-Findings-Report.pdf` (18 pages) through `functional-tests/scripts/build-spatial-findings-report.py`. It reviews the original PDF and raw frames, then records fresh in-game probes and actual computer-use drags. Production Core remains the same hash above; no implementation fixes were made during this investigation.

Finding groups:

- **F01, high:** logical desktop bounds do not keep projected windows/control buttons inside the physical screen. Right/bottom positions, narrower frames, and retained positions after GUI-scale changes reproduce clipping. Manual dragging reproduced right-edge loss and recovery via a remaining title strip.
- **F02, medium:** the detached hotbar is far below the window cluster, leaving roughly 350 pixels between the baseline window bottoms and HUD group in a 1280 x 900 frame.
- **F03, medium:** left-edge windows overlap the avatar; there is no reserved avatar region in the usable desktop layout.
- **F04, related high-impact trigger:** first/third-person pointer parallax can amplify window/HUD clipping. Disabling first-person parallax improved the corner-pointer case. This is not counted as a separately proven root cause from F01/F02.
- **F05, medium:** the carried stack is complete at an interior pointer position but clipped at the captured surface boundary. The count remained 32 in both scene records; returning it was confirmed by the server as chest 32 / carried 0. This narrows the previous report's visual concern.

`spatial-findings-r1` completed 68 assertion/stage/capture records with zero harness failures. Scene completion is not a visual acceptance assertion. `spatial-findings-hotbar-r1` completed 23 records with zero harness failures: a physical target inside the monitor accepted the projected click, and the server confirmed 32 dirt in hotbar slot 2. This distinguishes low/unstable presentation from a general hotbar transaction failure.

The focused matrix includes 1280 x 900, 1280 x 720 and 1000 x 900; GUI scales 2/3; first/third-person, free mouse, curve and crosshair; parallax on/off; window bounds/size; carried-item positions; and Alt Toggle. It is not the complete gameplay suite. No new dedicated-server, controller, shader, full combat, extreme-FOV or unrelated-mod matrix was run. Other loaders/versions remain outside scope. All 18 final PDF pages were rendered and visually inspected, with enlarged crops for the carried-item finding.

```powershell
.\functional-tests\scripts\Run-SpatialGuiQa.ps1 -Findings -Manual -RunId spatial-findings-r1
.\functional-tests\scripts\Run-SpatialGuiQa.ps1 -Findings -HotbarProbe -RunId spatial-findings-hotbar-r1
python functional-tests/scripts/build-spatial-findings-report.py
```

## Implemented visual fixes

The user approved retaining one desktop coordinate system/aspect ratio and fitting the complete spatial panel, without rearranging individual windows during movement-control transitions. The fixes remain isolated to `core-integrations/spatial-gui/` and therefore only Minecraft 26.3 Fabric Core. There are no public API or network-protocol changes.

- **F01/F04:** the whole surface is uniformly fitted and positioned in the visible camera rectangle, with margins. The fitter checks flat corners or all curved-mesh vertices, includes parallax/camera changes and updates the corresponding interaction basis. Fitting the fully open footprint preserves upstream opening/closing scale animation. If a smaller GUI cannot contain retained window bounds, one proportional logical canvas preserves those bounds and input applies its inverse scale.
- **F02:** the native detached hotbar and its survival decorations use a compact dock below the window cluster. Rendering, Core overlays and slot targeting share the dock offset. Gameplay movement restores the normal native HUD position.
- **F03:** third-person fitting reserves a column around the projected, expanded player volume and chooses the larger available side.
- **F05:** Core's existing carried-stack renderer runs once in the final physical-cursor HUD pass, inset from the screen edges. The captured duplicate is suppressed. Native server cursor, placement and outside-drop behavior are retained.

### Final focused runs

All four runs below loaded the exact rebuilt production JAR, SHA-256 `bb4068ff29ef826b3369ec30b0b9e33f9e08c88cfac55008a017572f93224e24`. Each completed with zero failures. Counts include assertion, stage and capture records rather than independent test cases.

| Run ID | Records | Affected coverage |
| --- | ---: | --- |
| `spatial-fixes-r3` | 92 | Original findings matrix, 24 projected-containment records, real pickup and authoritative chest return, Alt round trip, screenshots and manual pause |
| `spatial-fixes-regression` | 104 | Native and curved input, Alt Toggle/rebinding, gameplay camera and hotbar selection, feature disable/re-enable, world use/reach/obstruction, outside drops and close |
| `spatial-fixes-hotbar` | 25 | Compact dock target physically on-screen, visible click and authoritative integrated-server hotbar slot 2 containing 32 dirt |
| `spatial-fixes-absent` | 21 | Core-only startup, no Spatial GUI or its dependencies, native pickup/place and cleanup |

The findings matrix covers 1280 x 900, 1280 x 720 and 1000 x 900; GUI scales 2/3; window edges and a larger window; first/third person; free mouse, curved and crosshair modes; parallax on/off; carried-item interior/title/edge/outside positions. Before/during/after scene JSON confirms that window IDs, positions and sizes match across Alt and GUI-scale round trips; the report builder independently asserts these comparisons and writes `layout-preservation.json`.

Actual computer use on the final build dragged the inventory title toward the right screen edge, then clicked the chest source and player destination. F2 screenshots `manual-right-edge-fixed.png` and `manual-transfer-fixed.png` retain the complete controls and show the transferred 32 dirt. Visual inspection of the final raw frames confirmed the revised placement; containment assertions alone are not treated as a readability check.

An initial QA compilation failed because Java disallows a compound `var` declaration; that fixture was corrected before gameplay. The first production fitter was exercised in `spatial-fixes-r2`; visual review found excessive shrinking in the first-person corner-pointer case. It was replaced with a maximum-fitting-scale search and the final visual matrix was repeated as `r3`. Neither intermediate run is represented as validation of the final artifact.

### Final build, distribution and report

`:26.3:fabric:build -x test` passed, including the existing six compatibility-coverage tests and source-boundary check. The final source/JAR audit `python tools/verify-module-boundaries.py --sources --jars` passed. `git diff --check` passed. No complete functional suite or unrelated-loader build was run.

The rebuilt file is `dist/core/salts_inventory_update-26.3-Fabric-0.1.5.jar`, mirrored to `build/release/0.1.5/`. Both folders' checksum lists were refreshed. Comparing all 36 distribution JARs against the pre-experiment manifest found this target as the only changed file; the other 35 remained byte-identical. No upstream Spatial GUI classes, QA classes or required Spatial GUI dependency are bundled. Evidence is `build/spatial-gui-analysis/fixes-artifact-verification.json`, `fixes-module-build.log` and `fixes-boundary-audit.log`.

The refreshed `output/pdf/Salts-Inventory-Spatial-GUI-Fixes-Report.pdf` has 16 pages: unretouched before/after full frames, explicitly labeled carried-item pixel crops, physical drag/transfer evidence, hotbar transaction results and coverage limits. All pages were rendered with Poppler and visually reviewed, including full-size inspection of the evidence page and enlarged item crops.

Reproduce the final focused profiles and report:

```powershell
.\functional-tests\scripts\Run-SpatialGuiQa.ps1 -Findings -Fixes -Manual -RunId spatial-fixes-r3
# Capture the manual F2 shots during the pause, then create the run directory's continue file.
.\functional-tests\scripts\Run-SpatialGuiQa.ps1 -RunId spatial-fixes-regression
.\functional-tests\scripts\Run-SpatialGuiQa.ps1 -Findings -Fixes -HotbarProbe -RunId spatial-fixes-hotbar
.\functional-tests\scripts\Run-SpatialGuiQa.ps1 -Mode absent -RunId spatial-fixes-absent
.\gradlew.bat :26.3:fabric:build -x test
python tools/verify-module-boundaries.py --sources --jars
python functional-tests/scripts/build-spatial-fixes-report.py
```

The PDF builder requires the recorded screenshots, successful result files and current staged artifact verification. After a new build, restage the target and refresh its hash evidence before regenerating the report. Runs use disposable worlds and should not reuse old result directories as evidence for new code.

### Coverage limits after these fixes

These are focused checks of the affected integration. No fresh dedicated-server session was run after the visual fixes; earlier dedicated-server results above apply to the earlier artifact. Final transaction checks used a real integrated server. Other Minecraft versions/loaders, Controlify presentation, shaders, unrelated HUD/camera mods, every modded container/upstream setting, extreme FOV/near-camera avatar configurations and long multiplayer sessions remain untested. Animation continuity was preserved in code and observed during play but not measured frame by frame.

Fitting a complete large desktop into a small view can reduce physical text size. If the projected avatar consumes almost the entire view, the fitter cannot guarantee both a usable minimum panel size and a fully reserved avatar column; those extreme camera configurations were not validated. Windows retain their existing paint priority over the compact dock if an unusually dense layout overlaps it.
