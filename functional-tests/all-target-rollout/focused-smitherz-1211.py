"""Configured native SmitherZ socket opening, insertion and exact recovery."""
from pathlib import Path
import argparse,json,re,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);a=p.parse_args();checks=[]
def act(kind,**kw):return send(a.run,kind,**kw)['client']
def check(name,ok):
 checks.append({'check':name,'passed':bool(ok)});(a.run/'smitherz-focused-checks.json').write_text(json.dumps({'scope':'Configured linking-menu construction, three native sockets, hammer/gem insertion, exact recovery and original tab return. Random linking outcomes are outside this focused check.','checks':checks},indent=2));assert ok,name;print('PASS',name,flush=True)
def snap():
 act('snapshot-server',ticks=3);return json.loads((a.run/'server-state.json').read_text())
def tap(x,y):return act('xy-click',x=x,y=y,ticks=14)
def window(s):return next(w for w in s['windows'] if w.get('menuId')=='smitherz:smither')
def point(s,index):
 w=window(s);v=next(v for v in w['slots'] if v['index']==index);x=v['x']+8;y=v['y']+8
 r=next(r for r in w['template']['regions'] if not r['exterior'] and r['source']['x']<=x<r['source']['x']+r['source']['width'] and r['source']['y']<=y<r['source']['y']+r['source']['height'])
 b=w['bounds'];pad=w['template']['padding'];return b['x']+6+pad+r['x']+x-r['source']['x'],b['y']+20+pad+r['y']+y-r['source']['y']
def inventory(s,index):
 w=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');v=next(v for v in w['inventoryCells'] if v['inventorySlot']==index);return v['x'],v['y']
act('close',ticks=3);act('fixture',block='minecraft:smithing_table',ticks=8);act('command',command='gamemode survival @a',ticks=3);act('command',command='clear @a',ticks=3)
s=act('open',ticks=16);b=s['windows'][0]['bounds'];s=tap(b['x']+39,b['y']-10)
check('original-linking-tab-opens-native-configured-menu',window(s)['nativeScreen'].endswith('.SmitherScreen') and not window(s).get('layoutError') and len(window(s)['slots'])>38)
for index,item in [(0,'minecraft:iron_sword[smitherz:gems={gems:[],size:3}]'),(1,'smitherz:smither_hammer_4'),(2,'smitherz:gem[smitherz:gem="smitherz:strength_1"]')]:act('command',command=f'item replace entity @a inventory.{index} with {item}',ticks=4)
if not any(w['kind']=='INVENTORY'for w in s['workspaceWindows']):s=act('inventory',ticks=8)
s=tap(*inventory(s,9));s=tap(*point(s,0))
check('equipment-activates-three-original-sockets',sum(v['active'] for v in window(s)['nativeSlots'] if not v['player'] and v['menuSlotId']>=2)==3)
s=tap(*inventory(s,10));s=tap(*point(s,1));s=tap(*inventory(s,11));s=tap(*point(s,2));server=snap();m=next(m for m in server['menus']if m['class'].endswith('.SmitherScreenHandler'))
check('original-slots-accept-native-hammer-and-gem','smither_hammer_4' in m['slots'][1]['item'] and 'strength_1' in m['slots'][2]['item'])
for target,index in [(11,2),(10,1),(9,0)]:s=tap(*point(s,index));s=tap(*inventory(s,target))
server=snap();items={v['index']:v['item']for v in server['inventory']};m=next(m for m in server['menus']if m['class'].endswith('.SmitherScreenHandler'))
check('all-three-items-recover-once-with-clear-cursor','iron_sword' in items[9] and 'smither_hammer_4' in items[10] and 'strength_1' in items[11] and all(v['item']=='{}'for v in m['slots'][:2]) and all(x['carried']=='{}'for x in server['menus']))
b=window(s)['bounds'];s=tap(b['x']+11,b['y']-10);check('first-tab-returns-to-native-smithing',any(w.get('menuId')=='minecraft:smithing'for w in s['windows']))
