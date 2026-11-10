"""Extend SIU's existing pixel chrome for the 26.3 native-view experiment only."""
from pathlib import Path
from collections import Counter
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
DEST = ROOT / "artwork/shared-resources/core/assets/salts_inventory_update/textures/gui"
DEST.mkdir(parents=True, exist_ok=True)
source = Image.open(ROOT / "artwork/shared-resources/core/assets/salts_inventory_update/textures/gui/window_controls.png").convert("RGBA")
size = source.height // 3
sheet = Image.new("RGBA", (source.width + size, source.height))
sheet.paste(source, (0, 0))
for row in range(3):
    tile = source.crop((2 * size, row * size, 3 * size, (row + 1) * size))
    draw = ImageDraw.Draw(tile)
    # Retain the original bevel and replace only the X's glyph pixels.
    face = next(color for color, _ in Counter(tile.getpixel((x, y)) for y in range(2, size - 2) for x in range(2, size - 2)).most_common()
                if color[3] and color[:3] not in ((0, 0, 0), (255, 255, 255)))
    draw.rectangle((2, 2, size - 3, size - 3), fill=face)
    draw.rectangle((3, 3, size - 4, size - 4), outline=(0, 0, 0, 255))
    draw.line((3, 4, size - 4, 4), fill=(0, 0, 0, 255))
    sheet.paste(tile, (source.width, row * size))
sheet.save(DEST / "window_controls_native_view.png")

button = Image.new("RGBA", (96, 96))
for row, face in enumerate((198, 210, 170)):
    tile = Image.new("RGBA", (96, 32))
    draw = ImageDraw.Draw(tile)
    draw.polygon(((2, 0), (93, 0), (95, 2), (95, 29), (93, 31), (2, 31), (0, 29), (0, 2)), fill=(0, 0, 0, 255))
    draw.rectangle((2, 1, 92, 29), fill=(face, face, face, 255))
    draw.line((2, 1, 92, 1), fill=(255, 255, 255, 255))
    draw.line((1, 2, 1, 28), fill=(255, 255, 255, 255))
    draw.line((93, 2, 93, 29), fill=(85, 85, 85, 255))
    draw.line((2, 30, 93, 30), fill=(85, 85, 85, 255))
    button.paste(tile, (0, row * 32))
button.save(DEST / "desktop_top_tab.png")
