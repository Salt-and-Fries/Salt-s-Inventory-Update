# Project Everything: approved 56-family designs, Minecraft 1.20.1

Status: implementation and focused UI review complete, 28 October 2026.

## Delivered scope

Implemented the approved inventory/container designs on the applicable 1.20.1 Fabric and Forge targets. The reviewed catalog contains 68 exact screen profiles; ordinary vanilla-shaped storage reuses SIU's existing windows. The comparison report accounts for all 56 requested mod families with 126 inventory/container views and four deliberately native viewers, plus an Umvuthi trade-state follow-up.

Exposure album/lectern viewers, Waystone/Sharestone destination selection and other non-inventory dialogs retain their original screens. Warp Plate item slots are included. Supplementaries Red Merchant reuses the existing SIU trading implementation, including synchronized offers, payment and result slots.

The new work is limited to Minecraft 1.20.1. It does not sign off later-version ports or every loader/release/configuration combination. No version bump, commit, push or publishing was requested. Core remains 0.1.6 and Compatibility remains 0.1.3. No subagents were used for this implementation.

## Implementation

- Compatibility describes native artwork regions, complete 18-pixel slot cells, exterior tabs/controls, compact bounds and dynamic inventory shapes through the native-template API. Exact menu and screen claims preserve unrelated screens and existing explicit integrations.
- Fixed clipped Letter/Lightroom/Woodcutter details, recipe drawers and nested search focus, machine utility strips, original model previews and variable-size storage layouts. Rocket Sensor retains all six detection modes.
- Preserved original Moonlight Fabric opening data/factories and Forge entity-menu constructors. Moa, Guard and Wolf use their original authoritative entity inventories. Spawn hamster retains its native cargo and preview.
- Small Ships uses original authenticated paging and keeps one desktop session. Anvil/reforge transitions preserve one window and its position. Supplementaries Safe uses a compact exact-menu slot layout.
- MEGA Cells and ME Requester use AE2 desktop styles. Requester retains its original request model and controls, synchronizes the exact owning window, routes authenticated actions to the original handlers, and preserves dynamically added row widget coordinates and the complete footer. Fixed production AE2 member/descriptor mapping, including SIU input-record accessors.
- Mowzie trades retain the original GUI callback and native server handler, including prediction, payment, ownership checks and progression state. Umvuthi's Replenish action stays compact and survives reopening.
- Added the narrowly required public native-screen, rendering-target, inverse input, nested text-input, retained player-slot, merchant and session-payload capabilities to 1.20.1 Core. Native widgets take pointer priority over underlying slots, as in vanilla. Existing API-created windows retain their own paths. See [native template documentation](native-window-templates-1.20.1.md).

## Focused validation

Both Core and Compatibility Fabric/Forge JAR builds passed. Thirty-four distinct focused regression tests passed across the relevant runs (24 Core and 10 Compatibility). Logs:

- `build/reviewed56-final-focused-checks2.log`
- `build/reviewed56-native-widget-priority-checks.log`
- `build/reviewed56-public-input-mapping-fix.log`

Actual in-game checks used native openers. They covered the reviewed UI geometry, tabs, previews and controls, plus recipe search, equipped Accessorify shortcuts, ship paging and a five-diamond cargo transfer, XP deposit/withdrawal/reopening, a completed Red Merchant trade and anvil/reforge round trips. Sculptor consumed the correct payment and started its test. Umvuthi consumed seven gold blocks, changed to Replenish and retained that state after reopening. Requester's checkbox and batch-size edits were verified in server NBT and persisted after normal close/reopen. An empty-filter stock amount resets according to upstream Requester rules; no SIU override was added.

Fabric evidence uses production intermediary launches and original audited upstream releases. Forge screenshots use the official SRG-to-named remapper and record original/derived hashes and resource preservation. They are development-runtime evidence, not a separate production SRG gameplay launch.

These were focused checks, not a complete functional/gameplay suite. Recipe/progression permutations, full Sculptor challenge completion, every equipment combination, multiplayer and every optional state were not exhaustively tested.

## Known upstream fixture limitation

The combined Charm Forked fixture fails to construct its kiln without SIU too because `RecipeBookType.KILN` is absent. Matching isolated native and SIU kiln screenshots are included. Smelting output was not established by this focused check. This is recorded rather than counted as a passed processing test.

## Final artifacts

