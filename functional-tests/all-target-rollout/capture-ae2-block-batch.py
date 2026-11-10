"""Natural AE2 block-screen captures with each installed release's own resources."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import capture,send
CASES=['inscriber','cell_workbench','io_port','condenser','molecular_assembler','vibration_chamber','drive',
       'sky_stone_chest','smooth_sky_stone_chest','me_chest','wireless_access_point','spatial_io_port','spatial_anchor',
       'interface','pattern_provider','security_station']
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True)
p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);p.add_argument('--only',action='append');a=p.parse_args()
send(a.run,'catalog',ticks=1);catalog=json.loads((a.run/'catalog.json').read_text());rows=[]
for name in CASES:
 if a.only and name not in a.only:continue
 block='ae2:'+name
 if block not in catalog['blocks'] and name=='me_chest' and 'ae2:chest' in catalog['blocks']:block='ae2:chest'
 if block not in catalog['blocks']:continue
 try:
  send(a.run,'close',ticks=3);send(a.run,'fixture',block=block,ticks=16)
  entry='client-use-block' if a.side=='siu' else 'open'
  if name=='me_chest':
   # The top opens the installed cell's terminal; the visible side opens
   # the chest's cell-management inventory even before a cell is installed.
   command='server-command' if a.side=='siu' else 'command'
   x,y,z=map(int,catalog.get('origin','8,65,8').split(','))
   send(a.run,command,command=f'fill {x-1} {y} {z+1} {x+1} {y+2} {z+3} minecraft:air',ticks=4)
   send(a.run,command,command='kill @e[type=!minecraft:player]',ticks=4)
   send(a.run,command,command='item replace entity @a weapon.mainhand with minecraft:air',ticks=4)
   send(a.run,command,command=f'tp @a {x+.5} {y} {z+2.5} 180 30',ticks=16)
   entry='open-ray'
  s=send(a.run,entry,ticks=20)['client']
  if (a.side=='native' and not s.get('menuClass')) or (a.side=='siu' and not s.get('windows')):
   s=send(a.run,entry,ticks=20)['client']
  if a.side=='siu':assert s.get('windows') and any(w.get('definition','').endswith('.AEWindow') for w in s['windows']),'No AE2 hosted screen'
  else:assert s.get('menuClass') and s.get('screen','').startswith('appeng.'),'No native AE2 screen'
  capture(a.run,a.side,a.minecraft,a.loader,'ae2-'+name.replace('_','-'),'Normal client block interaction; the installed AE2 release renders its own screen resources.')
  rows.append({'ui':name,'status':'pictured natural opening; feature checks separate'})
 except Exception as e:
  rows.append({'ui':name,'status':'pending','reason':str(e)[:300]});print('PENDING',name,str(e)[:100],flush=True)
 (a.run/'ae2-block-capture-checks.json').write_text(json.dumps(rows,indent=2))
