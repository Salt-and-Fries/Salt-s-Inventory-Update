# Project Everything integration checklist

Updated: 2026-10-27. Completion entries refer to the linked implementation evidence; the next-batch order is a planning estimate, not a new gameplay validation report.

## Source and status

The original modpack UI audit is **199 PDF pages**, containing **647 UI-related entries** (131 P1, 249 P2, 267 P3). Those entries include loader ports, libraries, configuration screens and UI hooks as well as content mods; this is not a list of 199 independent container mods.

This checklist preserves every selected audit entry. It crosses off the eight completed first-batch integrations and all ten completed second-batch families on their available SIU targets, plus ten third-batch families on Minecraft 1.20.1. There are **28 delivered families**, represented by **29 original audit entries** because Farmer's Delight and Refabricated are separate audit entries. Third-batch completion is limited to 1.20.1; it does not imply later-version ports. An unchecked entry only means it has not been signed off in this batch tracker. It does not mean the mod is broken, needs an adapter, or lacks an integration already implemented elsewhere in SIU.

Source inventory: `build/ui-modpack-audit-20261025/ui-audit-inventory.csv`. Detailed class and asset evidence: `build/ui-modpack-audit-20261025/ui-audit-evidence.json`. Original report: `output/pdf/salts-inventory-modpack-ui-compatibility-audit.pdf`.

## Completed first batch

