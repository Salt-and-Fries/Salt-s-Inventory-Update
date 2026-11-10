"""Focused shipping-jar Extended AE checks; no complete gameplay suite."""
from pathlib import Path
import json, sys, time

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/all-target-rollout'))
from evidence import send

class Audit:
    def __init__(self,mc,run):
        self.mc=mc;self.run=ROOT/f'build/all-target-rollout/gameplay/{mc}-neoforge-{run}'
        self.out=ROOT/f'output/extended-ae-all-targets/{mc}/{run}';self.out.mkdir(parents=True,exist_ok=True)
        self.state={};self.checks=json.loads((self.out/"checks.json").read_text())if(self.out/"checks.json").exists()else[];self.records=json.loads((self.out/"captures.json").read_text())if(self.out/"captures.json").exists()else[]
        self.call('catalog',namespaces=['extendedae'],ticks=1)
        self.pos=tuple(int(n.strip())for n in json.loads((self.run/'catalog.json').read_text())['origin'].split(','))
    def call(self,kind,**args):
        try:self.reply=send(self.run,kind,**args)
        except RuntimeError as error:
            detail=error.args[0];raise RuntimeError(detail.get('error',str(detail)[:500]) if isinstance(detail,dict)else str(detail)[:500])from None
        self.state=self.reply['client'];return self.state
    def cmd(self,text,ticks=5):return self.call('command',command=text,ticks=ticks)
    def check(self,name,condition):
        self.checks=[r for r in self.checks if r['name']!=name]+[dict(name=name,passed=bool(condition))];(self.out/'checks.json').write_text(json.dumps(self.checks,indent=2));assert condition,name;print('PASS',name,flush=True)
    def win(self):return next(w for w in self.state['allWindows']if'.extendedae.' in w.get('nativeScreen',''))
    def capture(self,name):
        self.call('move',x=2,y=2,ticks=2);self.call('capture',capture=name,ticks=3)
        w=self.win();self.check(name+' uses AEWindow',w['definition'].endswith('.AEWindow'))
        self.check(name+' has native geometry',bool(w.get('wirelessGeometry')) and not w.get('wirelessObserverError'))
        row=dict(name=name,capture=str(self.run/'screenshots'/(name+'.png')),state=self.state,server=self.reply['server']);self.records=[r for r in self.records if r["name"]!=name]+[row];(self.out/'captures.json').write_text(json.dumps(self.records,indent=2));print(name,w['nativeScreen'].split('.')[-1],w['bounds'],flush=True)
    def reset(self,block='minecraft:air'):
        self.call('close');self.cmd('gamemode creative @a');self.call('fixture',block='minecraft:stone'if block=='minecraft:air'else block,ticks=5)
        x,y,z=self.pos
        self.cmd(f'tp @a {x+.5} {y} {z+4.5} 180 17',8)
        if block=='minecraft:air':self.cmd(f'setblock {x} {y} {z} minecraft:air')
    def block(self,name):
        self.reset('extendedae:'+name);x,y,z=self.pos;self.cmd(f'setblock {x} {y-1} {z} ae2:creative_energy_cell');self.call('open',ticks=16)
    def item(self,name):
        self.reset();self.call('held',item='extendedae:'+name,ticks=6);self.call('use-item',ticks=16)
    def part(self,name):
        self.reset();x,y,z=self.pos;self.cmd(f'setblock {x} {y} {z-1} minecraft:stone');self.cmd(f'setblock {x} {y-1} {z} ae2:creative_energy_cell')
        self.call('held',item='ae2:fluix_glass_cable');self.call('use-block-ray',ticks=10)
        self.call('held',item='extendedae:'+name);self.call('use-block-ray',ticks=10)
        self.call('held',item='minecraft:air');self.call('use-block-ray',ticks=16)
    def tap(self,x,y,button=0):
        self.call('move',x=x,y=y,ticks=1);self.call('click',x=x,y=y,button=button,ticks=1);return self.call('release',x=x,y=y,button=button,ticks=12)
    def buttons(self):return[b for b in self.win()['nativeWidgets']if b.get('visible')]
    def press(self,label):
        b=next((b for b in self.buttons()if label.lower()==b['message'].lower()),None)or next(b for b in self.buttons()if label.lower()in b['message'].lower());return self.tap(b['x']+b['width']/2,b['y']+b['height']/2)
    def blocks(self):
        for name in ['ex_pattern_provider','ex_interface','oversize_interface','wireless_connect','wireless_hub','ingredient_buffer','ex_drive','ex_molecular_assembler','ex_inscriber','caner','ex_io_port','circuit_cutter','crystal_assembler']:
            if name=='ex_inscriber'and self.mc=='26.1.2':continue
            self.block(name);self.capture(name)
    def parts(self):
        for name in ['ex_export_bus_part','ex_import_bus_part','active_formation_plane','mod_export_bus','mod_storage_bus','precise_export_bus','precise_storage_bus','tag_export_bus','tag_storage_bus','threshold_export_bus','threshold_level_emitter','smart_annihilation_plane','ex_pattern_access_part']:
            self.part(name);self.capture(name)
    def tools(self):
        self.item('pattern_modifier')
        for i,mode in enumerate(['multiply','replace','properties','clone']):
            self.check('Pattern Modifier page '+str(i),self.win()['extendedValues']['menu_page']==str(i));self.capture('pattern-modifier-'+mode);self.press('change')
        for item in ['config_modifier','void_cell']:
            self.item(item);self.capture(item)

if __name__=='__main__':
    a=Audit(sys.argv[1],sys.argv[2]);getattr(a,sys.argv[3])()
