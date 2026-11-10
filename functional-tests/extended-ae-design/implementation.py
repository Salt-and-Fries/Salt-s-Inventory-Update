"""Focused original Extended AE menus, hosted by the shipping compatibility jar."""
from pathlib import Path
import json,sys,hashlib
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
OUT=ROOT/'output/extended-ae-implementation';OUT.mkdir(exist_ok=True,parents=True)
class Audit:
 def __init__(self,loader='forge',run='extended-ae-impl-02'):
  self.loader=loader;self.run=ROOT/f'build/remaining-ui-audit/gameplay/1.20.1-{loader}-siu-{run}'
  self.file=OUT/(loader+'-'+run+'.json');self.rows=json.loads(self.file.read_text())if self.file.exists()else[];self.state={}
 def call(self,kind,**kw):
  r=send(self.run,kind,**kw);self.state=r['client'];return self.state
 def cmd(self,cmd,ticks=3):return self.call('command',command=cmd,ticks=ticks)
 def prefix(self):return'expatternprovider:'if self.loader=='forge'else'extendedae:'
 def win(self):return next(w for w in self.state.get('allWindows',self.state.get('windows',[]))if w.get('nativeScreen','').startswith(('com.github.glodblock.extendedae.','com.glodblock.github.extendedae.')))
 def capture(self,name):
  name=name.replace('_','-')
  self.call('move',x=2,y=2,ticks=2);self.call('capture',capture=name,ticks=4);w=self.win()
  assert 'InventoryDesktopScreen'in self.state['screen'],self.state['screen']
  path=self.run/'screenshots'/(name+'.png');r=dict(id=name,loader=self.loader,capture=path.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),**self.state)
  self.rows=[v for v in self.rows if v['id']!=name]+[r];self.file.write_text(json.dumps(self.rows,indent=2));print('CAPTURED',name,w['nativeScreen'].split('.')[-1],w['bounds'],flush=True);return r
 def reset(self,block='minecraft:air'):
  self.call('close');self.cmd('gamemode creative @a');self.cmd('fill 6 65 6 13 70 13 minecraft:air');self.call('fixture',block=block,ticks=5)
 def block(self,item):
  self.reset(self.prefix()+item);self.cmd('setblock 8 64 8 ae2:creative_energy_cell');self.call('open',ticks=15)
 def part(self,item):
  self.reset();self.cmd('setblock 8 65 7 minecraft:stone');self.cmd('setblock 8 64 8 ae2:creative_energy_cell');self.cmd('tp @p 8.5 65 12.5 180 17')
  self.call('held',item='ae2:fluix_glass_cable',ticks=4);self.call('use-block-ray',ticks=8)
  self.call('held',item=self.prefix()+item,ticks=4);self.call('use-block-ray',ticks=8)
  self.call('held',item='minecraft:air',ticks=3);self.call('use-block-ray',ticks=12)
 def tap(self,x,y,b=0):
  self.call('move',x=x,y=y,ticks=1);self.call('click',x=x,y=y,button=b,ticks=1);return self.call('release',x=x,y=y,button=b,ticks=8)
 def button(self,message):
  w=self.win();b=next(b for b in w['nativeWidgets']if b.get('visible',True)and message.lower()in b.get('message','').lower());return self.tap(b['x']+b['width']//2,b['y']+b['height']//2)
 def modifier(self):
  self.reset();self.call('held',item=self.prefix()+'pattern_modifier');self.call('use-item',ticks=15)
  for mode in ['multiply','replace','clone']:
   self.capture('pattern-modifier-'+mode)
   self.button('mode')
 def blocks(self):
  for item in ['ex_pattern_provider','ex_interface','wireless_connect','wireless_hub','ingredient_buffer','ex_drive','ex_molecular_assembler','ex_inscriber','caner','ex_io_port','circuit_cutter','oversize_interface']:
   if self.loader=='fabric'and item in ['wireless_hub','circuit_cutter','oversize_interface']:continue
   self.block(item);self.capture(item)
 def parts(self):
  for item in ['ex_export_bus_part','ex_import_bus_part','active_formation_plane','mod_export_bus','mod_storage_bus','precise_export_bus','precise_storage_bus','tag_export_bus','tag_storage_bus','threshold_export_bus','threshold_level_emitter','ex_pattern_access_part','ex_crafting_terminal']:
   if self.loader=='fabric'and item=='ex_crafting_terminal':continue
   self.part(item);self.capture(item)
if __name__=='__main__':
 a=Audit(sys.argv[1]if len(sys.argv)>1 else'forge',sys.argv[3]if len(sys.argv)>3 else'extended-ae-impl-02')
 getattr(a,sys.argv[2]if len(sys.argv)>2 else'modifier')()
