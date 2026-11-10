"""Focused checks for parent-owned Aircraft cargo controls; not a gameplay suite."""
from probe import *

def workspace(zone,state=None):
    state=state or act();bounds=window(zone,state)['bounds']
    return next(w for w in state['workspaceWindows'] if w['bounds']==bounds)

def control(zone,name):return click(**workspace(zone)['controls'][name])

def drag(zone,dx,dy,button=0):
    b=window(zone)['bounds'];x=b['x']+18;y=b['y']+8
    act('click',x=x,y=y,button=button,ticks=1)
    act('drag',x=x+dx,y=y+dy,dx=dx,dy=dy,button=button,ticks=1)
    return act('release',x=x+dx,y=y+dy,button=button)

def attached(state=None):
    state=state or act();m=window('MAIN',state)['bounds'];p=window('PORT',state)['bounds'];s=window('STARBOARD',state)['bounds']
    return p['x']+p['width']-1==m['x'] and s['x']==m['x']+m['width']-1 and p['y']==m['y']==s['y'] and p['width']==s['width']

def cargo(zone,state=None):
    w=window(zone,state);ids={c['slot'] for c in w['aircraftCells'] if c['kind']=='inventory'}
    return [s for s in w['slots'] if s['index'] in ids and s['count']]

def count(zone,name,state=None):return sum(s['count'] for s in cargo(zone,state) if s['item']==name)

def insert(zone,source,name,amount):
    item(name,amount);inventory();slot(zone,source=source)

def chest(state=None):return next(w for w in (state or act())['allWindows'] if w.get('menuId')=='minecraft:generic_9x3')

command('gamemode survival @a');open_aircraft();s=act()
check('cargo group opens attached with all 87 native positions',attached(s) and sum(len(w['aircraftCells']) for w in s['allWindows'])==87)
for zone in ('PORT','STARBOARD'):
    w=window(zone);controls=workspace(zone)['controls'];a=w['aircraftActions']
    check(zone+' exposes exactly sort out, sort inside, focus',set(controls)=={'SORT_WINDOWS','SORT_INVENTORY','FOCUS'},controls)
    check(zone+' rejects movement, resize, close, pin, lock and management',all(not a[k] for k in ('MOVE','RESIZE','USER_CLOSE','PIN','UNPIN','LOCK','UNLOCK','MANAGEMENT_MENU')),a)
    before=w['bounds'];drag(zone,26,18);check(zone+' title drag does not move it',window(zone)['bounds']==before)
    act('aircraft-handle-move',title=zone.title() if zone=='PORT' else 'Starboard',x=before['x']+21,y=before['y']+11)
    result=json.loads((RUN/'aircraft-handle.json').read_text())
    check(zone+' public handle rejects independent movement',not result['moved'] and window(zone)['bounds']==before,result)
    click(before['x']+18,before['y']+8,1)
    check(zone+' right click cannot open movement menu',not window(zone)['aircraftPopup'])
    control(zone,'FOCUS');check(zone+' focus button selects cargo target',workspace(zone)['focused']=='true')
    control(zone,'FOCUS');check(zone+' focus button can clear cargo target',workspace(zone)['focused']=='false')

before=window()['bounds'];drag('MAIN',22,22)
check('parent drag moves both wings together',attached() and window()['bounds']!=before)
show_inventory()
insert('PORT',23,'minecraft:stone',8);insert('PORT',24,'minecraft:stone',4);insert('PORT',25,'minecraft:dirt',3)
insert('MAIN',7,'minecraft:stone',1)
s=act();check('cargo fixture inserted through UI',count('PORT','minecraft:stone',s)==12 and count('PORT','minecraft:dirt',s)==3 and count('MAIN','minecraft:stone',s)==1)
control('PORT','SORT_INVENTORY');s=act(ticks=15)
check('side sort inside merges stacks and preserves contents',len(cargo('PORT',s))==2 and count('PORT','minecraft:stone',s)==12 and count('PORT','minecraft:dirt',s)==3)
# The three views share one real vehicle inventory. Salt correctly excludes
# aliases of the same physical owner from sort-out targets; use a real chest.
command('setblock 8 65 8 minecraft:chest')
command('data merge block 8 65 8 {Items:[{Slot:0b,id:"minecraft:stone",Count:1b}]}')
act('open',ticks=15)
b=chest()['bounds'];x=b['x']+18;y=b['y']+8;dx=8-b['x'];dy=320-b['y']
act('click',x=x,y=y,button=0,ticks=1);act('drag',x=x+dx,y=y+dy,dx=dx,dy=dy,button=0,ticks=1);act('release',x=x+dx,y=y+dy,button=0)
control('PORT','SORT_WINDOWS');s=act(ticks=15)
stored=sum(v['count'] for v in chest(s)['slots'] if not v['player'] and v['item']=='minecraft:stone')
check('side sort out moves matching cargo to external storage',stored==13 and count('MAIN','minecraft:stone',s)==1 and count('PORT','minecraft:stone',s)==0 and count('PORT','minecraft:dirt',s)==3)
item('minecraft:dirt',4);control('PORT','FOCUS');s=act();w=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');p=next(c for c in w['inventoryCells'] if c['inventorySlot']==9)
act('aircraft-quick',title='Inventory',x=p['x'],y=p['y'],ticks=15)
check('focused side receives ordinary quick transfer',count('PORT','minecraft:dirt')==7)
server('side-controls-server-state');capture('aircraft-parent-owned-side-controls')

control('MAIN','CLOSE');check('parent close disposes all three aircraft panes and leaves external storage',not any(w.get('aircraftZone') for w in act()['allWindows']) and chest() is not None)
act('close');act('aim-entity',entity='immersive_aircraft:cargo_airship');act('use-entity-ray',entity='immersive_aircraft:cargo_airship',shift=True,ticks=18);act('sneak',down=False)
check('reopening restores attached group and saved real cargo',attached() and count('MAIN','minecraft:stone')==1 and count('PORT','minecraft:dirt')==7)
control('MAIN','CLOSE')
open_aircraft('warship');act('gui-scale',scale=3,ticks=18)
check('tall Warship group stays aligned at GUI scale 3',attached())
for zone in ('PORT','STARBOARD'):
    check(zone+' retains exactly three controls at GUI scale 3',set(workspace(zone)['controls'])=={'SORT_WINDOWS','SORT_INVENTORY','FOCUS'})
before=window()['bounds'];drag('MAIN',-12,12);check('Warship wings follow clamped parent movement',attached())
capture('warship-parent-owned-side-controls-scale3')
control('MAIN','CLOSE');check('Warship parent close disposes both wings',not act()['allWindows'])
act('gui-scale',scale=2,ticks=18);open_aircraft();show_inventory();capture('aircraft-side-controls-final-fullscreen')
print('Focused Aircraft side-control checks finished.',flush=True)
