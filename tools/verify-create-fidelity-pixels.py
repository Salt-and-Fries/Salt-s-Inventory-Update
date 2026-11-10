"""Compare repaired native artwork directly with matched no-SIU screenshots."""
from pathlib import Path
import json
from PIL import Image,ImageChops

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/create-native-fidelity-fixes'
def read(p):return json.loads(Path(p).read_text(encoding='utf-8'))

def main():
    pairs={p['id']:p for p in read(OUT/'acceptance-evidence.json')['pairs']}
    checks=[]
    for identifier,title,rect in [
        ('blueprint','Complete flow arrow and adjoining clear area',(88,27,30,43)),
        ('schematicannon','Fuel field including right cap',(36,19,47,16)),
        ('schematicannon','Restored printer pictogram',(112,19,16,16)),
        ('requester','Complete wooden tray and apron',(16,21,196,35)),
        ('requester','Hook upper turn and lower curl',(25,56,25,33)),
        ('schematic-table','Arrow on native gray action material',(70,57,84,19)),
        ('controller','Original key pictograms',(20,20,140,10)),
        ('gauge','Centered native target arrows and slot',(46,24,84,24)),
        ('creative-ammo','Creative charcoal material left of the controls',(4,14,20,38)),
        ('creative-ammo','Creative charcoal material right of the controls',(145,14,45,38)),
        ('proximity','Native blue material',(4,14,24,30)),
    ]:
        pair=pairs[identifier];base=Path(pair['board']).parent;old=read(base/'original-evidence.json')['client'];new=read(base/'fixed-evidence.json')['client'];window=next(w for w in new['allWindows'] if w['menuId']==pair['menu_id'])
        region=next(r for r in window['composition']['regions'] if r['id']=='native-panel');x,y,w,h=rect
        a=old['bounds'];b=window['bounds'];s=region['source']
        left=Image.open(base/'original-full.png').convert('RGB').crop(((a['x']+x)*2,(a['y']+y)*2,(a['x']+x+w)*2,(a['y']+y+h)*2))
        xx=b['x']+8+region['x']+x-s['x'];yy=b['y']+24+region['y']+y-s['y']
        right=Image.open(base/'fixed-full.png').convert('RGB').crop((xx*2,yy*2,(xx+w)*2,(yy+h)*2))
        different=sum(pixel!=(0,0,0) for pixel in ImageChops.difference(left,right).get_flattened_data())
        checks.append({'window':identifier,'asset':title,'native_gui_rectangle':list(rect),'compared_pixels':w*h*4,'different_pixels':different,'exact_match':different==0})
    result={'scope':'Named static native artwork only; excludes SIU chrome, world/background, tooltips and animated companion models','checks':checks,'all_exact':all(c['exact_match'] for c in checks)}
    (OUT/'native-pixel-verification.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(result,indent=2))

if __name__=='__main__':main()
