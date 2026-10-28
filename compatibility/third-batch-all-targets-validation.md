# Focused validation of the later third-batch ports

Compatibility 0.1.3 / Core 0.1.6. This record concerns the 31 newly ported post-1.20.1 mod/target combinations in the [release matrix](third-batch-all-targets.md). It does not describe a complete SIU functional suite.

## Build, contract and distribution checks

- All ten later Compatibility targets compiled and assembled successfully: Fabric and NeoForge for 1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3. The two existing 1.20.1 Compatibility artifacts were preserved.
- Ten focused 1.21.1 JUnit checks passed: six native layout/slot-coordinate regressions, two Chipped material/catalog regressions and two Runic Forge layout/artwork-mask regressions. No failures or errors were recorded.
- Three additional per-version geometry checks passed on 26.1.2, 26.2 and 26.3. They verify that the newer smelter omits its removed book column while retaining complete input/output cells, progress artwork and inverse pointer mapping. The six paired 26.x artifacts were rebuilt after this adjustment.
- `tools/verify-third-batch-upstream.py build/third-batch-ports/jars` verified the SHA-512 of all 31 downloaded upstream releases and checked the changed menu constructors, native actions, mixin entrypoints and referenced native textures against those binaries.
- `tools/verify-third-batch-ports.py --jars` verified the 45 existing/new release combinations across the twelve SIU targets, their optional registration/metadata, implementation classes and packaged optional mixins.
- The module verifier passed for sources and all twelve Core/twelve Compatibility production JARs. Compatibility still compiles against the public API artifact and contains no copy of Core or its API classes.
- The initial port refreshed ten production Compatibility JARs in `dist/compatibility`; at that stage all Core/Controlify and 1.20.1 Compatibility files were unchanged. The later visual correction below supersedes the two 1.21.1 Core and two 1.21.1 Compatibility files. Distribution checksum lists were regenerated after each update.

Build/check evidence is under `build/third-batch-ports/`: `focused-checks-build.log`, `source-boundary.log`, `jar-boundary.log` and `release-artifacts.json`. The focused JUnit XML is under `compatibility/versions/1.21.1/fabric/build/test-results/test/`.

The later smelter adjustment is recorded in `modern-smelter-checks-build.log` and each 26.x Fabric module's `build/test-results/test/` directory.

## Gameplay evidence

Gameplay uses disposable worlds and mapped development clients. Screenshots are actual game renders. A window-opening observation is distinct from a successful recipe, item-conservation or source-lifetime check.

## Visual correction follow-up

- Chipped 4 on 1.21.1 supplies 72 x 18 button images and 18 x 18 mode icons, with separate highlighted files. The adapter now samples those complete images. Chipped 3 on 1.20.1 retains its correct double-height state sheets.
- A focused trace identified the dark drawer/tab cause: the native subclass's `renderBackground` reaches `Screen.renderTransparentBackground` without passing through the base `Screen.renderBackground` hook. The 1.21.1 Core guard now suppresses all three backdrop entrypoints while `AutomaticNativeScope` owns that screen. Newer Core targets already had those guards; 1.20.1 uses its existing legacy guard.
- Fresh 1.21.1 Fabric drawer pixels match the native texture exactly: RGBA (198,198,198,255). A rounded tab corner and adjacent world both show RGB (173,203,255), rather than a dark rectangle. Normal/highlighted Craft and all four mode buttons were captured separately.
- Both 1.21.1 Core and Compatibility loader targets assembled successfully. The focused linkage verifier checked the background selectors against each loader's actual Minecraft classes; the source/production module boundary remains API-only. The upstream asset check now also verifies the modern PNG dimensions and highlighted files.
- Latest distribution hashes are in `build/third-batch-visual-fixes/artifacts.json`: two Core 0.1.6 and two Compatibility 0.1.3 files updated, with the other 32 JARs unchanged, including every Controlify artifact. These are focused visual checks, not a complete functional suite.

The corrected [13-page all-version gallery](../output/pdf/siu-third-batch-all-versions-visual-fixes.pdf) contains 45 mod/version/loader entries and a four-way Chipped control close-up. Its [validation record](../output/third-batch-all-versions-visual-fixes/validation.json) and [screenshot manifest](../output/third-batch-all-versions-visual-fixes/screenshot-manifest.json) retain target/release labels and provenance. All four supported Chipped combinations passed raw-asset pixel comparisons: 176/176 Craft border pixels in each normal/highlighted state, 324/324 normal mode-icon pixels and 316/316 unobscured hover pixels (eight covered by the native tooltip). The ordinary non-hosted inventory control retained its normal world dimming with no native capture scope. All QA clients were stopped.

### Minecraft 26.3 NeoForge

`build/third-batch-ports/gameplay/26.3-neoforge-port02/` records six applicable window-opening checks and these actual operations:

- Ossuary: inserting eight bone blocks populated the detached recipe grid. Choosing a recipe and taking one skeleton left seven input blocks and one result on the authoritative cursor.
- Ossuary with another Inventory window focused: replacing the empty input with three bone blocks immediately populated the background window's recipe grid.
- Rune Crafting Altar: three cobblestone plus three amethyst shards produced eight arcane stones. Taking the output left two of each input and eight output items on the authoritative cursor.
- Red urn: pickup/return and closing after source removal were checked.

### Minecraft 1.21.1 NeoForge

The clean Graveyard/Runes/Chipped/Simply Swords cohort reached gameplay after separating the unrelated Aether dependency cohort. Its focused evidence includes:

