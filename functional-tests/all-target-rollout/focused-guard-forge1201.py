"""A normally opened guard's equipment and separate-inventory round trip."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);a=p.parse_args();checks=[]
def act(kind,**kw):return send(a.run,kind,**kw)['client']
def check(label,ok):
 checks.append({'check':label,'passed':bool(ok)});(a.run/'guard-equipment-focused-checks.json').write_text(json.dumps({'scope':'Natural Guard inventory opening, helmet insertion, recovery and close; other behavior controls not tested','checks':checks},indent=2));assert ok,label;print('PASS',label,flush=True)
def guard(s):return next(w for w in s['windows'] if w.get('menuId')=='salts_inventory_compat:guard')
def snapshot():
 if a.run.parent.parent.name=='all-target-rollout':
  s=send(a.run,'wait',ticks=3)['server']
  return {'menus':[{'class':s['menu'],'slots':[{'item':json.dumps(v)} for v in s['slots']]}],
          'inventory':[{'index':v['index'],'item':json.dumps(v)} for v in s['playerInventory']]}
 act('snapshot-server',ticks=3);return json.loads((a.run/'server-state.json').read_text())
def tap(x,y):return act('xy-click',x=x,y=y,ticks=12)
s=act('wait',ticks=2);check('native-guard-factory-and-screen-open',guard(s)['nativeScreen'].endswith('.GuardInventoryScreen') and not guard(s).get('layoutError'))
act('command',command='item replace entity @a inventory.0 with minecraft:iron_helmet',ticks=4)
if not any(w['kind']=='INVENTORY' for w in s['workspaceWindows']):s=act('inventory',ticks=8)
inv=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');v=next(v for v in inv['inventoryCells'] if v['inventorySlot']==9);s=tap(v['x'],v['y']);w=guard(s);r=next(r for r in w['template']['regions'] if r['id']=='body/0');x=w['bounds']['x']+8+r['x']+9;y=w['bounds']['y']+24+r['y']+9
s=tap(x,y);server=snapshot();m=next(m for m in server['menus'] if m['class'].endswith('.GuardContainer'))
check('native-equipment-accepts-helmet','minecraft:iron_helmet' in m['slots'][0]['item'])
s=tap(x,y);inv=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');v=next(v for v in inv['inventoryCells'] if v['inventorySlot']==10);tap(v['x'],v['y']);server=snapshot();m=next(m for m in server['menus'] if m['class'].endswith('.GuardContainer'))
check('helmet-recovers-to-separate-inventory',any(v['index']==10 and 'minecraft:iron_helmet' in v['item'] for v in server['inventory']) and (m['slots'][0]['item']=='{}' or 'minecraft:air' in m['slots'][0]['item']))
s=act('close',ticks=8);check('guard-window-closes-without-disconnect',s.get('worldLoaded') and not s['windows'])
