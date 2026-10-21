# Ancient Remnants search crash: issue 16

Date: 2026-10-21. Scope: recipe-browser searching and codex tooltips in Core 0.1.5 + Compatibility 0.1.2. Controlify was absent.

## Reproduction

The exact [reported crash](https://github.com/Salt-and-Fries/Salt-s-Inventory-Update/issues/16) reproduced in a rendered Minecraft 26.1.2 NeoForge client using the production distribution JARs, official REI 26.1.819, Ancient Remnants 1.3.1 and their dependencies. Searching for `r` crashed during screen rendering:

```text
Failed to test search filter: "r" with stack [minecraft:item@ancient_remnants:monolith_codex!1 ancient_remnants:monolith_codex]
Caused by: NullPointerException ... Item$TooltipContext.registries() ... is null
    at MonolithCodexItem.appendHoverText(MonolithCodexItem.java:48)
```

An empty and a researched codex were both registered. Normal world-backed hover tooltips succeeded for both before the search crashed. The failure therefore concerned REI's search tooltip context, rather than a missing Ancient Remnants registry in the world.

Evidence is preserved under `build/ancient-remnants-issue16/`: original distribution copies and hashes in `before/`, input provenance in `gameplay/26.1.2-neoforge-rei-before/staged-artifacts.json`, and the log `26.1.2-neoforge-rei-before.log`. The actual Minecraft crash report is `versions/26.1.2/neoforge/build/runtime-smoke/browser-search-rei-before/crash-reports/crash-2026-10-21_13.50.35-client.txt`. A crashed baseline is not a passing gameplay profile.

## Fix and affected builds

REI indexes tooltip text using an empty vanilla tooltip context. A researched Monolith Codex needs the current world's research registry. Compatibility's REI adapter now decorates its own copied entry renderers: when a search context has no registry provider and a client world is available, the delegate receives that world's vanilla tooltip context. The mouse point, tooltip flags and search flag are retained. Other contexts and rendering continue through the original renderer.

This retains REI's native query parser, matching, item variants and custom renderers. It uses the public upstream entry-renderer API and the existing public Core recipe-browser API. There is no Ancient Remnants dependency, global entry-registry mutation, integration-specific Core change, new mixin or Core API change.

The common defect exists in five shared version implementations, so the fix is present in ten Compatibility JARs: Fabric and NeoForge for **1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3**. The two **1.20.1** implementations already obtain tooltips through the player and remain byte-identical. All twelve Core and twelve Controlify JARs also remain byte-identical.

The [official Ancient Remnants release list](https://api.modrinth.com/v2/project/ancient-remnants/version), checked on this date, supplies releases for six of our targets: 1.20.1 Fabric/Forge, 1.21.1 Fabric/NeoForge, and 26.1.2 Fabric/NeoForge. It supplies none for our 1.21.11, 26.2 or 26.3 targets. Their updated adapters compiled and packaged; actual Ancient Remnants gameplay is not claimed for unavailable releases.

## Focused runtime results

| Minecraft | Loader | Browser | Ancient Remnants | Outcome |
| --- | --- | --- | --- | --- |
| 1.20.1 | Fabric | REI 12.1.785 | 1.5.2 | PASS |
| 1.20.1 | Forge | REI 12.1.785 | 1.5.2 | PASS |
| 1.21.1 | Fabric | REI 16.0.799 | 1.5.2 | PASS |
| 1.21.1 | NeoForge | REI 16.0.799 | 1.5.2 | PASS |
| 26.1.2 | Fabric | REI 26.1.819 | 1.3.1 | PASS |
| 26.1.2 | NeoForge | REI 26.1.819 | 1.3.1 | PASS |
| 1.21.1 | Fabric | JEI 19.69.0.457 | 1.5.2 | PASS |
| 26.1.2 | NeoForge | JEI 29.47.0.111 | 1.3.1 | PASS |

The eight accepted profiles contain **358 report records, zero failures and 72 native screenshots**. Counts include setup, lifecycle stages and capture records, not only assertions. All six available Ancient Remnants version/loader targets passed REI checks; the two JEI checks did not reproduce a search crash.

Each profile opens a real desktop recipe-browser window in a fresh integrated-server world and checks these queries: `r`, `#r`/`#codex` (REI) or `$r`/`$pages` (JEI), `monolith`, `@ancient_remnants`, `crafting table`, a deliberate no-match query, and clearing the filter. It verifies that the registered codex variants retain working hover tooltips and remain present under the matching name, tooltip and mod filters. REI registers both an empty and researched codex; these JEI releases register only the empty variant. Nine native framebuffer screenshots are saved per successful profile. Selected 26.1.2 REI screenshots were visually inspected and show both codices in the results.

The earlier JEI profiles did not crash, but each had two failed assertions from using REI's `#` tooltip-search prefix. The tested JEI releases use `#` for tags and `$` for tooltips, verified against the actual upstream `ElementPrefixParser` bytecode in both JARs. Searching `#codex` or `#pages` correctly produced no tag matches. The final fixture uses `$pages`, which occurs in the actual tooltip body. The `jei-fixed` and `jei-fixed2` reports are retained as failed fixture evidence; final JEI profiles use `jei-fixed3`. No production JEI code was changed to satisfy those mistaken expectations.

These are automated in-game checks: the harness uses the production desktop search setter, actual upstream search engines and normal screen rendering. They are not physical mouse/keyboard or Computer Use playthroughs. The opt-in profile excludes unrelated container, transfer, resource-reload and gameplay scenarios. Setup/lifecycle records are distinguished from assertions.

Results, screenshots and original shipping input hashes are under `build/ancient-remnants-issue16/gameplay/<minecraft>-<loader>-<browser>-fixed/` (or `jei-fixed3` as described above). Forge additionally records its genuine production SRG-to-named development-runtime transform in `forge-remap-lineage.json`. QA implementation directories are excluded from the production mod classpath. The first two final reports identify their scope through their scenario, check names and staged manifests; later reports also include an explicit search scope field.

## Reproduce and verify

Prepare official upstream inputs, then choose a fresh run ID:

```powershell
python tools/prepare-compatibility-qa.py --minecraft 26.1.2 --loader neoforge --scenario browser-rei --extra-project ancient-remnants --output build/ancient-remnants-issue16/26.1.2-neoforge-rei-fixtures.json
$qaArgs = @(
  '-I', 'functional-tests/chest-close-qa.init.gradle',
  '-PqaBrowserSearchOnly=true', '-PqaScenario=browser-rei',
  '-PqaMinecraft=26.1.2', '-PqaLoader=neoforge', '-PqaRunId=rei-another-run',
  '-PcompatibilityQaManifest=build/ancient-remnants-issue16/26.1.2-neoforge-rei-fixtures.json',
  ':26.1.2:neoforge:runBrowserSearchQa', '--console=plain'
)
& .\gradlew.bat @qaArgs
```

Change the manifest, target and scenario together for another loader/version or JEI. The historical init-script name also supports this separate browser-only profile. `--extra-project` adds a mod only to isolated QA inputs; downloads and required dependencies are recorded with hashes in the manifest.

Focused production rebuild and evidence checks:

```powershell
.\gradlew.bat -I functional-tests/compatibility-artifacts-qa.init.gradle bundleLatestMods --console=plain
python tools/verify-ancient-remnants-search.py
python tools/verify-module-boundaries.py --sources --jars
python tools/verify-mouse-tweaks-artifacts.py
git -c core.safecrlf=false diff --check
```

The production rebuild writes all twelve Compatibility 0.1.2 JARs and checksums to `dist/compatibility/`. The combined 36-JAR download is `dist/salts-inventory-all-latest.zip`. Both the focused build and final bundle completed successfully. The delta audit passed with changes only to the REI adapter class entries in the ten affected Compatibility archives; metadata and all 26 other JARs match the preserved baseline. Source/production module boundaries, packaging/checksums/bundle verification and whitespace checks also passed. `build/ancient-remnants-issue16/summary.json` verifies the original crash, all eight final profiles, actual shipping input hashes, original upstream hashes and the restricted artifact delta.

## Limits

This validates searching with the listed native mod releases in development launchers using production artifacts. It does not validate every JEI loader combination, EMI, dedicated multiplayer, resource-pack combinations or Ancient Remnants research/codex gameplay. The complete functional suite was not run. No separate JEI defect is claimed solely from the issue's wording.
