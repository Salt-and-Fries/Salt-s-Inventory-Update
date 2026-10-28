# Compatibility changelog

## 0.1.3 (in development)

- Fixes stretched and cropped Craft, Choose and preview-mode buttons with Chipped 4 on Minecraft 1.21.1, including their highlighted states.
- Extends the approved drawer, Graveyard, BetterEnd, Runes, Deep Aether Combiner, Chipped, Simply Swords, TieredZ, SmitherZ and AdditionZ integrations to each supported Minecraft version and loader with a matching upstream release. Preserves compact windows, connected tabs, complete previews and native workstation behavior.
- Keeps newer Ossuary recipe lists attached to their own desktop windows and preserves the configured socket count when opening SmitherZ's newer linking interface.
- Keeps newer BetterEnd smelters compact after their recipe-book button was removed upstream, and gives AdditionZ's newer Fletching Table its intended compact layout.

- Adds Simply Swords' Runic Forge on Minecraft 1.20.1 Fabric and Forge, with charcoal title/end bars and its native torn stone artwork. Preserves weapon, gem and tablet editing, preview tooltips, glowing channels and the original take-to-apply workflow.

- Adds Chipped's seven workbenches on Minecraft 1.20.1 Fabric and Forge, with a compact matching brown workshop, material picker, full variant browser, search, scrollbar and native preview modes. Craft and Craft All convert existing inventory stacks in place; changed materials and removed or out-of-range workbenches are handled safely.
- Keeps the complete Unlink Gear grindstone artwork and Upgrade Gear armor-stand preview visible on Minecraft 1.20.1. Selected anvil/reforge, smithing/linking and grindstone/unlinking tabs connect directly to their window frames, aligned to the left edge. Both grindstone views retain their full top outline.
- Adds compact Minecraft 1.20.1 desktop layouts for ExtendedDrawersAddon, BetterEnd, TieredZ, SmitherZ and AdditionZ on Fabric, The Graveyard containers and Runes on Fabric/Forge, and Deep Aether's Combiner on Forge.
- Keeps workstation tabs outside their frames, recipe-book toggles inside, and native recipes, animations, previews, upgrade controls and item rules intact. Gravestone editing stays native like editing a sign.

- Prevents newer Alloyed worlds from failing to load when its optional Create-based knife recipe is unavailable. The recipe remains available when Alloyed, Farmer's Delight and Create are installed.
- Preserves the standalone Alloyed forge's processing and Alloy Forgery's newer slot locks and live indicators in their desktop windows.
- Extends these native templates to every later supported target with a matching upstream release, including the standalone Alloyed 3.x forge. Parchment identifying tables, in-frame recipe buttons and compact native controls follow the installed mod release.
- Gives Better Archeology's identifying table a matching parchment frame and controls, while keeping its native artwork, slots and processing.
- Keeps the cooking and copper pot recipe buttons inside their widened frames.

Requires matching **Core 0.1.6** for the same Minecraft version and loader. Integrations remain optional.

Universal release notes: [Compatibility 0.1.3](../changelogs/changelog-compatibility-0.1.3.md).

- Adds compact Minecraft 1.20.1 desktop layouts for Bartering Station, Farmer's Delight / Refabricated, Better Archeology, Illager Invasion, Galosphere and Sawmill on Fabric and Forge, plus Alloy Forgery and Fabric Seasons: Extras on Fabric and Miner's Delight on Forge.
- Preserves native processing indicators, recipe selection, search, fuel controls and item rules in these layouts. Cooking recipe buttons sit inside widened pot windows; Alloy Forgery's lava reservoir remains outside the main frame.

- Extends the compact native-window integrations wherever upstream releases exist: Animal Feeding Trough and Nether Chested across all twelve targets; Brick & Mortar on 1.20.1 Fabric/Forge and 1.21.1 Fabric/NeoForge; Oxidized and Inmis on 1.20.1/1.21.1 Fabric; Expanded Delight on 1.20.1 Fabric and 1.21.1 NeoForge; Crabber's Delight on 1.20.1 Forge and 1.21.1 NeoForge. Iron Chests: Restocked remains available on 1.20.1 Fabric/Forge.
- Retains native processing, preview animations, widget interactions and slot permissions while fitting the desktop frame to the actual content.
- Preserves large-stack synchronization in newer Nether Chested menus.
- Identifies every Inmis backpack tier as storage, including the three-slot Baby Backpack.

