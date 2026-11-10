"""Real entry-point captures and focused interactions for the seven-mod fixture."""
from pathlib import Path
import argparse,json,sys
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send

class Audit:
    def __init__(self,run,side):
        self.run=Path(run);self.side=side;self.state={};self.rows=[]
        self.journal=self.run/'rollout-capture-journal.json'
        if self.journal.exists():self.rows=json.loads(self.journal.read_text())
    def a(self,kind,**kw):
        self.state=send(self.run,kind,**kw)['client'];return self.state
    def cmd(self,text):return self.a('command',command=text,ticks=5)
    def tap(self,x,y):
        self.a('move',x=x,y=y,ticks=2);self.a('click',x=x,y=y,ticks=1)
        return self.a('release',x=x,y=y,ticks=8)
    def win(self,menu):return next(w for w in self.state.get('allWindows',[]) if w.get('menuId')==menu)
    def check(self,name,ok):
        self.rows.append({'check':name,'passed':bool(ok),'side':self.side});self.save()
        assert ok,name
        print('PASS',name,flush=True)
    def save(self):self.journal.write_text(json.dumps(self.rows,indent=2))
    def capture(self,name):
        stem=self.side+'-'+name+'-1211-fabric'
        self.a('move',x=639,y=449,ticks=5);self.a('capture',capture=stem,ticks=16)
        self.check(name+'-expected-render-side',self.state.get('siu_loaded')==(self.side=='siu'))
        self.rows.append({'ui':name,'side':self.side,'screenshot':str(self.run/'screenshots'/(stem+'.png')),'state':self.state})
        self.save();print('CAPTURE',stem,flush=True)
    def block(self,block):
        self.a('close');self.a('fixture',block=block,ticks=6);self.cmd('gamemode survival @a')
        self.a('open',ticks=16)
    def accessories(self):
        if self.side=='native':
            self.check('native-accessories-menu',self.state.get('screen','').endswith('.AccessoriesScreen'))
        else:self.check('connected-normal-and-cosmetic-panel',len(self.win('salts_inventory_compat:accessories_equipment')['slots'])==36)
        self.capture('accessories')
    def bountiful(self):
        self.block('bountiful:bountyboard')
        if self.side=='siu':
            w=self.win('bountiful:board');v=w['nativeValues'];b=w['bounds']
            self.check('four-bounty-preview-rows',v['lastBountyRow']-v['firstBountyRow']==3)
            self.tap(b['x']+41,b['y']+40)
            self.check('native-bounty-selection',self.win('bountiful:board')['nativeValues']['selectedBounty']>=0)
            self.a('scroll',x=b['x']+86,y=b['y']+61,amount=-2,ticks=6)
            self.check('native-bounty-wheel',self.win('bountiful:board')['nativeValues']['firstBountyRow']>0)
        else:self.check('ordinary-native-board',self.state.get('screen','').endswith('.BoardScreen'))
        self.capture('bountiful')
    def toms(self):
        self.block('toms_trading_network:vending_machine')
        if self.side=='siu':
            w=self.win('toms_trading_network:vending_machine.menu_config');b=w['bounds']
            self.check('native-config-geometry',b['width']==184 and b['height']==146)
        else:
            self.check('native-config-screen',self.state.get('screen','').endswith('.VendingMachineConfigScreen'))
        self.capture('toms-config')
        if self.side=='siu':
            self.tap(b['x']+8+141,b['y']+24+13)
            self.check('native-customer-transition',self.win('toms_trading_network:vending_machine.menu_trading')['bounds']['height']==97)
        else:
            button=next(w for w in self.state['widgets'] if w.get('message')=='Trading View')
            self.tap(button['x']+button['width']/2,button['y']+button['height']/2)
            self.check('native-customer-screen',self.state.get('screen','').endswith('.VendingMachineTradingScreen'))
        self.capture('toms-customer')
    def minecells(self):
        self.block('minecells:cell_crafter')
        if self.side=='siu':
            w=self.win('minecells:cell_forge');self.check('native-blue-crafter',w['bounds']['width']==178)
        else:self.check('ordinary-native-crafter',self.state.get('screen','').endswith('.CellCrafterScreen'))
        self.capture('minecells-crafter')
        b=(self.win('minecells:cell_forge') if self.side=='siu' else self.state)['bounds']
        self.tap(b['x']+41,b['y']+(58 if self.side=='siu' else 36))
        if self.side=='siu':self.check('complete-native-parchment-picker',self.win('minecells:cell_forge')['bounds']['width']==204)
        self.capture('minecells-recipe-picker')
    def aircraft(self):
        self.a('close');self.cmd('kill @e[type=!minecraft:player]')
        self.cmd('summon immersive_aircraft:cargo_airship 8.5 65 8.5 {NoGravity:1b,Invulnerable:1b}')
        self.cmd('tp @a 8.5 65 10.5 180 8')
        self.a('aim-entity',entity='immersive_aircraft:cargo_airship',ticks=3)
        self.a('use-entity-ray',entity='immersive_aircraft:cargo_airship',shift=True,ticks=16)
        self.a('sneak',down=False,ticks=3)
        if self.side=='siu':self.check('cargo-airship-main-and-two-attached-panels',len(self.state.get('allWindows',[]))==3)
        else:self.check('native-aircraft-screen','immersive_aircraft' in self.state.get('screen',''))
        self.capture('aircraft-cargo-airship')
    def tradingpost(self):
        self.a('close');self.a('fixture',block='tradingpost:trading_post',ticks=6)
        self.cmd('kill @e[type=minecraft:villager]');self.cmd('gamemode survival @a')
        recipes=','.join('{buy:{id:"minecraft:emerald",count:'+str(price)+'},sell:{id:"minecraft:'+item+'",count:'+str(amount)+'},maxUses:100,uses:0,xp:2,priceMultiplier:0.0f}' for item,price,amount in [('bread',2,3),('diamond',3,1)])
        self.cmd('summon minecraft:villager 10 65 8 {NoAI:1b,Invulnerable:1b,Silent:1b,PersistenceRequired:1b,VillagerData:{type:"minecraft:plains",profession:"minecraft:librarian",level:4},Xp:200,Offers:{Recipes:['+recipes+']}}')
        self.cmd('item replace entity @a inventory.0 with minecraft:emerald 12')
        self.a('open',ticks=25)
        if self.side=='siu':
            w=self.win('tradingpost:trading_post');b=w['bounds']
            self.check('native-offers-received',w['nativeValues']['filteredOfferCount']==2)
        else:self.check('ordinary-native-trading-post',self.state.get('screen','').endswith('.TradingPostScreen'))
        self.capture('trading-post')
        if self.side=='siu':
            self.tap(b['x']+51,b['y']+32);self.a('text',text='bread',ticks=16)
            w=self.win('tradingpost:trading_post')
            self.check('native-search-filters-offers',w['nativeValues']['search']=='bread' and w['nativeValues']['filteredOfferCount']==1)
            self.tap(b['x']+36,b['y']+56)
            w=self.win('tradingpost:trading_post')
            self.check('filtered-offer-fills-real-payment-and-output',w['slots'][0]['count']>=2 and w['slots'][2]['item']=='minecraft:bread' and w['slots'][2]['count']==3)
            self.capture('trading-post-filtered-offer')
            before=w['slots'][0]['count'];self.tap(b['x']+226,b['y']+89)
            w=self.win('tradingpost:trading_post')
            self.check('native-purchase-deducts-exact-payment',w['slots'][0]['count']==before-2 and w['merchant']['offers'][0]['used']==1)
            self.a('snapshot-server',ticks=2)
            server=json.loads((self.run/'server-state.json').read_text())
            # SIU owns one shared cursor on the player's inventory menu; the
            # detached merchant menu's local cursor intentionally remains empty.
            self.check('native-purchase-delivers-three-bread',sum('count:3' in m['carried'] and 'minecraft:bread' in m['carried'] for m in server['menus'])==1)
    def travelers(self):
        self.a('close');self.a('fixture',block='minecraft:air',ticks=6);self.cmd('gamemode survival @a')
        self.a('held',item='travelersbackpack:standard',count=1,ticks=8);self.a('use-item',ticks=16)
        if self.side=='siu':self.check('backpack-native-adapter','TravelersWindows' in self.win('travelersbackpack:backpack_item')['template'])
        else:self.check('ordinary-native-backpack','travelersbackpack' in self.state.get('screen',''))
        self.capture('travelers-backpack')
        if self.side=='siu':
            w=self.win('travelersbackpack:backpack_item');b=w['bounds']
            self.tap(b['x']+164,b['y']+30)
            self.check('original-tool-toggle-routes-to-native-server',self.win('travelersbackpack:backpack_item')['nativeValues']['showTools'])
            self.capture('travelers-tools-drawer')
            self.a('inventory',ticks=6);self.cmd('item replace entity @a inventory.0 with minecraft:diamond 3')
            inv=next(w for w in self.state['workspaceWindows'] if w['kind']=='INVENTORY')
            cell=next(c for c in inv['inventoryCells'] if c['inventorySlot']==9)
            self.tap(cell['x'],cell['y']);self.tap(b['x']+16,b['y']+46)
            self.check('native-storage-insertion',self.win('travelersbackpack:backpack_item')['slots'][0]['item']=='minecraft:diamond' and self.win('travelersbackpack:backpack_item')['slots'][0]['count']==3)
            self.tap(b['x']+16,b['y']+46);self.tap(cell['x'],cell['y'])
            self.check('native-storage-withdrawal',self.win('travelersbackpack:backpack_item')['slots'][0]['count']==0)

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('run');p.add_argument('--side',required=True,choices=['native','siu']);p.add_argument('--case',required=True,choices=['accessories','bountiful','toms','minecells','aircraft','tradingpost','travelers']);a=p.parse_args()
    audit=Audit(a.run,a.side);audit.a('wait',ticks=1);getattr(audit,a.case)()
