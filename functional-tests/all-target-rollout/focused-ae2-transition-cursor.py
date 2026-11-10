"""Keep one exact shared cursor across AE2's original submenu replacements."""
from pathlib import Path
import argparse,json,re,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);a=p.parse_args();checks=[]
def act(kind,**kw):return send(a.run,kind,**kw)['client']
def check(label,ok):
 checks.append({'check':label,'passed':bool(ok)});(a.run/'ae2-transition-cursor-focused-checks.json').write_text(json.dumps({'scope':'Priority increment and two native submenu replacements while carrying exactly seven diamonds; recovery to separate inventory','checks':checks},indent=2));assert ok,label;print('PASS',label,flush=True)
def snapshot():
 act('snapshot-server',ticks=3);return json.loads((a.run/'server-state.json').read_text())
def count(nbt):
 if 'minecraft:air' in nbt:return 0
 m=re.search(r'(?:Count|count):\s*(\d+)',nbt);return int(m[1]) if m else 0
def button(s,name):
 v=next(v for w in s['windows'] for v in w['nativeWidgets'] if v.get('visible') and v.get('message')==name)
 return act('xy-click',x=v['x']+v['width']/2,y=v['y']+v['height']/2,ticks=16)
def carrying(server):
 # The physical inventory menu owns the shared cursor. An inactive native
 # submenu may expose an empty mirror until its authenticated action runs.
 owner=next((m for m in server['menus'] if m['class'].endswith('.InventoryMenu')),None)
 return owner is not None and 'minecraft:diamond' in owner['carried'] and count(owner['carried'])==7 and all(count(m['carried'])==0 or ('minecraft:diamond' in m['carried'] and count(m['carried'])==7) for m in server['menus'])
s=act('wait',ticks=2);check('real-priority-submenu-open',s['windows'][0]['menuId']=='ae2:priority')
act('command',command='gamemode survival @a',ticks=4);act('command',command='clear @a',ticks=3);act('command',command='item replace entity @a inventory.0 with minecraft:diamond 7',ticks=4)
if not any(w['kind']=='INVENTORY' for w in s['workspaceWindows']):s=act('inventory',ticks=8)
inv=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');v=next(v for v in inv['inventoryCells'] if v['inventorySlot']==9);s=act('xy-click',x=v['x'],y=v['y'],ticks=10)
check('separate-inventory-provides-seven-real-diamonds',carrying(snapshot()))
s=button(s,'+1');act('reviewed-block-probe',ticks=2);nbt=json.loads((a.run/'reviewed-block-probe.json').read_text())['nbt'];check('native-priority-action-updates-host',re.search(r'priority:1(?:[,}])',nbt) is not None)
s=button(s,'ME Storage Bus');check('return-to-owning-bus-preserves-exact-cursor',s['windows'][0]['menuId']=='ae2:storagebus' and carrying(snapshot()))
s=button(s,'Priority');check('reopen-priority-preserves-exact-cursor',s['windows'][0]['menuId']=='ae2:priority' and carrying(snapshot()))
inv=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');v=next(v for v in inv['inventoryCells'] if v['inventorySlot']==10);act('xy-click',x=v['x'],y=v['y'],ticks=10);server=snapshot()
check('cursor-recovers-once-with-no-extra-diamonds',sum(count(v['item']) for v in server['inventory'] if 'minecraft:diamond' in v['item'])==7 and all(count(m['carried'])==0 for m in server['menus']))
