from pathlib import Path
from PIL import Image, ImageDraw
folder = Path(__file__).resolve().parent / 'src/main/resources/assets/companion_example/textures/gui'
folder.mkdir(parents=True, exist_ok=True)
for name, height, color in [('machine',64,(181,199,187)),('tool',52,(193,192,172))]:
    artwork=Image.new('RGBA',(176,height),(46,64,54,255))
    draw=ImageDraw.Draw(artwork)
    draw.rectangle((1,1,174,height-2),fill=(*color,255))
    draw.line((2,2,173,2),fill=(238,243,231,255))
    draw.rectangle((3,3,172,19),fill=(154,180,160,255))
    draw.line((3,20,172,20),fill=(70,97,76,255))
    # The original fixture's border/padding/title dimensions are shared by the vanilla and Salt presentation.
    artwork.save(folder/(name+'.png'))
