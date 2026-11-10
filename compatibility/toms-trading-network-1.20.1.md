# Tom's Trading Network — Minecraft 1.20.1

Both vending screens keep their original arrangement, gray palette, artwork,
labels, fields, buttons and slot backgrounds. The player-inventory section is
removed and SIU supplies its normal frame and controls. Player inventory opens
as its usual separate window.

- Configuration: the native rectangle `(4,4,168,114)`, containing all eight
  ghost settings and sixteen real stock/payment cells, becomes a 184×146
  window. The native name field and five side controls remain in place.
- Customer trading: `(4,17,168,65)` becomes a 184×97 window. The native title is
  replaced by SIU's title and the old inventory caption is removed. All eight
  offer cells, the arrow, quantity field and Trade button keep their layout.
- The original creative-vending button stays above the frame when available.
- The original right-click rule popup retains count editing, NBT matching and
  tag-filter conversion. It paints above the frame, accepts input beyond the
  panel, and dismisses through the desktop's popup ownership hooks.

This is a Compatibility-only adapter; Core and the public API are unchanged.
Native menu slots, owner/range checks, stock storage and transaction methods
remain authoritative. Offer/ghost cells are virtual, not sortable storage.
Native configuration messages are routed to their authenticated menu's own
receiver. Ordinary native buttons continue through SIU's menu-button transport.

Optional client mixins remember native screens, supply empty labels for native
icon buttons that otherwise report null, and keep popup actions in their own
session. An ordinary native screen retains its original behavior.

## Focused validation

The live subject is the retained Tom's Trading Network Fabric 0.3.0 release for
Minecraft 1.20.1, with unchanged upstream JAR bytes. Nineteen focused live checks
passed for both cuts, all configuration/storage cells, renaming, side settings,
count/NBT popup actions, ghost copying/clearing without consuming the cursor,
real stock pickup, reopening, Trading View, batched purchase costs/results and
stock changes, read-only customer offers, insufficient payment, GUI scale 3,
and creative-button presence/action.

After the popup's painting/dismissal refinement, five additional focused checks
cover native count/NBT actions, outside dismissal/reopening, tag conversion and
opening the customer view. Its complete popup and the final original-layout
windows were visually inspected. These follow-up checks do not repeat the
earlier nineteen-check run.

Fabric remapping and Forge compilation/reobfuscation passed. Forge gameplay,
dedicated-server multiplayer, automated side transfer, recipe-browser drag
integration, other Minecraft versions and the complete functional/gameplay
suite were not run. Output evidence, native/final screenshots and both built
Compatibility JARs are in `output/toms-trading-network-implemented/`.
