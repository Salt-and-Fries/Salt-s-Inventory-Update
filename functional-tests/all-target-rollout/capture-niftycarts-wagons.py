"""Attach real chests, then open each of NiftyCarts' three wagon inventories."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path)
p.add_argument('--side',choices=['native','siu'],required=True)
p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);a=p.parse_args()
def act(kind,**kw):return send(a.run,kind,**kw)
def command(text,ticks=8):return act('server-command' if a.side=='siu' else 'command',command=text,ticks=ticks)
act('catalog',ticks=1);catalog=json.loads((a.run/'catalog.json').read_text())
x,y,z=map(int,catalog.get('origin','8,65,8').split(','))
act('close');act('fixture',block='minecraft:stone');command('kill @e[type=!minecraft:player]')
command(f'summon niftycarts:wagon {x+.5} {y} {z+.5} '+'{NoGravity:1b,Invulnerable:1b}')
command(f'tp @a {x+.5} {y} {z+3.5} 180 5');rows=[]
for chests in range(1,4):
 act('close',ticks=3);act('sneak',down=False,ticks=8)
 command('item replace entity @a weapon.mainhand with minecraft:chest')
 act('aim-entity',entity='niftycarts:wagon',ticks=8)
 act('use-entity-ray',entity='niftycarts:wagon',shift=False,ticks=12)
 command('item replace entity @a weapon.mainhand with minecraft:air')
 act('sneak',down=True,ticks=16)
 state=act('client-use-entity' if a.side=='siu' else 'use-entity',entity='niftycarts:wagon',shift=True,ticks=20)['client']
 act('sneak',down=False,ticks=3)
 if a.side=='siu' and not state.get('windows',state.get('allWindows',[])):
  state=act('client-use-entity',entity='niftycarts:wagon',shift=True,ticks=20)['client']
 slots=state.get('slots',[])
 assert len([s for s in slots if not s.get('player',False)])==chests*36,(chests,len(slots),state.get('screen'))
 capture(a.run,a.side,a.minecraft,a.loader,f'niftycarts-wagon-{chests}',
   f'Wagon with {chests} chest(s), attached by ordinary held-chest entity interaction. Real {chests*36}-slot native inventory entry and pictured state only.')
 rows.append({'chests_attached':chests,'real_container_cells':chests*36,'natural_entry_and_picture':True})
 (a.run/'niftycarts-wagon-entry-checks.json').write_text(json.dumps(rows,indent=2))
