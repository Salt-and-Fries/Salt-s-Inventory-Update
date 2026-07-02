# Changelog

## 0.1.1

- Added optional JEI desktop window compatibility across every supported version and loader.
- Added the `H` keybind for opening Salt's JEI window when JEI is installed.
- Added JEI-backed ingredient browsing with search, icon tabs, Favorites, Recent, saved default tab selection, scrolling, and JEI tooltips.
- Added in-window JEI recipe and uses views with category tabs, crafting station tabs, recipe scrolling, recipe tooltips, recipe bookmarks, and JEI sort controls.
- Added JEI recipe history navigation, including back, Shift-forward, Ctrl-return-to-list, Escape close-all, and `R`/`U` hover lookup support.
- Added JEI Move Items transfer buttons for compatible open Salt crafting/container windows, including missing ingredient feedback, repeated-click stacking, Shift max transfer, and server-authoritative validation.
- Added optional JEI metadata and compile-time integration while keeping all direct JEI imports isolated to `compat.jei` so Salt still launches without JEI installed.
- Ported the JEI compatibility pass to Minecraft 26.2, 26.1.2, 1.21.11, 1.21.1, and 1.20.1 across Fabric, NeoForge, and Forge where supported.
- Improved desktop drag previews, result-slot shift-click behavior, standalone inventory visual syncing, detailed diagnostics, and forced container window routing.

## 0.1.0

- Initial multi-version, multi-loader project setup.
- Renamed the project to Salt's Inventory Update.
