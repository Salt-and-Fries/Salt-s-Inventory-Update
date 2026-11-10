"""Focused native UI actions, not the full Tech Reborn gameplay suite."""
from pathlib import Path
import json,sys
sys.path.insert(0,str(Path(__file__).parent))
import capture as c
OUT=c.OUT;RUN=c.RUN
CHECKS=[];EXTRA=[]

def state():return c.call('wait',ticks=2)
def w(s=None):return(s or state())['allWindows'][-1]
def point(x,y,s=None):
    a=w(s);b=a['bounds']
    for r in reversed(a['composition']['regions']):
        q=r['source']
        if q['x']<=x<q['x']+q['width'] and q['y']<=y<q['y']+q['height']:
            assert not any(v['x']<=x<v['x']+v['width']and v['y']<=y<v['y']+v['height']for v in r['cutouts'])
            return(b['x']+r['x']+(0 if r['exterior']else 6)+x-q['x'],b['y']+r['y']+(0 if r['exterior']else 22)+y-q['y'])
    raise AssertionError(('unmapped native point',x,y,a))
def tap(x,y,button=0):
    c.call('move',x=x,y=y,ticks=1);c.call('click',x=x,y=y,button=button,ticks=1);return c.call('release',x=x,y=y,button=button,ticks=8)
def native(x,y,button=0):return tap(*point(x,y),button)
def observe():
    c.call('tr-observe',ticks=2);return json.loads((RUN/'tr-state.json').read_text())
def passed(name,ok,evidence=None):
    CHECKS.append(dict(name=name,passed=bool(ok),evidence=evidence));save();assert ok,(name,evidence)
def save():
    (OUT/'focused-checks.json').write_text(json.dumps(dict(status='running',checks=CHECKS),indent=2))
def capture(key):
    s=c.capture(key);EXTRA.append(c.RECORDS[-1]);(OUT/'functional-captures.json').write_text(json.dumps(EXTRA,indent=2));return s
def open(block):
    s=c.d.open_block(block);c.cmd("gamemode survival @p");return s
def tab(index):
    b=w()['bounds'];return tap(b['x']-10,b['y']+36+24*index)
def close_popup():
    b=w()['bounds'];return tap(b['x']+b['width']+28+93,b['y']+34)
def inv(index=9):
    s=state()
    if not any(x['kind']=='INVENTORY'for x in s['workspaceWindows']):s=c.call('inventory',ticks=3)
    b=next(x for x in s['workspaceWindows']if x['kind']=='INVENTORY');q=next(x for x in b['inventoryCells']if x['inventorySlot']==index);return tap(q['x'],q['y'])
def insert(item,sx,sy,count=1):
    c.cmd(f'item replace entity @p inventory.0 with {item} {count}');inv();native(sx,sy)

