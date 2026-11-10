"""Focused live Enchanted menu checks, not the complete gameplay suite."""
from pathlib import Path
import json
import sys

sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'project-everything-ten'))
from send import send


class Check:
    def __init__(self, output):
        self.out=Path(output);self.state={}
        self.folder=self.out/'enchanted-checks';self.folder.mkdir(exist_ok=True)
        self.journal=self.folder/'journal.json'
        self.results=json.loads(self.journal.read_text()) if self.journal.exists() else {'scope':'Enchanted affected menus only','checks':[]}

    def call(self, kind, **args):
        r=send(self.out,kind,**args);self.state=r.get('client',{});return r

    def cmd(self, command, ticks=4):self.call('command',command=command,ticks=ticks)

    def passed(self, name, condition):
        self.results['checks'].append({'id':name,'passed':bool(condition)})
        self.journal.write_text(json.dumps(self.results,indent=2));assert condition,name
        print('PASS',name,flush=True)

    def window(self, menu):
        w=next(w for w in self.state.get('allWindows',[]) if w.get('menuId')=='enchanted:'+menu)
        assert w['template']=='com.salts_inventory_update.compat.enchanted.EnchantedWindows$Template',w
        return w

    def point(self, menu, x, y):
        w=self.window(menu);b=w['bounds'];p=w['composition']
        for r in reversed(p['regions']):
            s=r['source']
            if s['x']<=x<s['x']+s['width'] and s['y']<=y<s['y']+s['height']:
                return dict(x=b['x']+7+p['padding']+r['x']+x-s['x'],y=b['y']+26+p['padding']+r['y']+y-s['y'])
        raise AssertionError((menu,x,y))

    def tap(self, point, button=0):
        self.call('click',**point,button=button,ticks=1);self.call('release',**point,button=button,ticks=3)

    def slot(self, menu, index):
        s=next(s for s in self.window(menu)['slots'] if s['index']==index)
        self.tap(self.point(menu,s['x']+8,s['y']+8))

    def inventory(self, index=9):
        w=next(w for w in self.state['workspaceWindows'] if w['kind']=='INVENTORY')
        self.tap(next(dict(x=c['x'],y=c['y']) for c in w['inventoryCells'] if c['inventorySlot']==index))

    def insert(self, menu, slot, item, count=1):
        self.cmd(f'item replace entity @a inventory.0 with {item} {count}')
        self.inventory();self.slot(menu,slot)

    def snap(self, name):
        self.call('snapshot-server',ticks=1);d=json.loads((self.out/'server-state.json').read_text())
        (self.folder/(name+'.json')).write_text(json.dumps(dict(client=self.state,server=d),indent=2));return d

    def capture(self,name):
        name=name.replace('_','-')
        self.call('move',x=639,y=449,ticks=1);self.call('capture',capture=name,ticks=4)
        (self.folder/(name+'-state.json')).write_text(json.dumps(self.state,indent=2))

    def open(self, menu):
        self.call('close');self.call('fixture',block='enchanted:'+menu)
        self.cmd('gamemode survival @a');self.cmd('tp @a 8.5 65 12.5 180 18')
        self.call('open-ray',ticks=12);self.window(menu);self.call('inventory',ticks=5)
        self.capture('enchanted-'+menu+'-empty')

    def drag(self, menu):
        w=self.window(menu);b=w['bounds'];info=next(v for v in self.state['workspaceWindows'] if v['kind']=='CONTAINER' and v['bounds']==b)
        if info['locked']=='true':
            if 'LOCK' in info['controls']:self.tap(info['controls']['LOCK'])
            else:
                at=info['controls']['ELLIPSIS'];self.tap(at);self.tap(dict(x=at['x']-14,y=at['y']+17))
        w=self.window(menu);b=w['bounds'];x=b['x']+20;y=b['y']+7
        self.call('click',x=x,y=y,button=0,ticks=1)
        self.call('drag',x=x-16,y=y+12,dx=-16,dy=12,button=0,ticks=1)
        self.call('release',x=x-16,y=y+12,button=0,ticks=2)
        a=self.window(menu)['bounds'];self.passed(menu+'-title-drag',a['x']==b['x']-16 and a['y']==b['y']+12)

    def close_reopen(self, menu):
        w=self.window(menu);info=next(v for v in self.state['workspaceWindows'] if v['kind']=='CONTAINER' and v['bounds']==w['bounds'])
        self.tap(info['controls']['CLOSE'])
        self.passed(menu+'-close-control',not any(w.get('menuId')=='enchanted:'+menu for w in self.state.get('allWindows',[])))
        self.call('close');self.call('open-ray',ticks=12)
        self.passed(menu+'-reopens-native-menu',self.window(menu) is not None)

    def oven(self):
        self.open('witch_oven')
        self.passed('oven-five-slot-layout',len([s for s in self.window('witch_oven')['slots'] if not s['player']])==5)
        self.insert('witch_oven',1,'enchanted:clay_jar',8)
        self.insert('witch_oven',0,'minecraft:oak_log',2)
        self.insert('witch_oven',2,'minecraft:coal',2)
        d=self.snap('oven-processing');values=self.window('witch_oven')['nativeValues']
        self.passed('oven-native-fuel-and-progress-sync',values['getBurnProgress']>0 and values['getCookProgress']>0)
        self.capture('enchanted-oven-processing')
        self.call('wait',ticks=220);d=self.snap('oven-output')
        m=next(m for m in d['menus'] if m['class'].endswith('WitchOvenMenu'))
        self.passed('oven-produces-native-charcoal','minecraft:charcoal' in m['slots'][3]['item'])
        self.slot('witch_oven',3);self.inventory(10);d=self.snap('oven-retrieved')
        self.passed('oven-result-to-separate-inventory','minecraft:charcoal' in d['inventory'][10]['item'])
        self.drag('witch_oven');self.capture('enchanted-oven-final')
        self.close_reopen('witch_oven')

    def shelf(self):
        self.open('poppet_shelf')
        self.passed('shelf-four-spaced-cells',len([s for s in self.window('poppet_shelf')['slots'] if not s['player']])==4)
        # Blank/sturdy/infused dolls are crafting materials, not functional
        # PoppetItem instances. Keep the publisher's shelf restrictions.
        for i,item in enumerate(['fire_poppet','earth_poppet','magic_poppet','hunger_poppet']):self.insert('poppet_shelf',i,'enchanted:'+item)
        d=self.snap('shelf-filled');m=next(m for m in d['menus'] if m['class'].endswith('PoppetShelfMenu'))
        self.passed('shelf-all-four-native-slots-accept-poppets',all('poppet' in s['item'] for s in m['slots'][:4]))
        self.capture('enchanted-shelf-filled')
        self.slot('poppet_shelf',3);self.inventory(10);d=self.snap('shelf-withdraw')
        self.passed('shelf-fourth-slot-to-separate-inventory','hunger_poppet' in d['inventory'][10]['item'])
        self.drag('poppet_shelf');self.capture('enchanted-shelf-final');self.close_reopen('poppet_shelf')

    def altar(self):
        self.call('close');self.cmd('fill 8 65 7 10 65 8 enchanted:altar')
        self.call('fixture',block='enchanted:altar',ticks=24)
        self.cmd('tp @a 8.5 65 12.5 180 18');self.call('open-ray',ticks=12)
        self.passed('altar-slot-free-native-status-window',self.window('altar')['slots']==[])
        # The controller is the native P000 part at the back-left of the altar.
        # Supply power through its normal saved-state fields in this disposable
        # fixture; no production recipe, power code or packet handler is replaced.
        self.cmd('data merge block 8 65 7 {currentPower:1234.0d,maxPower:5000.0d,rechargeMultiplier:2.0d}',ticks=16)
        values=self.window('altar')['nativeValues']
        self.passed('altar-live-power-data-sync',values['getMaxPower']==5000 and values['getCurrentPower']>=1234 and values['getRechargeMultiplier']==2)
        self.capture('enchanted-altar-powered');self.drag('altar');self.close_reopen('altar')
        self.passed('altar-reopened-power-readout',self.window('altar')['nativeValues']['getMaxPower']==5000)
        self.capture('enchanted-altar-final')
        self.call('close')
        self.cmd('clone 8 65 7 10 65 8 3 65 7 replace move',ticks=25)
        self.cmd('data merge block 3 65 7 {currentPower:5000.0d,maxPower:5000.0d,rechargeMultiplier:2.0d}',ticks=4)

    def wheel(self):
        self.open('spinning_wheel')
        self.passed('wheel-four-native-slots',len([s for s in self.window('spinning_wheel')['slots'] if not s['player']])==4)
        for i,item in enumerate(['enchanted:poppet','enchanted:hint_of_rebirth','minecraft:shield']):self.insert('spinning_wheel',i,item)
        self.call('wait',ticks=24)
        self.passed('wheel-native-processing-and-data-sync',self.window('spinning_wheel')['nativeValues']['getSpinProgress']>0)
        self.capture('enchanted-wheel-processing')
        self.call('wait',ticks=310);d=self.snap('wheel-output')
        m=next(m for m in d['menus'] if m['class'].endswith('SpinningWheelMenu'))
        self.passed('wheel-produces-sturdy-poppet','enchanted:sturdy_poppet' in m['slots'][3]['item'])
        self.slot('spinning_wheel',3);self.inventory(10);d=self.snap('wheel-retrieved')
        self.passed('wheel-output-to-separate-inventory','sturdy_poppet' in d['inventory'][10]['item'])
        self.drag('spinning_wheel');self.capture('enchanted-wheel-final');self.close_reopen('spinning_wheel')

    def distillery(self):
        self.open('distillery')
        self.passed('distillery-seven-native-slots',len([s for s in self.window('distillery')['slots'] if not s['player']])==7)
        self.insert('distillery',0,'enchanted:clay_jar',3)
        self.insert('distillery',1,'enchanted:breath_of_the_goddess')
        self.insert('distillery',2,'minecraft:lapis_lazuli')
        self.call('wait',ticks=24)
        self.passed('distillery-native-processing-and-data-sync',self.window('distillery')['nativeValues']['getCookProgress']>0)
        self.capture('enchanted-distillery-processing')
        self.call('wait',ticks=310);d=self.snap('distillery-output')
        m=next(m for m in d['menus'] if m['class'].endswith('DistilleryMenu'))
        expected=['tear_of_the_goddess','whiff_of_magic','slime_ball','foul_fume']
        self.passed('distillery-produces-all-four-native-outputs',all(item in m['slots'][i+3]['item'] for i,item in enumerate(expected)))
        self.capture('enchanted-distillery-four-outputs')
        for i in range(4):self.slot('distillery',i+3);self.inventory(10+i)
        d=self.snap('distillery-retrieved')
        self.passed('distillery-all-four-outputs-to-inventory',all(item in d['inventory'][10+i]['item'] for i,item in enumerate(expected)))
        self.drag('distillery');self.capture('enchanted-distillery-final');self.close_reopen('distillery')


if __name__=='__main__':
    c=Check(sys.argv[1]);getattr(c,sys.argv[2])()
