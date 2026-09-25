# Salt's Inventory Update 0.1.2 Changelog

First-time help window, in-game controls guide, persistent and linked window behavior, sorting into open containers, Sophisticated Storage and Backpacks integration on Forge/NeoForge, text box editing controls, configurable hotbar and mouse-focus behavior, full JEI/REI/EMI recipe-browser integration, crafting and inventory synchronization fixes, command cleanup, Forge/NeoForge Sinytra Connector compatibility fixes, and cross-version support.

This is also a protocol-breaking correctness and security release. Salt's Inventory Update 0.1.2 clients and servers use desktop protocol 2 and must be updated together. A genuinely unmodded peer continues to use vanilla container behavior, while a detected 0.1.1 Salt peer is rejected with a clear incompatibility message instead of being allowed to enter a partially compatible session.

Linked container reopening is now authorized by a bounded server-owned link graph. Existing visual links remain in client layout state, but an old link becomes reopenable only after both linked containers have been opened legitimately together once on 0.1.2. Raw saved source strings no longer grant remote container access.

This release adds a built-in Salt's Inventory Help window that introduces the desktop inventory workflow the first time a player opens a world with the mod installed. The same guide can be opened again with `/saltsinventory help`, includes formatted pages for controls and supported integrations, and is available across every supported Minecraft version and loader.

This release also fixes Salt-managed text boxes so Creative search, Recipe Browser search, anvil renaming, Tom's Simple Storage fields, and add-on text boxes support normal single-line editing controls such as arrow keys, selection, copy, cut, paste, and select-all while typing.

This release also fixes the Forge and NeoForge jar layout so the non-Fabric builds no longer ship Fabric API compatibility classes under `net.fabricmc`, preventing Java module/package export conflicts in modpacks that use Sinytra Connector or other mods that expose Fabric API packages.

The former JEI-only desktop window is now a neutral Recipe Browser Window. JEI remains supported, REI is supported across the full version and loader matrix, and EMI is supported on Minecraft 1.21.1 Fabric and NeoForge. Shared browsing, favorites, recipe lookup, recipe transfer, and recipe-tree controls now use Salt-owned UI assets and behavior.

## Supported Minecraft Versions And Loaders

- Added the help window feature for Minecraft 26.2 on Fabric and NeoForge.
- Ported the help window feature to Minecraft 26.1.2 on Fabric and NeoForge.
- Ported the help window feature to Minecraft 1.21.11 on Fabric and NeoForge.
- Ported the help window feature to Minecraft 1.21.1 on Fabric and NeoForge.
- Ported the help window feature to Minecraft 1.20.1 on Fabric and Forge.
- Ported persistent windows, linked windows, minimizable-window config, and automatic inventory-on-container-open behavior to every supported Minecraft version and loader.
- Ported the desktop text box editing fix to every supported Minecraft version and loader.
- Added sorting into open containers to all five supported Minecraft versions on Fabric and their Forge/NeoForge counterparts.
- Added Sophisticated Storage and Sophisticated Backpacks desktop integration on 1.20.1 Forge and 1.21.1, 1.21.11, 26.1.2, and 26.2 NeoForge; Fabric continues to use their native screens.
- Added REI recipe-browser support to every supported Minecraft version and loader.
- Added EMI recipe-browser support for Minecraft 1.21.1 on Fabric and NeoForge.
- Documented that Minecraft 1.21.1 and earlier are the supported EMI version range, while newer Minecraft versions currently have no compatible EMI release.
- Ported the shared recipe-browser UI, common textures, recipe transfer controls, favorites, REI tree windows, and window-control tooltips to every supported version and loader where the corresponding browser is available.
- Backported linked-window session-close behavior to Minecraft 1.20.1 and 1.21.1 so old-version builds match newer linked-window close behavior.
- Added the Forge/NeoForge packaging compatibility fix across every supported non-Fabric build.
- Kept loader behavior shared through the existing versioned Fabric desktop screen sources and Forge/NeoForge shims.
- Verified compile coverage across the full supported version and loader matrix.

