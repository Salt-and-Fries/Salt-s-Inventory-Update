# Third-batch focused validation: Minecraft 1.20.1

Implementation scope: [compact native windows](next-eight-1.20.1.md). Compatibility **0.1.3**, matching Core **0.1.6**. Production integration code was implemented by the primary agent; GPT-6.1 Terra at high reasoning performed the disposable-world gameplay checks, screenshot capture and PDF design.

The later [gear visual follow-up](gear-window-visual-fixes-1.20.1.md) supersedes the gear crops, selected-tab seams and corresponding screenshot pages below. The original recipe/item-operation evidence remains historical; the follow-up records its own build and runtime evidence.

## Build and API boundary

Executed from the repository root:

```powershell
.\gradlew.bat :compatibility:1.20.1:fabric:test --tests com.salts_inventory_update.compat.nativewindows.NativeBatchLayoutsTest :compatibility:1.20.1:fabric:remapJar :compatibility:1.20.1:forge:reobfJar --console=plain
python tools/verify-module-boundaries.py --sources
python tools/verify-module-boundaries.py --jars
```

The final build passed; all **five** targeted layout tests passed. They cover complete real-slot preservation, inverse pointer mapping, inactive drawer previews, exterior tabs, safe fallback for an unexpected topology, and ownership of the anvil name field. The source and packaged API-boundary checks passed. Packaging checks inspect dependencies and embedded classes; they are not gameplay tests.

Final build log: `build/project-everything-eight-mockups/build-implemented-05.log`. Boundary logs: `boundary-check.log` and `production-boundary-check.log` in the same directory. Final production artifact sizes and SHA-256 hashes: `production-artifacts.json` there.

Distributable files:

- `dist/compatibility/salts_inventory_compat-1.20.1-Fabric-0.1.3.jar`
- `dist/compatibility/salts_inventory_compat-1.20.1-Forge-0.1.3.jar`

These are the remapped/reobfuscated production artifacts, without an embedded Core. `dist/compatibility/SHA256SUMS.txt` records checksums. Only the two affected 1.20.1 Compatibility artifacts were refreshed for this batch.

## Gameplay evidence

The [implemented screenshot report](../output/pdf/siu-next-eight-implemented-windows-1.20.1.pdf) uses actual runtime captures, not the earlier design mockups. The detailed [validation record](../output/project-everything-eight-implemented/validation.json) and accompanying screenshot manifest identify the runs, states, artifact hashes, outcomes and remaining coverage limits. Original full screenshots and observer output are retained under `build/project-everything-eight-implemented/gameplay/`.

Checks are focused on the affected screens: native layout and slot mapping, compact bounds, movement, exterior tabs, recipe panels, input and result transfers, workstation transitions, processing, and relevant native controls. Verified real operations include fletching, reforging, anvil naming, smithing, grindstone output, gem linking, BetterEnd smelting, Ossuary carving, and Rune crafting. Item counts/results were checked against the integrated server rather than inferred from a seeded screenshot. Consult the validation record for additional checks and precise limits.

The drawer lock was inserted and removed through its native utility slot, changing the authoritative locked state and conserving the item. SmitherZ extraction consumed the socketed input and extraction hammer and returned one unsocketed sword; that randomized attempt did not return a separate gem, so gem survival is not claimed. The Chunk Loader consumed coal, activated with valid native upgrades, and changed its server chunk selection when clicked. Removing the drawer or Chunk Loader source closed its hosted window.

Issues corrected during this validation included drawer client-menu reconstruction, drawer source lifetime, complete title fitting, anvil text focus/cost rendering, synchronization of SmitherZ socket visibility and virtual linked-gem previews, and Deep Aether's `combiner_menu` registry alias.

## Fixture qualifications

Tests stage production SIU JARs into mapped development clients. Upstream release dependencies are mapped for those clients. Forge Runes embeds BundleAPI with an SRG method name in a mixin annotation; the QA fixture translates that annotation constant to the named development method, recording the transformed dependency's hash. This adjustment is confined to the excluded test fixture. No shipping upstream or SIU JAR is patched for it.

Fabric runs record their exact staged archive hashes separately. The later Fabric fixture used a Loader 0.19.5 build override, changing the loader minimum and build provenance metadata; its class/resource bytes match the final distribution apart from those two metadata entries. The preceding pinned run additionally predates the Forge-only Deep Aether menu alias. The entry-by-entry comparison is `build/project-everything-eight-mockups/tested-artifact-content-comparison.json`. This does not certify the oldest declared Fabric Loader version.

This is **not a complete functional suite**, clean-launcher certification, dedicated-server certification, or a promise about every upstream release, recipe, configuration, GUI scale, resolution or modpack combination. Core and Controlify production sources were not changed by this integration batch. Gravestone text editing remains native; Chipped and Simply Swords remain deferred.
