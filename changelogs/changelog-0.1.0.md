# Salt's Inventory Update 1.0 Changelog

Initial public release.

Salt's Inventory Update rebuilds Minecraft inventory interaction around a desktop-style UI. The release adds movable inventory/container windows, a standalone player inventory, an optional expandable inventory system, interactive hotbar and offhand slots, a character/status/crafting window, a desktop API for integrations, and Tom's Simple Storage compatibility.

## Supported Minecraft Versions And Loaders

- Added support for Minecraft 26.2 on Fabric and NeoForge.
- Added support for Minecraft 26.1.2 on Fabric and NeoForge.
- Added support for Minecraft 1.21.11 on Fabric and NeoForge.
- Added support for Minecraft 1.21.1 on Fabric and NeoForge.
- Added support for Minecraft 1.20.1 on Fabric and Forge.
- Added shared multi-version project support so the same gameplay systems can be built across the full version/loader matrix.
- Added Fabric Mod Menu integration on Fabric builds.
- Added Forge and NeoForge loader shims for entrypoints, events, key bindings, commands, and networking.

## Major Gameplay Additions

- Added the Salt Inventory Desktop, a new inventory surface that can hold multiple independent windows at the same time.
- Added desktop-style windows for player inventory, creative inventory, character/status/crafting, vanilla containers, supported mount inventories, and supported modded containers.
- Added movable windows with draggable title bars when unlocked.
- Added close, minimize, focus, pin, lock, and compact overflow controls to window title bars.
- Added adaptive title-bar controls: wide windows show all controls, while narrow windows collapse secondary controls into an ellipsis popup.
- Added one active focused window at a time.
- Added focused-window quick-move routing so shift-clicked items can move into the focused container when possible.
- Added window stacking and bring-to-front behavior when a window is clicked.
- Added saved per-world/per-server window state for position, size, lock state, pin mode, and API-provided local state.
- Added pinned windows that reopen at their saved positions instead of default placement.
- Added optional ghost-pinned windows that turn into translucent passive previews when closed.
- Added ghost-window promotion: clicking a ghost preview restores the interactive window.
- Added ghost-window persistence over the normal game HUD when no desktop screen is open.
- Added server-side hidden ghost sessions that can keep validating and syncing while hidden.
- Added dormant ghost-source recovery for reachable block-backed ghost sessions.
- Added configurable ghost-window opacity.
- Added window locking so windows can be protected from accidental movement and resizing.
- Added optional geometry reset when a locked window is locked again.
- Added configurable default for whether new windows open locked or unlocked.
- Added configurable window placement styles: Top Outside, Around the Inventory, and Vintage Story.
- Added optional resizing for supported storage windows and API windows.
- Added storage resizing that keeps slot size unchanged and changes how many slots are visible.
- Added scrollbars to storage windows when their contents do not fit.
- Added optional resize snapping so resizable windows trim extra empty space to clean slot-grid dimensions.
- Added default fixed sizing for functional stations whose layouts should not be compressed.
- Added dense-grid detection so regular unknown storage grids can become resizable when safe.
- Added a darkened inventory overlay while the desktop UI is active.
- Added active in-world movement while inventory windows are open.
- Added WASD, jump, sneak, and sprint key syncing while the desktop is open, except when text input is active.
- Added Alt camera control mode while inventory windows are open.
- Added hotbar-only mouse mode by holding Alt with no inventory windows open.
- Added cursor-based world targeting while the mouse is available over the desktop.
- Added world attack/use support outside interactive UI areas while windows are open.
- Added crosshair hiding while the desktop owns mouse control.
- Added hotbar scrolling while camera control is active.
- Added E press/release inventory toggling through the desktop controller.
- Added hold-E close-all behavior for Salt windows.
- Added a close-all progress overlay.
- Added configurable E hold duration from 0.5 to 10 seconds.
- Added safe close behavior that avoids dropping the desktop while a carried stack is active.

## Changed From Vanilla Minecraft

