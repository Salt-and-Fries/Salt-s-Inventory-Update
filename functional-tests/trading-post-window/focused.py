"""Focused live checks for Trading Post's compact merchant window (not a full suite)."""
from pathlib import Path
import json,re,sys
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'project-everything-ten'))
from send import send

class Check:
    def __init__(self,out):
        self.out=Path(out);self.folder=self.out/'trading-post-checks';self.folder.mkdir(exist_ok=True)
        self.state={};self.results=[]
    def call(self,kind,**args):
        self.state=send(self.out,kind,**args)['client'];return self.state
    def cmd(self,text):self.call('command',command=text,ticks=8)
    def window(self):return next(w for w in self.state['allWindows'] if w.get('menuId')=='tradingpost:trading_post')
    def info(self):return self.window()['nativeValues']
    def check(self,name,passed):
        self.results.append({'id':name,'passed':bool(passed)})
        (self.folder/'journal.json').write_text(json.dumps({'scope':'Trading Post layout, native search, aggregation, scrolling, payment and output clicks only','checks':self.results},indent=2))
        assert passed,name;print('PASS',name,flush=True)
    def tap(self,x,y):self.call('click',x=x,y=y,button=0,ticks=1);self.call('release',x=x,y=y,button=0,ticks=7)
    def point(self,x,y):
        b=self.window()['bounds'];return b['x']+6+x,b['y']+16+y
    def click(self,x,y):self.tap(*self.point(x,y))
    def inventory(self,index):
        w=next(w for w in self.state['workspaceWindows'] if w['kind']=='INVENTORY')
        p=next(p for p in w['inventoryCells'] if p['inventorySlot']==index);self.tap(p['x'],p['y'])
    def search(self,query):
        self.click(45,16);self.call('key',key=269,ticks=1)
        # Native EditBox checks physical GLFW Ctrl state, not a synthetic key
        # event's modifiers. Exercise ordinary End/Backspace edits instead.
        for _ in range(len(self.info()['search'])):self.call('key',key=259,ticks=1)
        if query:self.call('text',text=query,ticks=8)
        assert self.info()['search']==query,(self.info()['search'],query)
    def snapshot(self,name):
        self.call('snapshot-server',ticks=2);server=json.loads((self.out/'server-state.json').read_text())
        (self.folder/(name+'.json')).write_text(json.dumps({'client':self.state,'server':server},indent=2));return server
    def menu(self,server):return next(m for m in server['menus'] if m['class']==self.window()['menu'])
    def count(self,server,index):
        s=self.menu(server)['slots'][index]['item'];return int(re.search(r'Count:(\d+)b',s)[1])
    def capture(self,name):self.call('move',x=639,y=449,ticks=30);self.call('capture',capture=name,ticks=8)
    def setup(self):
        self.call('close');self.call('fixture',block='tradingpost:trading_post');self.cmd('kill @e[type=minecraft:villager]')
        self.cmd('gamemode survival @a')
        fixture=json.loads(Path(__file__).with_name('fixture.json').read_text())
        self.cmd('summon minecraft:villager 10 65 8 {Tags:["siu_trading_post"],NoAI:1b,Invulnerable:1b,Silent:1b,PersistenceRequired:1b,VillagerData:{type:"minecraft:plains",profession:"minecraft:librarian",level:4},Xp:200,Offers:{Recipes:['+','.join(fixture['recipes'])+']}}')
        self.cmd('summon minecraft:villager 6 65 8 {Tags:["siu_trading_farmer"],NoAI:1b,Invulnerable:1b,Silent:1b,PersistenceRequired:1b,VillagerData:{type:"minecraft:plains",profession:"minecraft:farmer",level:2},Xp:20,Offers:{Recipes:[{buy:{id:"minecraft:emerald",Count:1b},sell:{id:"minecraft:bread",Count:6b},maxUses:16,xp:2,priceMultiplier:0.0f},{buy:{id:"minecraft:wheat",Count:20b},sell:{id:"minecraft:emerald",Count:1b},maxUses:16,xp:2,priceMultiplier:0.0f}]}}')
        self.call('open-ray',ticks=25);self.call('inventory',ticks=10)
    def run(self):
        self.setup()
        self.check('merchant-style-with-24px-search-space',self.window()['bounds']['width']==254 and self.window()['bounds']['height']==136)
        self.check('two-villagers-ten-aggregated-offers',self.info()['filteredOfferCount']==10)
        self.capture('trading-post-overview')
        self.search('mending');self.check('native-tooltip-search-finds-enchantment',self.info()['filteredOfferCount']==1 and 'mending' in self.info()['filteredResults'][0])
        self.cmd('item replace entity @a inventory.2 with minecraft:emerald 18');self.cmd('item replace entity @a inventory.3 with minecraft:book 2')
        self.click(30,40);s=self.snapshot('mending-selected');a=self.count(s,0);b=self.count(s,1)
        self.check('filtered-index-selects-original-two-input-offer',a>=9 and b>=1 and self.count(s,2)==1)
        self.capture('trading-post-two-input')
        self.click(208,61);self.inventory(13);s=self.snapshot('mending-purchased')
        self.check('large-output-padding-clicks-result','minecraft:mending' in s['inventory'][13]['item'])
        self.check('native-trade-deducts-exact-payment',self.count(s,0)==a-9 and self.count(s,1)==b-1)
        self.check('live-merchant-XP-updates',self.window()['merchant']['xp']==205)
        self.check('live-offer-use-count-updates',self.window()['merchant']['offers'][1]['used']==1)
        self.click(136,73);self.inventory(15);s=self.snapshot('first-payment-withdrawn')
        self.check('moved-first-payment-hitbox',self.count(s,0)==0 and 'minecraft:emerald' in s['inventory'][15]['item'])
        self.click(162,73);self.inventory(16);s=self.snapshot('second-payment-withdrawn')
        self.check('moved-second-payment-hitbox',self.count(s,1)==0 and 'minecraft:book' in s['inventory'][16]['item'])
        self.click(30,40)
        self.search('zzzznosuchoffer');s=self.snapshot('empty-search')
        self.check('no-match-search-empty-and-payments-refunded',self.info()['filteredOfferCount']==0 and self.count(s,0)==0 and self.count(s,1)==0)
        self.capture('trading-post-empty-search')
        self.search('bread');self.check('search-reaches-second-villager',self.info()['filteredOfferCount']==1 and 'minecraft:bread' in self.info()['filteredResults'][0])
        self.click(30,40);s=self.snapshot('bread-selected');a=self.count(s,0)
        self.check('second-villager-result',self.count(s,2)==6 and self.window()['merchant']['level']==2)
        self.click(220,73);self.inventory(14);s=self.snapshot('bread-purchased')
        self.check('second-villager-purchase-and-cost','minecraft:bread' in s['inventory'][14]['item'] and self.count(s,0)==a-1)
        self.search('');self.check('clear-search-restores-all-offers',self.info()['filteredOfferCount']==10)
        x,y=self.point(30,50)
        for _ in range(7):self.call('scroll',x=x,y=y,amount=-1,ticks=3)
        self.check('four-row-scroll-reaches-last-offer',self.info()['tradeScroll']==6)
        self.click(30,100);self.check('bottom-row-selects-tenth-native-offer',self.info()['selectedTrade']==9)
        self.capture('trading-post-last-page')
        self.click(95,31);self.check('scrollbar-track-returns-to-top',self.info()['tradeScroll']==0)
        x,y=self.point(95,40);self.call('click',x=x,y=y,button=0,ticks=1)
        nx,ny=self.point(95,108);self.call('drag',x=nx,y=ny,dx=0,dy=ny-y,button=0,ticks=3);self.call('release',x=nx,y=ny,button=0,ticks=5)
        self.check('scrollbar-thumb-drags-to-last-page',self.info()['tradeScroll']==6)
        self.search('ink sac');self.click(30,40);s=self.snapshot('sold-out')
        self.check('sold-out-filter-retains-no-result',self.info()['filteredOfferCount']==1 and self.count(s,2)==0)
        self.capture('trading-post-sold-out')
        self.search('');self.click(95,31);self.click(30,40)
        self.capture('trading-post-final')
        self.call('close');self.call('open-ray',ticks=25)
        self.check('close-reopen-restores-native-offers-and-use-count',self.info()['filteredOfferCount']==10 and self.window()['merchant']['offers'][1]['used']==1)

if __name__=='__main__':Check(sys.argv[1]).run()
