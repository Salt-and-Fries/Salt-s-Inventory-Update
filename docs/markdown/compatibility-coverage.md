# Compatibility coverage audit

Original upstream availability reviewed 2026-10-20; Raised additions reviewed 2026-10-21. Current release labels are Core 0.1.5, Compatibility 0.1.2 and the optional Controlify companion 0.1.1 on every target. The integration gameplay and packaged-JAR evidence below records the preceding Core 0.1.4 / Compatibility 0.1.0 builds; it is not a claim of rerunning the complete integration matrix for the newer artifacts. The controller release has its own [validation record](../../controlify/VALIDATION.md).

Use the rebuilt Core and Compatibility pair from this checkout. The new 1.20.1 EMI adapter relies on the additive Core rendering hooks described below.

## Available upstream releases

All targets include adapters for Tom's Simple Storage, JEI and REI. Mod Menu is Fabric-only. The table describes published native inputs for the exact Minecraft version and loader; a project page listing several loaders and game versions does not mean every combination exists.

| Minecraft | Loader | AE2 | EMI | Sophisticated Core / Backpacks / Storage | Mod Menu | Raised |
| --- | --- | --- | --- | --- | --- | --- |
| 1.20.1 | fabric | 15.4.11 | 1.1.24+1.20.1+fabric | Maintained Fabric ports | 7.2.2 | 6.0.2 |
| 1.20.1 | forge | 15.4.11 | 1.1.24+1.20.1+forge | Official releases | No upstream release | 6.0.2 |
| 1.21.1 | fabric | No upstream release | 1.1.24+1.21.1+fabric | Maintained Fabric ports | 11.0.5 | 6.0.2 |
| 1.21.1 | neoforge | 19.2.18 | 1.1.24+1.21.1+neoforge | Official releases | No upstream release | 6.0.2 |
| 1.21.11 | fabric | No upstream release | No upstream release | No upstream release | 17.0.1 | 6.0.2 |
| 1.21.11 | neoforge | No upstream release | No upstream release | Official releases | No upstream release | 6.0.2 |
| 26.1.2 | fabric | No upstream release | No upstream release | No upstream release | 18.0.2 | 6.0.2 |
| 26.1.2 | neoforge | 26.1.13-beta | No upstream release | Official releases | No upstream release | 6.0.2 |
| 26.2 | fabric | No upstream release | No upstream release | No upstream release | 20.0.3 | 6.0.2 |
| 26.2 | neoforge | 26.2.1-alpha | No upstream release | Official releases | No upstream release | 6.0.2 |
| 26.3 | fabric | No upstream release | No upstream release | No upstream release | 21.0.0 | 6.0.2 |
| 26.3 | neoforge | 26.3.1-alpha | No upstream release | Official releases | No upstream release | 6.0.2 |

The [reviewed snapshot](../../compatibility/coverage-targets.json) now records all 120 cells across twelve targets and ten upstream projects: 88 available inputs and 32 unavailable combinations. This includes twelve newly added Raised cells; their focused runtime coverage is reported separately below. Each available input has a version ID, download URL and SHA-512 hash. Fabric Sophisticated entries use Salandora's maintained ports, which have a different native ABI from the official Forge/NeoForge releases.

AE2's [official downloads](https://appliedenergistics.org/download) and [published versions](https://modrinth.com/mod/ae2/versions) provide Fabric 1.20.1 but no modern Fabric release or 1.21.11 release for either loader. A separate search for published modern Fabric ports found add-ons and resource packs, rather than a maintained native AE2 port. Unavailable combinations cannot be enabled by relabeling a JAR.