- Report: `output/pdf/siu-project-everything-56-implemented-comparisons.pdf` (138 pages).
- Evidence index: `output/project-everything-56-implementation/implementation-evidence.json` (130 comparisons, no missing captures, original PNG hashes and crop coordinates).
- PDF QA: `output/project-everything-56-implementation/pdf-layout-check.json`. All pages rendered with Poppler; contact sheets and detailed changed pages visually reviewed.
- Built JAR copies and SHA-256 manifest: `output/project-everything-56-implementation/jars/`.
- Source design references: `output/project-everything-56-design/design-manifest.json` and `native-reference-plan.json`.
- Runtime evidence: `build/remaining-ui-audit/gameplay/*-integrated-reviewed-*`. Final food/letter/viewer captures are AG04/AG05; general Fabric views BB02/BB04 and PR08/PR13/PR18; Forge views BF01-BF05/BF08/BF10. Earlier failed captures remain preserved, and are superseded by the evidence index.

## Follow-up: Meadow and aging-barrel artwork clipping

Corrected the reported 1.20.1 artwork bounds: Cooking Cauldron upper graphic, Fondue pot/heater/output border, and the complete left outline/top cap on both NetherVinery aging barrels. Woodcutter keeps its full native tan output-slot artwork instead of the standard-slot override. The recipe-book button is retained. Only four shared screen profiles changed; Crimson and Warped use the same barrel screen. Core and Controlify were unchanged in this follow-up.

Both Compatibility targets rebuilt at 0.1.3. Seven focused profile regression tests passed, including full decorative-artwork coverage. All five windows were visually reviewed in a production Fabric client; Woodcutter recipe selection and taking its four-plank output worked through the corrected slot. Forge was rebuilt using the shared profiles; its gameplay fixture was not rerun for this small correction. No full functional suite ran.

The five-page before/fixed report is `output/pdf/siu-meadow-vinery-artwork-fixes.pdf`. It contains only the affected UIs. Evidence, focused checks, rebuilt JAR copies and hashes are in `output/project-everything-56-artwork-fixes/`; the earlier full atlas remains an accurate record of the pre-fix captures.

## Follow-up: Fermentation Barrel, native dark frames and XP panel

Vinery 1.4.41 reuses the older aging-barrel screen class with different slot geometry. The later six-slot layout is now detected by its four lower bottle cells and retained as one complete machine region, preserving every native slot offset beneath the plumbing. The lower Inventory label is excluded without cutting the bottle row. The older two-by-two aging-barrel layout retains its corrected artwork profile.

All four Giselle Addon windows use the original texture's exact opaque background RGB **77, 74, 78 (#4D4A4E)** for the SIU face, with original native bevel colors and a readable title. Production screenshots were sampled against the upstream texture pixels, and all four matched exactly. Gravity Normalizer also preserves the complete `13 / 10tick` status line and removes the unused Inventory label and player-grid edge.

The Experience Pouch keeps its full rounded inset-panel bevel and capacity-box bottom border, with padding between that panel and the outer window. Its ordinary controls still deposit and withdraw XP: the focused in-game check deposited 500, withdrew 50 and retained 450 after closing/reopening the same pouch.

Nine focused profile regression tests passed, and both 1.20.1 Compatibility 0.1.3 JARs rebuilt. All six affected windows were reviewed in a production Fabric client using the final JAR. Forge was compiled with the shared implementation; Forge gameplay and a complete functional suite were not repeated. Core and Controlify were unchanged in this follow-up. No versions were incremented and no commit/push was requested.

Report: `output/pdf/siu-barrel-dark-windows-xp-fixes.pdf` (six before/fixed pages, affected windows only). Original screenshots, exact-color checks, focused validation, and rebuilt JAR hashes are under `output/project-everything-56-dark-layout-fixes/`. Final gameplay evidence is `build/remaining-ui-audit/gameplay/1.20.1-fabric-integrated-reviewed-dark-layout03/`.

## Follow-up: suppress redundant Charm tidy buttons

Core 1.20.1 now cancels Charm Forked's shared `InventoryTidyingClient.addSortingButton` factory, covering both its player-inventory and container tidy buttons on every menu. Compatibility is not required. SIU's controls and Charm's other features remain intact; no upstream configuration is rewritten. The optional mixin is client-only and tolerates Charm being absent. Core versions remain 0.1.6.

Both Fabric and Forge Core builds passed. Focused production Fabric checks with Charm Forked 6.0.25 and Core alone confirmed the extra button is absent from kiln, chest and furnace windows. The exported transformed native factory confirms the common cancellation hook was applied. A separate Core-only launch without Charm or Compatibility successfully loaded a world. Forge gameplay and a complete functional suite were not repeated.

Evidence and rebuilt Core JAR hashes: `output/charm-tidy-button-suppression/`. Build log: `build/charm-tidy-button-suppression-build.log`. This follow-up is 1.20.1 only; later-version ports are not claimed.

