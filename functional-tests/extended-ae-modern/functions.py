"""Native addon action/transport checks through the real hosted widgets."""
from focused import Audit,ROOT
import sys,json

class Checks(Audit):
    def inventory(self):
        self.cmd('gamemode survival @a')
        if not any(w['kind']=='INVENTORY'for w in self.state['workspaceWindows']):self.call('inventory',ticks=15)
    def player_slot(self,index=9):
        c=next(c for w in self.state['workspaceWindows']if w['kind']=='INVENTORY'for c in w['inventoryCells']if c['inventorySlot']==index);return self.tap(c['x'],c['y'])
    def slot(self,index):
        w=self.win();s=w['slots'][index];b=w['nativeBounds'];return self.tap(b['x']+s['x']+7,b['y']+s['y']+7)
    def semantic(self,name):
        w=self.win();return[w['slots'][s['index']]for s in w['slotVisibility']if s['visible']and('id='+name+',')in s['semantic']]
    def text(self,value):
        b=next(b for b in self.buttons()if'AETextField'in b['type'])
        self.call('xy-button',x=b['x']+5,y=b['y']+4,button=1,ticks=6)
        self.tap(b['x']+5,b['y']+4);self.call('text',text=value,ticks=12);return self.call('wait',ticks=30)
    def pages(self):
        for block,count in [('ex_interface',2),('ex_molecular_assembler',8),('ex_inscriber',4)]:
            if block=='ex_inscriber'and self.mc=='26.1.2':continue
            self.block(block)
            for page in range(1,count):
                self.press('next');self.check(block+' native page '+str(page),self.win()['extendedValues']['menu_page']==str(page))
            self.capture(block+'-last-page');self.press('previous');self.check(block+' native previous page',self.win()['extendedValues']['menu_page']==str(count-2))
    def filters(self):
        self.part('mod_export_bus');self.text('minecraft');self.check('Native mod filter reaches server and synchronizes',self.win()['extendedValues'].get('menu_exp')=='minecraft');self.capture('mod-filter-edited')
        self.item('config_modifier');self.check('Configuration tool mode initialized',self.win()['extendedValues'].get('menu_mode')=='MUL')
        b=next(b for b in self.buttons()if b['type'].endswith('AE2Button'));self.tap(b['x']+10,b['y']+8);self.call('wait',ticks=16);self.check('Configuration tool changes native mode',self.win()['extendedValues'].get('menu_mode')=='DIV')
        self.text('42');self.check('Configuration numeric entry reaches server',self.win()['extendedValues'].get('menu_data')=='42');self.capture('config-modifier-edited')
        self.item('void_cell');buttons=[b for b in self.buttons()if b['type'].endswith('ActionEPPButton')];buttons.sort(key=lambda b:b['x']);b=buttons[1];self.tap(b['x']+7,b['y']+7);self.call('wait',ticks=16);self.check('Void Cell mode reaches native menu',self.win()['extendedValues'].get('menu_mode')=='1');self.capture('void-cell-mode')
        self.reset('ae2:interface');self.call('held',item='ae2:certus_quartz_cutting_knife');self.call('use-block-ray',ticks=16);self.capture('renamer');self.text('Extended Window');self.check('Renamer text reaches native menu',self.win()['extendedValues'].get('menu_name')=='Extended Window');self.call('key',key=257,ticks=10)
        self.check('Renamer native confirm closes its window',not any('.GuiRenamer'in w.get('nativeScreen','')for w in self.state['allWindows']))
    def output_sides(self):
        self.block('crystal_assembler');b=next(b for b in self.buttons()if b['type'].endswith('ServerSettingToggleButton'));self.tap(b['x']+7,b['y']+7);self.call('wait',ticks=15)
        self.press('output');self.capture('output-side-selection');self.check('Output-side native subview retained',self.win()['nativeScreen'].endswith('.OutputSideConfig'))
        buttons=[b for b in self.buttons()if b['type'].endswith('OutputButton')];self.check('All six native output-side controls retained',len(buttons)==6)
        before=self.reply['server']['nativeValues'].get('hostOutputSides')
        self.check('Output-side initial native host selection synchronized',set(self.win()['extendedValues'].get('outputs','').strip('[]').split(', '))==set(before.strip('[]').split(', ')))
        b=buttons[0];self.tap(b['x']+7,b['y']+7);self.call('wait',ticks=15)
        self.check('Output-side edit reaches native machine',self.reply['server']['nativeValues'].get('hostOutputSides')!=before)
        b=next(b for b in self.buttons()if b['type'].endswith('TabButton'));self.tap(b['x']+7,b['y']+7);self.check('Output-side return restores native machine',self.win()['nativeScreen'].endswith('.GuiCrystalAssembler'))
    def amount(self):
        self.part('precise_export_bus');self.call('seed-player',items=[dict(slot=9,item='minecraft:gold_ingot',count=1)],ticks=5);self.inventory();self.player_slot()
        slot=self.semantic('CONFIG')[0]['index'];self.slot(slot);self.player_slot();self.call('wait',ticks=12);w=self.win();s=w['slots'][slot]
        self.call('xy-button',x=w['nativeBounds']['x']+s['x']+7,y=w['nativeBounds']['y']+s['y']+7,button=2,ticks=12)
        self.check('Native middle-click opens amount editor',self.win()['nativeScreen'].endswith('.SetAmount'));self.capture('precise-amount-editor');self.press('+1');self.press('Set');self.call('wait',ticks=15)
        self.check('Native amount save returns to bus',self.win()['nativeScreen'].endswith('.GuiPreciseExportBus'))
    def pattern_actions(self):
        self.item('pattern_modifier');self.call('eae-seed-pattern',slot=9,ticks=5);self.inventory();self.player_slot();self.slot(self.semantic('ENCODED_PATTERN')[0]['index']);self.press('x2')
        p=next(p for p in self.reply['server']['slots']if p['item']=='ae2:processing_pattern');self.check('Native multiply modifies processing pattern', 'amount=2'in p['components']or 'amount: 2'in p['components']);self.capture('modifier-multiplied')
        self.press('clear');self.check('Native clear restores blank pattern',any(s['item']=='ae2:blank_pattern'for s in self.win()['slots']if not s['player']))
    def pattern_access(self):
        self.part('ex_pattern_access_part');x,y,z=self.pos
        self.cmd(f'setblock {x+1} {y} {z} extendedae:ex_pattern_provider');self.call('close');self.cmd(f'tp @a {x+1.5} {y} {z+4.5} 180 17',12);self.call('use-block-ray',ticks=20)
        self.call('eae-seed-pattern',slot=9,ticks=4);self.inventory();self.player_slot();self.slot(self.semantic('ENCODED_PATTERN')[0]['index']);self.call('wait',ticks=30);self.check('Pattern installed in real provider',any(s['item']=='ae2:processing_pattern'and not s['player']for s in self.reply['server']['slots']));self.call('close');self.cmd(f'tp @a {x+.5} {y} {z+4.5} 180 17',12);self.call('use-block-ray',ticks=25)
        self.check('Native PAT receives populated provider rows',self.win()['extendedValues'].get('rows',0)>0);self.capture('pattern-access-populated')
        self.check('Native PAT search-mode control retained',any(b['type'].endswith('$SearchButton')for b in self.buttons()))
        self.text('no_such_pattern');self.check('Native PAT search filters provider rows',self.win()['extendedValues'].get('rows',0)==0)
        self.text('');self.check('Native PAT search clear restores rows',self.win()['extendedValues'].get('rows',0)>0)
        self.inventory();w=self.win();point=next(v for k,v in w['wirelessGeometry'].items()if k.startswith('slot/PatternSlot/'))
        self.tap(w['nativeBounds']['x']+point[0]+7,w['nativeBounds']['y']+point[1]+7)
        self.check('Native PAT provider-ID withdrawal',self.reply['server']['cursor']['item']=='ae2:processing_pattern')
        self.player_slot(10)
    def wireless(self):
        self.reset('ae2:wireless_access_point');x,y,z=self.pos
        # The access point connects through its rear face; power each adjacent
        # face so the fixture remains independent of version-default orientation.
        for dx,dy,dz in [(0,-1,0),(0,1,0),(1,0,0),(-1,0,0),(0,0,1),(0,0,-1)]:self.cmd(f'setblock {x+dx} {y+dy} {z+dz} ae2:creative_energy_cell',5)
        self.call('wait',ticks=35)
        item='extendedae:wireless_ex_pat[ae2:stored_energy=1000000.0d,ae2:wireless_link_target={dimension:"minecraft:overworld",pos:[I;'+f'{x},{y},{z}' +']}]'
        self.cmd('item replace entity @a weapon.mainhand with '+item,10);self.call('use-item',ticks=25);self.capture('wireless-pattern-access');self.check('Wireless native PAT retained',self.win()['nativeScreen'].endswith('.GuiWirelessExPAT'))
        self.check('Wireless PAT connects to powered native access point','connected=true'in self.reply['server']['nativeValues'].get('link',''))
    def matrix(self):
        self.reset('extendedae:assembler_matrix_frame');x,y,z=self.pos
        self.cmd(f'fill {x} {y} {z} {x+3} {y+2} {z+2} extendedae:assembler_matrix_frame')
        for a,b in [((1,0,1),(2,0,1)),((1,2,1),(2,2,1)),((1,1,0),(2,1,0)),((1,1,2),(2,1,2)),((0,1,1),(0,1,1)),((3,1,1),(3,1,1))]:
            self.cmd(f'fill {x+a[0]} {y+a[1]} {z+a[2]} {x+b[0]} {y+b[1]} {z+b[2]} extendedae:assembler_matrix_wall')
        self.cmd(f'setblock {x+1} {y+1} {z+1} extendedae:assembler_matrix_pattern');self.cmd(f'setblock {x+2} {y+1} {z+1} extendedae:assembler_matrix_crafter');self.cmd(f'setblock {x} {y-1} {z} ae2:creative_energy_cell',60)
        self.call('eae-seed-pattern',matrix=[x+1,y+1,z+1],ticks=30);self.call('open',ticks=30)
        self.check('Assembler Matrix receives populated native rows',self.win()['extendedValues'].get('rows',0)>0);self.capture('assembler-matrix-populated')
        self.inventory();w=self.win();s=next(s for s in w['slots']if s['item']=='ae2:processing_pattern'and not s['player']);self.tap(w['nativeBounds']['x']+s['x']+7,w['nativeBounds']['y']+s['y']+7)
        self.check('Assembler Matrix native withdrawal reaches cursor',self.reply['server']['cursor']['item']=='ae2:processing_pattern');self.player_slot(10)
    def geometry(self):
        for record in self.records:
            s=record['state'];w=next(w for w in s['allWindows']if'.extendedae.'in w.get('nativeScreen',''));bounds=w['bounds'];top=w.get('nativeBounds',{}).get('y',bounds['y']);bottom=bounds['y']+bounds['height']
            visible={v['index']for v in w.get('slotVisibility',[])if v['visible']}
            slots=[v for v in w['slots']if v['index']in visible and not v['player']and 0<=v['x']<bounds['width']-18]
            self.check(record['name']+' complete main-slot bottom rims',all(top+slot['y']+17<=bottom-3 for slot in slots))
    def base_ae2(self):
        self.reset('ae2:drive');self.call('open',ticks=12)
        w=next(w for w in self.state['allWindows']if w.get('nativeScreen','').endswith('.DriveScreen'))
        self.check('Existing AE2 Drive still uses native AEWindow',w['definition'].endswith('.AEWindow'));self.call('capture',capture='existing-ae2-drive',ticks=3)

if __name__=='__main__':
    a=Checks(sys.argv[1],sys.argv[2]);getattr(a,sys.argv[3])()
