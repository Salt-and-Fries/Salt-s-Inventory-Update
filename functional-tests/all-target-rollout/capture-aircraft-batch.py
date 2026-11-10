"""Open each published aircraft inventory through normal sneak interaction."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import capture,send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True)
p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);a=p.parse_args()
cmd='server-command' if a.side=='siu' else 'command';rows=[]
send(a.run,'catalog',ticks=1);catalog=json.loads((a.run/'catalog.json').read_text())
x,y,z=map(int,catalog.get('origin','8,65,8').split(','))
def command(text,ticks=8):return send(a.run,cmd,command=text,ticks=ticks)
for name in ['airship','biplane','cargo_airship','gyrodyne','quadrocopter','bamboo_hopper','warship']:
 if 'immersive_aircraft:'+name not in catalog['items']:continue
 try:
  send(a.run,'close',ticks=3);send(a.run,'fixture',block='minecraft:stone',ticks=12)
  command('kill @e[type=!minecraft:player]');command('gamemode survival @a')
  command(f'summon immersive_aircraft:{name} {x+.5} {y} {z+.5} '+'{NoGravity:1b,Invulnerable:1b}')
  command(f'tp @a {x+.5} {y} {z+3.5} 180 5')
  # Hold the input across ticks; a one-frame entity flag is overwritten by
  # normal player input before the interaction packet is dispatched.
  send(a.run,'sneak',down=True,ticks=16)
  s=send(a.run,'client-use-entity' if a.side=='siu' else 'use-entity',entity='immersive_aircraft:'+name,shift=True,ticks=20)['client']
  send(a.run,'sneak',down=False,ticks=4)
  if a.side=='siu':assert any('Aircraft' in w.get('definition','') for w in s.get('windows',[])),'Aircraft window not opened'
  else:assert 'immersive_aircraft' in s.get('screen',''),'Native aircraft screen not opened'
  capture(a.run,a.side,a.minecraft,a.loader,'aircraft-'+name.replace('_','-'))
  rows.append({'aircraft':name,'status':'natural inventory opening and pictured equipment/cargo layout'})
 except Exception as e:
  rows.append({'aircraft':name,'status':'pending','reason':str(e)[:250]});print('PENDING',name,str(e)[:120],flush=True)
 (a.run/'aircraft-entry-capture-checks.json').write_text(json.dumps(rows,indent=2))
