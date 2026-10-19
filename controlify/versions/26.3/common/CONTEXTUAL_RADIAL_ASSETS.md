# Contextual wheel textures

These editable assets are shared by all twelve companion builds. Their canonical
PNG/PDN source paths remain in this `versions/26.3` directory; native rendering
adapters use the same artwork on each supported game version and loader.

The held-RT recipe and creative wheels use Salt's existing dynamic window renderer
for the large wheel frame and materials frame. Those surfaces resize through Core's
ordinary window skin and native title drawing, with no dedicated panel PNGs or
synthetic inventory windows. The source container stays live on the right, and the
destination container stays live below the ingredients on the left.
The remaining wheel artwork comes from16 shipped PNGs through
`ContextualRadialAssets`, with no procedural shape fallback.
The default images are deliberately simple pixel placeholders: edit the production
PNGs directly in a pixel editor or replace them in a resource pack. Native item
icons and font text are drawn separately, so the textures contain no labels or items.
The live source container on the right continues to render with Core's own native
window textures and widgets.

All paths below are relative to
`assets/salts_inventory_controlify/textures/gui/contextual/wheel/`.
The production files are in [the wheel texture folder](src/main/resources/assets/salts_inventory_controlify/textures/gui/contextual/wheel/).

| PNG | Source size | Purpose and transparency |
| --- | --- | --- |
| `backdrop.png` | 760 × 450 | Optional extra artwork faded by opening; native inventory dimming remains consistent |
| `recipe_card.png` | 32 × 32 | Available recipe background |
| `unavailable_recipe_card.png` | 32 × 32 | Missing-ingredients background, muted red |
| `hovered_card.png` | 32 × 32 | Recipe hover state; opaque backgrounds are supported |
| `category.png` | 32 × 32 | Normal category frame |
| `selected_category.png` | 32 × 32 | Selected category with gold frame |
| `creative_tab.png` | 32 × 32 | Normal creative-tab background |
| `hovered_creative_tab.png` | 32 × 32 | Creative hover state, beneath the actual selected-tab marker |
| `active_creative_tab.png` | 32 × 32 | Actual native selected tab: green bottom marker, clear center |
| `wheel_ring.png` | 288 × 288 | Complete circular outer ring; inner disk and corners transparent |
| `wheel_ring_highlight.png` | 288 × 288 | Complete highlighted ring on the same canvas as the base ring |
| `carousel_opening.png` | 160 × 90 | Foreground entry/exit artwork; recipe cards pass behind it |
| `alphabet_label.png` | 10 × 12 | Normal empty letter box |
| `highlighted_alphabet_label.png` | 10 × 12 | Gold-framed empty letter box |
| `alphabet_anchor.png` | 2 × 2 | White dot, tinted for the current state |
| `alphabet_leader.png` | 1 × 1 | White segment texel, stretched/tinted for leader lines |

Keep each PNG's dimensions when editing. Indexed PNGs and ordinary RGB/RGBA PNGs
are accepted, including manual palettes and partial transparency. Minecraft decodes
them to RGBA without rewriting the files. The backdrop PNG is required but can be
fully transparent without disabling native inventory dimming. Creative tabs draw their ordinary background,
then hover artwork, then the actual selected-tab marker, then the native item icon.
The selected marker therefore stays visible even when the hover artwork has an
opaque fill. A clear center in the marker leaves the hover appearance visible there.

Both ring PNGs share the full288 × 288 canvas and center144,144. Author the complete
highlighted circle with the same geometry and fixed lighting as the base ring.
The renderer reveals the sector corresponding to the ten visible recipes out of
the current category/filter. More recipes produce a smaller sector; ten or fewer
recipes highlight the full ring. It crops
integer source pixels through native textured rectangles at their original UVs;
the highlight texture never rotates. This preserves its authored shading and crisp
pixel placement at every angle, including across12 o'clock. The same native canvas
transform applies to the base and highlight. The selector moves the cutout, rather
than moving or resampling the artwork itself.
The carousel doorway renders after all recipe cards and item icons in a later native
GUI stratum, so its opaque pixels mask their entry and exit. Ring, highlight, letters
and connecting lines then render in a later stratum above the opening. Alphabet
labels alternate inside and outside the ring with exact first-recipe anchors;
aiming at a letter puts that first recipe at the carousel's top middle.
The former arc boundary lines are removed entirely. `panel.png`, `wheel_panel.png`, `arc_boundary.png` and `outer_marker.png` are
retired runtime assets and are neither required nor used.

