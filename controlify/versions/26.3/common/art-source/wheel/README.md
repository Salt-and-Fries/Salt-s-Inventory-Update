# Editable contextual-wheel placeholders

The production PNGs are in
[the runtime texture folder](../../src/main/resources/assets/salts_inventory_controlify/textures/gui/contextual/wheel/).
They are intentionally simple game UI placeholders, with flat colors and visible
pixel edges. Edit those small images directly in a pixel editor; no code change is
needed to change the skin.

The materials and wheel backgrounds now use Core's ordinary dynamic window skin,
and the former arc boundary lines and outer marker are removed. Their four generated originals and
prompts remain here for provenance, marked `runtime: false` in the manifest. The16
remaining runtime wheel PNGs are the active editable artwork. Checking/exporting
skips retired entries and does not recreate those removed assets.

All20 `*-generated.png` files here are the selected unchanged originals from
separate **built-in imagegen** calls, one per asset/variant. The
[generation manifest](generation-manifest.json) records the exact prompt for each
call, original dimensions and SHA-256, final dimensions, palette and exported PNG
SHA-256. No external image-generation CLI or API key was used. The central panel
prompt originally requested328 × 408; the final composition uses344 × 408 and the
same plain source is sampled to that confirmed output size.

The [contact sheet](contact-sheet.png) shows the original exported pixel grid on
a checkerboard. The production PNGs are authoritative after manual edits; this
reference sheet and manifest export hashes need not match those edits.
Transparency is real alpha, not checkerboard artwork baked into
the PNGs. The native item and text renderer supplies the contents of the empty
frames. Available and unavailable recipe backgrounds are separate. Hover artwork
can have an opaque fill; the actual creative-tab marker renders above it, before
the native item icon, so the green marker remains visible.
Recipe cards and their item icons render behind `carousel_opening.png`; the doorway's
opaque pixels provide the intended entry and exit masking.
The outer ring, highlight, alphabet labels and connecting lines render above the doorway.

## Reproducible export

The [exporter](export-wheel-assets.py) requires Python and Pillow. Run it from the
repository root:

```powershell
python controlify/versions/26.3/common/art-source/wheel/export-wheel-assets.py --check
python controlify/versions/26.3/common/art-source/wheel/export-wheel-assets.py --export --contact-sheet
```

`--check` accepts decoded indexed/RGB/RGBA PNGs, manual palettes, partial alpha,
transparent backdrops and unused backup PNGs. It verifies required source sizes
without modifying production textures. `--export` deliberately replaces
them from the selected originals, so preserve any manual production edits first.
Source originals are never overwritten by export. Nearest-neighbor sampling and
the fixed small palettes remove gradients and antialiasing on the final pixel grid.
Transparency becomes0/255, except `backdrop.png` with constant217 alpha.

The thin10 × 12 letter boxes use nearest samples that include the source border
edges, keeping the generated one-pixel outline visible at the tiny output size.
The generated green marker is registered at x10,y29 on its32 × 32 tab canvas.
The preserved original gold arc remains a source reference. Future exports derive
a complete highlighted ring from the full base-ring geometry and highlight palette.
Both production canvases retain center144,144. The runtime reveals only the aimed
sector corresponding to ten visible recipes out of the current category/filter through pixel-aligned source-UV crops, keeping artwork and
lighting fixed rather than rotating the texture. Manually edited full-ring PNGs
remain the primary authoring path and are never recreated by `--check`.
The sector becomes a full ring when ten or fewer recipes exist. Alphabet labels
alternate inside and outside the ring; aiming at a letter places its first recipe
at the carousel's top middle. Held bumpers rotate continuously at five recipes per
second. Both recipe sticks use a large activation dead zone and ignore inward spring-return motion.

See [the complete asset contract](../../CONTEXTUAL_RADIAL_ASSETS.md) for all paths,
exact dimensions, frame orientation and renderer tinting semantics. Resource packs
can replace any production PNG at the same namespace/path and source dimensions.