- Polishes native container windows on Minecraft 1.20.1 using Core's public template API: Oxidized and Expanded Delight on Fabric, Brick & Mortar and Nether Chested on Fabric/Forge, Crabber's Delight on Forge, and Inmis on Fabric.
- Places Brick & Mortar's animated preview outside the kiln's top border. The compact kiln body stays in place as the preview opens; the complete group remains on screen, with all three hatch buttons and preview-item tooltips retained.
- Fits the Copper Kiln and Juicer frames to their actual artwork, removes excess right/bottom padding, and sizes every Inmis backpack to its real slot grid, including the three-slot Baby Backpack. Scrollbar space is used only when needed.
- Uses standard SIU/vanilla slot frames for Nether Chested while preserving its 54 slots and large-stack counts, and tightens the Crab Trap's bait/catch panel. Item rules, processing and synchronization stay in the original mods.

- Adds Animal Feeding Trough integration on Minecraft 1.20.1 Fabric and Forge. Its native inventory opens automatically as a compact desktop window with a centered slot and the usual sorting and transfer controls. Native feeding, item permissions, fill-level updates and crouch-empty-hand XP collection remain in the upstream mod.
- Fixes stale Animal Feeding Trough desktop sessions after the source is destroyed or replaced, and closes them outside the normal interaction range.
- Adds Iron Chests: Restocked integration on Minecraft 1.20.1 Fabric and Forge. All 15 chest and barrel variants open as desktop windows without the force-container setting.
- Enables in-container sorting and transfers between Iron Chests storage and other desktop inventories while preserving native item restrictions, including the dirt-only chest.
- Shows each tier's complete storage grid by default. Resized windows reflow their slots and provide scrolling when needed, including access to the last slot of 91-slot Netherite storage.

## 0.1.2

This release provides **Compatibility 0.1.2** for all twelve supported Minecraft/loader targets and requires the matching **Core 0.1.5**. Third-party targets remain optional and follow their upstream version/loader availability.

### New accessory and HUD integrations

- Adds **Trinkets 3.7.2 on 1.20.1 Fabric** and **Trinkets 3.10.0 on 1.21.1 Fabric**. Native accessory groups and multi-instance hover panels attach to Character, preserving native artwork, slot icons, tooltips, item rules, callbacks, quick transfers and equipment/capacity synchronization through Core's public API.
- Renders Trinkets panels above the player preview with separate background, panel and item passes. Inactive slots no longer overwrite expanded popup borders, and covered slots do not steal popup hits, including the offhand-ring overlap.
- Keeps Character's real offhand available whenever Trinkets is active, including when the HUD also shows it. Both views use the same real stack, so offhand-linked accessories remain accessible with either hotbar configuration.
- Adds **Curios on 1.20.1 Forge and 1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3 NeoForge**. Character's Curios button or the native inventory binding opens an equal-width accessory panel above Character, with a small gap and the cosmetic tab outside its edge.
- Moves the Curios panel and Character as one bounded group. Character moves down to make room, upward dragging stops at the group's top wall, all rows are shown when possible, and excessive slot capacity uses a scrollbar.
- Preserves each Curios release's native equipment/cosmetic slot classes, artwork, launcher/tab, visibility buttons, icons, tooltips, item permissions, equip callbacks and server synchronization. Legacy atlas and translucent cosmetic overlay corrections are included.
- Fixes the Curios shortcut inside an already-open desktop: one toggle per physical press, with text editing preserved. Keeps the complete rightmost visibility buttons visible and clickable in ordinary and scrolling layouts across all six Curios targets.
- Handles live Curios slot-capacity growth/shrink safely. Obsolete slot snapshots, broadcasts and queued clicks are guarded until the replacement session is ready, preserving the shared cursor stack.
- Adds optional **Raised 6.0.2** integration on all twelve targets. Detached hotbar/offhand geometry and interaction follow horizontal/vertical offsets, combined groups and live settings through Core's public HUD layout API. Native Raised selection behavior and window-hosted layouts are preserved.

### Existing integration fixes

- Fixes REI searches crashing on registry-backed tooltips, including researched Ancient Remnants Monolith Codices. From 1.21.1 onward, desktop entry copies receive the current world's registry context without hiding entries, replacing custom renderers or changing search syntax. The 1.20.1 path already uses player-backed tooltips.
- Fixes Tom's Storage desktop registration on 1.20.1 Forge when client startup has already fired. Both 1.20.1 loader builds include the shared initialization repair.
- Fixes AE2 1.20.1 Fabric registration in remapped release JARs by accepting both dotted and internal native target names.
- Restores the complete optional integration mixin list in the Forge 1.20.1 release manifest, including AE2. Build checks verify that manifest and mod metadata agree.
- Keeps Sophisticated client screen factories and legacy client packet handlers out of dedicated-server initialization across all eight applicable targets.
- Fixes Forge 1.20.1 Sophisticated production mappings for detached-menu synchronization, including both native method selectors and vanilla invocation mappings.
- Supports both older Pair and newer Tuple crafting-popup bounds in Sophisticated 26.1.2 NeoForge. Selects the hook from bytecode without initializing client classes on the server, preserving popup position and hit testing when its window moves.

