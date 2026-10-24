# Salt's Inventory Compatibility 0.1.2

This release provides **Compatibility 0.1.2** for all twelve supported Minecraft/loader targets and requires the matching **Core 0.1.5**. Third-party targets remain optional and follow their upstream version/loader availability.

## New accessory and HUD integrations

- Adds **Trinkets 3.7.2 on 1.20.1 Fabric** and **Trinkets 3.10.0 on 1.21.1 Fabric**. Native accessory groups and multi-instance hover panels attach to Character, preserving native artwork, slot icons, tooltips, item rules, callbacks, quick transfers and equipment/capacity synchronization through Core's public API.
- Renders Trinkets panels above the player preview with separate background, panel and item passes. Inactive slots no longer overwrite expanded popup borders, and covered slots do not steal popup hits, including the offhand-ring overlap.
- Keeps Character's real offhand available whenever Trinkets is active, including when the HUD also shows it. Both views use the same real stack, so offhand-linked accessories remain accessible with either hotbar configuration.
- Adds **Curios on 1.20.1 Forge and 1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3 NeoForge**. Character's Curios button or the native inventory binding opens an equal-width accessory panel above Character, with a small gap and the cosmetic tab outside its edge.
- Moves the Curios panel and Character as one bounded group. Character moves down to make room, upward dragging stops at the group's top wall, all rows are shown when possible, and excessive slot capacity uses a scrollbar.
- Preserves each Curios release's native equipment/cosmetic slot classes, artwork, launcher/tab, visibility buttons, icons, tooltips, item permissions, equip callbacks and server synchronization. Legacy atlas and translucent cosmetic overlay corrections are included.
- Fixes the Curios shortcut inside an already-open desktop: one toggle per physical press, with text editing preserved. Keeps the complete rightmost visibility buttons visible and clickable in ordinary and scrolling layouts across all six Curios targets.
- Handles live Curios slot-capacity growth/shrink safely. Obsolete slot snapshots, broadcasts and queued clicks are guarded until the replacement session is ready, preserving the shared cursor stack.
- Adds optional **Raised 6.0.2** integration on all twelve targets. Detached hotbar/offhand geometry and interaction follow horizontal/vertical offsets, combined groups and live settings through Core's public HUD layout API. Native Raised selection behavior and window-hosted layouts are preserved.

## Existing integration fixes

- Fixes REI searches crashing on registry-backed tooltips, including researched Ancient Remnants Monolith Codices. From 1.21.1 onward, desktop entry copies receive the current world's registry context without hiding entries, replacing custom renderers or changing search syntax. The 1.20.1 path already uses player-backed tooltips.
- Fixes Tom's Storage desktop registration on 1.20.1 Forge when client startup has already fired. Both 1.20.1 loader builds include the shared initialization repair.
- Fixes AE2 1.20.1 Fabric registration in remapped release JARs by accepting both dotted and internal native target names.
- Restores the complete optional integration mixin list in the Forge 1.20.1 release manifest, including AE2. Build checks verify that manifest and mod metadata agree.
- Keeps Sophisticated client screen factories and legacy client packet handlers out of dedicated-server initialization across all eight applicable targets.
- Fixes Forge 1.20.1 Sophisticated production mappings for detached-menu synchronization, including both native method selectors and vanilla invocation mappings.
- Supports both older Pair and newer Tuple crafting-popup bounds in Sophisticated 26.1.2 NeoForge. Selects the hook from bytecode without initializing client classes on the server, preserving popup position and hit testing when its window moves.

## API, packaging and installation

- Rebuilds every artifact against Core API 6 and removes calls to deleted Minimize/Ghost Pins members. AE2 no longer renders removed Minimize/Restore controls or collapsed windows.
- Keeps all native integration code in this optional extension, using only Core's public API. Core and third-party mods are separate downloads and are not embedded in the extension.
- Retains the supported AE2, Tom's Simple Storage, Sophisticated Backpacks/Storage, JEI, REI and EMI implementations and existing native designs from 0.1.1. Core now owns the optional Fabric Mod Menu config entrypoint, so opening Core settings does not require this extension.
- Provides a shared artwork input at `artwork/shared-resources/compatibility` for extension-owned textures. Installed third-party mods continue to supply their own version-specific native artwork.
- Produces twelve independent remapped release JARs in `dist/compatibility`, with exact Core dependency pins, optional native dependency metadata, checksums and inclusion in the combined release bundle.

## Validation

Current-artifact checks passed across all twelve targets: 66 optional-integration/absence profiles, eight Trinkets/Curios profiles, twelve Tom's pickup profiles, twelve AE2 atlas/crafting-job profiles, twelve Raised profiles and eight Ancient Remnants browser-search profiles. Source/production-bytecode checks enforce API-only access; the availability audit covers 94 available and 38 explicitly unavailable integration cells. The final full-project build passed all 1,637 unit tests. See the [full validation report](../functional-tests/FULL-VALIDATION-20261023.md) for runtime lineage, selected screenshot reviews and remaining limits.

Install matching Core and Compatibility files for the same Minecraft version and loader, plus the third-party mods you use and their dependencies. Fabric requires the matching Fabric API. Native accessory integrations require the matching server-side mods as documented in their guides. **Controlify 0.1.1** remains optional.

Use this same changelog for every Compatibility 0.1.2 artifact. A release JAR does not imply that every upstream mod exists for that target.
