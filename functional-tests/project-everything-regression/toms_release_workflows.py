"""Native Tom-family release workflows, using screen callbacks and read-only ledgers."""
from pathlib import Path
import argparse
import hashlib
import json
import sys

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/all-target-rollout'))
from evidence import send


class Checks:
    def __init__(self,profile):
        self.profile=Path(profile);self.client={};self.results=[]
        self.out=self.profile/'toms-release';self.out.mkdir(exist_ok=True)
    def call(self,kind='wait',**values):
        reply=send(self.profile,kind,**values);self.client=reply.get('client',{});return reply
    def expect(self,name,condition,**details):
        self.results.append(dict(check=name,passed=bool(condition),**details))
        (self.out/'checks.json').write_text(json.dumps(dict(
            scope='Named native Tom workflows only; not a complete publisher or all-target suite',
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),checks=self.results),indent=2)+'\n')
        assert condition,name
        print('PASS',name,flush=True)
    def state(self,name):
        self.call('regression-state',ticks=2)
        data=json.loads((self.profile/'regression-state.json').read_text())
        (self.out/(name+'.json')).write_text(json.dumps(data,indent=2)+'\n');return data
    def seed(self,slot,item,count):
        self.call('regression-seed-player',items=[dict(slot=slot,item=item,count=count,snbt='{siu_regression_marker:42}')],ticks=4)
    def player(self,slot):self.call('regression-player-click',slot=slot,ticks=5)
    def slot(self,session,slot):self.call('regression-slot-click',session=session,slot=slot,ticks=5)
    def toms(self,session):
        self.call('regression-toms-state',session=session,ticks=3)
        return json.loads((self.profile/'release-toms-state.json').read_text())
    def window(self,session):
        self.call('wait',ticks=3)
        return next(w for w in self.client.get('allWindows',self.client.get('windows',[])) if w.get('sessionId')==session)
    def local(self,session,x,y):
        b=self.window(session)['bounds']
        self.call('regression-tap',session=session,x=b['x']+8+x,y=b['y']+24+y,ticks=10)
    def text(self,session,x,y,value):
        self.local(session,x,y);self.call('key',key=65,modifiers=2,ticks=2)
        self.call('key',key=259,ticks=2)
        self.call('text',text=value,ticks=12)
    def move(self,session):
        w=self.window(session)
        if w.get('locked') in [True,'true']:self.call('regression-control',session=session,control='LOCK',ticks=3)
        old=self.window(session)['bounds'];self.call('regression-move',session=session,dx=18,dy=12,ticks=5)
        self.expect('native-window-is-moved-before-its-controls-are-exercised',self.window(session)['bounds']!=old)
    def button(self,message):
        self.call('wait',ticks=3)
        choices=[{**widget,'source_session':window.get('sessionId')} for window in self.client.get('allWindows',self.client.get('windows',[]))
                 for widget in window.get('nativeWidgets',[]) if widget.get('message')==message and widget.get('visible',True)]
        self.expect('native-'+message.lower().replace(' ','-')+'-control-visible',len(choices)==1 and choices[0].get('active',True))
        self.call('regression-template-widget-tap',session=choices[0]['source_session'],widget_type=choices[0]['type'],message=message,ordinal=0,ticks=16)
    @staticmethod
    def count(cells,item):return sum(cell['count'] for cell in cells if cell['item']==item)
    @staticmethod
    def menu(data,suffix):return next(menu for menu in data['menus'] if menu['type'].endswith('.'+suffix))
    def configurator(self):
        def reopen():
            self.call('close',ticks=4);self.call('held',item='toms_storage:inventory_configurator',ticks=3)
            self.call('sneak',down=True,ticks=5);self.call('client-use-block',ticks=20);self.call('sneak',down=False,ticks=3)
            return self.menu(self.state('configurator-open'),'InventoryConfiguratorMenu')['session']
        def settings():return self.menu(self.toms(sid),'InventoryConfiguratorMenu')
        self.call('close',ticks=3);self.call('fixture',block='minecraft:chest',ticks=10)
        self.call('command',command='gamemode survival @a',ticks=3);self.call('command',command='clear @a',ticks=3)
        sid=reopen();self.move(sid);before=settings()
        for index,key in [(0,'priority'),(3,'side'),(4,'skip'),(5,'keepLast')]:
            prior=settings();self.local(sid,9,9+index*18);after=settings()
            self.expect('configurator-'+key+'-updates-the-native-block-filter',after[key]!=prior[key])
        changed=settings();self.local(sid,9,45)
        self.expect('remove-connected-retains-the-main-block-filter',len(settings()['connected'])==1)
        self.call('inventory',ticks=4);self.seed(9,'toms_storage:item_filter',1)
        self.player(9);self.slot(sid,0);inserted=self.state('configurator-filter-inserted')
        sample=self.menu(inserted,'InventoryConfiguratorMenu')['slots'][0]
        self.expect('configurator-real-filter-slot-accepts-the-full-tagged-item',sample['item']=='toms_storage:item_filter'
            and sample['count']==1 and 'siu_regression_marker' in sample['signature'] and inserted['cursor']['count']==0)
        sid=reopen();persisted=settings();reopened=self.state('configurator-persisted')
        self.expect('configurator-settings-and-filter-persist-through-native-reopening',
            all(persisted[k]==changed[k] for k in ['priority','side','skip','keepLast'])
            and self.menu(reopened,'InventoryConfiguratorMenu')['slots'][0]['signature']==sample['signature'])
        self.call('inventory',ticks=4);self.slot(sid,0);self.player(10);returned=self.state('configurator-filter-returned')
        self.expect('configurator-filter-returns-once-with-its-complete-data',returned['cursor']['count']==0
            and returned['inventory'][10]['signature']==sample['signature'] and returned['inventory'][10]['count']==1)
        # Native 2.10.3 ItemFilterSlot accepts an ordinary sample at capacity one;
        # it does not advertise a mayPlace refusal. Do not impose a new rule.
        # Its editor does require a real installed IItemFilter, so test that
        # actual missing prerequisite after recovering the filter above.
        self.call('regression-slot-click',session=sid,slot=0,button=1,ticks=8)
        denied=self.state('configurator-empty-editor-refused')
        self.expect('empty-filter-cannot-open-a-native-filter-editor',
            self.menu(denied,'InventoryConfiguratorMenu')['session']==sid
            and len(denied['menus'])==1 and denied['cursor']['count']==0
            and denied['inventory'][10]['signature']==sample['signature'])
        self.local(sid,151,9)
        sid=reopen();cleared=settings()
        self.expect('clear-configuration-restores-native-defaults',all(cleared[k]==before[k] for k in ['priority','side','skip','keepLast']))
        self.call('close',ticks=4)
    def link(self):
        self.call('close',ticks=3);self.call('catalog',ticks=2)
        catalog=json.loads((self.profile/'catalog.json').read_text());x,y,z=map(int,catalog.get('origin','8,65,8').split(','))
        self.call('fixture',block='toms_storage:inventory_cable_connector',ticks=10)
        for command in [f'fill {x-1} {y-1} {z-2} {x+1} {y-1} {z} minecraft:iron_block',
                        f'setblock {x} {y} {z-1} minecraft:beacon',
                        f'setblock {x} {y} {z} toms_storage:inventory_cable_connector[facing=north]',
                        'gamemode survival @a','item replace entity @a weapon.mainhand with minecraft:air']:
            self.call('command',command=command,ticks=10)
        self.call('wait',ticks=80);self.call('client-use-block',ticks=20)
        sid=self.menu(self.state('link-open'),'InventoryLinkMenu')['session']
        def settings():return self.menu(self.toms(sid),'InventoryLinkMenu')
        initial=settings();self.expect('real-beacon-enables-the-native-channel-interface',initial['beaconLevel']>=1)
        self.move(sid)
        for i in range(8):
            self.text(sid,40,22,'Release channel '+str(i));self.local(sid,123,22)
        channels=settings()['channels']
        self.expect('name-entry-and-add-create-eight-native-owned-channels',len(channels)==len(initial['channels'])+8
            and len({c['id'] for c in channels})==len(channels)
            and all(any(c['name']=='Release channel '+str(i) for c in channels) for i in range(8)))
        rendered=self.toms(sid)['client'];b=self.window(sid)['bounds']
        self.expect('native-channel-snapshot-matches-the-rendered-list',set(rendered['channelIds'])=={c['id'] for c in channels})
        self.call('scroll',x=b['x']+8+40,y=b['y']+24+80,amount=-1,ticks=6)
        self.expect('wheel-scroll-reaches-the-next-channel-row',self.toms(sid)['client']['scrollRow']==1)
        self.local(sid,20,40)
        self.expect('scrolled-list-click-selects-the-channel-drawn-there',settings()['selected']==rendered['channelIds'][1])
        sx,sy=b['x']+8+116,b['y']+24+126
        self.call('click',x=sx,y=sy,button=0,ticks=2)
        self.call('drag',x=sx,y=sy-91,dx=0,dy=-91,button=0,ticks=4)
        self.call('release',x=sx,y=sy-91,button=0,ticks=6)
        self.expect('native-scrollbar-drag-restores-the-first-row',self.toms(sid)['client']['scrollRow']==0)
        self.local(sid,20,40);selected=settings()['selected'];self.expect('visible-channel-selection-updates-the-real-connector',selected!='null')
        old=next(c for c in channels if c['id']==selected);self.local(sid,157,22);changed=settings()
        current=next(c for c in changed['channels'] if c['id']==selected)
        self.expect('native-privacy-toggle-keeps-owner-and-updates-channel-access',current['public']!=old['public'] and current['owner']==old['owner'])
        self.local(sid,140,22);deleted=settings()
        self.expect('delete-removes-only-the-selected-native-channel',len(deleted['channels'])==len(channels)-1 and not any(c['id']==selected for c in deleted['channels']))
        self.call('close',ticks=5);self.call('client-use-block',ticks=20)
        sid=self.menu(self.state('link-reopened'),'InventoryLinkMenu')['session'];persisted=settings()
        self.expect('native-channel-list-and-selection-persist-through-reopening',
            persisted['selected']==deleted['selected'] and sorted(persisted['channels'],key=lambda c:c['id'])==sorted(deleted['channels'],key=lambda c:c['id']))
        self.call('close',ticks=4)
    def terminal(self):
        self.call('close',ticks=3);self.call('fixture',block='toms_storage:storage_terminal',ticks=10)
        self.call('command',command='gamemode survival @a',ticks=4);self.call('command',command='clear @a',ticks=4)
        self.call('regression-toms-network',ticks=60);self.call('client-use-block',ticks=30)
        sid=self.menu(self.state('terminal-connected'),'StorageTerminalMenu')['session']
        def entries():return self.toms(sid)['client']['entries']
        def balance(signature):return sum(s['count'] for s in self.toms(sid)['chest'] if s['signature']==signature)
        def search(value):
            point=self.toms(sid)['client']['searchPoint']
            self.call('regression-tap',session=sid,x=point['x']+6,y=point['y']+5,ticks=4)
            self.call('key',key=65,modifiers=2,ticks=2);self.call('key',key=259,ticks=2);self.call('text',text=value,ticks=15)
        def grid(item=None):
            rendered=self.toms(sid)['client'];index=0
            if item:index=next(i for i,e in enumerate(rendered['entries']) if e['item']==item)
            g=rendered['grid'];index-=g['scrollRow']*g['columns'];self.expect('terminal-requested-cell-is-in-the-visible-grid',index>=0)
            self.call('regression-tap',session=sid,x=g['x']+(index%g['columns'])*18+8,y=g['y']+(index//g['columns'])*18+8,ticks=16)
        self.expect('real-connected-network-publishes-its-initial-stock',
            {e['item']:e['quantity'] for e in entries()}=={'minecraft:dirt':32,'minecraft:oak_planks':32})
        self.move(sid);search('dirt')
        self.expect('terminal-search-filters-the-real-catalogue',len(entries())==1 and entries()[0]['item']=='minecraft:dirt')
        search('no_such_release_item');self.expect('unmatched-search-hides-all-catalogue-cells',not entries())
        search('');self.expect('clearing-search-restores-connected-stock',len(entries())==2)
        self.call('inventory',ticks=4);self.seed(10,'minecraft:diamond',7)
        signature=self.state('terminal-seeded')['inventory'][10]['signature'];self.player(10);grid()
        deposited=self.state('terminal-deposited')
        self.expect('terminal-deposit-reaches-the-real-chest-once-with-complete-data',balance(signature)==7 and deposited['cursor']['count']==0)
        self.expect('terminal-receives-the-native-stock-increase',any(e['item']=='minecraft:diamond' and e['quantity']==7 for e in entries()))
        grid('minecraft:diamond');picked=self.state('terminal-withdrawn')
        self.expect('terminal-withdrawal-preserves-exact-count-and-components',picked['cursor']['signature']==signature and picked['cursor']['count']==7 and balance(signature)==0)
        self.player(11);returned=self.state('terminal-returned')
        self.expect('terminal-withdrawal-returns-once-to-separate-inventory',returned['cursor']['count']==0 and returned['inventory'][11]['signature']==signature and returned['inventory'][11]['count']==7)
        self.player(11);grid();self.call('close',ticks=5);self.call('client-use-block',ticks=25)
        sid=self.menu(self.state('terminal-reopened'),'StorageTerminalMenu')['session']
        self.expect('terminal-reopening-restores-persisted-native-stock',balance(signature)==7 and any(e['item']=='minecraft:diamond' and e['quantity']==7 for e in entries()))
        network=json.loads((self.profile/'release-toms-network.json').read_text())
        self.call('command',command=f"item replace block {network['x']} {network['y']} {network['z']} container.1 with minecraft:stone 64",ticks=50)
        self.expect('live-terminal-refreshes-after-an-external-native-storage-update',
            any(e['item']=='minecraft:stone' and e['quantity']==64 for e in entries()) and not any(e['item']=='minecraft:oak_planks' for e in entries()))
        grid('minecraft:diamond');self.call('regression-close',session=sid,ticks=6);closed=self.state('terminal-closed-with-cursor')
        self.expect('closing-terminal-with-withdrawal-keeps-every-item-on-the-shared-cursor',
            closed['cursor']['signature']==signature and closed['cursor']['count']==7 and balance(signature)==0)
        self.player(12);self.expect('closed-terminal-withdrawal-is-deposited-once',self.state('terminal-final')['inventory'][12]['signature']==signature and self.state('terminal-final-count')['inventory'][12]['count']==7)
        self.call('close',ticks=4)
    def vending(self):
        self.call('close',ticks=3);self.call('fixture',block='toms_trading_network:vending_machine',ticks=12)
        self.call('command',command='gamemode survival @a',ticks=4)
        self.call('command',command='clear @a',ticks=4)
        self.call('client-use-block',ticks=20)
        initial=self.state('vending-owner-empty');owner=self.menu(initial,'VendingMachineConfigMenu')['session']
        self.call('inventory',ticks=5)
        for slot,item,ghost in [(9,'minecraft:emerald',0),(10,'minecraft:diamond',4)]:
            self.seed(slot,item,1);self.player(slot);self.slot(owner,ghost);self.player(slot)
        self.seed(9,'minecraft:emerald',12);self.seed(10,'minecraft:diamond',6)
        self.player(10);self.slot(owner,8)
        stock=self.state('vending-stocked');ledger=stock['nativeHost']['tomsVending']
        self.expect('vending-uses-real-stock-and-noncreative-offer',not ledger['creative']
            and ledger['config'][0]['item']=='minecraft:emerald' and ledger['config'][0]['count']==1
            and ledger['config'][4]['item']=='minecraft:diamond' and ledger['config'][4]['count']==1
            and self.count(ledger['inputs'],'minecraft:diamond')==6 and stock['cursor']['count']==0)
        original_config=ledger['config']
        self.button('Trading View');customer=self.state('vending-customer');sid=self.menu(customer,'VendingMachineTradingMenu')['session']
        # SIU keeps independent native windows open. A customer-view button need
        # not close the owner's existing source; test the native customer menu
        # and its real backing stock rather than impose single-screen lifetime.
        self.expect('native-trading-view-opens-one-customer-source-for-the-same-stock',
            sum(menu['type'].endswith('.VendingMachineTradingMenu') for menu in customer['menus'])==1
            and customer['nativeHost']['tomsVending']==ledger)
        self.call('wait',ticks=3)
        current=next(window for window in self.client.get('allWindows',self.client.get('windows',[])) if window.get('sessionId')==sid)
        if current.get('locked') in [True,'true']:self.call('regression-control',session=sid,control='LOCK',ticks=3)
        bounds=current['bounds'].copy()
        self.call('regression-move',session=sid,dx=24,dy=12,ticks=5)
        self.call('wait',ticks=3)
        moved=next(window for window in self.client.get('allWindows',self.client.get('windows',[])) if window.get('sessionId')==sid)
        self.expect('customer-window-moves-before-the-native-trade-click',moved['bounds']!=bounds)
        self.button('Trade');purchase=self.state('vending-purchase');stock=purchase['nativeHost']['tomsVending']
        self.expect('purchase-consumes-payment-and-stock-on-the-server',
            self.count(purchase['inventory'],'minecraft:emerald')==11 and self.count(purchase['inventory'],'minecraft:diamond')==1
            and self.count(stock['inputs'],'minecraft:diamond')==5 and self.count(stock['outputs'],'minecraft:emerald')==1
            and purchase['cursor']['count']==0)
        diamond=next(cell for cell in stock['inputs'] if cell['item']=='minecraft:diamond')
        emerald=next(cell for cell in stock['outputs'] if cell['item']=='minecraft:emerald')
        self.expect('purchase-retains-complete-native-stack-data',
            all(cell['signature']==diamond['signature'] for cell in purchase['inventory'] if cell['item']=='minecraft:diamond')
            and all(cell['signature']==emerald['signature'] for cell in purchase['inventory'] if cell['item']=='minecraft:emerald'))
        self.call('command',command='clear @a minecraft:emerald',ticks=4)
        denied_before=self.state('vending-no-payment-before');self.button('Trade');denied=self.state('vending-no-payment-after')
        self.expect('native-refused-purchase-cannot-create-output-or-consume-stock',
            denied['inventory']==denied_before['inventory'] and denied['cursor']==denied_before['cursor']
            and denied['nativeHost']['tomsVending']==denied_before['nativeHost']['tomsVending'])
        self.call('close',ticks=4);self.call('client-use-block',ticks=18)
        reopened=self.state('vending-reopened');self.menu(reopened,'VendingMachineConfigMenu')
        self.expect('offer-payment-and-stock-persist-through-native-reopening',
            reopened['nativeHost']['tomsVending']==denied['nativeHost']['tomsVending']
            and reopened['nativeHost']['tomsVending']['config']==original_config)
        self.call('close',ticks=4)


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('profile',type=Path)
    parser.add_argument('--only',choices=['vending','configurator','link','terminal'],required=True);args=parser.parse_args()
    getattr(Checks(args.profile),args.only)()


if __name__=='__main__':main()
