"""Create one real death grave, then enter it through the native /graves list."""
from pathlib import Path
import argparse,json,sys
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send

p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);a=p.parse_args()
rows=[]
def act(kind,**kw):
    result=send(a.run,kind,**kw);rows.append(result)
    (a.run/'grave-entry-journal.json').write_text(json.dumps(rows,indent=2))
    return result['client']
def cmd(text,ticks=5):return act('command',command=text,ticks=ticks)
def capture(name):return act('capture',capture=a.side+'-'+name+'-1211-fabric',ticks=16)
act('close');act('fixture',block='minecraft:air',ticks=5)
cmd('gamerule doImmediateRespawn true');cmd('gamerule keepInventory false')
cmd('gamemode survival @a');cmd('spawnpoint @a 8 65 12')
cmd('item replace entity @a inventory.0 with minecraft:diamond 7')
cmd('item replace entity @a inventory.1 with minecraft:gold_ingot 16')
cmd('kill @a',ticks=50)
s=cmd('execute as @a run graves',ticks=16)
assert s.get('siu_loaded')==(a.side=='siu')
assert 'bounds' in s and not s.get('allWindows'), 'Grave selection list must keep its native screen'
capture('graves-native-selection-list')
b=s['bounds'];act('click',x=b['x']+16,y=b['y']+26,ticks=16)
if a.side=='siu':
    s=act('wait',ticks=8)
    assert any(w.get('menuId')=='salts_inventory_compat:universal_grave' for w in s.get('allWindows',[])),s.get('screen')
else:
    s=act('wait',ticks=8);assert len(s.get('slots',[]))>=45,s.get('screen')
capture('universal-grave-storage')
print('Captured real death-grave storage and native selection list:',a.side)