## Desktop Window Behavior

- Added a `Persistent windows` config option that saves visible windows when the screen is cleared, then restores them the next time a Salt window opens.
- Added a `Global Pins` config option, disabled by default, that shares non-block window pin layouts across worlds and servers while keeping block-backed windows scoped to their world and block source.
- Changed held `E` behavior while persistent windows are enabled to show `Clear Screen` and hide windows persistently instead of permanently closing them.
- Added held `Esc` support using the same hold duration as held `E`; when completed, it truly closes all windows and suppresses the follow-up pause action until Escape is released and pressed again.
- Added a `Minimizable windows` config option, disabled by default, that controls whether minimize buttons appear on windows.
- Added an `Open inventory when containers are opened` config option, including creative-mode support so opening a container opens the creative inventory window instead of the survival inventory window.
- Added a `Return Hotbar to Inventory` config option that places the interactive hotbar below Inventory and Creative windows and moves the offhand slot into the Character window.
- Added a configurable `Change Mouse Focus` keybind, bound to Left Alt by default, for switching between Salt UI control and camera control while windows remain open.
- Added hotbar-only mouse interaction when the mouse-focus key is held without another Salt window open.
- Fixed the camera snapping after closing the desktop by preserving the first real mouse movement during the handoff back to gameplay.
- Updated container placement so closed or pinned inventory and creative inventory windows still reserve their saved placement, preventing newly opened containers from overlapping the inventory when it is reopened.
- Changed window ellipsis menus so using buttons inside the popup does not immediately close the popup.
- When a title bar is crowded, moved every control except Close into the ellipsis menu, including Sort into open containers and its Shift action.
- Added a Link title-bar button and link-selection mode. Linked windows open and close together bidirectionally, while linked container windows still respect nearby and line-of-sight checks before reopening.
- Added link-mode visual states: the origin window is green with an outline, linked windows are green without an outline, and selectable unlinked windows receive a blue highlight without an outline.
- Extended the shared window-control texture with a dedicated Link button column so the control can be edited consistently with the other title-bar buttons.
- Added delayed tooltips to title-bar controls; hovering a button for one second now shows its name.
- Fixed linked container windows on Minecraft 1.20.1 and 1.21.1 so closing a container by right-clicking the same block again also closes its linked companion windows, matching the normal title-bar close button behavior.
- Changed server-driven session removals in old-version clients to run the same linked-window close cascade used by newer builds before removing the origin window.
- When riding a chest boat or another inventory-bearing entity, pressing `E` opens its container together with the player inventory and pressing `E` again closes both. Opening the same entity while not riding it remains a normal container action.
- Fixed resized player inventories reverting to their default width during layout refreshes or after reopening; valid user-selected width and height are preserved.
- Fixed the offhand slot drawing above overlapping desktop windows.

## Sort Into Open Containers

- Added a window-control button for the player inventory, storage containers, and supported processing containers. Normal click moves only items whose type is already present in another open storage window; unmatched items stay in the source.
- Shift-click tries storage containing the same item type, then storage containing an item with a shared tag, then the focused storage window, then initially empty storage windows. Full destinations pass leftovers to the next eligible destination.
- Player inventory sorting uses main-inventory slots only. Furnaces contribute output slots, and brewing stands contribute their three bottle slots even while brewing; machine inputs and fuel remain untouched. Player inventory and processing windows are never destinations.
- Routing uses a snapshot of destination contents taken at the start of the action, and excludes minimized, hidden, ghosted, closed, and duplicate views of the source inventory. Transfers retain normal slot restrictions, stack compatibility, and output extraction rewards.
- Extended the editable window-control texture with normal and Shift sorting columns. Tooltips read “Sort into open containers” and “Force sort into open containers” while Shift is held.

## Storage And Container Compatibility

