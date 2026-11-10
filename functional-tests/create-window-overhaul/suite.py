"""Focused Create window feature checks against a live disposable world.

Only fixture contents/world state use server setup commands. Tested actions use
the original screen input methods through SIU's committed pointer mapping.
Server snapshots, screenshots and a result journal are retained per assertion.
The journal does not claim complete coverage until every planned group is run.
"""
from pathlib import Path
import argparse,json,re,sys,traceback
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'project-everything-ten'))
from send import send

class Suite:
    def __init__(self,output):
        self.output=output;self.evidence=output/'features';self.evidence.mkdir(exist_ok=True)
        self.journal=self.evidence/'journal.json'
        self.results=json.loads(self.journal.read_text(encoding='utf-8')) if self.journal.exists() else {'scope':'Create affected-window features; development acceptance in progress','complete':False,'checks':[],'groups':{}}
        self.state={}
    def call(self,kind,**args):
        self.state=send(self.output,kind,**args)['client'];return self.state
    def save(self):self.journal.write_text(json.dumps(self.results,indent=2)+'\n',encoding='utf-8')
    def window(self,menu=None):
        windows=[w for w in self.state.get('allWindows',[]) if menu is None or w.get('menuId')==menu]
        assert len(windows)==1,('Expected one window',menu,[(w.get('menuId'),w.get('bounds')) for w in windows])
        w=windows[0];assert w.get('template')=='com.salts_inventory_update.compat.create.CreateWindows$Template',w.get('definition')
        return w
    def native_point(self,x,y,menu=None):
        w=self.window(menu);p=w['composition'];b=w['bounds']
        # Retained older runs use the padded frame. The native-frame cannon
        # declares a 15px title strip and no horizontal/body padding.
        native_cannon=w.get('menuId')=='create:schematicannon' and any(r['id']=='native-panel' and r['source']['y']==13 for r in p['regions'])
        native_frame=w.get('menuId') in {'create:toolbox','create:schematic_table','create:crafting_blueprint','create:filter','create:attribute_filter'} and not p.get('scrollable',True)
        declared_header=w.get('templateState',{}).get('headerHeight')
        inset_x,inset_y=(0,declared_header) if declared_header else (0,15) if native_cannon else (0,14) if native_frame else (8,24)
        for r in reversed(p['regions']):
            s=r['source']
            if not r['interactive'] or not(s['x']<=x<s['x']+s['width'] and s['y']<=y<s['y']+s['height']):continue
            if any(a['x']<=x<a['x']+a['width'] and a['y']<=y<a['y']+a['height'] for a in r.get('cutouts',[])):continue
            if r.get('exterior'):return {'x':b['x']+r['x']+x-s['x'],'y':b['y']+r['y']+y-s['y']}
            return {'x':b['x']+inset_x+p['padding']+r['x']+x-s['x']-w.get('hostScrollX',0),'y':b['y']+inset_y+p['padding']+r['y']+y-s['y']-w.get('hostScrollY',0)}
        raise AssertionError(('Native control is not mapped',x,y))
    def tap(self,point,button=0):
        self.call('click',**point,button=button,ticks=1);self.call('release',**point,button=button,ticks=2)
    def tap_native(self,x,y,menu=None):self.tap(self.native_point(x,y,menu))
    def snapshot(self,label,capture=True):
        self.call('snapshot-server',ticks=1)
        record={'client':self.state,'server':json.loads((self.output/'server-state.json').read_text(encoding='utf-8'))}
        path=self.evidence/(label+'.json');path.write_text(json.dumps(record,indent=2)+'\n',encoding='utf-8')
        if capture:self.call('capture',capture=label.replace('_','-'),ticks=1)
        return record
    def check(self,label,action):
        try:
            action();self.results['checks'].append({'id':label,'passed':True});print('PASS',label,flush=True)
        except Exception as e:
            self.results['checks'].append({'id':label,'passed':False,'error':str(e)});self.save();raise
        self.save()
    def fixture(self,block='minecraft:air'):
        self.call('close',ticks=1);self.call('fixture',block=block,ticks=2)
        self.call('command',command='tp @a 8.5 65 12.5 180 18',ticks=2)
    def open_item(self,item,shift=False):
        self.call('held',item=item,ticks=2);self.call('use-item',shift=shift,ticks=10);self.window()
    def open_block(self,block):
        self.fixture(block);self.call('open-ray',ticks=10);self.window()
    def player_cell(self,index=9):
        inventory=next(w for w in self.state['workspaceWindows'] if w['kind']=='INVENTORY')
        return next(c for c in inventory['inventoryCells'] if c['inventorySlot']==index)
    def prepare_reference(self,item='minecraft:stone',count=64):
        if not any(w['kind']=='INVENTORY' for w in self.state.get('workspaceWindows',[])):self.call('inventory',ticks=3)
        self.call('command',command=f'item replace entity @a inventory.0 with {item} {count}',ticks=3)
        return self.player_cell()
    def ghost(self,x,y,item='minecraft:stone',count=64):
        cell=self.prepare_reference(item,count);self.tap(cell);self.tap_native(x,y);self.tap(cell)
    def server_menu(self,label):
        rows=self.snapshot(label,capture=False)['server']['menus']
        return next(m for m in rows if m['class']==self.window()['menu'])
    def saved_item(self,label):
        data=self.snapshot(label,capture=False)['server'];return data['inventory'][data['selected']]['item']
    def controls(self):
        return next(w for w in self.state['workspaceWindows'] if w['kind']=='CONTAINER')['controls']
    def block_probe(self,label,x,y,z):
        self.call('reviewed-block-probe',x=x,y=y,z=z,ticks=1)
        data=json.loads((self.output/'reviewed-block-probe.json').read_text(encoding='utf-8'))
        (self.evidence/(label+'.block.json')).write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8');return data
    def stock_fixture(self):
        self.fixture('create:stock_ticker')
        for command in ['kill @e[type=minecraft:villager]',
            'kill @e[type=create:seat]',
            'fill 8 65 9 10 67 11 minecraft:air',
            'fill 9 65 7 12 68 12 minecraft:air',
            'setblock 8 66 6 minecraft:air',
            'setblock 8 65 6 minecraft:air',
            'setblock 8 65 5 minecraft:air',
            'setblock 8 65 5 minecraft:chest{Items:[{Slot:0b,id:"minecraft:stone",Count:64b},{Slot:1b,id:"minecraft:iron_ingot",Count:64b}]}',
            'setblock 8 65 6 create:packager[facing=south]',
            'setblock 8 66 6 create:stock_link[face=floor,facing=south]{Freq:[I;1,2,3,4]}',
            'data merge block 8 65 8 {Freq:[I;1,2,3,4]}',
            'setblock 9 65 8 create:white_seat',
            'summon create:seat 9.5 65 8.5 {Passengers:[{id:"minecraft:villager",NoAI:1b,Invulnerable:1b}]}',
            'tp @a 8.5 65 10.5 180 18']:
            self.call('command',command=command,ticks=3)
        self.call('wait',ticks=40)
    def stock_open(self):
        self.call('aim-entity',entity='minecraft:villager',ticks=2);self.call('use-entity-ray',entity='minecraft:villager',ticks=12);self.window('create:stock_keeper_request')
    def stock(self):
        self.stock_fixture();self.stock_open();self.snapshot('stock-filled')
        self.tap_native(100,26);self.call('text',text='stone',ticks=6);self.snapshot('stock-search')
        self.tap(self.native_point(100,26),button=1);self.call('key',key=257,ticks=2)
        self.tap_native(30,46);self.call('scroll',**self.native_point(30,46),amount=4,ticks=2)
        self.tap_native(55,46);self.tap(self.native_point(55,46),button=1)
        footer_y=self.window()['nativeBounds']['height']-33
        self.tap_native(50,footer_y);self.call('text',text='Foundry',ticks=2);self.snapshot('stock-basket')
        self.tap_native(180,footer_y);self.call('wait',ticks=60);self.snapshot('stock-sent')
        self.check('stock-order-withdrawal',lambda:self.assert_contains(self.block_probe('stock-source',8,65,5)['nbt'],'Count:59b'))
        self.check('stock-package-quantity',lambda:self.assert_contains(self.block_probe('stock-package',8,65,6)['nbt'],'Count:5b'))
        self.check('stock-package-address',lambda:self.assert_contains(self.block_probe('stock-package-address',8,65,6)['nbt'],'Address:"Foundry"'))
        self.tap_native(193,25);self.call('wait',ticks=3)
        self.check('stock-lock',lambda:self.assert_equal(self.window()['nativeValues']['isLocked'],True))
        self.snapshot('stock-lock')
        self.tap_native(193,25);self.call('wait',ticks=2)
        self.check('stock-unlock',lambda:self.assert_equal(self.window()['nativeValues']['isLocked'],False))
        self.tap(self.controls()['CLOSE'])
        self.check('stock-address-persistence',lambda:self.assert_contains(self.snapshot('stock-closed',capture=False)['server']['fixture'],'Foundry'))
        self.results['groups']['stock']='filled native network, search, basket increment/decrement, actual package creation, destination, lock and close persistence';self.save()
    def categories(self):
        self.stock_fixture();self.call('command',command='tp @a 8.5 65 12.5 180 18',ticks=2);self.call('open-ray',ticks=8)
        self.window('create:stock_keeper_category')
        for index,name in enumerate(['Metals','Stone']):
            self.tap_native(27,32+20*index)
            cell=self.prepare_reference('create:filter',1);self.tap(cell);self.tap_native(24,32)
            self.check('category-filter-owned-'+str(index),lambda:self.assert_contains(self.server_menu('category-filter-'+str(index))['carried'],'id:"minecraft:air"'))
            self.check('category-filter-transferred-'+str(index),lambda:self.assert_equal([(a['item'],a['count']) for a in self.window()['slots'] if not a['player']],[('create:filter',1)]))
            self.tap_native(100,32);self.call('key',key=269,ticks=0)
            for _ in range(28):self.call('key',key=259,ticks=0)
            self.call('text',text=name,ticks=2);self.tap_native(176,68)
            self.check('category-add-'+str(index),lambda index=index:self.assert_equal(len(self.window()['nativeValues']['schedule']),index+1))
        self.tap_native(184,40)
        self.check('category-move-down',lambda:self.assert_contains(self.window()['nativeValues']['schedule'][0],'Stone'))
        self.snapshot('categories-reordered');self.close_native(176,115)
        self.check('category-save',lambda:self.assert_contains(self.snapshot('categories-saved',capture=False)['server']['fixture'],'Metals'))
        self.call('open-ray',ticks=8);self.tap_native(168,32)
        self.check('category-delete',lambda:self.assert_equal(len(self.window()['nativeValues']['schedule']),1))
        self.check('category-refund',lambda:self.assert_equal(sum('id:"create:filter"' in a['item'] for a in self.snapshot('category-refund')['server']['inventory']),1))
        self.close_native(176,115)
        self.results['groups']['categories']='real filter ownership, name editing, add, move, delete, original refund and server save';self.save()
    def stock_category_hiding(self):
        self.call('command',command='gamemode survival @a',ticks=2);self.stock_fixture()
        self.call('command',command='data merge block 8 65 8 {Categories:[{Count:1b,id:"create:filter",tag:{Blacklist:0b,Items:{Items:[{Count:1b,Slot:0,id:"minecraft:stone"}],Size:18},RespectNBT:0b}}]}',ticks=10)
        self.stock_open();self.call('wait',ticks=60);self.snapshot('stock-category-header');self.tap_native(70,40);self.tap(self.controls()['CLOSE'])
        self.check('stock-hide-category-persist',lambda:self.assert_contains(self.snapshot('stock-category-hidden-save',capture=False)['server']['fixture'],'Indices:[I;0]'))
        self.stock_open();self.call('wait',ticks=10);self.snapshot('stock-category-hidden-reopen');self.tap_native(70,40);self.tap(self.controls()['CLOSE'])
        self.check('stock-show-category-persist',lambda:self.assert_contains(self.snapshot('stock-category-shown-save',capture=False)['server']['fixture'],'HiddenCategories:[]'))
        self.results['groups']['stock_category_hiding']='native category header hide/show and per-player authoritative persistence';self.save()
    def real_inventories(self):
        self.open_block('create:brown_toolbox');cell=self.prepare_reference();self.tap(cell)
        slot=next(a for a in self.window()['slots'] if not a['player'] and a['x']>=0)
        self.tap_native(slot['x']+8,slot['y']+8)
        self.check('toolbox-insert',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),64))
        self.tap(self.native_point(slot['x']+8,slot['y']+8),button=1);self.tap(cell)
        self.check('toolbox-split-withdraw',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),32))
        self.tap_native(120,78)
        self.check('toolbox-deposit-all',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),64))
        self.snapshot('toolbox-filled-functional');self.close_native(194,156)
        self.call('open-ray',ticks=7)
        self.check('toolbox-reopen',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),64))

        self.open_block('create:package_frogport');cell=self.prepare_reference('minecraft:stone',16);self.tap(cell);self.tap_native(35,17);self.tap(cell)
        self.check('frogport-reject-nonpackage',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),0))
        cell=self.prepare_reference('create:cardboard_package_10x12',1);self.tap(cell);self.tap_native(35,17)
        self.check('frogport-insert-package',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),1))
        self.tap_native(35,17);self.tap(cell)
        self.check('frogport-withdraw-package',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),0))
        # The name pencil is SIU-local; the original EditBox and original save
        # action remain native. Locate the mapped edit state from committed UI.
        w=self.window();self.tap({'x':w['bounds']['x']+130,'y':w['bounds']['y']+w.get('templateState',{}).get('headerHeight',17)-8})
        self.call('text',text='Foundry Mail',ticks=2);self.call('key',key=257,ticks=2);self.close_native(196,67)
        self.check('frogport-name-save',lambda:self.assert_contains(self.snapshot('frogport-renamed',capture=False)['server']['fixture'],'Foundry Mail'))

        for creative in [False,True]:
            name='creative-ammo' if creative else 'ammo';self.fixture();self.open_item('createbigcannons:'+('creative_' if creative else '')+'autocannon_ammo_container')
            cell=self.prepare_reference('minecraft:stone',16);self.tap(cell);self.tap_native(40,34);self.tap(cell)
            self.check(name+'-reject-invalid',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),0))
            cell=self.prepare_reference('createbigcannons:autocannon_cartridge',16);self.tap(cell);self.tap_native(40,34);self.tap(cell)
            count=1 if creative else 16
            self.check(name+'-native-capacity',lambda count=count:self.assert_equal(self.window()['slots'][0]['count'],count))
            for _ in range(10):self.call('scroll',**self.native_point(100,34),amount=1,ticks=0)
            self.call('wait',ticks=22)
            self.check(name+'-spacing',lambda:self.assert_equal(self.held_tag(name+'-spacing','TracerSpacing'),6))
            self.close_native(175 if creative else 155,68)
            self.check(name+'-persist',lambda:self.assert_contains(self.saved_item(name+'-saved'),'Ammo:'))
            self.call('use-item',shift=False,ticks=7)
            self.check(name+'-reopen',lambda count=count:self.assert_equal(self.window()['slots'][0]['count'],count))
            self.tap_native(40,34);self.tap(self.player_cell(10))
            self.check(name+'-withdraw',lambda:self.assert_equal(self.window()['slots'][0]['count'],0))
        self.results['groups']['real_inventories']='toolbox transfer/split/deposit, package restrictions and transfer, real regular/creative ammo capacity, rejection, spacing, reopen and withdrawal';self.save()
    def entity_windows(self):
        self.fixture('minecraft:stone');self.call('command',command='kill @e[type=create:crafting_blueprint]',ticks=1)
        self.call('held',item='create:crafting_blueprint',ticks=2);self.call('use-block-ray',ticks=3);self.call('held',item='minecraft:air',ticks=1)
        self.call('command',command='tp @a 8.5 65 10.5 180 18',ticks=2)
        self.call('aim-entity',entity='create:crafting_blueprint',ticks=1);self.call('use-entity-ray',entity='create:crafting_blueprint',ticks=6);self.window('create:crafting_blueprint')
        cell=self.prepare_reference('minecraft:oak_planks',64);self.tap(cell)
        for x,y in [(37,29),(55,29),(37,47),(55,47)]:self.tap_native(x,y)
        self.tap(cell);self.call('wait',ticks=4)
        self.check('blueprint-inferred-result',lambda:self.assert_equal(self.window()['slots'][45]['item'],'minecraft:crafting_table'))
        self.check('blueprint-ghost-preserves-planks',lambda:self.assert_contains(self.snapshot('blueprint-filled')['server']['inventory'][9]['item'],'Count:64b'))
        self.close_native(155,94);self.call('close',ticks=2);self.call('aim-entity',entity='create:crafting_blueprint',ticks=3);self.call('use-entity-ray',entity='create:crafting_blueprint',ticks=8)
        self.check('blueprint-world-craft',lambda:self.assert_equal(sum('id:"minecraft:crafting_table"' in a['item'] for a in self.snapshot('blueprint-crafted')['server']['inventory']),1))
        self.call('held',item='create:wrench',ticks=1);self.call('aim-entity',entity='create:crafting_blueprint',ticks=3);self.call('use-entity-ray',entity='create:crafting_blueprint',ticks=6)
        self.check('blueprint-wrench-reopens',lambda:self.window('create:crafting_blueprint'))
        self.tap_native(126,94)
        self.check('blueprint-clear',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),0));self.close_native(155,94)

        self.fixture();self.call('command',command='kill @e[type=create:crafting_blueprint]',ticks=2);self.call('command',command='kill @e[type=railways:conductor]',ticks=1);self.call('wait',ticks=25)
        self.call('command',command='summon railways:conductor 8.5 65 8.5 {NoAI:1b}',ticks=3)
        self.call('command',command='tp @a 8.5 65 10.5 180 18',ticks=1);self.call('held',item='create:brown_toolbox',ticks=1)
        self.call('aim-entity',entity='railways:conductor',ticks=1);self.call('use-entity-ray',entity='railways:conductor',ticks=3);self.call('held',item='minecraft:air',ticks=1)
        self.call('use-entity-ray',entity='railways:conductor',ticks=6);self.window('railways:mounted_toolbox')
        cell=self.prepare_reference('minecraft:stone',32);self.tap(cell);slot=next(a for a in self.window()['slots'] if not a['player'] and a['x']>=0)
        self.tap_native(slot['x']+8,slot['y']+8)
        self.check('mounted-toolbox-transfer',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),32))
        self.call('command',command='item replace entity @a inventory.0 with minecraft:stone 16',ticks=2);self.tap_native(120,78)
        self.check('mounted-toolbox-deposit',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),48))
        self.snapshot('mounted-toolbox-filled');self.close_native(194,156);self.call('close',ticks=2);self.call('aim-entity',entity='railways:conductor',ticks=3);self.call('use-entity-ray',entity='railways:conductor',ticks=6)
        self.check('mounted-toolbox-reopen',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),48))

        self.fixture()
        for command in ['kill @e[type=railways:conductor]','setblock 8 65 7 minecraft:stone','setblock 8 65 8 create:factory_gauge[face=wall,facing=south]{bottom_left:{Freq:[I;1,2,3,4],FilterAmount:1},bottom_right:{Freq:[I;1,2,3,4],FilterAmount:1},top_left:{Freq:[I;1,2,3,4],FilterAmount:1},top_right:{Freq:[I;1,2,3,4],FilterAmount:1}}','tp @a 8.5 65 12.5 175.9 16']:
            self.call('command',command=command,ticks=2)
        self.call('wait',ticks=30);self.call('open-ray',ticks=6);self.window('create:factory_panel_set_item');slot=next(a for a in self.window()['slots'] if not a['player']);self.ghost(slot['x']+8,slot['y']+8,item='minecraft:iron_ingot');self.close_native(160,73)
        self.check('gauge-reference-save',lambda:self.assert_contains(self.snapshot('gauge-saved')['server']['fixture'],'minecraft:iron_ingot'))
        self.results['groups']['entity_windows']='native blueprint references/result/crafting/clear, mounted inventory/deposit/reopen, gauge ghost filter save';self.save()
    def processing(self):
        from fixtures import prepare_schematics,deployed_schematic
        forge='-forge-' in self.output.name;owner='Dev' if forge else 'UIAudit'
        runtime=(Path('versions/1.20.1/forge/build/remaining-audit-native')/self.output.name.split('-integrated-',1)[1] if forge else Path(json.loads((self.output/'launch.json').read_text(encoding='utf-8'))['cwd']))
        prepare_schematics(runtime,owner)
        self.open_block('create:schematic_table');self.tap_native(215,32)
        cell=self.prepare_reference('create:empty_schematic',1);self.tap(cell);self.tap_native(29,67)
        self.tap_native(53,67);self.call('wait',ticks=60)
        self.check('schematic-table-real-upload',lambda:self.assert_equal(self.window()['slots'][1]['item'],'create:schematic'))
        self.snapshot('schematic-table-uploaded');self.tap_native(174,67);self.tap(self.player_cell(10))
        self.check('schematic-table-take-result',lambda:self.assert_equal(self.window()['slots'][1]['count'],0))
        self.open_block('create:schematicannon')
        self.call('command',command='setblock 7 65 8 minecraft:chest{Items:[{Slot:0b,id:"minecraft:stone",Count:64b}]}',ticks=2)
        self.call('command',command='fill 12 65 4 12 65 19 minecraft:air',ticks=1)
        menu=self.window()['menu']
        for slot,item,count,tag in [(0,'create:schematic',1,deployed_schematic(owner=owner)),(2,'minecraft:book',1,None),(4,'minecraft:gunpowder',4,None)]:
            args={'menu':menu,'slot':slot,'item':item,'count':count}
            if tag:args['snbt']=tag
            self.call('reviewed-session-seed',**args,ticks=5)
        self.tap_native(17,120)
        for mode in range(4):
            self.tap_native(42+18*mode,120)
            self.check('cannon-replacement-'+str(mode),lambda mode=mode:self.assert_contains(self.snapshot('cannon-policy-'+str(mode),capture=False)['server']['fixture'],'ReplaceMode:'+str(mode)))
        self.tap_native(120,120);self.tap_native(144,120)
        self.check('cannon-options',lambda:self.assert_contains(self.snapshot('cannon-options')['server']['fixture'],'SkipMissing:1b'))
        self.tap_native(78,120);self.tap_native(17,120)
        self.tap_native(84,94);self.call('wait',ticks=35)
        self.check('cannon-play',lambda:self.assert_contains(self.snapshot('cannon-running')['server']['fixture'],'State:"RUNNING"'))
        self.tap_native(102,94);self.call('wait',ticks=15)
        paused=self.snapshot('cannon-pause')['server']['fixture'];placed=re.search(r'AmountPlaced:(\d+)',paused).group(1)
        self.call('wait',ticks=20)
        self.check('cannon-pauses-progress',lambda:self.assert_contains(self.snapshot('cannon-paused-stable',capture=False)['server']['fixture'],'AmountPlaced:'+placed))
        self.tap_native(120,94)
        self.check('cannon-stop-reset',lambda:self.assert_contains(self.snapshot('cannon-stop',capture=False)['server']['fixture'],'AmountPlaced:0'))
        self.tap_native(84,94);self.call('wait',ticks=200)
        self.check('cannon-completes-placement',lambda:self.assert_contains(self.block_probe('cannon-last-block',12,65,19)['block'],'minecraft:stone'))
        self.check('cannon-consumes-materials',lambda:self.assert_contains(self.block_probe('cannon-chest-materials',7,65,8)['nbt'],'Count:48b'))
        self.check('cannon-checklist',lambda:self.assert_equal(self.window()['slots'][3]['item'],'minecraft:written_book'))
        self.snapshot('cannon-complete')
        self.results['groups']['processing']='real schematic upload/result, all replacement policies and settings, play/pause/stop/resume, world block placement, materials, fuel and checklist';self.save()
    def management(self):
        self.fixture();self.open_item('create:filter')
        def info():return next(w for w in self.state['workspaceWindows'] if w['kind']=='CONTAINER')
        if info()['locked']=='true':self.tap(self.controls()['LOCK'])
        self.check('window-unlock',lambda:self.assert_equal(info()['locked'],'false'))
        old=info()['bounds'];start={'x':old['x']+15,'y':old['y']+9};end={'x':start['x']+36,'y':start['y']+24}
        self.call('click',**start,button=0,ticks=1);self.call('drag',**end,button=0,dx=36,dy=24,ticks=1);self.call('release',**end,button=0,ticks=1)
        self.check('window-move',lambda:self.assert_equal(info()['bounds']['x'],old['x']+36))
        focused=info()['focused'];self.tap(self.controls()['FOCUS'])
        self.check('window-focus',lambda:self.assert_equal(info()['focused'],'false' if focused=='true' else 'true'))
        initial=info()['pinMode'];self.tap(self.controls()['PIN'])
        self.check('window-pin',lambda:self.assert_equal(info()['pinMode']!=initial,True))
        self.tap(self.controls()['PIN'])
        self.check('window-pin-cycle',lambda:self.assert_equal(info()['pinMode'],initial))
        if info()['pinMode']!='UNPINNED':self.tap(self.controls()['PIN'])
        self.tap(self.controls()['LOCK']);self.check('window-lock',lambda:self.assert_equal(info()['locked'],'true'))
        if not any(w['kind']=='INVENTORY' for w in self.state.get('workspaceWindows',[])):self.call('inventory',ticks=3)
        self.tap(self.controls()['LINK'])
        b=info()['bounds'];self.tap({'x':b['x']+20,'y':b['y']+9}) # clear a previously remembered group
        self.tap(self.controls()['LINK'])
        inv=next(w for w in self.state['workspaceWindows'] if w['kind']=='INVENTORY');self.tap({'x':inv['bounds']['x']+20,'y':inv['bounds']['y']+9})
        self.tap(self.controls()['LINK']);self.snapshot('window-linked-management')
        self.tap(self.controls()['CLOSE'])
        self.check('window-close-releases-session',lambda:self.assert_equal(self.state['sessions'],0))
        self.check('window-linked-close',lambda:self.assert_equal(len(self.state['workspaceWindows']),0))
        self.check('window-close-releases-held-source',lambda:self.assert_equal(self.snapshot('window-management-closed',capture=False)['server']['selectedLocked'],False))
        self.results['groups']['management']='custom title controls: unlock, move, focus, pin/unpin, lock, link selection, close and held-source release';self.save()
    def schedule_timeline(self):
        self.fixture();self.open_item('create:schedule');self.tap_native(50,32);self.tap_native(110,97);self.call('text',text='Harbor',ticks=1);self.tap_native(233,96)
        def schedule():return self.window()['nativeValues']['schedule']
        self.tap_native(211,82);self.check('schedule-duplicate',lambda:self.assert_equal(schedule().count('Id:"create:destination"'),2))
        self.tap_native(90,36);self.tap_native(110,97);self.call('key',key=269,ticks=0)
        for _ in range(6):self.call('key',key=259,ticks=0)
        self.call('text',text='Foundry',ticks=1);self.tap_native(233,96);self.tap_native(224,50)
        self.check('schedule-move-down',lambda:self.assert_equal(schedule().index('Harbor')<schedule().index('Foundry'),True))
        self.tap_native(224,110);self.check('schedule-move-up',lambda:self.assert_equal(schedule().index('Foundry')<schedule().index('Harbor'),True))
        self.tap_native(72,205);self.check('schedule-skip-progress',lambda:self.assert_contains(schedule(),'Progress:1'))
        self.tap_native(54,205);self.check('schedule-restart-progress',lambda:self.assert_equal('Progress:' in schedule(),False))
        self.tap_native(211,30);self.check('schedule-delete-entry',lambda:self.assert_equal(schedule().count('Id:"create:destination"'),1))
        self.tap_native(75,77);self.tap_native(233,96);self.check('schedule-and-append',lambda:self.assert_equal(schedule().count('Id:"create:delay"'),2))
        self.tap_native(122,60);self.tap_native(233,96);self.check('schedule-or-append',lambda:self.assert_equal(schedule().count('Id:"create:delay"'),3))
        self.tap_native(184,60);self.tap_native(233,96)
        self.check('schedule-third-alternative',lambda:self.assert_equal(schedule().count('Id:"create:delay"'),4))
        self.tap_native(205,76);self.call('wait',ticks=12);self.tap_native(140,60)
        self.check('schedule-horizontal-reaches-next-column',lambda:self.assert_equal(self.window()['templateState']['editing'],True))
        self.call('scroll',**self.native_point(90,97),amount=1,ticks=1);self.tap_native(233,96)
        self.check('schedule-offscreen-condition-edit',lambda:self.assert_contains(schedule(),'Value:6'))
        self.tap(self.native_point(90,80),button=1)
        self.check('schedule-remove-condition',lambda:self.assert_equal(schedule().count('Id:"create:delay"'),3))
        self.snapshot('schedule-logical-conditions')
        for _ in range(3):self.tap_native(211,82)
        self.check('schedule-four-entries',lambda:self.assert_equal(schedule().count('Id:"create:destination"'),4))
        for _ in range(20):self.call('scroll',**self.native_point(150,150),amount=-1,ticks=0)
        self.call('wait',ticks=15);self.snapshot('schedule-scrolled-to-end');self.tap_native(50,162)
        self.check('schedule-vertical-reaches-add',lambda:self.assert_equal(self.window()['templateState']['editing'],True))
        self.tap_native(20,96);self.close_native(223,205)
        self.check('schedule-complex-save',lambda:self.assert_equal(self.saved_item('schedule-complex-saved').count('Id:"create:destination"'),4))
        self.results['groups']['schedule_timeline']='duplicate, rename, up/down, skip/restart, delete, AND/OR, horizontal column navigation, right-click removal, vertical navigation and save';self.save()
    def autocomplete(self):
        self.fixture()
        for command in ['fill 12 64 0 12 64 25 minecraft:stone','fill 12 65 0 12 65 25 create:track[shape=zo,turn=false]','setblock 14 65 5 create:track_station{TargetTrack:{X:-2,Y:0,Z:0},TargetDirection:1b}']:
            self.call('command',command=command,ticks=5)
        self.call('wait',ticks=80)
        self.open_item('create:schedule');self.tap_native(50,32);self.tap_native(110,97);self.call('wait',ticks=8)
        self.snapshot('schedule-station-suggestions')
        popup=next((r for r in self.window()['composition']['regions'] if r['id']=='destination-suggestions'),None)
        self.check('schedule-autocomplete-complete-popup',lambda:self.assert_equal(popup is not None,True))
        b=popup['source'];self.tap_native(b['x']+4,b['y']+5);self.tap_native(233,96);self.close_native(223,205)
        self.check('schedule-autocomplete-pointer-selection',lambda:self.assert_contains(self.saved_item('schedule-suggestion-saved'),'Text:"Track Station"'))
        self.results['groups']['autocomplete']='real native rail graph/station, full original suggestions rectangle, pointer selection and saved native destination';self.save()
    def attribute_list(self):
        self.fixture();self.open_item('create:attribute_filter');self.ghost(24,35)
        for i in range(5):
            if i:self.call('scroll',**self.native_point(100,35),amount=-1,ticks=1)
            self.tap_native(191 if i%2==0 else 209,35)
        self.check('attribute-five-live-rules',lambda:self.assert_equal(self.window()['templateState']['ruleCount'],5))
        w=self.window();self.call('scroll',x=w['bounds']['x']+18,y=w['bounds']['y']+75,amount=-1,ticks=2)
        self.check('attribute-list-scroll',lambda:self.assert_equal(self.window()['templateState']['ruleScroll'],1))
        self.snapshot('attribute-five-rules');self.close_native(217,70);self.call('use-item',shift=False,ticks=6)
        self.check('attribute-five-rule-reopen',lambda:self.assert_equal(self.window()['templateState']['ruleCount'],5))
        self.tap_native(188,70);self.check('attribute-list-clear',lambda:self.assert_equal(self.window()['templateState']['ruleCount'],0));self.close_native(217,70)
        self.results['groups']['attribute_list']='five real attribute rules, inverted selections, scroll, persistence and clear';self.save()
    def connected_port(self):
        self.fixture('create:package_frogport')
        self.call('command',command='setblock 8 66 8 create:chain_conveyor',ticks=2)
        self.call('command',command='data merge block 8 65 8 {Target:{Type:"ChainConveyor",RelativePos:{X:0,Y:1,Z:0},ChainPos:0f}}',ticks=3)
        self.call('open-ray',ticks=8);self.snapshot('frogport-connected')
        self.tap_native(64,67);self.close_native(196,67)
        self.check('frogport-send-only',lambda:self.assert_contains(self.snapshot('frogport-send-only',capture=False)['server']['fixture'],'AcceptsPackages:0b'))
        self.call('close',ticks=2);self.call('command',command='tp @a 8.5 65 12.5 180 18',ticks=3);self.call('open-ray',ticks=7);self.tap_native(46,67);self.close_native(196,67)
        self.check('frogport-send-receive',lambda:self.assert_contains(self.snapshot('frogport-send-receive',capture=False)['server']['fixture'],'AcceptsPackages:1b'))
        self.results['groups']['connected_port']='both original connected-port modes and authoritative persistence';self.save()
    def responsive(self):
        self.fixture();self.open_item('create:schedule');self.tap_native(50,32);self.tap_native(110,97);self.call('text',text='Harbor',ticks=1);self.tap_native(233,96)
        for scale in [3,4]:
            self.call('gui-scale',scale=scale,width=1280,height=960,ticks=6);b=self.window()['bounds']
            self.check('window-fits-gui-'+str(scale),lambda b=b:self.assert_equal(b['x']>=0 and b['y']>=0 and b['x']+b['width']<=self.state['guiWidth'] and b['y']+b['height']<=self.state['guiHeight'],True))
            self.snapshot('schedule-gui-'+str(scale))
        b=self.window()['bounds'];self.tap({'x':b['x']+b['width']-12,'y':b['y']+b['height']-13})
        self.check('window-small-gui-scroll',lambda:self.assert_equal(self.window().get('hostScrollY',0)>0,True))
        self.close_native(223,205)
        self.check('window-small-gui-confirm',lambda:self.assert_contains(self.saved_item('schedule-small-gui-saved'),'Harbor'))
        self.call('gui-scale',scale=2,width=1280,height=900,ticks=3)
        self.results['groups']['responsive']='GUI scales 3 and 4, fitting, host viewport scroll and native confirmation/save';self.save()
    def close_native(self,x,y):
        self.tap_native(x,y);self.assert_equal(self.state['sessions'],0)
    def configurations(self):
        self.fixture();self.open_item('create:filter');self.ghost(31,33)
        self.check('filter-ghost-copy-preserves-stack',lambda:self.assert_equal(self.snapshot('filter-copy')['server']['inventory'][9]['item'],'{Count:64b,id:"minecraft:stone"}'))
        for name,x,tag in [('deny',27,'Blacklist:1b'),('allow',45,'Blacklist:0b'),('data',69,'RespectNBT:1b'),('ignore-data',87,'RespectNBT:0b')]:
            self.tap_native(x,84)
            self.close_native(190,84)
            self.check('filter-'+name,lambda tag=tag:self.assert_contains(self.saved_item('filter-'+name),tag))
            self.call('use-item',shift=False,ticks=7)
        self.close_native(190,84);self.call('use-item',shift=False,ticks=7)
        self.check('filter-reopen-reference',lambda:self.assert_equal(self.window()['slots'][36]['item'],'minecraft:stone'))
        self.tap_native(161,84)
        self.check('filter-clear',lambda:self.assert_equal(self.window()['slots'][36]['count'],0));self.close_native(190,84)

        self.fixture();self.open_item('create:attribute_filter');self.ghost(24,35)
        self.tap_native(191,35)
        self.check('attribute-add',lambda:self.assert_equal(self.window()['templateState']['ruleCount'],1))
        self.call('scroll',**self.native_point(100,35),amount=-1,ticks=2);self.tap_native(209,35)
        self.check('attribute-invert',lambda:self.assert_equal(self.window()['templateState']['ruleCount'],2))
        for x in [65,83,47]:self.tap_native(x,70)
        self.close_native(217,70)
        self.check('attribute-persist',lambda:self.assert_contains(self.saved_item('attribute-saved'),'Attribute'))
        self.call('use-item',shift=False,ticks=7)
        self.check('attribute-reopen',lambda:self.assert_equal(self.window()['templateState']['ruleCount'],2))
        self.tap_native(188,70)
        self.check('attribute-clear',lambda:self.assert_equal(self.window()['templateState']['ruleCount'],0));self.close_native(217,70)

        self.fixture();self.open_item('create:package_filter');self.tap_native(90,32)
        self.call('text',text='Foundry / East Warehouse',ticks=4)
        self.close_native(194,64)
        self.check('package-address',lambda:self.assert_contains(self.saved_item('package-address'),'Foundry / East Warehouse'))
        self.call('use-item',shift=False,ticks=7)
        self.tap_native(165,64)
        self.close_native(194,64)
        self.check('package-clear',lambda:self.assert_contains(self.saved_item('package-cleared'),'Address:""'))
        self.call('use-item',shift=False,ticks=7)
        self.call('text',text='Parcel Hub',ticks=3);self.close_native(194,64)
        self.call('use-item',shift=False,ticks=7);self.check('package-reopen',lambda:self.assert_contains(self.saved_item('package-reopened'),'Parcel Hub'))

        self.fixture();self.open_item('create:linked_controller',shift=True)
        slots=[a for a in self.window()['slots'] if not a['player']]
        cell=self.prepare_reference();self.tap(cell)
        for slot in slots:self.tap_native(slot['x']+8,slot['y']+8)
        self.tap(cell)
        self.check('controller-twelve-references',lambda:self.assert_equal(sum(a['item']=='minecraft:stone' for a in self.window()['slots'] if not a['player']),12))
        self.close_native(155,94)
        self.check('controller-persist',lambda:self.assert_contains(self.saved_item('controller-saved'),'Size:12'))
        self.call('use-item',shift=True,ticks=7);self.tap_native(126,94)
        self.check('controller-clear',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),0));self.close_native(155,94)

        self.open_block('create:redstone_requester');self.ghost(35,36)
        for _ in range(3):self.call('scroll',**self.native_point(35,36),amount=1,ticks=1)
        self.tap_native(90,73);self.call('text',text='Foundry',ticks=2);self.tap_native(21,104)
        self.close_native(211,104)
        self.check('requester-request-save',lambda:self.assert_contains(self.snapshot('requester-saved')['server']['fixture'],'Amount:4'))
        self.check('requester-address-save',lambda:self.assert_contains(self.snapshot('requester-address',capture=False)['server']['fixture'],'Foundry'))
        self.call('open-ray',ticks=7);self.tap_native(39,104);self.close_native(211,104)
        self.check('requester-full-policy',lambda:self.assert_contains(self.snapshot('requester-policy',capture=False)['server']['fixture'],'AllowPartial:0b'))
        self.results['groups']['configurations']='native ghost assignment, clear, modes, text and server persistence';self.save()
    def schedules(self):
        self.fixture();self.open_item('create:schedule');self.tap_native(50,32)
        self.tap_native(110,97);self.call('text',text='Harbor*',ticks=2);self.tap_native(233,96)
        self.close_native(223,205)
        self.check('schedule-destination-text',lambda:self.assert_contains(self.saved_item('schedule-destination'),'Text:"Harbor*"'))
        for index,name in enumerate(['package_delivery','package_retrieval','rename','throttle'],1):
            self.call('use-item',shift=False,ticks=5);self.tap_native(90,36)
            self.call('scroll',**self.native_point(100,73),amount=-1,ticks=2)
            self.snapshot('schedule-form-'+name)
            if name=='rename':self.tap_native(110,97);self.call('text',text='Foundry Express',ticks=2)
            if name=='throttle':self.call('scroll',**self.native_point(100,97),amount=1,ticks=2)
            self.tap_native(233,96);self.close_native(223,205)
            self.check('schedule-instruction-'+name,lambda name=name:self.assert_contains(self.saved_item('schedule-instruction-'+name),'Id:"create:'+name+'"'))
        # Return to the original destination and exercise every native condition
        # form, including the item-reference and two-frequency variants.
        self.call('use-item',shift=False,ticks=5);self.tap_native(90,36)
        for _ in range(4):self.call('scroll',**self.native_point(100,73),amount=1,ticks=1)
        self.tap_native(233,96)
        self.tap_native(53,60);self.tap_native(233,96)
        conditions=['delay','time_of_day','fluid_threshold','item_threshold','redstone_link','player_count','idle','unloaded','powered']
        for index,name in enumerate(conditions):
            self.tap_native(90,60)
            if index:self.call('scroll',**self.native_point(100,73),amount=-1,ticks=2)
            self.snapshot('schedule-condition-form-'+name)
            if name=='item_threshold':self.ghost(62,96)
            if name=='redstone_link':self.ghost(62,96);self.ghost(82,96,item='minecraft:redstone')
            self.tap_native(233,96);self.close_native(223,205)
            self.check('schedule-condition-'+name,lambda name=name:self.assert_contains(self.saved_item('schedule-condition-'+name),'Id:"create:'+name+'"'))
            self.call('use-item',shift=False,ticks=5)
        self.tap_native(30,205);self.close_native(223,205)
        self.check('schedule-loop-toggle',lambda:self.assert_contains(self.saved_item('schedule-loop-off'),'Cyclic:0b'))
        self.results['groups']['schedules']='all five native instruction and nine condition forms, text, ghost references, loop and saved reopening';self.save()
    def held_tag(self,label,key):
        server=self.snapshot(label,capture=False)['server'];data=server['inventory'][server['selected']]['item']
        found=re.search(r'\b'+re.escape(key)+r':(-?\d+)',data);assert found,data
        return int(found[1])
    def fuzes(self):
        for name,item,key,maximum in [('timed','timed_fuze','FuzeTimer',495),('proximity','proximity_fuze','DetonationDistance',32),('inertia','delayed_inertia_fuze','FuzeTimer',100),('impact','delayed_impact_fuze','FuzeTimer',100)]:
            self.fixture();self.open_item('createbigcannons:'+item);point=self.native_point(80,38);selected=self.state['selected']
            self.check(name+'-backing-protected',lambda:self.assert_equal(self.snapshot(name+'-source-lock',capture=False)['server']['selectedLocked'],True))
            for _ in range(110):self.call('scroll',**point,amount=-1,ticks=0)
            self.call('wait',ticks=25)
            self.check(name+'-minimum',lambda: self.assert_equal(self.held_tag(name+'-minimum',key),1))
            for _ in range(110):self.call('scroll',**point,amount=1,ticks=0)
            self.call('wait',ticks=25)
            self.check(name+'-maximum',lambda: self.assert_equal(self.held_tag(name+'-maximum',key),maximum))
            self.check(name+'-wheel-stays-in-window',lambda:self.assert_equal(self.state['selected'],selected))
            self.tap_native(155,68)
            self.check(name+'-confirm-closes',lambda:self.assert_equal(self.state.get('sessions'),0))
            self.check(name+'-saved-on-confirm',lambda:self.assert_equal(self.held_tag(name+'-saved',key),maximum))
            self.check(name+'-source-released',lambda:self.assert_equal(self.snapshot(name+'-released',capture=False)['server']['selectedLocked'],False))
            self.call('use-item',shift=False,ticks=10);self.window();self.snapshot(name+'-reopened')
            # Change once and use SIU's close control, exercising removed() save
            # ordering separately from the original native checkmark.
            self.call('scroll',**self.native_point(80,38),amount=-1,ticks=0)
            control=self.state['workspaceWindows'][-1]['controls']['CLOSE'];self.tap(control)
            expected=maximum-(5 if name=='timed' else 1)
            self.check(name+'-saved-on-siu-close',lambda:self.assert_equal(self.held_tag(name+'-siu-close',key),expected))
        self.results['groups']['fuzes']='passed native wheel endpoints, debounce, native confirm, reopen and SIU close; modifier shortcut remains separate';self.save()
    @staticmethod
    def assert_equal(actual,expected):assert actual==expected,(actual,expected)
    @staticmethod
    def assert_contains(actual,expected):assert expected in actual,(expected,actual)

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('output',type=Path);p.add_argument('--group',choices=['fuzes','configurations','schedules','stock','categories','stock_category_hiding','real_inventories','entity_windows','processing','management','schedule_timeline','autocomplete','attribute_list','connected_port','responsive'],required=True);a=p.parse_args();suite=Suite(a.output)
    try:getattr(suite,a.group)()
    except Exception:traceback.print_exc();sys.exit(1)
