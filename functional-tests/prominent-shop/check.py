"""Focused native shop input, transaction, synchronization and layout checks."""
from pathlib import Path
import importlib.util
import json
import re
import sys
import time

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('forge_check',ROOT/'functional-tests/prominent-forge/check.py')
forge=importlib.util.module_from_spec(spec);spec.loader.exec_module(forge)


def item_count(state,item):
    return sum(int(re.search(r'Count:(\d+)b',c['item'])[1]) for c in state['inventory'] if 'id:"'+item+'"' in c['item'])


class Shop(forge.Check):
    def shop(self,state):return next(w for w in state['allWindows'] if 'shop' in w)
    def data(self,state):return self.shop(state)['shop']
    def read_server(self):
        self.act('prominent-shop-state',ticks=3)
        s=json.loads((self.run/'prominent-shop-state.json').read_text())
        (self.out/f'{self.sequence:03}-server.json').write_text(json.dumps(s,indent=2))
        return s

    def tab(self,group):
        s=self.act(ticks=3)
        tabs=[e for e in self.data(s)['elements'] if e['type'].endswith('HoverableButtonContainer') and e.get('width')==61]
        b=tabs[['BUY','SELL','TRADE'].index(group)]
        s=self.click(b['x']+25,b['y']+12,ticks=12)
        self.check('Native '+group+' tab works',self.data(s)['group']==group)
        return s

    def buy_first(self):
        s=self.act(ticks=12)
        row=next(e for e in self.data(s)['elements'] if e['type'].endswith('HoverableButtonContainer') and e.get('width')==158)
        return self.click(row['x']+75,row['y']+11,ticks=12)

    def seed(self,index,item,count=1):self.command(f'item replace entity @p inventory.{index} with {item} {count}',ticks=3)

    def open(self):
        self.command('tp @p 8.5 65 11 180 0',ticks=8)
        s=self.act('use-entity-ray',entity='prominent:serkonid',ticks=25)
        self.command('tp @p 8.5 65 11 90 0',ticks=3)
        return self.act(ticks=5)

    def run_checks(self):
        self.act('fixture',block='minecraft:air',ticks=6)
        for c in ['kill @e[type=prominent:serkonid]','gamerule sendCommandFeedback false','gamerule announceAdvancements false',
                  'gamemode survival @p','time set 6000','weather clear','advancement grant @p only minecraft:end/kill_dragon',
                  'prominent:cinders @p set 1234',
                  'summon prominent:serkonid 8.5 65 8.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,trades:"prominent:past/treasurer",trades_timestamp:'+str(int(time.time()*1000))+'L}']:
            self.command(c,ticks=5)
        s=self.open();w=self.shop(s)
        self.check('Shop opens through ordinary trader interaction in its dedicated template',w['menuId']=='prominent:cinder_trader' and 'ProminentShopWindows' in w['template'])
        server=self.read_server()
        self.check('Native offers and portrait identity synchronize to the owning window',len(self.data(s)['offers'])==len(server['menus'][0]['offers'])>20 and int(self.data(s)['portrait'])==server['entities'][0]['id'])
        s=self.move(s,'CONTAINER',199,55)
        self.check('Copied stone header drags the complete native shop',self.shop(s)['bounds']['x']==199 and self.shop(s)['bounds']['y']==55)
        self.act('inventory',ticks=5)
        s=self.move(self.act(),'INVENTORY',230,250)
        self.buy_first();state=self.read_server()
        self.check('Missing native payment cannot purchase a reward',item_count(state,'soulsweapons:moonstone')==0 and state['cinders']==1234)
        self.seed(0,'minecraft:diamond',2)
        self.buy_first();state=self.read_server()
        self.check('Buy consumes one actual diamond and delivers a native Moonstone',item_count(state,'minecraft:diamond')==1 and item_count(state,'soulsweapons:moonstone')==1)
        self.tab('TRADE');self.seed(1,'minecraft:gold_ingot',32)
        self.buy_first();state=self.read_server()
        self.check('Trade consumes sixteen gold and delivers the original diamond reward',item_count(state,'minecraft:gold_ingot')==16 and item_count(state,'minecraft:diamond')==2)
        self.tab('SELL');self.seed(2,'mythicupgrades:peridot_crystal_shard');self.seed(3,'minecraft:amethyst_shard')
        self.buy_first();state=self.read_server()
        self.check('Sell consumes both native ingredients and delivers its reward',item_count(state,'mythicupgrades:peridot_crystal_shard')==0 and item_count(state,'minecraft:amethyst_shard')==0 and item_count(state,'minecraft:diamond')==3)
        self.check('Item-for-item trades preserve the native Cinders balance',state['cinders']==1234)
        s=self.tab('BUY');scroll=next(e for e in self.data(s)['elements'] if e['type'].endswith('ScrollContainer'))
        before=next(e['y'] for e in self.data(s)['elements'] if e['type'].endswith('HoverableButtonContainer') and e.get('width')==158)
        s=self.act('scroll',x=scroll['x']+80,y=scroll['y']+70,amount=-5,ticks=15)
        after=next(e['y'] for e in self.data(s)['elements'] if e['type'].endswith('HoverableButtonContainer') and e.get('width')==158)
        self.check('Native offer list scrolls within its own viewport',after<before,dict(first_row_before=before,first_row_after=after))
        state=self.read_server()
        # Client item renderers may add an empty tag to their item preview.
        # Compare all actual offer fields while ignoring that empty tag only.
        self.check('Displayed offers match the authoritative native menu',
                   json.dumps(self.data(s)['offers'],sort_keys=True).replace(',tag:{}','')==json.dumps(state['menus'][0]['offers'],sort_keys=True).replace(',tag:{}',''))
        s=self.act('gui-scale',scale=3,ticks=18)
        s=self.move(s,'CONTAINER',92,5);s=self.move(s,'INVENTORY',125,205)
        self.tab('SELL');s=self.tab('BUY');self.seed(0,'minecraft:diamond',2)
        before=item_count(self.read_server(),'soulsweapons:moonstone');self.buy_first();state=self.read_server()
        self.check('Native purchase remains aligned after live GUI scale and position changes',item_count(state,'soulsweapons:moonstone')==before+1)
        self.command('item replace entity @p hotbar.0 with minecraft:air',ticks=3)
        for group in ['BUY','SELL','TRADE']:
            self.tab(group)
            self.act('pointer',x=25,y=25,ticks=100,capture='shop-'+group.lower()+'-gui3')
        s=self.act();shop=self.shop(s)['bounds'];inv=next(w['bounds'] for w in s['workspaceWindows'] if w['kind']=='INVENTORY')
        self.check('Shop, complete tab caps and separate Inventory fit GUI scale 3',shop['width']==242 and shop['height']==159 and shop['y']+shop['height']<inv['y'] and inv['y']+inv['height']<=s['guiHeight'])
        w=next(w for w in s['workspaceWindows'] if w['kind']=='CONTAINER')
        self.click(**w['controls']['CLOSE']);s=self.act(ticks=8);state=self.read_server()
        self.check('Shop close leaves Inventory open and removes the native trader handler',not s['allWindows'] and any(w['kind']=='INVENTORY' for w in s['workspaceWindows']) and state['entities'][0]['handlers']==0)
        self.act('close',ticks=5);s=self.open()
        self.check('The same trader reopens with its original offers',len(self.data(s)['offers'])>20)
        self.command('tp @p 18.5 65 11 90 0',ticks=12);s=self.act(ticks=8);state=self.read_server()
        self.check('Original eight-block trader reach still closes the session',not s['allWindows'] and state['entities'][0]['handlers']==0)
        self.command('tp @p 8.5 65 11 180 0',ticks=8);self.open()
        self.act('gui-scale',scale=2,ticks=15)
        s=self.move(self.act(),'CONTAINER',199,55);self.act('inventory',ticks=5);self.move(self.act(),'INVENTORY',230,250)
        self.tab('BUY');self.act('pointer',x=25,y=25,ticks=20,capture='shop-buy-gui2')


if __name__=='__main__':
    Shop(Path(sys.argv[1]),'shop').run_checks()
