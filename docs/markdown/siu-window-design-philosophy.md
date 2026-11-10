# SIU window design philosophy

Updated: 2026-11-02

A good SIU window should feel like the original mod learned to live on a
desktop. Preserve the mod's visual identity, working layout and behavior, then
give the player SIU's window management and independent inventory.

This guide records the approach refined with the user throughout Project
Everything. The strongest results came from close observation, the user's
specific feedback, restrained changes and repeated visual checks. These are
repeatable methods, rather than a dependence on an agent remembering a previous
conversation. The current request and the user's approved design take priority
over the examples here.

## 1. Treat the original UI as the design system

The original interface already contains decisions about grouping, materials,
hierarchy and interaction. Understand those decisions before drawing a window.

- Preserve the relative positions of inputs, outputs, gauges and controls.
- Keep distinctive slot silhouettes, ghost items, decorative circuitry,
  machinery diagrams, channel strips and animations.
- Reuse the original texture palette, bevels and icon sizes.
- Keep a native action beside the thing it affects. A button below a crafting
  output belongs there; a native sidebar action belongs in the sidebar.
- Move a group together when a change is needed. Preserve the relationships
  inside that group.

The Create Schematicannon established the clearest example: retain almost the
entire native interface, left-align its title and fit SIU management controls
into its existing title strip. Its distinctive body, footer and proportions
provide the identity of the window.

Consistency comes from recognizable SIU behavior and careful geometry. It does
not require every mod to have the same gray rectangle.

## 2. Establish what actually exists

Use genuine in-game references without SIU. Inspect the matching release's
screen registration, rendering code, menu slots and textures when needed.
Publisher source helps explain a screenshot, especially when important content
only appears during operation.

For each interface, record:

1. Minecraft version, loader and exact mod release.
2. How to open it, including prerequisites such as a formed multiblock.
3. Screen/menu identity and which other blocks reuse that screen.
4. Real machine slots, player slots, native controls and external fragments.
5. Dynamic states, expanding panels and content hidden in an idle screenshot.

Distinguish screen types from opening routes, tiers and states. The audited
Powah 1.20.1 release has eleven concrete container screen types; Ender Gate
reuses the Ender Cell screen. A
collection of seventy-three captured views is useful evidence, but it is not
seventy-three independent UI designs.

Follow the established inventory/container scope. Guidebooks, destination
selection screens and other non-inventory interfaces normally retain their
native presentation. Account for them in the report instead of silently
omitting them or inventing unnecessary windows.

## 3. Choose the lightest suitable composition

| Composition | When it fits | What to preserve |
| --- | --- | --- |
| Native frame with SIU controls | The mod already has a useful header and a distinctive complete body, as with Create. | Native silhouette, title strip, footer, spacing and external artwork. |
| Integrated SIU frame | SIU chrome can surround a compact working area naturally, as with Powah. | Native apparatus, complete slot art, exact palette and internal relationships. |
| Framed inset or cutout | Elaborate artwork should read as a panel inside a themed shell, as with Weapon Fusion. | Original working artwork inside a deliberate, palette-matched inner rim. |
| Attached panel | A native tray, upgrade strip or companion inventory needs its own area. | Its complete border, native actions and clear relationship to its parent. |

The automatic fallback remains useful for interfaces without a dedicated
integration. An intentionally designed integration should use the composition
that fits its native UI; accidentally falling back is a defect to investigate.

Avoid adding a second header or replacement outer frame when the native one
already does the job. Conversely, when a new frame helps, make its inset and
material relationship deliberate.

## 4. Preserve function before tightening space

An attractive screenshot is only one state of a working interface. Reserve the
space needed for full gauges, animated sprites, changing numbers, populated
slots, status messages and expanded controls.

Compactness should come from removing the embedded player inventory,
unnecessary duplicate headings and proven unused space. An empty-looking
machine panel may contain an animation or a future value. Check before shrinking
it.