- Integrated Sophisticated Storage and Backpacks into Salt desktop windows on Forge/NeoForge while retaining native search, sorting, upgrades, settings, transfers, side tabs, and storage rules. Their mods remain optional.
- Fixed item alignment and interaction across Sophisticated storage and ordinary Salt slots, including drag previews, double-click collection, upgrade slots, and authenticated multi-window transfers.
- Prevented an open item Backpack from being moved or used through another inventory view until its final Salt window closes.
- Preserved native chest, trapped chest, barrel, ender chest, and shulker open/close state, sounds, and animations while their Salt windows are open. Each ender-chest window tracks its own physical block even though the inventory contents are shared.
- Moved shared Creative paging buttons to Salt-owned textures so Forge/NeoForge builds do not depend on a Fabric texture path.

## Item Interaction Fixes

- Fixed the affected legacy versions where left- and right-drag placement committed the opposite distribution from the on-screen preview, including drags involving Sophisticated windows.
- Fixed rapid clicking over an empty slot so the first placement and pickup follow vanilla behavior instead of suppressing the placement.

## Text Box Editing Controls

- Replaced append-and-backspace-only custom text handling with reusable single-line text editing state.
- Added caret, selection, and horizontal text view tracking while keeping the existing public `text()` and `focused()` state API compatible.
- Added Left, Right, Home, End, Shift-selection, Ctrl+A, Ctrl+C, Ctrl+X, Ctrl+V, Backspace, Delete, Ctrl+Backspace, and Ctrl+Delete handling for Salt desktop text boxes.
- Added Unicode-safe cursor movement and deletion so surrogate-pair characters are not split by cursor or delete operations.
- Added filtered paste through Minecraft's clipboard handler so pasted text respects the same single-line character filtering as typed text.
- Added visible caret and selection rendering for shared `DesktopWidgets` text boxes and for built-in inline fields that use Salt's custom rendering.
- Updated Creative search, Recipe Browser search, anvil rename, Tom's Simple Storage terminal search, and Tom's Simple Storage inventory-link name fields to use the shared editor behavior.
- Preserved focused-text-field keyboard capture so movement, hotbar, inventory, and global desktop shortcuts do not fire while typing.
- Preserved each field's existing Enter and Escape behavior, including clear-or-blur search fields and blur-only anvil rename handling.
- Kept existing side effects tied to actual text changes, including Creative and Recipe Browser scroll resets, browser search sync, anvil rename submission, Tom's terminal settings sync, and inventory-link name updates.
- Fixed legacy 1.20.1 and 1.21.1 key event modifier detection so Ctrl and Shift shortcuts honor the key event modifier mask as well as the current keyboard state.

## Recipe Browser Compatibility

- Replaced the JEI-specific desktop bridge with a neutral recipe-browser adapter shared by JEI, REI, and EMI.
- Added deterministic browser selection with source priority `EMI > REI > JEI` when more than one supported browser is installed.
- Kept the existing JEI integration and adapted it to the neutral interface without changing the saved window-state key or the existing `H` keybind translation key.
- Renamed player-facing references from `JEI Window` to `Recipe Browser Window` while preserving compatibility with existing config and layout data.
- Added native REI plugins and loader metadata for Fabric, Forge, and NeoForge across the supported version matrix.
- Added native EMI plugins for Minecraft 1.21.1 Fabric and NeoForge, including a guarded client bootstrap that remains inert when EMI is absent.
- Kept recipe-browser mods optional so the game launches normally without JEI, REI, or EMI and pressing `H` cleanly does nothing.

## Recipe Browser Window

