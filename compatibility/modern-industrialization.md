# Modern Industrialization windows

The approved SIU mockups are implemented using MI's original menus, renderers,
widgets, recipe actions and slot rules. Player inventory stays in the existing
SIU inventory window. Core and the public API are unchanged by this adapter.

## Published targets

| Minecraft | Loader | Publisher release | SIU Compatibility |
| --- | --- | --- | --- |
| 1.20.1 | Fabric | 1.8.6 | 0.1.3 |
| 1.21.1 | NeoForge | 2.5.11 | 0.1.3 |

These are the publisher releases matching SIU's current version/loader matrix.
See [publisher releases](https://modrinth.com/mod/modern-industrialization/versions)
and [upstream source](https://github.com/AztechMC/Modern-Industrialization).
The adapter is optional and initializes only when `modern_industrialization` is
installed. No support is implied for an unavailable mod/loader combination.

## Composition

- Machine windows cover the 107 device variants in the approved 1.20.1 atlas.
  MI still chooses the arrangement and renders its energy, heat, recipe,
  fluid, reactor, tank and status components. Native upgrade trays remain attached.
- Forge Hammer retains its tool/input/output cells, complete recipe board,
  selection, crafting, native tool damage and inventory return behavior.
- Item Pipe retains its 21 phantom filters, real upgrade cell, warning, allow/block
  mode, connection mode and both priority controls.
- Fluid Pipe retains its fluid selector, connection mode and priority controls.

Native machine actions sit in a separate row below SIU's title controls. Sparse
item/fluid hatches and energy/transformer panels use shorter bodies; dynamic
multiblock and recipe areas retain their required space. Shape-selection trays
expand with their native text and arrows. Full 18-pixel slot rims are retained.
Configuration cells are excluded from ordinary SIU chest sorting and recipe-source
scans; native quick-move behavior remains available.

### Crop and tray refinements

Machine crops now include the full native component artwork, with six pixels of
body padding where the original working area permits it. The Large Tank retains
its complete 46-by-62 gauge; boilers retain both the thermometer and heat bar.
Sparse panels no longer keep the original full-width canvas just to display one
energy icon or cell. Long compact-window titles use SIU's ellipsis and show their
complete name on hover.

Native shape and upgrade trays retain their outside bevels. Their open inner
edges are replaced with a matching wall and rounded bottom corner wherever the
tray extends below its parent. The replacement occupies only the empty four-pixel
join, preserving native slots, labels, arrows and hit targets.

Space for conditional overclock artwork, formed multiblock information and the
reactor's live grid is reserved even in empty fixtures. Cropping uses the native
component bounds, not a scan of currently visible pixels. Newer MI's duplicate
title and Inventory label are suppressed separately from its component renderers.

## Native transport

Each menu allows only its original MI channels. MI's own decoders and receivers
still validate and execute the actions. Fabric's queued receivers run inside the
authenticated owning-menu callback, on the server thread. Addressed MI client
updates temporarily select the matching native menu, then restore the previous
menu. The 1.8.x fluid-selector null label is normalized to an empty component so
the native widget can participate in SIU's existing layout snapshot.

## Focused verification

Evidence and exact runtime inputs are retained under
`build/modern-industrialization-implementation/` and the disposable
`build/remaining-ui-audit/gameplay/*mi-windows*` profiles. Earlier failed captures
are retained alongside corrected runs.

- Both publisher releases: opening/layout observations for the 107 device
  variants; Forge Hammer and both connected pipe editors checked separately.
  Geometry checks require complete active native cells, visible widgets and
  component artwork, including reserved active-state areas and source cutouts.
- Actual native slot locking and auto-eject changes verified in server block NBT.
- Forge Hammer: seed eight iron ingots plus an iron hammer, select a recipe,
  take four iron bolts, and verify seven ingots remain and the hammer receives
  native damage. The cursor is empty after returning the result to inventory.
- Pipe priority, connection and allow/block modes survive closing and reopening.
  Phantom item filters preserve the carried items; fluid selection preserves the
  water bucket. A real water-bucket transfer into a Configurable Tank returns
  an empty bucket and stores water in the server block.
- Expanded shape selection, movement/resizing and final archive checks are
  recorded in the implementation verification JSON.
- The refinement pass recaptures all 110 approved views on each target. Expanded
  left trays, taller right trays, the complete tank gauge, boiler thermometer,
  empty/charged energy icons and compact-window native controls are reviewed
  separately. Evidence is retained in the `*mi-refine03` profiles and
  `build/mi-window-refinement/`.

This is focused UI/integration coverage. A complete functional/gameplay suite was
not run. Long-running production chains, every formed multiblock, reactor
simulation, external recipe viewers and multiplayer network conditions were not
exhaustively exercised. Unformed multiblocks correctly retain MI's native
"Shape Invalid" state; that is not formed-machine production coverage.

The reviewed design PDF remains at
`output/pdf/siu-modern-industrialization-window-mockups.pdf`; it is a design
reference, not a new gameplay report.
