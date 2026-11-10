# Tech Reborn windows - Minecraft 1.20.1 Fabric

The approved Tech Reborn 5.8.14 / Reborn Core 5.8.14 designs are implemented in
Compatibility 0.1.3, using Core 0.1.6's public native-window API. This page covers
1.20.1 Fabric only. The subsequent [all-target rollout](tech-reborn-all-targets.md)
covers the latest 5.8.15 patch and the newer official Fabric releases.

## Presentation

- All 51 registered machine screen types retain their native apparatus, slot
  identities, special slot artwork, gauges, progress, labels and live values.
- Upgrade wells form a vertical tray on the **right**. Native item/fluid/redstone
  settings tabs attach directly to the **left**, following the user's correction.
- The tray's complete surround, including its right and bottom inner gutters,
  uses opaque frame material without painting over native upgrade cells.
- The embedded player grid is omitted. Actual armor charging cells remain in
  energy-storage windows, backed by their real equipment slots.
- Hologram and lock controls have a separate row under SIU's title. Repeated
  native hologram selectables are replaced so old invisible hit boxes cannot
  remain after the multiblock state changes.
- Item and fluid inspectors keep the six native faces, permitted pencils,
  ordering and checkboxes. Their owned frame uses SIU's gray palette and Close
  returns to the parent view. Output-only inspectors retain fewer controls.
- Item settings keep their native help. Redstone uses the approved compact
  rows; fluid settings omit the empty help panel and keep the machine context.
  Storage/Tank Unit text space
  is retained because populated units use it. The slot-free Elevator is narrower.
- The Manual and external link prompts retain their normal native presentation.
  The pinned unused DestructoPack handler has no reachable registered screen.

## Transport and ownership

The adapter uses the publisher's extended opening buffer, original menu/screen
factories, slots, quick-move behavior and action decoders. Screen construction
reuses the authenticated menu instead of the publisher factory's temporary
second menu. No native recipe, energy, fluid or slot permissions are replaced.

Reborn Core's property packet has no menu ID and its listener helper recognizes
only vanilla's private listener class. A lifetime-owned listener routes the
publisher's unchanged codec through the owning SIU session. This keeps values
such as blast-furnace heat current in detached windows. The listener is removed
when the parent closes. Native actions validate the owner block and payload
before invoking the original receiver.

The six-face diagram already transforms its scissor coordinates. Its optional
capture hook uses those coordinates and the public capture-target dimensions,
without applying a second translation. Normal native rendering is unchanged.

## Focused evidence

The final production fixture loads the shipping jar bytes with the original
publisher release. Its observer is excluded from the distributable jar.

- [Captured states](../output/tech-reborn-implementation/captures.json)
- [Focused action checks](../output/tech-reborn-implementation/focused-checks.json)
- [Cell, widget and geometry checks](../output/tech-reborn-implementation/geometry-checks.json)
- [Release preservation and jar hashes](../output/tech-reborn-implementation/verification.json)
- [Implemented comparison PDF](../output/pdf/siu-tech-reborn-implemented-comparisons.pdf)

The tray-surround follow-up was checked separately on all 28 machine screens
with upgrades. Pixel checks confirm opaque right/bottom gutters and unchanged
native cell artwork. Its [focused results](../output/tech-reborn-tray-fix/checks.json)
and [release verification](../output/tech-reborn-tray-fix/verification.json) apply
to the updated shipping jar; the comparison PDF records the earlier capture.

These checks cover the affected UIs and representative actions, not the full
Tech Reborn recipe/automation/gameplay suite. No complete gameplay suite was run.
The target remap build passes. The broader repository build encounters existing
module-boundary violations in other integrations; this adapter uses the public
SIU API only. Core/API and unrelated release jars remain byte-identical.
