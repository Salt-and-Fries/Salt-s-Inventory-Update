"""Focused live acceptance checks for the compact vanilla merchant restyle."""
from pathlib import Path
import json,sys
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'project-everything-ten'))
from send import send

class MerchantCheck:
    def __init__(self,output,attempt=None):
        self.out=Path(output);self.folder=self.out/'merchant-checks'
        if attempt:self.folder=self.folder/attempt
        self.folder.mkdir(parents=True,exist_ok=True)
        self.state={};self.journal={'scope':'Affected vanilla merchant visuals, trading, scrolling and window controls only','checks':[]}
    def call(self,kind,**args):
        r=send(self.out,kind,**args);self.state=r.get('client',{});return r
    def check(self,name,condition):
        self.journal['checks'].append({'id':name,'passed':bool(condition)})
        (self.folder/'journal.json').write_text(json.dumps(self.journal,indent=2));assert condition,name;print('PASS',name,flush=True)
    def window(self):return next(w for w in self.state['allWindows'] if w.get('menuId')=='minecraft:merchant')
    def info(self):return self.window()['merchant']
    def tap(self,x,y):
        self.call('click',x=x,y=y,button=0,ticks=1);self.call('release',x=x,y=y,button=0,ticks=7)
    def point(self,x,y):
        b=self.window()['bounds'];return b['x']+6+x,b['y']+16+y
    def native(self,x,y):self.tap(*self.point(x,y))
    def detail(self,x,y):self.native(x-8,y+16)
    def select(self,row):self.native(30,20+row*20)
    def inventory(self,index):
        w=next(w for w in self.state['workspaceWindows'] if w['kind']=='INVENTORY')
        at=next(p for p in w['inventoryCells'] if p['inventorySlot']==index);self.tap(at['x'],at['y'])
    def cmd(self,s):self.call('command',command=s,ticks=10)
    def snap(self,name):
        self.call('snapshot-server',ticks=1);d=json.loads((self.out/'server-state.json').read_text())
        (self.folder/(name+'.json')).write_text(json.dumps({'client':self.state,'server':d},indent=2));return d
    def menu(self,d):return next(m for m in d['menus'] if m['class']==self.window()['menu'])
    def count(self,d,slot):
        import re
        item=self.menu(d)['slots'][slot]['item'];m=re.search(r'Count:(\d+)b',item);return int(m[1])
    def capture(self,name):
        self.call('capture',capture=name,ticks=8)
        (self.folder/(name+'-client.json')).write_text(json.dumps(self.state,indent=2))
    def setup(self):
        fixture=json.loads(Path('build/remaining-ui-audit/gameplay/1.20.1-fabric-native-villager-review01/merchant-review/fixture.json').read_text())
        self.call('close');self.call('fixture',block='minecraft:air');self.cmd('kill @e[type=minecraft:villager]');self.cmd('gamemode survival @a')
        self.cmd('summon minecraft:villager 8.5 65 8.5 {Tags:["siu_review_merchant"],NoAI:1b,Invulnerable:1b,Silent:1b,PersistenceRequired:1b,VillagerData:{type:"minecraft:plains",profession:"minecraft:librarian",level:4},Xp:200,Offers:{Recipes:['+','.join(fixture['recipes'])+']}}')
        self.cmd('tp @a 8.5 65 11.5 180 8');self.call('aim-entity',entity='minecraft:villager',radius=8)
        self.call('use-entity-ray',entity='minecraft:villager',shift=False,ticks=15);self.call('inventory',ticks=8)
    def run(self):
        self.setup();b=self.window()['bounds'];self.check('112px-height-with-four-pixels-extra-bottom-padding',b['height']==112)
        self.check('four-visible-offers-and-eight-scrollable-offers',len(self.info()['offers'])==8 and self.info()['scroll']==0)
        self.check('no-XP-preview-with-empty-payment',self.info()['futureXp']==0)
        self.capture('merchant-empty')
        self.cmd('item replace entity @a inventory.0 with minecraft:paper 48')
        self.inventory(9);self.snap('paper-picked-up');self.detail(144,45)
        d=self.snap('paper-payment');self.check('single-payment-result-and-XP-preview',self.count(d,2)==1 and self.info()['futureXp']==5)
        self.detail(216,33);self.inventory(10);d=self.snap('single-trade')
        self.check('large-result-padding-is-clickable','minecraft:emerald' in d['inventory'][10]['item'])
        self.check('single-trade-consumes-exact-cost',self.count(d,0)==24 and self.info()['offers'][0]['used']==1)
        self.check('trade-updates-merchant-XP',self.info()['xp']==205)
        self.detail(144,45);self.inventory(9);d=self.snap('payments-cleared')
        self.check('removing-payment-removes-prospective-XP',self.info()['futureXp']==0 and self.count(d,2)==0)
        self.cmd('item replace entity @a inventory.2 with minecraft:emerald 18')
        self.cmd('item replace entity @a inventory.3 with minecraft:book 2')
        self.select(1);d=self.snap('two-payments');a=self.count(d,0);book=self.count(d,1)
        self.check('two-ingredient-offer-autofills-native-payment',a>=9 and book>=1 and self.count(d,2)==1)
        self.detail(216,33);self.inventory(13);d=self.snap('two-ingredient-trade')
        self.check('two-ingredient-trade-costs-and-result',self.count(d,0)==a-9 and self.count(d,1)==book-1 and 'minecraft:mending' in d['inventory'][13]['item'])
        self.check('second-trade-updates-XP',self.info()['xp']==210 and self.info()['offers'][1]['used']==1)
        x,y=self.point(30,40)
        for _ in range(4):self.call('scroll',x=x,y=y,amount=-1,ticks=4)
        self.select(2);d=self.snap('sold-out')
        self.check('four-row-scroll-reaches-final-page',self.info()['scroll']==4 and self.info()['selected']==6)
        self.check('sold-out-has-no-result-or-preview',self.info()['offers'][6]['outOfStock'] and self.count(d,2)==0 and self.info()['futureXp']==0)
        self.detail(216,33);d=self.snap('sold-out-click')
        self.check('sold-out-result-cannot-be-taken','Count:0b' in d['menus'][0]['carried'])
        self.capture('merchant-sold-out')
        for _ in range(3):self.call('scroll',x=x,y=y,amount=-1,ticks=2)
        self.check('scroll-clamps-at-four',self.info()['scroll']==4)
        for _ in range(8):self.call('scroll',x=x,y=y,amount=1,ticks=2)
        self.check('scroll-clamps-at-zero',self.info()['scroll']==0)
        w=next(w for w in self.state['workspaceWindows'] if w['kind']=='CONTAINER');b=w['bounds']
        if w['locked']=='true':self.tap(**w['controls']['LOCK'])
        self.call('click',x=b['x']+25,y=b['y']+7,button=0,ticks=1)
        self.call('drag',x=b['x']-5,y=b['y']+17,dx=-30,dy=10,button=0,ticks=1)
        self.call('release',x=b['x']-5,y=b['y']+17,button=0,ticks=5)
        at=self.window()['bounds'];self.check('native-window-controls-and-title-drag',at['x']==b['x']-30 and at['y']==b['y']+10)
        self.call('gui-scale',scale=3,width=1280,height=900,ticks=12);b=self.window()['bounds']
        self.check('compact-layout-fits-GUI-scale-three',b['height']==112 and b['x']>=0 and b['x']+b['width']<=self.state['guiWidth'])
        self.capture('merchant-gui-scale-three');self.call('gui-scale',scale=2,width=1280,height=900,ticks=10)
        self.call('close');self.call('aim-entity',entity='minecraft:villager',radius=8);self.call('use-entity-ray',entity='minecraft:villager',shift=False,ticks=12)
        self.check('close-and-reopen-retains-trades-and-XP',self.info()['xp']==210 and self.info()['offers'][0]['used']==1 and self.info()['offers'][1]['used']==1)
        self.call('close');self.cmd('data modify entity @e[type=minecraft:villager,tag=siu_review_merchant,limit=1] VillagerData.level set value 5')
        self.cmd('data modify entity @e[type=minecraft:villager,tag=siu_review_merchant,limit=1] Xp set value 300')
        self.call('use-entity-ray',entity='minecraft:villager',shift=False,ticks=12)
        self.check('master-villager-keeps-compact-layout',self.info()['level']==5 and self.window()['bounds']['height']==112)
        self.capture('merchant-master')
        self.call('close');self.cmd('data modify entity @e[type=minecraft:villager,tag=siu_review_merchant,limit=1] Offers.Recipes[0].maxUses set value 2')
        self.cmd('item replace entity @a inventory.0 with minecraft:paper 24')
        self.call('use-entity-ray',entity='minecraft:villager',shift=False,ticks=12);self.call('inventory',ticks=8)
        self.inventory(9);self.snap('last-payment-picked-up');self.detail(144,45);self.detail(216,33);self.inventory(14)
        d=self.snap('last-use-trade')
        self.check('purchase-refreshes-live-sold-out-state',self.info()['offers'][0]['used']==2 and self.info()['offers'][0]['outOfStock'] and self.count(d,2)==0)

    def spacing(self):
        self.setup();self.check('height-increased-only-four-pixels',self.window()['bounds']['height']==112)
        self.check('retains-four-offer-scroll-page',len(self.info()['offers'])==8 and self.info()['scroll']==0)
        self.cmd('item replace entity @a inventory.2 with minecraft:emerald 18')
        self.cmd('item replace entity @a inventory.3 with minecraft:book 2')
        self.select(1);d=self.snap('shifted-payments');a=self.count(d,0);book=self.count(d,1)
        self.check('shifted-payment-slots-show-native-result',a>=9 and book>=1 and self.count(d,2)==1)
        self.capture('merchant-spacing-two-input')
        self.detail(216,33);self.inventory(13);d=self.snap('shifted-output-taken')
        self.check('shifted-output-padding-clicks-correct-slot','minecraft:mending' in d['inventory'][13]['item'])
        self.check('shifted-trade-preserves-exact-costs',self.count(d,0)==a-9 and self.count(d,1)==book-1)
        self.detail(144,45);self.inventory(15);d=self.snap('shifted-first-input-withdrawn')
        self.check('shifted-first-input-hitbox','minecraft:emerald' in d['inventory'][15]['item'] and self.count(d,0)==0)
        self.detail(170,45);self.inventory(16);d=self.snap('shifted-second-input-withdrawn')
        self.check('shifted-second-input-hitbox','minecraft:book' in d['inventory'][16]['item'] and self.count(d,1)==0)
        x,y=self.point(30,40)
        for _ in range(4):self.call('scroll',x=x,y=y,amount=-1,ticks=3)
        self.select(2);self.capture('merchant-spacing-last-page')
        self.check('last-four-rows-remain-accessible',self.info()['scroll']==4 and self.info()['selected']==6)

if __name__=='__main__':
    c=MerchantCheck(sys.argv[1]);getattr(c,sys.argv[2] if len(sys.argv)>2 else 'run')()
