"""Real death -> native grave list -> storage pictures; no withdrawal claim."""
from pathlib import Path
import argparse,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True)
p.add_argument('--minecraft',required=True);p.add_argument('--loader',default='fabric');a=p.parse_args()
def act(kind,**kw):return send(a.run,kind,**kw)
def command(text,ticks=8):return act('server-command' if a.side=='siu' else 'command',command=text,ticks=ticks)
act('close',ticks=3);act('fixture',block='minecraft:stone',ticks=16)
if a.minecraft in ['1.20.1','1.21.1']:
 command('gamerule doImmediateRespawn true');command('gamerule keepInventory false')
else:
 command('gamerule minecraft:immediate_respawn true');command('gamerule minecraft:keep_inventory false')
command('gamemode survival @a');command('spawnpoint @a 8 65 12');command('clear @a')
command('item replace entity @a inventory.0 with minecraft:diamond 7')
command('item replace entity @a inventory.1 with minecraft:gold_ingot 16');command('kill @a',ticks=50)
s=command('execute as @a run graves',ticks=20)['client']
assert s.get('bounds') and not s.get('windows',s.get('allWindows',[])),'Grave selection must stay native'
act('capture',capture=f'{a.side}-graves-native-selection-{a.minecraft.replace(".","")}-{a.loader}',ticks=16)
b=s['bounds'];x=b['x']+16;y=b['y']+26
act('click',x=x,y=y,ticks=1);s=act('release',x=x,y=y,ticks=20)['client']
if a.side=='siu':
 w=next(w for w in s.get('windows',s.get('allWindows',[])) if w.get('definition','').endswith('.GraveWindow'))
 assert any(v['item']=='minecraft:diamond' and v['count']==7 for v in w['slots']),'Actual death loot missing'
else:
 assert any(v['item']=='minecraft:diamond' and v['count']==7 for v in s.get('slots',[])),'Actual death loot missing'
capture(a.run,a.side,a.minecraft,a.loader,'universal-grave-storage','A real seven-diamond death grave, entered through its original selection list. This run checks the native list, storage entry and pictured loot; withdrawal is not tested here.')
print('PASS real death, native list and storage entry',a.side,flush=True)
