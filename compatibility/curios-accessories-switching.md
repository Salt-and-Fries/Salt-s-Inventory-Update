# Curios and Accessories equipment switching

Curios uses the upper-left corner of the Character portrait; Accessories uses
the upper-right corner. Both buttons fit inside the portrait rim; the
Accessories anchor is inset two pixels from its previous position. Selecting
a different system closes the previous
attached panel before requesting its replacement. Clicking the selected
system's button closes that panel. The Character and independent inventory
stay open, and closing Character disposes its equipment panel.

A shared compatibility-side selection tracks pending requests as well as open
panels, so a delayed reply from the previously selected system is discarded.
Each panel still uses its own native equipment inventory. Switching the view
does not transfer, duplicate or unequip items. Aether's equipment subview keeps
its existing exclusive relationship with both general equipment systems.

The 1.20.1 and 1.21.1 source families and their four compatibility jars were
updated. Later Curios targets do not include an Accessories adapter and retain
their existing launcher. Core and its public API are unchanged.

Focused verification:

- Ten selection/lifecycle checks pass in each affected source family. They
  cover switching, pending-request retries, stale replies, reentrant close
  callbacks, disconnect cleanup and independent Character owners.
- All four compatibility artifacts compile and package successfully. Shipping
  updates preserve unrelated entries and include no observation/test classes.
- 1.21.1 NeoForge: 35 focused in-game checks pass through normal published mouse
  hit regions. These include repeated switches in both directions, one live
  equipment session, same-button close/reopen, a tagged seven-item cursor,
  native Curios gloves equipped/recovered with metadata preserved, Aether
  transitions, and Character-close cleanup.
- 1.20.1 Forge: 16 pair-only in-game checks pass on the official production
  launcher, including both real launchers, repeated swaps, the tagged shared
  cursor, same-button close, and Character-close recovery. This fixture has
  Accessories 1.0.0-beta.49+1.20.1, Curios 5.14.1+1.20.1 and required Cloth Config;
  it does not include an equipment-item addon or compatibility bridge.

The live fixture uses Accessories 1.1.0-beta.54+1.21.1 and Curios 9.5.1+1.21.1
with the existing mixed publisher configuration. This does not establish every
item effect, Accessories item-specific equip rule or compatibility-layer
combination. No complete gameplay suite was run for this change.

Screenshots and focused results: `output/curios-accessories-swap/`.
