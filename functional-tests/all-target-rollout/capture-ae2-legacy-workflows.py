"""AE2 15 native item, network, CPU and configuration entry pictures only."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import capture,send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True)
p.add_argument('--loader',required=True);p.add_argument('--only',action='append')
p.add_argument('--minecraft',default='1.20.1')
p.add_argument('--press-only',action='store_true',help='Old excluded native observer has click-down but no release; use only for native press-triggered buttons')
a=p.parse_args()
if a.minecraft!='1.20.1' and not a.only:p.error('Modern targets require explicit navigation cases; legacy saved-item NBT is not a modern fixture')
if a.press_only and a.side!='native':p.error('Press-only is an old native observer capability')
cmd='server-command' if a.side=='siu' else 'command';rows=[];state={}
send(a.run,'catalog',ticks=1);catalog=json.loads((a.run/'catalog.json').read_text())
def act(kind,**kw):
 global state
 state=send(a.run,kind,**kw)['client'];return state
def command(text,ticks=6):return act(cmd,command=text,ticks=ticks)
def windows():return state.get('windows',state.get('allWindows',[]))
def widgets():return [x for w in windows() for x in w.get('nativeWidgets',[])] if a.side=='siu' else state.get('widgets',[])
def tap(x,y):
 act('click',x=x,y=y,ticks=1);return act('wait' if a.press_only else 'release',x=x,y=y,ticks=16)
def button(message):
 act('wait',ticks=3)
 b=next(w for w in widgets() if w.get('visible') and w.get('message')==message)
 return tap(b['x']+b['width']/2,b['y']+b['height']/2)
def picture(ui):
 if a.side=='siu':assert any(w.get('definition','').endswith('.AEWindow') for w in windows()),'Expected dedicated AE2 window'
 else:assert state.get('menuClass','').startswith('appeng.'),'Expected original native AE2 screen'
 capture(a.run,a.side,a.minecraft,a.loader,'ae2-'+ui,'Actual item/block entry or visible native navigation. This exact publisher release supplies its own resources. Pictured state only; this is not a complete feature test.')
def fixture(block='minecraft:stone'):
 assert block in catalog['blocks'],'Unknown native block: '+block
 act('close',ticks=3)
 command('fill 6 65 6 10 68 10 minecraft:air',ticks=3)
 command('fill 6 64 6 10 64 10 minecraft:stone',ticks=3)
 act('fixture',block=block,ticks=10)
 command('gamemode creative @a');command('kill @e[type=!minecraft:player]')
def held(item,tag='{}'):
 if a.side=='native':act('held',item='ae2:'+item,snbt=tag,ticks=8)
 else:command('item replace entity @a weapon.mainhand with ae2:'+item+(tag if tag!='{}' else ''),ticks=8)
def open_block():return act('client-use-block' if a.side=='siu' else 'open',ticks=24)
def part(name):
 fixture('ae2:cable_bus')
 command('setblock 8 65 8 ae2:cable_bus{cable:{id:"ae2:fluix_glass_cable"},south:{id:"ae2:'+name+'"}}')
 command('setblock 9 65 8 ae2:creative_energy_cell')
 command('tp @a 8.5 65 11.5 180 24',ticks=12)
 act('use-block-ray',ticks=20)
def run(ui,action):
 if a.only and ui not in a.only:return
 try:action();rows.append({'ui':ui,'status':'natural-entry-and-picture-pass'})
 except Exception as error:
  rows.append({'ui':ui,'status':'pending','reason':str(error)[:350]});print('PENDING',ui,str(error)[:100],flush=True)
  if isinstance(error,TimeoutError):raise
 (a.run/'ae2-legacy-navigation-checks.json').write_text(json.dumps(rows,indent=2))

def item_ui(ui,item,tag='{}'):
 fixture();held(item,tag);act('client-use-item' if a.side=='siu' else 'use-item',ticks=20);picture(ui)
run('network-tool',lambda:item_ui('network-tool','network_tool'))
run('quartz-cutting-knife',lambda:item_ui('quartz-cutting-knife','certus_quartz_cutting_knife'))
for kind in ['item','fluid']:
 tag='{internalCurrentPower:1000000.0d}'
 run('portable-'+kind+'-cell',lambda kind=kind,tag=tag:item_ui('portable-'+kind+'-cell','portable_'+kind+'_cell_64k',tag))
for item in ['wireless_terminal','wireless_crafting_terminal']:
 def wireless(item=item):
  # The default north-facing aerial connects to its south/back face.
  fixture('ae2:wireless_access_point');command('setblock 8 65 9 ae2:creative_energy_cell',ticks=30)
  held(item,'{internalCurrentPower:1000000.0d,accessPoint:{dimension:"minecraft:overworld",pos:[I;8,65,8]}}')
  act('client-use-item' if a.side=='siu' else 'use-item',ticks=20);picture(item.replace('_','-'))
 run(item.replace('_','-'),wireless)
def block_ui(ui,block,tool=False):
 fixture('ae2:'+block)
 command('setblock 8 64 8 ae2:cable_bus{cable:{id:"ae2:fluix_glass_cable"}}')
 command('setblock 9 64 8 ae2:creative_energy_cell',ticks=60)
 if tool:held('network_tool')
 open_block();picture(ui)
run('controller-network-status',lambda:block_ui('controller-network-status','controller'))
run('network-tool-status',lambda:block_ui('network-tool-status','controller',True))
run('crafting-cpu',lambda:block_ui('crafting-cpu','64k_crafting_storage'))
def quantum():
 fixture('ae2:quantum_link')
 for dx,dz in [(-1,-1),(-1,0),(-1,1),(0,-1),(0,1),(1,-1),(1,0),(1,1)]:command(f'setblock {8+dx} 65 {8+dz} ae2:quantum_ring',ticks=2)
 open_block();picture('quantum-network-bridge')
run('quantum-network-bridge',quantum)
def priority():part('storage_bus');button('Priority');picture('priority')
run('priority',priority)
def settings():part('terminal');button('Terminal Settings');picture('terminal-settings')
run('terminal-settings',settings)
for label in ['Processing Patterns','Stonecutting Patterns','Smithing Table Patterns']:
 def mode(label=label):
  part('pattern_encoding_terminal');button(label);picture('pattern-'+label.lower().replace(' ','-'))
 run('pattern-'+label.lower().replace(' ','-'),mode)
def crafting_status():part('terminal');button('Crafting Status');picture('crafting-status')
run('crafting-status',crafting_status)
def stock_amount():
 part('cable_interface')
 # Populate only an already-open native ghost/config slot. The visible native
 # button still owns the transition to its stock-amount container.
 if a.side=='native':act('seed',slot=37,item='minecraft:iron_ingot',count=64,ticks=12)
 else:act('reviewed-session-seed',menu='appeng.menu.implementations.InterfaceMenu',slot=37,item='minecraft:iron_ingot',count=64,ticks=12)
 button('Set amount to stock');picture('stock-amount')
run('stock-amount',stock_amount)
