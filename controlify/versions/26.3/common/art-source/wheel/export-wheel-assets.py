"""Compile the selected imagegen originals to the exact editable Minecraft pixel grid.

This is an offline asset export, never a runtime shape renderer. Original images
remain unchanged. Run with --check to validate manually edited production PNGs
without overwriting them. Only --export recreates production PNGs from the sources.
"""
import argparse
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageDraw

ART = Path(__file__).resolve().parent
COMMON = ART.parents[1]
TARGET = COMMON / "src/main/resources/assets/salts_inventory_controlify/textures/gui/contextual/wheel"
if not TARGET.is_dir() and (COMMON / "wheel").is_dir():
    TARGET = COMMON / "wheel"  # The extracted test ZIP's editable-assets layout.
MANIFEST = ART / "generation-manifest.json"
GRAY = ["101010", "303030", "555555", "858585", "dddddd"]
GOLD = ["9c7d22", "d4af36", "ffe34d", "fff8cc"]
RED = ["422c32", "6b3a3a", "963e3e", "d27777"]
GREEN = ["3c7758", "5db080", "8de7bb"]
WHITE_PRIMITIVES = {"alphabet_anchor", "alphabet_leader"}
TRANSPARENT = {"hovered_card", "hovered_creative_tab", "active_creative_tab", "outer_marker",
               "wheel_ring", "wheel_ring_highlight", "carousel_opening"}


def threshold(image):
    return image.getchannel("A").point(lambda value: 255 if value >= 128 else 0)


def cropped_art(image):
    bounds = threshold(image).getbbox()
    assert bounds is not None, "Generated source has no visible artwork"
    return image.crop(bounds)


def palette_for(name):
    if name in WHITE_PRIMITIVES:
        return ["ffffff"]
    if name == "backdrop":
        return ["121620"]
    if name == "active_creative_tab":
        return GREEN
    if name in {"hovered_card", "hovered_creative_tab", "wheel_ring_highlight"}:
        return GOLD
    if name in {"selected_category", "highlighted_alphabet_label", "outer_marker"}:
        return GRAY + GOLD
    return GRAY + (RED if name == "unavailable_recipe_card" else [])


def runtime_assets(assets):
    # Retired generated sources/prompts remain in the provenance manifest, but
    # dynamic Core windows and the removed arc lines have no runtime PNGs.
    return [asset for asset in assets if asset.get("runtime", True)]


def flatten(image, name):
    colors = [tuple(bytes.fromhex(value)) for value in palette_for(name)]
    cache = {}
    pixels = []
    values = image.get_flattened_data() if hasattr(image, "get_flattened_data") else image.getdata()
    for pixel in values:
        if pixel[3] < 128:
            pixels.append((0, 0, 0, 0))
            continue
        rgb = pixel[:3]
        if rgb not in cache:
            cache[rgb] = min(colors, key=lambda color: sum((color[i] - rgb[i]) ** 2 for i in range(3)))
        pixels.append((*cache[rgb], 217 if name == "backdrop" else 255))
    result = Image.new("RGBA", image.size)
    result.putdata(pixels)
    return result


def label_grid(source, size, gold):
    # Keep the generated thin border when compiling the very small 10x12 grid.
    # Ordinary center-sampled downscaling can skip a sub-cell source edge entirely.
    edge = Image.new("L", source.size)
    values = source.get_flattened_data() if hasattr(source, "get_flattened_data") else source.getdata()
    edge.putdata([255 if (pixel[0] > 150 and pixel[1] > 100 and pixel[2] < 100
                         if gold else min(pixel[:3]) > 150) else 0 for pixel in values])
    bounds = edge.getbbox()
    assert bounds, "Generated label frame is missing"
    source = source.crop(bounds)
    grid = Image.new("RGBA", size)
    grid.putdata([source.getpixel((round(x * (source.width - 1) / (size[0] - 1)),
                                  round(y * (source.height - 1) / (size[1] - 1))))
                  for y in range(size[1]) for x in range(size[0])])
    return grid


