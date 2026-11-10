"""Focused pixel and geometry checks against actual Quark/SIU game captures."""
from pathlib import Path
import json
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'build/quark-window-implementation'
REFS={x['id']:x for x in json.loads((ROOT/'build/quark-window-design/native-references.json').read_text())['cases']}
MENU={'quark-backpack':'salts_inventory_compat:quark_backpack','quark-matrix-enchanter':'quark:matrix_enchanting',
      'quark-held-shulker':'quark:held_shulker_box','quark-feeding-trough':'quark:feeding_trough'}
CHECKS=[]
for ident in ['quark-crate','quark-feeding-trough','quark-crafter','quark-held-shulker','quark-backpack','quark-matrix-enchanter']:
    case=json.loads((WORK/(ident+'.json')).read_text());state=case['state']
    window=next(w for w in state['allWindows'] if w['menuId']==MENU.get(ident,'quark:'+ident[6:]));b=window['bounds']
    capture=Image.open(case['capture']).convert('RGB');scale=capture.width/state['guiWidth']
    actual=capture.crop((b['x']*scale,b['y']*scale,(b['x']+b['width'])*scale,(b['y']+b['height'])*scale)).resize((b['width'],b['height']),Image.Resampling.NEAREST)
    expected=Image.open(ROOT/'output/quark-window-design/original'/(ident+'.png')).convert('RGB')
    cells=[c for c in REFS[ident]['state']['slots'] if (c['index']>=46 if ident=='quark-backpack' else not c['player'])]
    if ident=='quark-crate':cells=cells[:54]
    pixels=0
    for cell in cells:
        sx,sy=cell['x']-1,cell['y']-1
        if ident=='quark-backpack':dx,dy=7+sx-4,26+sy-80
        else:
            region=next(p for p in window['composition']['regions'] if p['source']['x']<=sx and p['source']['y']<=sy
                        and sx+18<=p['source']['x']+p['source']['width'] and sy+18<=p['source']['y']+p['source']['height'])
            dx,dy=7+region['x']+sx-region['source']['x'],24+region['y']+sy-region['source']['y']
        assert dx>=0 and dy>=0 and dx+18<=b['width'] and dy+18<=b['height'],(ident,cell)
        # Count text can reach the lower/right rim. Compare the count-free rim portions.
        probes=[(x,0) for x in range(18)]+[(0,y) for y in range(1,18)]+[(17,y) for y in range(1,10)]+[(x,17) for x in range(1,6)]
        for x,y in probes:
            assert actual.getpixel((dx+x,dy+y))==expected.getpixel((sx+x,sy+y)),(ident,cell['index'],x,y)
            pixels+=1
    if ident=='quark-crafter':
        # The original result preview has a complete 26px frame beyond its 18px logical cell.
        region=window['composition']['regions'][0]['source']
        assert region['x']<=129 and region['x']+region['width']>=155 and region['y']<=30 and region['y']+region['height']>=56
    if ident=='quark-backpack':
        for y in range(61):
            for x in range(169):
                if 3<=x<165 and 3<=y<57:continue
                assert actual.getpixel((7+x,26+y))==expected.getpixel((4+x,80+y)),('Backpack stitched surround',x,y)
    actual.save(WORK/(ident+'-window.png'))
    CHECKS.append(dict(id=ident,complete_cells=len(cells),rim_pixels_compared=pixels,rim_pixel_differences=0,size=[b['width'],b['height']]))
assert sum(c['complete_cells'] for c in CHECKS)==130
report=dict(scope='Focused screenshot geometry and count-free rim pixels; no gameplay claims',complete_cells=130,
            rim_pixels_compared=sum(c['rim_pixels_compared'] for c in CHECKS),checks=CHECKS,
            notes='Full 18px bounds retained; count-overlay pixels excluded. Complete Crafter result frame and Backpack stitched surround checked separately.')
(WORK/'layout-checks.json').write_text(json.dumps(report,indent=2))
print('PASS: 130 complete cells,',report['rim_pixels_compared'],'matching native rim pixels, full Crafter result frame and Backpack stitching')
