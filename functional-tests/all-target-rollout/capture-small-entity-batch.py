"""Natural Guard Villager and NiftyCarts inventory entry; pictured states only."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True)
p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);p.add_argument('--only',action='append');a=p.parse_args()
command_kind='server-command' if a.side=='siu' else 'command';rows=[]
def act(kind,**kwargs):return send(a.run,kind,**kwargs)
def command(text,ticks=8):return act(command_kind,command=text,ticks=ticks)
act('catalog',ticks=1);catalog=json.loads((a.run/'catalog.json').read_text());items=set(catalog['items'])
x,y,z=map(int,catalog.get('origin','8,65,8').split(','))
cases=[('guard-villager','guardvillagers:guard','guardvillagers:guard_spawn_egg',False),
       *[(f'niftycarts-{kind.replace("_","-")}',f'niftycarts:{kind}',f'niftycarts:{kind}',True) for kind in ['plow','seed_drill','supply_cart','hand_cart','reaper','wagon','animal_cart']]]
for ui,entity,item,shift in cases:
 if a.only and ui not in a.only:continue
 if item not in items and ('niftycarts:oak_'+item.split(':',1)[1]) not in items:continue
 try:
  act('close',ticks=3);act('fixture',block='minecraft:stone',ticks=12)
  command('kill @e[type=!minecraft:player]');command('gamemode survival @a');command('difficulty normal');command('time set day')
  if ui=='guard-villager':command('effect give @a minecraft:hero_of_the_village 600 0 true')
  # Guard 2.0.9 explicitly requires isEffectiveAi() for opening its menu.
  # Keep its AI enabled; nonliving carts need no behavioral fixture changes.
  no_ai='0b' if ui=='guard-villager' else '1b'
  command(f'summon {entity} {x+.5} {y} {z+.5} '+'{NoAI:'+no_ai+',NoGravity:1b,Invulnerable:1b,PersistenceRequired:1b}',ticks=20)
  command(f'tp @a {x+.5} {y} {z+3.5} 180 5');command('item replace entity @a weapon.mainhand with minecraft:air')
  act('sneak',down=shift,ticks=16)
  state=act('client-use-entity' if a.side=='siu' else 'use-entity',entity=entity,shift=shift,ticks=20)['client']
  act('sneak',down=False,ticks=4)
  if ui=='guard-villager' and ((a.side=='native' and state.get('screen')=='none') or (a.side=='siu' and not state.get('windows',state.get('allWindows',[])))):
   # Try the other ordinary interaction after an unconsumed first click.
   act('sneak',down=True,ticks=16)
   state=act('client-use-entity' if a.side=='siu' else 'use-entity',entity=entity,shift=True,ticks=20)['client']
   act('sneak',down=False,ticks=4)
  if a.side=='siu' and not state.get('windows',state.get('allWindows',[])):
   state=act('client-use-entity',entity=entity,shift=shift,ticks=20)['client']
  if a.side=='siu':
   assert state.get('windows',state.get('allWindows',[])),'No inventory window opened'
   if ui in ['guard-villager','niftycarts-plow']:assert all('AutomaticNativeWindow' not in w.get('definition','') for w in state.get('windows',state.get('allWindows',[]))),f'{ui} used automatic fallback'
  capture(a.run,a.side,a.minecraft,a.loader,ui,'Ordinary entity interaction with the native inventory permissions and required crouch/hero setup. Pictured state only.')
  rows.append({'ui':ui,'status':'natural entry and real screenshot'})
 except Exception as error:
  rows.append({'ui':ui,'status':'pending','reason':str(error)[:300]});print('PENDING',ui,str(error)[:150],flush=True)
 (a.run/'small-entity-entry-capture-checks.json').write_text(json.dumps(rows,indent=2))
