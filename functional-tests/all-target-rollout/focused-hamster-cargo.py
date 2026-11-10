"""One normally opened hamster's native cargo and exact recovery only."""
from pathlib import Path
import argparse,json,re,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--pet',choices=['hamster','ministrosity'],default='hamster');a=p.parse_args();checks=[]
menu_ids=['salts_inventory_compat:hamster','spawn:hamster_inventory'] if a.pet=='hamster' else ['salts_inventory_compat:ministrosity']
screen_suffix='.HamsterInventoryScreen' if a.pet=='hamster' else '.MinistrosityInventoryScreen'
menu_suffix='.HamsterInventoryMenu' if a.pet=='hamster' else '.MinistrostiyMenu'
def act(kind,**kw):return send(a.run,kind,**kw)['client']
def check(label,ok):
 checks.append({'check':label,'passed':bool(ok)});(a.run/(a.pet+'-cargo-focused-checks.json')).write_text(json.dumps({'scope':'Ordinary '+a.pet+' inventory, seven-apple insertion and recovery, normal close; no full pet gameplay suite','checks':checks},indent=2));assert ok,label;print('PASS',label,flush=True)
def window(s):return next(w for w in s['windows'] if w.get('menuId')in menu_ids)
def snapshot():
 act('snapshot-server',ticks=3);return json.loads((a.run/'server-state.json').read_text())
def count(nbt):
 if 'minecraft:air' in nbt:return 0
 m=re.search(r'(?:Count|count):\s*(\d+)',nbt);return int(m[1]) if m else 0
def cargo(s):
 w=window(s);v=next(v for v in w['slots'] if not v['player']);x=v['x']+8;y=v['y']+8;r=next(r for r in w['template']['regions'] if r['source']['x']<=x<r['source']['x']+r['source']['width'] and r['source']['y']<=y<r['source']['y']+r['source']['height']);b=w['bounds']
 return b['x']+6+r['x']+x-r['source']['x'],b['y']+20+r['y']+y-r['source']['y']
def tap(x,y):return act('xy-click',x=x,y=y,ticks=12)
def menu(s):return next(m for m in s['menus'] if m['class'].endswith(menu_suffix))
s=act('wait',ticks=2);check('native-'+a.pet+'-menu-and-renderer-open',window(s)['nativeScreen'].endswith(screen_suffix) and not window(s).get('layoutError'))
act('command',command='gamemode survival @a',ticks=4);act('command',command='item replace entity @a inventory.0 with minecraft:apple 7',ticks=4)
if not any(w['kind']=='INVENTORY' for w in s['workspaceWindows']):s=act('inventory',ticks=8)
inv=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');v=next(v for v in inv['inventoryCells'] if v['inventorySlot']==9);s=tap(v['x'],v['y']);s=tap(*cargo(s));server=snapshot();stored=menu(server)['slots'][0]['item'];check('seven-apples-enter-native-pet-cargo','minecraft:apple' in stored and count(stored)==7)
s=tap(*cargo(s));inv=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');v=next(v for v in inv['inventoryCells'] if v['inventorySlot']==10);tap(v['x'],v['y']);server=snapshot();stored=next(v['item'] for v in server['inventory'] if v['index']==10)
check('exact-stack-recovers-with-clear-cursor','minecraft:apple' in stored and count(stored)==7 and count(menu(server)['slots'][0]['item'])==0 and all(count(m['carried'])==0 for m in server['menus']))
s=act('close',ticks=8);check('native-pet-window-closes-normally',s.get('worldLoaded') and not s['windows'])
