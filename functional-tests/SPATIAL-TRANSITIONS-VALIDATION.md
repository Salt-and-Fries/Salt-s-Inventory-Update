# Spatial GUI transition validation

Historical 26.3 Fabric results. See [the port validation](SPATIAL-PORTS-VALIDATION.md) for the subsequent twelve-target rollout.

Date: 2026-10-22. Scope: optional Spatial GUI integration in **26.3 Fabric Core 0.1.5**.

## Changes

- Opening begins from the normal presentation and smoothly follows the fitted spatial surface as the camera clears the avatar.
- Alt release starts from the actual displayed panel. Projected position and size use critically damped motion, avoiding abrupt side changes or acceleration caused by interpolating world-space depth.
- Alt entry retains its screen-aligned endpoint and reversible elapsed-time camera transition.
- Closing retains the fitted presentation until Spatial GUI finishes drawing its captured texture. Its scale/fade animation no longer falls back to an unrelated world-space placement.
- Closing interpolates from the current camera to the saved gameplay perspective, including when movement control is active. Spatial GUI's temporary third-person override cannot become the closing destination.
- Rapid reversals, closing during opening and reopening during closing continue from the visible pose. A completed session clears its presentation state.

Window coordinates, the fixed desktop canvas, the public API and the server protocol are unchanged by this follow-up.

## Executed checks

`Run-SpatialGuiQa.ps1 -Transitions -RunId transitions-final` ran against the production JAR with a separate QA mod and unmodified Spatial GUI 1.8. Runtime: Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, YACL 3.9.7+26.3-fabric, Mod Menu 21.0.0 and Java 25.

**34 transition scenarios passed; 128 PASS records, 34 measurements and 69 screenshots; zero failures.** The probe recorded 4,647 samples, including one preceding frame when available.

| Presentation/settings | Scenarios |
| --- | --- |
| Third-person, first-person flat, first-person curved | Opening; Alt entry; Alt release after changing facing; rapid Alt reversals; closing; closing during opening; reopening during closing; closing while movement control is active. Eight scenarios in each presentation. |
| Hold mode and detached HUD | Opening; holding Alt; releasing Alt; closing the last window through its handle. |
| Persistent windows | Opening; hiding; restoring; permanent cleanup. |
| Container workspace | Adding the inventory window; closing all windows. |

Server-authoritative pickup/place checks also passed after the new presentation settled: the real chest slot and its rendered target agreed, with the expected 32-item transfer.

The QA-only probe observes `addScreenQuadMesh`, where the surface is actually submitted. It projects the mesh corners, including curved endpoints, through the current camera. It rejects non-finite values and flags corner steps exceeding `max(0.06 NDC, elapsedSeconds * 8 NDC/s)`. This detects large discontinuities; it is not a claim of mathematical continuity or subjective smoothness at every frame rate. An earlier probe sampled stale transforms after the closing texture stopped drawing; those invisible samples were excluded by moving observation to mesh submission.

The previous distribution JAR reproduced opening and Alt-release discontinuities. Alt entry remained smooth, matching the user report. The final run detected no discontinuities under the stated threshold.

`Test-SpatialSurfaceTransition.ps1 -JavaHome C:/Users/texca/.jdks/ms-25.0.2` passed **13 deterministic assertions** for opening, exact normal alignment, reversals, closing placement/scale, reopening, independent sessions and depth-independent projected motion.

Build and packaging checks:

- `gradlew.bat :26.3:fabric:build -x test` passed, including the six existing compatibility coverage checks and source boundary verification. The separate deterministic test above supplies the focused transition unit coverage.
- `python tools/verify-module-boundaries.py --sources --jars` passed: source boundaries and all 12 Core + 12 Compatibility production JARs.
- `git diff --check` passed.
- The refreshed distribution and release mirror match the exact production JAR used by the successful gameplay run. No QA classes or upstream Spatial GUI classes are embedded.
- Hash comparison confirmed that the other **35 distribution JARs are unchanged**.

## Evidence and artifacts

- [Gameplay result](../build/gameplay-qa/packaged-core/26.3-fabric-transitions-final/results.json)
- [Frame measurements and screenshots](../build/gameplay-qa/packaged-core/26.3-fabric-transitions-final/)
- [Build log](../build/spatial-gui-analysis/transitions-release-build.log)
- [Artifact verification](../build/spatial-gui-analysis/transitions-artifact-verification.json)
- [Updated Core JAR](../dist/core/salts_inventory_update-26.3-Fabric-0.1.5.jar)
- [Release mirror](../build/release/0.1.5/salts_inventory_update-26.3-Fabric-0.1.5.jar)

SHA-256: `0362ac6146e2311db25ee1aa1d59a074eefdba552388214a735b4cd169bf45b0`.

## Coverage limits

This was focused transition QA, not the complete functional/gameplay suite. Automated native callbacks exercised Toggle; Hold used the existing QA-only physical-key polling seam, with production movement state, camera and rendering. A sustained physical Alt-plus-mouse session was not repeated. Representative during/settled screenshots were visually inspected; screenshots alone do not establish motion quality.

The run used a disposable integrated-server world, FOV override enabled, standard animation settings, fixed GUI scale, three panel presentations and both hotbar placements. Other camera mods, shaders, extreme custom animation/transform settings, arbitrary frame rates, third-person-front gameplay and multiplayer latency were not retested. Other Minecraft/loader targets were not relaunched because this follow-up changes only the optional 26.3 Fabric integration.
