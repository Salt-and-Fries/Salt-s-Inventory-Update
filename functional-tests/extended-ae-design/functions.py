"""Affected Extended AE actions through live SIU pointer/text input; not a full gameplay suite."""
from implementation import Audit,ROOT,OUT
import json,sys,re
class Checks(Audit):
 def __init__(self,*args):
  super().__init__(*args);p=OUT/(self.loader+'-functions.json');self.results=json.loads(p.read_text())if p.exists()else[]
 def check(self,name,condition,detail=None):
  self.results=[r for r in self.results if r['name']!=name];self.results.append(dict(name=name,passed=bool(condition),detail=detail));(OUT/(self.loader+'-functions.json')).write_text(json.dumps(self.results,indent=2));assert condition,(name,detail);print('PASS',name,flush=True)
 def crafting_product(self):
  self.part('ex_crafting_terminal');self.cmd('item replace entity @p inventory.0 with minecraft:oak_log 1');self.inventory();self.player_slot(9);self.native_slot(41);self.check('native crafting preview',any(s['item']=='minecraft:oak_planks'for s in self.win()['slots']if not s['player']));self.capture('crafting-with-output');self.native_slot(50);self.call('snapshot-server');d=json.loads((self.run/'server-state.json').read_text());self.check('native output creates oak planks',any('oak_planks'in m.get('carried','')for m in d['menus']));self.player_slot(9);self.capture('crafting-output-taken')
 def paged_machines(self):
  for block in ['ex_interface','ex_molecular_assembler','ex_inscriber']:
   self.block(block);self.button('next');self.check(block+' native next page',self.win()['nativeValues'].get('page')=='1',self.win()['nativeValues']);self.capture(block+'-page-two');self.button('previous');self.check(block+' native previous page',self.win()['nativeValues'].get('page')=='0',self.win()['nativeValues'])
 def management(self):
  self.reset();self.call('held',item=self.prefix()+'pattern_modifier');self.call('use-item',ticks=12);self.button('mode');w=self.win();old=w['bounds'];button=next(b for b in w['nativeWidgets']if b.get('message')=='Replace');old_button=(button['x'],button['y']);meta=next(w for w in self.state['workspaceWindows']if w['kind']=='CONTAINER');p=meta['controls']['LOCK'];self.tap(p['x'],p['y']);x=old['x']+25;y=old['y']+8;self.call('click',x=x,y=y,button=0,ticks=2);self.call('drag',x=x-70,y=y+40,dx=-70,dy=40,button=0,ticks=5);self.call('release',x=x-70,y=y+40,button=0,ticks=5);w=self.win();new=w['bounds'];button=next(b for b in w['nativeWidgets']if b.get('message')=='Replace');self.check('native controls follow window movement',(new['x'],new['y'])!=(old['x'],old['y'])and(button['x']-old_button[0],button['y']-old_button[1])==(new['x']-old['x'],new['y']-old['y']),dict(before=old,after=new));self.button('mode');self.check('mode action still works after movement',self.win()['nativeValues'].get('page')=='2');self.capture('modifier-moved');meta=next(w for w in self.state['workspaceWindows']if w['kind']=='CONTAINER');p=meta['controls']['CLOSE'];self.tap(p['x'],p['y']);self.check('SIU close ends addon window',not any('GuiPatternModifier'in w.get('nativeScreen','')for w in self.state.get('allWindows',[])))
 def pattern_access(self):
  self.part('ex_pattern_access_part');self.cmd('setblock 9 65 8 '+self.prefix()+'ex_pattern_provider');self.cmd('data merge block 9 65 8 {patterns:[{Slot:0,id:"ae2:crafting_pattern",Count:1b,tag:{in:[{id:"minecraft:oak_log",Count:1b},{},{},{},{},{},{},{},{}],out:{id:"minecraft:oak_planks",Count:4b},recipe:"minecraft:oak_planks",substitute:0b,substituteFluids:0b}}]}',ticks=50);self.capture('pattern-access-populated');self.check('pattern access receives native provider rows',self.win()['nativeValues'].get('rows',0)>0,self.win()['nativeValues'])
  w=self.win();slot=next(s for s in w['slots']if s['item']=='ae2:crafting_pattern'and not s['player']);self.native_slot(slot['index']);self.call('snapshot-server');d=json.loads((self.run/'server-state.json').read_text());self.check('pattern access withdraws native provider pattern',any('crafting_pattern'in m.get('carried','')for m in d['menus']));self.native_slot(slot['index']);self.check('pattern access reinserts native provider pattern',any(s['item']=='ae2:crafting_pattern'for s in self.win()['slots']if not s['player']))
 def native_slot(self,index,button=0):
  w=self.win();s=next(s for s in w['slots']if s['index']==index);b=w['nativeBounds'];return self.tap(b['x']+s['x']+7,b['y']+s['y']+7,button)
 def player_slot(self,index):
  c=next(c for w in self.state['workspaceWindows']if w['kind']=='INVENTORY'for c in w['inventoryCells']if c['inventorySlot']==index);return self.tap(c['x'],c['y'])
 def inventory(self):self.cmd('gamemode survival @a');self.call('inventory',ticks=8)
 def modifier_actions(self):
  self.reset();self.call('held',item=self.prefix()+'pattern_modifier');self.cmd('item replace entity @p inventory.0 with ae2:processing_pattern{in:[{id:"minecraft:iron_ore","#c":"ae2:i","#":1L}],out:[{id:"minecraft:iron_ingot","#c":"ae2:i","#":1L}]} 1');self.call('use-item',ticks=12);self.inventory()
  self.player_slot(9);w=self.win();target=next(s for s in w['slots']if not s['player']and s['item']=='minecraft:air'and s['x']>0 and s['y']>50);self.native_slot(target['index']);self.button('x2');self.call('snapshot-server');state=json.loads((self.run/'server-state.json').read_text());item=state['inventory'][0]['item'];self.check('native pattern multiply x2',bool(re.search(r'2L',item)),item)
  self.button('mode');self.capture('replace-functional');self.check('replace mode synchronized',self.win()['nativeValues'].get('page')=='1',self.win()['nativeValues'])
  self.button('mode');self.check('clone mode synchronized',self.win()['nativeValues'].get('page')=='2',self.win()['nativeValues']);self.capture('clone-functional')
  self.button('mode');self.button('Clear');self.check('native clear returns blank pattern',any(s['item']=='ae2:blank_pattern'for s in self.win()['slots']if not s['player']))
 def text_filters(self):
  self.part('mod_export_bus');w=self.win();box=next(b for b in w['nativeWidgets']if 'AETextField'in b['type']);self.tap(box['x']+10,box['y']+4);self.call('text',text='minecraft',ticks=15);self.capture('mod-filter-functional');self.check('mod filter synchronized',self.win()['nativeValues'].get('exp')=='minecraft',self.win()['nativeValues'])
 def crafting_modes(self):
  self.part('ex_crafting_terminal')
  for mode in ['Crafting','Stonecutter','Smithing','Anvil']:
   b=next(b for b in self.win()['nativeWidgets']if b.get('message')==mode and b['type'].endswith('.TabButton'));self.tap(b['x']+9,b['y']+9)
   self.capture('extended-terminal-'+mode.lower());w=self.win();tabs=[b for b in w['nativeWidgets']if b.get('visible')and b.get('message')in['Crafting','Stonecutting','Smithing','Anvil']];root=w['bounds'];self.check(mode+' mode tab remains external',all(b['x']+b['width']>root['x']+root['width']-20 for b in tabs),tabs);self.check(mode+' page synchronized',mode.upper()in str(w['nativeValues']),w['nativeValues'])
 def wireless(self,item='wireless_ex_pat'):
  self.reset('ae2:wireless_access_point');self.cmd('gamemode survival @a')
  for p in ['8 64 8','8 65 9','7 65 8','8 66 8','8 65 7']:self.cmd('setblock '+p+' ae2:creative_energy_cell')
  self.call('held',item=self.prefix()+item,snbt='{accessPoint:{dimension:"minecraft:overworld",pos:[I;8,65,8]},internalCurrentPower:1000000.0d}',ticks=10);self.call('use-item',ticks=20);self.capture(item)
 def optional(self):
  self.block('ex_emc_interface');self.capture('emc-interface')
  for part in ['ex_emc_import_bus_part','ex_emc_export_bus_part']:self.part(part);self.capture(part)
 def matrix(self):
  pre=self.prefix();self.reset(pre+'assembler_matrix_frame')
  self.cmd('fill 8 65 8 11 67 10 '+pre+'assembler_matrix_frame')
  for pos in ['9 65 9 10 65 9','9 67 9 10 67 9','9 66 8 10 66 8','9 66 10 10 66 10']:self.cmd('fill '+pos+' '+pre+'assembler_matrix_wall')
  for pos in ['8 66 9','11 66 9']:self.cmd('setblock '+pos+' '+pre+'assembler_matrix_wall')
  self.cmd('setblock 9 66 9 '+pre+'assembler_matrix_pattern');self.cmd('setblock 10 66 9 '+pre+'assembler_matrix_crafter');self.cmd('setblock 8 64 8 ae2:creative_energy_cell',ticks=80)
  self.cmd('data merge block 9 66 9 {pattern:[{Slot:0,id:"ae2:crafting_pattern",Count:1b,tag:{in:[{id:"minecraft:oak_log",Count:1b},{},{},{},{},{},{},{},{}],out:{id:"minecraft:oak_planks",Count:4b},recipe:"minecraft:oak_planks",substitute:0b,substituteFluids:0b}}]}',ticks=30)
  self.call('open',ticks=30);self.capture('assembler-matrix-populated');self.check('assembler matrix receives live rows',self.win()['nativeValues'].get('rows',0)>0,self.win()['nativeValues'])
 def matrix_action(self):
  self.matrix();w=self.win();s=next(s for s in w['slots']if not s['player']and s['item']=='ae2:crafting_pattern');b=w['nativeBounds'];self.tap(b['x']+s['x']+7,b['y']+s['y']+7);self.call('snapshot-server');server=json.loads((self.run/'server-state.json').read_text());self.check('matrix native action withdraws pattern',any('crafting_pattern'in m.get('carried','')for m in server['menus']));self.inventory();self.player_slot(9)
 def inspectors(self):
  patterns=json.loads((ROOT/'build/remaining-ui-audit/fixtures/own-pattern-item-fixtures.json').read_text())
  for p in patterns:
   self.reset();self.call('held',item=p['item'],snbt=p['nbt']);self.call('eae-pattern-view',ticks=12);self.check(p['id']+' inspector stays native','.client.gui.pattern.Gui'in self.state['screen'],self.state['screen']);self.call('capture',capture='inspector-'+p['id'])
 def amount(self):
  self.part('precise_export_bus');self.cmd('item replace entity @p inventory.0 with minecraft:gold_ingot 1');self.inventory();self.player_slot(9)
  w=self.win();target=next(s for s in w['slots']if not s['player']and 0<s['x']<150 and s['y']>20)
  self.native_slot(target['index']);self.player_slot(9);self.native_slot(target['index'],2)
  self.capture('precise-amount-editor');self.check('native amount editor opens',self.win()['nativeScreen'].endswith('.SetAmount'),self.win()['nativeScreen']);self.button('+1');self.button('Set');self.check('amount returns to parent',self.win()['nativeScreen'].endswith('.GuiPreciseExportBus'))
 def renamer(self):
  self.reset('ae2:interface');self.call('held',item='ae2:certus_quartz_cutting_knife');self.call('use-block-ray',ticks=15);self.capture('renamer')
  box=next(b for b in self.win()['nativeWidgets']if 'AETextField'in b['type']);self.tap(box['x']+12,box['y']+5);self.call('text',text='Extended AE Test',ticks=12);self.call('key',key=257,ticks=8);self.call('snapshot-server');server=json.loads((self.run/'server-state.json').read_text());self.check('native renamer updates original block','Extended AE Test'in server.get('fixture',''),server.get('fixture'))
if __name__=='__main__':
 a=Checks(sys.argv[1],sys.argv[3]if len(sys.argv)>3 else'extended-ae-impl-06');getattr(a,sys.argv[2])()
