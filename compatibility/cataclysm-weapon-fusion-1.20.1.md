# Cataclysm Weapon Fusion — Minecraft 1.20.1 Forge

The fusion machine now sits inside a recessed opening in a dark gray SIU
window. The outer shell uses RGB **70, 73, 79**, taken from the approved
mockup. Its inset uses SIU's existing player/model preview outline rather
than a new border design. Both textures derive their shadows and highlights
from the same gray palette. The inset center is transparent, and the shell
is drawn only outside the opening; there is no black portrait background or
opaque gray backing underneath the native panel.

The native machine remains at its original scale and layout on a 176 × 71
canvas. Following the user's refinement, the walls extend inward by five
GUI pixels on every side. The visible opening is 166 × 61, using native
source coordinates (5,5) through (170,65). This conceals the original outer
frame and rounded corners beneath the matching SIU rim. Source and destination
offsets are equal, so the hammer, special sockets, plus, arrow and complete
invalid-recipe marker retain their positions and size. The circuitry meets
the inner walls. No native player inventory is included.

The old centered caption is replaced with its exact underlying atlas pixels;
the original localized title appears once in the SIU header, retaining
Cataclysm's red formatting. Final window dimensions remain 194 × 104 GUI pixels,
and the logical content origin remains 9 pixels from the left and 24 from the
top. Only the aperture has narrowed. Player inventory opens separately.

## Implementation

`WeaponFusionWindows` uses the public native-template API and the original
`WeaponfusionMenu`. Native slots, recipe lookup, result consumption,
quick-move policy, closing and validity checks remain owned by Cataclysm.
Machine inputs/results are not classified as sortable bulk storage. No new
action packets, server crafting logic, Core changes or public API changes
were needed. The shared `RecessedNativeChrome` compatibility utility owns its
derived textures, releases them when the window closes, and regenerates
them after a resource reload.

This delivers **Weapon Fusion only**, not every Cataclysm interface or other
Minecraft versions. The integration loads conditionally when Cataclysm is
installed. Fabric remapping checks optional-mod isolation; it is not a claim
that Cataclysm's Forge release runs on Fabric.

## Focused validation

The tested fixture is Minecraft **1.20.1 / Forge 47.4.0**, Cataclysm **3.31**,
Lionfish API **3.0**, Curios **5.14.1+1.20.1**, Core **0.1.6** and Compatibility
**0.1.3**. The initial isolated fixture lacked Curios; this release references
its classes during startup despite not declaring a mandatory dependency.
The successful fixture uses the same pinned Curios jar as the original pack.

The established excluded observer opens the real block through ordinary
interaction, drives normal screen clicks and drags, and reads client/server
state. Operator commands only prepare disposable items and blocks. The
launcher remaps release jars from SRG to the development namespace; this is
an in-game development-client check, not production-namespace or multiplayer
validation. The artifact and remap lineage files retain exact hashes.

Seventeen focused checks passed:

- The dedicated native panel has all three machine slots and a transparent inset.
- Every opaque pixel inside the visible machine opening matches the original
  atlas at GUI scales 2 and 3, including the cleaned former title area.
- Shell and rim colors match the specified gray palette at both scales;
  the original frame is covered on all four sides.
- Ordinary insertion accepts invalid input without producing a result; all
  173 opaque pixels of the native invalid-recipe cross remain visible.
- Dragging moves the native panel and its slot hit positions together.
- Infernal Forge + Void Core produces Void Forge, and Ignitium Chestplate +
  Elytra produces Ignitium Elytra Chestplate. Taking either result consumes
  both inputs and transfers the original result into separate SIU Inventory.
- Closing returns an unused input; ordinary reopening restores a clean panel.

Forge reobfuscation and Fabric remapping both succeeded. The packaged release
contains no observation/QA classes. The preexisting Forge Core jar retained
SHA-256 `d12449e25700077fd8d12a69fbc0cf43c16aa7ce60953b881f86033f435a9bd4`.
No complete functional/gameplay suite ran. Dedicated-server multiplayer,
shift-click variants, keyboard-only navigation, concurrent fusion windows,
other mod releases and resource packs were not tested.

The first recessed revision remains in `output/weapon-fusion-recessed/`.
The refined build, full-screen captures, checks and provenance are in
`output/weapon-fusion-recessed-inset/`. The focused driver is
`functional-tests/weapon-fusion-recessed/focused.py`.
