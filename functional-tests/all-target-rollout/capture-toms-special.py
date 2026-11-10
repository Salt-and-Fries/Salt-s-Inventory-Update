"""Use Tom's real beacon-link and held-configurator workflows; no forced menus."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True)
p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);p.add_argument('--only',action='append');a=p.parse_args();rows=[]
cmd='server-command' if a.side=='siu' else 'command'
def act(kind,**kw):return send(a.run,kind,**kw)
def command(text,ticks=8):return act(cmd,command=text,ticks=ticks)
act('catalog',ticks=1);catalog=json.loads((a.run/'catalog.json').read_text());x,y,z=map(int,catalog.get('origin','8,65,8').split(','))
def identifier(name,kind):
 modern='toms_storage:'+name;legacy='toms_storage:ts.'+name
 return modern if modern in catalog[kind] else legacy if legacy in catalog[kind] else None
connector=identifier('inventory_cable_connector','blocks');configurator=identifier('inventory_configurator','items')
for ui in ['toms-inventory-link','toms-inventory-configurator']:
 if a.only and ui not in a.only:continue
 if (ui.endswith('link') and not connector) or (ui.endswith('configurator') and not configurator):continue
 try:
  act('close',ticks=3);act('sneak',down=False,ticks=4)
  command('kill @e[type=!minecraft:player]');command('gamemode survival @a');command('time set day')
  if ui.endswith('link'):
   act('fixture',block=connector,ticks=12)
   command(f'fill {x-1} {y-1} {z-2} {x+1} {y-1} {z} minecraft:iron_block')
   command(f'setblock {x} {y} {z-1} minecraft:beacon')
   command(f'setblock {x} {y} {z} {connector}[facing=north]',ticks=40)
   command('item replace entity @a weapon.mainhand with minecraft:air')
  else:
   act('fixture',block='minecraft:chest',ticks=12)
   command('item replace entity @a weapon.mainhand with '+configurator)
   act('sneak',down=True,ticks=16)
  act('client-use-block' if a.side=='siu' else 'open',shift=ui.endswith('configurator'),ticks=20)
  act('sneak',down=False,ticks=4)
  capture(a.run,a.side,a.minecraft,a.loader,ui,'Ordinary client interaction after the publisher-required beacon or held-configurator setup. Pictured state only.')
  rows.append({'ui':ui,'status':'real workflow and capture'})
 except Exception as e:
  rows.append({'ui':ui,'status':'pending','reason':str(e)[:300]});print('PENDING',ui,str(e)[:150],flush=True)
 (a.run/'toms-special-capture-checks.json').write_text(json.dumps(rows,indent=2))
