"""Powah affected-window acceptance only; this is not a full gameplay suite."""
from pathlib import Path
import argparse
import json
import sys

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send

# Menu type, visible native machine cells, approved body size.
CASES=[
    ('reactor','reactor',5,190,75),('energy_cable','cable',0,174,29),
    ('ender_cell','ender_cell',2,190,73),('ender_gate','ender_cell',2,190,73),
    ('energy_cell','energy_cell',2,190,50),('energy_discharger','discharger',7,204,75),
    ('energy_hopper','energy_hopper',0,190,50),('furnator','furnator',2,190,75),
    ('magmator','magmator',1,190,75),('player_transmitter','player_transmitter',1,203,50),
    ('solar_panel','solar',0,190,50),('thermo_generator','thermo',1,200,75),
]


class Check:
    def __init__(self,run):
        self.run=Path(run);self.out=self.run/'powah-checks';self.out.mkdir(exist_ok=True)
        self.path=self.out/'journal.json'
        self.results=json.loads(self.path.read_text())if self.path.exists()else dict(scope='Powah affected-menu checks only',checks=[],captures=[])
        self.state={}
    def call(self,kind,**args):
        self.state=send(self.run,kind,**args)['client'];return self.state
    def passed(self,name,value,detail=None):
        self.results['checks']=[r for r in self.results['checks']if r['id']!=name]
        self.results['checks'].append(dict(id=name,passed=bool(value),detail=detail));self.save()
        assert value,(name,detail)
        print('PASS',name,flush=True)
    def save(self):self.path.write_text(json.dumps(self.results,indent=2))
    def w(self,path):return next(w for w in self.state.get('allWindows',[])if w.get('menuId')=='powah:'+path)
    def point(self,path,x,y):
        w=self.w(path);b=w['bounds'];p=w['composition']
        for r in reversed(p['regions']):
            s=r['source']
            if s['x']<=x<s['x']+s['width']and s['y']<=y<s['y']+s['height']and not any(q['x']<=x<q['x']+q['width']and q['y']<=y<q['y']+q['height']for q in r['cutouts']):
                exterior=r.get('exterior',False)
                return dict(x=b['x']+(0 if exterior else 7+p['padding']-w['hostScrollX'])+r['x']+x-s['x'],
                            y=b['y']+(0 if exterior else 25+p['padding']-w['hostScrollY'])+r['y']+y-s['y'])
        raise AssertionError((path,x,y))
    def tap(self,p,button=0):
        self.call('move',**p,ticks=1);self.call('click',**p,button=button,ticks=1);self.call('release',**p,button=button,ticks=5)
    def cmd(self,s):self.call('command',command=s,ticks=3)
    def open(self,block,tier='basic',face='north'):
        self.call('close');self.cmd('gamemode survival @a')
        self.call('gui-scale',scale=2,ticks=3)
        if block=='ender_gate':
            # Native Gate survival requires a valid energy attachment. A cable
            # advertises that immediately, unlike a just-created cell adapter.
            self.call('powah-setup',block='powah:energy_cell_basic',ticks=10)
            self.cmd('setblock 8 65 7 powah:energy_cable_starter[waterlogged=false]')
            self.call('fixture',block='powah:'+block+'_'+tier,ticks=10)
        else:self.call('powah-setup',block='powah:'+block+'_'+tier,ticks=10)
        if block=='reactor':
            self.call('powah-place-reactor',ticks=260)
            self.call('held',item='minecraft:air',ticks=5)
        if block=='energy_cable':self.call('powah-open-face',face=face,ticks=12)
        else:self.call('open',ticks=12)
    def capture(self,name,path):
        capture='powah-'+name.replace('_','-')
        self.call('move',x=2,y=2,ticks=2);self.call('capture',capture=capture,ticks=3)
        state=self.state;w=self.w(path)
        (self.out/(name+'-client.json')).write_text(json.dumps(state,indent=2))
        entry=dict(id=name,menu=path,screenshot=str(self.run/'screenshots'/(capture+'.png')),bounds=w['bounds'],composition=w['composition'])
        self.results['captures']=[r for r in self.results['captures']if r['id']!=name]+[entry];self.save()
    def snapshot(self,name='state'):
        self.call('powah-state',ticks=2);d=json.loads((self.run/'powah-state.json').read_text());(self.out/(name+'-server.json')).write_text(json.dumps(d,indent=2));return d
    def machine(self,d,path):return next(r for r in d['menus']if r['class'].split('.')[-1].lower().startswith({'energy_cell':'energycell','ender_cell':'endercell','discharger':'discharger','cable':'cable','energy_hopper':'energyhopper','player_transmitter':'playertransmitter','solar':'solar','thermo':'thermo'}.get(path,path)))
    def native_state(self,path,name='state'):return self.machine(self.snapshot(name),path)['state']
    def slot(self,path,index,button=0):
        s=next(s for s in self.w(path)['slots']if s['index']==index);self.tap(self.point(path,s['x']+8,s['y']+8),button)
    def inventory(self,index=9):
        if not any(w['kind']=='INVENTORY'for w in self.state.get('workspaceWindows',[])):self.call('inventory',ticks=3)
        w=next(w for w in self.state['workspaceWindows']if w['kind']=='INVENTORY')
        c=next(c for c in w['inventoryCells']if c['inventorySlot']==index)
        self.tap(dict(x=c['x'],y=c['y']))
    def insert(self,path,index,item,count=1):
        self.cmd(f'item replace entity @a inventory.0 with {item} {count}');self.inventory();self.slot(path,index)
    def geometry(self,start=0):
        for block,path,count,width,height in CASES[start:]:
            tier='starter'if block in ['reactor','energy_cable']else'basic'
            self.open(block,tier);w=self.w(path)
            self.passed(block+'-reviewed-template',w['templateClass']=='com.salts_inventory_update.compat.powah.PowahClient$Template'and not w['layoutError'])
            self.passed(block+'-approved-size',w['bounds']['width']==width and w['bounds']['height']==height+32,w['bounds'])
            slots=[s for s in w['slots']if not s['player']and s['y']<500]
            self.passed(block+'-visible-native-slots',len(slots)==count)
            # All complete native 18px cells and native widgets have a mapped home.
            placed=[]
            for s in w['nativeSlots']:
                b=s['bounds'];mx=b['x']+b['width']//2;my=b['y']+b['height']//2
                if s['player']or b['y']>500:
                    try:self.point(path,mx,my)
                    except AssertionError:continue
                    raise AssertionError(('embedded-player-or-helper-cell',block,s))
                p=self.point(path,mx,my);placed.append(p)
            self.passed(block+'-native-slot-and-player-separation',len(placed)==count)
            n=w['nativeBounds'];widgets=[v for v in w['nativeWidgets']if v.get('visible',True)]
            for v in widgets:self.point(path,v['x']-n['x']+v['width']/2,v['y']-n['y']+v['height']/2)
            self.passed(block+'-all-native-controls-mapped',len(widgets)==(2 if path=='cable'else 20 if path=='ender_cell'and tier=='nitro'else 9 if path=='reactor'else 10 if path=='ender_cell'else 8),len(widgets))
            self.call('inventory',ticks=3);self.capture(block,path)
    def settings(self,block='energy_cell',path='energy_cell',tier='basic'):
        self.open(block,tier)
        self.call('inventory',ticks=3)
        offsets=[(186,22),(186,10),(186,16),(192,22),(180,16),(192,16)]
        for i,(x,y)in enumerate(offsets):
            before=self.native_state(path,f'{block}-face-{i}-before')['faces']
            self.tap(self.point(path,x,y));after=self.native_state(path,f'{block}-face-{i}-after')['faces']
            self.passed(block+f'-face-{i}-server-action',after[i]!=before[i]and all(after[j]==before[j]for j in range(6)if j!=i))
        before=self.native_state(path,block+'-all-before')['faces'];self.tap(self.point(path,192,10));after=self.native_state(path,block+'-all-after')['faces']
        self.passed(block+'-all-faces-server-action',all(a!=b for a,b in zip(before,after)))
        modes=set()
        for i in range(3):
            before=self.native_state(path,block+'-redstone-before')['getRedstoneMode'];modes.add(before)
            self.tap(self.point(path,182,38));after=self.native_state(path,block+'-redstone-after')['getRedstoneMode']
            self.passed(block+f'-redstone-{i}-server-action',after!=before)
        self.passed(block+'-all-redstone-modes',len(modes)==3)
        self.capture(block+'-configured',path)
    def cable(self):
        self.open('energy_cable','starter')
        d=self.machine(self.snapshot('cable-north'),'cable');self.passed('cable-original-clicked-face',d['face']=='north',d.get('face'))
        i=2;before=d['state']['faces'];self.tap(self.point('cable',14,14));after=self.native_state('cable','cable-after')['faces']
        self.passed('cable-mode-only-affects-clicked-face',after[i]!=before[i]and all(after[j]==before[j]for j in range(6)if j!=i))
        before=self.native_state('cable')['getRedstoneMode'];self.tap(self.point('cable',139,14));after=self.native_state('cable')['getRedstoneMode']
        self.passed('cable-redstone-native-action',before!=after);self.capture('energy_cable-functional','cable')
        self.call('close');self.call('powah-setup',block='powah:energy_cable_starter',ticks=8)
        self.cmd('setblock 9 65 8 powah:energy_cell_basic[waterlogged=false]')
        self.call('powah-open-face',face='east',ticks=12)
        d=self.machine(self.snapshot('cable-east'),'cable');self.passed('cable-nondefault-clicked-face',d['face']=='east',d.get('face'))
        before=d['state']['faces'];self.tap(self.point('cable',14,14));after=self.native_state('cable','cable-east-action')['faces']
        self.passed('cable-east-mode-only-affects-east',after[5]!=before[5]and all(after[j]==before[j]for j in range(5)))
        self.capture('energy_cable-east','cable')
    def channels(self):
        for block in ['ender_cell','ender_gate']:
            self.open(block,'nitro');path='ender_cell'
            for channel in [1,5,11,0]:
                self.tap(self.point(path,11+14*channel,61));value=self.native_state(path,block+'-channel-'+str(channel))['channel']
                self.passed(block+f'-native-channel-{channel}',int(value)==channel,value)
            self.capture(block+'-nitro-channels',path)
    def transfer(self,block,path,index,item,count=1):
        modern=not self.run.name.startswith('1.20.1-');binding='bound_player'if modern else'bound_player_id'
        self.open(block,'starter'if block=='reactor'else'basic');self.call('inventory',ticks=3)
        if item=='powah:binding_card':
            self.insert(path,index,item)
            invalid=self.machine(self.snapshot('transmitter-unbound-card'),path)
            self.passed('transmitter-rejects-unbound-card','minecraft:air'in invalid['slots'][0]['item'])
            self.inventory()
            # Binding is the publisher's ordinary item-use action, outside a GUI.
            self.call('close')
            self.call('held',item=item,ticks=3);self.call('use-item',ticks=5)
            self.cmd('item replace entity @a inventory.0 from entity @p hotbar.0')
            bound=self.snapshot('transmitter-native-bound-item')
            self.passed('transmitter-native-item-binding',binding in bound['inventory'][9]['item'])
            self.call('held',item='minecraft:air',ticks=3);self.call('open',ticks=12)
            self.inventory();self.slot(path,index)
        elif block=='energy_discharger':
            # Forge's native discharge adapter requires a charged input.
            charged='[powah:energy_stored=10000L]'if modern else'{powah_tile_data:{energy_stored_main_energy:10000L}}'
            self.insert(path,index,item+charged,count)
        else:self.insert(path,index,item,count)
        d=self.machine(self.snapshot(block+'-insert'),path)
        cell=next(s for s in d['slots']if s['index']==index)
        self.passed(block+'-native-item-insertion',item in cell['item'],cell)
        self.slot(path,index);self.inventory(10);d=self.snapshot(block+'-withdraw')
        self.passed(block+'-item-retrieval-to-player',item in d['inventory'][10]['item'],d['inventory'][10])
        if item=='powah:binding_card':self.passed('transmitter-card-binding-preserved',binding in d['inventory'][10]['item'])
        self.capture(block+'-transfer',path)
    def generation(self):
        self.open('furnator');self.call('inventory',ticks=3)
        before=self.native_state('furnator')['stored'];self.insert('furnator',1,'minecraft:coal',8);self.call('wait',ticks=25)
        after=self.native_state('furnator','furnator-generating')['stored'];self.passed('furnator-native-generation-active',int(after)>int(before))
        self.capture('furnator-active','furnator')
    def gauge(self):
        self.open('energy_cell');before=self.native_state('energy_cell')['stored']
        self.call('powah-set-energy',ticks=12);after=self.native_state('energy_cell','energy-cell-filled')['stored']
        self.passed('energy-cell-active-fill-fixture',int(after)>int(before));self.capture('energy_cell-filled','energy_cell')
    def reactor(self):
        self.open('reactor','starter');d=self.native_state('reactor','reactor-before')
        self.passed('reactor-formed-multiblock',d.get('isBuilt')=='true',d.get('isBuilt'))
        before=d['isGenModeOn'];self.tap(self.point('reactor',-7,14));after=self.native_state('reactor','reactor-mode')['isGenModeOn']
        self.passed('reactor-native-generation-toggle',before!=after)
        self.call('inventory',ticks=3);self.insert('reactor',1,'powah:uraninite',8);self.insert('reactor',2,'minecraft:coal',8);self.insert('reactor',3,'minecraft:redstone',8);self.insert('reactor',4,'minecraft:ice',8)
        self.call('wait',ticks=20);self.capture('reactor-active','reactor')
    def transfers(self):
        for args in [('energy_cell','energy_cell',0,'powah:battery_starter',1),('energy_discharger','discharger',0,'powah:battery_starter',1),('magmator','magmator',0,'powah:battery_starter',1),('thermo_generator','thermo',0,'powah:battery_starter',1),('player_transmitter','player_transmitter',0,'powah:binding_card',1)]:self.transfer(*args)
    def fluids(self):
        for block,path,bucket,fluid in [('magmator','magmator','lava_bucket','lava'),('thermo_generator','thermo','water_bucket','water')]:
            self.open(block);self.call('close');self.call('held',item='minecraft:'+bucket,ticks=3)
            self.call('open',ticks=6);self.call('held',item='minecraft:air',ticks=3);self.call('open',ticks=12)
            d=self.native_state(path,block+'-fluid');self.passed(block+'-native-bucket-interaction','minecraft:'+fluid in d['nbt'],d['nbt'])
            self.capture(block+'-fluid-active',path)
            point=self.point(path,164,68);self.call('move',**point,ticks=3)
            self.call('capture',capture='powah-'+block.replace('_','-')+'-tank-tooltip',ticks=2)
    def controls(self):
        path='energy_cell';self.open('energy_cell');self.call('inventory',ticks=3)
        def workspace():return next(w for w in self.state['workspaceWindows']if w['kind']=='CONTAINER')
        old=workspace();self.tap(old['controls']['PIN']);self.passed('siu-powah-window-pins',workspace()['pinMode']!=old['pinMode'])
        if workspace()['locked']!='false':self.tap(workspace()['controls']['LOCK'])
        self.passed('siu-powah-window-unlocks',workspace()['locked']=='false')
        b=self.w(path)['bounds'];p=dict(x=b['x']+25,y=b['y']+10);q=dict(x=p['x']-55,y=p['y']+30)
        self.call('click',**p,button=0,ticks=1);self.call('drag',**q,dx=-55,dy=30,button=0,ticks=2);self.call('release',**q,button=0,ticks=4)
        self.passed('siu-powah-window-moves',self.w(path)['bounds']!=b)
        before=self.native_state(path,'moved-before')['faces'];self.tap(self.point(path,186,22));after=self.native_state(path,'moved-after')['faces']
        self.passed('siu-powah-attached-control-follows-and-works',before[0]!=after[0]);self.capture('moved-window-with-inventory',path)
        self.tap(workspace()['controls']['CLOSE']);self.passed('siu-powah-closes-independently',not self.state.get('allWindows')and any(w['kind']=='INVENTORY'for w in self.state['workspaceWindows']))
        self.call('open',ticks=12);self.passed('siu-powah-window-reopens',bool(self.w(path)))
    def quick(self):
        self.open('ender_cell');self.call('inventory',ticks=3);path='ender_cell'
        self.cmd('item replace entity @a inventory.0 with powah:energy_cell_starter')
        workspace=next(w for w in self.state['workspaceWindows']if w['kind']=='CONTAINER')
        self.tap(workspace['controls']['FOCUS'])
        inventory=next(w for w in self.state['workspaceWindows']if w['kind']=='INVENTORY')
        cell=next(c for c in inventory['inventoryCells']if c['inventorySlot']==9)
        before=self.native_state(path,'ender-capacity-before')['capacity']
        self.call('powah-quick-player',x=cell['x'],y=cell['y'],ticks=12)
        d=self.snapshot('ender-capacity-upgraded');after=self.machine(d,path)['state']['capacity']
        self.passed('ender-cell-native-capacity-upgrade-quick-move',int(after)>int(before),[before,after])
        self.passed('ender-cell-capacity-upgrade-consumes-cell','powah:energy_cell_starter'not in d['inventory'][9]['item'])
        self.capture('ender_cell-capacity-upgraded',path)
    def scales(self):
        for block,path in [('energy_discharger','discharger'),('thermo_generator','thermo'),('ender_cell','ender_cell')]:
            self.open(block)
            for scale in [3,4]:
                self.call('gui-scale',scale=scale,ticks=5)
                w=self.w(path);n=w['nativeBounds']
                self.passed(f'{block}-scale-{scale}-layout',not w['layoutError'] and w['hostScrollX']==0 and w['hostScrollY']==0)
                widgets=[v for v in w['nativeWidgets']if v.get('visible',True)]
                for v in widgets:self.point(path,v['x']-n['x']+v['width']/2,v['y']-n['y']+v['height']/2)
                self.passed(f'{block}-scale-{scale}-controls',bool(widgets))
                self.call('move',x=1,y=1,ticks=2)
                self.call('capture',capture=f'powah-{block.replace("_","-")}-scale-{scale}',ticks=2)
        self.call('gui-scale',scale=2,ticks=5)
    def presentation(self):
        self.open('energy_cell');self.cmd('kill @e[type=minecraft:item]')
        self.cmd('time set noon');self.cmd('weather clear');self.call('inventory',ticks=3)
        workspace=next(w for w in self.state['workspaceWindows']if w['kind']=='CONTAINER')
        if workspace['locked']!='false':self.tap(workspace['controls']['LOCK'])
        b=self.w('energy_cell')['bounds'];dx=190-b['x'];dy=80-b['y']
        p=dict(x=b['x']+24,y=b['y']+10);q=dict(x=p['x']+dx,y=p['y']+dy)
        self.call('click',**p,button=0,ticks=1);self.call('drag',**q,dx=dx,dy=dy,button=0,ticks=2);self.call('release',**q,button=0,ticks=4)
        self.capture('moved-window-with-inventory','energy_cell')
    def fluid_final(self):
        self.fluids();self.reactor()
        self.call('close');self.call('held',item='minecraft:water_bucket',ticks=3)
        self.call('open',ticks=6);self.call('held',item='minecraft:air',ticks=3);self.call('open',ticks=12)
        d=self.native_state('reactor','reactor-fluid')
        self.passed('reactor-native-bucket-interaction','minecraft:water'in d['nbt'],d['nbt'])
        self.capture('reactor-fluid-active','reactor')
        point=self.point('reactor',164,68);self.call('move',**point,ticks=3)
        self.call('capture',capture='powah-reactor-tank-tooltip',ticks=2)
        self.presentation()
    def full_tank_move(self):
        self.open('magmator');path='magmator'
        self.tap(self.point(path,182,38))
        self.passed('magmator-stops-for-full-tank-fixture',self.native_state(path)['getRedstoneMode']=='ON')
        self.call('close')
        for _ in range(4):
            self.call('held',item='minecraft:lava_bucket',ticks=2);self.call('open',ticks=3);self.call('held',item='minecraft:air',ticks=2)
        self.call('open',ticks=12);self.capture('magmator-full-tank',path)
        self.snapshot('magmator-full-tank')
        workspace=next(w for w in self.state['workspaceWindows']if w['kind']=='CONTAINER')
        if workspace['locked']!='false':self.tap(workspace['controls']['LOCK'])
        b=self.w(path)['bounds'];dx=120-b['x'];dy=85-b['y'];p=dict(x=b['x']+24,y=b['y']+10);q=dict(x=p['x']+dx,y=p['y']+dy)
        self.call('click',**p,button=0,ticks=1);self.call('drag',**q,dx=dx,dy=dy,button=0,ticks=2);self.call('release',**q,button=0,ticks=4)
        self.passed('powah-full-fluid-window-moves',self.w(path)['bounds']!=b)
        self.capture('magmator-full-tank-moved',path);self.presentation()
    def final_build(self):
        self.settings();self.cable();self.channels();self.reactor();self.scales();self.presentation()
    def behavior(self):
        self.settings();self.cable();self.channels()
        self.transfers();self.generation();self.gauge();self.reactor();self.fluids();self.controls()
    def active(self):
        self.generation();self.gauge();self.reactor();self.fluids();self.controls()
    def run_all(self):
        self.geometry();self.behavior()


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--phase',default='all');p.add_argument('--start',type=int,default=0);a=p.parse_args();check=Check(a.run)
    if a.phase=='all':check.run_all()
    elif a.phase=='geometry':check.geometry(a.start)
    else:getattr(check,a.phase)()
