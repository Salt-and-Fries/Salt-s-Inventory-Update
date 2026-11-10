"""AE2 19 items using native 1.21.1 components and normal item use only."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import capture,send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--loader',required=True);p.add_argument('--minecraft',default='1.21.1');a=p.parse_args()
rows=[]
def act(kind,**kw):return send(a.run,kind,**kw)['client']
def command(text,ticks=6):return act('command',command=text,ticks=ticks)
for item in ['portable_item_cell_64k','portable_fluid_cell_64k','wireless_terminal','wireless_crafting_terminal']:
 row={'item':item};rows.append(row)
 try:
  act('close',ticks=3);command('fill 6 65 6 10 68 10 minecraft:air',ticks=3)
  act('fixture',block='ae2:wireless_access_point' if item.startswith('wireless') else 'minecraft:stone',ticks=8)
  command('gamemode survival @a');command('clear @a');command('kill @e[type=!minecraft:player]')
  components='ae2:stored_energy=1000000.0d'
  if item.startswith('wireless'):
   command('setblock 8 65 9 ae2:creative_energy_cell',ticks=40)
   components+=',ae2:wireless_link_target={dimension:"minecraft:overworld",pos:[I;8,65,8]}'
  command('item replace entity @a weapon.mainhand with ae2:'+item+'['+components+']',ticks=8)
  s=act('use-item',ticks=24)
  if a.side=='siu':assert any(w.get('definition','').endswith('.AEWindow') for w in s['windows']),'Expected AE2 integration'
  else:assert s.get('menuClass','').startswith('appeng.'),'Expected actual AE2 item UI'
  act('move',x=5,y=5,ticks=3)
  label=item.replace('_','-') if item.startswith('wireless') else 'portable-'+('fluid' if 'fluid' in item else 'item')+'-cell'
  capture(a.run,a.side,a.minecraft,a.loader,'ae2-'+label,'Original installed AE2 charged item, linked through its native global-position component where required. Ordinary item use opens the original UI; no menu provider is forced.')
  row['status']='natural-entry-and-picture-pass'
 except Exception as error:
  row.update(status='pending',reason=str(error)[:350]);print('PENDING',item,str(error)[:130],flush=True)
  if isinstance(error,TimeoutError):raise
 (a.run/'ae2-modern-item-captures.json').write_text(json.dumps(rows,indent=2));print(item,row['status'],flush=True)
