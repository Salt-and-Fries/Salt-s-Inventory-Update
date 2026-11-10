# The Aether — Minecraft 1.20.1

The approved Aether designs are implemented in the Fabric and Forge
Compatibility JARs. Core and the public API are unchanged.

The [all-target report](aether-all-targets.md) also covers the completed
1.21.1 Fabric/NeoForge ports and publisher applicability for later targets.

| Interface | SIU behavior |
| --- | --- |
| Altar | Compact Salt window; original ingredient, fuel, enlarged result, recipe-book button, flames and progress arrow. |
| Freezer | Same native recipe layout, using the original freezing artwork and menu. |
| Incubator | Both native slots, recipe book and complete incubation meter. |
| Book of Lore | Complete native book and inspected-item slot, with native Previous/Next controls underneath. |
| Aether Accessories | Cloud toggles the existing Character window into Aether's native upper-inventory arrangement. All eight equipment slots keep their roles, ghost icons and visibility switches. No additional equipment window opens. |

The conditional Quark Oddities Backpack remains attached to **Inventory**.
Closing Character disposes its Aether equipment binding and leaves Inventory
and Backpack open. Closing Inventory closes Backpack and leaves Character in
its current mode. Moving Character moves its complete contents; moving
Inventory carries Backpack. Backpack keeps
the existing 27-slot layout and has no independent Close button.

The crown opens Aether's original Moa Skins screen. Returning preserves the
Character mode. The supporter-customization shortcut is retained when
the native account-perk condition enables it. At the left edge, those shortcuts
move above the panel so their artwork remains visible. The separate existing
Curios/Accessories launcher and the Aether cloud replace each other's equipment
presentation. Opening the general Curios/Accessories panel restores normal
Character layout and closes the Aether binding first. Pressing cloud closes
that general panel before enabling Aether mode. Late open responses cannot
make both presentations visible together.

## Native behavior and scope

The workstation and Lore windows use real Aether menus and their original
renderers. Character reuses the player's existing crafting, armor and offhand
slots, so mode changes retain crafting contents and the shared cursor.
Equipment uses the exact server-side native slots, preserving item
validation and equipped-item behavior. Client snapshots use shadow slots so
desktop synchronization cannot write over the player's live equipment.
Visibility actions pass through the existing server session validation and the
native equipment backend. Normal crafting, armor and inventory slots are not
duplicated. The authenticated equipment session is kept in the desktop's
session registry without constructing an equipment window. Version-local
Compatibility mixins route its slots through the existing Character and
leave Core and its public API unchanged.

The Fabric release uses Accessories for its equipment backend. Forge uses
Curios. Their visibility switches use their respective original renderer or
texture. Treasure chests continue to use SIU's existing vanilla chest support.
Sun Altar configuration, Valkyrie dialogue, Moa Skins and title menus retain
their native non-inventory presentation.

Tested publisher releases:

- Fabric: Aether `1.20.1-1.5.2-beta.9-fabric`, Accessories
  `1.0.0-beta.48+1.20.1`, Cloth Config `11.1.136`.
- Forge: Aether `1.20.1-1.5.2-neoforge`, Curios `5.14.1`, Quark `4.0-462`,
  Zeta `1.0-31` and Quark Oddities `1.20.1`.

The minimal Fabric profile exposed an upstream Accessories beta48 renderer
reference to Sodium's `VertexBufferWriter` when equipping a visible cape.
Final cape tests therefore include Sodium `0.5.13` and Indium `1.0.36` in the
disposable test profile. Aether and Accessories publisher JARs are unmodified.
No Sodium dependency or workaround is added to SIU's shipped JARs.
The pinned [Accessories release](https://modrinth.com/mod/accessories/version/A7wwzJ0e)
and its renderer bytecode are recorded with the test evidence.

## Focused verification

Current Character-mode evidence and exact tested artifact hashes are in
[verification.json](../output/aether-character-mode/verification.json).
The focused scripts are in `functional-tests/aether-windows`.
The Character revision has focused checks for mode switching, exclusive
general panels, real equipment transfers, crafting across mode changes,
relocated armor/offhand/recipe controls, crown return, closing/reopening,
movement and the independent Backpack. Two layout tests check the native
coordinates and lack of overlap. Eighteen complete slot frames and ghosts per
loader are compared with native captures.

The final production builds passed **83 focused in-game assertions**: 34 on
Fabric, 39 on Forge, plus five generic-launcher checks per loader with Aether
absent. These include eight alternating-launcher cycles on each loader. Both
layout tests and all 36 slot-artwork comparisons passed. Full-screen captures
show [Aether mode on Fabric](../output/aether-character-mode/aether-character-fabric.png)
and [Character with the independent Inventory/Backpack on Forge](../output/aether-character-mode/aether-character-forge-and-backpack.png).

The revised 1.20.1 Compatibility JARs are staged in `dist/compatibility` and
included in `dist/salts-inventory-all-latest.zip`. Other bundle entries and
the existing Core/Controlify artifacts are preserved byte for byte.

The [initial workstation/Lore verification](../output/aether-implemented/verification.json)
is historical evidence for the first Aether implementation; its separate
equipment-window presentation has been superseded by Character mode.

The initial checks cover Altar/Freezer processing, active incubation and Lore
inspection/navigation. These unchanged workstations were not rerun for the
Character revision. Artwork comparisons allow one RGB level of framebuffer
rounding.

These are affected-menu checks on integrated servers at GUI scale 2, not a
complete gameplay suite. Dedicated multiplayer, other GUI scales, arbitrary
equipment-slot datapacks, supporter-only customization and a full Moa hatch
cycle were not exercised. Later Minecraft releases are outside this change.

Full-screen in-game screenshots are saved under
[output/aether-character-mode](../output/aether-character-mode).
