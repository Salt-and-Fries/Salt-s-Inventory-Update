# Kibe windows across supported targets

The approved Kibe windows are implemented wherever an official Kibe release
overlaps SIU's supported Minecraft versions and loaders. Core and the public
API are unchanged.

| Minecraft | Loader | Kibe release | Verification |
| --- | --- | --- | --- |
| 1.20.1 | Fabric | 1.10.1-BETA+1.20 | Previous 184 focused checks retained |
| 1.21.1 | Fabric | 2.3.1 | All twenty-two views and focused live checks |
| 1.21.1 | NeoForge | 2.3.1 | All twenty-two views and focused live checks |

Availability was checked November 2, 2026 against the publisher's
[Modrinth releases](https://modrinth.com/mod/kibe/versions) and
[CurseForge files](https://www.curseforge.com/minecraft/mc-mods/kibe/files/all).
There is no official Kibe release for SIU's 1.20.1 Forge, 1.21.11, 26.1.2,
26.2 or 26.3 targets. Older Minecraft releases outside SIU's target set are
not additional Core ports. The [complete target table](../output/kibe-all-targets/compatibility-table.md)
separates availability from implementation and verification.

## Adaptation to Kibe 2.x

- The thirteen custom screen types remain, but all Cobblestone and Basalt
  Generator tiers now share `kibe:block_generator`. Their native entity decides
  which approved width to use; every tier retains its full apparatus and grid.
- Cooler food uses the native container data component. The existing public
  held-item source lease permits legitimate component updates while protecting
  the source slot. The optional feeding guard targets the actual published
  method names on Fabric and NeoForge and preserves native pause/resume behavior.
- Entangled Chest rune colors are now an array; Bag channels use Kibe's typed
  `rune_set` component. The windows use the newer atlas's complete eight-by-ten
  rune glyphs, with native order, colors, key and ownership behavior.
- Native working atlases are identical between the two reviewed 2.3.1 loaders.
  Most are identical to 1.20.1; the Entangled artwork has changed and is read
  from the installed release. Animated generator bars and the filled XP gauge
  remain native renders, including their complete borders.
- Original factories, hand/stack or block-position opening data, menus, slot
  restrictions, transactions and server behavior remain in charge through
  Core's existing transport. No replacement machine protocol is added.
- The approved cyan, teal, gold and charcoal frames retain ordinary SIU window
  management. Narrow headers use the normal adaptive More/Close arrangement.
  The Pocket Crafting Table reuses vanilla crafting support. Chunk Loader
  configuration remains a native screen.
- NeoForge is tested with Kibe's Forgified Fabric API dependency and Kotlin for
  Forge. These are third-party dependencies to install separately; they are
  not bundled into Compatibility.

## Focused verification

Each additional loader passed **184 focused live assertions**, covering all
twenty-two views and all 375 native slot envelopes, plus native transfers,
disposal, restrictions, representative shift transfer, Cooler feeding and
component persistence, offhand use, XP conversion and collection, colored Bag
linkage and shared storage, generation, crafting, native-screen preservation,
window movement and management, torch supply, Drawbridge cover and all Wither
Builder ingredient cells. This adds 368 checks to the retained baseline.

Fresh references without SIU were captured for all twenty-two 2.3.1 views.
Comparisons against both loaders matched **80,128 stable working-area pixels**
with zero differences. Animated fills, item interiors and count-glyph overhang
are excluded. The intentional new frames, titles and relocated rune row are
outside that working-area comparison.

- [Fabric window gallery](../output/kibe-all-targets/1.21.1-fabric-siu-kibe-modern-01/all-windows.png)
- [NeoForge window gallery](../output/kibe-all-targets/1.21.1-neoforge-integrated-kibe-modern-01/all-windows.png)
- [Fabric active states](../output/kibe-all-targets/1.21.1-fabric-siu-kibe-modern-01/active-windows.png)
- [NeoForge active states](../output/kibe-all-targets/1.21.1-neoforge-integrated-kibe-modern-01/active-windows.png)
- [Exact artifacts, journals and coverage](../output/kibe-all-targets/verification.json)

These are affected-window checks, not a complete gameplay suite. Original
publisher and shipping subject jar bytes are used in disposable integrated
server worlds with excluded observers. Dedicated multiplayer, every world
automation/redstone configuration, spawning a Wither, all timing profiles,
alternate resource packs, translations, controller input and optional mod
combinations are not exhaustively tested. The native reference is captured on
Fabric; byte-identical working atlases and separate live NeoForge checks support
the cross-loader artwork comparison.

Only the two new Compatibility artifacts, their checksum entries and matching
combined-bundle entries are replaced. The existing 1.20.1 Kibe jar, Core,
Controlify and unrelated release artifacts keep their prior bytes. The
[baseline implementation report](kibe-1.20.1.md) retains its original evidence.