- All seven Chipped stations and Runic Forge opened through actual block interaction, with their dedicated designs and expected bounds.
- Chipped material selection preserved the original source stacks and cursor. Craft converted exactly sixteen selected stone to sixteen `chipped:stone_mini_tiles`, left another six stone untouched and cleared the selection.
- Runic Forge accepted a Slumbering Lichblade, Runefused Gem and Runic Tablet. Client/server previews agreed on awakening level and gem-power components. Taking the weapon committed those components on the authoritative cursor and cleared the consumed forge slots.
- Removing the Runic Forge source closed its session.
- Deep Aether was checked separately after two narrowly mapped synthetic lambda names in its nested Accessories dependency were translated for the named development runtime. No native mixins were disabled and no production JAR was changed. Three each of Golden Amber, Frozen Goldenleaf Berries and Bio Crystal produced three Antidotes, which were taken onto the server cursor. The recipe book and complete Combiner layout remained visible.

### Minecraft 1.21.1 Fabric

The final clean-resource run uses original upstream mods/mixins plus a synthetic, Loom-remapped QA library bundle preserving nested library classes, service entries and Kotlin metadata. Earlier runs with missing metadata are provisional and do not substitute for this run.

- Both directions of Anvil/Reforge, Smithing/Link and Grindstone/Unlink transitions reached the expected native handlers.
- SmitherZ used a one-socket diamond sword and a valid component-based strength gem. The native Link action added the gem to the sword, charged one point of hammer durability and preserved the linked gem when the sword was taken. The virtual socket preview then cleared.
- Drawer opening used the actual crouch/block interaction, and removing its source closed the detached session.
- A clean-resource follow-up opened all seven Chipped stations and Runic Forge with their dedicated definitions, and crafted eight Rune Altar output items from one unit of each input. This supersedes the earlier provisional openings with Kotlin metadata warnings.
- BetterEnd's smelter and ordinary furnace opened in bounds. Clicking the smelter's native recipe-book button changed its visible state from false to true.
- The final definition audit caught AdditionZ's renamed menu ID: the first fletching observation used the generic automatic host. After correcting the registration to `additionz:fletching`, the focused retest resolved `NativeTemplateWindow` with source crop `(29,16,125,55)` and 137 × 81 bounds. Three each of flint, sticks and feathers produced one batch of six arrows; taking it left two of each input and six arrows on the authoritative cursor. The final report uses this corrected capture, not the earlier fallback.

## Coverage across the later targets

All 31 applicable family/target combinations received focused runtime observations, split into isolated cohorts where upstream dependencies required it. The final observer additionally checks the intended custom/native-template definition; an unexpected generic fallback is a failure even when its bounds are valid. Conventional Graveyard storage and ordinary BetterEnd furnaces intentionally retain Core's built-in definitions.

| Minecraft | Loader | Applicable families | Executed operation examples |
| --- | --- | --- | --- |
| 1.21.1 | Fabric | 9 | Gear tab transitions, component-based gem linking, drawer source closure, Ossuary/urn actions, fletching recipe, smelter book toggle |
| 1.21.1 | NeoForge | 5 | Chipped selection/craft, Runic Forge preview/commit/source closure, Deep Aether Combiner recipe |
| 1.21.11 | Fabric | 1 | Ossuary recipe/take and urn pickup/return/source closure |
| 1.21.11 | NeoForge | 1 | Ossuary recipe/take and urn pickup/return/source closure |
| 26.1.2 | Fabric | 3 | Ossuary recipe/take, Rune Altar craft and urn/source closure |
| 26.1.2 | NeoForge | 2 | Ossuary recipe/take, Rune Altar craft and urn/source closure |
| 26.2 | Fabric | 3 | Ossuary/Rune/urn actions; updated End Stone Smelter processing, flame and arrow |
| 26.2 | NeoForge | 2 | Ossuary recipe/take, Rune Altar craft and urn/source closure |
| 26.3 | Fabric | 3 | Ossuary/Rune/urn actions and updated smelter geometry |
| 26.3 | NeoForge | 2 | Ossuary/Rune/urn actions and background-focus recipe synchronization |

The 26.2 smelter processed three raw iron into three iron ingots, consumed one of two coal blocks and returned all three ingots. The initial 26.3 attempt with ordinary coal did not process; native BetterEnd fuel rules reject that fuel. It is not recorded as a processing pass. The newer smelter has no upstream recipe book, while 1.21.1 retains its native book.

Tests cover representative real operations and all reviewed opening/layout variants, rather than every possible recipe, configured socket count or animation timing on every target. Missing optional families were exercised by the isolated target/cohort startups; for example, 1.21.11 starts with the other nine families absent.

## Test environment limits

Some initial combined development fixtures failed before gameplay due to upstream nested-library remapping, dependency requirements or unrelated mixin conflicts. These failed launches are not successful SIU runtime checks and do not establish a production bug in the integrations. Original upstream files and any isolated QA transformations have separate hash lineage. No affected integration UI mixin is to be removed to obtain a passing result.

Further target-specific observations and screenshots are recorded in the accompanying runtime report. Dedicated-server multiplayer, every upstream configuration/resource pack and a complete SIU functional suite are outside these focused checks.

The [initial port screenshot PDF](../output/pdf/siu-third-batch-all-targets-ui.pdf) contains 24 actual runtime crops across eight pages. [Aggregate validation](../output/third-batch-all-targets/validation.json) and the [screenshot manifest](../output/third-batch-all-targets/screenshot-manifest.json) retain per-run evidence and QA mapping limitations. The visual correction gallery linked above supersedes its affected images. Screenshots are representative; the linked records contain the complete target/opening coverage.
