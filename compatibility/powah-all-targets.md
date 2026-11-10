# Powah! windows across supported targets

The approved eleven menu layouts, covering twelve opening routes, are available
for every official Powah release target that overlaps SIU's target set.
Core and the public API are unchanged.

| Minecraft | Loader | Tested Powah release | Coverage |
| --- | --- | --- | --- |
| 1.20.1 | Fabric | 5.0.11 | Prior focused live checks retained |
| 1.20.1 | Forge | 5.0.11 | Prior focused live checks retained |
| 1.21.1 | NeoForge | 6.2.10 | All twelve opening routes and focused live checks |
| 26.1.2 | NeoForge | 7.0.4-alpha | All twelve opening routes and focused live checks |

Availability was checked November 2, 2026 against the publisher's
[Modrinth releases](https://modrinth.com/mod/powah/versions) and
[CurseForge releases](https://www.curseforge.com/minecraft/mc-mods/powah-rearchitected/files/all).
The 26.1.2 release is alpha. There are no official Powah releases for SIU's
1.21.11, 26.2 or 26.3 targets, nor for Fabric on the newer supported Minecraft
versions. Older Minecraft releases outside SIU's target set are not new Core
ports. The complete twelve-target table is saved with the evidence.

## Modern adaptations

- The native working atlases are identical across the reviewed 5.x, 6.x and
  7.x publisher jars. The approved gray/teal frames, native slot artwork,
  channel strips, gauges and attached controls retain their layout.
- The 26.1.2 release renames container classes to menus and exposes the owning
  tile through `blockEntity`. Reconstruction and cable-face retention use
  these actual names. Native image color access, dynamic frame registration
  and the NeoForge reload-listener event match the newer client APIs.
- The modern packets are typed payload records. Compatibility uses the
  publisher's stream codecs, checks the authenticated owner, position, face,
  channel and applicable menu, and invokes its original action handlers.
- New native tank clicks are preserved: left-click fills from the shared cursor
  or fills an empty carried container; right-click drains into it. The original
  item/fluid capability rules stay in charge. Cursor results are committed
  through SIU's public shared-cursor API.
- The 1.21.1 native fluid draw bypasses GUI capture, so its publisher renderer is
  reused on the final surface with native tint and fill. The 26.1.2 renderer
  already uses the new GUI extractor and needs no duplicate overlay.
- Binding cards retain their native bound-player data components. Ender Cell's
  hidden upgrade helper remains usable through quick move without adding a
  visible helper slot. Non-container guide and world interfaces retain their
  original handling.

## Focused verification

Both newer targets passed 125 focused live assertions each using the original
publisher jars and unchanged shipping Core jars. Checks cover all twelve
opening routes, dimensions, complete native slot/control mapping, exclusion of
embedded player cells, all-face and individual-face controls, three redstone
modes, non-default cable faces, Ender Cell/Gate channels, energy-item transfer,
binding-card validation and component preservation, active generation, formed
Reactor controls, tank fills, native quick-move upgrades and window lifecycle.

The native tank fill/drain action is checked separately on Reactor, Magmator
and Thermo Generator, including the returned bucket and empty-tank state.
Saved image checks verify visible fluid color and the last fill row. The full
window galleries and active captures were visually inspected.

This is focused affected-window verification, not a complete gameplay suite.
Dedicated multiplayer, every tier/automation configuration, alternate resource
packs and optional mod combinations were not exhaustively tested.

Evidence: [verification and exact release hashes](../output/powah-all-targets/verification.json),
[all SIU targets](../output/powah-all-targets/compatibility-table.md),
[1.21.1 windows](../output/powah-all-targets/1.21.1-all-windows.png),
[26.1.2 windows](../output/powah-all-targets/26.1.2-all-windows.png).
The [1.20.1 implementation report](powah-1.20.1.md) retains the baseline evidence.

Only the two additional Compatibility jars and release checksums change in this
follow-up. The combined bundle also receives the previously delivered 1.20.1
Powah jars. Core, Controlify, other Compatibility jars and unrelated bundle
entries retain their previous bytes. Test observers are excluded from releases.
