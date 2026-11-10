"""Focused read-only, native Info, modifying-list, and withdrawal checks."""
from pathlib import Path
import argparse,json,sys,re
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--grave-index',type=int,default=0);a=p.parse_args();state={};checks=[]
def act(kind,**kw):
    global state
    state=send(a.run,kind,**kw)['client'];return state
def window():return next(w for w in state.get('allWindows',[]) if w.get('menuId')=='salts_inventory_compat:universal_grave')
def tap(x,y):act('click',x=x,y=y,ticks=1);return act('release',x=x,y=y,ticks=12)
def snapshot():
    act('snapshot-server',ticks=2);return json.loads((a.run/'server-state.json').read_text())

def count(value):
    match=re.search(r'(?:Count|count):\s*(\d+)',value)
    return int(match[1]) if match else 0
def check(name,ok):
    checks.append({'check':name,'passed':bool(ok)});(a.run/'grave-focused-checks.json').write_text(json.dumps({'scope':'Read-only storage, native Info compass, modifying entry and one item withdrawal only','checks':checks},indent=2))
    assert ok,name
    print('PASS',name,flush=True)
act('wait',ticks=1);b=window()['bounds'];tap(b['x']+23,b['y']+33)
server=snapshot();check('read-only-preview-cannot-take-loot',window()['slots'][0]['count']==7 and all(count(m['carried'])==0 for m in server['menus']))
act('command',command='item replace entity @a inventory.0 with minecraft:compass',ticks=6)
act('inventory',ticks=5);inv=next(w for w in state['workspaceWindows'] if w['kind']=='INVENTORY');cell=next(c for c in inv['inventoryCells'] if c['inventorySlot']==9);tap(cell['x'],cell['y'])
tap(b['x']+23,b['y']+119);server=snapshot()
check('native-Info-turns-carried-compass-into-grave-compass',sum('universal_graves:grave_compass' in row['item'] and count(row['item'])==1 for row in server['inventory'])==1 and all(count(m['carried'])==0 for m in server['menus']))
act('close');act('command',command='execute as @a run graves modify',ticks=12)
check('modifying-list-remains-native','bounds' in state and not state.get('allWindows'))
b=state['bounds'];tap(b['x']+16+18*(a.grave_index%9),b['y']+26+18*(a.grave_index//9))
b=window()['bounds'];tap(b['x']+23,b['y']+33);server=snapshot()
# Legacy Core mirrors its shared player cursor into each observed menu. Count
# the actual carried stack once, rather than treating those views as duplicates.
carried=[m['carried'] for m in server['menus'] if count(m['carried'])]
check('modifying-view-withdraws-real-seven-diamonds',window()['slots'][0]['count']==0 and bool(carried) and all('minecraft:diamond' in value and count(value)==7 for value in carried))
act('inventory',ticks=5);inv=next(w for w in state['workspaceWindows'] if w['kind']=='INVENTORY');cell=next(c for c in inv['inventoryCells'] if c['inventorySlot']==9);tap(cell['x'],cell['y']);server=snapshot()
check('separate-player-inventory-receives-exact-loot','minecraft:diamond' in server['inventory'][9]['item'] and count(server['inventory'][9]['item'])==7)
