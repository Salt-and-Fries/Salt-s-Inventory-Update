"""Matched Create-family container pictures through the publisher's real entries."""
from pathlib import Path
import argparse,json,sys
HERE=Path(__file__).parent;ROOT=HERE.parents[1]
sys.path.insert(0,str(HERE));sys.path.insert(0,str(HERE.parent/'create-window-overhaul'))
from evidence import send,capture
from suite import Suite
from capture_originals import SCREEN_MENUS
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);p.add_argument('--only',action='append');a=p.parse_args()
class OriginalEntries(Suite):
 def window(self,menu=None):
  if a.side=='native':
   assert self.state.get('menuClass'),'No native container screen'
   identity=SCREEN_MENUS[self.state['screen']]
   if menu is not None:assert identity==menu,(identity,menu)
   return {'menuId':identity,'nativeScreen':self.state['screen'],'bounds':self.state['bounds']}
  windows=[w for w in self.state.get('windows',[]) if menu is None or w.get('menuId')==menu]
  assert len(windows)==1,[(w.get('menuId'),w.get('definition')) for w in windows]
  w=windows[0];assert w.get('templateClass')=='com.salts_inventory_update.compat.create.CreateWindows$Template',w.get('definition')
  assert not w.get('layoutError'),w.get('layoutError')
  return w
 def native_point(self,x,y,menu=None):
  if a.side=='native':
   b=self.window(menu)['bounds'];return {'x':b['x']+x,'y':b['y']+y}
  return super().native_point(x,y,menu)
s=OriginalEntries(a.run);rows=[]
def record(name,title):
 s.call('wait',ticks=12);s.call('move',x=5,y=5,ticks=3);w=s.window()
 capture(a.run,a.side,a.minecraft,a.loader,'create-'+name,title+': original release menu, ordinary client block/item/entity entry. Pictured state, not a complete feature test.')
def run(name,action):
 if a.only and not any(name.startswith(x) for x in a.only):return
 row={'ui':'create-'+name};rows.append(row)
 try:action();row['status']='ordinary-entry-and-picture-pass'
 except Exception as error:
  row.update(status='pending',reason=str(error)[:400]);print('PENDING',name,str(error)[:180],flush=True)
  if isinstance(error,TimeoutError):raise
 (a.run/'create-family-captures.json').write_text(json.dumps(rows,indent=2));print(name,row['status'],flush=True)
s.call('command',command='gamemode survival @a',ticks=3)
for command in ['weather clear','time set day','gamerule doWeatherCycle false','kill @e[type=!minecraft:player]','fill 6 65 7 12 72 13 minecraft:air']:
 s.call('command',command=command,ticks=3)
for name,title,block in [('toolbox','Toolbox','create:brown_toolbox'),('schematicannon','Schematicannon','create:schematicannon'),('schematic-table','Schematic Table','create:schematic_table'),('frogport','Package Frogport','create:package_frogport'),('postbox','Postbox','create:brown_postbox'),('requester','Redstone Requester','create:redstone_requester')]:
 def block_case(name=name,title=title,block=block):
  s.open_block(block);record(name,title)
  if name=='schematicannon':s.tap_native(17,120);record('schematicannon-settings','Schematicannon settings')
 run(name,block_case)
for name,title,item,shift in [('filter','List Filter','create:filter',False),('attribute','Attribute Filter','create:attribute_filter',False),('package-filter','Package Filter','create:package_filter',False),('controller','Linked Controller','create:linked_controller',True),('schedule','Train Schedule','create:schedule',False),('ammo','Autocannon Ammo Container','createbigcannons:autocannon_ammo_container',False),('creative-ammo','Creative Ammo Container','createbigcannons:creative_autocannon_ammo_container',False),('timed','Timed Fuze','createbigcannons:timed_fuze',False),('proximity','Proximity Fuze','createbigcannons:proximity_fuze',False),('inertia','Delayed Inertia Fuze','createbigcannons:delayed_inertia_fuze',False),('impact','Delayed Impact Fuze','createbigcannons:delayed_impact_fuze',False)]:
 def item_case(name=name,title=title,item=item,shift=shift):
  s.fixture();s.open_item(item,shift);record(name,title)
  if name=='schedule':s.tap_native(50,32);record('schedule-editor','Schedule instruction editor')
 run(name,item_case)
def stock():
 s.stock_fixture();s.call('command',command='tp @a 8.5 65 12.5 180 18',ticks=3);s.call('open-ray',ticks=12);record('categories','Stock Ticker categories');s.tap_native(27,32);record('category-editor','Stock Ticker category editor')
 s.call('close',ticks=2);s.call('command',command='tp @a 11.5 65 8.5',ticks=4);s.stock_open();s.call('wait',ticks=50);record('stock','Stock Ticker request catalogue')
run('stock',stock)
def blueprint():
 s.fixture('minecraft:stone');s.call('held',item='create:crafting_blueprint',ticks=3);s.call('use-block-ray',ticks=4);s.call('held',item='minecraft:air',ticks=3);s.call('command',command='tp @a 8.5 65 10.5 180 18',ticks=3);s.call('aim-entity',entity='create:crafting_blueprint',ticks=4);s.call('use-entity-ray',entity='create:crafting_blueprint',ticks=12);record('blueprint','Crafting Blueprint')
run('blueprint',blueprint)
def mounted():
 s.fixture();s.call('command',command='kill @e[type=create:crafting_blueprint]',ticks=3);s.call('command',command='summon railways:conductor 8.5 65 8.5 {NoAI:1b}',ticks=4);s.call('command',command='tp @a 8.5 65 10.5 180 18',ticks=4);s.call('held',item='create:brown_toolbox',ticks=3);s.call('aim-entity',entity='railways:conductor',ticks=3);s.call('use-entity-ray',entity='railways:conductor',ticks=4);s.call('held',item='minecraft:air',ticks=3);s.call('use-entity-ray',entity='railways:conductor',ticks=12);record('mounted-toolbox','Mounted Toolbox')
run('mounted-toolbox',mounted)
def gauge():
 s.fixture()
 for command in ['kill @e[type=railways:conductor]','setblock 8 65 7 minecraft:stone','setblock 8 65 8 create:factory_gauge[face=wall,facing=south]{bottom_left:{Freq:[I;1,2,3,4],FilterAmount:1},bottom_right:{Freq:[I;1,2,3,4],FilterAmount:1},top_left:{Freq:[I;1,2,3,4],FilterAmount:1},top_right:{Freq:[I;1,2,3,4],FilterAmount:1}}','tp @a 8.5 65 12.5 175.9 16']:s.call('command',command=command,ticks=4)
 s.call('wait',ticks=20);s.call('open-ray',ticks=12)
 if not s.state.get('windows') and not s.state.get('menuClass'):s.call('open-ray',ticks=12)
 record('gauge','Factory Gauge monitored item')
run('gauge',gauge)