- Added Salt-owned common textures for browser tabs, navigation arrows, option buttons, history and favorite controls, recipe transfer, station panels, station slots, and the dynamic window background.
- Fixed missing browser textures by removing runtime dependence on JEI-owned UI assets.
- Added browser-provided item and fluid tabs, search filtering, Favorites and Recent tabs, recipe categories, crafting-station tabs, and paged recipe layouts.
- Fixed recipe and uses lookup so selecting an entry only shows recipes related to that entry instead of every registered container and recipe.
- Added left-click and `R` recipe lookup plus right-click and `U` uses lookup throughout the browser and supported recipe views.
- Made recipe-browser windows grow vertically when the left crafting-station tab list needs more room, preventing tabs from overlapping the options controls.
- Added top-tab paging when a browser exposes more entry or category tabs than fit across the window.
- Added recipe favorite buttons and fixed REI favorites so favoriting a recipe stores its output entry instead of REI's display-level quick-craft favorite.
- Positioned favorite and transfer controls outside recipe content so they no longer clip into recipe panels or cover ingredients.
- Added Move Items buttons for compatible open crafting windows, including ingredient availability checks and the existing server-authoritative transfer payload.
- Added recipe sorting controls for bookmarked-first and craftable-first ordering.
- Added item, station, tab, recipe-control, and browser-widget tooltips.

## REI Recipe Trees

- Added a working full-window action for REI's compact tag-tree recipes while preserving the existing copy action.
- Added a dedicated resizable Recipe Tree window that renders only the selected tree.
- Added click-and-drag panning and mouse-wheel zooming for large trees.
- Added a stretchable 3x3 dynamic window background using the same edge-and-center treatment as the 3D model display, with the border expanded by one pixel in every direction.
- Added tooltips for tag nodes and item nodes at the mouse position.
- Added left-click, right-click, `R`, and `U` navigation from tree items to their recipes and uses.

## EMI Compatibility

- Added native EMI item-index, search, category, recipe, uses, Favorites, Recent, sorting, and crafting-station support to the Salt Recipe Browser Window on Minecraft 1.21.1.
- Added EMI crafting-recipe transfer plans for the player 2x2 grid, crafting tables, and crafters when the recipe fits the active crafting window.
- Added EMI recipe favorite controls that store the recipe output entry.
- Forwarded EMI widget rendering and mouse input, including widget-specific tooltips next to the cursor.
- Cached EMI index entries, filtered search results, and recipe widgets so hover highlights and tooltips update at the normal game frame rate.
- Fixed EMI slot highlighting so the highlight renders behind the ingredient instead of replacing it with a solid white square.
- Compacted EMI item-tag and block-tag recipes and JEI tag recipes imported through JEMI, replacing oversized widget bounds and keeping entries top-packed instead of stretching them across tall windows.
- Added `version_differences.md` to record the EMI support boundary for future ports.

## Crafting And Slot Synchronization

- Fixed crafting-table result slots rendering as empty even though the completed item could still be taken.
- Fixed the same invisible crafting result after JEI or REI Move Items filled an open crafting grid.
- Added detached crafting-result recalculation and synchronization across every supported Minecraft version and loader.
- Fixed Minecraft 1.20.1 and 1.21.1 desktop inventories requiring a first click to reconnect before item pickup or placement would work.
- Fixed the remaining Minecraft 1.21.1 container-to-player-inventory path where the first placement click was ignored after taking an item from a container.
- Acknowledged player-menu state before cursor interaction on legacy versions and finalized all shared menus after detached container clicks without weakening stale-state or replay protection.

## Forge And NeoForge Compatibility Hotfix

- Removed mod-provided `net.fabricmc` compatibility classes from Forge and NeoForge upload jars.
- Moved Forge and NeoForge Fabric-style compatibility shims into Salt-owned packages under `com.salts_inventory_update.platform.fabric.api`.
- Moved the shared loader helper into the Salt-owned `com.salts_inventory_update.platform.loader.api` package.
- Updated shared imports across every supported Minecraft version so common desktop, networking, config, inventory expansion, Tom's Simple Storage compatibility, and session code use Salt-owned platform wrappers instead of direct `net.fabricmc` wrapper packages.
- Added Fabric-only platform wrapper source sets for legacy Minecraft 1.20.1 and modern Minecraft versions so Fabric builds continue to call the real Fabric API and Fabric Loader from the shared wrapper imports.
- Excluded Fabric entrypoint classes from Forge and NeoForge compilation so non-Fabric jars do not contain Fabric entrypoints.
- Fixed the class/package conflict pattern that could crash before the FML loading screen with Sinytra Connector, including duplicate exports for packages such as `net.fabricmc.fabric.api.client.networking.v1`.
- Kept this as a packaging and platform-layer compatibility fix; the Fabric runtime behavior and gameplay behavior are unchanged by the hotfix.

