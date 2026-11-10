"""Focused checks of the remaining native Create frames; no full gameplay suite."""
from pathlib import Path
import argparse,json
from suite import Suite

class NativeAll(Suite):
    def photo(self,label):self.call('move',x=3,y=3,ticks=2);return self.snapshot(label)
    def ports(self):
        for name,block in [('frogport','create:package_frogport'),('postbox','create:brown_postbox')]:
            self.open_block(block)
            self.check(name+'-native-header',lambda name=name:self.assert_equal(self.window()['templateState']['headerHeight'],17 if name=='frogport' else 24))
            self.check(name+'-compact-management',lambda:self.assert_equal('ELLIPSIS' in self.controls(),True))
            cell=self.prepare_reference('minecraft:stone',16);self.tap(cell);self.tap_native(35,17);self.tap(cell)
            self.check(name+'-reject-invalid',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),0))
            cell=self.prepare_reference('create:cardboard_package_10x12',1);self.tap(cell);self.tap_native(35,17)
            self.check(name+'-package-insert',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),1))
            self.tap_native(35,17);self.tap(cell)
            self.check(name+'-package-withdraw',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),0))
            b=self.window()['bounds'];h=self.window()['templateState']['headerHeight'];self.tap({'x':b['x']+130,'y':b['y']+h-8})
            self.check(name+'-native-header-edit-focus',lambda:self.assert_equal(self.window()['templateState']['renaming'],True))
            self.call('text',text='Foundry '+name+' dispatch',ticks=3);self.photo(name+'-header-editing')
            self.call('key',key=257,ticks=2)
            self.check(name+'-enter-finishes-edit',lambda:self.assert_equal(self.window()['templateState']['renaming'],False))
            self.close_native(196,67)
            self.check(name+'-native-address-save',lambda name=name:self.assert_contains(self.snapshot(name+'-address-save',capture=False)['server']['fixture'],'Foundry '+name+' dispatch'))
            self.call('open-ray',ticks=8);self.photo(name+'-renamed')
            self.tap(self.controls()['ELLIPSIS']);self.photo(name+'-management-menu');self.tap(self.controls()['ELLIPSIS']);self.tap(self.controls()['CLOSE'])
            self.check(name+'-siu-close',lambda:self.assert_equal(self.state['sessions'],0))
        self.results['groups']['ports']='both native headers, real package restrictions/transfers, original editable name and Enter/save, compact management and close';self.save()
    def remaining_configurations(self):
        self.fixture();self.open_item('create:package_filter');self.tap_native(90,32);self.call('text',text='Foundry / East Warehouse',ticks=3)
        self.close_native(194,64);self.check('package-filter-address-save',lambda:self.assert_contains(self.saved_item('package-filter-saved'),'Foundry / East Warehouse'))
        self.call('use-item',shift=False,ticks=7);self.photo('package-filter-address');self.tap_native(165,64);self.close_native(194,64)
        self.check('package-filter-clear',lambda:self.assert_contains(self.saved_item('package-filter-cleared'),'Address:""'))
        self.fixture();self.open_item('create:linked_controller',shift=True);cell=self.prepare_reference();self.tap(cell)
        for slot in [a for a in self.window()['slots'] if not a['player']]:self.tap_native(slot['x']+8,slot['y']+8)
        self.tap(cell)
        self.check('controller-twelve-references',lambda:self.assert_equal(sum(a['item']=='minecraft:stone' for a in self.window()['slots'] if not a['player']),12))
        self.photo('controller-filled');self.close_native(155,94)
        self.check('controller-native-save',lambda:self.assert_contains(self.saved_item('controller-saved'),'Size:12'))
        self.call('use-item',shift=True,ticks=7);self.tap_native(126,94)
        self.check('controller-native-clear',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),0));self.close_native(155,94)
        self.open_block('create:redstone_requester');self.ghost(35,36)
        for _ in range(3):self.call('scroll',**self.native_point(35,36),amount=1,ticks=1)
        self.tap_native(90,73);self.call('text',text='Foundry',ticks=2);self.tap_native(21,104);self.photo('requester-filled');self.close_native(211,104)
        self.check('requester-quantity-save',lambda:self.assert_contains(self.snapshot('requester-saved',capture=False)['server']['fixture'],'Amount:4'))
        self.check('requester-address-save',lambda:self.assert_contains(self.snapshot('requester-address',capture=False)['server']['fixture'],'Foundry'))
        self.call('open-ray',ticks=7);self.tap_native(39,104);self.close_native(211,104)
        self.check('requester-full-policy',lambda:self.assert_contains(self.snapshot('requester-policy',capture=False)['server']['fixture'],'AllowPartial:0b'))
        self.results['groups']['remaining_configurations']='package address/clear, all controller references/save/clear, requester quantities/address/policy';self.save()
    def ammo(self):
        for creative in [False,True]:
            name='creative-ammo' if creative else 'ammo';self.fixture();self.open_item('createbigcannons:'+('creative_' if creative else '')+'autocannon_ammo_container')
            cell=self.prepare_reference('minecraft:stone',16);self.tap(cell);self.tap_native(40,34);self.tap(cell)
            self.check(name+'-reject-invalid',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),0))
            cell=self.prepare_reference('createbigcannons:autocannon_cartridge',16);self.tap(cell);self.tap_native(40,34);self.tap(cell);count=1 if creative else 16
            self.check(name+'-native-capacity',lambda count=count:self.assert_equal(self.window()['slots'][0]['count'],count))
            for _ in range(7):self.call('scroll',**self.native_point(100,34),amount=1,ticks=0)
            self.call('wait',ticks=22)
            self.check(name+'-native-spacing',lambda name=name:self.assert_equal(self.held_tag(name+'-spacing','TracerSpacing'),6))
            self.photo(name+'-filled');self.close_native(175 if creative else 155,68)
            self.check(name+'-native-save',lambda name=name:self.assert_contains(self.saved_item(name+'-saved'),'Ammo:'))
            self.call('use-item',shift=False,ticks=7)
            self.check(name+'-reopen',lambda count=count:self.assert_equal(self.window()['slots'][0]['count'],count))
            self.tap_native(40,34);self.tap(self.player_cell(10))
            self.check(name+'-withdraw',lambda:self.assert_equal(self.window()['slots'][0]['count'],0))
        self.results['groups']['ammo']='regular/creative native capacities, rejection, insertion/withdrawal, tracer spacing and original footer save/reopen';self.save()
    def mounted_gauge(self):
        self.fixture();self.call('command',command='kill @e[type=create:crafting_blueprint]',ticks=2);self.call('command',command='kill @e[type=railways:conductor]',ticks=2)
        self.call('command',command='summon railways:conductor 8.5 65 8.5 {NoAI:1b}',ticks=3);self.call('command',command='tp @a 8.5 65 10.5 180 18',ticks=2)
        self.call('held',item='create:brown_toolbox',ticks=1);self.call('aim-entity',entity='railways:conductor',ticks=1);self.call('use-entity-ray',entity='railways:conductor',ticks=3)
        self.call('held',item='minecraft:air',ticks=1);self.call('use-entity-ray',entity='railways:conductor',ticks=6);self.window('railways:mounted_toolbox')
        cell=self.prepare_reference('minecraft:stone',32);self.tap(cell);slot=next(a for a in self.window()['slots'] if not a['player'] and a['x']>=0);self.tap_native(slot['x']+8,slot['y']+8)
        self.check('mounted-toolbox-transfer',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),32))
        self.call('command',command='item replace entity @a inventory.0 with minecraft:stone 16',ticks=2);self.tap_native(120,78)
        self.check('mounted-toolbox-deposit',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),48))
        self.photo('mounted-toolbox-filled');self.close_native(194,156);self.call('close',ticks=2);self.call('aim-entity',entity='railways:conductor',ticks=2);self.call('use-entity-ray',entity='railways:conductor',ticks=6)
        self.check('mounted-toolbox-reopen',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),48))
        self.fixture()
        for command in ['kill @e[type=railways:conductor]','setblock 8 65 7 minecraft:stone','setblock 8 65 8 create:factory_gauge[face=wall,facing=south]{bottom_left:{Freq:[I;1,2,3,4],FilterAmount:1},bottom_right:{Freq:[I;1,2,3,4],FilterAmount:1},top_left:{Freq:[I;1,2,3,4],FilterAmount:1},top_right:{Freq:[I;1,2,3,4],FilterAmount:1}}','tp @a 8.5 65 12.5 175.9 16']:self.call('command',command=command,ticks=3)
        self.call('wait',ticks=30);self.call('open-ray',ticks=8);self.window('create:factory_panel_set_item');slot=next(a for a in self.window()['slots'] if not a['player']);self.ghost(slot['x']+8,slot['y']+8,item='minecraft:iron_ingot')
        self.photo('gauge-filled');self.close_native(153,72)
        self.check('gauge-reference-save',lambda:self.assert_contains(self.snapshot('gauge-saved',capture=False)['server']['fixture'],'minecraft:iron_ingot'))
        self.results['groups']['mounted_gauge']='actual mounted inventory/deposit/native confirm/reopen and Factory Gauge reference save';self.save()
    def fuze_controls(self):
        for name,item,key in [('timed','timed_fuze','FuzeTimer'),('proximity','proximity_fuze','DetonationDistance'),('inertia','delayed_inertia_fuze','FuzeTimer'),('impact','delayed_impact_fuze','FuzeTimer')]:
            self.fixture();self.open_item('createbigcannons:'+item);before=self.held_tag(name+'-initial',key)
            self.check(name+'-held-source-protected',lambda:self.assert_equal(self.snapshot(name+'-source',capture=False)['server']['selectedLocked'],True))
            self.call('scroll',**self.native_point(80,38),amount=1,ticks=30);after=self.held_tag(name+'-changed',key)
            self.check(name+'-native-wheel',lambda before=before,after=after:self.assert_equal(after>before,True))
            self.photo(name+'-changed');self.close_native(155,68)
            self.check(name+'-native-confirm-save',lambda name=name,key=key,after=after:self.assert_equal(self.held_tag(name+'-saved',key),after))
            self.check(name+'-source-released',lambda:self.assert_equal(self.snapshot(name+'-released',capture=False)['server']['selectedLocked'],False))
            self.call('use-item',shift=False,ticks=7);self.call('scroll',**self.native_point(80,38),amount=1,ticks=30);changed=self.held_tag(name+'-second',key);self.tap(self.controls()['CLOSE'])
            self.check(name+'-siu-close-save',lambda name=name,key=key,changed=changed:self.assert_equal(self.held_tag(name+'-siu-saved',key),changed))
        self.results['groups']['fuze_controls']='one native wheel change, original confirm/reopen, SIU close and held-source ownership per fuze; range endpoints unchanged and not repeated';self.save()

def run(output,groups):
    s=NativeAll(output);s.results['scope']='Focused checks of remaining native Create-family inventory/container windows; not the complete gameplay suite'
    s.call('gui-scale',scale=2,width=1280,height=900,ticks=4);s.call('command',command='gamemode survival @a',ticks=2)
    for group in groups:
        getattr(s,group)();s.save()
    latest={a['id']:a for a in s.results['checks']};print(json.dumps({'checks':len(latest),'passed':all(a['passed'] for a in latest.values()),'groups':s.results['groups']}))

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('output',type=Path);p.add_argument('--groups',nargs='+',default=['ports','remaining_configurations','ammo','mounted_gauge','fuze_controls','categories','stock','stock_category_hiding','connected_port','schedules','schedule_timeline','autocomplete']);a=p.parse_args();run(a.output,a.groups)