Keep sorting and other actions appropriate to the actual menu. A status screen
with zero machine slots should not acquire an invented storage grid. Preserve
the mod's slot restrictions and server actions. SIU's visual arrangement does
not grant new item permissions or replace the original protocol.

When a menu belongs to an existing family, start with its established SIU
implementation. Modded villager trading should inherit the approved trading
layout; AE2 extensions should follow the appropriate AE2 generation.

## 5. Make extraction precise

### Use complete visual envelopes

Measure more than a sprite's idle colored center. Include every bevel, shadow,
slot rim, animation extent and attached decoration. A fluid tank needs its full
fill area and both end borders. A slot needs its entire native frame, even when
its clickable item area is smaller.

Use GUI pixels consistently. Distinguish logical coordinates from physical
screenshot pixels, and document the source origin and destination offset. A
one-pixel correction means one GUI pixel, regardless of screenshot scale.

### Respect transparency and silhouettes

A rectangular screen capture can contain darkened world pixels between native
modules or around curved corners. Use publisher texture alpha, explicit masks
or composed regions to preserve the actual shape. A dark rectangle copied from
the world is not part of the window material.

Remove a local artifact with a local mask or reconstruction of the correct
border. Do not shave an entire side or bottom to remove a small remnant of the
old inventory behind a curve.

Preserve live slot overlays and item rendering in implementation. A static atlas
crop alone can miss ghost flowers, card silhouettes or other artwork added by
the screen's slot renderer.

### Keep texture scale intact

Use native sprite sizes and nearest-neighbor scaling for pixel-art previews.
Stretch only texture regions designed for stretching, such as the center of a
nine-sliced frame. Never stretch a whole button to fill a wider area.

## 6. Match the material, including the frame

Sample flat background colors from the original texture rather than estimating
them from a compressed screenshot. Choose bevel and rim colors from the same
palette. A dark machine body should have a compatible window shell and readable
title; a light gray header surrounding an unrelated dark patch often looks
unfinished.

Powah's gray machines retain their native gray surface. Ender Cell and Ender
Gate use their teal artwork as the basis for the shell. The treatment differs
because their source materials differ.

Keep custom slot visuals when they communicate meaning. A colored output well,
flower designation or special battery/card slot should remain recognizable.
Ordinary SIU slot frames should not cover those details.

## 7. Give controls a clear home

The title and SIU management controls form one coherent header. Use the normal
applicable control set when it fits. Use More (`...`) and Close (`X`) when the
available header cannot hold the full set cleanly. Preserve readable title
space and a clear inset from the right border.

Native mod controls keep their original grouping and purpose. Similar-looking
icons may perform different actions; inspect their behavior before merging or
removing them. Preserve a native top-right identity/navigation icon in that
position, with management controls placed to its left when appropriate.

Suppress an identified redundant external sorting provider through the relevant
integration. Do not remove arbitrary buttons merely because their icon resembles
a sort action. Respect the user's explicit per-mod exceptions, such as retaining
the Traveler's Backpack tool control.

## 8. Finish attachments as complete objects

An attached tray may extend above or below the parent. Its exposed inner edge
still needs a complete border. Do not depend on the parent covering a raw cut
edge at every possible size.

Check both sides of an attachment, all corners and the join in every open or
selected state. Tabs should retain the intended rounded parent corner and
appear to continue behind it. Selected and unselected tabs can require different
overlap geometry.

Define attachment behavior explicitly. The approved Immersive Aircraft side
windows follow their parent, cannot be dragged independently and omit Close and
movement controls. The Quark backpack has a different approved arrangement: it
opens and closes with the inventory, omits Close, and retains its permitted
positioning controls. Those examples are separate policies, not one universal
rule for every companion.

## 9. Use one geometry model for drawing and interaction

In implementation, the same source-to-window transform must govern artwork,
slots, click targets, dragging, hover highlights, tooltips and text focus.
Moving only the pixels produces a broken interface.

Preserve real menu slot IDs and native actions. Resolve current widget geometry
after resize or state changes. Keep hidden controls from receiving clicks or
keyboard focus. Check scroll content, scrollbar position, viewport clipping and
input mapping together.