## First-Time Help Window

- Added a first-time player help popup that opens when a player enters a world with Salt's Inventory installed.
- Added a persisted client config flag so the first-time help window only opens automatically once.
- Added a Salt desktop help window using the same movable window mechanics as the rest of the UI.
- Added normal Salt window title controls to the help window so it can be moved, focused, pinned, locked, minimized, or closed like other windows.
- Added a dedicated help-window page model for title text, icon textures, section headings, keybind rows, button rows, and body text.
- Added page navigation with Back and Next buttons at the bottom of the help window.
- Added page count text so players can see their position in the guide.

## Commands

- Added `/saltsinventory help` to manually reopen the help window at any time.
- Kept `/saltsinventory config` as the config command path.
- Removed the alternate `/salts_inventory` command spelling so commands consistently use `/saltsinventory`.
- Updated help text to point players at `/saltsinventory help`.

## Help Window Pages

- Added a Welcome page that explains the new inventory experience, window-based inventories, moving items between open windows, and how to reopen the guide.
- Added a Main Controls page for opening and closing the inventory, character window, optional Recipe Browser window, closing all Salt windows, returning mouse control to the camera, and closing the desktop.
- Added a Window Controls page explaining unlocked window movement, supported resizing, and each title-bar button.
- Added title-button entries for Focus, Pin, Lock, Minimize, and Close, each with its matching button sprite.
- Added an Inventory Tools page explaining the interactive hotbar, offhand interaction, and optional expandable inventory slot purchases.
- Added a Recipe Browser page for players who have JEI, REI, or EMI installed.
- Added a Tom's Simple Storage page for players who have Tom's Simple Storage installed.

## Conditional Compatibility Help

- Added runtime detection so the Recipe Browser help page only appears when a supported recipe browser is installed.
- Added runtime detection so the Tom's Simple Storage help page only appears when Tom's Simple Storage is installed.
- Changed the Main Controls page so the `H` keybind row for opening the Recipe Browser window only appears when a supported browser is installed.
- Kept conditional pages out of the page count when their related mod is not installed.
- Preserved the help window flow when neither optional compatibility mod is installed.

## Text Formatting And Readability

- Added section heading formatting with divider lines to separate help topics.
- Added body text wrapping that keeps manual sentence breaks visually separated without adding extra gaps to automatic wrapped lines.
- Added small spacing between manually separated lines and sentences for easier scanning.
- Added taller help window sizing so longer pages have more room for formatted text.
- Added adaptive help window height growth when page content needs more vertical space.
- Added keybind box rendering so mentioned binds display as boxed controls instead of plain text.
- Added support for multi-key bind rows such as `Hold E`.
- Added icon and texture support inside help pages so explanatory rows can show real UI control sprites.

## Help Window Button Rendering

- Changed the help window Back and Next buttons to use Minecraft menu button textures.
- Added enabled, hovered, and disabled button states for help navigation.
- Fixed missing 1.20.1 button textures by mapping the newer `widget/button`, `widget/button_highlighted`, and `widget/button_disabled` sprite IDs to the legacy `textures/gui/widgets.png` button regions.
- Kept the newer sprite-based button rendering for newer Minecraft versions.

## Recipe Browser Runtime Testing

