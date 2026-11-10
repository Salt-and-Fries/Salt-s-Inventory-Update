"""Native portable-workbench cell, ghost filter, toolbar and recovery checks."""
from pathlib import Path
import argparse,json,sys
from evidence import send,capture

p=argparse.ArgumentParser();p.add_argument('run',type=Path);a=p.parse_args();checks=[]
def act(kind,**kw):return send(a.run,kind,**kw)['client']
def window(s):return next(w for w in s.get('allWindows',s.get('windows',[])) if w.get('nativeScreen','').endswith('.PortableCellWorkbenchScreen'))
def check(name,okay):
    checks.append(dict(check=name,passed=bool(okay)))
    (a.run/'mega-workbench-functional-checks.json').write_text(json.dumps(dict(scope='Portable workbench native cell, ghost filter, copy/clear controls and item recovery only; not a full MEGA Cells suite',checks=checks),indent=2))
    assert okay,name
    print('PASS',name,flush=True)
def tap(x,y):return act('xy-click',x=x,y=y,ticks=14)
def cell(s,index):
    inv=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');v=next(c for c in inv['inventoryCells'] if c['inventorySlot']==index);return v['x'],v['y']
def point(s,index):
    w=window(s);slot=next(c for c in w['slots'] if c['index']==index);b=w['nativeBounds'];return b['x']+slot['x']+8,b['y']+slot['y']+8
def snapshot():
    act('snapshot-server',ticks=3);return json.loads((a.run/'server-state.json').read_text())
def menu(t):return next(m for m in t['menus'] if m['class'].endswith('.PortableCellWorkbenchMenu'))
def slot(t,index):return menu(t)['slots'][index]['item']

act('close',ticks=3);act('command',command='gamemode survival @a',ticks=3);act('command',command='clear @a',ticks=3)
act('held',item='megacells:portable_cell_workbench',ticks=3)
for command in ['item replace entity @a inventory.0 with ae2:item_storage_cell_1k','item replace entity @a inventory.1 with minecraft:stone 16']:act('command',command=command,ticks=3)
s=act('use-item',ticks=16);s=act('inventory',ticks=8)
check('complete-native-workbench-is-open',window(s)['bounds']['width']==176 and window(s)['bounds']['height']==162)
s=tap(*cell(s,9));s=tap(*point(s,0));t=snapshot()
check('real-cell-can-be-inserted','ae2:item_storage_cell_1k' in slot(t,0))
w=window(s);ghost=next(v for v in w['slots'] if not v['player'] and v['x']==8 and v['y']==29)
s=tap(*cell(s,10));s=tap(*point(s,ghost['index']));t=snapshot()
check('ghost-filter-reaches-native-menu-without-consuming-stack','minecraft:stone' in slot(t,ghost['index']) and any('minecraft:stone' in m['carried'] and ('count:16' in m['carried'] or 'count: 16' in m['carried']) for m in t['menus']))
s=tap(*cell(s,10));before=window(s).get('aeCopyMode')
toggle=next(v for v in window(s)['nativeWidgets'] if v['visible'] and v['type'].endswith('.ToggleButton'))
s=tap(toggle['x']+toggle['width']/2,toggle['y']+toggle['height']/2)
check('native-copy-mode-button-still-works',before is not None and window(s).get('aeCopyMode')!=before)
s=tap(toggle['x']+toggle['width']/2,toggle['y']+toggle['height']/2)
check('native-copy-mode-can-be-restored',window(s).get('aeCopyMode')==before)
capture(a.run,'siu','1.21.1','neoforge','megacells-portable-cell-workbench-populated','Native cell inserted and ghost filter configured through real window clicks; copy-mode toolbar checked.')
clear=next(v for v in window(s)['nativeWidgets'] if v['visible'] and v.get('message','').startswith('Clear'))
s=tap(clear['x']+clear['width']/2,clear['y']+clear['height']/2);t=snapshot()
check('native-clear-button-clears-filter','minecraft:stone' not in slot(t,ghost['index']))
s=tap(*point(s,0));s=tap(*cell(s,9));act('close',ticks=8);t=snapshot();inventory={v['index']:v['item'] for v in t['inventory']}
check('cell-and-input-stack-recover-with-clear-cursor','ae2:item_storage_cell_1k' in inventory[9] and 'minecraft:stone' in inventory[10] and ('count:16' in inventory[10] or 'count: 16' in inventory[10]) and all(m['carried']=='{}' for m in t['menus']) and not t['selectedLocked'])
