# Quark windows — Minecraft 1.20.1 Forge

The six approved mockups are implemented in Compatibility 0.1.3, against Quark
4.0-462, Zeta 1.0-31 and optional Quark Oddities 1.20.1. Core JARs and public API
signatures are unchanged. Quark remains optional and is not bundled.

The same designs also support Minecraft 1.21.1 NeoForge. See
[the all-target compatibility guide](quark-all-targets.md) for publisher
availability and the differences in the newer release.

| Interface | Implementation |
| --- | --- |
| Storage Crate | Original six-row grid, scrollbar and native 640-item capacity. Search has a separate row and uses Quark's own matching and configured dimming color. SIU supplies sorting and transfer controls. |
| Feeding Trough | Complete native nine-cell grid in a compact window; original feeding inventory and item transfers. |
| Crafter | Original input grid, disabled-cell artwork, powered indicator and full result-preview frame. Empty-cell toggles use the authenticated menu-button route. Recipe inputs are excluded from storage sorting. |
| Held Shulker Box | Native 27-cell inventory, ordinary inventory right-click entry, source-item protection and native persistence. |
| Backpack | Native 27-slot storage and stitched leather artwork. The equipped bag opens automatically above player Inventory. See the linked lifetime below. |
| Matrix Enchanting | Native item/lapis/output cells, charge, bookshelf/XP indicators, 5×5 puzzle, piece docket and generate button. Native operations retain server-side eligibility, payment and enchanting rules. |

Crate, Backpack and Matrix Enchanting follow Quark Oddities' module availability.
The separate Aether/Quark backpack integration is outside this Quark-alone adapter.

## Linked Backpack

- Inventory and Backpack open and close together. Inventory's X and inventory-key
  toggle close the bag; reopening restores the same equipped bag's contents.
- Backpack has no X or user Close action. Its storage sort/transfer, focus, pin,
  lock and link controls remain available.
- Dragging either title bar moves both windows. Pin and lock apply to the pair.
  Screen-edge fitting and GUI-scale changes retain the attachment.
- Quark's filled-bag binding restriction remains active. An empty bag can be
  unequipped normally, closing the child; re-equipping opens one child again.

The child uses original Quark slots on the server and isolated client snapshots.
It does not recreate the native player/crafting slots. Two client-only Compatibility
hooks prevent SIU's lock-to-default-geometry setting from resetting either member
independently; they apply only to this active pair. No Core/API change is needed.

Crate scroll and Matrix operations are sent to the owning authenticated SIU
session, then executed by Quark's original menu or enchanting block entity.
The Matrix docket's physical-display scissor is translated through the public
native-capture geometry API, preserving its pieces and native scrolling viewport.

## Focused verification

Real game evidence is retained in `build/quark-window-implementation/` and the
`1.20.1-forge-integrated-quark-*` profiles under
`build/remaining-ui-audit/gameplay/`.

- All six layouts: complete cell bounds and native artwork, including the full
  Crafter result frame and Backpack stitching. Pixel probes compare 130 cell
  rims with SIU-absent screenshots, excluding count text.
- Crate: wheel/drag scroll with server agreement; search/clear; 640-item limit,
  remainder return and SIU sorting without count loss.
- Trough: native item transfer. Crafter: disable/re-enable, recipe preview and
  redstone-triggered ingredient consumption.
- Held Shulker: ordinary entry, transfer saved into source NBT and source lock.
- Matrix: item/lapis input, paid generation, selection/rotation, placement/removal,
  enchanted output and native input consumption. Docket visibility checked again
  after the scissor correction.
- Backpack: insertion/persistence, both drag headers, edge fit, pin/lock pairing,
  Inventory X and key-toggle lifecycle, no duplicate child, filled/empty equip
  rules and GUI scaling. Quark without Oddities opens the Trough and adds no bag
  to ordinary Inventory.

These are focused integration checks, not a complete gameplay suite. Matrix
duplicate-piece merging, every configured item blacklist and the optional Aether
integration were not separately exercised.

Screenshot checks can be repeated with:

```powershell
python tools/check-quark-window-captures.py
```
