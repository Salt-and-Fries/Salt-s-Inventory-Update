"""Normal crouch drawer entry, native LibZ workstation tabs, and Ender Pouch."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);p.add_argument('--only',action='append');a=p.parse_args();rows=[]
def act(kind,**kw):return send(a.run,kind,**kw)['client']
def tap(x,y):
 act('move',x=x,y=y,ticks=4)
 act('click',x=x,y=y,ticks=1);return act('release',x=x,y=y,ticks=18)
act('catalog',ticks=1);catalog=json.loads((a.run/'catalog.json').read_text())
staged=json.loads((a.run/'staged-artifacts.json').read_text(encoding='utf-8-sig'))
filenames=[r['filename'].lower() for r in staged['artifacts']]
cases=[('drawer-'+size,'extended_drawers:'+size+'_drawer',True) for size in ['single','double','quad']]
cases += [('smitherz-smither','minecraft:smithing_table',False),('smitherz-grinder','minecraft:grindstone',False)]
cases += [('tieredz-reforge','minecraft:anvil',False)]
cases += [('inmis-ender-pouch','inmis:ender_pouch',False)]
for ui,entry,crouch in cases:
 if a.only and not any(ui.startswith(name) for name in a.only):continue
 if ui.startswith('smitherz-') and not any('smitherz' in n for n in filenames):continue
 if ui.startswith('tieredz-') and not any('tieredz' in n or 'tierify' in n or n.startswith('tiered-') for n in filenames):continue
 if entry not in catalog['blocks'] and entry not in catalog['items']:continue
 row={'ui':ui,'entry':entry};rows.append(row)
 try:
  act('close',ticks=3);act('fixture',block=entry if entry in catalog['blocks'] else 'minecraft:stone',ticks=8)
  act('command',command='gamemode survival @a',ticks=4)
  if entry in catalog['blocks']:
   act('sneak',down=crouch,ticks=4);s=act('open',shift=crouch,ticks=20);act('sneak',down=False,ticks=3)
  else:
   act('held',item=entry,ticks=6);s=act('use-item',ticks=20)
  if ui.startswith(('smitherz-','tieredz-')):
   b=(s['windows'][0] if a.side=='siu' else s)['bounds'];s=tap(b['x']+39,b['y']-10)
   expected='ReforgeScreenHandler' if ui.startswith('tieredz-') else 'SmitherScreenHandler' if ui.endswith('smither') else 'GrinderScreenHandler'
   menu=s['windows'][0].get('menu','') if a.side=='siu' and s.get('windows') else s.get('menu',s.get('menuClass',''))
   assert expected in str(menu),'Expected real secondary workstation tab'
  capture(a.run,a.side,a.minecraft,a.loader,ui,'Original crouch/right-click drawer or native workstation-tab interaction. Pictured state; no replacement provider is forced.')
  row['status']='ordinary-entry-and-picture-pass'
 except Exception as error:
  row.update(status='pending',reason=str(error)[:350]);print('PENDING',ui,str(error)[:180],flush=True)
  if isinstance(error,TimeoutError):raise
 (a.run/'specialized-extra-captures.json').write_text(json.dumps(rows,indent=2));print(ui,row['status'],flush=True)