- ~~[Iron Chests: Restocked](https://modrinth.com/mod/ironchests)~~
- ~~[Animal Feeding Trough](https://modrinth.com/mod/animal_feeding_trough)~~
- ~~[Oxidized](https://modrinth.com/mod/oxidized)~~
- ~~[Brick & Mortar](https://modrinth.com/mod/brick-and-mortar)~~
- ~~[Expanded Delight](https://modrinth.com/mod/expanded-delight)~~
- ~~[Crabber's Delight](https://modrinth.com/mod/crabbers-delight)~~
- ~~[Nether Chested](https://modrinth.com/mod/nether-chested)~~
- ~~[Inmis](https://modrinth.com/mod/inmis)~~

These eight refer to the completed integration/port work, rather than new testing performed while writing this checklist. See [the all-target port record](native-containers-all-targets.md).

## Completed second batch: all available SIU targets

The ten families below are **complete for this Project Everything batch**, including their applicable ports across the twelve SIU version/loader targets. Availability is recorded per upstream release, not inferred from the Minecraft or loader name alone.

- [x] ~~Bartering Station~~
- [x] ~~Alloy Forgery~~
- [x] ~~Fabric Seasons: Extras~~
- [x] ~~Alloyed~~
- [x] ~~Farmer's Delight / Refabricated~~
- [x] ~~Miner's Delight~~
- [x] ~~Better Archeology~~
- [x] ~~Illager Invasion~~
- [x] ~~Galosphere~~
- [x] ~~Sawmill~~

See the [1.20.1 implementation record](project-everything-ten-1.20.1.md), [later-version coverage and validation](project-everything-ten-all-targets.md), and [38-page screenshot atlas](../../output/pdf/siu-project-everything-ten-all-targets-ui.pdf). The later ports cover 50 available mod/target combinations, in addition to 17 on 1.20.1. Core is 0.1.6; Compatibility is 0.1.3.

Completion records the delivered integration scope and focused checks; it does not erase the documented test limits. Fabric 1.21.x gameplay used mapped release classes. Dedicated-server and every possible recipe/modpack combination were not certified. Legacy Alloyed 1.20.1 owns no screen, so its inventory workflow was checked; later releases use their standalone forge. This does not claim full Create support.

### Completed second-batch order (historical)

This is the original order used for the now-completed second batch. The implementation records above supersede these initial estimates. For example, legacy Alloyed has no owned forge UI, while its newer standalone releases do. The third-batch recommendations follow below.

Start each candidate with the automatic host. Add a Compatibility-side native profile/template only where the baseline needs changes. Preserve native slot logic, animations and server behavior; use the public API for regions, compact bounds and input. Existing explicit API windows keep priority. No new Core changes are proposed by this triage; actual validation could reveal a general API gap.

| Order | Mod / family | First UI scope | Why this order |
| --- | --- | --- | --- |
| 1 | [Bartering Station](https://modrinth.com/mod/bartering-station) | Bartering Station container | One conventional block inventory: gold input and collected bartering results. Closest to the storage containers already adapted. |
| 2 | [Alloy Forgery](https://modrinth.com/mod/alloy-forgery) | Alloy Forge menu family | One furnace-like menu family with existing native textures, processing indicators, inputs, fuel and output. Strong reuse of the kiln/furnace work. |
| 3 | [Fabric Seasons: Extras](https://modrinth.com/mod/fabric-seasons-extras) | Crop Heater / Crop Chiller | The audited heater/chiller screens share AirConditioningScreenHandler. Small bounded machine interfaces make a narrow first profile. |
| 4 | [Alloyed](https://modrinth.com/mod/create-alloyed) | Alloy Forge | One audited ForgeMenu / ForgeScreen family. A contained processing interface is a good native-template candidate. |
| 5 | [Farmer's Delight](https://modrinth.com/mod/farmers-delight) / [Farmer's Delight Refabricated](https://modrinth.com/mod/farmers-delight-refabricated) | Cooking Pot; loader ports treated as one family | A conventional cooking interface with a bounded ingredient grid, serving-container slot and result. Useful groundwork for other Delight add-ons. |
| 6 | [Miner's Delight](https://modrinth.com/mod/miners-delight) | Copper Pot | One audited CopperPotMenu, with a layout and serving workflow closely related to Farmer's Delight. Implement after that base is validated. |
| 7 | [Better Archeology](https://modrinth.com/mod/better-archeology) | Fossil inventory and Identifying interface | Two relatively small bounded container families; more coverage than a single-machine mod, but still a reasonable fit for native regions. |
| 8 | [Illager Invasion](https://modrinth.com/mod/illager-invasion) | Imbuing Table | One audited ImbuingMenu / ImbuingScreen family. A contained workbench, with more interaction semantics to verify than a plain inventory. |
| 9 | [Galosphere](https://modrinth.com/mod/galosphere) | Audited Combustion Table | The audited 1.20.1 build has one detected container family. Its small workbench is a plausible native-profile candidate. |
| 10 | [Sawmill](https://modrinth.com/mod/universal-sawmill) | Sawmill / Woodcutter | One workstation family, but recipe selection, scrolling, search and wide variants add input coverage. Put it last in this first batch. |

Farmer's Delight and Farmer's Delight Refabricated count as one integration family here, but remain two entries in the original audit below.

### Original second-batch acceptance checklist

1. **Bartering Station:** Native slot restrictions, returned items, full output inventory, shift-click, and nearby piglin availability.
2. **Alloy Forgery:** Each available forge tier, heat/fuel indicators, valid and invalid recipes, progress, output extraction and compact padding.
3. **Fabric Seasons: Extras:** Both heater and chiller textures, activity levels, inventory interaction, and the separate Patchouli guidebook remaining usable.
4. **Alloyed:** Recipe and progress rendering, slot roles and synchronization; older Create-dependent releases must be assessed separately from newer standalone releases.
5. **Farmer's Delight:** Cooking progress, heat, serving containers, output counts and any version-specific recipe-book control. Canvas sign editors should retain their native behavior.
6. **Miner's Delight:** Copper-cup serving rules, ingredient/result transfers, cooking progress and native container restrictions.
7. **Better Archeology:** Both menu families, identification states, any overlays/progress, slot eligibility and item transfers.
8. **Illager Invasion:** Native choices and feedback, enchantment/cost restrictions, result eligibility and server synchronization.
9. **Galosphere:** Combustion-table controls and recipe/result behavior. Re-inventory newer releases before promising whole-mod coverage: the audit includes one 1.20.1 Forge build only.
10. **Sawmill:** Recipe selection, search focus, scrolling, narrow/wide layouts, output amounts and carpenter-villager interactions staying native.

Each adapted container also needs focused checks for dragging, clipping, compact padding, tooltips, item pickup/place, shift-click, multiwindow transfers, closing/reopening and the mod being absent. The linked reports record the checks actually executed and their limits; this planning document does not execute them.

### Audited implementation entry points

These are identifiers observed in the pinned audit binaries, not promises about all newer releases.

- **Bartering Station:** `fuzs.barteringstation.world.inventory.BarteringStationMenu`
- **Alloy Forgery:** `wraith.alloyforgery.AlloyForgeScreenHandler`
- **Fabric Seasons: Extras:** `io.github.lucaargolo.seasonsextras.screenhandlers.AirConditioningScreenHandler`
- **Alloyed:** `com.molybdenum.alloyed.common.screen.ForgeMenu`
- **Farmer's Delight:** `vectorwing.farmersdelight.common.block.entity.container.CookingPotMenu`
- **Farmer's Delight Refabricated:** `vectorwing.farmersdelight.common.block.entity.container.CookingPotMenu`
- **Miner's Delight:** `com.sammy.minersdelight.content.block.copper_pot.CopperPotMenu`
- **Better Archeology:** `net.Pandarix.betterarcheology.screen.FossilInventoryScreenHandler`, `net.Pandarix.betterarcheology.screen.IdentifyingScreenHandler`
- **Illager Invasion:** `fuzs.illagerinvasion.world.inventory.ImbuingMenu`
- **Galosphere:** `net.orcinus.galosphere.client.gui.CombustionTableMenu`
- **Sawmill:** `net.mehvahdjukaar.sawmill.SawmillMenu`

### Leave for a later batch

- Broad machine/UI suites such as Create, Tech Reborn and Applied Energistics add-ons require a much larger interface inventory.
- Vanilla-menu rewrites and inventory-wide input changes require a different audit from rectangular native-container hosting.
- Multi-merchant trading screens, entity-owned inventories and virtual/ghost-slot filter screens have additional synchronization or lifecycle concerns.
- VillagersPlus now documents additional workstation/trading scope beyond the one Alchemist menu in the pinned audit; re-audit it before treating the whole mod as a one-screen task.

## Completed third batch: ten delivered on 1.20.1

This is an **estimated easiest-first order**, based on the retained concrete menu/screen inventory, shared UI families, existing native-template capabilities and upstream project information reviewed on October 27, 2026. All ten candidates now have compact Compatibility-side integrations for this batch on their supported 1.20.1 loaders. The table retains the original planning order and scope; delivery and focused-check limits are recorded below.

The pinned audit includes a Minecraft 1.20.1 release for every candidate, so 1.20.1 is a practical common starting point, using each mod's actual loader. That is not a promise that every candidate exists on both Fabric and Forge. Each release needs its own menu inventory before implementation or porting.

| Order | Mod | Initial UI scope | Why it makes this shortlist / remaining complexity |
| --- | --- | --- | --- |
| 1 | [ExtendedDrawersAddon](https://modrinth.com/mod/extendeddrawersaddon) | Drawer upgrade screen | One small dedicated upgrade inventory; closest to the compact storage work already completed. Preserve upgrade restrictions and the base mod's world interaction. |
| 2 | [The Graveyard](https://modrinth.com/mod/the-graveyard-fabric) | Ossuary; coffin, urn and sarcophagus storage; gravestone editor | One audited custom container family plus ordinary storage. Keep the separate gravestone editor native. |
| 3 | [BetterEnd](https://modrinth.com/mod/betterend) | End Stone Smelter and furnace variants | One audited smelter/recipe-book family. Reuse the furnace, progress and recipe-component work; inspect ordinary crafting/anvil variants too. |
| 4 | [Runes](https://modrinth.com/mod/runes) | Rune Crafting Altar | One dedicated altar menu and texture, with smithing-like recipes. Small input/result surface and clear item-conservation checks. |
| 5 | [Deep Aether](https://modrinth.com/mod/deep-aether) | Combiner | One audited Combiner menu/screen pair. Compact workstation, though its Aether dependency adds fixture setup and must remain a separate scope. |
| 6 | [Chipped](https://modrinth.com/mod/chipped) | All seven workbench variants | Seven tables share one audited WorkbenchMenu/WorkbenchScreen family. Reuse native recipe-grid and scrolling behavior; verify every table's choices. |
| 7 | [Simply Swords](https://modrinth.com/mod/simply-swords) | Runic Forge | The reviewed 1.70.2 workstation has 11 visible sockets and a hidden preview. Preserve its torn artwork, take/close-to-commit workflow, preview tooltip and channel animation. Broader weapon/HUD hooks are outside this batch. |
| 8 | [TieredZ](https://modrinth.com/mod/tieredz) | Reforge Gear and anvil entry tab | One dedicated reforging menu. Lower in this batch because the tab must also be reachable from SIU's anvil window. Tierify is a separate candidate, not silently included. |
| 9 | [SmitherZ](https://modrinth.com/mod/smitherz) | Smither and Grinder; smithing/grindstone tabs | Two compact workstation families. More input work because native tab transitions, gem linking/unlinking and output rules must remain intact. |
| 10 | [AdditionZ](https://modrinth.com/mod/additionz) | Fletching Table and Chunk Loader | Two audited menu families, but inventory and Trinkets-related hooks broaden the review. Put it last among this shortlist. |

### Delivered third-batch scope

- [x] ~~ExtendedDrawersAddon~~ — 1.20.1 and 1.21.1 Fabric.
- [x] ~~The Graveyard containers~~ — all twelve supported targets; the gravestone text editor stays native.
- [x] ~~BetterEnd~~ — Fabric on 1.20.1, 1.21.1, 26.1.2, 26.2 and 26.3, including the End Stone Smelter and recipe book.
- [x] ~~Runes~~ — all supported targets except 1.21.11, which has no matching release.
- [x] ~~Deep Aether Combiner~~ — 1.20.1 Forge and 1.21.1 NeoForge; this does not claim all Aether UI.
- [x] ~~TieredZ~~ — 1.20.1 and 1.21.1 Fabric, including the anvil entry tab and naming.
- [x] ~~SmitherZ~~ — 1.20.1 and 1.21.1 Fabric, including linking/unlinking and workstation tabs.
- [x] ~~AdditionZ workstations~~ — 1.20.1 and 1.21.1 Fabric, fletching and chunk-loader interfaces; optional Trinkets content is outside this batch.
- [x] ~~Chipped~~ — all seven workbenches on 1.20.1 Fabric/Forge and 1.21.1 Fabric/NeoForge, including the material picker, native previews, variant search/scrolling and in-place crafting.
- [x] ~~Simply Swords: Runic Forge~~ — 1.20.1 Fabric/Forge and 1.21.1 Fabric/NeoForge; charcoal two-bar design, native weapon/gem/tablet editing, preview tooltip and channel feedback.

See the [implementation record](../../compatibility/next-eight-1.20.1.md), [focused validation and limitations](../../compatibility/next-eight-1.20.1-validation.md), and [implemented screenshot PDF](../../output/pdf/siu-next-eight-implemented-windows-1.20.1.pdf). Later ports are recorded in the [exact release matrix](../../compatibility/third-batch-all-targets.md). These are focused checks, not a complete functional suite or dedicated-server certification.

Chipped has its own [implementation and focused coverage](../../compatibility/chipped-1.20.1.md) and [proposal-versus-runtime screenshot report](../../output/pdf/siu-chipped-implemented-windows-1.20.1.pdf). Its workflow uses a dedicated public-API window rather than a native upper-screen crop.

Simply Swords has a [Runic Forge implementation record](../../compatibility/simply-swords-1.20.1.md) and [actual screenshot report](../../output/pdf/siu-simply-swords-implemented-windows-1.20.1.pdf). Its later port covers the same workstation scope; broader weapon/HUD integration is outside this batch.

Concrete audited entry points:

- **ExtendedDrawersAddon:** `DrawerScreenHandler` / `DrawerScreen`.
- **The Graveyard:** `OssuaryScreenHandler` / `OssuaryScreen`; standalone `GravestoneScreen`.
- **BetterEnd:** `EndStoneSmelterMenu`, `EndStoneSmelterScreen` and its recipe component.
- **Runes:** `RuneCraftingScreenHandler` / `RuneCraftingScreen`.
- **Deep Aether:** `CombinerMenu` / `CombinerScreen`.
- **Chipped:** `WorkbenchMenu` / `WorkbenchScreen`.
- **Simply Swords:** `RunicForgeScreenHandler` / `RunicForgeScreen`.
- **TieredZ:** `ReforgeScreenHandler` / `ReforgeScreen`, plus the anvil tab hook.
- **SmitherZ:** `SmitherScreenHandler` and `GrinderScreenHandler`, plus native smithing/grindstone tab hooks.
- **AdditionZ:** `FletchingScreenHandler` and `ChunkLoaderScreenHandler`, plus its inventory/Trinkets hooks.

The source rows are audit 222, 579, 83, 499, 168, 110, 524, 583, 532 and 19 respectively. The full CSV and JSON class inventories remain the evidence behind these estimates. This is not a new gameplay test or a claim that no Core API gap can appear.

Visual Workbench, Easy Anvils and Easy Magic are useful follow-ups, but modify existing vanilla menu behavior. They need a persistence/native-menu-ownership audit before being treated as easier than a small independently registered workstation. Entity-owned storage, virtual trading/filter screens and broad technical mods remain later work. Avoid claiming an entire parent mod such as The Aether or Create is covered by adapting one add-on.

## Original selected audit entries

Checkboxes below record completion of the first eight, second ten and the scoped third ten Project Everything families. Audit entry numbers and priorities are retained. **Next #N** refers to the third-batch recommendation table above. Future candidates remain unchecked.

### P1 (131 entries)

- [ ] Audit 6: [Let's Do] API — Container/menu.
- [ ] Audit 9: [[Let's Do] Candlelight - Farm&Charm compat](https://modrinth.com/mod/lets-do-candlelight-farmcharm-compat) — Standalone screen.
- [ ] Audit 10: [[Let's Do] Farm & Charm](https://modrinth.com/mod/lets-do-farm-charm) — Standalone screen.
- [ ] Audit 11: [[Let's Do] HerbalBrews](https://modrinth.com/mod/lets-do-herbalbrews) — Standalone screen.
- [ ] Audit 12: [[Let's Do] Meadow](https://modrinth.com/mod/lets-do-meadow) — Standalone screen.
- [ ] Audit 13: [[Let's Do] NetherVinery](https://modrinth.com/mod/lets-do-nethervinery) — Standalone screen.
- [ ] Audit 14: [[Let's Do] Vinery](https://modrinth.com/mod/lets-do-vinery) — Standalone screen.
- [ ] Audit 15: [Accessories](https://modrinth.com/mod/accessories) — Inventory/render/input.
- [ ] Audit 17: [Ad Astra](https://modrinth.com/mod/ad-astra) — Container/menu.
- [x] Audit 19: ~~[AdditionZ](https://modrinth.com/mod/additionz)~~ — Container/menu. — **Delivered third batch on all matching target releases; see scope and limits above.**
- [ ] Audit 27: [Aether Addon: Protect Your Moa](https://modrinth.com/mod/aether-protect-your-moa) — Container/menu.
- [ ] Audit 31: [Alex's Mobs](https://modrinth.com/mod/alexs-mobs) — Standalone screen.
- [x] Audit 33: ~~[Alloy Forgery](https://modrinth.com/mod/alloy-forgery)~~ — Container/menu. — **Completed second batch on available SIU targets; see scope and limits above.**
- [x] Audit 34: ~~[Alloyed](https://modrinth.com/mod/create-alloyed)~~ — Container/menu. — **Completed second batch on available SIU targets; see scope and limits above.**
- [ ] Audit 41: [Amendments](https://modrinth.com/mod/amendments) — Standalone screen.
- [x] Audit 42: ~~[Animal Feeding Trough](https://modrinth.com/mod/animal_feeding_trough)~~ — Container/menu. — **Completed first batch.**
- [ ] Audit 48: [Applied Energistics 2 Wireless Terminals](https://modrinth.com/mod/applied-energistics-2-wireless-terminals) — Standalone screen.
- [ ] Audit 52: [Archon Renewed](https://modrinth.com/mod/archon-renewed) — Container/menu.
- [ ] Audit 64: [BackSlot](https://modrinth.com/mod/backslot) — Inventory/render/input.
- [ ] Audit 65: [BackSlotAddon](https://modrinth.com/mod/backslotaddon) — Framework/settings.
- [x] Audit 70: ~~[Bartering Station](https://modrinth.com/mod/bartering-station)~~ — Container/menu. — **Completed second batch on available SIU targets; see scope and limits above.**
- [x] Audit 74: ~~[Better Archeology](https://modrinth.com/mod/better-archeology)~~ — Container/menu. — **Completed second batch on available SIU targets; see scope and limits above.**
- [x] Audit 83: ~~[BetterEnd](https://modrinth.com/mod/betterend)~~ — Container/menu. — **Delivered third batch on all matching target releases; see scope and limits above.**
- [ ] Audit 88: [Blue Skies](https://modrinth.com/mod/blue-skies) — Container/menu.
- [ ] Audit 97: [Botania](https://modrinth.com/mod/botania) — Container/menu.
- [ ] Audit 98: [Bountiful](https://modrinth.com/mod/bountiful) — Container/menu.
- [x] Audit 99: ~~[Brick & Mortar](https://modrinth.com/mod/brick-and-mortar)~~ — Container/menu. — **Completed first batch.**
- [ ] Audit 101: [Building Wands](https://modrinth.com/mod/building-wands) — Container/menu.
- [ ] Audit 105: [Charm Forked](https://modrinth.com/mod/charm-forked) — Container/menu.
- [x] ~~Audit 110: [Chipped](https://modrinth.com/mod/chipped)~~ — Seven workbenches delivered on 1.20.1 Fabric/Forge and 1.21.1 Fabric/NeoForge; see the cross-target matrix.
- [ ] Audit 119: [Client Sort](https://modrinth.com/mod/clientsort) — Inventory/render/input.
- [ ] Audit 129: [Convenient Decor](https://modrinth.com/mod/convenient-decor) — Framework/settings.
- [x] Audit 130: ~~[Crabber's Delight](https://modrinth.com/mod/crabbers-delight)~~ — Container/menu. — **Completed first batch.**
- [ ] Audit 133: [Create](https://modrinth.com/mod/create) — Container/menu.
- [ ] Audit 136: [Create Big Cannons](https://modrinth.com/mod/create-big-cannons) — Container/menu.
- [ ] Audit 138: [Create Fabric](https://modrinth.com/mod/create-fabric) — Container/menu.
- [ ] Audit 141: Create Simulated — Inventory/render/input.
- [ ] Audit 151: [Create: Shuffle Filter](https://modrinth.com/mod/create-shuffle-filter) — Container/menu.
- [ ] Audit 152: [Create: Steam 'n' Rails](https://modrinth.com/mod/create-steam-n-rails) — Container/menu.
- [x] Audit 168: ~~[Deep Aether](https://modrinth.com/mod/deep-aether)~~ — Container/menu. — **Delivered third batch on all matching target releases; see scope and limits above.**
- [ ] Audit 182: [DungeonZ](https://modrinth.com/mod/dungeonz) — Container/menu.
- [ ] Audit 186: [EarlyStage](https://modrinth.com/mod/earlystage) — Framework/settings.
- [ ] Audit 187: [Easy Anvils](https://modrinth.com/mod/easy-anvils) — Container/menu.
- [ ] Audit 188: Easy Disenchanting — Framework/settings.
- [ ] Audit 189: [Easy Magic](https://modrinth.com/mod/easy-magic) — Container/menu.
- [ ] Audit 195: [ElevatorMod](https://modrinth.com/mod/elevatormod) — Container/menu.
- [ ] Audit 205: [Enchanted: Witchcraft](https://modrinth.com/mod/enchanted-witchcraft) — Container/menu.
- [x] Audit 217: ~~[Expanded Delight](https://modrinth.com/mod/expanded-delight)~~ — Container/menu. — **Completed first batch.**
- [ ] Audit 219: [Exposure](https://modrinth.com/mod/exposure) — Container/menu.
- [ ] Audit 220: [Extended AE](https://modrinth.com/mod/extended-ae) — Inventory/render/input.
- [x] Audit 222: ~~[ExtendedDrawersAddon](https://modrinth.com/mod/extendeddrawersaddon)~~ — Container/menu. — **Delivered third batch on all matching target releases; see scope and limits above.**
- [ ] Audit 224: [Extreme sound muffler](https://modrinth.com/mod/extreme_sound_muffler) — Inventory/render/input.
- [x] Audit 238: ~~[Fabric Seasons: Extras](https://modrinth.com/mod/fabric-seasons-extras)~~ — Container/menu. — **Completed second batch on available SIU targets; see scope and limits above.**
- [ ] Audit 243: [FancyMenu](https://modrinth.com/mod/fancymenu) — Inventory/render/input.
- [x] Audit 244: ~~[Farmer's Delight](https://modrinth.com/mod/farmers-delight)~~ — Container/menu. — **Completed second batch on available SIU targets; see scope and limits above.**
- [x] Audit 245: ~~[Farmer's Delight Refabricated](https://modrinth.com/mod/farmers-delight-refabricated)~~ — Container/menu. — **Completed second batch on available SIU targets; see scope and limits above.**
- [ ] Audit 275: [Gallery](https://modrinth.com/mod/gallery) — Container/menu.
- [x] Audit 276: ~~[Galosphere](https://modrinth.com/mod/galosphere)~~ — Container/menu. — **Completed second batch on available SIU targets; see scope and limits above.**
- [ ] Audit 282: [Guard Villagers](https://modrinth.com/mod/guard-villagers) — Container/menu.
- [ ] Audit 283: [Guard Villagers (Fabric/Quilt)](https://modrinth.com/mod/guard-villagers-fabricquilt) — Container/menu.
- [ ] Audit 294: [IJM's Tweaks](https://modrinth.com/mod/ijm-tweaks) — Inventory/render/input.
- [x] Audit 295: ~~[Illager Invasion](https://modrinth.com/mod/illager-invasion)~~ — Container/menu. — **Completed second batch on available SIU targets; see scope and limits above.**
- [ ] Audit 297: [Immersive Aircraft](https://modrinth.com/mod/immersive-aircraft) — Container/menu.
- [x] Audit 307: ~~[Inmis](https://modrinth.com/mod/inmis)~~ — Container/menu. — **Completed first batch.**
- [ ] Audit 312: [Inventory Sorting](https://modrinth.com/mod/inventory-sorting) — Inventory/render/input.
- [x] Audit 317: ~~[Iron Chests: Restocked](https://modrinth.com/mod/ironchests)~~ — Container/menu. — **Completed first batch.**
- [ ] Audit 320: Item Filters — Container/menu.
- [ ] Audit 323: [ItemLocks](https://modrinth.com/mod/itemlocks) — Inventory/render/input.
- [ ] Audit 331: [JobsAddon](https://modrinth.com/mod/jobsaddon) — Standalone screen.
- [ ] Audit 339: [Kibe](https://modrinth.com/mod/kibe) — Container/menu.
- [ ] Audit 346: [L_Ender's Cataclysm](https://modrinth.com/mod/l_enders-cataclysm) — Container/menu.
- [ ] Audit 356: [LevelZ](https://modrinth.com/mod/levelz) — Inventory/render/input.
- [ ] Audit 357: LibGui — Standalone screen.
- [ ] Audit 364: [Lootr](https://modrinth.com/mod/lootr) — Framework/settings.
- [ ] Audit 371: [ME Requester](https://modrinth.com/mod/merequester) — Container/menu.
- [ ] Audit 381: [Mine Cells - Dead Cells Mod](https://modrinth.com/mod/minecells) — Container/menu.
- [x] Audit 382: ~~[Miner's Delight](https://modrinth.com/mod/miners-delight)~~ — Container/menu. — **Completed second batch on available SIU targets; see scope and limits above.**
- [ ] Audit 389: [Modern Industrialization](https://modrinth.com/mod/modern-industrialization) — Container/menu.
- [ ] Audit 398: [Mowzie's Mobs](https://modrinth.com/mod/mowzies-mobs) — Standalone screen.
- [ ] Audit 405: [Mythic Mounts](https://modrinth.com/mod/mythic-mounts) — Container/menu.
- [x] Audit 413: ~~[Nether Chested](https://modrinth.com/mod/nether-chested)~~ — Standalone screen. — **Completed first batch.**
- [ ] Audit 415: [NiftyCarts](https://modrinth.com/mod/niftycarts) — Container/menu.
- [ ] Audit 422: [NutritionZ](https://modrinth.com/mod/nutritionz) — Inventory/render/input.
- [x] Audit 430: ~~[Oxidized](https://modrinth.com/mod/oxidized)~~ — Container/menu. — **Completed first batch.**
- [ ] Audit 433: [PartyAddon](https://modrinth.com/mod/partyaddon) — Standalone screen.
- [ ] Audit 436: [Pehkui](https://modrinth.com/mod/pehkui) — Inventory/render/input.
- [ ] Audit 441: Polymer (Core) — Inventory/render/input.
- [ ] Audit 442: [Polymorph](https://modrinth.com/mod/polymorph) — Framework/settings.
- [ ] Audit 443: [Polymorphic Energistics](https://modrinth.com/mod/polymorphic-energistics) — Inventory/render/input.
- [ ] Audit 453: [Potion Stacks](https://modrinth.com/mod/potion-stacks) — Framework/settings.
- [ ] Audit 454: [Powah!](https://modrinth.com/mod/powah) — Container/menu.
- [ ] Audit 459: Prominent Base (embedded) — Container/menu.
- [ ] Audit 460: Prominent Global (pack integration) — Inventory/render/input.
- [ ] Audit 469: [Quark](https://modrinth.com/mod/quark) — Container/menu.
- [ ] Audit 481: [Reborn Core](https://modrinth.com/mod/reborncore) — Container/menu.
- [ ] Audit 495: [RevampedWolf](https://modrinth.com/mod/revampedwolf) — Container/menu.
- [ ] Audit 496: [Revive](https://modrinth.com/mod/revive) — Container/menu.
- [x] Audit 499: ~~[Runes](https://modrinth.com/mod/runes)~~ — Container/menu. — **Delivered third batch on all matching target releases; see scope and limits above.**
- [x] Audit 505: ~~[Sawmill](https://modrinth.com/mod/universal-sawmill)~~ — Container/menu. — **Completed second batch on available SIU targets; see scope and limits above.**
- [ ] Audit 513: sgui — Container/menu.
- [ ] Audit 520: [Simple Storage Network](https://modrinth.com/mod/simple-storage-network) — Standalone screen.
- [x] ~~Audit 524: [Simply Swords](https://modrinth.com/mod/simply-swords)~~ — Runic Forge delivered on 1.20.1 Fabric/Forge and 1.21.1 Fabric/NeoForge; see the cross-target matrix.
- [ ] Audit 529: [Small Ships](https://modrinth.com/mod/small-ships) — Container/menu.
- [x] Audit 532: ~~[SmitherZ](https://modrinth.com/mod/smitherz)~~ — Container/menu. — **Delivered third batch on all matching target releases; see scope and limits above.**
- [ ] Audit 546: [Sortilege](https://modrinth.com/mod/sortilege) — Container/menu.
- [ ] Audit 547: Soulbinding — Inventory/render/input.
- [ ] Audit 552: [Spawn](https://modrinth.com/mod/spawn-mod) — Container/menu.
- [ ] Audit 554: [Spell Engine](https://modrinth.com/mod/spell-engine) — Container/menu.
- [ ] Audit 558: [Stalwart Dungeons](https://modrinth.com/mod/stalwart-dungeons) — Container/menu.
- [ ] Audit 561: [Steam 'n' Rails Neoforge](https://modrinth.com/mod/create-steam-n-rails-1.21.1) — Container/menu.
- [ ] Audit 568: [Supplementaries](https://modrinth.com/mod/supplementaries) — Container/menu.
- [ ] Audit 574: Tech Reborn — Container/menu.
- [ ] Audit 576: [The Aether](https://modrinth.com/mod/aether) — Container/menu.
- [ ] Audit 578: [The Bumblezone - Fabric](https://modrinth.com/mod/the-bumblezone-fabric) — Container/menu.
- [x] Audit 579: ~~[The Graveyard (Fabric/Neoforge)](https://modrinth.com/mod/the-graveyard-fabric)~~ — Container/menu. — **Delivered third batch on all matching target releases; see scope and limits above.**
- [ ] Audit 582: [Things](https://modrinth.com/mod/things) — Container/menu.
- [x] Audit 583: ~~[TieredZ](https://modrinth.com/mod/tieredz)~~ — Container/menu. — **Delivered third batch on all matching target releases; see scope and limits above.**
- [ ] Audit 584: [Tierify](https://modrinth.com/mod/tierify) — Container/menu.
- [ ] Audit 585: [Tom's Trading Network](https://modrinth.com/mod/toms-trading-network) — Container/menu.
- [ ] Audit 587: [Trading Post](https://modrinth.com/mod/trading-post) — Container/menu.
- [ ] Audit 589: [Transmog](https://modrinth.com/mod/transmog) — Container/menu.
- [ ] Audit 590: [TrashSlot](https://modrinth.com/mod/trashslot) — Framework/settings.
- [ ] Audit 591: [Traveler's Backpack](https://modrinth.com/mod/travelersbackpack) — Container/menu.
- [ ] Audit 596: [Universal Graves](https://modrinth.com/mod/universal-graves) — Standalone screen.
- [ ] Audit 600: [Vanilla Backport](https://modrinth.com/mod/vanillabackport) — Inventory/render/input.
- [ ] Audit 605: [VillagersPlus](https://modrinth.com/mod/villagersplus) — Container/menu.
- [ ] Audit 607: [Visual Workbench](https://modrinth.com/mod/visual-workbench) — Container/menu.
- [ ] Audit 613: [Waystones](https://modrinth.com/mod/waystones) — Container/menu.
- [ ] Audit 619: [Wraith Waystones](https://modrinth.com/mod/fwaystones) — Container/menu.
- [ ] Audit 632: [You're in Grave Danger](https://modrinth.com/mod/yigd) — Standalone screen.
- [ ] Audit 645: [Zenith Renewed](https://modrinth.com/mod/zenith-renewed) — Container/menu.

### P2 (249 entries)

- [ ] Audit 2: [[Deprecated] Trinkets Compat Layer for Accessories](https://modrinth.com/mod/accessories-tc-layer) — Inventory/render/input.
- [ ] Audit 4: [[ETF] Entity Texture Features](https://modrinth.com/mod/entitytexturefeatures) — Tooltip.
- [ ] Audit 5: [[Let's Do Addon] EMI Compat](https://modrinth.com/mod/lets-do-emi-compat) — Framework/settings.
- [ ] Audit 7: [[Let's Do] Bakery - Farm&Charm Compat](https://modrinth.com/mod/lets-do-bakery-farmcharm-compat) — Shared appliance/workflow integration.
- [ ] Audit 8: [[Let's Do] Brewery - Farm&Charm Compat](https://modrinth.com/mod/lets-do-brewery-farmcharm-compat) — Shared appliance/workflow integration.
- [ ] Audit 16: [Accessorify](https://modrinth.com/mod/accessorify) — HUD/overlay.
- [ ] Audit 18: [Ad-Astra: Giselle Addon](https://modrinth.com/mod/ad-astra-giselle-addon) — Container/menu.
- [ ] Audit 20: [Advanced Loot Info (ALI)](https://modrinth.com/mod/advanced-loot-info) — Recipe/browser addon.
- [ ] Audit 21: [Advanced Netherite](https://modrinth.com/mod/advanced-netherite) — Tooltip.
- [ ] Audit 26: [AE2 EMI Crafting Integration](https://modrinth.com/mod/ae2-emi-crafting) — Framework/settings.
- [ ] Audit 35: [Amarite](https://modrinth.com/mod/amarite) — HUD/overlay.
- [ ] Audit 44: [Another Furniture](https://modrinth.com/mod/another-furniture) — Framework/settings.
- [ ] Audit 45: [Antique Atlas 4](https://modrinth.com/mod/antique-atlas-4) — Standalone screen.
- [ ] Audit 46: [AppleSkin](https://modrinth.com/mod/appleskin) — HUD/overlay.
- [ ] Audit 47: [Applied Botanics](https://modrinth.com/mod/applied-botanics) — Storage semantic extension.
- [ ] Audit 49: [Aquamirae](https://modrinth.com/mod/aquamirae) — Standalone screen.
- [ ] Audit 50: [Archers (RPG Series)](https://modrinth.com/mod/archers) — Tooltip.
- [ ] Audit 53: [Armor Statues](https://modrinth.com/mod/armor-statues) — Standalone screen.
- [ ] Audit 54: [Artifacts](https://modrinth.com/mod/artifacts) — HUD/overlay.
- [ ] Audit 59: Attribute Tooltip Ordering — Tooltip.
- [ ] Audit 60: [Audio Improvements](https://modrinth.com/mod/audio-improvements) — Standalone screen.
- [ ] Audit 63: [AzureLib](https://modrinth.com/mod/azurelib) — Standalone screen.
- [ ] Audit 67: [BadOptimizations](https://modrinth.com/mod/badoptimizations) — HUD/overlay.
- [ ] Audit 68: [BaguetteLib](https://modrinth.com/mod/baguettelib) — HUD/overlay.
- [ ] Audit 73: [Better Advancements](https://modrinth.com/mod/better-advancements) — Standalone screen.
- [ ] Audit 76: [Better Combat](https://modrinth.com/mod/better-combat) — HUD/overlay.
- [ ] Audit 79: [Better Mount HUD](https://modrinth.com/mod/better-mount-hud) — HUD/overlay.
- [ ] Audit 80: [Better Statistics Screen](https://modrinth.com/mod/better-stats) — Standalone screen.
- [ ] Audit 81: [Better Trim Tooltips](https://modrinth.com/mod/better-trim-tooltips) — Tooltip.
- [ ] Audit 89: [Blueprint](https://modrinth.com/mod/blueprint) — Standalone screen.
- [ ] Audit 90: [Blur+](https://modrinth.com/mod/blur-plus) — HUD/overlay.
- [ ] Audit 92: [Boatload](https://modrinth.com/mod/boatload) — Framework/settings.
- [ ] Audit 96: [Bosses of Mass Destruction](https://modrinth.com/mod/bosses-of-mass-destruction) — HUD/overlay.
- [ ] Audit 100: [Build Guide](https://modrinth.com/mod/build-guide) — Container/menu.
- [ ] Audit 103: [Certain Questing Additions](https://modrinth.com/mod/certain-questing-additions) — Framework/settings.
- [ ] Audit 111: [Chipped Express](https://modrinth.com/mod/chipped-express) — Framework/settings.
- [ ] Audit 115: [Citadel](https://modrinth.com/mod/citadel) — Standalone screen.
- [ ] Audit 116: Class Nametags — Framework/settings.
- [ ] Audit 118: Client Fixes — Tooltip.
- [ ] Audit 120: clientcrafting mod — Framework/settings.
- [ ] Audit 123: [Combat Roll](https://modrinth.com/mod/combat-roll) — HUD/overlay.
- [ ] Audit 134: Create Aeronautics — Framework/settings.
- [ ] Audit 135: [Create Aeronautics](https://modrinth.com/mod/create-aeronautics) — Framework/settings.
- [ ] Audit 137: [Create Crafts & Additions](https://modrinth.com/mod/createaddition) — Recipe/browser addon.
- [ ] Audit 139: Create Offroad — Framework/settings.
- [ ] Audit 140: [Create Questing](https://modrinth.com/mod/create-questing) — Framework/settings.
- [ ] Audit 142: [Create Slice & Dice](https://modrinth.com/mod/slice-and-dice) — Recipe/browser addon.
- [ ] Audit 143: [Create: Copycats+](https://modrinth.com/mod/copycats) — Tooltip.
- [ ] Audit 144: [Create: Dragons Plus](https://modrinth.com/mod/create-dragons-plus) — Recipe/browser addon.
- [ ] Audit 145: [Create: Enchantment Industry](https://modrinth.com/mod/create-enchantment-industry) — Recipe/browser addon.
- [ ] Audit 147: [Create: Extended Wrenches](https://modrinth.com/mod/extended-wrenches) — Recipe/browser addon.
- [ ] Audit 148: [Create: Factory](https://modrinth.com/mod/create-factory) — Recipe/browser addon.
- [ ] Audit 149: [Create: New Age](https://modrinth.com/mod/create-new-age) — Recipe/browser addon.
- [ ] Audit 150: [Create: Pattern Schematics](https://modrinth.com/mod/create-pattern-schematics) — Standalone screen.
- [ ] Audit 153: [Create: Testosterone](https://modrinth.com/mod/testosterone) — HUD/overlay.
- [ ] Audit 160: Cumulus — Standalone screen.
- [ ] Audit 162: Custom Portal Api — HUD/overlay.
- [ ] Audit 163: [Custom Tooltip API](https://modrinth.com/mod/custom-tooltip-api) — Inventory/render/input.
- [ ] Audit 165: [Dark Utilities](https://modrinth.com/mod/dark-utilities) — Framework/settings.
- [ ] Audit 167: [Debugify](https://modrinth.com/mod/debugify) — Inventory/render/input.
- [ ] Audit 169: [Deeper and Darker](https://modrinth.com/mod/deeperdarker) — Tooltip.
- [ ] Audit 171: [Dehydration](https://modrinth.com/mod/dehydration) — HUD/overlay.
- [ ] Audit 173: [Detail Armor Bar Reconstructed](https://modrinth.com/mod/detail-armor-bar-reconstructed) — Standalone screen.
- [ ] Audit 174: [Distant Horizons](https://modrinth.com/mod/distanthorizons) — Standalone screen.
- [ ] Audit 175: [Distraction Free Recipes (EMI / REI / JEI)](https://modrinth.com/mod/distraction-free-recipes) — HUD/overlay.
- [ ] Audit 176: [Do a Barrel Roll](https://modrinth.com/mod/do-a-barrel-roll) — HUD/overlay.
- [ ] Audit 178: [Dragon Mounts: Legacy](https://modrinth.com/mod/dragon-mounts-legacy) — Framework/settings.
- [ ] Audit 183: [Dynamic FPS](https://modrinth.com/mod/dynamic-fps) — HUD/overlay.
- [ ] Audit 185: Dynamic RPG Resource Bars — HUD/overlay.
- [ ] Audit 193: [Eden Ring Reforked](https://modrinth.com/mod/eden-ring-reforked) — Standalone screen.
- [ ] Audit 194: [Eldritch End](https://modrinth.com/mod/eldritch-end) — HUD/overlay.
- [ ] Audit 196: [EMI Addon: Extra Mod Integrations](https://modrinth.com/mod/extra-mod-integrations) — Recipe/browser addon.
- [ ] Audit 197: [EMI Compat](https://modrinth.com/mod/emi-compat) — Framework/settings.
- [ ] Audit 198: [EMI Enchanting](https://modrinth.com/mod/emi-enchanting) — Framework/settings.
- [ ] Audit 199: [EMI Loot](https://modrinth.com/mod/emi-loot) — Recipe/browser addon.
- [ ] Audit 200: [EMI Ores](https://modrinth.com/mod/emi-ores) — Recipe/browser addon.
- [ ] Audit 201: [EMI professions (EMIP)](https://modrinth.com/mod/emi-professions-emip) — Recipe/browser addon.
- [ ] Audit 202: [EMI Trades](https://modrinth.com/mod/emitrades) — Framework/settings.
- [ ] Audit 203: [EMIffect](https://modrinth.com/mod/emiffect) — Framework/settings.
- [ ] Audit 204: EmLib — HUD/overlay.
- [ ] Audit 206: [Enchantment Descriptions](https://modrinth.com/mod/enchantment-descriptions) — Framework/settings.
- [ ] Audit 207: [End's Delight](https://modrinth.com/mod/ends-delight) — Tooltip.
- [ ] Audit 208: [Enderman Overhaul](https://modrinth.com/mod/enderman-overhaul) — HUD/overlay.
- [ ] Audit 209: [Entity Culling](https://modrinth.com/mod/entityculling) — HUD/overlay.
- [ ] Audit 210: [EnvironmentZ](https://modrinth.com/mod/environmentz) — HUD/overlay.
- [ ] Audit 213: [Essential Mod](https://modrinth.com/mod/essential) — Container/menu.
- [ ] Audit 214: [Estrogen](https://modrinth.com/mod/estrogen) — Standalone screen.
- [ ] Audit 216: [Every Compat (Wood Good)](https://modrinth.com/mod/every-compat) — Standalone screen.
- [ ] Audit 218: [Explorer's Compass](https://modrinth.com/mod/explorers-compass) — Standalone screen.
- [ ] Audit 221: [Extended Drawers](https://modrinth.com/mod/extended-drawers) — Standalone screen.
- [ ] Audit 223: [Extra RPG Attributes](https://modrinth.com/mod/extra-rpg-attributes) — HUD/overlay.
- [ ] Audit 241: [Fancy Block Particles - Renewed](https://modrinth.com/mod/fbp-renewed) — Standalone screen.
- [ ] Audit 242: Fancy Hotbar — HUD/overlay.
- [ ] Audit 248: [FleshZ](https://modrinth.com/mod/fleshz) — Framework/settings.
- [ ] Audit 263: [Formations (Structure Library)](https://modrinth.com/mod/formations) — Standalone screen.
- [ ] Audit 266: [Frostiful](https://modrinth.com/mod/frostiful) — HUD/overlay.
- [ ] Audit 267: FTB Library — Container/menu.
- [ ] Audit 268: [FTB Quest Translator](https://modrinth.com/mod/ftb-quest-translator) — Standalone screen.
- [ ] Audit 269: FTB Quests — Standalone screen.
- [ ] Audit 271: FTB Teams — Standalone screen.
- [ ] Audit 272: [FTB X EMI Compat](https://modrinth.com/mod/ftb-x-emi-compat) — Framework/settings.
- [ ] Audit 279: Gimm1q — Standalone screen.
- [ ] Audit 281: [Gnetum](https://modrinth.com/mod/gnetum) — Standalone screen.
- [ ] Audit 284: [Gun Scaling](https://modrinth.com/mod/gun-scaling) — Tooltip.
- [ ] Audit 285: [Handcrafted](https://modrinth.com/mod/handcrafted) — Tooltip.
- [ ] Audit 287: [Highlight](https://modrinth.com/mod/highlight) — Framework/settings.
- [ ] Audit 288: [HT's TreeChop](https://modrinth.com/mod/treechop) — Standalone screen.
- [ ] Audit 289: [Hybrid Aquatic](https://modrinth.com/mod/hybrid-aquatic) — HUD/overlay.
- [ ] Audit 290: [Icarus x Transmog compat](https://modrinth.com/mod/icarus-x-transmog-compat) — Framework/settings.
- [ ] Audit 291: [Iceberg](https://modrinth.com/mod/iceberg) — Framework/settings.
- [ ] Audit 292: [iChunUtil](https://modrinth.com/mod/ichunutil) — HUD/overlay.
- [ ] Audit 293: [Idwtialsimmoedm](https://modrinth.com/mod/idwtialsimmoedm) — Inventory/render/input.
- [ ] Audit 296: [ImmediatelyFast](https://modrinth.com/mod/immediatelyfast) — Inventory/render/input.
- [ ] Audit 298: [Immersive Armor HUD](https://modrinth.com/mod/immersive-armor-hud) — HUD/overlay.
- [ ] Audit 299: [Immersive Armors](https://modrinth.com/mod/immersive-armors) — HUD/overlay.
- [ ] Audit 300: [Immersive Melodies](https://modrinth.com/mod/immersive-melodies) — Standalone screen.
- [ ] Audit 301: [Immersive Melodies in Backpacks](https://modrinth.com/mod/immersive-melodies-in-backpacks) — Framework/settings.
- [ ] Audit 302: [Immersive Messages API](https://modrinth.com/mod/immersive-messages-api) — Tooltip.
- [ ] Audit 303: [Immersive Optimization](https://modrinth.com/mod/immersive-optimization) — HUD/overlay.
- [ ] Audit 304: [Immersive Tips](https://modrinth.com/mod/immersive-tips) — Tooltip.
- [ ] Audit 305: [Immersive UI](https://modrinth.com/mod/immersive-ui) — HUD/overlay.
- [ ] Audit 308: [InmisAddon](https://modrinth.com/mod/inmisaddon) — Inventory/render/input.
- [ ] Audit 311: [Inventory Blur](https://modrinth.com/mod/inventory-blur) — Framework/settings.
- [ ] Audit 313: [Invocations](https://modrinth.com/mod/invocations) — HUD/overlay.
- [ ] Audit 315: [Iris Shaders](https://modrinth.com/mod/iris) — Standalone screen.
- [ ] Audit 316: [Iris/Oculus Shader Folder](https://modrinth.com/mod/iris-shader-folder) — Tooltip.
- [ ] Audit 319: [Item Borders](https://modrinth.com/mod/item-borders) — Inventory/render/input.
- [ ] Audit 321: [Item Highlighter](https://modrinth.com/mod/item-highlighter) — Inventory/render/input.
- [ ] Audit 322: [Item Obliterator](https://modrinth.com/mod/item-obliterator) — Framework/settings.
- [ ] Audit 325: [Jade Addons (Fabric)](https://modrinth.com/mod/jade-addons-fabric) — Tooltip.
- [ ] Audit 326: [Jade Addons (Neo/Forge)](https://modrinth.com/mod/jade-addons-forge) — Framework/settings.
- [ ] Audit 327: Jade Prettier HP — Framework/settings.
- [ ] Audit 328: [Jade 🔍](https://modrinth.com/mod/jade) — HUD/overlay.
- [ ] Audit 329: [Jaden's Nether Expansion](https://modrinth.com/mod/jadens-nether-expansion) — HUD/overlay.
- [ ] Audit 333: [Just Enough Breeding (JEBr)](https://modrinth.com/mod/justenoughbreeding) — Recipe/browser addon.
- [ ] Audit 334: [Just Enough Professions (JEP)](https://modrinth.com/mod/just-enough-professions-jep) — Recipe/browser addon.
- [ ] Audit 335: [Just Enough Resources (JER)](https://modrinth.com/mod/just-enough-resources-jer) — Recipe/browser addon.
- [ ] Audit 347: LambDynamicLights (Runtime) — Standalone screen.
- [ ] Audit 348: [LambDynamicLights - Dynamic Lights](https://modrinth.com/mod/lambdynamiclights) — Standalone screen.
- [ ] Audit 350: [Lavender](https://modrinth.com/mod/lavender) — Inventory/render/input.
- [ ] Audit 353: [Legacy: [Let's Do] Brewery](https://modrinth.com/mod/lets-do-brewery) — Shared appliance/workflow integration.
- [ ] Audit 354: [Legendary Tooltips](https://modrinth.com/mod/legendary-tooltips) — HUD/overlay.
- [ ] Audit 355: [Legendary Tooltips x Traveler's Backpack Fix](https://modrinth.com/mod/legendary-tooltips-x-travelers-backpack-fix) — Tooltip.
- [ ] Audit 359: [Lionfish-API](https://modrinth.com/mod/lionfish-api) — Standalone screen.
- [ ] Audit 366: [Map Atlases [Forge]](https://modrinth.com/mod/map-atlases-forge) — Standalone screen.
- [ ] Audit 368: [Marium's Soulslike Weaponry](https://modrinth.com/mod/mariums-soulslike-weaponry) — HUD/overlay.
- [ ] Audit 369: [MC Dungeons Armors](https://modrinth.com/mod/mcda) — Framework/settings.
- [ ] Audit 370: [MC Dungeons Weapons](https://modrinth.com/mod/mcdw) — HUD/overlay.
- [ ] Audit 372: [MedievalWeapons](https://modrinth.com/mod/medievalweapons) — HUD/overlay.
- [ ] Audit 375: [MEGA Cells](https://modrinth.com/mod/mega) — Storage semantic extension.
- [ ] Audit 380: [Mindful Darkness](https://modrinth.com/mod/mindful-darkness) — Framework/settings.
- [ ] Audit 383: [Mining Speed Tooltips](https://modrinth.com/mod/mining-speed-tooltips) — Tooltip.
- [ ] Audit 385: Mmm Indicators — Framework/settings.
- [ ] Audit 387: MobHealthBar Tweaks — Framework/settings.
- [ ] Audit 392: [Moog's Glow Up](https://modrinth.com/mod/moogs-glow-up) — Tooltip.
- [ ] Audit 393: [Moog's Structure Lib (moogs_structures)](https://modrinth.com/mod/moogs-structure-lib) — Standalone screen.
- [ ] Audit 400: [Mutant Monsters](https://modrinth.com/mod/mutant-monsters) — Standalone screen.
- [ ] Audit 401: [Mutant Skeleton Armor Tweaks](https://modrinth.com/mod/mutant-skeleton-armor-tweaks) — Tooltip.
- [ ] Audit 402: [My Nether's Delight](https://modrinth.com/mod/my-nethers-delight) — Recipe/browser addon.
- [ ] Audit 403: [My Nether's Delight Refabricated](https://modrinth.com/mod/my-nethers-delight-refabricated) — Recipe/browser addon.
- [ ] Audit 404: [Mythic Charms](https://modrinth.com/mod/mythic-charms) — Recipe/browser addon.
- [ ] Audit 406: [Nameplate](https://modrinth.com/mod/nameplate) — Framework/settings.
- [ ] Audit 409: [Nature's Compass](https://modrinth.com/mod/natures-compass) — Standalone screen.
- [ ] Audit 414: [Nicer Skies](https://modrinth.com/mod/nicer-skies) — Tooltip.
- [ ] Audit 416: Nitrogen — Recipe/browser addon.
- [ ] Audit 417: [No Chat Reports](https://modrinth.com/mod/no-chat-reports) — Standalone screen.
- [ ] Audit 418: No Indium? — Standalone screen.
- [ ] Audit 421: [Not Enough Recipe Book [NERB]](https://modrinth.com/mod/notenoughrecipebook) — Framework/settings.
- [ ] Audit 424: [Oculus](https://modrinth.com/mod/oculus) — Standalone screen.
- [ ] Audit 427: [OPAC-UI](https://modrinth.com/mod/opac-ui) — Standalone screen.
- [ ] Audit 428: [Open Parties and Claims](https://modrinth.com/mod/open-parties-and-claims) — Container/menu.
- [ ] Audit 429: [Overflowing Bars](https://modrinth.com/mod/overflowing-bars) — Standalone screen.
- [ ] Audit 434: [Patchouli](https://modrinth.com/mod/patchouli) — Inventory/render/input.
- [ ] Audit 437: [Pick Up Notifier](https://modrinth.com/mod/pick-up-notifier) — Framework/settings.
- [ ] Audit 438: [Ping Wheel](https://modrinth.com/mod/ping-wheel) — HUD/overlay.
- [ ] Audit 440: [Pockets](https://modrinth.com/mod/pockets) — Framework/settings.
- [ ] Audit 444: [Polytone](https://modrinth.com/mod/polytone) — Inventory/render/input.
- [ ] Audit 445: Ponder — Standalone screen.
- [ ] Audit 455: [Presence Footsteps](https://modrinth.com/mod/presence-footsteps) — HUD/overlay.
- [ ] Audit 456: [Progress Peek](https://modrinth.com/mod/progresspeek) — HUD/overlay.
- [ ] Audit 457: [Projectile Damage Attribute](https://modrinth.com/mod/projectile-damage-attribute) — HUD/overlay.
- [ ] Audit 462: Prominent Talents — Framework/settings.
- [ ] Audit 464: [Pufferfish's Skills](https://modrinth.com/mod/skills) — Standalone screen.
- [ ] Audit 468: PVelocity-Prom2-Patch — Standalone screen.
- [ ] Audit 470: [Quartz Elevator](https://modrinth.com/mod/quartz-elevator) — Framework/settings.
- [ ] Audit 472: Quests Customizer — Framework/settings.
- [ ] Audit 473: [quick pack](https://modrinth.com/mod/quick-pack) — HUD/overlay.
- [ ] Audit 474: [Raise Sound Limit Simplified](https://modrinth.com/mod/rsls) — HUD/overlay.
- [ ] Audit 475: [Ranged Weapon API](https://modrinth.com/mod/ranged-weapon-api) — Tooltip.
- [ ] Audit 478: [Reacharound](https://modrinth.com/mod/reacharound) — HUD/overlay.
- [ ] Audit 479: [Reactive Music](https://modrinth.com/mod/reactive-music) — HUD/overlay.
- [ ] Audit 482: recipeessentials Mod — Framework/settings.
- [ ] Audit 483: [RecipeRemover](https://modrinth.com/mod/reciperemover) — Inventory/render/input.
- [ ] Audit 484: [Redirected](https://modrinth.com/mod/redirected) — HUD/overlay.
- [ ] Audit 487: [Reliable Advancements](https://modrinth.com/mod/reliable-advancements) — Standalone screen.
- [ ] Audit 488: [Reliable EMI (REMI)](https://modrinth.com/mod/reliable-emi) — Inventory/render/input.
- [ ] Audit 494: [Resourcify](https://modrinth.com/mod/resourcify) — Standalone screen.
- [ ] Audit 497: [Roughly Enough Professions (REP)](https://modrinth.com/mod/roughly-enough-professions-rep) — Recipe/browser addon.
- [ ] Audit 501: [Sable](https://modrinth.com/mod/sable) — Standalone screen.
- [ ] Audit 506: [Scholar](https://modrinth.com/mod/scholar) — Container/menu.
- [ ] Audit 508: [Seamless Sleep](https://modrinth.com/mod/seamless-sleep) — HUD/overlay.
- [ ] Audit 510: [SeasonHud-Fabric](https://modrinth.com/mod/seasonhud-fabric) — Standalone screen.
- [ ] Audit 515: [Shield Expansion](https://modrinth.com/mod/shield-expansion) — Tooltip.
- [ ] Audit 517: [Show Me What You Got](https://modrinth.com/mod/show-me-what-you-got) — Inventory/render/input.
- [ ] Audit 518: [Shut Up MCD](https://modrinth.com/mod/shut-up-mcd) — Framework/settings.
- [ ] Audit 521: [Simple Voice Chat](https://modrinth.com/mod/simple-voice-chat) — Standalone screen.
- [ ] Audit 522: [Simply Bows](https://modrinth.com/mod/simply-bows) — Tooltip.
- [ ] Audit 523: [Simply Skills](https://modrinth.com/mod/simply-skills) — HUD/overlay.
- [ ] Audit 526: [Simply Tooltips](https://modrinth.com/mod/simply-tooltips) — Standalone screen.
- [ ] Audit 527: SimplyCompat — Standalone screen.
- [ ] Audit 528: [Sinytra Connector](https://modrinth.com/mod/connector) — HUD/overlay.
- [ ] Audit 531: [Smart Tooltip Scroll](https://modrinth.com/mod/smart-tooltip-scroll) — Standalone screen.
- [ ] Audit 534: [Smooth Swapping](https://modrinth.com/mod/smooth-swapping) — Inventory/render/input.
- [ ] Audit 535: [SMWYG-Fix](https://modrinth.com/mod/smwyg-fix) — Tooltip.
- [ ] Audit 538: [Sodium](https://modrinth.com/mod/sodium) — HUD/overlay.
- [ ] Audit 540: [Sodium Extra](https://modrinth.com/mod/sodium-extra) — HUD/overlay.
- [ ] Audit 541: [Sodium Extras](https://modrinth.com/mod/sodium-extras) — HUD/overlay.
- [ ] Audit 545: [Sorted Enchantments](https://modrinth.com/mod/sorted-enchantments) — Framework/settings.
- [ ] Audit 550: [Sounds](https://modrinth.com/mod/sound) — Standalone screen.
- [ ] Audit 556: SpruceUI — Standalone screen.
- [ ] Audit 557: SpruceUI — Standalone screen.
- [ ] Audit 559: Statue Menus — Container/menu.
- [ ] Audit 560: [Status Effect Bars Reforged](https://modrinth.com/mod/status-effect-bars-reforged) — Framework/settings.
- [ ] Audit 562: [Stfu](https://modrinth.com/mod/shuttfup) — HUD/overlay.
- [ ] Audit 563: [Structure Gel API](https://modrinth.com/mod/structure-gel-api) — Standalone screen.
- [ ] Audit 564: [Stylish Effects](https://modrinth.com/mod/stylish-effects) — Standalone screen.
- [ ] Audit 566: [Superb Steeds](https://modrinth.com/mod/superb-steeds) — Tooltip.
- [ ] Audit 569: [Supplementaries Squared](https://modrinth.com/mod/supplementaries-squared) — Standalone screen.
- [ ] Audit 572: [TalkBubbles](https://modrinth.com/mod/talkbubbles) — HUD/overlay.
- [ ] Audit 577: [The Aether: Redux](https://modrinth.com/mod/the-aether-redux) — Standalone screen.
- [ ] Audit 580: [Thermoo](https://modrinth.com/mod/thermoo) — HUD/overlay.
- [ ] Audit 581: [Thermoo Patches](https://modrinth.com/mod/thermoo-patches) — HUD/overlay.
- [ ] Audit 586: [Tool Stats](https://modrinth.com/mod/tool-stats) — Framework/settings.
- [ ] Audit 588: [Translucency Fix](https://modrinth.com/mod/translucencyfix) — Framework/settings.
- [ ] Audit 592: [TravelerZ](https://modrinth.com/mod/travelerz) — HUD/overlay.
- [ ] Audit 593: [TravellersBackpack-Transmog-Compat](https://modrinth.com/mod/travellersbackpack-transmog-compat) — Framework/settings.
- [ ] Audit 597: UniversalCraft — Standalone screen.
- [ ] Audit 602: Veil — Standalone screen.
- [ ] Audit 603: Vigilance — Standalone screen.
- [ ] Audit 611: [Wakes](https://modrinth.com/mod/wakes) — HUD/overlay.
- [ ] Audit 612: [Wakes Reforged](https://modrinth.com/mod/wakes-reforged) — HUD/overlay.
- [ ] Audit 616: [What Are They Up To (Watut)](https://modrinth.com/mod/what-are-they-up-to) — Standalone screen.
- [ ] Audit 621: Xaero UI Tweaks — HUD/overlay.
- [ ] Audit 622: [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) — Standalone screen.
- [ ] Audit 623: [Xaero's World Map](https://modrinth.com/mod/xaeros-world-map) — Container/menu.
- [ ] Audit 625: [XaeroPlus](https://modrinth.com/mod/xaeroplus) — Standalone screen.
- [ ] Audit 626: [Xenon](https://modrinth.com/mod/xenon-forge) — Standalone screen.
- [ ] Audit 629: [YDM's MobHealthBar](https://modrinth.com/mod/ydms-mobhealthbar) — Standalone screen.
- [ ] Audit 644: [Zenith Attributes Renewed](https://modrinth.com/mod/zenith-attributes-renewed) — Inventory/render/input.

### P3 (267 entries)

- [ ] Audit 1: [3D Skin Layers](https://modrinth.com/mod/3dskinlayers) — Framework/settings.
- [ ] Audit 3: [[EMF] Entity Model Features](https://modrinth.com/mod/entity-model-features) — Framework/settings.
- [ ] Audit 22: [Advancements Fullscreen](https://modrinth.com/mod/advancements-fullscreen) — Framework/settings.
- [ ] Audit 23: [Advancements Search](https://modrinth.com/mod/advancements-search) — Framework/settings.
- [ ] Audit 24: [AdventureZ](https://modrinth.com/mod/adventurez) — Framework/settings.
- [ ] Audit 25: [AE2 + Mouse Tweaks Fix](https://modrinth.com/mod/ae2-mousetweaks-fix) — Framework/settings.
- [ ] Audit 28: [Aether: Lost Content Addon](https://modrinth.com/mod/aether-lost-content) — Framework/settings.
- [ ] Audit 29: Aged-Addition — Framework/settings.
- [ ] Audit 30: [Aileron](https://modrinth.com/mod/aileron) — Framework/settings.
- [ ] Audit 32: [All The Trims](https://modrinth.com/mod/allthetrims) — Framework/settings.
- [ ] Audit 36: [Amecs](https://modrinth.com/mod/amecs) — Framework/settings.
- [ ] Audit 37: Amecs API — Framework/settings.
- [ ] Audit 38: Amecs Core - Key Modifiers — Framework/settings.
- [ ] Audit 39: Amecs Core - Mouse Inputs — Framework/settings.
- [ ] Audit 40: Amecs Core - Priority Key Mappings — Framework/settings.
- [ ] Audit 43: [Animatica](https://modrinth.com/mod/animatica) — Framework/settings.
- [ ] Audit 51: [Architectury API](https://modrinth.com/mod/architectury-api) — Framework/settings.
- [ ] Audit 55: [Ascended Quark](https://modrinth.com/mod/ascended-quark) — Framework/settings.
- [ ] Audit 56: [Ash API](https://modrinth.com/mod/ash-api) — Framework/settings.
- [ ] Audit 57: [Astrocraft](https://modrinth.com/mod/astrocraft) — Framework/settings.
- [ ] Audit 58: [AsyncParticles](https://modrinth.com/mod/asyncparticles) — Framework/settings.
- [ ] Audit 61: [AutoSlabs](https://modrinth.com/mod/autoslabs) — Framework/settings.
- [ ] Audit 62: [Auudio](https://modrinth.com/mod/auudio) — Framework/settings.
- [ ] Audit 66: [Bad Wither No Cookie - Reloaded](https://modrinth.com/mod/bad-wither-no-cookie) — Framework/settings.
- [ ] Audit 69: [Balm](https://modrinth.com/mod/balm) — Framework/settings.
- [ ] Audit 71: [BCLib](https://modrinth.com/mod/bclib) — Framework/settings.
- [ ] Audit 72: [Bento Box](https://modrinth.com/mod/bento-box) — Standalone screen.
- [ ] Audit 75: [Better Biome Reblend](https://modrinth.com/mod/bbrb) — Framework/settings.
- [ ] Audit 77: [Better Compatibility Checker](https://modrinth.com/mod/better-compatibility-checker) — Framework/settings.
- [ ] Audit 78: [Better Mods Button](https://modrinth.com/mod/better-mods-button) — Framework/settings.
- [ ] Audit 82: [Better World Loading](https://modrinth.com/mod/better-world-loading) — Framework/settings.
- [ ] Audit 84: [BetterGrassify](https://modrinth.com/mod/bettergrassify) — Framework/settings.
- [ ] Audit 85: [BetterNether](https://modrinth.com/mod/betternether) — Framework/settings.
- [ ] Audit 86: [Biomes O' Plenty](https://modrinth.com/mod/biomes-o-plenty) — Framework/settings.
- [ ] Audit 87: [BisectHosting Server Integration Menu](https://modrinth.com/mod/bisect-mod) — Standalone screen.
- [ ] Audit 91: [Boat Item View](https://modrinth.com/mod/boat-item-view) — Framework/settings.
- [ ] Audit 93: Bobby — Framework/settings.
- [ ] Audit 94: [Bookshelf](https://modrinth.com/mod/bookshelf-lib) — Framework/settings.
- [ ] Audit 95: [Borderless Mining](https://modrinth.com/mod/borderless-mining) — Framework/settings.
- [ ] Audit 102: [Camera Overhaul](https://modrinth.com/mod/cameraoverhaul) — Framework/settings.
- [ ] Audit 104: [Chalk (Fabric)](https://modrinth.com/mod/chalk) — Framework/settings.
- [ ] Audit 106: Charmony — Framework/settings.
- [ ] Audit 107: [Chat Animation [Smooth Chat]](https://modrinth.com/mod/chatanimation) — Framework/settings.
- [ ] Audit 108: [Chat Heads](https://modrinth.com/mod/chat-heads) — Framework/settings.
- [ ] Audit 109: [Cherished Worlds](https://modrinth.com/mod/cherished-worlds) — Framework/settings.
- [ ] Audit 112: [CICADA](https://modrinth.com/mod/cicada) — Framework/settings.
- [ ] Audit 113: [CIT Resewn](https://modrinth.com/mod/cit-resewn) — Framework/settings.
- [ ] Audit 114: CIT Resewn: Defaults — Framework/settings.
- [ ] Audit 117: Clickable Advancements Mod — Framework/settings.
- [ ] Audit 121: [Cloth Config API](https://modrinth.com/mod/cloth-config) — Framework/settings.
- [ ] Audit 122: [Collective](https://modrinth.com/mod/collective) — Framework/settings.
- [ ] Audit 124: Configuration — Framework/settings.
- [ ] Audit 125: [Connectible Chains [Fabric]](https://modrinth.com/mod/connectiblechains) — Framework/settings.
- [ ] Audit 126: Connector Extras REI Bridge — Framework/settings.
- [ ] Audit 127: [Continuity](https://modrinth.com/mod/continuity) — Framework/settings.
- [ ] Audit 128: [Controlling](https://modrinth.com/mod/controlling) — Standalone screen.
- [ ] Audit 131: [Crash Assistant](https://modrinth.com/mod/crash-assistant) — Framework/settings.
- [ ] Audit 132: [Crawl](https://modrinth.com/mod/crawl) — Framework/settings.
- [ ] Audit 146: [Create: Estrogen](https://modrinth.com/mod/create-estrogen) — Framework/settings.
- [ ] Audit 154: [CreativeCore](https://modrinth.com/mod/creativecore) — Framework/settings.
- [ ] Audit 155: [Creeper Overhaul](https://modrinth.com/mod/creeper-overhaul) — Framework/settings.
- [ ] Audit 156: [Crop Growth Modifier](https://modrinth.com/mod/crop-growth-modifier) — Framework/settings.
- [ ] Audit 157: Croptopia — Framework/settings.
- [ ] Audit 158: [Cubes Without Borders](https://modrinth.com/mod/cubes-without-borders) — Framework/settings.
- [ ] Audit 159: [Cull Less Leaves](https://modrinth.com/mod/cull-less-leaves) — Framework/settings.
- [ ] Audit 161: cupboard — Framework/settings.
- [ ] Audit 164: [Cynosure](https://modrinth.com/mod/cynosure) — Framework/settings.
- [ ] Audit 166: [Dark Window Bar](https://modrinth.com/mod/dark-window-bar) — Framework/settings.
- [ ] Audit 170: [Default Options](https://modrinth.com/mod/default-options) — Framework/settings.
- [ ] Audit 172: [Delete Worlds To Trash](https://modrinth.com/mod/delete-worlds-to-trash) — Standalone screen.
- [ ] Audit 177: [Double Doors](https://modrinth.com/mod/double-doors) — Framework/settings.
- [ ] Audit 179: [Drip Sounds](https://modrinth.com/mod/dripsounds) — Framework/settings.
- [ ] Audit 180: [Drippy Loading Screen](https://modrinth.com/mod/drippy-loading-screen) — Standalone screen.
- [ ] Audit 181: [Dungeon Now Loading](https://modrinth.com/mod/dungeon-now-loading) — Framework/settings.
- [ ] Audit 184: Dynamic Resource Bars Formatting — Framework/settings.
- [ ] Audit 190: [Eating Animation](https://modrinth.com/mod/eating-animation) — Framework/settings.
- [ ] Audit 191: [Eating Animations](https://modrinth.com/mod/eating-animations) — Framework/settings.
- [ ] Audit 192: [Ecologics](https://modrinth.com/mod/ecologics) — Framework/settings.
- [ ] Audit 211: EpheroLib — Framework/settings.
- [ ] Audit 212: [EpheroLib](https://modrinth.com/mod/epherolib) — Framework/settings.
- [ ] Audit 215: [Euphoria Patches](https://modrinth.com/mod/euphoria-patches) — Framework/settings.
- [ ] Audit 225: [Fabric API](https://modrinth.com/mod/fabric-api) — Framework/settings.
- [ ] Audit 226: Fabric Events Lifecycle (v0) — Framework/settings.
- [ ] Audit 227: Fabric Game Rule API (v1) — Framework/settings.
- [ ] Audit 228: Fabric Item API (v1) — Framework/settings.
- [ ] Audit 229: Fabric Item Group API (v1) — Framework/settings.
- [ ] Audit 230: Fabric Networking API (v1) — Framework/settings.
- [ ] Audit 231: Fabric Object Builder API (v1) — Framework/settings.
- [ ] Audit 232: Fabric Renderer API (v1) — Framework/settings.
- [ ] Audit 233: Fabric Rendering (v1) — Framework/settings.
- [ ] Audit 234: Fabric Resource Loader (v0) — Framework/settings.
- [ ] Audit 235: Fabric Screen API (v1) — Framework/settings.
- [ ] Audit 236: Fabric Screen Handler API (v1) — Framework/settings.
- [ ] Audit 237: [Fabric Seasons](https://modrinth.com/mod/fabric-seasons) — Framework/settings.
- [ ] Audit 239: [FakerLib](https://modrinth.com/mod/fakerlib) — Framework/settings.
- [ ] Audit 240: [Falling Leaves](https://modrinth.com/mod/fallingleaves) — Framework/settings.
- [ ] Audit 246: [Fast IP Ping](https://modrinth.com/mod/fast-ip-ping) — Framework/settings.
- [ ] Audit 247: [First-person Model](https://modrinth.com/mod/first-person-model) — Framework/settings.
- [ ] Audit 249: Flywheel — Framework/settings.
- [ ] Audit 250: [Foolproof](https://modrinth.com/mod/foolproof) — Framework/settings.
- [ ] Audit 251: [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port) — Framework/settings.
- [ ] Audit 252: [Forge Config Screens](https://modrinth.com/mod/forge-config-screens) — Framework/settings.
- [ ] Audit 253: [Forgified Fabric API](https://modrinth.com/mod/forgified-fabric-api) — Framework/settings.
- [ ] Audit 254: Forgified Fabric Game Rule API (v1) — Framework/settings.
- [ ] Audit 255: Forgified Fabric Item API (v1) — Framework/settings.
- [ ] Audit 256: Forgified Fabric Item Group API (v1) — Framework/settings.
- [ ] Audit 257: Forgified Fabric Networking API (v1) — Framework/settings.
- [ ] Audit 258: Forgified Fabric Renderer API (v1) — Framework/settings.
- [ ] Audit 259: Forgified Fabric Rendering (v1) — Framework/settings.
- [ ] Audit 260: Forgified Fabric Resource Loader (v0) — Framework/settings.
- [ ] Audit 261: Forgified Fabric Screen API (v1) — Framework/settings.
- [ ] Audit 262: Forgified Fabric Screen Handler API (v1) — Framework/settings.
- [ ] Audit 264: [Fragmentum](https://modrinth.com/mod/fragmentum) — Framework/settings.
- [ ] Audit 265: [Friends&Foes (Fabric/Quilt)](https://modrinth.com/mod/friends-and-foes) — Framework/settings.
- [ ] Audit 270: [FTB Quests Freeze Fix](https://modrinth.com/mod/ftb-quests-freeze-fix) — Framework/settings.
- [ ] Audit 273: FTB XMod Compat — Recipe/browser addon.
- [ ] Audit 274: [Fzzy Config](https://modrinth.com/mod/fzzy-config) — Tooltip.
- [ ] Audit 277: [Gardens of the Dead](https://modrinth.com/mod/gardens-of-the-dead) — Framework/settings.
- [ ] Audit 278: [Geckolib](https://modrinth.com/mod/geckolib) — Framework/settings.
- [ ] Audit 280: [GlitchCore](https://modrinth.com/mod/glitchcore) — Framework/settings.
- [ ] Audit 286: [HerdsPanic](https://modrinth.com/mod/herdspanic) — Framework/settings.
- [ ] Audit 306: [ImmersiveThunder](https://modrinth.com/mod/immersivethunder) — Framework/settings.
- [ ] Audit 309: [Integrated API](https://modrinth.com/mod/integrated-api) — Framework/settings.
- [ ] Audit 310: [Integrated Villages](https://modrinth.com/mod/integrated-villages) — Framework/settings.
- [ ] Audit 314: [Iris & Oculus Search](https://modrinth.com/mod/irissearch) — Framework/settings.
- [ ] Audit 318: [It Takes a Pillage](https://modrinth.com/mod/it-takes-a-pillage) — Framework/settings.
- [ ] Audit 324: [Ixeris](https://modrinth.com/mod/ixeris) — Framework/settings.
- [ ] Audit 330: [JamLib](https://modrinth.com/mod/jamlib) — Framework/settings.
- [ ] Audit 332: [Jump Over Fences](https://modrinth.com/mod/jump-over-fences) — Framework/settings.
- [ ] Audit 336: [Just Zoom](https://modrinth.com/mod/just-zoom) — Framework/settings.
- [ ] Audit 337: [Kambrik](https://modrinth.com/mod/kambrik) — Framework/settings.
- [ ] Audit 338: [Keybind Fix](https://modrinth.com/mod/keybind-fix) — Framework/settings.
- [ ] Audit 340: Kirin UI — Framework/settings.
- [ ] Audit 341: [Kiwi 🥝](https://modrinth.com/mod/kiwi) — Framework/settings.
- [ ] Audit 342: [Konkrete](https://modrinth.com/mod/konkrete) — Framework/settings.
- [ ] Audit 343: [Krypton Reno](https://modrinth.com/mod/krypton-fnp) — Framework/settings.
- [ ] Audit 344: KumaAPI — Framework/settings.
- [ ] Audit 345: Kyrpt Config — Framework/settings.
- [ ] Audit 349: [Language Reload](https://modrinth.com/mod/language-reload) — Framework/settings.
- [ ] Audit 351: [lazy-language-loader](https://modrinth.com/mod/lazy-language-loader) — Framework/settings.
- [ ] Audit 352: [Leawind's Third Person](https://modrinth.com/mod/leawind-third-person) — Framework/settings.
- [ ] Audit 358: [LibZ](https://modrinth.com/mod/libz) — Framework/settings.
- [ ] Audit 360: [Load My Resources](https://modrinth.com/mod/load-my-resources) — Framework/settings.
- [ ] Audit 361: Loading Protection — Framework/settings.
- [ ] Audit 362: [Lodestone](https://modrinth.com/mod/lodestonelib) — Framework/settings.
- [ ] Audit 363: [Longer Chat History](https://modrinth.com/mod/longer-chat-history) — Framework/settings.
- [ ] Audit 365: [Luna](https://modrinth.com/mod/luna) — Framework/settings.
- [ ] Audit 367: [Map Distance Fix](https://modrinth.com/mod/map-distance-fix) — Framework/settings.
- [ ] Audit 373: [Mediumcore](https://modrinth.com/mod/alexs-mediumcore) — Framework/settings.
- [ ] Audit 374: mediumcore — Framework/settings.
- [ ] Audit 376: Memorysettings Mod — Framework/settings.
- [ ] Audit 377: [MidnightLib](https://modrinth.com/mod/midnightlib) — HUD/overlay.
- [ ] Audit 378: Milk Lib — Framework/settings.
- [ ] Audit 379: [Mim1q's Derelict](https://modrinth.com/mod/derelict) — Framework/settings.
- [ ] Audit 384: Missing Mods Checker — Framework/settings.
- [ ] Audit 386: [MmmMmmMmmMmm](https://modrinth.com/mod/mmmmmmmmmmmm) — Framework/settings.
- [ ] Audit 388: [Model Gap Fix](https://modrinth.com/mod/modelfix) — Framework/settings.
- [ ] Audit 390: [ModernFix](https://modrinth.com/mod/modernfix) — Framework/settings.
- [ ] Audit 391: [Modpack Update Checker](https://modrinth.com/mod/modpack-update-checker) — Standalone screen.
- [ ] Audit 394: [Moonlight Lib](https://modrinth.com/mod/moonlight) — Framework/settings.
- [ ] Audit 395: [More Culling](https://modrinth.com/mod/moreculling) — Framework/settings.
- [ ] Audit 396: [More Sounds](https://modrinth.com/mod/more-sounds) — Framework/settings.
- [ ] Audit 397: [MouseTweaks x Accessories Fix](https://modrinth.com/mod/mousetweaks-x-accessories-fix) — Framework/settings.
- [ ] Audit 399: [MRU](https://modrinth.com/mod/mru) — Framework/settings.
- [ ] Audit 407: [Naturalist](https://modrinth.com/mod/naturalist) — Framework/settings.
- [ ] Audit 408: Naturalist — Framework/settings.
- [ ] Audit 410: [Nature's Spirit](https://modrinth.com/mod/natures-spirit) — Framework/settings.
- [ ] Audit 411: [NBT Autocomplete](https://modrinth.com/mod/nbt-autocomplete) — Framework/settings.
- [ ] Audit 412: [Necronomicon API](https://modrinth.com/mod/necronomicon) — Framework/settings.
- [ ] Audit 419: [No Report Button](https://modrinth.com/mod/nrb) — Framework/settings.
- [ ] Audit 420: [Not Enough Animations](https://modrinth.com/mod/not-enough-animations) — Framework/settings.
- [ ] Audit 423: [Obscure API](https://modrinth.com/mod/obscure-api) — Framework/settings.
- [ ] Audit 425: [Oh The Biomes We've Gone](https://modrinth.com/mod/oh-the-biomes-weve-gone) — Framework/settings.
- [ ] Audit 426: [Only Excavators](https://modrinth.com/mod/only-excavators) — Framework/settings.
- [ ] Audit 431: [oωo (owo-lib)](https://modrinth.com/mod/owo-lib) — Framework/settings.
- [ ] Audit 432: [Particle Effects](https://modrinth.com/mod/particle-effects) — Framework/settings.
- [ ] Audit 435: [Paxi](https://modrinth.com/mod/paxi) — Framework/settings.
- [ ] Audit 439: [Platform](https://modrinth.com/mod/platform) — Framework/settings.
- [ ] Audit 446: Porting Lib Accessors — Framework/settings.
- [ ] Audit 447: Porting Lib Base — Framework/settings.
- [ ] Audit 448: Porting Lib Entity — Framework/settings.
- [ ] Audit 449: Porting Lib GameTest — Framework/settings.
- [ ] Audit 450: Porting Lib Gui Utils — Framework/settings.
- [ ] Audit 451: Porting Lib Items — Framework/settings.
- [ ] Audit 452: Porting Lib Models — Framework/settings.
- [ ] Audit 458: [Promenade](https://modrinth.com/mod/promenade) — Framework/settings.
- [ ] Audit 461: Prominent Settings — Framework/settings.
- [ ] Audit 463: [Pufferfish's Attributes](https://modrinth.com/mod/attributes) — Framework/settings.
- [ ] Audit 465: Puzzles Access Api — Framework/settings.
- [ ] Audit 466: Puzzles Api — Framework/settings.
- [ ] Audit 467: [Puzzles Lib](https://modrinth.com/mod/puzzles-lib) — Framework/settings.
- [ ] Audit 471: [Quests & Teams Fixes](https://modrinth.com/mod/quests-teams-fixes) — Framework/settings.
- [ ] Audit 476: Reach Entity Attributes — Framework/settings.
- [ ] Audit 477: Reach Entity Attributes — Framework/settings.
- [ ] Audit 480: [Rebind Narrator](https://modrinth.com/mod/rebind-narrator) — Framework/settings.
- [ ] Audit 485: [Reese's Sodium Options](https://modrinth.com/mod/reeses-sodium-options) — Framework/settings.
- [ ] Audit 486: [Regions Unexplored](https://modrinth.com/mod/regions-unexplored) — Framework/settings.
- [ ] Audit 489: [Repurposed Structures - Fabric](https://modrinth.com/mod/repurposed-structures-fabric) — Framework/settings.
- [ ] Audit 490: [Repurposed Structures - Neoforge/Forge](https://modrinth.com/mod/repurposed-structures-forge) — Framework/settings.
- [ ] Audit 491: [Resource Pack Overrides](https://modrinth.com/mod/resource-pack-overrides) — Framework/settings.
- [ ] Audit 492: [Resourceful Config](https://modrinth.com/mod/resourceful-config) — HUD/overlay.
- [ ] Audit 493: [Resourceful Lib](https://modrinth.com/mod/resourceful-lib) — Framework/settings.
- [ ] Audit 498: [RpgDifficulty](https://modrinth.com/mod/rpgdifficulty) — Framework/settings.
- [ ] Audit 500: [S-Lib](https://modrinth.com/mod/s-lib) — Framework/settings.
- [ ] Audit 502: [Sable Beyond](https://modrinth.com/mod/sable_beyond) — Framework/settings.
- [ ] Audit 503: SafLib — Framework/settings.
- [ ] Audit 504: [Satisfying Buttons](https://modrinth.com/mod/satisfying-buttons) — Framework/settings.
- [ ] Audit 507: [Seamless Loading Screen ](https://modrinth.com/mod/seamless-loading-screen) — Standalone screen.
- [ ] Audit 509: [Searchables](https://modrinth.com/mod/searchables) — Framework/settings.
- [ ] Audit 511: [Serene Seasons](https://modrinth.com/mod/serene-seasons) — Framework/settings.
- [ ] Audit 512: Server Browser — Standalone screen.
- [ ] Audit 514: [ShatterLib | OctoLib](https://modrinth.com/mod/shatterbyte-lib) — Framework/settings.
- [ ] Audit 516: [Shield Overhaul](https://modrinth.com/mod/shield-overhaul) — Framework/settings.
- [ ] Audit 519: [Simple Rich Discord Presence](https://modrinth.com/mod/srdp) — Framework/settings.
- [ ] Audit 525: [Simply Swords Tweaks](https://modrinth.com/mod/simply-swords-tweaks) — Framework/settings.
- [ ] Audit 530: [Smart Block Placement](https://modrinth.com/mod/smartblockplacement) — Framework/settings.
- [ ] Audit 533: [Smooth Scrolling Refurbished](https://modrinth.com/mod/smooth-scrolling-refurbished) — Framework/settings.
- [ ] Audit 536: [Snow Under Trees (Fabric)](https://modrinth.com/mod/snow-under-trees-remastered) — Framework/settings.
- [ ] Audit 537: [Snowy Spirit](https://modrinth.com/mod/snowy-spirit) — Framework/settings.
- [ ] Audit 539: [Sodium Dynamic Lights](https://modrinth.com/mod/sodium-dynamic-lights) — Framework/settings.
- [ ] Audit 542: [Sodium Options API](https://modrinth.com/mod/sodium-options-api) — Framework/settings.
- [ ] Audit 543: [Sodium Options Mod Compat](https://modrinth.com/mod/sodium-options-mod-compat) — Framework/settings.
- [ ] Audit 544: [SolLib](https://modrinth.com/mod/sollib) — Framework/settings.
- [ ] Audit 548: [Soulful Nether](https://modrinth.com/mod/soulfulnether) — Framework/settings.
- [ ] Audit 549: [Sound Physics Remastered](https://modrinth.com/mod/sound-physics-remastered) — Framework/settings.
- [ ] Audit 551: [spark](https://modrinth.com/mod/spark) — Framework/settings.
- [ ] Audit 553: SpectreLib — Framework/settings.
- [ ] Audit 555: [SpoiledZ](https://modrinth.com/mod/spoiledz) — Framework/settings.
- [ ] Audit 565: [Subtle Effects](https://modrinth.com/mod/subtle-effects) — Framework/settings.
- [ ] Audit 567: [SuperMartijn642's Config Lib](https://modrinth.com/mod/supermartijn642s-config-lib) — Framework/settings.
- [ ] Audit 570: [Surveyor Map Framework](https://modrinth.com/mod/surveyor) — Framework/settings.
- [ ] Audit 571: [Swampier Swamps](https://modrinth.com/mod/swampier-swamps) — Framework/settings.
- [ ] Audit 573: [TCDCommons API](https://modrinth.com/mod/tcdcommons) — Framework/settings.
- [ ] Audit 575: Terraform Wood API (v1) — Framework/settings.
- [ ] Audit 594: TRender — Standalone screen.
- [ ] Audit 595: [Underlay](https://modrinth.com/mod/underlay) — Framework/settings.
- [ ] Audit 598: [Unrestricted Enchanting](https://modrinth.com/mod/unrestricted-enchanting) — Framework/settings.
- [ ] Audit 599: [Updating World Icon](https://modrinth.com/mod/updating-world-icon) — Framework/settings.
- [ ] Audit 601: [Variant Crafting Tables](https://modrinth.com/mod/variant-crafting-tables) — Framework/settings.
- [ ] Audit 604: [Villager Names](https://modrinth.com/mod/villager-names-serilum) — Framework/settings.
- [ ] Audit 606: [VillagerTradeFix](https://modrinth.com/mod/villagertradefix) — Framework/settings.
- [ ] Audit 608: [Visuality](https://modrinth.com/mod/visuality) — Framework/settings.
- [ ] Audit 609: [Void Totem](https://modrinth.com/mod/voidtotem) — Framework/settings.
- [ ] Audit 610: [VoidZ](https://modrinth.com/mod/voidz) — Framework/settings.
- [ ] Audit 614: [Welcome Screen](https://modrinth.com/mod/welcome-screen) — Framework/settings.
- [ ] Audit 615: [WelcomeScreen](https://modrinth.com/mod/welcomescreen) — Standalone screen.
- [ ] Audit 617: [Winterly](https://modrinth.com/mod/winterly) — Framework/settings.
- [ ] Audit 618: [World Play Time](https://modrinth.com/mod/world-play-time) — Framework/settings.
- [ ] Audit 620: WunderLib — Framework/settings.
- [ ] Audit 624: XaeroLib — Framework/settings.
- [ ] Audit 627: [XP Storage](https://modrinth.com/mod/xp-storage) — Framework/settings.
- [ ] Audit 628: [XP Storage - Trinkets](https://modrinth.com/mod/xp-storage-trinkets) — Framework/settings.
- [ ] Audit 630: [Yeetus Experimentus](https://modrinth.com/mod/yeetus-experimentus) — Framework/settings.
- [ ] Audit 631: [YetAnotherConfigLib (YACL)](https://modrinth.com/mod/yacl) — Framework/settings.
- [ ] Audit 633: [YUNG's Better Desert Temples](https://modrinth.com/mod/yungs-better-desert-temples) — Framework/settings.
- [ ] Audit 634: [YUNG's Better Dungeons](https://modrinth.com/mod/yungs-better-dungeons) — Framework/settings.
- [ ] Audit 635: [YUNG's Better End Island](https://modrinth.com/mod/yungs-better-end-island) — Framework/settings.
- [ ] Audit 636: [YUNG's Better Jungle Temples](https://modrinth.com/mod/yungs-better-jungle-temples) — Framework/settings.
- [ ] Audit 637: [YUNG's Better Mineshafts](https://modrinth.com/mod/yungs-better-mineshafts) — Framework/settings.
- [ ] Audit 638: [YUNG's Better Nether Fortresses](https://modrinth.com/mod/yungs-better-nether-fortresses) — Framework/settings.
- [ ] Audit 639: [YUNG's Better Ocean Monuments](https://modrinth.com/mod/yungs-better-ocean-monuments) — Framework/settings.
- [ ] Audit 640: [YUNG's Better Strongholds](https://modrinth.com/mod/yungs-better-strongholds) — Framework/settings.
- [ ] Audit 641: [YUNG's Better Witch Huts](https://modrinth.com/mod/yungs-better-witch-huts) — Framework/settings.
- [ ] Audit 642: [YUNG's Cave Biomes](https://modrinth.com/mod/yungs-cave-biomes) — Framework/settings.
- [ ] Audit 643: [YUNG's Menu Tweaks](https://modrinth.com/mod/yungs-menu-tweaks) — Framework/settings.
- [ ] Audit 646: [Zeta](https://modrinth.com/mod/zeta) — Framework/settings.
- [ ] Audit 647: [Zume](https://modrinth.com/mod/zume) — Framework/settings.

## Validation performed for this update

- Reconciled all 647 original audit rows: **19 checked audit entries = 18 completed mod families** (eight first-batch families plus ten second-batch families; Farmer's Delight/Refabricated has two rows).
- Confirmed every second-batch family is represented in the release inventories and linked completed validation reports. Removed the obsolete 1.20.1-only status from all eleven corresponding audit entries.
- Confirmed the next ten recommendations are distinct original audit entries, remain unchecked and have concrete owned menu/screen evidence. Their full hook scope and exact version/loader availability must be checked during implementation.
- Reviewed upstream project information to inform this estimate. No mod code, build configuration or release JARs changed; no builds or gameplay tests ran for this tracker update.