- Replaced the single vanilla inventory screen with separate desktop windows.
- Changed the player inventory so it is no longer duplicated underneath every opened container.
- Changed container opening so supported containers open as their own focused windows instead of taking over the whole screen.
- Changed the hotbar so the actual HUD hotbar is the interactive inventory object instead of a duplicate row inside the inventory screen.
- Changed the offhand slot so it is rendered and interacted with beside the HUD hotbar in the desktop workflow.
- Changed the player inventory window to show main inventory storage only, without the hotbar row, armor slots, player model, or 2x2 crafting grid.
- Moved armor slots, player model preview, status readouts, potion effects, and 2x2 crafting into a separate Character Window.
- Changed creative mode inventory into a desktop creative window.
- Changed the vanilla creative screen replacement path so opening creative inventory while Salt is enabled shows the desktop creative window.
- Changed recipe placement so recipe-book actions are routed into the correct desktop container session.
- Changed merchant offer syncing so desktop merchant windows receive offer lists, villager level, XP, progress, and restock state without needing the vanilla merchant screen.
- Changed chest opener ownership checks so open desktop chest sessions still count as owning the chest container.
- Changed player inventory persistence to include optional Salt extra inventory slots.
- Changed player inventory queries so item searches, tag checks, predicate checks, counting, and clearing can include Salt extra inventory contents when enabled.
- Changed player inventory overflow insertion so items can spill into Salt extra slots after vanilla inventory insertion fails.
- Changed inventory drop/clear behavior so Salt extra slots drop or clear with the rest of the player inventory.
- Changed death restore behavior so Salt extra slots follow keepInventory/spectator rules.
- Changed unsupported modded container behavior to fall back to vanilla or the mod's own screen instead of forcing an unsafe desktop window.
- Changed remote-server behavior so Salt desktop gameplay disables itself when the server does not expose Salt desktop networking.

## Player Inventory Window

- Added a standalone player inventory window opened with the inventory key.
- Added a 9-column storage layout for the main inventory.
- Added a default three-row visible layout for normal inventory contents.
- Added automatic default window height growth up to eight visible rows as extra storage grows.
- Added scrollable inventory storage for contents beyond the visible rows.
- Added slot rendering and hit detection for normal main inventory slots and Salt extra inventory slots.
- Added direct item pickup, placement, splitting, and swapping inside the desktop inventory window.
- Added drag distribution across slots.
- Added vanilla-style pickup-all behavior on double click.
- Added shift-click movement out of the player inventory.
- Added shift-double-click movement of matching stacks.
- Added number-key hotbar swaps from desktop inventory slots.
- Added offhand swap support from desktop inventory slots.
- Added drop-key support from hovered desktop slots.
- Added bundle item scroll selection support while hovering bundle stacks.
- Added shared carried-stack state across all Salt windows.

## Optional Expandable Inventory

- Added optional expandable player inventory slots.
- Added a config toggle for showing the expandable inventory add-slot button.
- Added an add-slot button in the player inventory window when expandable inventory is enabled.
- Added the add-slot button to the creative inventory tab when expandable inventory is enabled.
- Added XP-level pricing for new slots: the next slot costs the current extra-slot count plus one level.
- Added server-side slot purchase validation.
- Added visual dimming when the player cannot afford the next extra slot.
- Added add-slot tooltips showing the action, cost, and the player's current level.
- Added a hard cap of 4096 extra slots per player.
- Added persistent player save data for extra slot count and extra inventory item stacks.
- Added client/server synchronization for extra slot count and extra slot contents.
- Added menu-slot injection so extra inventory slots become real player inventory menu slots.
- Added packet guards so the client can expand its player menu before reading expanded inventory packets.
- Added extra inventory snapshot loading, copying, resizing, clearing, dropping, and insertion behavior.
- Added support for extra inventory contents in inventory search, count, and clear operations.
- Added sorting of player storage targets so vanilla main inventory slots and extra slots have stable storage order.
- Added keepInventory-compatible death transfer for extra slots.
- Added spectator-compatible restore behavior for extra slots.
- Left expandable inventory disabled by default so public players opt into extra slots intentionally.

## Hotbar And Offhand

- Added interactive HUD hotbar slots during Salt inventory interaction.
- Added interactive offhand slot rendering beside the hotbar.
- Added hover highlights for HUD hotbar and offhand slots.
- Added item pickup, placement, and swapping between windows and the real hotbar.
- Added number-key hotbar selection when no slot consumes the key.
- Added number-key swapping from hovered slots into hotbar slots.
- Added offhand key swapping from hovered slots.
- Added mouse-wheel hotbar selection while camera control is active.
- Added hotbar-only interaction mode when Alt is held without open windows.
- Added shift-click routing from containers to the hotbar when a container is open without a visible player inventory window.

## Character Window

