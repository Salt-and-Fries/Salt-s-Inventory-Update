"""Open Traveler's native settings through its real gear tab."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True)
p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);a=p.parse_args()
def act(kind,**kw):return send(a.run,kind,**kw)['client']
act('catalog',ticks=1);catalog=json.loads((a.run/'catalog.json').read_text())
if 'travelersbackpack:backpack_settings' not in catalog['menus']:
 print('NOT APPLICABLE: no separate settings container in this publisher release.');raise SystemExit(0)
act('close',ticks=2);act('command',command='difficulty peaceful',ticks=2)
act('command',command='gamemode survival @a',ticks=2);act('fixture',block='minecraft:stone',ticks=8)
act('held',item='travelersbackpack:standard',ticks=5);s=act('use-item',ticks=16)
if a.side=='native':
 b=s['bounds'];x=b['x']+185;y=b['y']+16
else:
 w=next(w for w in s['windows'] if w.get('nativeScreen','').endswith('.BackpackScreen'))
 b=w['bounds'];r=next(r for r in w['template']['regions'] if r['exterior'] and r['source']['y']==4)
 x=b['x']+r['x']+12;y=b['y']+r['y']+12
act('click',x=x,y=y,ticks=1);s=act('release',x=x,y=y,ticks=20)
screens=[w.get('nativeScreen','') for w in s.get('windows',[])] if a.side=='siu' else [s.get('screen','')]
assert any(screen.endswith('.BackpackSettingsScreen') for screen in screens),screens
capture(a.run,a.side,a.minecraft,a.loader,'travelers-settings','Original gear tab opens the native Backpack Settings container. Normal navigation and pictured layout only; this does not certify every upgrade configuration.')
print('PASS original backpack settings transition',flush=True)
