"""Enter the native flower, cocoon and a genuinely populated bee briefcase."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import capture,send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True)
p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);a=p.parse_args()
cmd='server-command' if a.side=='siu' else 'command';rows=[]
def act(kind,**kw):return send(a.run,kind,**kw)
def command(text,ticks=6):return act(cmd,command=text,ticks=ticks)
act('catalog',ticks=1);catalog=json.loads((a.run/'catalog.json').read_text())
x,y,z=map(int,catalog.get('origin','8,65,8').split(','))
for ui,block in [('bumblezone-flower','the_bumblezone:crystalline_flower'),('bumblezone-cocoon','the_bumblezone:honey_cocoon')]:
 try:
  act('close',ticks=3);act('fixture',block=block,ticks=16)
  command('kill @e[type=!minecraft:player]')
  command(f'tp @a {x+.5} {y} {z+3.5} 180 0')
  if ui.endswith('flower'):command(f'setblock {x} {y-1} {z} minecraft:amethyst_block')
  command('time set day')
  act('client-use-block' if a.side=='siu' else 'open',ticks=20)
  capture(a.run,a.side,a.minecraft,a.loader,ui,'Normal client interaction with the original mod block; pictured layout only.')
  rows.append({'ui':ui,'status':'real block entry and capture'})
 except Exception as e:rows.append({'ui':ui,'status':'pending','reason':str(e)[:200]});print('PENDING',ui,str(e)[:100],flush=True)
try:
 act('close',ticks=3);act('fixture',block='minecraft:stone',ticks=12)
 command('kill @e[type=!minecraft:player]');command('gamemode survival @a');command('time set day')
 if a.side=='siu':command('item replace entity @a weapon.mainhand with the_bumblezone:buzzing_briefcase')
 else:act('held',item='the_bumblezone:buzzing_briefcase',count=1,ticks=6)
 command(f'tp @a {x+.5} {y} {z+2.5} 180 0')
 command(f'summon minecraft:bee {x+.5} {y+1} {z+.5} '+'{NoAI:1b,NoGravity:1b,Health:8.0f}')
 # Use the actual briefcase/bee interaction, preserving the publisher's own
 # data-component encoding rather than synthesizing stored-bee NBT.
 act('client-use-entity' if a.side=='siu' else 'use-entity',entity='minecraft:bee',shift=False,ticks=20)
 act('sneak',down=True,ticks=12)
 act('client-use-item' if a.side=='siu' else 'use-item',shift=True,ticks=20)
 act('sneak',down=False,ticks=4)
 capture(a.run,a.side,a.minecraft,a.loader,'bumblezone-briefcase','Original briefcase item used on an actual bee, then opened with sneak-use. Bee contents and actions need separate focused confirmation.')
 rows.append({'ui':'bumblezone-briefcase','status':'real item/entity workflow and capture'})
except Exception as e:rows.append({'ui':'bumblezone-briefcase','status':'pending','reason':str(e)[:200]});print('PENDING briefcase',str(e)[:100],flush=True)
(a.run/'bumblezone-entry-capture-checks.json').write_text(json.dumps(rows,indent=2))
