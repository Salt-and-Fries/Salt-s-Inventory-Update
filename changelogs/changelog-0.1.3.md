# Salt's Inventory Update 0.1.3 Changelog

This update improves inventory clicks and item dragging.

- Fixed small mouse movements inside a single slot accidentally starting a drag. Item distribution now begins only after moving over another valid slot.
- Fixed placement clicks and item drags sometimes being ignored when moving items in quick succession.
- Fixed quickly placing an item and then double-clicking it failing to pick it back up and collect matching items.
- Fixed drag previews showing incorrect amounts when the number of held items changes while dragging.
- Fixed inventory clicks and Shift-click transfers sometimes needing multiple attempts on Minecraft 1.21.1.
