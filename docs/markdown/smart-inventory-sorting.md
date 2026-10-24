# Sorting within a window

Core **0.1.5**, on **all twelve supported Minecraft/loader targets**, adds **Sort this inventory** immediately after **Sort into open containers**. This includes Fabric and Forge 1.20.1, plus Fabric and NeoForge 1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3. Both controls move into More Controls together when the full title row does not fit. The new button uses column 1 of `window_controls.png`, the former minimize location; all three existing visual states are retained.

The two buttons have separate purposes:

- **Sort into open containers** transfers items to other open storage windows. Its existing Shift behavior is unchanged.
- **Sort this inventory** consolidates matching partial stacks and rearranges the selected window's storage. Shift does not change this action. Items never cross into another window.

## Default order

Items fill the eligible slots from the top left, with empty slots after the items:

1. Resources: gems, ingots, nuggets, raw materials, ores and resource storage blocks.
2. Equipment: tools, weapons, armor and enchanted books.
3. Supplies: food, potions and consumables.
4. Crafting materials, components and utility items/blocks.
5. Building materials and decoration, with related material variants together.
6. Bulk terrain, including dirt, gravel, sand and common stone.

Classification reads existing vanilla and common `c:` tags, item components and creative categories. Recognized legacy `forge:` tags are also accepted. Blocks contribute their block tags as well as their item tags. Common material subtags and identifier families keep related items together; creative-menu order and registry identifiers provide deterministic fallbacks. Equipment also considers maximum durability, enchantment levels and condition; food considers nutrition. The algorithm never builds or reads localized tooltips.

Modded items using common metadata participate automatically. Conservative identifier suffixes recognize otherwise untagged gems, ingots, nuggets and ores. Unclassified content receives a stable fallback; there is no universal inference of a modded item's economic value.

## Optional datapack overrides

No per-item sorting tags are required. A pack can override exceptional items with these optional item or block tags, in descending priority:

```
salts_inventory_update:sort/valuables
salts_inventory_update:sort/equipment
salts_inventory_update:sort/supplies
salts_inventory_update:sort/crafting
salts_inventory_update:sort/building
salts_inventory_update:sort/terrain
```

For example, `data/salts_inventory_update/tags/item/sort/valuables.json` (use `tags/items/` on 1.20.1):

```json
{
  "replace": false,
  "values": ["example:ancient_relic", "#example:precious_materials"]
}
```

If an item belongs to several override categories, the first category in the list wins. Tags and creative contents are read for the current world when sorting, so a subsequent sort reflects a datapack reload.

## Slot and network boundaries

Player sorting includes main inventory and purchased expansion storage, excluding hotbar, armor, offhand and crafting. Container sorting is available for vanilla storage and menus whose storage slots are explicitly declared through the existing public `DesktopMenuBindings.registerStorage` API. Virtual entries, inactive/fake slots and source-locked slots are excluded. Processing windows, including furnace outputs and brewing slots, do not get the new button. Their existing transfer-sort control is unaffected.

The server authenticates the target session, token and state through the existing mutation transport, then derives the inventory and ordering itself. A complete placement plan must fit the eligible slots and respect item and slot stack limits before anything changes. Only stacks with identical item components (or NBT on 1.20.1) merge. Slot callbacks are checked against snapshots of the whole menu; unexpected changes trigger rollback and synchronization. Sorting is refused while holding a cursor stack or when a safe complete plan cannot be formed. Snapshot verification work is bounded; unusually large/custom layouts may be refused rather than partially rearranged.

The additional packet is advertised independently on modern versions; 1.20.1 negotiates a separate in-place sorting capability through its authenticated transport. A client connected to an older Core server hides the new button. Desktop protocol 5 and the existing transfer-sort behavior are unchanged. Both client and server need a Core build with in-place sorting support for it to work in multiplayer.

`DesktopWindowControl.SORT_INVENTORY` identifies the new control in public decoration state. It does not add a mod-owned `DesktopSurfaceControl` permission. Custom real storage menus opt in through `DesktopMenuBindings.registerStorage`; no integration-specific sorter is required. Compatibility and Controlify JARs are not required or rebuilt for this change.

## Focused validation

See [the executed sorting checks and coverage limits](../../functional-tests/INVENTORY-SORT-VALIDATION.md).
