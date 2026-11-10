# Powah! windows — Minecraft 1.20.1

Implements the approved Powah! mockups for **Powah 5.0.11 on Fabric and Forge**.
The eleven concrete menu types cover twelve opening routes because Ender Gate
shares Ender Cell's menu. Newer Minecraft releases are not part of this pass.
All shipping changes are in Compatibility; Core and the public API are unchanged.

The later [all-target rollout](powah-all-targets.md) adds the applicable modern
NeoForge releases. This document retains the original 1.20.1 evidence.

| Menu | Retained working panel |
| --- | --- |
| Reactor | Five native slot roles, FE/coolant gauges, resource indicators and generation mode |
| Energy Cable | The actual clicked face, its energy direction and redstone control |
| Ender Cell / Ender Gate | Native teal artwork, energy display, charge/discharge cells and tier-limited channel strip |
| Energy Cell | Energy display and two charge/discharge cells |
| Energy Discharger | Energy display and all seven native battery cells |
| Energy Hopper | Wide native energy display and face controls |
| Furnator | Fuel cell, burn indicator, FE gauge and charge cell |
| Magmator | Complete lava tank, energy instrumentation and charge cell |
| Player Transmitter | Binding-card cell, bound-player indicator and energy display |
| Solar Panel | Wide native energy display and face controls |
| Thermo Generator | Complete coolant tank, generation text, FE gauge and charge cell |

## Presentation and behavior

- Native working artwork, slot silhouettes, ghost icons, animations and relative
  positions remain intact. SIU frames use the native gray or Ender teal palette.
  Transparent atlas gaps are masked from the captured native screen so world
  pixels and screen dimming cannot become window material.
- The embedded player inventory is omitted; SIU's inventory opens separately.
  Native external energy-face and redstone controls follow their parent and
  retain their original callbacks and tooltips. Reactor's generation-mode tab
  remains attached on the left.
- Normal SIU management controls are provided; narrow headers use More and
  Close through the existing window manager. Machine cells do not become
  generic storage-sort targets.
- Original menus retain item restrictions, real tile state, quick move and
  native transactions. Ender Cell's off-screen capacity-upgrade helper remains
  operational through quick move without inventing a visible third slot.
- Native actions route through the existing public authenticated payload API.
  Packet size, tile ownership, cable face, channel limits and reactor-only
  actions are checked before invoking the publisher's handlers. A small optional
  Cable opening mixin retains the clicked face on the server menu.
- No hard dependency is added when Powah is absent. Resource reload clears
  masks and regenerates tinted frame textures; closed windows release them.
- Powah's immediate-mode fluid draw bypasses native GUI capture. Its original
  tank renderer is reused on the final public API surface with the native tint,
  capacity, fill amount and bounded tank area, including Reactor coolant.

## Focused verification

The [verification record](../output/powah-implemented/verification.json) records
the exact tested and distributed JAR hashes, source revision, test journals,
per-loader checks and coverage limits. The
[comparison PDF](../output/pdf/siu-powah-implemented-windows.pdf) contains all
twelve original/implemented views, Forge examples, active machinery and full
game screens. Every UI image is an actual game capture.

Focused checks on both production loaders cover all twelve opening routes,
approved dimensions, complete native cell/control mapping, exclusion of
embedded player/helper cells, individual/all-face changes, redstone modes,
non-default cable faces, Ender Cell/Gate channels, energy-item transfers,
unbound-card rejection and bound-card NBT retention. Active-state checks cover
Furnator generation, formed Reactor controls, bucket-filled Magmator/Thermo
tanks and their bottom-edge tooltips. Window checks cover pinning, unlocking,
moving, attached input, independent close/reopen and Ender Cell capacity
upgrades through SIU's public quick-move route. Final build checks repeat all
four native action packet families and representative alternate GUI scales.

Stable native artwork comparison checks **150,142 opaque working-area pixels
across 24 Fabric/Forge views**, with zero differences from the recorded native
references. Live numeric values, fills and animations are excluded from that
comparison. Publisher working-body textures are identical across the two
loaders.

These are affected-window checks, not a full functional or gameplay suite.
Representative tiers and states are exercised; dedicated multiplayer, every
automation configuration, alternate resource packs and optional recipe-viewer
combinations are not exhaustively tested. The manual, recipe pages, Energizing
Orb/Rod interactions and world overlays retain their native flows.

Publisher baseline: [Powah 1.20.1 source at the reviewed revision](https://github.com/Technici4n/Powah/tree/b7343d065eec0c57c4bd99dcde7bbdbccccf1307).
The excluded fixtures and observation helpers are not packaged in the jars.
