# Accessories — Minecraft 1.20.1 Fabric

Accessories' player equipment is now a Curios-style panel attached above SIU's
Character window. The button beside the character preview opens and closes it.
The original Accessories shortcut opens the same panel. Each functional
position is followed by its cosmetic partner; all available positions are
shown, including types hidden by the native unused-slot filter.
The slot frames have a one-pixel gap inside each functional/cosmetic pair and
a five-pixel gap between pairs, keeping four pairs across the standard panel.

The panel follows Character at the same width, closes with its owner, and
scrolls when its complete contents cannot fit above Character. Slot counts
and types come from Accessories' live containers rather than a fixed list.
Native slot icons, cosmetic hearts, item tooltips and the per-position rendering
switch remain available. Character retains its existing fixed size.

## Native behavior and scope

The server menu uses Accessories' original `AccessoriesInternalSlot` instances
over the real equipment and cosmetic containers. Native placement predicates,
stack limits, removal restrictions, curse exceptions, callbacks, attribute
updates and synchronization retain their original ownership. Client menu
snapshots use private storage and never overwrite live wearable containers.
Changes to container capacity or identity replace the attached session before
obsolete positions can be clicked. Equipment positions are excluded from SIU
storage sorting.

The rendering switch calls the original Accessories visibility callback. The
player launch interception does not intercept crouch-H entity targeting;
other-entity equipment screens retain their native full-screen behavior.

This adapter is gated to the audited **Accessories Fabric
1.0.0-beta.48+1.20.1** release. It implements Accessories alone. Trinkets/Curios
compatibility layers and other Accessories releases/loaders are not signed off
by this change. The existing Trinkets and Curios integrations are unchanged.
Only Compatibility and its build wiring were changed; Core/public API source
was not changed for this implementation. The tested Core 0.1.6 artifact retains
SHA-256 `4822c251556e35b9f7eea5103f83791e2f58f6aa154701b247fed051c1e22673`.

## Focused validation

Production Fabric checks use unmodified upstream release jars, Core 0.1.6 and
Compatibility 0.1.3 in a disposable integrated-server world. The fixture also
includes the audited Sodium/Indium pair used by the native rendering baseline.
Vanilla items annotated with Accessories' operator commands serve as compatible
equipment fixtures; this mod alone does not supply ordinary wearable items.

Checks cover opening/toggling, all 14 functional and 14 cosmetic positions,
separate storage, native insertion rejection, rendering switches, functional
attribute application/removal, survival binding curse and the different
cosmetic/creative removal exceptions, native capacity changes, attachment
movement/closing, carried-item conservation and scrolling at GUI scales 2/3.
The native shortcut reopen path is checked after dismissing the desktop.
Accessories and SIU both bind H by default. The shortcut was tested with
Accessories rebound to J through Minecraft's Controls screen in the disposable
profile; the adapter does not silently change the player's bindings.

The raw check journal retains incorrect probe assumptions about resizing
Character and expecting scrolling while every row still fitted. A further
probe incorrectly toggled the desktop closed before clicking it. These were
fixture mistakes. Default-H attempts also retain their observed key conflict.
Code inspection found and fixed a retained-workspace launcher edge case where
an inactive workspace would not be brought back on screen. The final shortcut
checks pass with the conflict-free binding.

Fabric remapping and shared-source Forge compilation/reobfuscation pass.
Forge compilation is a loader-isolation check, not a Forge Accessories adapter
or a gameplay sign-off. Dedicated-server multiplayer, compatibility layers,
resource-pack variants, controller navigation and every alternate click
gesture were not tested. No complete functional/gameplay suite was run.

See `output/accessories-implemented/` for the tested Fabric artifact, full-screen
game captures, evidence journal and artifact manifest.
