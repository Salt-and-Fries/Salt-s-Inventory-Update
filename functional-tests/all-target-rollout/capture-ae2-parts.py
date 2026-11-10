"""Open actual installed AE2 parts by ray interaction; no menu factories are forced."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import capture,send

PARTS=['terminal','crafting_terminal','pattern_encoding_terminal','pattern_access_terminal',
       'export_bus','import_bus','storage_bus','formation_plane','level_emitter','energy_level_emitter',
       'cable_interface','cable_pattern_provider']
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True)
p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);p.add_argument('--only',action='append');a=p.parse_args()
cmd='server-command' if a.side=='siu' else 'command'
def command(text,ticks=10):return send(a.run,cmd,command=text,ticks=ticks)
send(a.run,'catalog',ticks=1);catalog=json.loads((a.run/'catalog.json').read_text());items=set(catalog['items'])
rows=[]
for part in PARTS:
 if a.only and part not in a.only:continue
 if 'ae2:'+part not in items:continue
 try:
  send(a.run,'close',ticks=3);send(a.run,'fixture',block='ae2:cable_bus',ticks=8)
  command('gamemode creative @a');command('kill @e[type=!minecraft:player]')
  # NBT is the publisher's normal saved-part schema. The original part and
  # original ray dispatch choose the menu, including its native opening packet.
  command('setblock 8 65 8 ae2:cable_bus{cable:{id:"ae2:fluix_glass_cable"},south:{id:"ae2:'+part+'"}}')
  command('setblock 9 65 8 ae2:creative_energy_cell')
  # A previously held portable/wireless cell has its own right-click action.
  # Use an empty hand for the native part interaction on this fresh fixture.
  command('item replace entity @a weapon.mainhand with minecraft:air',ticks=3)
  command('tp @a 8.5 65 11.5 180 24',ticks=15)
  s=send(a.run,'use-block-ray',ticks=20)['client']
  if a.side=='siu' and not s.get('windows'):
   # A freshly placed grid can need its original node/channel tick before a
   # part becomes interactive. Retry the same player interaction after it.
   send(a.run,'wait',ticks=80)
   s=send(a.run,'use-block-ray',ticks=20)['client']
  if a.side=='siu':
   assert any(w.get('definition','').endswith('.AEWindow') for w in s.get('windows',[])),'No integrated AE2 window'
  else:assert s.get('menuClass','').startswith('appeng.'),'No original AE2 part screen'
  ui='ae2-'+part.removeprefix('cable_').replace('_','-')+('-part' if part.startswith('cable_') else '')
  capture(a.run,a.side,a.minecraft,a.loader,ui,'Actual south-facing native part on a powered cable, opened by the player ray. Pictured default configuration; feature tests are separate.')
  rows.append({'part':part,'status':'natural-entry-and-picture-pass'})
 except Exception as e:
  rows.append({'part':part,'status':'pending','reason':str(e)[:300]});print('PENDING',part,str(e)[:100],flush=True)
  if isinstance(e,TimeoutError):raise
 (a.run/'ae2-part-capture-checks.json').write_text(json.dumps(rows,indent=2))
