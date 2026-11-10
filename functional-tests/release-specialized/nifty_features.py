"""Native hoe/seed cells, full data, moved input and cart ownership lifetime."""
from pathlib import Path
import hashlib, json, sys

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'functional-tests/project-everything-ten'))
from send import send
from mowzie_features import item_count, stack

CASES = [('plow', 'PlowMenu', 'PlowScreen', 'minecraft:iron_hoe', 3),
         ('seed_drill', 'SeedDrillMenu', 'SeedDrillScreen', 'minecraft:wheat_seeds', 9)]


class Carts:
    def __init__(self, profile, mc):
        self.profile = Path(profile); self.mc = mc; self.state = {}; self.checks = []; self.completed = []
        self.out = self.profile / 'nifty-release'; self.out.mkdir()
    def act(self, kind='wait', **kw):
        self.state = send(self.profile, kind, **kw)['client']; return self.state
    def save(self, complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete, checks=self.checks, cases=self.completed,
            scope='Ordinary crouched cart entry, all three native hoe cells/all nine seed cells, full typed data, source-owned persistence, moved inputs and invalidation. Not agricultural world automation or cart driving.',
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()), indent=2)+'\n')
    def check(self, name, condition):
        self.checks.append(dict(name=self.kind+':'+name, passed=bool(condition))); self.save()
        assert condition, name
        print('PASS', self.kind, name, flush=True)
    def w(self):
        rows = [v for v in self.state['allWindows'] if v.get('menu') == 'net.jmb19905.niftycarts.container.'+self.menu]
        assert len(rows) == 1 and rows[0].get('nativeScreen') == 'net.jmb19905.niftycarts.client.screen.'+self.screen
        assert rows[0].get('niftyInputRegions')
        return rows[0]
    def tap(self, x, y):
        self.act('move',x=x,y=y,ticks=1); self.act('click',x=x,y=y,button=0,ticks=1)
        self.act('release',x=x,y=y,button=0,ticks=8)
    def slot(self, index):
        w = self.w(); cell = next(v for v in w['slots'] if v['index'] == index)
        x,y = cell['x']+8,cell['y']+8
        r = next(v for v in w['niftyInputRegions'] if v['interactive'] and v['source']['x']<=x<v['source']['x']+v['source']['width'] and v['source']['y']<=y<v['source']['y']+v['source']['height'])
        self.tap(r['x']+x-r['source']['x'],r['y']+y-r['source']['y'])
    def inv(self, index):
        values = [v for v in self.state['workspaceWindows'] if v['kind']=='INVENTORY']
        if not values:
            self.act('inventory',ticks=5); values = [v for v in self.state['workspaceWindows'] if v['kind']=='INVENTORY']
        assert len(values)==1
        cell = next(v for v in values[0]['inventoryCells'] if v['inventorySlot']==index)
        self.tap(cell['x'],cell['y'])
    def snapshot(self, label):
        self.act('nifty-native-state',ticks=2); value = json.loads((self.profile/'nifty-native-state.json').read_text())
        path = self.out/(str(len(self.checks)).zfill(4)+'-'+self.kind+'-'+label+'.json'); assert not path.exists()
        path.write_text(json.dumps(dict(client=self.state,server=value),indent=2)+'\n'); return value
    def native(self, data): return next(v for v in data['menus'] if v['class']=='net.jmb19905.niftycarts.container.'+self.menu)
    def enter(self):
        self.act('aim-entity',entity='niftycarts:'+self.kind,ticks=5)
        self.act('use-entity-ray',entity='niftycarts:'+self.kind,shift=True,ticks=16)
        self.act('sneak',down=False,ticks=3); self.w()
    def seed(self,index,item,count,marker):
        data='{release_cart_marker:'+str(marker)+',typed:[B;2b,6b]}'
        if self.mc=='1.20.1': suffix='{release_cart_marker:'+str(marker)+',typed:[B;2b,6b]'+(',Damage:1'if item.endswith('_hoe')else'')+'}'
        else: suffix='[minecraft:custom_data='+data+(',minecraft:damage=1'if item.endswith('_hoe')else'')+']'
        self.act('command',command=f'item replace entity @p container.{index} with {item}{suffix} {count}',ticks=4)
    def scenario(self):
        self.act('close',ticks=3); self.act('fixture',block='minecraft:air',ticks=4)
        for command in ['kill @e[type=!minecraft:player]','gamemode survival @p','clear @p',
                        'summon niftycarts:'+self.kind+' 8.5 65 8.5 {NoGravity:1b,Invulnerable:1b}',
                        'tp @p 8.5 65 10.5 180 8','item replace entity @p weapon.mainhand with minecraft:air']:
            self.act('command',command=command,ticks=6)
        self.enter(); self.check('ordinary-native-entry-has-complete-cells',len([v for v in self.w()['slots']if not v['player']])==self.size)
        before = self.w()['bounds']; workspace = next(v for v in self.state['workspaceWindows']if v['kind']=='CONTAINER'and v['bounds']==before)
        if workspace.get('locked')in['true',True]:self.tap(**workspace['controls']['LOCK'])
        self.act('regression-move',session=self.w()['sessionId'],dx=14,dy=10,ticks=3)
        self.act('wait',ticks=2)
        self.check('cart-window-moves-before-native-slot-input',self.w()['bounds']!=before)
        for i in range(self.size): self.seed(9+i,self.item,1 if self.kind=='plow'else i+1,42+i)
        before = self.snapshot('seeded')
        for i in range(self.size): self.inv(9+i);self.slot(i)
        inserted = self.snapshot('inserted')
        self.check('every-native-slot-retains-its-designated-complete-stack',all(stack(self.native(inserted)['slots'][i]['item'])==stack(before['inventory'][9+i]['item'])for i in range(self.size)))
        self.check('native-input-consumes-each-player-reference-once',all(item_count(inserted['inventory'][9+i]['item'])[1]==0 for i in range(self.size))and all(item_count(v['carried'])[1]==0 for v in inserted['menus']))
        self.act('close',ticks=4);self.enter();reopened=self.snapshot('reopened')
        self.check('cart-equipment-persists-through-ordinary-reopening',all(stack(self.native(reopened)['slots'][i]['item'])==stack(before['inventory'][9+i]['item'])for i in range(self.size)))
        for i in range(self.size):self.slot(i);self.inv(9+i)
        returned=self.snapshot('returned')
        self.check('all-native-cell-contents-return-once-with-complete-data',all(stack(returned['inventory'][9+i]['item'])==stack(before['inventory'][9+i]['item'])for i in range(self.size))and all(item_count(v['item'])[1]==0 for v in self.native(returned)['slots'][:self.size]))
        if self.kind=='seed_drill':
            self.seed(20,'minecraft:diamond',3,99);foreign=self.snapshot('foreign-before');self.inv(20);self.slot(0);denied=self.snapshot('foreign-refused')
            self.check('native-seed-filter-refuses-foreign-item-with-full-cursor-data',item_count(self.native(denied)['slots'][0]['item'])[1]==0 and stack(self.native(denied)['carried'])==stack(foreign['inventory'][20]['item']))
            self.inv(20)
        self.act('capture',capture='release-nifty-'+self.kind.replace('_','-'),ticks=2)
        self.act('command',command='kill @e[type=niftycarts:'+self.kind+']',ticks=15)
        self.check('cart-invalidation-closes-its-native-source',not any(v.get('menu')=='net.jmb19905.niftycarts.container.'+self.menu for v in self.state.get('allWindows',[])))
        self.completed.append(self.kind);self.save()
    def run(self):
        # The raw launcher display fixture also configures the latest target;
        # the older reference protocol additionally exposes this same setting.
        if self.mc in ['1.20.1','1.21.1']:self.act('gui-scale',scale=2,width=1280,height=900,ticks=4)
        else:self.act('wait',ticks=3)
        for self.kind,self.menu,self.screen,self.item,self.size in CASES:self.scenario()
        self.act('close',ticks=2);self.save(True)
        return dict(cases=2,failed=0,checks=len(self.checks),nifty_features_checked=True)


def run(profile,mc,loader,*args,**kw):return Carts(profile,mc).run()
