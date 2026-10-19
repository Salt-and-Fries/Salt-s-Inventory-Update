"""Export the imagegen design onto the editable 64x32 Minecraft pixel grid."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
source = Image.open(Path(__file__).with_name('tuck-away-generated.png')).convert('RGBA')
pixels = source.resize((64, 32), Image.Resampling.NEAREST)
# A fixed Minecraft-style palette keeps every logical pixel a single flat color.
palette = [tuple(bytes.fromhex(value)) for value in [
    '101010', '202020', '303030', '414141', '555555', '6c6c6c', '858585', 'a0a0a0',
    'bcbcbc', 'dddddd', 'f5f5ec', 'fff8cc', 'f8e78a', 'ffe34d', 'd4af36', '9c7d22',
]]
output = Image.new('RGBA', pixels.size)
pixel_values = [pixels.getpixel((x, y)) for y in range(32) for x in range(64)]
output.putdata([
    (*min(palette, key=lambda color: sum((color[i] - pixel[i]) ** 2 for i in range(3))), 255)
    if pixel[3] >= 128 else (0, 0, 0, 0) for pixel in pixel_values
])
target = ROOT / 'src/main/resources/assets/salts_inventory_controlify/textures/gui/workspace/tuck_away.png'
target.parent.mkdir(parents=True, exist_ok=True)
output.save(target)
assert output.size == (64, 32) and {output.getpixel((x, y))[3] for y in range(32) for x in range(64)} == {0, 255}
assert output.crop((0, 0, 32, 32)).tobytes() != output.crop((32, 0, 64, 32)).tobytes()
print(target)
