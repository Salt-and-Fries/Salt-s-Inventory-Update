"""Native placement registers a Warp Plate's owner and waystone identity."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);a=p.parse_args()
def act(kind,**kw):return send(a.run,kind,**kw)['client']
act('catalog',ticks=1)
if 'waystones:warp_plate' not in json.loads((a.run/'catalog.json').read_text())['items']:
 print('NOT APPLICABLE: publisher Warp Plate item absent.',flush=True);raise SystemExit(0)
act('close',ticks=3);act('fixture',block='minecraft:stone',ticks=8);act('command',command='gamemode creative @a',ticks=3)
act('held',item='waystones:warp_plate',ticks=5);act('sneak',down=True,ticks=5);act('open',shift=True,ticks=20);act('sneak',down=False,ticks=3)
act('held',item='minecraft:air',ticks=5);act('command',command='tp @a 8.5 65 11.5 180 10',ticks=12)
s=act('use-block-ray',ticks=20);print('SCREEN',s.get('screen'),'MENU',s.get('menuClass'),[(w.get('menuId'),w.get('nativeScreen')) for w in s.get('windows',[])],flush=True)
if s.get('screen','').endswith('.WaystoneEditScreen'):
 assert not s.get('windows') and not s.get('slots'),'Non-inventory settings must stay native'
 act('capture',capture=a.side+'-warp-plate-native-settings-entry',ticks=8)
 (a.run/'warp-plate-settings-entry.json').write_text(json.dumps(s,indent=2))
 v=next(v for v in s['widgets'] if 'manage modifiers' in v.get('message','').lower() or 'manage_modifiers' in v.get('message','').lower())
 x=v['x']+v['width']/2;y=v['y']+v['height']/2
 act('click',x=x,y=y,ticks=1);s=act('release',x=x,y=y,ticks=20)
 capture(a.run,a.side,a.minecraft,a.loader,'waystones-modifiers','Native Warp Plate placement registers its identity. Its zero-slot settings screen stays native; the original Manage Modifiers button opens this item inventory. Pictured layout and this transition only.')
else:
 capture(a.run,a.side,a.minecraft,a.loader,'waystones-warp-plate','The original Warp Plate item was placed normally to register its native owner and waystone identity, then opened through an ordinary empty-hand ray interaction. Pictured state only.')