- Added a Character Window opened with the C key.
- Added armor slot interaction in the Character Window.
- Added player model preview that follows the mouse.
- Added health display.
- Added hunger display.
- Added XP level display.
- Added active potion-effect display with icons and durations.
- Added overflow indicator for additional active effects.
- Added 2x2 inventory crafting slots.
- Added 2x2 crafting result slot.
- Added recipe-book access for character crafting.
- Moved vanilla inventory equipment/status/crafting functions out of the main inventory window into this dedicated window.

## Creative Inventory Window

- Added a desktop creative inventory window for creative players.
- Added creative tab rendering with top and bottom tab rows.
- Added creative tab paging when there are more tabs than fit.
- Added creative catalog grid with scroll support.
- Added creative inventory tab support.
- Added creative search box support.
- Added search text editing, clearing, and chat-key search activation.
- Added remembered creative tab, search text, and scroll position while the desktop remains active.
- Added creative delete slot support.
- Added creative inventory clear behavior through the delete slot.
- Added creative item pickup from the catalog.
- Added shift-pickup stack sizing for catalog picks.
- Added creative drop handling for catalog and carried stacks.
- Added pick-block clone behavior for creative players.
- Added hotbar/offhand placement from catalog items.
- Added saved hotbar load/save key handling.
- Added creative carried-stack synchronization for creative players.
- Added expandable inventory add-slot support inside the creative inventory tab.

## Vanilla Container Windows

- Added desktop capture for generic 9x1 storage containers.
- Added desktop capture for generic 9x2 storage containers.
- Added desktop capture for generic 9x3 storage containers.
- Added desktop capture for generic 9x4 storage containers.
- Added desktop capture for generic 9x5 storage containers.
- Added desktop capture for generic 9x6 storage containers.
- Added desktop capture for generic 3x3 containers.
- Added desktop capture for shulker boxes.
- Added desktop capture for hoppers.
- Added compact storage-window rendering for known storage menus.
- Added resizable storage support for known storage menus when resizing is enabled.
- Added desktop capture for furnaces.
- Added desktop capture for blast furnaces.
- Added desktop capture for smokers.
- Added compact furnace-style rendering with input, fuel, result, burn indicator, progress arrow, and recipe-book button.
- Added desktop capture for crafting tables.
- Added crafting table rendering with 3x3 grid, result slot, progress arrow art, recipe-book button, and recipe placement routing.
- Added desktop capture for anvils.
- Added anvil rendering with input slots, result slot, text field, rename submission, cost display, and error display.
- Added desktop capture for smithing tables.
- Added smithing rendering with template, base, addition, result, upgrade label, hammer art, and equipment preview.
- Added desktop capture for grindstones.
- Added grindstone rendering with input slots, result slot, grindstone art, and result arrow.
- Added desktop capture for stonecutters.
- Added stonecutter rendering with input/result slots, recipe grid, selected recipe state, recipe scrollbar, and recipe click handling.
- Added desktop capture for looms.
- Added loom rendering with banner, dye, pattern, result slots, pattern grid, pattern scrollbar, and banner preview.
- Added desktop capture for crafters.
- Added crafter rendering with 3x3 slot grid, enabled/disabled slot states, output display, powered/unpowered redstone indicator, and slot-state toggles.
- Added desktop capture for beacons.
- Added beacon rendering with payment slot, material icons, primary effect buttons, secondary/upgrade buttons, confirm/cancel actions, and effect validation.
- Added desktop capture for brewing stands.
- Added brewing stand rendering with fuel, ingredient, bottle slots, fuel meter, progress indicator, and bubble animation.
- Added desktop capture for cartography tables.
- Added cartography rendering with map slot, extra input slot, result slot, map preview, plus/arrow art, lock indicators, duplicate-map preview, and error state.
- Added desktop capture for enchanting tables.
- Added enchanting rendering with item/lapis slots, animated book, enchantment options, level clues, costs, disabled states, and click handling.
- Added desktop capture for merchants and villagers.
- Added merchant rendering with trade list, scrolling, selected trade, payment/result slots, discount display, out-of-stock indicators, villager level, XP progress, and restock/progress state.
- Added desktop capture for horse inventories.
- Added desktop capture for camel inventories.
- Added desktop capture for llama inventories.
- Added desktop capture for nautilus inventories on supported versions.
- Added mount inventory rendering with entity-aware special session support.

## Recipe Book And Crafting

- Added recipe-book windows that attach to compatible desktop container windows.
- Added recipe-book button rendering for furnace-like windows.
- Added recipe-book button rendering for crafting table windows.
- Added recipe-book button rendering for character crafting.
- Added recipe-book support for Tom's Storage crafting terminals.
- Added recipe placement interception so recipes place into the active desktop session.
- Added server-side recipe validation against unlocked recipes.
- Added ghost recipe support when the server reports a ghost-placement result.
- Added recipe-book search synchronization hooks for supported API windows.
- Added recipe-book slot-click notifications when desktop slots are clicked.

