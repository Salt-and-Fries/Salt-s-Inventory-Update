# Immersive Aircraft — Minecraft 1.20.1 Fabric

The approved Cargo Airship mockup is implemented: native equipment and fuel
remain in the central window, with Port and Starboard cargo windows alongside
it. Both cargo windows now use the same width and symmetric padding, as
requested during implementation. The player's ordinary SIU Inventory opens
independently.

The shared screen adapts to the aircraft's real slot descriptions. Cargo
Airship has 23 central positions and 32 on each side, preserving all 87 owned
positions. Airship, Biplane, Gyrodyne and Quadrocopter only open their central
window. Warship has its original three boilers and two eleven-row cargo wings.
Shorter aircraft use shorter central windows. No player inventory slots are
painted over the aircraft's working area.

## Native behavior

Each window owns an authenticated SIU menu session exposing a disjoint subset
of the original vehicle's slots. The server uses the original native Slot
instances over the real vehicle inventory; no second storage inventory is
created. The native menu retains validity checks and balanced start/stop-open
callbacks. Client menu snapshots use private containers while Aircraft's own
equipment synchronization and vehicle ticking continue normally.

Native fuel, weapon, upgrade, booster, banner and dye predicates and stack
limits remain in charge. The full native boiler and equipment sprites are
drawn from the installed mod's atlas, with original empty-slot tooltips and
live fuel-use feedback. Cargo retains its original column/row ordering.
SIU's storage sorting excludes equipment and fuel positions. Quick transfers
use SIU's existing routing and native destination-slot rules.

The cargo panes dock beside the main pane and follow it when moved. Each side
header exposes exactly three actions: sort out to other containers, sort this
container, and focus. Side panes cannot close, pin, link, lock, resize or move
independently, including through desktop handle/controller movement. Movement
and window management remain on the parent. Closing the parent closes its
cargo panes; reopening restores the group and real stored items.

A compatibility-only control filter keeps Salt's original sorting/focus routing.
The compact side headers retain equal widths, centered cargo and complete titles.
The parent layout briefly permits unlocking/moving a child synchronously; outside
that operation its movement and lock permissions are unavailable. This accounts
for Salt automatically locking immovable windows without changing Core or its API.

The adapter is gated to the audited **Immersive Aircraft Fabric
1.1.5+1.20.1** release. Unsupported loader/release combinations retain the
native opening path. No Core or public API changes were made for this work.
The tested Core 0.1.6 artifact retains SHA-256
`4822c251556e35b9f7eea5103f83791e2f58f6aa154701b247fed051c1e22673`.

## Focused validation

Production Fabric checks use the original upstream release, its audited Fabric
API, Core 0.1.6 and the built Compatibility 0.1.3 artifact in a disposable
integrated-server world. Screens open through actual sneak/ray interaction.
Operator commands supply test aircraft and item fixtures; menu storage and
transfers are exercised through the real UI and public desktop input API.

Checks cover all six base aircraft layouts and their complete native sprites,
all 87 Cargo Airship positions without duplicates, equal cargo margins,
boiler fuel insertion and invalid-input rejection, upgrades, banners, dyes,
weapon and booster inputs, central and both upper/lower side cargo grids,
quick transfer in both directions, sorting protection for equipment, dragging
the connected group, carried-item conservation, repeated close/reopen and
Warship's three boilers and tall cargo grids. GUI scales 2 and 3 are checked.
Final geometry checks follow the smaller-window and viewport-alignment polish.

The journal retains corrected fixture mistakes: Inventory initially obscured
some target coordinates, and the older non-ray probe failed to set the local
secondary-use input and mounted the aircraft. After moving Inventory aside
and using the ordinary ray interaction, those checks passed. The native
inventory contents remained intact. One screenshot name containing an
underscore was rejected by the excluded capture fixture and retaken with a
valid name; it was not an adapter exception.

Fabric remapping and shared-source Forge compilation/reobfuscation pass.
Forge compilation is an isolation check, not a Forge Aircraft adapter or
gameplay sign-off. Dedicated-server multiplayer, combat/flight gameplay,
arbitrary vehicle addons/datapacks, resource-pack variants and every alternate
click gesture were not tested. No complete functional/gameplay suite was run.

See `output/immersive-aircraft-implemented/` for the tested artifact, full-screen
captures, focused evidence and the manifest recording exact tested builds.

The subsequent side-control update is recorded separately in
`output/immersive-aircraft-side-controls/`. Its checks focus on the three allowed
actions, rejected independent movement, parent docking and close/reopen lifetime,
with Cargo Airship and the tall Warship at GUI scales 2 and 3. The earlier
six-aircraft gameplay checks were not rerun for this header/ownership change.
