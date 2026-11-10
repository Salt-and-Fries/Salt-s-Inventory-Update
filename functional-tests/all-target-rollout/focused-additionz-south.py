"""Verify the entire native South glyph survives the integrated-window crop."""
from pathlib import Path
import argparse,json
from PIL import Image

p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--minecraft',required=True);a=p.parse_args()
root=Path(__file__).resolve().parents[2]
rows=json.loads((root/'build/all-target-rollout/ui-capture-ledger.json').read_text())
r=next(r for r in reversed(rows) if r['side']=='siu' and r['ui']=='additionz-chunk-loader' and Path(r['run']).resolve()==a.run.resolve())
s=r['state'];w=s.get('allWindows',s.get('windows'))[0];regions=w['template']['regions'];region=next(v for v in regions if v['id']=='body');b=w['bounds'];src=region['source']
frame=Image.open(r['screenshot']).convert('RGB');native=Image.open(root/f'build/window-detail-fixes-05/chunk_loader-{a.minecraft.replace(".","")[:4]}.png').convert('RGB')
sx=frame.width/s['guiWidth'];sy=frame.height/s['guiHeight'];same=[]
for y in range(72,80):
    for x in range(38,49):
        dx=b['x']+6+region['x']+x-src['x'];dy=b['y']+20+region['y']+y-src['y']
        same.append(frame.getpixel((int((dx+.5)*sx),int((dy+.5)*sy)))==native.getpixel((x,y)))
checks=[dict(check='native-south-glyph-inside-source-region',passed=src['y']+src['height']>=80),dict(check='all-eight-native-south-glyph-rows-render-exactly',passed=all(same)),dict(check='crop-stops-before-player-inventory-border',passed=src['y']+src['height']<83)]
(a.run/'additionz-south-focused-checks.json').write_text(json.dumps(dict(scope='Native opening, source crop and 88 South-glyph pixel samples only; no chunk-loading gameplay suite',checks=checks),indent=2))
for v in checks:
    assert v['passed'],v['check'];print('PASS',v['check'])
