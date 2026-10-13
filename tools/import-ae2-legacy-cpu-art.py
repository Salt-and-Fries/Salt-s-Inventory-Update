"""Read the tiny CPU regions needed for desktop fidelity on AE2 15.

Only pixel data is imported, under the extension's own resource name. No native
AE2 class, whole texture or texture override is copied. The upstream input is read
only, and each region carries its original coordinates and RGBA digest.
"""
import hashlib
import json
from io import BytesIO
from pathlib import Path
from zipfile import ZipFile
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'build/ae2-port/appliedenergistics2-26.3.1-alpha.jar'
OUTPUT = ROOT / 'compatibility/ae2/versions/1.20.1/common/src/main/resources/ae2-legacy-cpu-art.json'
REGIONS = {
    'row': ('cpu_selector.png', (100, 0, 67, 22)),
    'rowSelected': ('cpu_selector.png', (100, 22, 67, 22)),
    'storage': ('states.png', (208, 224, 10, 10)),
    'processor': ('states.png', (208, 234, 10, 10)),
    'craft': ('states.png', (208, 244, 10, 10)),
    'terminal': ('states.png', (192, 224, 10, 10)),
    'machine': ('states.png', (192, 234, 10, 10)),
}


def read_regions():
    result = {}
    with ZipFile(SOURCE) as jar:
        for name, (texture, (x, y, width, height)) in REGIONS.items():
            resource = 'assets/ae2/textures/guis/' + texture
            image = Image.open(BytesIO(jar.read(resource))).convert('RGBA')
            rgba = bytes(channel for py in range(y, y + height) for px in range(x, x + width)
                         for channel in image.getpixel((px, py)))
            rectangles = []; previous = {}
            for py in range(height):
                runs = {}; px = 0
                while px < width:
                    color = image.getpixel((x + px, y + py)); end = px + 1
                    while end < width and image.getpixel((x + end, y + py)) == color: end += 1
                    if color[3]:
                        argb = (color[3] << 24) | (color[0] << 16) | (color[1] << 8) | color[2]
                        if argb >= 1 << 31: argb -= 1 << 32
                        key = (px, end - px, argb)
                        if key in previous:
                            index = previous[key]; rectangles[index][3] += 1
                        else:
                            index = len(rectangles); rectangles.append([px, py, end-px, 1, argb])
                        runs[key] = index
                    px = end
                previous = runs
            result[name] = {'resource': resource, 'sourceRect': [x,y,width,height],
                            'rgbaSha256': hashlib.sha256(rgba).hexdigest(),
                            'width':width, 'height':height, 'rectangles':rectangles}
    return result


if __name__ == '__main__':
    value = {'sourceArtifact':'Applied Energistics 2 26.3.1-alpha',
             'sourceJarSha256':hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
             'license':'LGPL-3.0-or-later', 'scope':'AE2 15 desktop CPU views only',
             'regions':read_regions()}
    OUTPUT.parent.mkdir(parents=True,exist_ok=True)
    OUTPUT.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
    print('Imported',len(value['regions']),'desktop CPU regions;',OUTPUT.stat().st_size,'bytes')