Keep mod-specific integration in Compatibility and use the established public
API. A visual change should preserve that contract unless the user explicitly
requests an API change. Consult the current API documentation for implementation
details rather than treating this philosophy as a substitute for the contract.

## 10. Port the design to the actual release

Shared names do not guarantee shared textures or widget geometry. Verify the
actual mod version for each supported Minecraft/loader target. Reuse the design
principles while adapting to that release's native interface.

AE2 is the recurring example: use vintage textures where they apply, and modern
textures, icon rendering and tab geometry in newer releases. A crop that worked
for the older atlas can cut a modern icon in half.

Keep availability, implementation and verification separate. A successful build
does not establish that a window looks correct in game. A screenshot on one
loader is not evidence of a gameplay check on every loader.

## 11. Review against the original, one defect at a time

Put the original and proposed window next to each other. Compare these in order:

1. **Completeness:** every slot, native action and important indicator remains.
2. **Identity:** palette, materials, hierarchy and distinctive shapes survive.
3. **Layout:** original relationships remain recognizable and balanced.
4. **Edges:** every sprite, slot border, tab, tank and text field is complete.
5. **Spacing:** the content clears the header, controls and bottom border.
6. **Behavior:** visible controls and their interaction targets agree.

Inspect the last row, bottom pixels and external fragments explicitly. Many
earlier defects hid there: portable-cell slot bottoms, a clipped South label,
truncated spell rows and trays whose inner rim stopped at the parent boundary.

Prefer a small, local correction when it solves the issue. Move a misaligned
group together. Add the missing few pixels of clearance. Correct the extraction
mask. Preserve successful parts of the design while fixing the defect.

## 12. Make evidence useful and honest

For design reports, place the native game UI on the left and the proposed SIU
window on the right. Label mockups clearly. Use comparable GUI magnification
where practical, and identify enlarged detail views.

Explain each distinct layout in the main report. Put repeated tiers and variants
in a coverage table or gallery, clearly stating which were actually captured.
Do not imply that repeated images are new screen types or that uncaptured states
were verified.

Render the finished PDF and visually inspect it. Check page text bounds, image
clarity, captions and complete edges. A valid PDF file can still have a poor
layout.

For implementation, use focused checks appropriate to the changed features:
slot transfers and restrictions, native buttons, scrolling, text editing,
attachments, resizing and relevant active states. Follow the repository's
testing preferences. Report what ran and any coverage limits; run a complete
functional suite only when the user explicitly requests it.

Pixel comparisons help substantiate preserved artwork. Compare stable opaque
regions; exclude world backgrounds, animations, cursors and intentionally
changed chrome. Keep the original screenshots immutable and record their source
version and hashes when building a repeatable comparison set.

## Practical workflow for the next mod

1. Read this guide and the user's latest decisions.
2. Inventory the real container screens and their opening routes.
3. Obtain native references for the exact release and inspect active content.
4. Pick the composition that preserves the most useful native structure.
5. Specify complete source regions, masks, palette, slot IDs and control groups.
6. Build the requested mockups with native artwork and existing SIU assets.
7. Compare every design with its original; repair clipping and spacing locally.
8. Deliver the requested stage. When implementation is authorized, carry those
   designs into the real integration and verify the affected behavior.
9. Save the evidence, coverage limits and any new reusable lesson.

The final question is: **Does this feel like the mod's own interface, with SIU's
window behavior added carefully?**

## Project references

- [Native Create treatment and acceptance evidence](create-native-window-rollout-1.20.1.md)
- [Native window templates and geometry contract](native-window-templates-1.20.1.md)
- [Compact vanilla trading decisions](vanilla-merchant-window-restyle-1.20.1.md)
- [Companion window contract](companion_windows.md)
- [Powah mockup builder and geometry checks](../../tools/build-powah-window-mockups.py)
- [Powah design report](../../output/pdf/siu-powah-window-mockups.pdf)
