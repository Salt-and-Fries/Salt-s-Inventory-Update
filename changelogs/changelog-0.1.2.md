# Salt's Inventory Update 0.1.2 Changelog

First-time help window, in-game controls guide, conditional compatibility pages, command cleanup, and cross-version help UI support.

This release adds a built-in Salt's Inventory Help window that introduces the desktop inventory workflow the first time a player opens a world with the mod installed. The same guide can be opened again with `/saltsinventory help`, includes formatted pages for controls and supported integrations, and is available across every supported Minecraft version and loader.

## Supported Minecraft Versions And Loaders

- Added the help window feature for Minecraft 26.2 on Fabric and NeoForge.
- Ported the help window feature to Minecraft 26.1.2 on Fabric and NeoForge.
- Ported the help window feature to Minecraft 1.21.11 on Fabric and NeoForge.
- Ported the help window feature to Minecraft 1.21.1 on Fabric and NeoForge.
- Ported the help window feature to Minecraft 1.20.1 on Fabric and Forge.
- Kept loader behavior shared through the existing versioned Fabric desktop screen sources and Forge/NeoForge shims.
- Verified compile coverage across the full supported version and loader matrix.

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
- Added a Main Controls page for opening and closing the inventory, character window, optional JEI window, closing all Salt windows, returning mouse control to the camera, and closing the desktop.
- Added a Window Controls page explaining unlocked window movement, supported resizing, and each title-bar button.
- Added title-button entries for Focus, Pin, Lock, Minimize, and Close, each with its matching button sprite.
- Added an Inventory Tools page explaining the interactive hotbar, offhand interaction, and optional expandable inventory slot purchases.
- Added a JEI page for players who have Just Enough Items installed.
- Added a Tom's Simple Storage page for players who have Tom's Simple Storage installed.

## Conditional Compatibility Help

- Added runtime detection so the JEI help page only appears when JEI is installed.
- Added runtime detection so the Tom's Simple Storage help page only appears when Tom's Simple Storage is installed.
- Changed the Main Controls page so the `H` keybind row for opening the JEI window only appears when JEI is installed.
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

## JEI Runtime Testing

- Added an `includeJeiRuntime` Gradle property so client runs can be launched without JEI while still compiling against the JEI API.
- Added support for running `runClient` without JEI using `-PincludeJeiRuntime=false`.
- Used the JEI-free runtime path to verify that conditional JEI help content is hidden when JEI is absent.

## Compatibility And Safety

- Kept the help system client-side and config-backed so it does not affect server inventory sessions.
- Kept the help window separate from functional inventory windows so it cannot move items or alter desktop session state.
- Kept optional integration pages guarded by mod detection so the guide does not mention unavailable controls or pages.
- Preserved existing Salt desktop controls while adding the new manual help command.
- Verified the feature with normal compile coverage and JEI-absent compile coverage.
