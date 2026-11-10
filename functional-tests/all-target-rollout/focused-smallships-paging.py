"""Focused cargo round trip and native ship-page replacement, through real clicks."""
from pathlib import Path
import argparse,json,re,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);a=p.parse_args();checks=[]

def act(kind,**kw):return send(a.run,kind,**kw)['client']
def state():return act('wait',ticks=3)
def ship(s):return next(w for w in s['windows'] if w.get('menuId') in ['minecraft:ship_container','smallships:ship_container'])
def check(name,ok):
 checks.append({'check':name,'passed':bool(ok)})
 (a.run/'smallships-cargo-paging-focused-checks.json').write_text(json.dumps({'scope':'One ship, normal opening, cargo transfer, next/previous page replacement, exact stored item recovery only','checks':checks},indent=2))
 assert ok,name
 print('PASS',name,flush=True)
def point(s,x,y):
 w=ship(s);b=w['bounds']
 r=next(r for r in w['template']['regions'] if r['interactive'] and r['source']['x']<=x<r['source']['x']+r['source']['width'] and r['source']['y']<=y<r['source']['y']+r['source']['height'])
 return b['x']+6+r['x']+x-r['source']['x'],b['y']+20+r['y']+y-r['source']['y']
def tap(x,y):return act('xy-click',x=x,y=y,ticks=15)
def page(s,label):
 w=ship(s);b=w['nativeBounds'];button=next(b for b in w['nativeWidgets'] if b['message']==label and b['active'] and b['visible'])
 return tap(*point(s,button['x']-b['x']+button['width']/2,button['y']-b['y']+button['height']/2))
def cargo(s):
 slot=next(v for v in ship(s)['nativeSlots'] if not v['player'] and v['active']);b=slot['bounds']
 return point(s,b['x']+b['width']/2,b['y']+b['height']/2)
def snapshot():
 act('snapshot-server',ticks=3);return json.loads((a.run/'server-state.json').read_text())
def menu(s):return next(m for m in s['menus'] if 'ShipContainerMenu' in m['class'])
def count(item):
 if 'minecraft:air' in item:return 0
 m=re.search(r'(?:Count|count):\s*(\d+)',item);return int(m[1]) if m else 0
act('close');act('fixture',block='minecraft:air',ticks=8)
for command in ['gamemode survival @a','clear @a','kill @e[type=!minecraft:player]',
 'summon smallships:galleon 8.5 65 8.5','tp @a 8.5 65 13 180 10',
 'item replace entity @a inventory.0 with minecraft:diamond 7']:
 act('command',command=command,ticks=8)
act('sneak',down=True,ticks=4);s=act('use-entity',entity='smallships:galleon',shift=True,ticks=24)
check('normal-opening-has-complete-unclipped-regions',not ship(s).get('layoutError') and {'cargo','statistics','pager'}.issubset({r['id'] for r in ship(s)['template']['regions']}))
if not any(w['kind']=='INVENTORY' for w in s['workspaceWindows']):s=act('inventory',ticks=8)
inv=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');cell=next(c for c in inv['inventoryCells'] if c['inventorySlot']==9)
s=tap(cell['x'],cell['y']);s=tap(*cargo(s));server=snapshot()
check('real-player-stack-enters-first-page-cargo','minecraft:diamond' in menu(server)['slots'][0]['item'] and count(menu(server)['slots'][0]['item'])==7)
s=page(s,'>');server=snapshot()
check('next-page-replaces-one-window-and-shows-different-cargo',sum(w.get('menuId') in ['minecraft:ship_container','smallships:ship_container'] for w in s['windows'])==1 and count(menu(server)['slots'][0]['item'])==0 and next(b for b in ship(s)['nativeWidgets'] if b['message']=='<')['active'])
s=page(s,'<');server=snapshot()
check('previous-page-restores-the-exact-stored-stack','minecraft:diamond' in menu(server)['slots'][0]['item'] and count(menu(server)['slots'][0]['item'])==7)
s=tap(*cargo(s));s=page(s,'>');server=snapshot()
check('paging-with-held-stack-keeps-seven-on-shared-cursor',all('minecraft:diamond' in m['carried'] and count(m['carried'])==7 for m in server['menus']))
s=page(s,'<');inv=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');cell=next(c for c in inv['inventoryCells'] if c['inventorySlot']==10);tap(cell['x'],cell['y']);server=snapshot()
stored=next(v['item'] for v in server['inventory'] if v['index']==10)
check('cargo-round-trip-returns-seven-with-empty-cursor','minecraft:diamond' in stored and count(stored)==7 and all(count(m['carried'])==0 for m in server['menus']))
