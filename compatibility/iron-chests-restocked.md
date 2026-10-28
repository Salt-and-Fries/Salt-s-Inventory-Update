# Iron Chests: Restocked

The Compatibility extension supports Iron Chests: Restocked on Minecraft 1.20.1 Fabric and Forge, using upstream 5.0.2 as the reviewed baseline. Install the matching Core and Compatibility JARs on the server and client, plus Iron Chests and its own dependencies. Iron Chests remains optional: these providers are resolved only when `ironchests` is installed.

All 15 standard inventories are supported: Dirt Chest, plus Copper, Iron, Gold, Diamond, Crystal, Obsidian and Netherite chests and barrels. Normal opening uses a desktop window; the force-container option is unnecessary. An incompatible native slot layout falls back to its native screen.

Each tier initially displays its full grid. Resizing reflows slots without changing their menu IDs. Smaller views provide wheel and draggable scrollbar access to every slot, with controller scroll-region metadata. Local scroll state is clamped when geometry changes. Titles, slot artwork, tooltips, shared-cursor gestures, window management, in-container sorting and cross-inventory sorting/transfers use Core's existing services.

## API boundary

Implementation lives in `versions/1.20.1/fabric/src/main/java/com/salts_inventory_update/compat/ironchests`; the Forge build also compiles this shared version source, following the project's existing convention.

- `DesktopEnvironment` gates optional loading, server-start registration and atomic registration scopes.
- `DesktopServerApi.registerWindow` declares eligibility after checking the reviewed native inventory layout.
- `DesktopClientApi.registerWindow` advertises client support and supplies the storage view.
- `DesktopMenuBindings.registerStorage` declares only the native storage slots eligible for sorting/transfers. Player inventory slots are excluded.
- `DesktopWindowDefinition`, slot/render/input contexts and `DesktopWidgets` supply geometry, slot hit testing, artwork and scrolling.

No Core API additions, Core implementation imports, access wideners or mixins are needed. No copy of Iron Chests or Core is bundled. Native menu factories and native Slot instances retain item predicates, synchronized contents and native menu validity/close behavior. Native block opening remains responsible for lock and obstruction checks.

## Validation

Focused checks executed against the production Compatibility JARs and Core 0.1.5:

- Fabric and Forge: all 15 variants opened without forcing; pickup, return, splitting, final-slot access, sorting/stack merging, quick transfer and reopen matched authoritative server contents.
- Both loaders: dirt-only rejection from the real player window, three-slot drag distribution, resized 91-slot scrolling, transfer into player Inventory, expansion after scrolling and GUI scale 3 bounds.
- Both loaders: source removal, native locked/unlocked and obstructed opening, distance invalidation, and an ordinary vanilla-chest regression check.
- Forge Computer Use: physically dragged the scrollbar to the bottom and transferred all 16 gold from slot 90 into player Inventory, with matching server state and empty cursors.
- Forge native Iron Dolly: viewer count blocks pickup while a desktop is open; closing releases the viewer. Picking up, placing and reopening the closed chest preserved both stored stacks.
- Fabric: Core plus Compatibility started and operated a vanilla chest with Iron Chests absent.
- Three geometry regression tests passed. Source/API boundaries, optional metadata and both production JAR contents passed inspection.

These are focused integration checks, not a complete functional suite. Dedicated multiplayer, every lock/key-ring and dolly combination, custom upstream data packs, and physical controller navigation were not exhausted. Controller scroll metadata is provided, but no controller gameplay pass is claimed.

Detailed observations, screenshots and fixture corrections are saved under `build/iron-chests-compatibility/`. Validation used isolated disposable worlds; fixture setup artifacts are excluded from product findings.
