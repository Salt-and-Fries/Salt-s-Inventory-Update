# Remaining Project Everything window scope - Minecraft 1.20.1

**Current status:** see the [updated remaining queue](project-everything-current-remaining.md):
the live implementation queue tracks completed integrations. The 31-mod table below is the historical shortlist,
not the current implementation backlog.

Remaining original extra-work shortlist, Minecraft 1.20.1; completed/parked Create family and newly discovered Create addons omitted as requested. Previously implemented 56-family and earlier batches omitted.

31 original shortlisted mods remain: 25 with inventory-related screen scope and 6 that stay native under the inventory-only rule. There are 173 audited screen types before optional/uncaptured cases.

Count concrete audited native screen types, grouping tiered blocks and mode variants sharing that type. Include inventory-adjacent editing dialogs and item filters. This is not the number of distinct textures, machine blocks or custom renderer classes that must be written.

| Mod | Screen types | Captured views | Scope |
|---|---:|---:|---|
| Accessories | 1 | 1 | Accessories/equipment screen; adaptive slot groups. |
| Ad Astra | 14 | 14 | Eleven machines and three vehicle screen types; planet selection remains native. |
| Applied Botanics | 1 | 1 | Portable mana-storage cell; reuses AE2 storage UI rather than requiring a new standalone renderer. |
| Alex's Mobs | 1 | 1 | Transmutation Table. |
| Applied Energistics 2 Wireless Terminals | 5 | 5 | All five approved windows implemented on 1.20.1 Fabric / Forge: crafting, trash, magnet configuration, pattern encoding and pattern access. The newer Forge release's native wireless settings page is also retained. [Implementation and focused checks](../../compatibility/ae2/wireless-terminals-1.20.1.md). |
| Bountiful | 1 | 1 | Bounty Board. |
| DungeonZ | 0 | 0 | Dungeon/difficulty selection; no inventory slots in the captured portal UI. |
| ElevatorMod | 0 | 0 | Elevator options; no inventory slots. |
| Enchanted: Witchcraft | 5 | 5 | Altar, Distillery, Poppet Shelf, Spinning Wheel and Witch's Oven. |
| Extended AE | 24 | 25 | Twenty-four captured screen types across 25 views. Four uncaptured pattern-inspection types need scope classification; not included in the count. |
| Immersive Aircraft | 1 | 1 | Shared adaptive aircraft-inventory screen; vehicle capacities/upgrade arrangements vary. |
| Kibe | 13 | 22 | Thirteen screen types across 22 captured machine/tier/item views. |
| L_Ender's Cataclysm | 2 | 2 | Netherite Ministrosity inventory and Weapon Fusion/Fusion Anvil. |
| Mine Cells | 1 | 2 | Cell Crafter; normal and unbreakable variants are two captured views. |
| Modern Industrialization | 4 | 110 | Generic machine screen, Forge Hammer, item-pipe and fluid-pipe configuration; approved designs implemented on 1.20.1 Fabric / 1.21.1 NeoForge. [Focused verification](../../compatibility/modern-industrialization.md). |
| Powah! | 11 | 73 | Eleven concrete machine/network screen types across 73 captured tier/variant views; three abstract library bases are not extra UIs. |
| Prominent Base (embedded) | 3 | 3 | Primordial Forge crafting, Forge upgrade and Cinder/Serkonid trading. Dialogue, weekly quests and lottery stay native. |
| Quark | 4 | 4 | All six approved layouts implemented on 1.20.1 Forge. Also ported to 1.21.1 NeoForge: five Quark layouts plus SIU's existing vanilla Crafter. Backpack follows player Inventory on both targets. [Availability and focused verification](../../compatibility/quark-all-targets.md). |
| Simple Storage Network | 10 | 10 | Terminals, remote variants, cable filters and collection interface. |
| Sortilege | 0 | 0 | Knowledge Book is a book viewer. Its single synthetic slot rejects both insert and take; it is not an inventory window. |
| Armor Statues | 0 | 0 | Armor-stand pose/configuration controls rather than a separate inventory UI. |
| Tech Reborn | 51 | 51 | Fifty-one audited concrete machine screen types; tiers/configurations may share implementations. |
| The Aether | 5 | 5 | Altar, Freezer, Incubator, Accessories and Book of Lore item inspection. Lore takes an actual item, unlike a read-only guidebook. One additional optional Accessories Backpack integration exists with Quark Oddities. |
| The Bumblezone | 3 | 3 | Crystalline Flower, Honey Cocoon and Buzzing Briefcase. |
| Things | 0 | 0 | Displacement Tome destination management; captured menu contains no inventory slots. |
| Tom's Trading Network | 2 | 2 | Vending-machine configuration and customer trading. |
| Trading Post | 1 | 1 | Multi-villager trading; reuse the existing SIU merchant implementation where possible. |
| Traveler's Backpack | 2 | 2 | Backpack and its inventory/filter settings. |
| Gravestone / Universal Graves | 1 | 3 | User shortlist alias mapped to the Universal Graves storage captures in the report. Reuses a vanilla chest-style screen; grave-list selection is not another storage layout. |
| Wraith Waystones | 0 | 0 | Destination selectors/waystone settings stay native; repeated stone textures and the reusable parent are not separate inventory windows. |
| Zenith Renewed | 7 | 8 | Historical audit count; all seven types are now implemented on 1.20.1 Fabric, including a ninth Alexandria library reference captured during design. [Implementation and focused checks](../../compatibility/zenith-1.20.1.md). |

## Conditional scope

- Extended AE: four pattern-inspection screen types are retained but uncaptured; classify before counting as inventory work.
- Quark: two optional Oddities screen types inactive in the audited pack.
- The Aether: one optional Quark Oddities backpack-integration screen, inactive in the audited pack.
- Gravestone alias refers to Universal Graves in the saved report; a different mod literally named Gravestone would need a separate inventory check.

Reconciled against saved native gameplay captures, static inventory and source review; no new gameplay tests or implementation changes.

The JSON and CSV are in `output/project-everything-remaining/`. Counts refer to the pinned audit, not every release or future addon.

## Native UI preservation workstream

The six zero-window mods remain in Project Everything as preservation checks, separate from inventory-window implementation. Their original Minecraft screens should open normally, keep their own size/layout and input, and close/reopen correctly with SIU installed. A centered guidebook or settings panel should keep that original presentation; native-screen preservation does not mean stretching its artwork across the display.

- [ ] **DungeonZ**: prior audit records 1 native openings with zero SIU windows; focused controls/current-build verification remains pending.
- [ ] **ElevatorMod**: prior audit records 1 native openings with zero SIU windows; focused controls/current-build verification remains pending.
- [ ] **Sortilege**: prior audit records 1 native openings with zero SIU windows; focused controls/current-build verification remains pending.
- [ ] **Armor Statues**: dedicated native opening and control checks remain pending; no retained gameplay pair.
- [ ] **Things**: prior audit records 1 native openings with zero SIU windows; focused controls/current-build verification remains pending.
- [ ] **Wraith Waystones**: prior audit records 11 native openings with zero SIU windows; focused controls/current-build verification remains pending.

If a conflict is found, use an exact screen/menu preservation rule. Avoid whole-mod exclusion so any genuine inventory interfaces can still receive windows. Full expectations and prior evidence are in `output/project-everything-remaining/native-preservation-checklist.json`. No runtime/API code changed for this planning update.
