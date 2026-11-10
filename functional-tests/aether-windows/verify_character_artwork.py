"""Compare full Character slot frames/ghosts with actual native Aether captures."""
from pathlib import Path
import argparse,json
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]


def main():
    p=argparse.ArgumentParser();p.add_argument('output',type=Path);p.add_argument('--loader',required=True,choices=['fabric','forge']);a=p.parse_args();out=a.output
    d=json.loads((out/'aether-checks/character-aether-empty-state.json').read_text());w=next(w for w in d['workspaceWindows']if w['kind']=='CHARACTER');assert w['aetherMode'];c=w['content'];image=Image.open(out/'screenshots/character-aether-empty.png').convert('RGB')
    if a.loader=='forge':ref=json.loads((ROOT/'tmp/pdfs/aether/forge-accessories-reference.json').read_text())['native']
    else:ref=next(r for r in json.loads((ROOT/'output/remaining-ui-audit/coverage.json').read_text())['cases']if r['id']=='bb-aether-accessories')['native']
    native=Image.open(ROOT/ref['capture']).convert('RGB');b=ref['bounds'];cells=[]
    for name,x,y in [('helmet',59,8),('chest',59,26),('legs',59,44),('boots',59,62),('craft-1',116,18),('craft-2',134,18),('craft-3',116,36),('craft-4',134,36),('result',154,28),('offhand',116,62)]:cells.append((name,x,y))
    cells.extend((v['identifier']+str(v['index']),77+v['x'],8+v['y'])for v in w['aetherCells'])
    checks=[]
    for name,x,y in cells:
        expected=native.crop(((b['x']+x-1)*2,(b['y']+y-1)*2,(b['x']+x+17)*2,(b['y']+y+17)*2));actual=image.crop(((c['x']+x-1)*2,(c['y']+y-1)*2,(c['x']+x+17)*2,(c['y']+y+17)*2))
        differences=[max(abs(a-b)for a,b in zip(p,q))for p,q in zip(expected.getdata(),actual.getdata())];checks.append(dict(slot=name,maximum_channel_delta=max(differences),pixels_outside_tolerance=sum(d>1 for d in differences)))
    result=dict(loader=a.loader,slots=len(checks),channel_tolerance=1,failures=sum(v['pixels_outside_tolerance']>0 for v in checks),reference=ref['capture'],checks=checks)
    (out/'aether-checks/character-artwork.json').write_text(json.dumps(result,indent=2));print(json.dumps(result,indent=2));assert result['failures']==0


if __name__=='__main__':main()
