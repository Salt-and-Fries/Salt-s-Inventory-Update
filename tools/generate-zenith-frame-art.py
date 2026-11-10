"""Recolor SIU's existing 11-pixel frame to the approved Zenith palettes."""
from pathlib import Path
import json
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
source=ROOT/'artwork/shared-resources/core/assets/salts_inventory_update/textures/gui/window.png'
out=ROOT/'artwork/shared-resources/compatibility/assets/salts_inventory_compat/textures/gui/zenith'
out.mkdir(parents=True,exist_ok=True)
manifest=json.loads((ROOT/'output/zenith-window-design/manifest.json').read_text())
names={'zenith-library':'library','zenith-salvaging-table':'salvage','zenith-gem-cutting-table':'gem_cutting',
       'zenith-reforging-table':'reforging','zenith-augmenting-table':'augmenting'}
for check in manifest['checks']:
    if check['id'] not in names:continue
    palette=check['palette'];im=Image.open(source).convert('RGBA')
    def tint(v):
        g=v[0]
        rgb=tuple(round(c*g/198)for c in palette)if g<=198 else tuple(round(c+(255-c)*(g-198)/57*.30)for c in palette)
        return (*rgb,v[3])
    im.putdata([tint(v)for v in im.get_flattened_data()])
    assert im.getpixel((5,5))[:3]==tuple(palette)
    p=out/(names[check['id']]+'_window.png');im.save(p);print(p.relative_to(ROOT))
