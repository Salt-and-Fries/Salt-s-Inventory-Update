"""Record actual original UI openings in disposable worlds; design evidence only."""
from pathlib import Path
import sys,json,hashlib
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
OUT=ROOT/'output/extended-ae-window-design'
class Audit:
 def __init__(self,loader='forge',run='extended-ae-design-01'):
  self.loader=loader;self.run=ROOT/f'build/remaining-ui-audit/gameplay/1.20.1-{loader}-native-{run}';self.file=OUT/(loader+('-optional'if'optional'in run else'')+'-extra-captures.json');self.rows=json.loads(self.file.read_text())if self.file.exists()else[];self.state={}
 def call(self,kind,**kw):self.state=send(self.run,kind,**kw)['client'];return self.state
 def cmd(self,cmd,ticks=3):return self.call('command',command=cmd,ticks=ticks)
 def capture(self,name,opening,notes=''):
  self.call('move',x=2,y=2,ticks=2);self.call('capture',capture=name,ticks=4);assert 'bounds'in self.state,(name,self.state)
  path=self.run/'screenshots'/(name+'.png');r=dict(id=name,loader=self.loader,release='1.20-1.4.18-forge'if self.loader=='forge'else'1.20-1.0.2-fabric',opening=opening,notes=notes,native=dict(capture=path.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),**self.state))
  self.rows=[r0 for r0 in self.rows if r0['id']!=name]+[r];self.file.write_text(json.dumps(self.rows,indent=2));print('CAPTURED',name,self.state['screen'].split('.')[-1],self.state['bounds'],flush=True);return r
 def prefix(self):return'expatternprovider:'if self.loader=='forge'else'extendedae:'
 def reset(self,block='minecraft:air'):
  self.call('close');self.cmd('gamemode creative @a');self.cmd('fill 6 65 6 13 70 13 minecraft:air');self.call('fixture',block=block,ticks=5)
 def part(self,item):
  self.reset();self.cmd('setblock 8 65 7 minecraft:stone');self.cmd('setblock 8 64 8 ae2:creative_energy_cell');self.cmd('tp @p 8.5 65 12.5 180 17')
  self.call('held',item='ae2:fluix_glass_cable',ticks=4);self.call('use-block-ray',ticks=8)
  self.call('held',item=self.prefix()+item,ticks=4);self.call('use-block-ray',ticks=8)
  self.call('held',item='minecraft:air',ticks=3);self.call('use-block-ray',ticks=12)
 def tap(self,x,y):
  self.call('move',x=x,y=y,ticks=1);self.call('click',x=x,y=y,button=0,ticks=1);self.call('release',x=x,y=y,button=0,ticks=8)
if __name__=='__main__':
 a=Audit()
 for item in ['ex_export_bus_part','ex_import_bus_part','active_formation_plane','mod_export_bus','mod_storage_bus','precise_export_bus','precise_storage_bus','tag_export_bus','tag_storage_bus','threshold_export_bus','threshold_level_emitter','ex_pattern_access_part','ex_crafting_terminal']:
  try:a.part(item);a.capture('forge-'+item.replace('_','-'),'Attach the original '+item+' to a powered ME cable, then use its front face.')
  except Exception as error:print('OPEN FAILED',item,str(error)[:200],flush=True)
