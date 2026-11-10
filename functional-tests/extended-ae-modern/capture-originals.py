"""Capture publisher screens in disposable clients with SIU completely absent."""
from pathlib import Path
import sys,json,hashlib,time,uuid
from PIL import Image
from focused import Audit,ROOT
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send

class Native(Audit):
    def __init__(self,mc,optional=False,run_id='extended-ae-pdf-01'):
        self.mc=mc;self.run=ROOT/f'build/remaining-ui-audit/gameplay/{mc}-neoforge-native-{run_id}'
        self.out=ROOT/'output/extended-ae-modern-comparisons';self.out.mkdir(parents=True,exist_ok=True)
        self.file=self.out/f'native-{mc}.json';self.rows=json.loads(self.file.read_text())if self.file.exists()else[]
        self.state={};self.pos=(8,65,8)
        self.references={r['name']:r for r in json.loads((ROOT/f'output/extended-ae-all-targets/{mc}/extended-ae-port-13/captures.json').read_text())}
        if optional:
            self.run=ROOT/f'build/remaining-ui-audit/gameplay/{mc}-neoforge-native-extended-ae-emc-pdf-01'
            extra=ROOT/f'output/extended-ae-all-targets/{mc}/extended-ae-emc-pdf-01/captures.json'
            self.references.update({r['name']:r for r in json.loads(extra.read_text())})
        self.call('wait',ticks=2)
    def call(self,kind,**args):
        try:self.reply=send(self.run,kind,**args)
        except RuntimeError as error:raise RuntimeError(str(error.args[0].get('error',error.args[0]))[:900])from None
        self.state=self.reply['client'];return self.state
    def win(self):return {**self.state,'nativeScreen':self.state['screen'],'extendedValues':{'menu_'+k:v for k,v in self.state.get('extendedMenuValues',{}).items()}}
    def buttons(self):return [b for b in self.state.get('widgets',[])if b.get('visible')]
    def capture(self,name):
        stem=name.replace('_','-')+'-'+uuid.uuid4().hex[:8]
        self.call('move',x=2,y=2,ticks=2);self.call('capture',capture=stem,ticks=3)
        assert self.state['siu_loaded']is False
        expected=next(w['nativeScreen']for w in self.references[name]['state']['allWindows']if '.extendedae.'in w.get('nativeScreen',''))
        assert self.state['screen']==expected,(name,self.state['screen'],expected)
        image=self.run/'screenshots'/(stem+'.png');deadline=time.monotonic()+10
        while True:
            try:
                with Image.open(image)as im:im.verify()
                break
            except (OSError,ValueError):
                if time.monotonic()>deadline:raise
                time.sleep(.1)
        row=dict(name=name,minecraft=self.mc,loader='neoforge',capture=str(image),sha256=hashlib.sha256(image.read_bytes()).hexdigest(),state=self.state)
        self.rows=[r for r in self.rows if r['name']!=name]+[row];self.file.write_text(json.dumps(self.rows,indent=2)+'\n')
        print('CAPTURE',self.mc,name,self.state['screen'].split('.')[-1],flush=True)
    def tools(self):
        self.item('pattern_modifier')
        for i,mode in enumerate(['multiply','replace','properties','clone']):
            assert self.state['extendedMenuValues']['page']==str(i);self.capture('pattern-modifier-'+mode)
            if i<3:self.press('change')
        for name in ['config_modifier','void_cell']:
            self.item(name);self.capture(name)
    def parts(self):
        for name in ['ex_export_bus_part','ex_import_bus_part','active_formation_plane','mod_export_bus','mod_storage_bus','precise_export_bus','precise_storage_bus','tag_export_bus','tag_storage_bus','threshold_export_bus','threshold_level_emitter','smart_annihilation_plane']:
            if any(r['name']==name for r in self.rows):continue
            self.part(name);self.capture(name)
    def extras(self):
        self.reset('ae2:interface');self.call('held',item='ae2:certus_quartz_cutting_knife');self.call('use-block-ray',ticks=16);self.capture('renamer')
        self.block('crystal_assembler');b=next(b for b in self.buttons()if b['type'].endswith('ServerSettingToggleButton'));self.tap(b['x']+7,b['y']+7);self.press('output');self.capture('output-side-selection')
        self.part('precise_export_bus');self.cmd('gamemode survival @a');self.cmd('item replace entity @a inventory.0 with minecraft:gold_ingot 1',8)
        source=next(s for s in self.state['slots']if s['player']and s['item']=='minecraft:gold_ingot');self.cell(source)
        index=next(v['index']for v in self.state['slotSemantics']if 'id=CONFIG,'in v['semantic']);target=self.state['slots'][index];self.cell(target);self.cell(source);self.call('wait',ticks=10)
        self.cell(target,button=2);self.capture('precise-amount-editor')
        self.reset('ae2:wireless_access_point');x,y,z=self.pos
        for dx,dy,dz in [(0,-1,0),(0,1,0),(1,0,0),(-1,0,0),(0,0,1),(0,0,-1)]:self.cmd(f'setblock {x+dx} {y+dy} {z+dz} ae2:creative_energy_cell',5)
        self.call('wait',ticks=35)
        item='extendedae:wireless_ex_pat[ae2:stored_energy=1000000.0d,ae2:wireless_link_target={dimension:"minecraft:overworld",pos:[I;'+f'{x},{y},{z}'+']}]'
        self.cmd('item replace entity @a weapon.mainhand with '+item,10);self.call('use-item',ticks=25);self.capture('wireless-pattern-access')
    def cell(self,slot,button=0):
        b=self.state['bounds'];return self.tap(b['x']+slot['x']+7,b['y']+slot['y']+7,button)
    def populated(self):
        self.part('ex_pattern_access_part');x,y,z=self.pos
        self.cmd(f'setblock {x+1} {y} {z} extendedae:ex_pattern_provider');self.call('close');self.cmd(f'tp @a {x+1.5} {y} {z+4.5} 180 17',12);self.call('use-block-ray',ticks=20)
        self.call('eae-seed-pattern',slot=9,ticks=5);source=next(s for s in self.state['slots']if s['player']and s['item']=='ae2:processing_pattern');self.cell(source)
        index=next(v['index']for v in self.state['slotSemantics']if 'id=ENCODED_PATTERN,'in v['semantic']);self.cell(self.state['slots'][index]);self.call('wait',ticks=25);self.call('close');self.cmd(f'tp @a {x+.5} {y} {z+4.5} 180 17',12);self.call('use-block-ray',ticks=25);self.capture('pattern-access-populated')
        self.reset('extendedae:assembler_matrix_frame');x,y,z=self.pos
        self.cmd(f'fill {x} {y} {z} {x+3} {y+2} {z+2} extendedae:assembler_matrix_frame')
        for a,b in [((1,0,1),(2,0,1)),((1,2,1),(2,2,1)),((1,1,0),(2,1,0)),((1,1,2),(2,1,2)),((0,1,1),(0,1,1)),((3,1,1),(3,1,1))]:self.cmd(f'fill {x+a[0]} {y+a[1]} {z+a[2]} {x+b[0]} {y+b[1]} {z+b[2]} extendedae:assembler_matrix_wall')
        self.cmd(f'setblock {x+1} {y+1} {z+1} extendedae:assembler_matrix_pattern');self.cmd(f'setblock {x+2} {y+1} {z+1} extendedae:assembler_matrix_crafter');self.cmd(f'setblock {x} {y-1} {z} ae2:creative_energy_cell',60)
        self.call('eae-seed-pattern',matrix=[x+1,y+1,z+1],ticks=30);self.call('open',ticks=30);self.capture('assembler-matrix-populated')
    def optional(self):
        self.block('ex_emc_interface');self.capture('ex-emc-interface')
        for name in ['ex_emc_import_bus_part','ex_emc_export_bus_part']:
            self.part(name);self.capture(name.replace('_','-'))
    def output_only(self):
        self.block('crystal_assembler');b=next(b for b in self.buttons()if b['type'].endswith('ServerSettingToggleButton'));self.tap(b['x']+7,b['y']+7);self.press('output');self.capture('output-side-selection')

if __name__=='__main__':
    a=Native(sys.argv[1],sys.argv[2]=='optional',sys.argv[3]if len(sys.argv)>3 else'extended-ae-pdf-01');getattr(a,sys.argv[2])()
