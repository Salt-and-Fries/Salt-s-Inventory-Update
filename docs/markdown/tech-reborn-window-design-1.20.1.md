# Tech Reborn window designs - 1.20.1 Fabric

Design stage complete, November 3, 2026. Implementation remains pending.

- [Comparison PDF](../../output/pdf/siu-tech-reborn-window-mockups.pdf)
- [Design manifest](../../output/tech-reborn-window-design/manifest.json)
- [Verification](../../output/tech-reborn-window-design/verification.json)
- [Repeatable builder](../../tools/build-tech-reborn-window-mockups.py)

The 63-page PDF contains all 51 registered machine screen types, five shared
configuration designs and two populated-unit examples. Every comparison uses a
real SIU-free original on the left and a static proposed window on the right.
The eight unformed multiblock states and the native Manual are accounted for in
the appendix. DestructoPackScreenHandler has no screen registration or opening
reference in this release and is not an additional reachable inventory UI.

## Native evidence

Tech Reborn 5.8.14 and embedded Reborn Core 5.8.14 ran in a minimal production
Fabric client. The reference has no SIU Core, Compatibility or Controlify.
All 51 machines were opened. All eight special multiblocks were constructed
from their native writeMultiblock definitions and passed native validity checks.
The item/fluid/redstone configuration views were opened through their native
tabs. Storage and Tank Units were populated through normal slot clicks.

The source is pinned to tag 5.8.14, commit
`380a06253bea70924a2470454875cbea99682a0a`. Hashes and observations for 67
captures are retained in `output/tech-reborn-window-design/fresh-native.json`.
Original historical reference captures also remain unchanged.

## Proposed composition

Use an integrated SIU frame in the native flat `#C6C6C6` palette. Keep the
working apparatus as a group with its original slots, tanks, arrows, readouts
and controls. Remove the embedded player inventory. Armor charging slots belong
to the working area and remain present even though their inventory is the
player's own inventory.

Retain the vertical four-slot upgrade tray on the left. Use a second short
left rail for the native settings tabs, keeping those actions on the side
without extending a tall tab column through an empty lower half. Both rails
have complete borders. Native lock and hologram buttons get a separate clear
row beneath the SIU title. Use the normal applicable management controls or
More and Close when the title needs the room.

The Elevator gets a narrow gauge-only status window. The other sparse status
areas retain space required by their dynamic native messages. Populated
Storage/Tank examples demonstrate why their initially empty text area is needed.

Item configuration can expand the parent to retain its complete native help
panel. Item input, item output and fluid inspectors are temporary children of
the machine session. Their normal Close returns to the native parent view.
The compact redstone settings view retains every native row and cycling action;
the selected left tab returns to the machine. These are proposed behaviors,
not an implemented integration.

## Geometry details for implementation

- Inherited container bounds report height 166, but GuiBase actually draws a
  176-pixel native frame. Use the real draw envelope for references.
- The player grid begins at y=94. Several work slots and controls extend to
  y=89/90, so retain their full rims and bottom padding.
- Do not discard the four player armor slots in energy-storage screens when
  removing the embedded player grid and hotbar.
- Configuration tips and redstone controls use the area at y=93 and below.
  It is meaningful configuration content rather than an inventory to crop out.
- The native inspector close overlaps the top-right inset rim. Removing that
  close requires restoring the rim beneath it. The original 62-pixel diagram
  starts seven pixels below its popup's top.
- Native energy-storage readouts render at 0.6 scale. Preserve live rendering;
  the mockups retain the source's full two-pixel raster detail rather than
  reducing those glyphs to one pixel per GUI pixel.
- Keep native slot IDs, item/fluid permissions, packets, text focus, tooltips,
  configuration copy/paste, multiblock warnings and hologram actions. Use the
  established public API when implementation is authorized.

## Checks performed

The registered screen set matches all 51 machine references. All 63 PDF pages
were rendered and reviewed, including all 58 proposed views. The verifier
checks 331 complete native slot crops and six small energy readouts against the
original raster pixels. Native capture hashes and the existing release bundle
are unchanged.

These are focused scope, opening, state and visual checks. No compatibility
implementation or complete functional/gameplay suite was performed.


## Implemented follow-up

The approved 1.20.1 Fabric designs are now implemented. The user's final
correction takes precedence over this report's original rail arrangement:
**upgrade-slot tray on the right, settings tabs directly on the left**.
See [implementation and focused evidence](../../compatibility/tech-reborn-1.20.1.md)
and [actual game comparisons](../../output/pdf/siu-tech-reborn-implemented-comparisons.pdf).
The original mockup PDF is preserved as design history.
