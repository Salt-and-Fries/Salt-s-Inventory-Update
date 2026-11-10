"""Named older publisher block entries and the native decree analyzer command."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);p.add_argument('--only',action='append');a=p.parse_args();rows=[]
def act(kind,**kw):return send(a.run,kind,**kw)['client']
def opened(s):return bool(s.get('windows')) if a.side=='siu' else bool(s.get('menuClass'))
act('catalog',ticks=1);catalog=json.loads((a.run/'catalog.json').read_text());blocks=set(catalog['blocks'])
act('command',command='fill 5 64 5 12 64 14 minecraft:stone',ticks=3)
cases=[('additionz-chunk-loader','additionz:chunk_loader'),('additionz-fletching','minecraft:fletching_table'),
 ('charm-kiln','charm:kiln'),('shared-ag-bakery-small-pot','bakery:small_cooking_pot'),('bakery-baker-station','bakery:baker_station'),
 ('extended-drawer-single','extended_drawers:single_drawer'),('extended-drawer-double','extended_drawers:double_drawer'),
 ('extended-drawer-quad','extended_drawers:quad_drawer'),('extended-drawer-compacting','extended_drawers:compacting_drawer')]
for ui,block in cases:
 if a.only and ui not in a.only:continue
 if block not in blocks:continue
 if ui=='additionz-fletching' and 'additionz:chunk_loader' not in blocks:continue
 row={'ui':ui};rows.append(row)
 try:
  act('close',ticks=2);act('command',command='gamemode creative @a',ticks=2);act('sneak',down=False,ticks=2)
  act('held',item='minecraft:air',ticks=3);act('fixture',block=block,ticks=8);s=act('client-use-block' if a.side=='siu' else 'open',ticks=18)
  if not opened(s):
   act('command',command='tp @a 8.5 65 11.5 180 25',ticks=6)
   s=act('use-block-ray',ticks=18)
  if not opened(s) and ui.startswith('extended-drawer'):
   act('sneak',down=True,ticks=4);s=act('client-use-block' if a.side=='siu' else 'open',shift=True,ticks=18);act('sneak',down=False,ticks=2)
  capture(a.run,a.side,a.minecraft,a.loader,ui,'Actual installed publisher block and ordinary right-click entry. Drawer modifier interaction is recorded in the observer history. Pictured state only; not a full gameplay suite.')
  row['status']='ordinary-entry-and-picture-pass'
 except Exception as error:
  row.update(status='pending',reason=str(error)[:250]);print('PENDING',ui,str(error)[:130],flush=True)
 (a.run/'older-publisher-entry-captures.json').write_text(json.dumps(rows,indent=2))
if 'bountiful:analyzer' in catalog['menus'] and (not a.only or 'bountiful-decree-analyzer' in a.only):
 act('close',ticks=2);act('command',command='gamemode survival @a',ticks=2)
 act('command',command='execute as @a run bo util analyzer',ticks=20)
 capture(a.run,a.side,a.minecraft,a.loader,'bountiful-decree-analyzer','Original /bo util analyzer developer command opens the publisher decree-analysis inventory. This is a separate admin tool from the normal bounty board; pictured default analysis state only.')
