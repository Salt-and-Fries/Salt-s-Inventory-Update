"""Pixel checks on complete empty slot frames, ghosts and the Incubator meter."""
from pathlib import Path
from io import BytesIO
import argparse
import hashlib
import json
import zipfile
from PIL import Image

ROOT=Path(__file__).resolve().parents[2]
FORGE=ROOT/'build/ui-modpack-audit-20261025/jars/e2fdd161f35ca1b6db774906e88e266d7f12eab6.jar'
CASES={
    'altar':[(55,16,18,18),(55,52,18,18),(110,30,26,26)],
    'freezer':[(55,16,18,18),(55,52,18,18),(110,30,26,26)],
    'incubator':[(72,16,18,18),(72,52,18,18),(103,16,9,54)],
    'lore':[(82,62,18,18)],
}


def main():
    p=argparse.ArgumentParser();p.add_argument('output',type=Path);p.add_argument('--loader',required=True,choices=['fabric','forge']);a=p.parse_args();out=a.output
    checks=[];slots=0;pixels=0
    for name,rects in CASES.items():
        case='book_of_lore' if name=='lore' else name
        capture='aether-lore-empty' if name=='lore' else f'aether-{name}-empty'
        d=json.loads((out/'aether-checks'/(capture+'-state.json')).read_text());w=next(w for w in d['allWindows'] if w.get('menuId')=='aether:'+case);plan=w['composition'];image=Image.open(out/'screenshots'/(capture+'.png')).convert('RGBA')
        with zipfile.ZipFile(FORGE) as z:source=Image.open(BytesIO(z.read('assets/aether/textures/gui/menu/'+('lore_book' if name=='lore' else name)+'.png'))).convert('RGBA')
        for sx,sy,width,height in rects:
            region=next(r for r in plan['regions'] if r['source']['x']<=sx and r['source']['y']<=sy and r['source']['x']+r['source']['width']>=sx+width and r['source']['y']+r['source']['height']>=sy+height)
            x=w['bounds']['x']+7+plan['padding']+region['x']+sx-region['source']['x'];y=w['bounds']['y']+26+plan['padding']+region['y']+sy-region['source']['y']
            target=image.crop((x*2,y*2,(x+width)*2,(y+height)*2)).resize((width,height),Image.Resampling.NEAREST)
            ox,oy=(12,2) if name=='lore' else (0,0);expected=source.crop((sx-ox,sy-oy,sx-ox+width,sy-oy+height))
            mismatch=sum(p!=q for p,q in zip(target.getdata(),expected.getdata()));checks.append({'case':case,'source':[sx,sy,width,height],'mismatch':mismatch});pixels+=width*height
            if not(name=='incubator' and sx==103):slots+=1
    # Equipment glyphs are copied from the actual native references, not a
    # guessed generic equipment icon or substituted plain slot.
    capture='aether-equipment-empty';d=json.loads((out/'aether-checks'/(capture+'-state.json')).read_text());w=next(w for w in d['allWindows'] if w.get('menuId')=='salts_inventory_compat:aether_equipment');image=Image.open(out/'screenshots'/(capture+'.png')).convert('RGBA')
    if a.loader=='forge':ref=json.loads((ROOT/'tmp/pdfs/aether/forge-accessories-reference.json').read_text())['native']
    else:ref=next(r for r in json.loads((ROOT/'output/remaining-ui-audit/coverage.json').read_text())['cases'] if r['id']=='bb-aether-accessories')['native']
    native=Image.open(ROOT/ref['capture']).convert('RGBA');b=ref['bounds'];width=max(c['x']+20 for c in w['aetherCells'])
    for cell in w['aetherCells']:
        sx=77+cell['x']-1;sy=8+cell['y']-1;x=w['bounds']['x']+(w['bounds']['width']-width)//2+cell['x'];y=w['bounds']['y']+29+cell['y']
        expected=native.crop(((b['x']+sx)*2,(b['y']+sy)*2,(b['x']+sx+18)*2,(b['y']+sy+18)*2))
        target=image.crop((x*2,y*2,(x+18)*2,(y+18)*2))
        # Native inventory captures differ by one RGB level from the desktop
        # framebuffer. Do not mistake that rounding for a lost or shifted pixel.
        delta=[max(abs(a-b) for a,b in zip(p,q)) for p,q in zip(target.getdata(),expected.getdata())]
        mismatch=sum(d>1 for d in delta)
        checks.append({'case':'equipment','identifier':cell['identifier'],'index':cell['index'],'mismatch_at_scale2':mismatch,'maximum_channel_delta':max(delta),'channel_tolerance':1});slots+=1;pixels+=324
    result={'scope':'Complete slot artwork and full Incubator meter at GUI scale 2','loader':a.loader,'slots':slots,'logical_pixels':pixels,'checks':checks,'failures':sum(c.get('mismatch',c.get('mismatch_at_scale2',0))>0 for c in checks)}
    (out/'aether-checks/artwork.json').write_text(json.dumps(result,indent=2));print(json.dumps(result,indent=2));assert result['failures']==0


if __name__=='__main__':main()
