# Ad Astra and Giselle Addon native windows

Implemented November 3, 2026. All fourteen approved Ad Astra inventory screen
types and all four Giselle Addon screen types use the original native metal
frames, materials and working apparatus. The player inventory is separate.

| Minecraft | Loader | Ad Astra | Giselle Addon | Screen types |
| --- | --- | --- | --- | ---: |
| 1.20.1 | Fabric | 1.15.21 | 6.19 | 14 + 4 |
| 1.20.1 | Forge | 1.15.21 | 6.20 | 14 + 4 |
| 1.21.1 | Fabric | 1.16.26 | 8.2 | 14 + 4 |
| 1.21.1 | NeoForge | 1.16.26 | 8.1 | 14 + 4 |

These are all publisher release targets overlapping SIU's supported targets.
The fourteen base container atlases and four add-on container atlases are
identical across these releases. Minecraft 1.21.1 uses Common Storage Lib in
place of the older Botarium dependency.

## Presentation

- Preserve each complete native circuit, casing, slot surround, gauge and
  attached charging/options bar. Do not substitute generic slot backgrounds.
- Preserve the raised Solar Panel and Oxygen Distributor monitors, including
  their pipes and the Distributor toolbar's overhang.
- Put SIU controls in the matching native title area. Use More/Close when the
  available title space cannot fit the normal controls.
- Gear mode reveals an attached matching drawer containing the original
  configuration controls. It follows its machine window and has no separate
  movement or close controls. The inventory-free Rocket Sensor also receives
  enough space for the original configuration control group.
- Keep the original NASA click-to-craft preview behavior. Preserve ordinary
  cargo, fuel-container ports and the Lander's take-only recovery slots.
- Keep Planets navigation native rather than turning it into an inventory
  window. Book and overlay interfaces remain outside this inventory scope.

## Integration and validation

Production changes are confined to Compatibility. Core, the public API and
other release jars are unchanged. Original menu opening data, native menus,
native widgets and native packets remain in use.

The native machine-packet resolver additionally recognizes a player's live SIU
menu session. It retains the original distance limit, checks the requesting
player's level, exact block position and block-entity identity, and rejects
removed or no-longer-valid menu owners. Port selection targets the original
configuration widget; it does not depend on the globally open Minecraft
screen being the native screen during SIU input dispatch.

Vehicle sorting includes only the eight Rocket or sixteen Rover cargo cells.
Fuel input/return cells and every Lander recovery cell are excluded. Invisible
configuration port selectors are active only while Gear mode is active, so
they do not intercept ordinary item input.

Focused live checks cover all eighteen windows on each target, including all
107 non-player native slot envelopes per target, inventory separation, real
item insertion/extraction, NASA crafting, add-on fluid containers, gravity and
sensor controls, side configuration/reset, cargo sorting and permissions, and
SIU movement/focus/pin/lock/close controls. These are focused checks of the
changed windows, not a complete gameplay suite. Dedicated-server multiplayer,
every recipe/automation combination, resource packs, controllers and optional
mod combinations are not exhaustively covered.

The excluded test observer prepares recipe ingredients and invokes the native
NASA recipe-cache update during fixture setup. Crafting and interaction checks
then use actual input events. It is not included in shipping jars.

Evidence:

- [Approved geometry](../output/ad-astra-window-design/manifest.json)
- [Focused verification and tested jar hashes](../output/ad-astra-implemented/verification.json)
- [Target coverage](../output/ad-astra-implemented/compatibility-table.md)
- [Actual screenshot comparisons](../output/pdf/siu-ad-astra-implemented-windows.pdf)

Publisher sources: [Ad Astra releases](https://modrinth.com/mod/ad-astra/versions)
and [Giselle Addon releases](https://modrinth.com/mod/ad-astra-giselle-addon/versions).
