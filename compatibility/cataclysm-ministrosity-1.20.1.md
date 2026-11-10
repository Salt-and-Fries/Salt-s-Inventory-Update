# Netherite Ministrosity — Minecraft 1.20.1 Forge

The companion inventory follows the approved mockup: a warm brown SIU shell
(RGB **59, 51, 45**), the actual pet in a dark portrait on the left, and the
original gold 5×3 chest on the right. The portrait has a uniform dark brown
rim (RGB **41, 37, 33**), with no white line on any side. Its black interior
uses RGB **24, 22, 23**. The full window is 202 × 112 GUI pixels.

The chest's ears, lid, red corner and gold cells come from the installed
`ministrosity2.png` texture. The native screen supplies the actual 15 slot
backgrounds and normal item interactions. The clean atlas supplies its shaped
outer frame, avoiding fragments of the old model or heading in transparent
areas. Only the two former player-inventory joins are excluded; their three
exposed edges are closed as in the mockup. Player inventory is separate.

The portrait renders the owning live entity with Minecraft's normal inventory
renderer, at scale 36 with a centering correction for this model. It retains
the mod's renderer, skin and animations. It is clipped to its own viewport;
the centered model has clearance on all sides at GUI scales 2 and 3.

## Native behavior

Cataclysm's original `MinistrostiyMenu` has no registered menu type and opens
through a custom packet. The existing compatibility bridge for untyped entity
menus now also handles this companion. A conditional opening mixin captures
the original inventory-opening method after the mod's interaction/ownership
checks. When SIU does not capture it, the original opening continues normally.

The bridge registers a compatibility menu type, carries the actual entity ID
and container size, and constructs the original menu and native screen for
that entity. Its 17-position container has 15 visible storage slots at native
indices 2–16. The two hidden positions are not exposed as cargo. The client
uses that pet's own inventory to preserve the native container-identity check.
Opening sets the native chest-open animation; the original menu's removal
method still handles the close animation. Original alive, inventory-identity
and eight-block validity rules remain authoritative.

Both sides explicitly classify the fifteen non-player cells as storage for
SIU sorting. The actual `MinistrositySlot` placement predicate continues to
enforce Cataclysm's blacklist. No custom inventory copy, crafting logic, Core
changes or public API changes were introduced. The other entity bridges keep
their existing behavior. This targets Cataclysm **3.31** on **1.20.1 Forge**;
it does not claim other mod releases or loaders.

## Focused validation

The disposable Forge 47.4.0 fixture uses the original pinned Cataclysm 3.31,
Lionfish API 3.0, Curios 5.14.1+1.20.1, Core 0.1.6 and Compatibility 0.1.3.
The excluded observer awakens/tames a real spawned companion using a Lava
Power Cell, then opens it through ordinary entity interaction. Operator
commands prepare disposable items, the pet and range-test teleports. Screen
clicks, drags and SIU controls exercise its real inventory.

Focused checks cover:

- The dedicated companion template, all 15 native slots, ownership and open animation.
- Exact chest artwork and gold-cell pixels at GUI scales 2 and 3, including
  transparent areas, the removed inventory joins and the restored frame edges.
- A continuous dark portrait perimeter and a complete live model at both scales.
- Native shulker-box rejection and safe return of the rejected item.
- Individual insertion into all 15 displayed cells through separate SIU Inventory.
- SIU sorting within the companion and transfer of matching items into it,
  while the blacklisted item stays in player inventory.
- Correct cell clicks after dragging, persistent contents and native close animation.
- Ordinary reopening of the same pet, and automatic closure beyond native reach.

Forge reobfuscation and Fabric remapping succeed. Fabric is an optional-mod
compile/isolation check, not a Cataclysm Fabric runtime claim. The established
launcher remaps release jars from SRG to the development namespace; exact
release and remapped hashes are retained in the artifact lineage files.
This is an in-game development-client check, not production-namespace or
dedicated-server multiplayer validation. No complete gameplay suite ran.
Physical mouse/controller navigation, every shift-click variation, concurrent
companions, alternate resource packs and other releases were not tested.

The first visual passes exposed a native-model fragment, portrait centering
and missing storage classification; those were corrected before final capture.
The range case was repeated with a longer solid test platform after the
initial scene-restoration step let the test player fall. The restored pet's
contents were reverified before the final capture; this was a fixture correction.
The focused driver is `functional-tests/cataclysm-ministrosity/focused.py`.
The tested build, screenshots, results and provenance are saved in
`output/cataclysm-ministrosity-implemented/`.