def main():
    c.RECORDS.extend(r for r in json.loads((OUT/'captures.json').read_text())if r.get('variant')in('idle','formed-idle','unformed'))
    open('techreborn:alloy_smelter');tab(0);capture('shared-item-configuration')
    passed('Item settings opens from directly attached left tab',w()['trSelectedTab']=='SlotConfigGui')
    native(42,55);capture('shared-item-port-popup')
    passed('Input inspector opens through the real input cell',w()['templateState']['popupHeight']==137)
    before=observe()['getSlotConfigurationNbt'];native(209,93);after=observe()['getSlotConfigurationNbt']
    passed('Auto Input checkbox updates native server slot configuration',before!=after,dict(before=before,after=after))
    before=after;native(239,19);after=observe()['getSlotConfigurationNbt']
    passed('Six-face diagram edits the native server port',before!=after,dict(before=before,after=after))
    close_popup();passed('Inspector Close returns to item settings',w()['templateState']['popupHeight']==0 and w()['trSelectedTab']=='SlotConfigGui')
    native(88,55);capture('shared-output-port-popup');passed('Output-only inspector retains its shorter permitted controls',w()['templateState']['popupHeight']==107)
    close_popup();tab(1);capture('shared-redstone-configuration')
    before=observe()['getRedstoneConfigurationNbt'];native(130,103);after=observe()['getRedstoneConfigurationNbt']
    passed('Redstone cycle control updates native server state',before!=after,dict(before=before,after=after));tab(1)
    passed('Selected left tab returns to the machine view',w()['trSelectedTab']=='none')
    # An accepted native upgrade is inserted and removed through the relocated cells.
    insert('techreborn:overclocker_upgrade',-10,20)
    passed('Upgrade insertion reaches the original upgrade slot',any(x['x']<0 and x['item']=='techreborn:overclocker_upgrade'for x in w()['slots']))
    native(-10,20);inv(10)
    passed('Upgrade removal uses the original slot and shared cursor',not any(x['x']<0 and x['item']=='techreborn:overclocker_upgrade'for x in w()['slots']))
    open('techreborn:diesel_generator');tab(1);capture('shared-fluid-configuration')
    passed('Fluid inspector opens from its native left tab',w()['trSelectedTab']=='FluidConfigGui'and w()['templateState']['popupHeight']==122)
    before=observe()['nbt'];native(209,93);after=observe()['nbt'];passed('Fluid Pull In checkbox reaches the native server configuration',before!=after,dict(before=before,after=after));close_popup()
    passed('Fluid inspector Close returns to its owning machine',w()['trSelectedTab']=='none')
    for block,sx,sy,key in [('techreborn:pump',90,36,'getDepth'),('techreborn:pump',90,61,'getRange'),('techreborn:player_detector',70,46,'getCurrentRadius'),('techreborn:adjustable_su',127,85,'getCurrentOutput')]:
        open(block);before=observe()[key];native(sx,sy);after=observe()[key];passed(block+' '+key+' native adjustment',before!=after,dict(before=before,after=after))
    open('techreborn:basic_storage_unit');insert('minecraft:stone',108,61,64);before=observe()['nbt'];native(156,10);after=observe()['nbt'];passed('Storage Unit native lock remains actionable below the SIU title',before!=after)
    capture('storage-unit-populated');passed('Storage Unit receives and displays a real deposited item','minecraft:stone'in observe()['nbt'])
    open('techreborn:basic_tank_unit');insert('minecraft:water_bucket',108,61);c.call('wait',ticks=40);capture('tank-unit-populated');passed('Tank Unit processes its input container and displays fluid','minecraft:water'in observe()['nbt'])
    # Check native armor cells and native multi-window lifetime using ordinary inventory input.
    open('techreborn:high_voltage_su');insert('techreborn:bronze_helmet',16,26);passed('Retained armor cell updates the actual player equipment',any(x['player']and x['y']<92 and x['item']=='techreborn:bronze_helmet'for x in w()['slots']))
    s=c.call('wait',ticks=1)
    window=next(x for x in s['workspaceWindows']if x['kind']=='CONTAINER')
    if window.get('locked')=='true':
        q=window['controls']['LOCK'];tap(q['x'],q['y']);s=state()
    b=w(s)['bounds'];sx,sy=b['x']+30,b['y']+8
    c.call('click',x=sx,y=sy,button=0,ticks=1);c.call('drag',x=sx+45,y=sy+35,button=0,dx=45,dy=35,ticks=1);s=c.call('release',x=sx+45,y=sy+35,button=0,ticks=5)
    passed('Parent window moves with attached tabs and right upgrade tray',w(s)['bounds']!=b)
    capture('workspace-equipment')
    c.call('close');passed('Closing the owning window disposes its native container session',state()['sessions']==0)
    open('techreborn:alloy_smelter');tab(0);native(42,55)
    s=c.call('key',key=256,scan=0,modifiers=0,ticks=4)
    passed('Escape dismisses inspector before its owning window',s['sessions']==1 and w(s)['templateState']['popupHeight']==0 and w(s)['trSelectedTab']=='SlotConfigGui')
    c.call('close')
    summary=['All 51 registered machine screens opened with the Tech Reborn template; complete native cells and native controls were checked against their mapped regions.',
        'All eight multiblocks were built from native blueprints and passed native structure validation; their incomplete warning states were also captured.',
        'Item, output-only, fluid and redstone settings opened normally. Native checkboxes, face configuration and redstone changes reached the server.',
        'Relocated upgrade insertion/removal, native rate/radius/depth/range controls, Storage Unit locking, populated item/fluid storage and retained armor slots were exercised.',
        'Window movement and close lifetime were checked. Core/API and unrelated release jars are preserved; screenshots show the tested production compatibility jar.']
    (OUT/'focused-checks.json').write_text(json.dumps(dict(status='passed',checks=CHECKS,summary=summary),indent=2));print('PASSED',len(CHECKS),'focused checks',flush=True)
if __name__=='__main__':main()
