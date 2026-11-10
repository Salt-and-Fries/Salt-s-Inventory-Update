"""Named opaque-art landmarks, compared with exact matched SIU-absent captures."""
from pathlib import Path
import json
from PIL import Image,ImageChops
ROOT=Path(__file__).resolve().parents[1]
def read(p):return json.loads(p.read_text(encoding='utf-8'))
SAMPLES=[
 ('blueprint','Flow arrow',(88,27,30,43)),
 ('schematicannon','Fuel field and right cap',(36,19,47,16)),
 ('schematicannon','Printer pictogram',(112,19,16,16)),
 ('requester','Wooden tray and apron',(16,21,196,35)),
 ('requester','Hook turns',(25,56,25,33)),
 ('schematic-table','Arrow on original material',(70,57,84,19)),
 ('controller','Original key pictograms',(20,20,140,10)),
 ('gauge','Target arrows and slot',(46,24,84,24)),
 ('creative-ammo','Charcoal material left',(4,14,20,38)),
 ('creative-ammo','Charcoal material right',(145,14,45,38)),
 ('proximity','Native blue material',(4,14,24,30)),
 ('frogport','Empty package cell row',(26,8,164,18)),
 ('postbox','Empty package cell row',(26,8,164,18)),
 ('package-filter','Address field frame',(12,24,194,18)),
 # Avoid the partially transparent right corner: it blends with the world.
 ('schedule','Opaque native upper paper edge',(16,14,220,4)),
 ('categories','Native category row',(4,19,184,18)),
 ('category-editor','Native editor upper panel',(4,14,182,8)),
 ('ammo','Native brown material',(4,14,20,38)),
 ('timed','Native left edge',(2,14,3,58)),
 ('inertia','Native left edge',(2,14,3,58)),
 ('impact','Native left edge',(2,14,3,58)),
]
def main():
    dest=ROOT/'output/create-native-all';pairs={p['id']:p for p in read(dest/'manifest.json')};checks=[]
    for ident,label,rect in SAMPLES:
        folder=Path(pairs[ident]['folder']);old=read(folder/'original-evidence.json')['client'];new=read(folder/'fixed-evidence.json')['client']
        win=next(w for w in new['allWindows'] if w['menuId']==pairs[ident]['menu']);r=next(r for r in win['composition']['regions'] if r['id']=='native-panel')
        n=old['bounds'];b=win['bounds'];s=r['source'];x,y,w,h=rect
        a=Image.open(folder/'original-full.png').convert('RGB').crop(((n['x']+x)*2,(n['y']+y)*2,(n['x']+x+w)*2,(n['y']+y+h)*2))
        xx=b['x']+r['x']+x-s['x'];yy=b['y']+win['templateState']['headerHeight']+r['y']+y-s['y']
        f=Image.open(folder/'fixed-full.png').convert('RGB').crop((xx*2,yy*2,(xx+w)*2,(yy+h)*2))
        bad=sum(p!=(0,0,0) for p in ImageChops.difference(a,f).get_flattened_data())
        checks.append({'window':ident,'asset':label,'native_rect':rect,'compared_pixels':w*h*4,'different_pixels':bad,'exact_match':bad==0})
    result={'scope':'Named static native artwork, not full-image identity; excludes titles, SIU controls, world pixels, dynamic models and tooltips','checks':checks,'all_exact':all(r['exact_match'] for r in checks)}
    (dest/'native-pixel-verification.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8');print(json.dumps(result,indent=2))
if __name__=='__main__':main()