def export(assets):
    TARGET.mkdir(parents=True, exist_ok=True)
    for asset in runtime_assets(assets):
        name = asset["id"]
        source = Image.open(ART / asset["selected_original"]).convert("RGBA")
        size = asset["width"], asset["height"]
        if name == "active_creative_tab":
            # Register the generated marker on the shared 32px tab canvas.
            marker = flatten(cropped_art(source).resize((12, 2), Image.Resampling.NEAREST), name)
            pixels = Image.new("RGBA", size)
            pixels.paste(marker, (10, 29))
        elif name == "wheel_ring_highlight":
            # Future exports author the whole shared ring; runtime crops its aimed sector
            # without rotating this lighting. --check never replaces manually edited PNGs.
            ring = Image.open(TARGET / "wheel_ring.png").convert("RGBA")
            pixels = flatten(ring, name)
        elif name in {"alphabet_label", "highlighted_alphabet_label"}:
            pixels = flatten(label_grid(source, size, name == "highlighted_alphabet_label"), name)
        else:
            if name in TRANSPARENT:
                source = cropped_art(source)
            pixels = flatten(source.resize(size, Image.Resampling.NEAREST), name)
        pixels.save(TARGET / (name + ".png"), optimize=True)
        asset["original_sha256"] = hashlib.sha256((ART / asset["selected_original"]).read_bytes()).hexdigest()
        asset["source_dimensions"] = list(Image.open(ART / asset["selected_original"]).size)
        asset["palette"] = palette_for(name)
        asset["production_sha256"] = hashlib.sha256((TARGET / (name + ".png")).read_bytes()).hexdigest()
        asset.pop("source", None)
    MANIFEST.write_text(json.dumps({"schema_version": 1, "generator": "built-in imagegen", "assets": assets}, indent=2) + "\n", encoding="utf-8")


def check(assets):
    assets = runtime_assets(assets)
    expected = {asset["id"] + ".png" for asset in assets}
    missing = expected - {path.name for path in TARGET.glob("*.png")}
    assert not missing, "Missing wheel textures: " + ", ".join(sorted(missing))
    for asset in assets:
        original = Image.open(TARGET / (asset["id"] + ".png"))
        original.load()
        assert original.format == "PNG" and original.size == (asset["width"], asset["height"]), asset["id"]
        # Minecraft's native decoder expands indexed/RGB PNGs to RGBA. Inspect
        # those decoded pixels without resaving or imposing a generated palette.
        image = original.convert("RGBA")
        alpha = image.getchannel("A")
        assert asset["id"] == "backdrop" or alpha.getextrema()[1] > 0, asset["id"]
    print(f"Verified {len(assets)} required decoded PNG textures and exact source dimensions; manual palettes, alpha and unused backups preserved")


def contact_sheet(assets):
    assets = runtime_assets(assets)
    cell_w, cell_h = 220, 180
    sheet = Image.new("RGB", (cell_w * 4, cell_h * 5), "#242830")
    draw = ImageDraw.Draw(sheet)
    for index, asset in enumerate(assets):
        x, y = index % 4 * cell_w, index // 4 * cell_h
        draw.text((x + 8, y + 6), asset["id"], fill="white")
        draw.text((x + 8, y + 22), f"{asset['width']} x {asset['height']}", fill="#bcbcbc")
        for row in range(y + 42, y + cell_h - 8, 8):
            for col in range(x + 8, x + cell_w - 8, 8):
                draw.rectangle((col, row, col + 7, row + 7), fill="#414141" if ((row - y) // 8 + (col - x) // 8) % 2 else "#555555")
        image = Image.open(TARGET / (asset["id"] + ".png")).convert("RGBA")
        scale = min(202 / image.width, 128 / image.height)
        preview = image.resize((max(1, round(image.width * scale)), max(1, round(image.height * scale))), Image.Resampling.NEAREST)
        sheet.paste(preview, (x + (cell_w - preview.width) // 2, y + 44 + (128 - preview.height) // 2), preview)
    sheet.save(ART / "contact-sheet.png")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--export", action="store_true", help="Overwrite production PNGs from preserved generated originals")
    parser.add_argument("--check", action="store_true", help="Validate production PNGs without changing them")
    parser.add_argument("--contact-sheet", action="store_true", help="Write the pixel-grid inspection sheet")
    args = parser.parse_args()
    assets = json.loads(MANIFEST.read_text(encoding="utf-8"))["assets"]
    if args.export:
        export(assets)
    check(assets)
    if args.contact_sheet:
        contact_sheet(assets)
