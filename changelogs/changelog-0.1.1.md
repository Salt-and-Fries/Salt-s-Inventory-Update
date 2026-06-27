# Salt's Inventory Update 0.1.1 Changelog

Drag distribution preview parity update.

This release improves desktop inventory dragging so Salt windows show vanilla-style placement previews while the mouse is still held down, instead of waiting until release to visually update the slots.

## Supported Minecraft Versions And Loaders

- Added the drag-preview update for Minecraft 26.2 on Fabric and NeoForge.
- Ported the same drag-preview behavior to Minecraft 26.1.2 on Fabric and NeoForge.
- Ported the same drag-preview behavior to Minecraft 1.21.11 on Fabric and NeoForge.
- Ported the same drag-preview behavior to Minecraft 1.21.1 on Fabric and NeoForge.
- Ported the same drag-preview behavior to Minecraft 1.20.1 on Fabric and Forge.
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

## Compatibility And Safety

- Preserved existing single-session vanilla `QUICK_CRAFT` release behavior.
- Preserved existing cross-window and cross-session manual release-time fallback behavior.
- Avoided per-hover server clicks, packet changes, server logic changes, and public API changes.
- Adapted preview calculation to older Minecraft versions that use the set-based vanilla quick-craft placement helper.