## Item Interaction Details

- Added normal left-click pickup and placement.
- Added right-click splitting and single-item placement.
- Added outside-click carried-stack dropping.
- Added creative outside-click dropping.
- Added drag distribution across slots in a single session using vanilla quick craft.
- Added manual drag distribution across slots when a drag spans multiple sessions.
- Added shift-click quick move from player inventory, hotbar, containers, and supported windows.
- Added focused-container quick move.
- Added quick move into player main inventory from hotbar.
- Added quick move into hotbar from main inventory.
- Added quick move from containers into player inventory.
- Added quick move from containers into hotbar when no player inventory window is visible.
- Added quick move into focused Tom's Storage terminal sessions.
- Added clone-click handling for creative players.
- Added hotbar-number swap handling.
- Added offhand-swap handling.
- Added drop-key handling.
- Added bundle selected-item scrolling and clearing on quick move/swap.
- Added shared carried-stack sync between all open server sessions and the player inventory menu.

## Server Session And Multiplayer Support

- Added a client/server ready handshake for Salt desktop sessions.
- Added per-player desktop session management.
- Added a maximum of 16 live desktop sessions per player.
- Added automatic oldest-session closing when the session cap is reached.
- Added stable source keys for block-backed windows.
- Added stable combined source keys for double chests.
- Added stable source keys for entity-backed mount windows.
- Added same-source toggling so reopening the same source closes or toggles the existing session.
- Added server-side slot, data, carried-stack, merchant-offer, visibility, and close synchronization.
- Added server-side click handling for player inventory sessions.
- Added server-side click handling for captured container sessions.
- Added server-side button handling for crafters, beacons, merchants, and vanilla menu buttons.
- Added server-side anvil rename handling.
- Added server-side recipe placement handling.
- Added server-side custom payload handling for API windows.
- Added server-side visibility changes for ghost-pinned sessions.
- Added server-side pin-state tracking.
- Added session invalidation when a menu is no longer valid.
- Added disconnect cleanup for all desktop sessions.
- Added runtime shutdown cleanup when the mod is disabled.
- Added fallback behavior for servers or menus that cannot support Salt desktop networking.

## Configuration

- Added `/saltsinventory config`.
- Added `/salts_inventory config`.
- Added a Fabric Mod Menu config screen entry.
- Added a JSON-backed client config file.
- Added "Enable the Mod" toggle.
- Added "Expandable Inventory" toggle.
- Added "Window Opening Style" selector.
- Added "Open Unlocked" toggle.
- Added "Allow Resizing" toggle.
- Added "Enable Window Snapping" toggle.
- Added "Reset Locked Windows" toggle.
- Added "Enable Ghost Pins" toggle.
- Added "Ghost Window Opacity" slider.
- Added "E Hold Close-All Time" slider.
- Added "Reset Defaults" control.
- Added runtime warning text when Salt desktop networking is unavailable on a server.
- Added Mod Menu links for Discord, API docs, source, and donations.

## Desktop API For Add-Ons

- Added Salt Desktop Window API v2.
- Added client window registration by menu type.
- Added client window replacement by menu type.
- Added predicate-based client window registration.
- Added server window registration for session capture.
- Added server payload registration for custom desktop packets.
- Added typed payload helpers on supported versions.
- Added raw byte payload fallback for Minecraft 1.20.1.
- Added client window hooks for state creation, title, default size, minimum size, resize policy, snap size, open/close, move, resize, focus, ghost, tick, render, slot hit detection, mouse input, keyboard input, text input, tooltips, payloads, and local state save/load.
- Added server window hooks for state creation, open, tick, close, visibility changes, and pin changes.
- Added desktop contexts exposing Minecraft, menu, original title, session id, source key, window geometry, focused/minimized/ghosted state, carried stack, menu slots, container slots, and menu slot ids.
- Added render helpers for text, scaled text, fills, sprites, textures, nine-slice windows, one-pixel nine-slice elements, items, virtual items, slots, slot backgrounds, slot highlights, entity previews, and tooltips.
- Added input helpers for modifier keys, mouse buttons, menu buttons, rename packets, slot clicks, quick moves, recipe-book toggles, recipe-book search, and custom payload sends.
- Added reusable desktop widgets: text boxes, icon buttons, text buttons, scrollbars, dropdowns, virtual item grids, virtual item records, widget rectangles, and text-box state.
- Added virtual item grid support for terminals and other non-slot item lists.
- Added API resize policy support for fixed windows and storage-grid windows.
- Added API local-state persistence inside Salt window state.
- Added API inheritence of desktop lifecycle, shared carried stack, hotbar/offhand interaction, shift-click handling, lock, pin, ghost pin, placement, and saved geometry.
- Added API documentation under `docs/markdown/desktop_window_api.md`.

