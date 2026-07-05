# Salt's Inventory Update 0.1.2 Changelog

First-time help window, in-game controls guide, persistent and linked window behavior, new desktop window config options, container/inventory placement fixes, command cleanup, Forge/NeoForge Sinytra Connector compatibility fixes, and cross-version support.

This release adds a built-in Salt's Inventory Help window that introduces the desktop inventory workflow the first time a player opens a world with the mod installed. The same guide can be opened again with `/saltsinventory help`, includes formatted pages for controls and supported integrations, and is available across every supported Minecraft version and loader.

This release also fixes the Forge and NeoForge jar layout so the non-Fabric builds no longer ship Fabric API compatibility classes under `net.fabricmc`, preventing Java module/package export conflicts in modpacks that use Sinytra Connector or other mods that expose Fabric API packages.

## Supported Minecraft Versions And Loaders

- Added the help window feature for Minecraft 26.2 on Fabric and NeoForge.
- Ported the help window feature to Minecraft 26.1.2 on Fabric and NeoForge.
- Ported the help window feature to Minecraft 1.21.11 on Fabric and NeoForge.
- Ported the help window feature to Minecraft 1.21.1 on Fabric and NeoForge.
- Ported the help window feature to Minecraft 1.20.1 on Fabric and Forge.
- Ported persistent windows, linked windows, minimizable-window config, and automatic inventory-on-container-open behavior to every supported Minecraft version and loader.
- Added the Forge/NeoForge packaging compatibility fix across every supported non-Fabric build.
- Kept loader behavior shared through the existing versioned Fabric desktop screen sources and Forge/NeoForge shims.
- Verified compile coverage across the full supported version and loader matrix.

## Desktop Window Behavior

- Added a `Persistent windows` config option that saves visible windows when the screen is cleared, then restores them the next time a Salt window opens.
- Changed held `E` behavior while persistent windows are enabled to show `Clear Screen` and hide windows persistently instead of permanently closing them.
- Added held `Esc` support using the same hold duration as held `E`; when completed, it truly closes all windows and suppresses the follow-up pause action until Escape is released and pressed again.
- Added a `Minimizable windows` config option, disabled by default, that controls whether minimize buttons appear on windows.
- Added an `Open inventory when containers are opened` config option, including creative-mode support so opening a container opens the creative inventory window instead of the survival inventory window.
- Updated container placement so closed or pinned inventory and creative inventory windows still reserve their saved placement, preventing newly opened containers from overlapping the inventory when it is reopened.
- Changed window ellipsis menus so using buttons inside the popup does not immediately close the popup.
- Added a Link title-bar button and link-selection mode. Linked windows open and close together bidirectionally, while linked container windows still respect nearby and line-of-sight checks before reopening.
- Added link-mode visual states: the origin window is green with an outline, linked windows are green without an outline, and selectable unlinked windows receive a blue highlight without an outline.

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

## Build And Jar Verification

- Added a `verifyNonFabricModJars` Gradle task that fails if Forge or NeoForge upload jars contain `net/fabricmc` entries, `fabric.mod.json`, or Fabric entrypoint classes.
- Wired the root `build` task to run `verifyNonFabricModJars` automatically.
- Verified `compileJava`, `verifyNonFabricModJars`, and the full `build` task after the compatibility fix.
- Scanned the generated Forge and NeoForge upload jars and confirmed they contain no `net/fabricmc` entries and no Fabric entrypoint classes.
- Smoke-tested NeoForge `runClient` on Minecraft 1.21.1, 1.21.11, 26.1.2, and 26.2 by launching the client, opening an existing world, closing the first-time help window, and opening the inventory.
- Confirmed the NeoForge smoke-test logs contained no crash, exception, fatal error, build failure, or error markers, and that each tested client shut down normally.

## Compatibility And Safety

- Kept the help system client-side and config-backed so it does not affect server inventory sessions.
- Kept the help window separate from functional inventory windows so it cannot move items or alter desktop session state.
- Kept optional integration pages guarded by mod detection so the guide does not mention unavailable controls or pages.
- Preserved existing Salt desktop controls while adding the new manual help command.
- Verified the feature with normal compile coverage and JEI-absent compile coverage.
