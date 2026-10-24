# Character window layout — 26.3 Fabric

Validated on 2026-10-22 as an initial 26.3 Fabric preview. The initial loader gate preserved NeoForge during that preview; the subsequent all-version port removes that gate and applies the layout to every Core target. This document records the preview checks. See [all-version validation](CHARACTER-LAYOUT-ALL-VERSIONS-VALIDATION.md) for the port's results.

## Changes

- Armor, player preview, offhand, crafting grid, crafting result and recipe-book button use vanilla upper-inventory positions.
- The Fabric window is 176 × 102 GUI units, including its title bar and four extra GUI pixels of bottom padding below the vanilla upper-inventory area. Health, Hunger and XP text and the unused lower area are removed. No Crafting label is added.
- A dedicated transparent 16 × 16 `character_crafting_arrow.png` replaces the 24-pixel atlas arrow in this layout, leaving two GUI pixels of clearance beside both adjacent slot borders. It is editable in the shared Core texture folder; other layouts retain their existing atlas arrow.
- The offhand slot keeps the existing Return Hotbar to Inventory render and interaction gates.

## Focused checks performed

- `./gradlew.bat :26.3:fabric:build --console=plain` passed, including the module's default automated checks. Log: `build/character-layout/build.log`.
- A one-off coordinate check compared the new positions with vanilla and verified that the previous NeoForge constant values and offhand gates were preserved. Result: `build/character-layout/layout-checks.json`.
- Launched a rendered 26.3 Fabric development client in the isolated `character-window-capture` profile and entered its disposable singleplayer world.
- Opened the character window and dragged it to the center; visually checked the compact layout and absence of the removed text.
- Opened and closed the recipe book with one click each at its new position.
- With Return Hotbar to Inventory disabled, verified that the offhand slot was absent. Enabled the setting through the config screen, reopened the window and verified that the offhand slot appeared below/right of the player preview. Clicking its empty slot highlighted the matching slot area.
- Inspected both saved screenshot crops in `output/screenshots/26.3-fabric-character-layout/`.
- For the bottom-padding/arrow follow-up, `:26.3:fabric:classes :26.3:fabric:jar` passed without rerunning the test suite. Verified the PNG's dimensions, alpha channel and exact packaged bytes, calculated both arrow clearances, and passed the shared-texture source ownership check. Log: `build/character-layout/padding-arrow-build.log`.
- Reopened the isolated singleplayer client for one focused visual check of the bottom margin and new arrow. Both rendered correctly with the offhand enabled. Screenshot: `output/screenshots/26.3-fabric-character-layout/bottom-padding-and-arrow.png`. No additional gameplay scenarios or automated suite were run for this follow-up.

## Packaging

Updated `dist/core/salts_inventory_update-26.3-Fabric-0.1.5.jar`, its `build/release/0.1.5/` mirror, both Core checksum manifests and the matching entries in `dist/salts-inventory-all-latest.zip`. Verified that the other 35 distribution JARs were unchanged and that the production JAR contains Fabric metadata and excludes the gameplay QA package.

## Coverage limits

These were focused layout/control checks, not a complete functional or gameplay suite. No actual crafting, armor/offhand item transfer, potion-effect layout, Spatial GUI presentation, multiplayer, other Minecraft version or NeoForge gameplay test was run for this change. The GUI client used rebuilt development classes; production packaging was inspected separately.
