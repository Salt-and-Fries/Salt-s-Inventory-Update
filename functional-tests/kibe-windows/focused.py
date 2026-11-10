"""Focused Kibe window checks in the real production client; not a full suite."""
from pathlib import Path
import argparse,json,sys
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send


class Check:
    def __init__(self,run,out=None):
        self.run=run;self.out=out or ROOT/'output/kibe-implemented';self.out.mkdir(parents=True,exist_ok=True)
        self.journal=self.out/('journal-'+run.name+'.json')
        self.results=json.loads(self.journal.read_text(encoding='utf-8'))if self.journal.exists()else dict(scope='Focused Kibe menus only',run=str(run),checks=[],captures=[])
        self.state={}
    def save(self):self.journal.write_text(json.dumps(self.results,indent=2),encoding='utf-8')
    def call(self,kind,**args):self.state=send(self.run,kind,**args)['client'];return self.state
    def cmd(self,command):return self.call('command',command=command,ticks=4)
    def passed(self,id,condition,detail=None):
        self.results['checks']=[x for x in self.results['checks']if x['id']!=id]+[dict(id=id,passed=bool(condition),detail=detail)]
        self.save();assert condition,(id,detail);print('PASS',id,flush=True)
    def window(self,path):return next(w for w in self.state.get('allWindows',[])if w.get('menuId')=='kibe:'+path)
    def point(self,path,x,y):
        w=self.window(path);b=w['bounds'];p=w['composition']
        for r in p['regions']:
            s=r['source']
            if s['x']<=x<s['x']+s['width']and s['y']<=y<s['y']+s['height']:
                return dict(x=b['x']+7+p['padding']+r['x']+x-s['x'],y=b['y']+26+p['padding']+r['y']+y-s['y'])
        raise AssertionError((path,x,y))
    def tap(self,p,button=0):
        self.call('move',**p,ticks=1);self.call('click',**p,button=button,ticks=1);self.call('release',**p,button=button,ticks=5)
    def slot(self,path,index,button=0):
        s=next(s for s in self.window(path)['slots']if s['index']==index);self.tap(self.point(path,s['x']+8,s['y']+8),button)
    def inventory(self,index=9):
        if not any(w['kind']=='INVENTORY'for w in self.state.get('workspaceWindows',[])):self.call('inventory',ticks=4)
        inv=next(w for w in self.state['workspaceWindows']if w['kind']=='INVENTORY')
        cell=next(c for c in inv['inventoryCells']if c['inventorySlot']==index)
        self.tap(dict(x=cell['x'],y=cell['y']))
    def insert(self,path,index,item,count=1):
        self.cmd(f'item replace entity @a inventory.0 with {item} {count}');self.inventory();self.slot(path,index)
    def snap(self,name='state'):
        self.call('kibe-state',ticks=3);d=json.loads((self.run/'kibe-state.json').read_text());(self.out/(name+'-server.json')).write_text(json.dumps(d,indent=2),encoding='utf-8');return d
    def menu(self,d,suffix):return next(m for m in d['menus']if m['class'].endswith(suffix))
    def open(self,path):
        self.call('close');self.cmd('gamemode survival @a');self.call('kibe-food',food=20)
        if path in ('cooler_item','entangled_bag','pocket_trash_can'):
            self.call('fixture',block='minecraft:air',ticks=5)
            self.call('held',item='kibe:'+('cooler'if path=='cooler_item'else path),ticks=4);self.call('use-item',ticks=12)
        else:
            self.cmd('kill @e[type=minecraft:item]')
            self.call('fixture',block='kibe:'+path,ticks=5);self.cmd('kill @e[type=minecraft:item]');self.call('open',ticks=12)
    def capture(self,id,path):
        if not any(w['kind']=='INVENTORY'for w in self.state.get('workspaceWindows',[])):self.call('inventory',ticks=4)
        self.call('move',x=2,y=2,ticks=2)
        name='kibe-'+id.replace('_','-');self.call('capture',capture=name,ticks=4)
        w=self.window(path);(self.out/(id+'-client.json')).write_text(json.dumps(self.state,indent=2),encoding='utf-8')
        entry=dict(id=id,menu=path,screenshot=str(self.run/'screenshots'/(name+'.png')),bounds=w['bounds'],composition=w['composition'])
        self.results['captures']=[x for x in self.results['captures']if x['id']!=id]+[entry];self.save()
    def geometry(self):
        manifest=json.loads((ROOT/'output/kibe-window-design/manifest.json').read_text(encoding='utf-8'))
        source=json.loads((ROOT/'tmp/pdfs/kibe/audit-cases.json').read_text(encoding='utf-8'))
        for c in manifest['cases']:
            id=c['id'];path='cooler_item'if id=='pr-kibe-cooler'else id.removeprefix('item-').removeprefix('kibe-').replace('-','_')
            self.open(path);w=self.window(path)
            self.passed(id+'-dedicated-template',w['templateClass']=='com.salts_inventory_update.compat.kibe.KibeClient$Template'and not w['layoutError'],w['templateClass'])
            self.passed(id+'-approved-size',[w['bounds']['width'],w['bounds']['height']]==c['size'],w['bounds'])
            actual=w['composition']['regions'][0];expected=c['source_rect']
            self.passed(id+'-complete-operative-panel',actual['source']==dict(x=expected[0],y=expected[1],width=expected[2]-expected[0],height=expected[3]-expected[1]))
            positions=[]
            for s in w['nativeSlots']:
                b=s['bounds'];matches=[]
                for r in w['composition']['regions']:
                    a=r['source']
                    if a['x']<=b['x']and a['y']<=b['y']and b['x']+b['width']<=a['x']+a['width']and b['y']+b['height']<=a['y']+a['height']:matches.append(r)
                if s['player']:assert not matches
                else:
                    assert len(matches)==1
                    r=matches[0];a=r['source'];positions.append(dict(index=s['menuSlotId'],destination=[7+r['x']+b['x']-a['x'],26+r['y']+b['y']-a['y'],b['width'],b['height']]))
            self.passed(id+'-complete-slot-mapping',positions==[dict(index=s['index'],destination=s['destination'])for s in c['slots']],positions)
            self.passed(id+'-no-embedded-player-inventory',all(not s['player']or not (actual['source']['y']<=s['bounds']['y']<actual['source']['y']+actual['source']['height'])for s in w['nativeSlots']))
            self.capture(id,path)
    def transfers(self):
        cases=[('cooler','CoolerScreenHandler',0,'minecraft:bread',3),('cooler_item','CoolerBlockItemScreenHandler',0,'minecraft:bread',3),
               ('big_torch','BigTorchScreenHandler',0,'minecraft:torch',8),('breaker','BreakerScreenHandler',0,'minecraft:stone',7),
               ('placer','PlacerScreenHandler',4,'minecraft:stone',7),('drawbridge','DrawbridgeScreenHandler',0,'minecraft:stone',7),
               ('wither_builder','WitherBuilderScreenHandler',0,'minecraft:soul_sand',4),('vacuum_hopper','VacuumHopperScreenHandler',2,'minecraft:stone',7),
               ('entangled_chest','EntangledChestScreenHandler',4,'minecraft:diamond',7),('entangled_bag','EntangledBagScreenHandler',5,'minecraft:emerald',7)]
        for path,cls,index,item,count in cases:
            self.open(path);self.insert(path,index,item,count)
            m=self.menu(self.snap(path+'-insert'),cls);s=next(s for s in m['slots']if s['index']==index)
            self.passed(path+'-insert-native-slot',s['item']==item and s['count']==count,s)
            self.slot(path,index);self.inventory(10)
            d=self.snap(path+'-extract');s=next(s for s in d['inventory']if s['index']==10)
            self.passed(path+'-extract-native-slot',s['item']==item and s['count']==count,s)
        for path,cls in [('trash_can','TrashCanScreenHandler'),('pocket_trash_can','PocketTrashCanScreenHandler')]:
            self.open(path);self.insert(path,0,'minecraft:cobblestone',7);d=self.snap(path+'-discard')
            self.passed(path+'-native-disposal',all(s['count']==0 for s in self.menu(d,cls)['slots'])and not any(s['item']=='minecraft:cobblestone'and s['count']for s in d['inventory']))

    def rejects(self):
        for path,index,item in [('cooler',0,'minecraft:diamond'),('cooler_item',0,'minecraft:diamond'),('big_torch',0,'minecraft:stone'),('placer',0,'minecraft:bread'),('drawbridge',0,'minecraft:bread'),('wither_builder',0,'minecraft:stone'),('wither_builder',2,'minecraft:stone'),('cobblestone_generator_mk1',26,'minecraft:bread')]:
            self.open(path);self.insert(path,index,item,1)
            s=next(s for s in self.window(path)['slots']if s['index']==index)
            self.passed(path+f'-slot-{index}-native-rejection',s['count']==0,s)
            self.inventory();d=self.snap(path+f'-rejected-{index}')
            self.passed(path+f'-slot-{index}-rejected-item-returned',any(s['index']==9 and s['item']==item and s['count']==1 for s in d['inventory']))

    def quick(self):
        for path,cls,item in [('breaker','BreakerScreenHandler','minecraft:stone'),('big_torch','BigTorchScreenHandler','minecraft:torch'),('cooler_item','CoolerBlockItemScreenHandler','minecraft:bread')]:
            self.open(path);self.call('inventory',ticks=4);self.cmd(f'item replace entity @a inventory.0 with {item} 7')
            title={'breaker':'Breaker','big_torch':'Big Torch','cooler_item':'Cooler'}[path]
            self.call('kibe-window-action',title=title,action='FOCUS',ticks=4)
            inv=next(w for w in self.state['workspaceWindows']if w['kind']=='INVENTORY')
            cell=next(c for c in inv['inventoryCells']if c['inventorySlot']==9)
            self.call('kibe-quick',title='Inventory',x=cell['x'],y=cell['y'],ticks=8)
            d=self.snap(path+'-quick-in');m=self.menu(d,cls)
            self.passed(path+'-native-quick-in',sum(s['count']for s in m['slots']if s['item']==item)==7,m['slots'])
            index=next(s['index']for s in m['slots']if s['count'])
            s=next(s for s in self.window(path)['slots']if s['index']==index)
            self.call('kibe-quick',title=title,**self.point(path,s['x']+8,s['y']+8),ticks=8)
            d=self.snap(path+'-quick-out')
            self.passed(path+'-native-quick-out',sum(s['count']for s in d['inventory']if s['item']==item)==7 and not any(s['count']for s in self.menu(d,cls)['slots']))

    def cooler(self):
        # Peaceful restores hunger independently of the Cooler; disable that
        # world mechanic before measuring the native feeding guard.
        self.cmd('difficulty normal')
        self.open('cooler_item');self.insert('cooler_item',0,'minecraft:bread',8)
        self.call('kibe-food',food=10,ticks=30);d=self.snap('cooler-open-not-feeding')
        self.passed('cooler-pauses-feeding-while-menu-open',d['food']==10 and self.menu(d,'CoolerBlockItemScreenHandler')['slots'][0]['count']==8,d)
        self.call('snapshot-server',ticks=3)
        selected=json.loads((self.run/'server-state.json').read_text())['selectedLocked']
        # Use the source lease exposed to normal SIU input. Raw vanilla
        # InventoryMenu packets bypass the desktop gesture route and are not
        # a simulation of clicking a protected SIU cell.
        d=self.snap('cooler-held-item-lock')
        self.passed('cooler-original-held-slot-protected',selected and any(s['index']==0 and s['item']=='kibe:cooler'for s in d['inventory']))
        self.call('kibe-window-action',title='Cooler',action='USER_CLOSE',ticks=20);d=self.snap('cooler-closed-feeding')
        held=next(s for s in d['inventory']if s['item']=='kibe:cooler')
        self.passed('cooler-resumes-native-feeding-after-close',d['food']>10 and 'Count:6b' in held['nbt'],d)
        self.call('use-item',ticks=10);m=self.menu(self.snap('cooler-reopen-food'),'CoolerBlockItemScreenHandler')
        self.passed('cooler-remaining-food-persists',m['slots'][0]['count']==6,m)
        self.call('close');self.call('fixture',block='minecraft:air',ticks=4)
        self.cmd('item replace entity @a weapon.offhand with kibe:cooler');self.call('kibe-offhand-use',ticks=12)
        self.passed('cooler-offhand-open',self.window('cooler_item')['templateClass'].endswith('KibeClient$Template'))
        self.insert('cooler_item',0,'minecraft:carrot',4);self.call('close')
        d=self.snap('cooler-offhand-persistence')
        self.passed('cooler-offhand-tag-preserved',any(s['index']==40 and 'minecraft:carrot' in s['nbt']for s in d['inventory']))

    def xp(self):
        self.open('vacuum_hopper');self.call('kibe-fill-xp',amount=8000,ticks=8)
        self.insert('vacuum_hopper',1,'minecraft:glass_bottle',2);self.call('wait',ticks=60)
        d=self.snap('vacuum-bottle-conversion');m=self.menu(d,'VacuumHopperScreenHandler')
        self.passed('vacuum-native-bottle-conversion',m['slots'][0]['item']=='minecraft:experience_bottle'and m['slots'][0]['count']==2,m)
        self.capture('vacuum-xp-conversion','vacuum_hopper');self.slot('vacuum_hopper',0);self.inventory(11)
        self.insert('vacuum_hopper',1,'minecraft:bucket',1);self.call('wait',ticks=85)
        m=self.menu(self.snap('vacuum-bucket-conversion'),'VacuumHopperScreenHandler')
        self.passed('vacuum-native-bucket-conversion',m['slots'][0]['item']=='kibe:liquid_xp_bucket'and m['slots'][0]['count']==1,m)
        self.cmd('summon minecraft:item 8.5 66 8.5 {Item:{id:"minecraft:diamond",Count:3b}}');self.call('wait',ticks=50)
        m=self.menu(self.snap('vacuum-item-collection'),'VacuumHopperScreenHandler')
        self.passed('vacuum-native-item-collection',any(s['index']>=2 and s['item']=='minecraft:diamond'and s['count']==3 for s in m['slots']),m)
        self.call('move',**self.point('vacuum_hopper',106,32),ticks=5);self.call('capture',capture='kibe-vacuum-tank-tooltip',ticks=3)

    def runes(self):
        self.open('entangled_chest');self.call('kibe-runes',ticks=8)
        self.insert('entangled_chest',3,'minecraft:diamond',9);self.capture('entangled-colored-runes','entangled_chest')
        self.call('held',item='kibe:entangled_bag',ticks=5);self.call('open',shift=True,ticks=8);self.call('use-item',shift=False,ticks=10)
        d=self.snap('entangled-bag-linked');bag=self.menu(d,'EntangledBagScreenHandler')
        self.passed('bag-native-link-copies-colored-channel','rune1:"orange"' in bag['tag']and 'rune8:"light_gray"' in bag['tag'],bag['tag'])
        self.passed('bag-shares-real-chest-inventory',any(s['index']==3 and s['item']=='minecraft:diamond'and s['count']==9 for s in bag['slots']),bag['slots'])
        self.slot('entangled_bag',3);self.inventory(12)
        d=self.snap('entangled-shared-after-withdraw')
        linked=[m for m in d['menus']if m['class'].endswith(('EntangledBagScreenHandler','EntangledChestScreenHandler'))]
        self.passed('bag-chest-shared-withdraw-sync',len(linked)==2 and all(next(s for s in m['slots']if s['index']==3)['count']==0 for m in linked),linked)
        self.capture('entangled-bag-linked-colored','entangled_bag')

    def generators(self):
        for block in ['cobblestone','basalt']:
            path=block+'_generator_mk1';self.open(path);self.call('wait',ticks=120)
            m=self.menu(self.snap(path+'-production'),'BlockGeneratorScreenHandler')
            self.passed(block+'-native-generation',sum(s['count']for s in m['slots']if s['item']=='minecraft:'+block)>0,m['slots'])
            self.capture(path+'-working',path)

    def machines(self):
        self.open('big_torch');self.insert('big_torch',0,'minecraft:torch',64)
        self.capture('big-torch-supplied','big_torch')
        self.call('move',**self.point('big_torch',40,25),ticks=4);self.call('capture',capture='kibe-big-torch-tooltip',ticks=3)
        m=self.menu(self.snap('big-torch-state'),'BigTorchScreenHandler')
        self.passed('big-torch-native-supply-and-state',m['slots'][0]['count']==64 and 'minecraft:torch' in m['ownerNbt'],m)
        self.open('drawbridge');self.insert('drawbridge',1,'minecraft:stone',5)
        self.inventory();d=self.snap('drawbridge-cover');m=self.menu(d,'DrawbridgeScreenHandler')
        self.passed('drawbridge-native-cover-one-item-limit',m['slots'][1]['count']==1 and m['slots'][1]['item']=='minecraft:stone'and any(s['index']==9 and s['count']==4 for s in d['inventory']),d)
        self.capture('drawbridge-covered','drawbridge')
        self.open('wither_builder')
        for index in [0,1,3,5]:self.insert('wither_builder',index,'minecraft:soul_sand',1)
        for index in [2,4,6]:self.insert('wither_builder',index,'minecraft:wither_skeleton_skull',1)
        m=self.menu(self.snap('wither-ingredients'),'WitherBuilderScreenHandler')
        self.passed('wither-builder-all-seven-native-ingredient-slots',all(s['count']==1 and s['item']==('minecraft:wither_skeleton_skull'if s['index']in[2,4,6]else'minecraft:soul_sand')for s in m['slots']),m)
        self.capture('wither-builder-supplied','wither_builder')

    def crafting(self):
        self.call('close');self.call('fixture',block='minecraft:air',ticks=4);self.call('held',item='kibe:pocket_crafting_table',ticks=4);self.call('use-item',ticks=10)
        w=next(w for w in self.state['allWindows']if w['menuId']=='minecraft:crafting')
        self.passed('pocket-crafting-existing-vanilla-integration',not w.get('templateClass','').endswith('KibeClient$Template')and w['menuId']=='minecraft:crafting',w.get('templateClass','vanilla Core window'))
        self.call('inventory',ticks=3);self.cmd('item replace entity @a inventory.0 with minecraft:oak_log 1');self.inventory()
        self.call('kibe-slot',title='Pocket Crafting Table',slot=1,ticks=7)
        m=self.menu(self.snap('pocket-crafting-result'),'PocketCraftingTable$use$1$1')
        self.passed('pocket-crafting-native-result-update',m['slots'][0]['item']=='minecraft:oak_planks'and m['slots'][0]['count']==4,m)
        self.call('kibe-slot',title='Pocket Crafting Table',slot=0,ticks=5);self.inventory(10)
        d=self.snap('pocket-crafting-output')
        self.passed('pocket-crafting-result-can-be-taken',any(s['index']==10 and s['item']=='minecraft:oak_planks'and s['count']==4 for s in d['inventory']))
        self.cmd('item replace entity @a inventory.0 with minecraft:oak_log 1');self.inventory()
        self.call('kibe-slot',title='Pocket Crafting Table',slot=1,ticks=5)
        self.call('capture',capture='kibe-pocket-crafting-current',ticks=3)
        (self.out/'pocket-crafting-client.json').write_text(json.dumps(self.state,indent=2),encoding='utf-8')
        self.call('close',ticks=15);d=self.snap('pocket-crafting-close')
        self.passed('pocket-crafting-close-returns-unused-input',sum(s['count']for s in d['inventory']if s['item']=='minecraft:oak_log')==1,d['inventory'])

    def chrome(self):
        self.open('cooler');self.call('inventory',ticks=4)
        w=self.window('cooler');old=w['bounds'].copy()
        self.call('kibe-window-action',title='Cooler',action='UNLOCK',ticks=3)
        p=dict(x=old['x']+20,y=old['y']+9);self.call('move',**p,ticks=1);self.call('click',**p,button=0,ticks=1)
        self.call('drag',x=p['x']+35,y=p['y']+25,dx=35,dy=25,button=0,ticks=3);self.call('release',x=p['x']+35,y=p['y']+25,button=0,ticks=4)
        b=self.window('cooler')['bounds']
        self.passed('window-native-title-drag',b['x']==old['x']+35 and b['y']==old['y']+25,b)
        self.insert('cooler',0,'minecraft:bread',3);m=self.menu(self.snap('cooler-after-drag'),'CoolerScreenHandler')
        self.passed('window-slot-routing-after-drag',m['slots'][0]['count']==3)
        for action,field,value in [('PIN','pinMode','PINNED'),('UNPIN','pinMode','UNPINNED'),('LOCK','locked','true'),('UNLOCK','locked','false'),('FOCUS','focused','true')]:
            self.call('kibe-window-action',title='Cooler',action=action,ticks=4)
            v=next(w for w in self.state['workspaceWindows']if w['kind']=='CONTAINER')
            self.passed('window-'+action.lower()+'-management-action',v[field]==value,v)
        v=next(w for w in self.state['workspaceWindows']if w['kind']=='CONTAINER');self.tap(v['controls']['ELLIPSIS'])
        self.call('capture',capture='kibe-cooler-management-menu',ticks=3)
        self.call('key',key=256,ticks=4)
        if not self.state.get('allWindows'):self.call('inventory',ticks=3)
        self.call('kibe-window-action',title='Cooler',action='USER_CLOSE',ticks=8)
        self.passed('window-close-disposes-native-session',not self.snap('cooler-chrome-closed')['menus'])

    def native_screen(self):
        self.call('close');self.call('fixture',block='kibe:chunk_loader',ticks=5)
        self.cmd('setblock 8 65 8 kibe:chunk_loader[enabled=true]');self.call('open',ticks=12)
        self.passed('chunk-loader-keeps-original-screen',self.state['screen']=='io.github.lucaargolo.kibe.blocks.chunkloader.ChunkLoaderScreen',self.state['screen'])
        self.call('capture',capture='kibe-native-chunk-loader',ticks=3)


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--run',type=Path,required=True);p.add_argument('section',choices=['geometry','transfers','rejects','quick','cooler','xp','runes','generators','crafting','native_screen','chrome','machines']);a=p.parse_args()
    check=Check(a.run);getattr(check,a.section)()