`ContextualRadialAssets.Element` declares the exact source sizes and resource IDs.
`draw(graphics, element, x, y, width, height, color)` applies normal native textured
rendering and supports tinting/opacity. `drawRegion` samples an explicit source
rectangle; `drawRingHighlight` submits at most576 merged source-pixel strips, or a
single complete source rectangle for a full ring. Wide sectors preserve their
two separated row spans without painting across the unselected gap.
`setTheme(Theme)` accepts a complete immutable
map of all elements; each `Sprite` declares its source dimensions. Missing files use
Minecraft's normal missing-resource diagnostics, rather than silently drawing another
shape. The existing workspace tuck sprite is a separate asset described below.

## Artwork sources and export

The [source notes](art-source/wheel/README.md),
[exact prompt manifest](art-source/wheel/generation-manifest.json) and
[pixel-grid contact sheet](art-source/wheel/contact-sheet.png) accompany all20 selected
original images. Each asset/variant was generated with its own built-in imagegen
call. Four original panel/line/marker records are retained as provenance with
`runtime: false`; their prompts and source images remain unchanged. The exporter
only checks or recreates the16 active runtime images, so it cannot restore the
removed panel and boundary PNGs. It uses nearest-neighbor grid sampling and a small flat palette;
it also registers the generated active-tab marker on its shared canvas and derives
a full highlighted ring from the base ring with the highlight palette. Thin10 × 12
label borders use edge-preserving nearest samples.
Original images are preserved unchanged with SHA-256 hashes in the manifest.

From the repository root, validate edits without changing a PNG:

```powershell
python controlify/versions/26.3/common/art-source/wheel/export-wheel-assets.py --check
```

The check reads decoded PNGs and verifies required files and source dimensions.
It accepts manual palettes, transparent backdrops, opaque hover artwork, and unused
backup PNGs. It never resaves production files. Paint.NET `.pdn` working files stay
editable in the workspace and are excluded from jar archives; unused backup PNGs
are not referenced by the renderer.

To recreate all production PNGs from the selected generated originals and refresh
the inspection sheet:

```powershell
python controlify/versions/26.3/common/art-source/wheel/export-wheel-assets.py --export --contact-sheet
```

`--export` replaces production edits; keep manual edits in the production PNGs or
save a separate copy before recreating them. The export step is an offline artwork
pipeline and never runs while the game renders the wheel.

Controller windows use Controlify's native action guides for quick-action bindings;
the separate recipe-book hint icon and its colored/grayscale textures are removed.
Guide labels can be translated with `salts_inventory_controlify.guide.*` language
keys. Binding glyphs use Controlify's current controller font and configured inputs.
English labels contain one or two words. Inventory guides use centered horizontal
rows; native undirected LS/RS glyphs represent stick controls. Gameplay guides keep
their native layout and clear tucked tabs and departing previews.

## Editable tuck sprite

The production [tuck_away.png](src/main/resources/assets/salts_inventory_controlify/textures/gui/workspace/tuck_away.png)
is a **64 × 32 RGBA PNG** containing two **32 × 32** frames: normal on the left,
selected on the right. Keep those dimensions and frame order when editing. Every
pixel is a flat palette color or fully transparent, without antialiasing. The
renderer samples the relevant frame and uses the workspace animation's scale and
opacity; the normal scale is 0.68× and selected scale 1.15×, proportionally fitted
to the available space in small viewports.

Resource packs can replace
`assets/salts_inventory_controlify/textures/gui/workspace/tuck_away.png` directly.
Inactive window icons use Minecraft's native item models/textures, including the
actual source block for loaded vanilla or modded blocks. They share the animated
preview position and opacity; active windows still render their native surface.

The [art source notes](art-source/README.md) retain the generated design, prompt and
optional pixel-grid exporter. Edit the production PNG directly to keep your edits;
rerunning the exporter recreates it from the generated design.

The carousel has ten main card positions plus fading ingress/egress cards at its
bottom opening. It rotates individual recipes continuously, including across the
end of the list. Held LB/RB advance counterclockwise/clockwise at five recipes per
second. Right-stick angle movement changes the target list position; unchanged
stick samples preserve the player's fine bumper position. Left-stick selection,
details, highlight, and A actions always refer to a displayed card during animation.
The centered top card is an exact recipe anchor. Each side distributes cards from
that anchor to the doorway at135 degrees; overscan reaches the hidden180-degree
bottom seam before wrapping. A catalog with one recipe renders one stationary
card, and tiny catalogs begin ingress at the same hidden seam.
Both recipe sticks activate at0.78 magnitude and release at0.64. Inward spring
return freezes their selection until neutral or a deliberate outward deflection.
