# Alex's Mobs Transmutation Table — Minecraft 1.20.1 Forge

The extracted native panel is now the window itself. There is no added strip
above it. The original 16-pixel title row provides dragging and SIU controls,
with the green "Transmutation Table" title left-aligned at x=6. That row is
drawn directly from the installed unlabeled GUI atlas, preserving its border
and circuitry without remnants of the old centered text.

The complete original 176 by 107 panel retains its upper, side and lower borders,
all three outcome strips and icons, cost text, hover feedback, circuitry and
green input socket. Native body rows 16 through 106 join their own title row
without moving or resizing the native controls. Native row 107 begins the gray
player-inventory section and is excluded. Player inventory remains available
as its ordinary separate SIU window. The final window is 176 by 107 GUI pixels,
16 pixels shorter than the previous implementation with an added drag strip.

The corner cleanup masks only the ten native pixels belonging to the old
inventory behind the two lower curves. Each corner loses a two-pixel step on
row 105 and a three-pixel step on row 106, including its continuing inventory
edge. The world shows through those steps. The window keeps its full 176 by 107
bounds, side artwork, purple curve and bottom lip; no entire row or column is
cropped and no background color is painted over the corner.

## Native behavior and ownership

The adapter uses the original menu, real input Slot, item predicates, native
screen and native choice widgets. Normal opening, item insertion/removal,
native quick-move rules, closing and validity checks stay with the mod.
The input is a machine input rather than sortable storage.

The original choice callback sends an unscoped packet assuming the player's
current container is the table. Only while this adapter dispatches that native
callback, a compatibility mixin sends a one-byte authenticated SIU action
instead. The server validates the index and live input/possibilities, then calls
the original `transmute` method. The native method still owns XP requirements,
conversion quantities, learning, rerolls, sounds and advancements.

Each server session synchronizes its actual three possibilities to its own
window. The native screen reads a global proxy, so the adapter restores that
window's choices before painting and input, and reconciles the same original
button-visibility rule. No UI context is retained outside an input callback.
Native screens opened without SIU dispatch retain their ordinary callback.

No Core implementation or public API was changed. Forge compilation also
required marking the preceding Aircraft Core-target mixins optional (`@Pseudo`)
because the Compatibility compile classpath contains the public API only;
their filters and runtime behavior were not changed.

## Focused validation

The tested release is Alex's Mobs **1.22.9** with Citadel **2.6.3** on Minecraft
**1.20.1 Forge**. The disposable named Forge client loads the existing Core
0.1.6 distribution and rebuilt Compatibility 0.1.3. The established launcher
transforms SRG names to the development namespace; original and transformed
hashes and unchanged-resource provenance are retained in its lineage file.
This is not a production-namespace client claim.

The initial implementation passed twenty focused checks. The native-header
revision passed 22 focused checks: native dimensions and body/header alignment,
controls within the original title row, inventory omission, item insertion
through separate SIU Inventory, all three live previews and native buttons,
actual dragging of the native title row, all three successful conversions with
native XP/learning/reroll updates, insufficient XP, GUI scale 3, input return on
close and ordinary block reopening. Full-screen images cover empty, usable and
insufficient-XP states at GUI scales 2 and 3. The scale-3 screenshot was retaken
with the reopened window clear of the fractional viewport edge, preserving the
complete right border. An empty-body comparison matched all 64,064 opaque
screen pixels to the original atlas; the first title pixel is at native x=6.

The observer uses operator commands for disposable block/item/XP fixtures and
simulates ordinary menu input. It does not create an artificial subject screen
or force a menu provider. An initial observer tried reading nonexistent private
content-size helpers; its observation was corrected to use the real content
origin. The original first-pass image and failed observer output remain in the
build folder. The final validation used the corrected observer and final build.

Forge reobfuscation and Fabric remapping both pass. Fabric compilation is an
isolation check, not an Alex's Mobs Fabric implementation. Dedicated-server
multiplayer, physical controller or keyboard-only navigation, multiple tables
open concurrently, other mod releases and resource packs were not tested.
No complete functional/gameplay suite was run.

The earlier added-strip implementation is preserved in
`output/alexsmobs-transmutation-implemented/`. The revised native-header build,
focused evidence and exact tested hashes are in
`output/alexsmobs-transmutation-native-header/`.
The subsequent corner-only cleanup is in
`output/alexsmobs-transmutation-clean-corners/`, with separate focused visual
checks at GUI scales 2 and 3. The earlier conversion/gameplay checks are not
rerun for this small mask change.