## Tom's Simple Storage Compatibility

- Added optional detection for Tom's Simple Storage.
- Added desktop support for the Tom's Storage Terminal.
- Added desktop support for the Tom's Crafting Terminal.
- Added desktop support for the Tom's Inventory Configurator.
- Added desktop support for the Tom's Level Emitter.
- Added desktop support for the Tom's Inventory Link.
- Added desktop support for the Tom's Item Filter.
- Added desktop support for the Tom's Tag Item Filter.
- Added desktop support for the Tom's Filing Cabinet.
- Added Tom's terminal virtual item snapshots.
- Added terminal snapshot truncation to stay under packet limits.
- Added terminal item count overlays.
- Added terminal search box.
- Added terminal search keep-memory per source.
- Added terminal search options for auto select, keep search, recipe-book sync, and smart recipe-book behavior.
- Added terminal sort mode cycling by amount, name, and mod.
- Added terminal ascending/descending sort toggle.
- Added terminal control mode cycling for default, AE-style, and RS-style behavior.
- Added terminal ghost/keep-last mode toggle.
- Added terminal scrolling and resize-aware visible rows/columns.
- Added terminal click actions for pull/push stack, shift pull, pull one, get half, get quarter, and space-click actions.
- Added crafting terminal 3x3 crafting grid.
- Added crafting terminal result slot.
- Added crafting terminal clear button.
- Added crafting terminal recipe-book button and search sync.
- Added inventory configurator controls for priority, add/deny, side, skip inventory, keep last, and reset/deny behavior.
- Added level emitter slot rendering.
- Added level emitter greater-than/less-than toggle.
- Added level emitter count controls for -10, -1, +1, and +10.
- Added inventory link channel list.
- Added inventory link channel creation, deletion, selection, public/private toggle, name editing, and scrolling.
- Added item filter allow/deny toggle.
- Added item filter NBT matching toggle.
- Added item filter 3x3 filter slot grid.
- Added tag item filter sample slot.
- Added tag item filter available-tag list from the sample item.
- Added tag item filter add/remove controls.
- Added tag item filter allow/deny toggle.
- Added filing cabinet 9x5 visible slot grid.
- Added filing cabinet scroll support through menu button updates.
- Added Tom's Storage server payloads for NBT updates and terminal actions.
- Added Tom's Storage server snapshots for terminal contents, inventory-link channels, and tag filters.

## UI Assets

- Added custom Salt window frame texture.
- Added custom Salt window control texture.
- Added custom slot textures.
- Added custom large-slot texture.
- Added custom scrollbar background texture.
- Added custom search-bar texture.
- Added custom increase-inventory button texture.
- Added custom 3D model display texture.
- Added custom container widget texture for furnace/crafting-style progress art.
- Added custom anvil hammer texture.
- Added custom smithing hammer texture.
- Added custom grindstone texture.
- Added custom stonecutter container texture.
- Added custom loom input, options, and preview textures.
- Added custom crafter slot, disabled slot, output display, powered redstone, and unpowered redstone textures.
- Added custom beacon UI texture.
- Added custom brewing stand slot texture.
- Added Salt mod icon and language entries.

## Debugging, Compatibility, And Safety

- Added structured desktop debug logging.
- Added mount diagnostics for camel and llama session debugging.
- Added functional test harness support behind an opt-in build flag.
- Added functional test scripts for source parity and runtime matrix checks.
- Added version/loader documentation for adding future targets.
- Added mixin accessors and invokers needed for desktop screens, menu constructors, recipe books, mouse state, HUD integration, and player inventory expansion.
- Added graceful config load/save fallback when config files are missing or malformed.
- Added graceful window state load/save fallback when saved geometry is missing or malformed.
- Added custom desktop payload size protection.
- Added Tom's Storage payload size protection.
- Added fallback reflection paths for fields that differ across target versions.
- Added user-facing runtime disable message when a remote server cannot support Salt desktop features.
