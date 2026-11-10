"""Ordinary native merchant use, exact payment/refusal/output and owner persistence."""
from pathlib import Path
import hashlib,json,sys
import nbtlib
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
from mowzie_features import stack,item_count


def inventory(state):return tuple(stack(v['item'])for v in state['inventory'])
def empty(value):return not nbtlib.parse_nbt(value)
def empty_cursors(state):return all(empty(v['item'])for v in state['cursors'])


def plain_emerald(text):
    value=nbtlib.parse_nbt(text)
    if not value:return False
    key='Count'if 'Count'in value else'count'
    for optional in ['tag','components','ForgeCaps']:
        if optional in value and isinstance(value[optional],nbtlib.Compound)and not value[optional]:del value[optional]
    return set(value)=={'id',key} and item_count(text)[0]=='minecraft:emerald'and 0<item_count(text)[1]<=61


class RedMerchant:
    def __init__(self,profile,mc,loader):
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.n=0;self.checks=[]
        self.command='server-command'if loader=='neoforge'else'command';self.out=self.profile/'red-merchant-release';self.out.mkdir()
    def act(self,kind,**kw):return send(self.profile,kind,**kw)
    def cmd(self,text):self.act(self.command,command=text,ticks=7)
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=['supplementaries-red-merchant'],checks=self.checks,
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            scope='Actual spawned Red Merchant and native randomized offers through the builtin four-row trading window: scroll, selection, refusal, exact payment/output/uses, moved slots, owner reopening and entity invalidation. No fabricated offers or every trade/restocking/progression claim.'),indent=2)+'\n')
    def check(self,name,value):
        self.checks.append(dict(name=name,passed=bool(value)));self.save();assert value,name;print('PASS',name,flush=True)
    def state(self,label,ledger=False):
        self.act('regression-red-merchant-ledger'if ledger else'regression-red-merchant-state',ticks=6)
        data=json.loads((self.profile/'release-red-merchant-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(data,indent=2)+'\n');return data
    def enter(self):self.act('regression-use-entity',entity='supplementaries:red_merchant',shift=False,ticks=25)
    def inv(self,index):self.act('regression-player-click',slot=index,ticks=10)
    def slot(self,state,index):self.act('regression-slot-click',session=state['client']['session'],slot=index,ticks=12)
    def run(self):
        self.act('close',ticks=3);self.act('fixture',block='minecraft:air',ticks=4)
        for command in ['difficulty normal','gamemode survival @p','clear @p','kill @e[type=!minecraft:player]',
            'summon supplementaries:red_merchant 8.5 65 8.5 {NoAI:1b,PersistenceRequired:1b}', 'tp @p 8.5 65 10.5 180 0']:
            self.cmd(command)
        self.enter();initial=self.state('initial');sid=initial['client']['session']
        self.check('native-red-merchant-enters-builtin-bridge-with-original-offers',initial['menus']==1 and initial['client']['menu']=='net.mehvahdjukaar.supplementaries.common.inventories.RedMerchantMenu'and initial['client']['offers']==initial['offers']and len(initial['offers'])>4 and all(empty(v)for v in initial['cells']))
        self.act('regression-red-merchant-row',scroll=-1,ticks=10);down=self.state('scrolled-down')
        self.check('native-offer-list-scrolls-one-entry-without-item-mutation',down['client']['scroll']==initial['client']['scroll']+1 and inventory(down)==inventory(initial)and empty_cursors(down))
        self.act('regression-red-merchant-row',scroll=1,ticks=10);top=self.state('scrolled-top')
        self.check('reverse-scroll-restores-first-four-offers',top['client']['scroll']==0)
        offers=[v for v in top['offers']if not v['outOfStock']and plain_emerald(v['costA'])and empty(v['costB'])and not empty(v['result'])]
        self.check('original-offers-provide-an-unmodified-payable-native-trade',bool(offers));offer=offers[0];index=offer['index']
        for _ in range(max(0,index-3)):
            self.act('regression-red-merchant-row',scroll=-1,ticks=8)
        self.act('regression-red-merchant-row',index=index,ticks=12);selected=self.state('selected')
        self.check('actual-row-click-selects-requested-native-offer-with-empty-output',selected['client']['selection']==index and all(empty(v)for v in selected['cells']))
        if selected['client']['locked']:self.act('regression-control',session=sid,control='LOCK',ticks=4)
        self.act('regression-move',session=sid,dx=16,dy=12,ticks=4);moved=self.state('moved')
        self.check('trading-window-moves-before-slot-interactions',moved['client']['bounds']!=selected['client']['bounds'])
        data='{release_red_merchant_marker:71,typed:[B;1b,5b]}'
        suffix=data if self.mc=='1.20.1'else'[minecraft:custom_data='+data+']'
        self.cmd('item replace entity @p container.10 with minecraft:diamond'+suffix+' 3');foreign=self.state('foreign-fixture')
        self.inv(10);self.slot(foreign,0);refused=self.state('foreign-payment')
        if foreign['foreignAccepted']:
            self.check('native-invalid-payment-is-not-a-trade',stack(refused['cells'][0])==stack(foreign['inventory'][10]['item'])and empty(refused['cells'][2])and refused['offers'][index]['uses']==offer['uses'])
            self.slot(refused,0)
        else:
            self.check('native-payment-slot-refusal-keeps-full-cursor',empty(refused['cells'][0])and empty(refused['cells'][2])and any(stack(v['item'])==stack(foreign['inventory'][10]['item'])for v in refused['cursors']))
        self.inv(10);recovered=self.state('foreign-recovered')
        self.check('invalid-payment-recovers-exactly-once',inventory(recovered)==inventory(foreign)and empty_cursors(recovered)and all(empty(v)for v in recovered['cells']))
        cost=item_count(offer['costA'])[1];self.cmd('item replace entity @p container.9 with minecraft:emerald '+str(cost+3));funded=self.state('funded')
        self.inv(9);self.slot(funded,0);ready=self.state('ready')
        self.check('actual-payment-unlocks-exact-native-result',stack(ready['cells'][0])==stack(offer['costA'],cost+3)and stack(ready['cells'][2])==stack(offer['result'])and empty_cursors(ready))
        self.slot(ready,2);self.inv(11);traded=self.state('traded')
        self.check('result-pickup-charges-native-cost-and-increments-uses-once',stack(traded['inventory'][11]['item'])==stack(offer['result'])and stack(traded['cells'][0])==stack(offer['costA'],3)and traded['offers'][index]['uses']==offer['uses']+1 and empty_cursors(traded))
        self.slot(traded,0);self.inv(9);settled=self.state('settled');expected=list(inventory(funded));expected[9]=stack(offer['costA'],3);expected[11]=stack(offer['result'])
        self.check('full-inventory-ledger-has-only-paid-result-and-exact-change',inventory(settled)==tuple(expected)and empty_cursors(settled)and all(empty(v)for v in settled['cells']))
        self.act('regression-control',session=sid,control='CLOSE',ticks=14);self.enter();reopened=self.state('reopened')
        self.check('same-native-trader-retains-uses-and-settled-items-on-reopening',reopened['owner']==initial['owner']and reopened['offers'][index]['uses']==offer['uses']+1 and stack(reopened['offers'][index]['result'])==stack(offer['result'])and inventory(reopened)==inventory(settled)and empty_cursors(reopened))
        # Keep unrelated native death loot from entering this ownership ledger.
        # This does not change offers, selection, payment or trade callbacks.
        self.cmd('gamerule doMobLoot false');self.cmd('kill @e[type=supplementaries:red_merchant]');self.act('wait',ticks=16);invalid=self.state('entity-removed',True)
        self.check('entity-invalidation-closes-owned-trade-without-item-loss',invalid['menus']==0 and inventory(invalid)==inventory(settled)and empty_cursors(invalid))
        self.cmd('gamerule doMobLoot true');self.act('close',ticks=3);self.save(True)
        return dict(cases=1,failed=0,checks=len(self.checks),red_merchant_features_checked=True)


def run(profile,mc,loader,*args,**kw):return RedMerchant(profile,mc,loader).run()
