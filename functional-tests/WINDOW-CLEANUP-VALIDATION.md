# Core window cleanup and API 6 validation

This record covers the Core 0.1.5, Compatibility 0.1.2 and Controlify 0.1.1 development builds after removing minimization and ghost pins. It supersedes earlier API5 and metadata-only evidence for these builds. The earlier [issue 15 reproduction](CHEST-CLOSE-ISSUE-15-VALIDATION.md) remains a record of the original failure.

The later [Tom's Storage 1.20.1 follow-up](TOMS-PICKUP-ISSUE-17-VALIDATION.md#minecraft-1201-follow-up) changes only one initialization class in the two 1.20.1 Compatibility JARs. Its passing focused gameplay results and new hashes supersede those two artifact hashes in this record. Core, Controlify and the other ten Compatibility artifacts remain unchanged.

## Changes

Hiding a persistent chest window now suspends its native container viewer without disposing its retained menu. Restoring the window resumes that viewer. Final removal stops an active viewer once and cannot reopen a disposed menu. Server viewer queries exclude hidden sessions. Bound ender-chest identity remains owned by the menu.

Minimization and ghost pins were removed from window rendering, controls, menus, settings, config serialization, packets, server state and the public API. No deprecated API stubs remain. Old ghost-pinned saved positions become ordinary pins; obsolete settings are ignored and disappear when saved. Ordinary pins, Global Pins, Persistent Windows and recipe ghost previews remain supported.

The public integration API is **6**. Window options contain five flags, state snapshots no longer contain minimized/ghosted fields, and the related actions, control and callbacks are absent. Compatibility's AE2 implementations and the compiled consumer examples use the reduced API. All bundled extensions were rebuilt. External integrations must recompile and update their provider descriptors to `apiVersion=6`; see the [migration guide](../docs/markdown/desktop_window_api.md#migrating-to-015--api-6).

## Standalone Core gameplay

All twelve shipping Core targets passed the focused rendered Minecraft profile, with a fresh integrated server and no Compatibility or Controlify installed:

| Minecraft | Loader | Chest scenarios | Result |
| --- | --- | ---: | --- |
| 1.20.1 | Fabric | 6 | PASS |
| 1.20.1 | Forge | 6 | PASS |
| 1.21.1 | Fabric | 6 | PASS |
| 1.21.1 | NeoForge | 6 | PASS |
| 1.21.11 | Fabric | 6 | PASS |
| 1.21.11 | NeoForge | 6 | PASS |
| 26.1.2 | Fabric | 6 | PASS |
| 26.1.2 | NeoForge | 6 | PASS |
| 26.2 | Fabric | 6 | PASS |
| 26.2 | NeoForge | 6 | PASS |
| 26.3 | Fabric | 6 | PASS |
| 26.3 | NeoForge | 6 | PASS |

Each profile tests Persistent Windows off/on with hold E, hold Escape and tap Escape. It asserts an initially open physical chest, then zero native viewers and a fully closed lid after dismissal. Holds last 900 milliseconds against the configured 500-millisecond threshold, and closure is checked before key release. All cases preserve the 32 stored dirt items. The two hide/restore cases reopen the same retained session, restoring its native viewer and lid while preserving contents.

Additional checks use the woven production `ChestMenu` with a counting native container to verify repeated hide, restore, final removal and attempted post-removal restoration do not double-close or revive viewers. Runtime reflection confirms removed exported members and config fields are absent. The real settings screen retains Persistent Windows and excludes the removed option rows.

There are **72 chest scenarios, 1,836 report records and 192 native screenshots** in the selected twelve-profile matrix. Evidence is under `build/core-window-cleanup/gameplay/<minecraft>-<loader>-api6-final1/`, including `results.json` and the staged production artifact hashes. The final 1.20.1 reruns use `api6-final2` because recompilation changed two compiler-generated local-variable names in debug tables. Forge uses the existing genuine SRG-to-MCP development-runtime transform; its original distribution hash and transformed runtime hash are recorded separately in `forge-remap-lineage.json`. Core loads from a production JAR, with implementation output directories excluded from the runtime classpath.

Screenshots visually reviewed include the 1.20.1 Fabric and 26.1.2 NeoForge closed chest and settings, plus the restored 26.1.2 NeoForge window with its retained items. These are selected visual checks, not a pixel comparison of every screenshot.

Reproduce with a fresh run ID, which preserves previous reports:

```powershell
$qaArgs = @('-I', 'functional-tests/chest-close-qa.init.gradle',
    '-PqaMatrix=true', '-PqaRunId=another-run', '-PincludeFunctionalTests=false',
    '-PsaltsDesktopRunDiagnostics=false', '-PsaltsDesktopRunTrace=false',
    '--no-parallel', '--max-workers=2', '--console=plain')
# Add :<minecraft>:<loader>:runChestCloseQa for each desired target.
& .\gradlew.bat @qaArgs ':26.3:neoforge:runChestCloseQa'
python tools/verify-chest-close-runtime.py --run-id another-run --allow-partial
```

## Build, API and companion checks

- The normal root build plus `:26.3:neoforge:compileCompanionDemo` passed. Its final log is `build/core-window-cleanup/root-build-release-final.log`. Source parity, API example compilation, API-only compile classpaths, module boundaries, integration coverage, production packaging and release bundle/checksum gates run through the root build.
- Current JUnit XML reports total **1,545 tests across 214 suites, with zero failures, errors or skipped tests**. This includes the shared common API tests, version-specific Core/Controlify checks, Compatibility tests and the loader-neutral controller model. Unchanged tasks may be up to date; their retained reports are included. Counts and groups are recorded in `build/core-window-cleanup/junit-summary.json`.
- New regression checks cover the reduced options/state schemas and actions/controls, absence of removed hooks/settings/internal state, preservation of unrelated config values, legacy ghost-layout migration and the ordinary two-state pin cycle.
- `tools/verify-window-feature-removal.py` scans all **36 production JARs, 8,696 classes and 220,877 member references**. It checks actual exported declarations, method/field references, constructor descriptors, API version 6, deleted packet/config fields and removed language keys. It also rejects the obsolete ghost-pin claim in Core's mod metadata. The report is `build/core-window-cleanup/api-removal-audit.json`.
- A real frozen older Compatibility binary is rejected by that audit for its `MINIMIZE` reference. This negative control establishes that a successful compilation alone is not being used to infer the absence of stale consumer calls.
- Native **26.3 Fabric and NeoForge** profiles load all three production mods, their pinned upstream Controlify dependencies and official integration fixtures. Fabric passed **233 distinct checks**; NeoForge passed **443**. The profiles produced 24 screenshots. They exercise public surface controls, native keyboard/tab/scroll navigation, Tom's Storage and JEI on both loaders, and AE2 network withdrawal/deposit/search plus Sophisticated storage/backpack interactions on NeoForge. These include AE2 recipe ghost slots, which are separate from the removed ghost-pin feature.
- Companion evidence is under `build/controlify-qa/26.3-<loader>-mod-compat-extended-jei-window-api6/`. `tools/verify-controlify-compat-runtime.py` verifies assertions, loaded production code sources, staged artifact inventories, upstream hashes and final release lineage; its summary is `build/core-window-cleanup/companion-runtime-summary.json`. Selected NeoForge AE2 network/cell-workbench and browser screenshots were visually reviewed.
- The final metadata correction removes only the words `ghost-pin, ` from Core's mod description and generated resource-pack description. Where a retained run predates that correction, `tools/artifact_lineage.py` requires the identical archive inventory, every class byte and every other resource byte, with exactly that text replacement in the mod metadata and optional `pack.mcmeta` entry. It records both runtime and final hashes. Its negative controls reject changed class bytes, mod identity or archive contents. This is explicit metadata-only lineage, not a claim that those JAR hashes are identical.
- `tools/generate-api-reference.py --check` confirms the 94-type public signature reference is synchronized. The current guide, descriptor examples and standalone companion demonstration target API6. `git diff --check` passes.

Verify the selected final Core matrix with:

```powershell
python tools/verify-chest-close-runtime.py --allow-description-correction `
    --target-run-id 1.20.1:fabric=api6-final2 --target-run-id 1.20.1:forge=api6-final2 `
    --summary build/core-window-cleanup/focused-runtime-summary.json
python tools/verify-window-feature-removal.py --summary build/core-window-cleanup/api-removal-audit.json
```

The opt-in metadata comparison is restricted to the exact correction above; an unrelated change still fails validation. Full-byte matches remain the default requirement.

Final artifact comparison passed: all 24 extension JARs match the frozen tested binaries exactly; ten Core JARs have only the verified description correction; both 1.20.1 Core JARs passed fresh `api6-final2` runs with exact final hashes. See `build/core-window-cleanup/final-byte-comparison.json` and `focused-runtime-summary.json`. The two final 1.20.1 reruns each passed all 153 records, in addition to the earlier complete matrix. All 36 distribution checksums and all 36 JAR members in the release ZIP were checked against the final files.

## Distribution

The normal root build collects only production artifacts, twelve per family:

- `dist/core/salts_inventory_update-<minecraft>-<loader>-0.1.5.jar`
- `dist/compatibility/salts_inventory_compat-<minecraft>-<loader>-0.1.2.jar`
- `dist/controlify/salts_inventory_controlify-<minecraft>-<loader>-0.1.1.jar`

Both extensions require Core 0.1.5 for the same game/loader. Fabric API and upstream integration/controller dependencies remain separate downloads. The root distribution also includes checksums and `dist/salts-inventory-all-latest.zip`.

## Coverage limits

These are focused checks for the changed behavior, not the complete functional suite. Core chest scenarios use a physical single chest and a separate counting-container lifecycle fixture; they do not exhaustively cover barrels, double chests, several ender chests, every modded container or multiple simultaneous players. Gameplay runs use disposable local integrated servers. Dedicated multiplayer, physical gamepads, operating-system keyboard input and exhaustive visual review of every integration/version are outside this run. Key/controller input in the harness is synthetic and routed through the production handlers.
