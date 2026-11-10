"""Exercise the relocated Configurator slot and the publisher's native controls."""
from pathlib import Path
import argparse,json
from evidence import send,capture

p=argparse.ArgumentParser();p.add_argument('run',type=Path);a=p.parse_args();checks=[]
def act(kind,**kw):return send(a.run,kind,**kw)['client']
def check(name,okay):
    checks.append(dict(check=name,passed=bool(okay)))
    (a.run/'configurator-focused-checks.json').write_text(json.dumps(dict(scope='Native Configurator slot, six relocated controls, clear configuration and filter-edit entry only; not a full Tom Storage suite',checks=checks),indent=2))
    assert okay,name
    print('PASS',name,flush=True)
def window(s):return next(w for w in s.get('allWindows',s.get('windows',[])) if w.get('definition','').endswith('$InventoryConfigurator'))
def tap(x,y,button=0):return act('xy-click',x=x,y=y,button=button,ticks=14)
def snapshot():
    act('snapshot-server',ticks=3);return json.loads((a.run/'server-state.json').read_text())
def menu(t):return next(m for m in t['menus'] if m['class'].endswith('.InventoryConfiguratorMenu'))
def settings():return menu(snapshot())['settings']
def control(s,index):
    b=window(s)['bounds'];return (b['x']+8+(143 if index==6 else 1)+8,b['y']+24+1+(0 if index==6 else index*18)+8)
def filter_slot(s):
    b=window(s)['bounds'];return b['x']+8+84+8,b['y']+24+46+8
def inv(s,index):
    w=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');v=next(v for v in w['inventoryCells'] if v['inventorySlot']==index);return v['x'],v['y']
def reopen():
    act('close',ticks=3);act('held',item='toms_storage:inventory_configurator',ticks=4);act('sneak',down=True,ticks=8)
    s=act('client-use-block',ticks=16);act('sneak',down=False,ticks=4);return act('wait',ticks=3)

s=act('wait',ticks=3);b=window(s)['bounds']
check('native-configurator-fits-complete-control-column',b['width']>=176 and b['height']>=140)
before=settings();s=tap(b['x']+8+40,b['y']+24+8)
check('blank-old-horizontal-button-area-does-not-change-settings',settings()==before)
s=tap(*control(s,0));after=settings();check('priority-button-reaches-native-priority',after['priority']!=before['priority'])
s=tap(*control(s,3));before=after;after=settings();check('side-button-reaches-native-side',after['side']!=before['side'])
for index,key in [(4,'skip'),(5,'keepLast')]:
    before=settings()[key];s=tap(*control(s,index));check(key+'-turns-on-off-at-visible-button',settings()[key]!=before)
    s=tap(*control(s,index));check(key+'-restores-original-value',settings()[key]==before)
s=tap(*control(s,2));check('remove-connected-keeps-original-main-block',settings()['connected']==1)

act('command',command='item replace entity @a inventory.0 with toms_storage:item_filter',ticks=4)
s=act('inventory',ticks=8);s=tap(*inv(s,9));s=tap(*filter_slot(s));t=snapshot()
check('relocated-filter-slot-accepts-real-filter', 'toms_storage:item_filter' in menu(t)['slots'][0]['item'])
capture(a.run,'siu','1.21.1','neoforge','toms-inventory-configurator-populated','Original filter inserted through the separated slot; priority, side, exclude and keep-last native controls checked through their visible buttons.')
s=tap(*filter_slot(s),button=1)
check('right-click-filter-opens-native-filter-editor',any(w.get('menuId')=='toms_storage:item_filter' for w in s.get('allWindows',s.get('windows',[]))))
s=reopen();s=act('inventory',ticks=6);s=tap(*filter_slot(s));s=tap(*inv(s,9));t=snapshot()
check('filter-recovers-to-player-with-clear-cursor',any(v['index']==9 and 'toms_storage:item_filter' in v['item'] for v in t['inventory']) and all(m['carried']=='{}' for m in t['menus']))
s=tap(*control(s,1));check('add-connected-enters-original-world-selection',not any(w.get('definition','').endswith('$InventoryConfigurator') for w in s.get('allWindows',s.get('windows',[]))))
s=reopen();s=tap(*control(s,6));check('clear-configuration-remains-separate-from-window-close',not any(w.get('definition','').endswith('$InventoryConfigurator') for w in s.get('allWindows',s.get('windows',[]))))
s=reopen();after=settings();check('cleared-configuration-reopens-with-native-defaults',after['priority']=='NORMAL' and after['side']=='down' and after['skip']=='false' and after['keepLast']=='false')
act('close',ticks=5)
