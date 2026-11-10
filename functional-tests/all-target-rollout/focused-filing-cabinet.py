"""Real Filing Cabinet cell targeting, wheel paging and scrollbar drag checks."""
from pathlib import Path
import argparse
import json
from evidence import send

p=argparse.ArgumentParser()
p.add_argument('run',type=Path)
p.add_argument('--existing-fixture',action='store_true',help='Continue an already seeded, untouched first page')
p.add_argument('--log',type=Path,help='Observe runtime slot-sync errors during this focused check')
a=p.parse_args()
checks=[]
log_offset=a.log.stat().st_size if a.log and a.log.exists() else 0

def act(kind,**kwargs):return send(a.run,kind,**kwargs)['client']
def window(state):return next(w for w in state.get('allWindows',state.get('windows',[])) if w.get('menuId')=='toms_storage:filing_cabinet')
def check(label,okay):
    checks.append(dict(check=label,passed=bool(okay)))
    (a.run/'filing-focused-checks.json').write_text(json.dumps(dict(scope='Filing Cabinet geometry, first/last cells, wheel, track and thumb only; no full gameplay suite',checks=checks),indent=2))
    assert okay,label
    print('PASS',label,flush=True)
def snapshot():
    if a.run.parent.parent.name=='all-target-rollout':
        # This observer includes an authoritative server snapshot with every
        # reply rather than exposing the later snapshot-server command.
        server=send(a.run,'wait',ticks=4)['server']
        return dict(menus=[dict(**{'class':server['menu']},carried=json.dumps(server['cursor']),
                                slots=[dict(index=i,item=json.dumps(v)) for i,v in enumerate(server['slots'])])],
                    inventory=[dict(index=v['index'],item=json.dumps(v)) for v in server['playerInventory']])
    act('snapshot-server',ticks=4)
    return json.loads((a.run/'server-state.json').read_text())
def native(server):return next(m for m in server['menus'] if m['class'].endswith('.FilingCabinetMenu'))
def item(server,index):return native(server)['slots'][index]['item']
def tap(x,y):
    act('click',x=x,y=y,button=0,ticks=2)
    return act('release',x=x,y=y,button=0,ticks=14)
def point(b,index):return b['x']+17+index%9*18,b['y']+33+index//9*18

if not a.existing_fixture:
 act('close',ticks=3)
 act('sneak',down=False,ticks=2)
 act('command',command='gamemode survival @a',ticks=2)
 act('held',item='minecraft:air',ticks=2)
 act('command',command='clear @a',ticks=2)
 act('fixture',block='toms_storage:filing_cabinet',ticks=6)
# This native block has a custom list serializer, rather than a vanilla
# command-addressable Container. Seed its own saved inventory before opening.
 act('catalog',ticks=1)
 catalog=json.loads((a.run/'catalog.json').read_text())
 sentinels={'minecraft:wooden_sword','minecraft:iron_sword','minecraft:diamond_sword'}
 choices=iter(sorted(v for v in catalog['items'] if v.startswith('minecraft:') and v!='minecraft:air' and v not in sentinels))
# Filing Cabinet compacts empty entries. Fill every cell with a distinct real
# item so indices 45 and 511 survive its original inventory deserializer.
 items=['{id:"'+next(choices)+'",count:1}' for _ in range(512)]
 for index,sentinel in [(0,'wooden_sword'),(45,'iron_sword'),(511,'diamond_sword')]:
    items[index]='{id:"minecraft:'+sentinel+'",count:1}'
 act('command',command='data merge block 8 65 8 {inventory:['+','.join(items)+']}',ticks=6)
 s=act('client-use-block',ticks=18)
else:
 s=act('wait',ticks=4)
b=window(s)['bounds']
check('grid-and-scrollbar-fit-inside-frame',b['width']>=196 and b['height']>=122)
t=snapshot()
check('native-first-page-shows-first-backing-cell','minecraft:wooden_sword' in item(t,0))
act('scroll',x=b['x']+40,y=b['y']+40,amount=-1,ticks=16)
t=snapshot()
check('wheel-scroll-moves-native-backing-offset-by-one-row','minecraft:iron_sword' in item(t,36))
sx=b['x']+8+9*18+4+7
top=b['y']+24
tap(sx,top+88)
t=snapshot()
check('track-click-reaches-last-partial-row-and-cell-511','minecraft:diamond_sword' in item(t,43))
act('capture',capture='filing-last-page-511',ticks=5)
tap(*point(b,43));t=snapshot()
check('last-rendered-cell-click-targets-native-cell-511',any('minecraft:diamond_sword' in m['carried'] for m in t['menus']) and 'minecraft:diamond_sword' not in item(t,43))
# Native cabinet cells are extraction-only. Recover through the connected
# player inventory, rather than trying to insert back into its grid.
s=act('inventory',ticks=8)
def inventory_cell(state,index):
    inv=next(w for w in state['workspaceWindows'] if w['kind']=='INVENTORY')
    v=next(v for v in inv['inventoryCells'] if v['inventorySlot']==index)
    return v['x'],v['y']
s=tap(*inventory_cell(s,9))
act('click',x=sx,y=top+82,button=0,ticks=2)
act('drag',x=sx,y=top+7,dx=0,dy=-75,button=0,ticks=14)
act('release',x=sx,y=top+7,button=0,ticks=12)
t=snapshot()
check('thumb-drag-returns-to-first-native-row','minecraft:wooden_sword' in item(t,0))
tap(*point(b,0));t=snapshot()
check('first-rendered-cell-click-targets-native-first-cell',any('minecraft:wooden_sword' in m['carried'] for m in t['menus']))
s=act('wait',ticks=2)
tap(*inventory_cell(s,10));t=snapshot()
recovered={v['index']:v['item'] for v in t['inventory']}
check('both-withdrawals-recover-to-player-with-clear-cursor','minecraft:diamond_sword' in recovered[9] and
      'minecraft:wooden_sword' in recovered[10] and all('minecraft:' not in m['carried'] or 'minecraft:air' in m['carried'] for m in t['menus']))
act('capture',capture='filing-first-page-after-drag',ticks=5)
if a.log:
    with a.log.open('rb') as stream:
        stream.seek(log_offset)
        messages=stream.read().decode('utf-8',errors='replace')
    check('no-slot-synchronization-errors-during-paging',
          'payload: salts_inventory_update:desktop_slot' not in messages and 'ArrayIndexOutOfBoundsException' not in messages)
