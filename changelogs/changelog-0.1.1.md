# Salt's Inventory Update 0.1.1 Changelog

Drag preview parity, result-slot shift-click fixes, standalone inventory visual syncing, and debugging controls.

This release improves desktop inventory dragging so Salt windows show vanilla-style placement previews while the mouse is still held down, fixes result-slot shift-click behavior for crafting-style containers such as the stonecutter, fixes stale standalone inventory visuals after desktop item moves, and adds new config tools for debugging and forcing containers into Salt desktop windows.

## Supported Minecraft Versions And Loaders

- Added the drag-preview update for Minecraft 26.2 on Fabric and NeoForge.
- Ported the same drag-preview behavior to Minecraft 26.1.2 on Fabric and NeoForge.
- Ported the same drag-preview behavior to Minecraft 1.21.11 on Fabric and NeoForge.
- Ported the same drag-preview behavior to Minecraft 1.21.1 on Fabric and NeoForge.
- Ported the same drag-preview behavior to Minecraft 1.20.1 on Fabric and Forge.
- Ported the standalone player-inventory visual sync fix across every supported version and loader.
- Kept loader behavior shared through the existing versioned Fabric desktop screen sources and Forge/NeoForge shims.

## Item Dragging

- Added vanilla-style render-time slot previews while drag-distributing a carried stack across multiple slots.
- Added preview stack counts that match the calculated quick-craft placement result before the mouse button is released.
- Added carried-stack remainder previews while dragging, including a yellow zero-count overlay when the carried stack is fully allocated by the preview.
- Added capped-count preview text when a target slot is limited by slot capacity or max stack size.
- Added vanilla-style preview filtering for inactive, invalid, incompatible, full, fake, and non-draggable slots.
- Kept actual item placement committed on mouse release, matching vanilla quick-craft timing.

## Hotbar, Offhand, And API Slots

- Added drag previews to the interactive desktop hotbar slots.
- Added drag previews to the interactive desktop offhand slot.
- Added drag previews to API-backed textureless slots so add-on windows render the same placement feedback as built-in windows.
- Kept hotbar and offhand hover rendering while allowing every eligible dragged-over slot to preview at the same time.

## Result Slot Shift-Clicking

- Fixed shift-clicking the stonecutter result slot so it crafts and transfers as many items as vanilla can produce from the current input, instead of only crafting one item.
- Extended the same vanilla result-slot quick-move path to other result containers, including crafting, furnaces, anvils, cartography tables, grindstones, merchants, smithing tables, and stonecutters.
- Preserved the shared carried-stack restore behavior around result-slot quick moves so desktop sessions stay in sync after bulk crafting.
- Kept normal non-result slot quick-move routing unchanged.

## Standalone Inventory Visual Sync

- Fixed standalone player inventory windows visually freezing after desktop item moves while the underlying inventory continued to work.
- Added client handling for `PLAYER_MENU_SESSION` slot updates so player-inventory slot packets update the local player menu instead of being dropped as missing desktop sessions.
- Added server-side player-menu slot snapshots during desktop broadcasts so standalone inventory slot visuals refresh after clicks, quick moves, recipe actions, and other desktop inventory operations.
- Preserved carried-stack synchronization while making source and destination slot visuals match the server-confirmed player inventory state.
- Kept the fix shared across Fabric, Forge, and NeoForge builds through the existing versioned desktop source and loader shim structure.

## Config And Diagnostics

- Added an `Enable Detailed Console Logs` config option for pack testing and bug capture.
- Added detailed desktop logs for container capture, forced container routing, container window rendering, API/custom window rendering, hovered slot rendering, textureless API slot rendering, and packet/session flow already covered by desktop trace logging.
- Wired detailed logging through the runtime config state so it can be toggled from the in-game config screen instead of requiring JVM flags.
- Added localization entries for the new config controls in every supported version.

## Forced Container Windows

- Added a `Force Containers as Windows` config submenu.
- Added a dynamic registry-driven list of every known menu container loaded by the game, including vanilla and modded menu types.
- Added per-container checkboxes that force selected menu types through Salt's desktop window path.
- Added client screen registration syncing when forced containers are toggled, cleared, config is reloaded, or config defaults are restored.
- Added server-side forced container capture so selected modded or modified vanilla containers can create Salt desktop sessions.
- Preserved vanilla or mod-provided screens for unchecked unsupported containers.

## Compatibility And Safety

- Preserved existing single-session vanilla `QUICK_CRAFT` release behavior.
- Preserved existing cross-window and cross-session manual release-time fallback behavior.
- Avoided per-hover server clicks, packet changes, server logic changes, and public API changes.
- Adapted preview calculation to older Minecraft versions that use the set-based vanilla quick-craft placement helper.
- Kept player-menu slot syncing scoped to desktop action broadcasts instead of adding per-frame inventory synchronization.
- Kept all changes client-rendering/config/session-routing scoped, with no new public API surface.
- Verified Fabric plus Forge/NeoForge compile coverage across the full supported version matrix.
