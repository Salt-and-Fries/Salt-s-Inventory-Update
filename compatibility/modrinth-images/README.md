# Compatibility page screenshots

These images accompany [the Modrinth page draft](../MODRINTH.md). They were
captured in live Minecraft clients on 2026-10-20 using Core and Compatibility
0.1.4 and the corresponding third-party mods.

| Image | Interface | Minecraft / loader |
| --- | --- | --- |
| `ae2-pattern-terminal.png` | AE2 pattern encoding terminal and toolbox | 26.3 / NeoForge |
| `toms-crafting-terminal.png` | Tom's Simple Storage crafting terminal | 1.21.1 / Fabric |
| `sophisticated-backpack.png` | Backpack, crafting upgrade and player inventory | 1.21.1 / NeoForge |
| `jei-recipe-browser.png` | JEI crafting recipe in the desktop recipe browser | 1.21.1 / Fabric |

The screenshots are cropped around the interfaces, including protruding tabs,
native borders and companion panels. They retain their original pixels; no
rescaling, retouching or generated interface elements were used.

Upload the four PNGs through Modrinth's description editor and replace the
draft's relative image paths with the resulting hosted URLs. Remove the hidden
publishing comment from the page before publishing. The screenshot captions
identify the illustrated version rather than implying identical designs on
every target.

`sources.json` records the source captures, crop rectangles and SHA-256 hashes.
Original framebuffer captures and scoped capture reports are retained locally
under `build/compatibility-marketing/2026-10-20`. These are focused screenshot
checks, not a complete gameplay acceptance suite.
