"""One real chest's hosted rendering, single-target drag and exact item recovery."""
from pathlib import Path
import argparse,json,re,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);a=p.parse_args();checks=[]
def act(kind,**kw):return send(a.run,kind,**kw)['client']
def chest(s):return next(w for w in s['windows'] if w.get('menuId')=='sophisticatedstorage:storage')
def check(name,ok):
 checks.append({'check':name,'passed':bool(ok)});(a.run/'sophisticated-slot-render-focused-checks.json').write_text(json.dumps({'scope':'One ordinary chest opening, native overlay rendering during a single-target drag, storage and withdrawal only','checks':checks},indent=2));assert ok,name;print('PASS',name,flush=True)
def count(nbt):
 if re.search(r'\bid:\s*"minecraft:air"',nbt):return 0
 m=re.search(r'(?:Count|count):\s*(\d+)',nbt);return int(m[1]) if m else 0
def snapshot():
 act('snapshot-server',ticks=3);return json.loads((a.run/'server-state.json').read_text())
def native_menu(s):return next(m for m in s['menus'] if 'sophisticatedstorage' in m['class'] and 'Container' in m['class'])
def point(s):
 w=chest(s);slot=next(v for v in w['slots'] if not v['player'] and v['index']==0);b=w['nativeBounds']
 return b['x']+slot['x']+8,b['y']+slot['y']+8
def tap(x,y):return act('xy-click',x=x,y=y,ticks=12)
act('close',ticks=3);act('fixture',block='sophisticatedstorage:chest',ticks=8)
act('command',command='gamemode survival @a',ticks=3);act('command',command='item replace entity @a inventory.0 with minecraft:diamond 7',ticks=4)
s=act('open',ticks=20);check('normal-storage-opening-and-overlays-remain-connected',s.get('worldLoaded') and chest(s)['definition'].endswith('.SophisticatedHostedScreenDefinition'))
s=act('inventory',ticks=8);inv=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');cell=next(v for v in inv['inventoryCells'] if v['inventorySlot']==9);s=tap(cell['x'],cell['y']);server=snapshot()
check('seven-real-diamonds-enter-shared-cursor',any('minecraft:diamond' in m['carried'] and count(m['carried'])==7 for m in server['menus']))
x,y=point(s);act('move',x=x,y=y,ticks=3);act('click',x=x,y=y,ticks=1);s=act('drag',x=x,y=y,dx=.1,dy=.1,button=0,ticks=8)
act('capture',capture='sophisticated-single-target-drag',ticks=12)
check('single-target-drag-renders-with-native-overlays',s.get('worldLoaded') and bool(chest(s)))
s=act('release',x=x,y=y,button=0,ticks=16);server=snapshot();item=native_menu(server)['slots'][0]['item']
check('single-target-drag-stores-exactly-seven','minecraft:diamond' in item and count(item)==7 and all(count(m['carried'])==0 for m in server['menus']))
s=tap(*point(s));inv=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');cell=next(v for v in inv['inventoryCells'] if v['inventorySlot']==10);tap(cell['x'],cell['y']);server=snapshot();stored=next(v['item'] for v in server['inventory'] if v['index']==10)
check('withdrawal-returns-exact-stack-to-separate-inventory','minecraft:diamond' in stored and count(stored)==7 and count(native_menu(server)['slots'][0]['item'])==0 and all(count(m['carried'])==0 for m in server['menus']))
