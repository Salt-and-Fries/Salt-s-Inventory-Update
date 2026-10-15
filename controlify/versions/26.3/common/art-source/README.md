# Tuck-away pixel sprite

Edit [the production sprite](../src/main/resources/assets/salts_inventory_controlify/textures/gui/workspace/tuck_away.png)
directly in a pixel editor. It is a 64×32 RGBA sheet: left 32×32 frame is normal,
right 32×32 frame is selected. Keep the frame order and transparent background.
The native renderer scales the sprite with the current workspace animation.

The design was created with the **built-in imagegen tool**, then exported with
nearest-neighbor sampling, a fixed 16-color palette and binary transparency onto
the actual Minecraft pixel grid. `tuck-away-generated.png` preserves the source.
`export-tuck-sprite.py` is an optional reproducible exporter using Pillow; rerunning
it replaces the production PNG with an export of that source, so keep manual edits
in the production PNG or save a separate copy before exporting again.

## Generation prompt

Use case: stylized-concept. Asset type: editable Minecraft mod UI pixel-art sprite
sheet. Create a transparent PNG containing exactly two same-size square frames
side by side: LEFT normal/unselected, RIGHT selected. Each frame must look like a
crisp 32 by 32 pixel sprite, enlarged with exact square nearest-neighbor pixels,
no anti-aliasing. Center identical 'tuck away windows' symbol in each frame: a small
stack of gray inventory windows moving downward into a shallow tray, using a clear
down chevron. Minecraft GUI palette: charcoal outline, medium stone-gray fills,
lighter top/left highlights. Selected frame has gold-yellow border and bright
pale-gold chevron; normal frame neutral gray border and ivory chevron. Identical
geometry and pixel grid in both frames, square tile framing, 2-pixel transparent
inset in each frame. Treat the entire canvas as a 64 by 32 logical-pixel sheet;
no extra decoration, labels, letters, text, shadows outside tiles, glow, gradient,
blurred pixels, photography, perspective or margins beyond the two frames. Output
clean game-ready pixel art on genuinely transparent background, intended to be
editable by the user and render sharply at small UI sizes.

## Contextual-wheel artwork

The separate [wheel source notes](wheel/README.md) preserve20 generated originals,
per-asset prompts, the pixel-grid exporter and its inspection sheet. The wheel
textures do not replace or modify this tuck sprite.