Primary release listings: [Tom's Storage](https://modrinth.com/mod/toms-storage/versions), [JEI](https://modrinth.com/mod/jei/versions), [REI](https://modrinth.com/mod/rei/versions), [EMI](https://modrinth.com/mod/emi/versions), [Mod Menu](https://modrinth.com/mod/modmenu/versions), [Sophisticated Core](https://modrinth.com/mod/sophisticated-core/versions), [Backpacks](https://modrinth.com/mod/sophisticated-backpacks/versions), [Storage](https://modrinth.com/mod/sophisticated-storage/versions), and the Fabric ports of [Core](https://modrinth.com/mod/sophisticated-core-unofficial-fabric-port/versions), [Backpacks](https://modrinth.com/mod/sophisticated-backpacks-unoffical-fabric-port/versions) and [Storage](https://modrinth.com/mod/sophisticated-storage-unofficial-fabric-port/versions).

## Implemented coverage additions

- EMI on Fabric and Forge 1.20.1, using its native recipe rendering, lookup, bookmarks and recipe transfer.
- Sophisticated Core, Backpacks and Storage on Fabric 1.20.1 and 1.21.1, with native menus, attached upgrade controls, settings, authenticated networking and extended opening data.
- Corrected Tom's Storage 1.20.1 adapters for the native 1.7.x ABI, seven native menus, legacy atlases, terminal settings and server state. Filing cabinets and the newer configurator item do not exist in this native release; its nine-slot filtered connector is supported instead.
- Storage source identity now comes from the authenticated native Sophisticated menu. Native factories keep the full opening payload, including the newer linked-storage data, rather than relying on a fixed payload length.
- The Fabric 1.20.1 slot-rendering redirect supports the native widened method in both development and production namespaces. Release inspection rejects an unmapped production selector.
- Emitter controls, inventory-link rows and tag-filter panels have sufficient default and minimum bounds on every target.
- Tom's Storage labels, counts and controls use explicit opaque text colors. Framebuffer probes verify the emitter caption, count and all four adjustment labels; interaction-only checks had missed their transparency on newer renderers.
- Additive public Core 1.20.1 recipe layout and tooltip hooks, matching the supported 1.21.1 provider contract. No compatibility adapter imports Core internals.
- Checked-in availability audit and regression tests that reject missing adapters, missing loader rows and unregistered Fabric hooks. Root builds also inspect production JAR coverage.

The [Fabric adapter guide](../../compatibility/sophisticated-fabric/README.md) describes native differences and lifecycle handling. Third-party dependencies remain optional and are never bundled.

## Reproduce validation

```powershell
python tools/audit-compatibility-coverage.py
python tools/test-compatibility-coverage.py
python tools/verify-module-boundaries.py --sources
.\functional-tests\scripts\Run-CompatibilityCoverage.ps1 -RunId review-coverage
.\gradlew.bat build
python tools/audit-compatibility-coverage.py --jars
```

A stopped matrix can retain completed passes while retrying affected scenarios. Resume verifies the original report, successful Gradle log and current upstream version pins; `PREVIOUS_PASS` rows point to their original evidence. Select affected scenarios with `-RerunScenarios`, or run a focused matrix first and pass both reports to `-ResumeFrom`. Use a new `-RunId` so earlier failed evidence remains available.

```powershell
.\functional-tests\scripts\Run-CompatibilityCoverage.ps1 -RunId review-coverage-r2 -ResumeFrom @('build/compatibility-coverage/runs/review-coverage/results.json') -RerunScenarios @('sophisticated')
```

Gameplay runs create disposable integrated-server worlds, use exact hash-checked upstream inputs, capture framebuffers and write machine-readable results under `build/gameplay-qa`. The runner skips only combinations explicitly unavailable in the reviewed snapshot. Snapshot QA uses explicit installed Loader/API/NeoForge versions sufficient for the chosen upstream release; ordinary production compile baselines and dependency metadata are unchanged. Browser cases assert the requested provider, click a visible transfer button, and check server-authoritative ingredients, crafting output and cursor conservation. Sophisticated cases exercise native menus, item mutation, upgrade tabs, settings and storage. These are focused integration checks rather than the unrelated full controller suite.

The packaged Fabric launcher loads the actual release files from `dist` with original upstream JARs, including their native nested dependencies. It removes project implementation directories from the game classpath, supplies a separate QA mod, and records the tested production SHA-256 hashes in `launch.log`. Build first, then run a focused case:

```powershell
.\gradlew.bat build
.\functional-tests\scripts\Run-PackagedCompatibilityQa.ps1 -Minecraft 1.20.1 -Scenario sophisticated -UseDistribution -RunId review-production
```

The packaged launcher currently supports Fabric 1.20.1 and 1.21.1. Development remapping is still active in this launcher, so production mixin selectors also require namespace inspection; a packaged development launch alone does not prove an installed production launcher works.

## Executed results

### Gameplay

The [consolidated report](../../build/compatibility-coverage/runs/coverage-20261020-verified/results.json) records **66 completed passing profiles**: 54 integration profiles and twelve runs without optional target mods. These historical runs cover the original 76 available upstream cells, before Raised was added; they do not establish Raised gameplay coverage. Each Sophisticated profile exercises Core, Backpacks and Storage together; Fabric REI profiles also exercise Mod Menu. The report points to each original successful launch log and result file, with the exact upstream inputs retained alongside them. Earlier failed attempts remain separate.

| Minecraft | Fabric passing profiles | Forge / NeoForge passing profiles |
| --- | ---: | ---: |
| 1.20.1 | 7 | 7 |
| 1.21.1 | 6 | 7 |
| 1.21.11 | 4 | 5 |
| 26.1.2 | 4 | 6 |
| 26.2 | 4 | 6 |
| 26.3 | 4 | 6 |

- Browser profiles activate the requested provider, render recipes, click the visible transfer button, and verify ingredient consumption, crafting output and cursor conservation on the integrated server. Legacy profiles also exercise the new public tooltip component API.
- All twelve Tom's Storage profiles verify six emitter text regions in actual framebuffers, in addition to window bounds and terminal interactions. The two 1.20.1 profiles exercise all seven native menus and verify filter/emitter clicks against native server state.
- All eight Sophisticated profiles exercise native backpack/storage windows, item mutations, drag distribution, upgrade controls, settings navigation and native opening data.
- All six eligible AE2 targets exercise native chest, drive, cell-workbench and inscriber windows, plus shared-cursor item interaction. These focused runs do not repeat the earlier full AE2 presentation atlas.
- Separate Core-only runs on Fabric and Forge 1.20.1 verify startup, authenticated sessions, inventory opening and resource reload with neither Compatibility nor any integration target mod loaded. Their evidence is in the [legacy report](../../build/compatibility-coverage/runs/coverage-20261020-legacy-final-r1/results.json).

The 66-profile report contains 3,090 passing diagnostic records and 590 framebuffer captures. These totals include completed stages and captures; they are not counts of independent unit tests. Visual review sampled native layouts, attached upgrade/settings controls, legacy Tom atlases, AE2's distinct legacy/modern artwork, corrected frame bounds and visible text. The emitter probes add targeted pixel assertions, rather than claiming every screen was compared pixel for pixel.

### Production artifacts and automated checks

The normal `gradlew.bat build` succeeds without QA properties. The [successful build log](../../build/compatibility-coverage/2026-10-20/root-build-r3.log) records the final checks. An earlier build exposed an incorrect audit assumption about JEI's package-private class name; that lookup was corrected. Separate namespace inspection caught and repaired the legacy Fabric slot selector before the final build.

- 973 Core and 260 Compatibility JUnit tests: **1,233 tests, zero failures, errors or skips**. The root build also passes the 6 coverage and 16 AE2 instruction-merge Python regressions, source feature parity, API example compilation checks and the controller companion's existing validation tasks.
- Source boundary: 959 Compatibility Java files, 485 native patch descriptions and all Core sources. The Compatibility compiler's 786 classpath entries contain no Core implementation classes.
- Production audits pass for all twelve Core and twelve Compatibility JARs, including dependency direction, no bundled Core/upstream implementations, no QA classes, required entrypoints, native AE2 resources and every available adapter. The independent controller companion also passes its existing JAR/native-symbol checks.
- Fourteen targeted production-selector checks pass against the actual release files, covering the new Fabric hooks and Core's 1.20.1 tooltip invoker/refmap. The [selector evidence](../../build/compatibility-coverage/2026-10-20/production-selectors.json) includes the inspected artifact hashes.

The [packaged report](../../build/compatibility-coverage/runs/coverage-20261020-production-r2/results.json) records these additional successful checks, with production SHA-256 hashes verified against `dist`:

| Minecraft / loader | Packaged scenario | Result records |
| --- | --- | ---: |
| 1.20.1 Fabric | Sophisticated Core / Backpacks / Storage | 89 |
| 1.20.1 Fabric | EMI and public tooltip API | 50 |
| 1.20.1 Fabric | Tom's Storage | 94 |
| 1.20.1 Fabric | Core alone | 12 |
| 1.20.1 Fabric | Compatibility without optional targets | 22 |
| 1.21.1 Fabric | Sophisticated Core / Backpacks / Storage | 89 |

These launches remove project implementation directories from the game classpath and confirm that Core and Compatibility classes load from JARs. Upstream mods are original published files with their declared nested libraries. Fabric development remapping remains active, so this is packaged development-launch evidence, supplemented by production namespace inspection. It is not a claim that every target was installed and tested through a production Minecraft launcher.

Release outputs:

- [Core JARs](../../dist/core): twelve `salts_inventory_update-<Minecraft>-<Loader>-0.1.5.jar` files.
- [Compatibility JARs](../../dist/compatibility): twelve `salts_inventory_compat-<Minecraft>-<Loader>-0.1.2.jar` files.
- [Controller companion JARs](../../dist/controlify): twelve independent 0.1.1 files retained by the normal root build.
- [Combined latest archive](../../dist/salts-inventory-all-latest.zip): all 36 JARs grouped by Minecraft version and loader.

Every directory's `SHA256SUMS.txt` matches its twelve production JARs. The combined archive's 36 JAR entries match the distribution files byte for byte and pass ZIP integrity checks. The [original artifact inventory](../../build/compatibility-coverage/2026-10-20/artifact-summary.json) records the pre-update paths, sizes and hashes. None of the produced artifacts are development, sources or intermediate JARs.

The earlier release-number update passed the normal root build in [its rebuild log](../../build/release-version-bump-build.log), with its [artifact verification](../../build/release-version-bump-verification.json) preserving that historical state: Compatibility 0.1.1, two Core 0.1.4.1 hotfixes, ten Core 0.1.4 files and Controlify 0.1.0. No gameplay was rerun for that metadata-only update.

The subsequent Core 0.1.4.2 patch suppresses automatic regular-window onboarding while any alternative workspace host is registered. The current builds consolidate that change into Core 0.1.5; Compatibility 0.1.2 and Controlify 0.1.1 both require Core 0.1.5 on every target. The controller release's fresh gameplay and final-artifact checks are recorded separately in [Controlify validation](../../controlify/VALIDATION.md); they do not replace the older 76-cell upstream availability audit.

### Validation scope

Gameplay uses disposable integrated-server worlds and the exact reviewed upstream releases. It covers the focused workflows above, rather than every feature of every upstream mod or every combination of simultaneously installed mods. Dedicated servers, remote multiplayer, custom resource packs and arbitrary modpacks have not been exercised in this audit. Core-only gameplay was explicitly repeated on the two 1.20.1 targets where its API changed; the other Core targets retain their existing behavior and receive the normal automated/build checks.

## Raised

Raised 6.0.2 is now an optional client integration in all twelve Compatibility builds. Pinned upstream coordinates and SHA-512 values are in `gradle/raised-targets.json` and the reviewed coverage matrix. The adapter consumes only the public HUD layout API; gameplay coverage is recorded separately in the [focused Raised validation report](../../functional-tests/RAISED-HUD-VALIDATION.md).