### API, packaging and installation

- Rebuilds every artifact against Core API 6 and removes calls to deleted Minimize/Ghost Pins members. AE2 no longer renders removed Minimize/Restore controls or collapsed windows.
- Keeps all native integration code in this optional extension, using only Core's public API. Core and third-party mods are separate downloads and are not embedded in the extension.
- Retains the supported AE2, Tom's Simple Storage, Sophisticated Backpacks/Storage, JEI, REI and EMI implementations and existing native designs from 0.1.1. Core now owns the optional Fabric Mod Menu config entrypoint, so opening Core settings does not require this extension.
- Provides a shared artwork input at `artwork/shared-resources/compatibility` for extension-owned textures. Installed third-party mods continue to supply their own version-specific native artwork.
- Produces twelve independent remapped release JARs in `dist/compatibility`, with exact Core dependency pins, optional native dependency metadata, checksums and inclusion in the combined release bundle.

### Validation

Current-artifact checks passed across all twelve targets: 66 optional-integration/absence profiles, eight Trinkets/Curios profiles, twelve Tom's pickup profiles, twelve AE2 atlas/crafting-job profiles, twelve Raised profiles and eight Ancient Remnants browser-search profiles. Source/production-bytecode checks enforce API-only access; the availability audit covers 94 available and 38 explicitly unavailable integration cells. The final full-project build passed all 1,637 unit tests. See the [full validation report](../functional-tests/FULL-VALIDATION-20261023.md) for runtime lineage, selected screenshot reviews and remaining limits.

Install matching Core and Compatibility files for the same Minecraft version and loader, plus the third-party mods you use and their dependencies. Fabric requires the matching Fabric API. Native accessory integrations require the matching server-side mods as documented in their guides. **Controlify 0.1.1** remains optional.

Use this same changelog for every Compatibility 0.1.2 artifact. A release JAR does not imply that every upstream mod exists for that target.

Standalone release notes: [Compatibility 0.1.2](../changelogs/changelog-compatibility-0.1.2.md).

## 0.1.1

- Publishes the rebuilt compatibility coverage update under a new version for all twelve Minecraft/loader targets.
- Includes the Minecraft 1.20.1 EMI integrations, maintained Sophisticated Fabric ports on 1.20.1 and 1.21.1, repaired legacy Tom's Storage integration, native Sophisticated opening-data fixes, and Tom's Storage window bounds and text visibility fixes.
- Requires Core 0.1.4.1 on Minecraft 1.20.1 Fabric and Forge, and Core 0.1.4 on the other supported targets. Third-party integration targets remain optional; Fabric also requires Fabric API.
- Keeps the same validated integration implementations as the rebuilt 0.1.0 artifacts; this publication update changes release numbers and dependency metadata.

## 0.1.0

- Packages third-party inventory and recipe-browser integrations as an optional companion to Salt's Inventory Update Core 0.1.4.
- Integrates supported releases of Applied Energistics 2, Tom's Simple Storage, Sophisticated Backpacks and Storage, JEI, REI, EMI and Fabric Mod Menu through the public Core API.
- Adds EMI support for Minecraft 1.20.1 on Fabric and Forge, alongside the existing 1.21.1 Fabric and NeoForge adapters.
- Adds Sophisticated's maintained Fabric ports on Minecraft 1.20.1 and 1.21.1, including native slot behavior, upgrade tabs, settings navigation, menu opening data and synchronization.
- Checks remapped Fabric slot-rendering hooks so the legacy native override remains active in production JARs.
- Repairs Tom's Storage 1.7.x support on Minecraft 1.20.1, including its native registry IDs, terminal actions and settings, seven available menus, filtered connectors, emitter and link state, recipe book and legacy texture atlases.
- Preserves the full native Sophisticated Storage opening payload, including newer linked-storage state, while deriving source identity from the authenticated server menu.
- Keeps emitter controls, inventory-link lists and tag-filter panels within their window frames, including minimum bounds for restored windows, across every target.
- Uses explicit opaque text colors for Tom's Storage labels, counts and controls so newer renderers do not make them transparent.
- Audits upstream availability across all twelve Minecraft/loader targets and checks that eligible adapters are present in source and production artifacts.
- Keeps third-party integrations optional and the Core framework independent.

Use the rebuilt Core 0.1.4 and Compatibility 0.1.0 pair for the same Minecraft version and loader; the 1.20.1 EMI adapter uses the added public Core rendering hooks. Fabric also requires Fabric API. Install third-party mods and their required dependencies separately. Availability follows the upstream release matrix; an extension JAR does not imply that upstream mods exist for every loader and Minecraft version.