## Follow-up: Palette controls below the title

Building Wands' Palette keeps its original rotation toggle, mode selector and conditional Gradient-height spinner in a dedicated row beneath the SIU title. The complete 54-slot grid moves down 18 GUI pixels. These upstream controls are drawn outside the native screen's child-widget list, so their original header region is now explicitly included in the shared compatibility composition. Input maps back to the native controls; Core and the API are unchanged.

Two focused regression checks passed: control-row coverage/inverse input mapping, and complete reference-cell coverage. Both 1.20.1 Compatibility JARs rebuilt at 0.1.3. Production Fabric checks confirmed Random/Sequential/Gradient switching, height increase/decrease, rotation-on state, native Palette slot clicks and persistence after reopening. All 54 slots remain visible. Forge gameplay and the complete functional suite were not repeated.

Screenshots, focused observations, validation and rebuilt JAR hashes are in `output/palette-controls-fix/`. Runtime evidence: `build/remaining-ui-audit/gameplay/1.20.1-fabric-integrated-palette-controls01/`.

## Follow-up: Magic Bags, flower guides and three clipped frames

All three Building Wands Magic Bag tiers now use the original texture's exact RGB 132, 132, 132 (#848484) throughout the window face and title bar. Botania's Flower Pouch restores all 32 empty-slot guides by using Botania's original slot-to-flower lookup and alpha renderer on the final API surface; the original alpha helper bypasses native GuiGraphics capture. Every restored guide matched the corresponding native screenshot pixels exactly.

Exposure's film rename display keeps the real slot and its interaction but removes the old external-tab background. Its complete original text-field frame is retained. Gallery's search crop includes the missing top row; a focused input check also exposed that editing the field bypassed the source screen's results refresh, so Gallery now opts into the existing native-screen text-input dispatch. Typing `kebab` reduced the list to its matching painting; selecting and clearing the text restored the list. Umvuthana's output crop now includes its full native 26-by-26 frame and lower white bevel.

Two focused regression checks passed: complete decorative-artwork coverage and reference-cell paint/input mapping. Both Compatibility 1.20.1 JARs rebuilt at 0.1.3. Production Fabric visual checks covered the three bags, Flower Pouch and film rename; film typing and Apply updated the held item's displayed name. Forge development-runtime checks covered Gallery and Umvuthana, retaining remap lineage and original texture bytes. A complete functional suite, multiplayer and later Minecraft versions were not tested for this follow-up. Core and its API were unchanged.

Report: `output/pdf/siu-window-detail-fixes.pdf` (five pages, seven actual fixed windows including the three bag tiers). Evidence, exact-pixel checks and rebuilt JAR hashes: `output/window-detail-fixes/`. Final observations are `1.20.1-fabric-integrated-window-details01` and `1.20.1-forge-integrated-reviewed-details02` under `build/remaining-ui-audit/gameplay/`.

## Follow-up: Bind Spells and Visual Workbench

Spell Engine 0.15.12's binding panel keeps its complete native choice frame from Y=13, including its upper bevel and lower edge. This release's GUI draws an empty book-input guide; it does not render a decorative book model inside the window. The animated book belongs to the block in the world.

Visual Workbench's separate `visualworkbench:crafting` menu now has an explicit native-template integration, preserving the full 3-by-3 grid, arrow, output frame and recipe toggle inside one regular SIU frame. Its original `ModCraftingMenu` remains authoritative, including the persistent block inventory. The earlier blanket exclusion of vanilla screen classes had left this mod-owned menu on automatic fallback bars.

The focused crafting check exposed a second issue: Core's implicit recipe-book hit rectangle could overlap a native template's relocated output. Core now leaves recipe-button routing to native hosted windows, while retaining built-in routing for its own furnace and other definitions. The public API is unchanged. The final production Fabric run confirmed that two planks craft four sticks and are consumed, and the ordinary furnace recipe button still opens its book. The preceding run with the same native-host routing branch also confirmed the integrated recipe toggle and that three cobblestone remain in the grid after closing/reopening.

Two focused geometry/artwork regression tests passed. Fabric and Forge Core 0.1.6 and Compatibility 0.1.3 JARs rebuilt. Forge gameplay, multiplayer and the complete functional suite were not repeated for this follow-up. Earlier diagnostic runs are retained; final screenshots use `1.20.1-fabric-integrated-spell-crafting03`.

Report: `output/pdf/siu-bind-spells-and-crafting-fixes.pdf` (two pages, affected windows only). Evidence, validation and all four rebuilt JAR hashes: `output/spell-crafting-fixes/`.