- Changed JEI development runtime inclusion to opt-in with `-PincludeJeiRuntime=true` while retaining compile-only JEI API support.
- Added opt-in REI development runtimes with `-PincludeReiRuntime=true` across supported versions and loaders.
- Added an opt-in EMI development runtime with `-PincludeEmiRuntime=true` for Minecraft 1.21.1 Fabric and NeoForge.
- Added the required REI and EMI Maven repositories and pinned compatible browser/API dependency versions per Minecraft version.
- Used browser-free runtime paths to verify that optional integration startup and conditional help content remain safe when no browser is installed.

## Build And Jar Verification

- Added a `verifyNonFabricModJars` Gradle task that fails if Forge or NeoForge upload jars contain `net/fabricmc` entries, `fabric.mod.json`, or Fabric entrypoint classes.
- Wired the root `build` task to run `verifyNonFabricModJars` automatically.
- Added functional harness coverage for pure desktop text editing operations, including cursor movement, Shift selection, select-all, copy, cut, paste, replacement typing, Backspace, Delete, paste filtering, max-length truncation, and Unicode-safe deletion.
- Added functional coverage that confirms EMI wins the shared browser selection when it is installed and reports itself available.
- Added source parity checks for browser entrypoints, optional runtime flags, EMI caching and compact tag layouts, recipe-widget tooltips, detached crafting results, player-menu state acknowledgements, and shared-menu finalization after container clicks.
- Added 80 sorting regression cases across the five Minecraft versions, covering exact and tag matching, destination priority, full and duplicate storage, output-only extraction, and item-count preservation.
- Verified `compileJava`, `verifyNonFabricModJars`, and the full `build` task after the compatibility fix.
- Verified the text-editing fix with `functionalTestCompile` across the supported version and loader matrix.
- Verified the linked-window close backport with targeted compile coverage for Minecraft 1.20.1 Fabric, 1.20.1 Forge, 1.21.1 Fabric, and 1.21.1 NeoForge.
- Scanned the generated Forge and NeoForge upload jars and confirmed they contain no `net/fabricmc` entries and no Fabric entrypoint classes.
- Smoke-tested NeoForge `runClient` on Minecraft 1.21.1, 1.21.11, 26.1.2, and 26.2 by launching the client, opening an existing world, closing the first-time help window, and opening the inventory.
- Confirmed the NeoForge smoke-test logs contained no crash, exception, fatal error, build failure, or error markers, and that each tested client shut down normally.
- Verified the legacy click fixes with consecutive first-click pickup and placement flows on Minecraft 1.20.1 and 1.21.1.
- Rebuilt all ten supported version/loader targets after the sorting control, mounted-entity window, resize, and compact-menu fixes.
- Verified in-game on 1.20.1 Forge that a narrowed inventory retains its width after closing and reopening, and on 1.21.1 NeoForge that a mounted chest-boat window opens and closes with the player inventory.

## Compatibility And Safety

- Added protocol-2 connection and capability negotiation, connection/session nonces, stale-state validation, and bounded per-action request rates.
- Made the player inventory menu the single cursor authority so closing multiple desktop sessions cannot return or drop duplicate carried stacks.
- Kept expanded inventory save/load, death drops, respawn copying, clearing, and synchronization active while the desktop UI is disabled; gameplay access remains dormant until re-enabled.
- Added centralized server authorization for menu mutations and current reach, line-of-sight, loaded-chunk, provider identity, and protection checks for linked and hidden container restoration.
- Bounded packet collections, custom data, recipe transfer work, and Tom's Storage compressed NBT before allocation or mutation.
- Made state/config writes atomic with backup recovery and stable hashed world/server identities.
- Kept the help system client-side and config-backed so it does not affect server inventory sessions.
- Kept the help window separate from functional inventory windows so it cannot move items or alter desktop session state.
- Kept optional integration pages guarded by mod detection so the guide does not mention unavailable controls or pages.
- Preserved existing Salt desktop controls while adding the new manual help command.
- Verified the feature with normal compile coverage and optional recipe-browser-absent compile coverage.
