# Salt's Inventory Update 0.1.1 Changelog

Drag preview parity, result-slot shift-click fixes, standalone inventory visual syncing, debugging controls, forced container routing, and optional JEI desktop compatibility.

This release improves desktop inventory dragging so Salt windows show vanilla-style placement previews while the mouse is still held down, fixes result-slot shift-click behavior for crafting-style containers such as the stonecutter, fixes stale standalone inventory visuals after desktop item moves, adds new config tools for debugging and forcing containers into Salt desktop windows, and adds a full optional JEI compatibility window built for Salt's desktop UI.

## Supported Minecraft Versions And Loaders

- Added the drag-preview update for Minecraft 26.2 on Fabric and NeoForge.
- Ported the same drag-preview behavior to Minecraft 26.1.2 on Fabric and NeoForge.
- Ported the same drag-preview behavior to Minecraft 1.21.11 on Fabric and NeoForge.
- Ported the same drag-preview behavior to Minecraft 1.21.1 on Fabric and NeoForge.
- Ported the same drag-preview behavior to Minecraft 1.20.1 on Fabric and Forge.
- Ported the standalone player-inventory visual sync fix across every supported version and loader.
- Added optional JEI window compatibility for Minecraft 26.2 on Fabric and NeoForge.
- Ported the JEI window compatibility pass to Minecraft 26.1.2 on Fabric and NeoForge.
- Ported the JEI window compatibility pass to Minecraft 1.21.11 on Fabric and NeoForge.
- Ported the JEI window compatibility pass to Minecraft 1.21.1 on Fabric and NeoForge.
- Ported the JEI window compatibility pass to Minecraft 1.20.1 on Fabric and Forge.
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

## Optional JEI Desktop Window

- Added optional Just Enough Items compatibility that only activates when JEI is installed and its runtime is available.
- Added a new `H` keybind, `key.salts_inventory_update.jei_window`, that opens or toggles Salt's JEI window.
- Added a standalone Salt desktop JEI window that opens at the top-right by default while preserving saved window positions.
- Kept the JEI window width consistent between the ingredient list and recipe or uses views.
- Reserved top-tab space for the JEI window so moving and reopening the window does not jump between modes.
- Added a compact ingredient browser with a JEI-backed search box, scrollable item grid, and JEI-rendered ingredient icons and tooltips.
- Moved ingredient type tabs above the window using JEI-style icon tabs instead of large text buttons.
- Added Favorites and Recent tabs backed by JEI's own bookmark and lookup history data.
- Added a default-tab toggle beside the JEI title so players can choose which tab opens first.
- Kept JEI imports isolated to `compat.jei`; core Salt desktop code talks to a neutral bridge and stays safe when JEI is absent.

## JEI Recipe And Uses Views

- Changed left-clicking a JEI ingredient to open recipes inside the same Salt JEI window instead of opening JEI's native screen.
- Changed right-clicking a JEI ingredient to open uses inside the same Salt JEI window.
- Added JEI recipe category tabs above the window using JEI category icons and JEI tab textures.
- Added top-tab paging buttons when recipe categories exceed the visible tab row.
- Added recipe and uses layouts rendered through JEI recipe layout drawables, including recipe overlays and JEI tooltips.
- Added a scrollable recipe list that fits as many recipe previews as the window can show instead of only one large preview.
- Fixed scrolling and scrollbar dragging for both Salt's recipe-list scrollbar and JEI's internal recipe layout scrollbars.
- Added left-side crafting station and catalyst tabs as one connected side tab that starts at the top of the window.
- Added close spacing for crafting station icons so station entries visually match JEI's original recipe screen.
- Added bottom-left recipe sort controls for JEI's bookmarked-recipes-first and craftable-recipes-first behavior.
- Added per-recipe bookmark buttons beside recipe previews, positioned to avoid recipe scrollbars.
- Kept JEI texture assets referenced from the installed JEI namespace at runtime instead of copying JEI textures into Salt.

## JEI Navigation And Shortcuts

- Added JEI-style recipe history inside Salt's JEI window.
- Added a back button above the recipe tabs that returns through previously viewed JEI recipe or uses pages.
- Added Shift-back behavior to move forward through JEI history after going back.
- Added Ctrl-back behavior to return directly to the main JEI ingredient list.
- Changed Escape behavior so pressing Esc closes all Salt windows, including the JEI window, from any JEI mode.
- Added JEI `R` and `U` lookup support while hovering item stacks, opening Salt's JEI recipe or uses window automatically.
- Added recipe drill-down from ingredients hovered inside JEI recipe layouts, with left-click opening recipes and right-click opening uses.
- Fixed stale ingredient-list hover tooltips from appearing while the JEI window is in a recipe or uses view.

## JEI Move Items

- Added a JEI-style Move Items button to qualifying recipe previews when a compatible Salt crafting/container window is open.
- Added server-authoritative JEI transfer packets that validate target sessions, recipe slots, ingredient alternatives, carried stack state, visibility, spectator state, and menu validity before moving anything.
- Added transfer planning through JEI recipe transfer data where available, with safe slot-based fallbacks for normal crafting grids.
- Added transfer sourcing from the player inventory plus all visible open Salt container windows.
- Added target prioritization that prefers the focused compatible crafting/container window, then the newest compatible open window.
- Added missing-ingredient tooltips and red missing-slot highlights when the transfer target exists but the required ingredients are unavailable.
- Added repeated-click stacking so clicking Move Items again adds another recipe set onto the same matching ingredients already in the crafting grid.
- Added Shift-click max transfer behavior for moving as many recipe sets as the available ingredients and target slot limits allow.
- Added safe target-grid clearing simulation so existing grid contents are only moved aside if there is room and the full recipe transfer can still succeed.
- Rejected unsupported opaque JEI transfer handlers instead of showing a broken transfer button.
- Preserved 1.20.1 behavior without Crafter-specific fallbacks because Minecraft 1.20.1 does not include the Crafter menu.

## Compatibility And Safety

- Preserved existing single-session vanilla `QUICK_CRAFT` release behavior.
- Preserved existing cross-window and cross-session manual release-time fallback behavior.
- Preserved optional JEI compatibility so Salt launches and existing inventory windows continue working when JEI is not installed.
- Avoided per-hover server clicks and public API changes.
- Adapted preview calculation to older Minecraft versions that use the set-based vanilla quick-craft placement helper.
- Adapted JEI packet encoding, ingredient comparison, mouse scrolling, resource identifiers, and transfer helpers across older Minecraft and JEI API versions.
- Kept player-menu slot syncing scoped to desktop action broadcasts instead of adding per-frame inventory synchronization.
- Kept non-JEI code free of direct `mezz.jei.*` references.
- Verified Fabric plus Forge/NeoForge build coverage across the full supported version matrix.
