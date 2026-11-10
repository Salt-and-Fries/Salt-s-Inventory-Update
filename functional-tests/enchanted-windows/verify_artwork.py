"""Check every live empty-cell border and ghost icon against publisher pixels."""
from pathlib import Path
from io import BytesIO
import json
import sys
import zipfile
import argparse
from PIL import Image

ROOT=Path(__file__).resolve().parents[2]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('output',type=Path)
parser.add_argument('menus',nargs='*')
parser.add_argument('--jar',type=Path,default=ROOT/'build/enchanted-implementation/enchanted-fabric-1.20.1-3.1.14.jar')
parser.add_argument('--asset-prefix',default='assets/enchanted/textures/gui/')
args=parser.parse_args()
out=args.output;result=[]
menus=args.menus or ['witch_oven','spinning_wheel','distillery','poppet_shelf']
with zipfile.ZipFile(args.jar) as jar:
    for menu in menus:
        name='enchanted-'+menu.replace('_','-')+'-empty'
        state=json.loads((out/'enchanted-checks'/(name+'-state.json')).read_text())
        shot=Image.open(out/'screenshots'/(name+'.png')).convert('RGB')
        atlas=Image.open(BytesIO(jar.read(args.asset_prefix+menu+'.png'))).convert('RGBA')
        w=next(w for w in state['allWindows'] if w.get('menuId')=='enchanted:'+menu)
        scale=shot.width/state['guiWidth'];assert scale==shot.height/state['guiHeight']==2
        for cell in w['slots']:
            if cell['player']:continue
            sx,sy=cell['x']-1,cell['y']-1
            region=next(r for r in w['composition']['regions'] if r['source']['x']<=sx and r['source']['y']<=sy
                and r['source']['x']+r['source']['width']>=sx+18 and r['source']['y']+r['source']['height']>=sy+18)
            dx=w['bounds']['x']+7+region['x']+sx-region['source']['x']
            dy=w['bounds']['y']+26+region['y']+sy-region['source']['y']
            differences=[]
            for y in range(18):
                for x in range(18):
                    expected=atlas.getpixel((sx+x,sy+y))[:3]
                    actual=shot.getpixel((int((dx+x+.5)*scale),int((dy+y+.5)*scale)))
                    if actual!=expected:differences.append(dict(x=x,y=y,expected=expected,actual=actual))
            row=dict(menu=menu,slot=cell['index'],checked_pixels=324,differences=differences)
            result.append(row)
            print(menu,cell['index'],'PASS' if not differences else f'FAIL {len(differences)} pixels')
assert len(result)==sum(dict(witch_oven=5,spinning_wheel=4,distillery=7,poppet_shelf=4)[m] for m in menus)
report=dict(scope='Complete empty slots and native ghost icons in the selected Enchanted menus; full gameplay suite excluded',
            slots=len(result),pixels=sum(r['checked_pixels'] for r in result),failures=sum(bool(r['differences']) for r in result),checks=result)
(out/'enchanted-checks/artwork-verification.json').write_text(json.dumps(report,indent=2))
assert report['failures']==0,report['failures']
