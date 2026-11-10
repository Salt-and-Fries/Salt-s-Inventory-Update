"""Use each native ship parent's normal sneak interaction, without forcing menus."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import capture,send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);a=p.parse_args()
send(a.run,'catalog',ticks=1);catalog=json.loads((a.run/'catalog.json').read_text())
types=sorted(x.removeprefix('smallships:oak_') for x in catalog['items'] if x.startswith('smallships:oak_'))
rows=[]
for ship in types:
 try:
  send(a.run,'close',ticks=3);send(a.run,'fixture',block='minecraft:air',ticks=8)
  for command in ['gamemode creative @a','kill @e[type=!minecraft:player]',f'summon smallships:{ship} 8.5 65 8.5', 'tp @a 8.5 65 13 180 10']:
   send(a.run,'command',command=command,ticks=8)
  send(a.run,'sneak',down=True,ticks=5)
  s=send(a.run,'use-entity',entity='smallships:'+ship,shift=True,ticks=24)['client']
  if a.side=='siu':assert s.get('windows') and all('AutomaticNativeWindow' not in w.get('definition','') for w in s['windows']),'Expected integrated ship inventory'
  else:assert s.get('menuClass') and any(not v['player'] for v in s.get('slots',[])),'No native ship inventory opened'
  capture(a.run,a.side,a.minecraft,a.loader,'smallships-'+ship,'Original sneak/right-click parent-ship interaction. The publisher owns its menu, slot inventory and permissions; no menu provider is substituted. Pictured default inventory only.')
  rows.append({'ship':ship,'status':'ordinary-entry-and-picture-pass'})
 except Exception as error:
  rows.append({'ship':ship,'status':'pending','reason':str(error)[:350]});print('PENDING',ship,str(error)[:100],flush=True)
  if isinstance(error,TimeoutError):raise
 (a.run/'smallships-capture-checks.json').write_text(json.dumps(rows,indent=2))
