# Enchanted: Witchcraft windows - Minecraft 1.20.1

Implements the approved five-screen mockup set for Enchanted **3.1.14** in the
Compatibility add-on. Core and the public SIU API are unchanged. The native
menus still own recipes, slot restrictions, power consumption and processing.

| Window | Final size | Retained native working area |
| --- | --- | --- |
| Witch's Oven | 150 x 90 | Five slots, both ghost jar icons, heat and progress |
| Spinning Wheel | 164 x 80 | Three inputs, result, curved arrows and animated wheel |
| Distillery | 148 x 111 | Jar input, two ingredients, four results, branches and bubbles |
| Poppet Shelf | 136 x 58 | Four independently spaced poppet slots |
| Altar | 160 x 64 | Live current/max power and recharge multiplier |

These widths remove spare side padding from the approved mockups. The normal
five SIU controls fit on the larger titles; the compact Shelf uses More and
Close. The four item menus use the separate SIU Inventory window. The Altar has
no item slots. Guidebooks, world interactions and recipe-viewer screens remain
native.

All five network constructors receive the original owning block position. The
Shelf's position comes from its persistent `PoppetShelfInventory`, rather than
casting that wrapper to a block entity. This preserves the original shelf
inventory and item restrictions. Blank/sturdy/infused crafting dolls do not
become valid shelf items merely because SIU is installed.

The Distillery retains the top and bottom output borders. Its old centered
title and lower-left Inventory label are removed from the extracted working
area, restoring their underlying native texture pixels.

## Focused verification

- Five geometry tests passed on the final build: complete slot borders,
  original inverse input mapping, processing artwork, title removal and
  exclusion of the integrated player inventory.
- Thirty-three distinct live assertions passed in a production Minecraft
  1.20.1 Fabric client with an integrated server, Enchanted 3.1.14, SIU Core
  0.1.6 and Compatibility 0.1.3, at GUI scale 2.
- Live checks covered all five windows, movement, Close/reopen, separate
  inventory transfers, Oven charcoal production, Wheel poppet production,
  all four Distillery recipe outputs and synchronized Altar power values.
- All 20 empty slots, including native ghost artwork, matched 6,480 publisher
  texture pixels. After the Distillery label cleanup, 13 Altar/Distillery live
  assertions passed again; its seven slots and 261 label-area pixels were
  rechecked. All five windows were captured together on the final build.
- Fabric remapping and Forge compilation/reobfuscation passed. The exact
  publisher Forge release also contains the native menu constructors and
  owner accessors used by this integration.

Power for the processing checks was supplied through the native Altar's saved
fields in a disposable test world. This checks native synchronization and
recipe processing; it does not certify every nature/upgrade power source or
the combat effects of every poppet. Forge gameplay, dedicated multiplayer,
other GUI scales and the complete gameplay suite were not run.

Evidence: [verification manifest](../output/enchanted-implemented/verification.json),
[all five live windows](../output/enchanted-implemented/all-five-windows.png).
The separate journal files in that output folder preserve the initial checks
and final focused rechecks. Delivered PNGs are unchanged game screenshots.

Reproduction:

- `functional-tests/enchanted-windows/focused.py`
- `functional-tests/enchanted-windows/verify_artwork.py`
- `functional-tests/enchanted-windows/capture.py`
- `tools/stage-enchanted-release.py`

The two 1.20.1 Compatibility JARs, checksums and corresponding entries in
`dist/salts-inventory-all-latest.zip` are updated. Other bundle entries and the
Core/Controlify distributions retain their previous bytes.

Publisher references: [Fabric 3.1.14](https://modrinth.com/mod/enchanted-witchcraft/version/JQc2jrQz),
[Forge 3.1.14](https://modrinth.com/mod/enchanted-witchcraft/version/lItoT82o).
The subsequent [all-target port](enchanted-all-targets.md) adds Enchanted 4.2.7
on 1.21.1 Fabric and NeoForge, while preserving these legacy JARs and evidence.
