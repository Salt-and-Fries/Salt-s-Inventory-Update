# Controlify 0.1.1 release validation

Historical record: this report certifies the Core 0.1.4.2 release set below. Current builds use Core 0.1.5 with updated companion dependency pins; this report does not certify fresh gameplay on those relabeled artifacts. Original production JARs are preserved under `build/controlify-release-0.1.1/snapshots/r2/`. See [current validation](../VALIDATION.md) for the separate relabeling checks.

Core **0.1.4.2**, Compatibility **0.1.2**, Controlify **0.1.1**.

All twelve Minecraft/loader targets have matching independently packaged release JARs. The root build passed. All 42 packaged gameplay profiles passed against the exact current production inputs, with no failed scenario records in the accepted reports.

The profiles contain **16,616 distinct profile/check pairs**, **18,464 PASS records** and **778 native screenshots**. A check exercised in multiple profiles is counted once in each profile.

## Fixes and release behavior

- Core suppresses automatic regular-window help while a public alternative workspace host is registered. Fresh-world tests run before the synthetic controller connects, preserve the unseen preference, check automatic help when the host is temporarily unregistered, and check manual help after restoring it.
- Both companions declare the matching Core patch. Compatibility and Controlify keep independent release labels. The old Minecraft 1.20.1 exact-version conflict is removed.
- Modern visual probes measure the real icon/native-pane crossfade. They retain texture, geometry, native item and settled-pane checks without rejecting an intended transition.
- Updated focused native tests require live ingredient-state refresh, stable membership while the wheel remains open, and newly eligible recipes after reopening. This preserves the existing documented selection-stability behavior; the older wait incorrectly expected immediate membership changes.

## Executed gameplay profiles

| Minecraft | Loader | Scope | Optional browser | Distinct checks | Screenshots |
| --- | --- | --- | --- | ---: | ---: |
| 1.20.1 | Fabric | feature-parity | — | 166 | 7 |
| 1.20.1 | Forge | feature-parity | — | 166 | 7 |
| 1.21.1 | Fabric | feature-parity | — | 169 | 7 |
| 1.21.1 | NeoForge | feature-parity | — | 169 | 7 |
| 1.21.11 | Fabric | feature-parity | — | 169 | 7 |
| 1.21.11 | NeoForge | feature-parity | — | 169 | 7 |
| 26.1.2 | Fabric | all | — | 1978 | 93 |
| 26.1.2 | NeoForge | all | — | 1978 | 93 |
| 26.2 | Fabric | all | — | 1978 | 93 |
| 26.2 | NeoForge | all | — | 1978 | 93 |
| 26.3 | Fabric | all | — | 1915 | 92 |
| 26.3 | NeoForge | all | — | 1915 | 92 |
| 1.20.1 | Fabric | playtest-adapters | — | 82 | 6 |
| 1.20.1 | Forge | playtest-adapters | — | 82 | 6 |
| 1.21.1 | Fabric | playtest-adapters | — | 80 | 6 |
| 1.21.1 | NeoForge | playtest-adapters | — | 80 | 6 |
| 1.21.11 | Fabric | playtest-adapters | — | 80 | 6 |
| 1.21.11 | NeoForge | playtest-adapters | — | 80 | 6 |
| 26.1.2 | Fabric | public-api | — | 81 | 3 |
| 26.1.2 | Fabric | wheel-polish | — | 118 | 4 |
| 26.1.2 | NeoForge | public-api | — | 81 | 3 |
| 26.1.2 | NeoForge | wheel-polish | — | 118 | 4 |
| 26.2 | Fabric | public-api | — | 81 | 3 |
| 26.2 | Fabric | wheel-polish | — | 118 | 4 |
| 26.2 | NeoForge | public-api | — | 81 | 3 |
| 26.2 | NeoForge | wheel-polish | — | 118 | 4 |
| 26.3 | Fabric | public-api | — | 81 | 3 |
| 26.3 | Fabric | wheel-polish | — | 118 | 4 |
| 26.3 | Fabric | playtest-fixes | — | 147 | 16 |
| 26.3 | Fabric | status-tray | — | 55 | 4 |
| 26.3 | Fabric | crafting-qol | — | 94 | 2 |
| 26.3 | Fabric | wheel-stability | — | 115 | 5 |
| 26.3 | Fabric | mod-compat | jei | 233 | 8 |
| 26.3 | Fabric | mod-compat | rei | 240 | 8 |
| 26.3 | NeoForge | public-api | — | 81 | 3 |
| 26.3 | NeoForge | wheel-polish | — | 118 | 4 |
| 26.3 | NeoForge | playtest-fixes | — | 147 | 16 |
| 26.3 | NeoForge | status-tray | — | 55 | 4 |
| 26.3 | NeoForge | crafting-qol | — | 94 | 2 |
| 26.3 | NeoForge | wheel-stability | — | 115 | 5 |
| 26.3 | NeoForge | mod-compat | jei | 443 | 16 |
| 26.3 | NeoForge | mod-compat | rei | 450 | 16 |

The twelve baselines cover legacy ported features and modern full recorded suites. Focused profiles cover legacy adapters, public APIs, wheel presentation, playtest regressions, the status tray, crafting behavior and wheel stability. The four 26.3 optional-mod profiles exercise JEI/REI alongside available Tom’s Storage, AE2 and Sophisticated adapters; the loader-specific report lists the actual installed targets.

## Build and artifact verification

The root build gates passed. The retained JUnit XML contains 1,447 tests with zero failures, errors or skips; Gradle may reuse unchanged test tasks. The release-version Python regression covers independently labeled companions and rejects incorrect Core pins.

All 36 production JARs have the correct filenames, embedded mod versions and matching Core dependencies. Module checksums and the complete bundle match the final bytes. Distribution folders contain no stale JARs. QA fixtures remain outside production archives. The root gates check API-only compiler/bytecode boundaries and native Controlify/Minecraft symbols.

- `dist/core/`: twelve Core JARs.
- `dist/compatibility/`: twelve Compatibility JARs.
- `dist/controlify/`: twelve Controlify JARs.
- `dist/salts-inventory-all-latest.zip`: all 36 production JARs.

## Evidence and reproduction

- Matrix: `build/controlify-release-0.1.1/controlify011-all/results.json`.
- Root build: `build/controlify-release-0.1.1/root-build-r2.log`.
- Machine-readable totals, artifact hashes and per-profile report links: `build/controlify-release-0.1.1/final-validation.json`.
- Original failed attempts are retained under the matrix’s `attempts/` directories. Only successfully rerun, strictly verified reports contribute to these totals.

Run `powershell -NoProfile -ExecutionPolicy Bypass -File tools/Run-ControlifyReleaseQa.ps1 -Phase all -RunId controlify011 -Resume` after the root build. Resume verifies existing reports against current artifacts and reruns any stale or failing profiles.

## Coverage limits

These are real rendered Minecraft clients and integrated-server worlds launched through development tooling. Original production JAR inputs, loaded code sources, pinned upstream dependencies and native remap lineage are verified; legacy loaders create the recorded named-namespace runtime derivatives. Native controller events are synthetic. Physical controller hardware, installed-launcher profiles, dedicated/remote multiplayer servers and arbitrary modpacks remain unverified.

Unregistering a workspace host verifies Core’s default onboarding path while the companion is still loaded; it is not a separate Core-only installation test. The optional-mod profiles cover the listed 26.3 combinations, not every upstream mod version on every target. Passing this matrix does not establish that the mod is bug free.
