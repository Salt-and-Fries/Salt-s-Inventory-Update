"""Compare stable native working pixels; ignore live fills and item interiors."""
from pathlib import Path
import hashlib,json
from PIL import Image

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'output/kibe-all-targets'
NATIVE=OUT/'1.21.1-fabric-native-kibe-reference-01'
RUNS=['1.21.1-fabric-siu-kibe-modern-01','1.21.1-neoforge-integrated-kibe-modern-01']
def read(p):return json.loads(p.read_text(encoding='utf-8'))
def logical(path,state):
    im=Image.open(path).convert('RGB')
    return im.resize((state['guiWidth'],state['guiHeight']),Image.Resampling.NEAREST)
def main():
    cases=read(ROOT/'output/kibe-window-design/manifest.json')['cases']
    originals={c['id']:c for c in read(NATIVE/'native-captures.json')};rows=[]
    for run in RUNS:
        folder=OUT/run;journal=read(folder/('journal-'+run+'.json'));captures={c['id']:c for c in journal['captures']}
        for case in cases:
            id=case['id'];original=originals[id];state=read(Path(original['state']));current=read(folder/(id+'-client.json'));cap=captures[id]
            a=logical(original['screenshot'],state);b=logical(cap['screenshot'],current)
            bounds=state['bounds'];dest=cap['bounds'];region=cap['composition']['regions'][0];s=region['source']
            window=next(w for w in current['allWindows']if w.get('menuId','').startswith('kibe:'))
            current_counts={slot['index']:slot['count']for slot in window['slots']}
            tested=diff=0;points=[]
            for y in range(s['y'],s['y']+s['height']):
                for x in range(s['x'],s['x']+s['width']):
                    # Item icons/counts and live bar progress vary across captures.
                    if any(not slot['player']and slot['x']<=x<slot['x']+16 and slot['y']<=y<slot['y']+16 for slot in state['slots']):continue
                    # Vanilla count glyphs and shadows can extend beyond the
                    # sixteen-pixel icon onto the lower/right slot rim.
                    if any(not slot['player']and max(slot['count'],current_counts.get(slot['index'],0))>1
                           and slot['x']+2<=x<slot['x']+18 and slot['y']+8<=y<slot['y']+18 for slot in state['slots']):continue
                    if 'generator'in id and ((8<=x<78 or 98<=x<168)and 19<=y<33):continue
                    ax=bounds['x']+x;ay=bounds['y']+y
                    bx=dest['x']+7+region['x']+x-s['x'];by=dest['y']+26+region['y']+y-s['y']
                    tested+=1
                    if a.getpixel((ax,ay))!=b.getpixel((bx,by)):
                        diff+=1
                        if len(points)<8:points.append([x,y,a.getpixel((ax,ay)),b.getpixel((bx,by))])
            rows.append(dict(run=run,id=id,pixels=tested,differing=diff,samples=points))
    report=dict(scope='Native 1.21.1 Fabric screenshots compared with each loader; working atlases are byte-identical across loaders. Live bar interiors, item interiors and count-glyph overhang excluded.',
                compared_pixels=sum(r['pixels']for r in rows),differing_pixels=sum(r['differing']for r in rows),views=rows)
    (OUT/'artwork-verification.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print(json.dumps({k:v for k,v in report.items()if k!='views'},indent=2))
    print([(r['run'],r['id'],r['differing'])for r in rows if r['differing']])
if __name__=='__main__':main()
