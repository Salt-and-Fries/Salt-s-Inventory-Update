"""Native linking and grinding with exact paid attempts and item ownership.

Up to three distinct, fully paid native linking attempts are exercised. This is
not a retry of an unanswered or failed SIU input. Native probability and RNG are
unchanged. Every unsuccessful roll must still preserve equipment and pay the
documented cost. A successful link is required before grinding is accepted.
"""
from pathlib import Path
import collections,copy,hashlib,json,sys
import nbtlib

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
from mowzie_features import stack,typed,item_count


def without_gems(text):
    value=copy.deepcopy(nbtlib.parse_nbt(text));assert str(value['id'])=='minecraft:iron_sword'
    if 'Count'in value:value.get('tag',nbtlib.Compound()).pop('Gems',None)
    else:
        component=value.get('components',nbtlib.Compound()).get('smitherz:gems')
        assert isinstance(component,nbtlib.Compound)and int(component['size'])==3
        component.pop('gems',None)
    return typed(value)


def paid_tool(before,properties):
    value=copy.deepcopy(nbtlib.parse_nbt(before));assert value
    key='Count'if'Count'in value else'count';count=int(value[key])
    if not properties['damageable']:
        if count==1:return stack('{}')
        value[key]=type(value[key])(count-1)
    else:
        assert count==1 and properties['damage']+1<properties['maxDamage'],'Use an unenchanted unbroken native tool fixture'
        if key=='Count':value.setdefault('tag',nbtlib.Compound())['Damage']=nbtlib.Int(properties['damage']+1)
        else:value.setdefault('components',nbtlib.Compound())['minecraft:damage']=nbtlib.Int(properties['damage']+1)
    return typed(value)


class Smither:
    def __init__(self,profile,mc,loader):
        assert loader=='fabric'and mc in ['1.20.1','1.21.1']
        self.profile=Path(profile);self.mc=mc;self.n=0;self.checks=[];self.completed=[]
        self.out=self.profile/'smitherz-release';self.out.mkdir()
    def act(self,command_type,**kw):return send(self.profile,command_type,**kw)
    def cmd(self,text):self.act('command',command=text,ticks=6)
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.completed,checks=self.checks,
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            scope='Native LibZ linking/grinding tabs, three actual sockets, refused input, up to three distinct paid unmodified-probability link attempts, exact tool/gem ownership, moved action, cancellation, result pickup and full recovery. No modified RNG/config or assumed successful random roll. Legacy native recovered-gem behavior must meet the same strict ledger or receive a separately reproduced upstream disposition.'),indent=2)+'\n')
    def check(self,name,value):
        self.checks.append(dict(name=name,passed=bool(value)));self.save();assert value,name;print('PASS',name,flush=True)
    def state(self,label):
        self.act('regression-smitherz-state',ticks=4);d=json.loads((self.profile/'release-smitherz-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(d,indent=2)+'\n');return d
    @staticmethod
    def empty(text):return not nbtlib.parse_nbt(text)
    @staticmethod
    def inventory(d):return [stack(v['item'])for v in d['inventory']]
    @staticmethod
    def empty_cursors(d):return all(not nbtlib.parse_nbt(v['item'])for v in d['cursors'])
    def inv(self,index):self.act('regression-player-click',slot=index,ticks=10)
    def slot(self,index,sid):self.act('regression-slot-click',session=sid,slot=index,ticks=10)
    def tab(self,parent,x):
        state=self.act('wait',ticks=3)['client'];peers=[v for v in state.get('windows',state.get('allWindows',[]))if v.get('menuId')==parent];assert len(peers)==1,parent
        sid=peers[0].get('sessionId',peers[0].get('session'));assert sid is not None
        self.act('regression-template-region-tap',session=sid,region='tabs',source_x=x,source_y=-12,ticks=18)
    def enter(self,parent):self.act('client-use-block',ticks=16);self.tab(parent,37)
    def seed(self,index,item,marker,extra=''):
        if self.mc=='1.20.1':tail='{release_smither_marker:'+str(marker)+',typed:[B;5b,9b]'+extra+'}'
        else:tail='[minecraft:custom_data={release_smither_marker:'+str(marker)+',typed:[B;5b,9b]}'+extra+']'
        self.cmd(f'item replace entity @p container.{index} with {item}{tail}')
    def run(self):
        self.act('close',ticks=3);self.act('fixture',block='minecraft:smithing_table',ticks=5);self.cmd('gamemode survival @p');self.cmd('clear @p');self.enter('minecraft:smithing')
        initial=self.state('smither-initial');sid=initial['client']['session']
        self.check('original-linking-tab-and-disabled-empty-action',initial['client']['screen']=='net.smitherz.screen.SmitherScreen'and len(initial['client']['buttons'])==1 and initial['client']['buttons'][0]['disabled'])
        self.seed(9,'minecraft:iron_sword',81,',GemSlots:3,Gems:[]'if self.mc=='1.20.1'else',smitherz:gems={gems:[],size:3}')
        gem='smitherz:strength_1_gem'if self.mc=='1.20.1'else'smitherz:gem'
        for n,(tool_slot,gem_slot)in enumerate([(10,11),(12,13),(14,15)]):
            self.seed(tool_slot,'smitherz:smither_hammer_4',82+2*n)
            self.seed(gem_slot,gem,83+2*n,''if self.mc=='1.20.1'else',smitherz:gem="smitherz:strength_1"')
        self.seed(17,'minecraft:diamond',89);seeded=self.state('smither-seeded');expected=self.inventory(seeded)
        self.inv(9);self.slot(0,sid);equipment=self.state('three-sockets');expected[9]=stack('{}')
        self.check('native-equipment-enables-three-original-gem-sockets',sum(v['gemSlot']and v['active']for v in equipment['cells'])==3 and stack(equipment['cells'][0]['item'])==stack(seeded['inventory'][9]['item']))
        self.inv(17);self.slot(1,sid);refused=self.state('foreign-hammer-refused')
        self.check('native-hammer-cell-refuses-foreign-diamond-with-exact-cursor',self.empty(refused['cells'][1]['item'])and any(stack(v['item'])==stack(seeded['inventory'][17]['item'])for v in refused['cursors']))
        self.inv(17)
        if equipment['client']['locked']:self.act('regression-control',session=sid,control='LOCK',ticks=3)
        self.act('regression-move',session=sid,dx=18,dy=12,ticks=3);moved=self.state('smither-moved')
        self.check('linking-window-moves-before-native-action',moved['client']['bounds']!=equipment['client']['bounds'])
        linked=None;chosen=None
        for n,(tool_slot,gem_slot)in enumerate([(10,11),(12,13),(14,15)],1):
            self.inv(tool_slot);self.slot(1,sid);self.inv(gem_slot);self.slot(2,sid);ready=self.state('attempt-'+str(n)+'-ready')
            self.check('attempt-'+str(n)+'-actual-native-prerequisites-enable-action',not ready['client']['buttons'][0]['disabled']and not ready['cells'][2]['linked'])
            expected[tool_slot]=stack('{}');expected[gem_slot]=stack('{}')
            button=ready['client']['buttons'][0];self.act('regression-template-widget-tap',session=sid,widget_type=button['type'],message=button['message'],ticks=18)
            outcome=self.state('attempt-'+str(n)+'-outcome')
            self.check('attempt-'+str(n)+'-charges-the-exact-native-tool-cost',stack(outcome['cells'][1]['item'])==paid_tool(ready['cells'][1]['item'],ready['tool']))
            self.check('attempt-'+str(n)+'-preserves-equipment-and-non-gem-data',without_gems(outcome['cells'][0]['item'])==without_gems(ready['cells'][0]['item'])and len(outcome['gems'])in [0,1])
            if outcome['gems']:
                self.check('successful-native-link-owns-exactly-the-original-gem',stack(outcome['gems'][0])==stack(ready['cells'][2]['item'])and outcome['cells'][2]['linked'])
                linked=outcome['cells'][0]['item'];chosen=outcome['gems'][0]
            else:self.check('failed-native-roll-consumes-the-gem-without-projecting-a-duplicate',self.empty(outcome['cells'][2]['item'])and not outcome['cells'][2]['linked'])
            if not self.empty(outcome['cells'][1]['item']):self.slot(1,sid);self.inv(tool_slot)
            expected[tool_slot]=stack(outcome['cells'][1]['item']);settled=self.state('attempt-'+str(n)+'-settled')
            self.check('attempt-'+str(n)+'-settles-all-other-player-items-and-cursors-exactly',self.inventory(settled)==expected and self.empty_cursors(settled))
            if linked is not None:break
        self.check('at-least-one-real-link-completes-within-three-distinct-paid-attempts',linked is not None)
        self.slot(0,sid);self.inv(9);expected[9]=stack(linked);recovered=self.state('linked-recovered')
        self.check('linked-equipment-recovers-once-with-no-physical-projection-duplicate',self.inventory(recovered)==expected and all(self.empty(v['item'])for v in recovered['cells'])and self.empty_cursors(recovered))
        self.act('close',ticks=6);self.enter('minecraft:smithing');reopened=self.state('link-reopened')
        self.check('linked-full-item-persists-through-reopening',self.inventory(reopened)==expected and all(self.empty(v['item'])for v in reopened['cells']))
        self.tab('smitherz:smither',11);self.completed.append('smitherz-smither');self.save()
        self.act('close',ticks=4);self.act('fixture',block='minecraft:grindstone',ticks=5);self.enter('minecraft:grindstone')
        grinder=self.state('grinder-initial');sid=grinder['client']['session'];self.check('original-grinding-tab-keeps-three-native-cells',grinder['client']['screen']=='net.smitherz.screen.GrinderScreen'and len(grinder['cells'])==3)
        self.seed(16,'smitherz:extraction_hammer_4',90);grinder_seeded=self.state('grinder-tool');expected=self.inventory(grinder_seeded)
        self.inv(9);self.slot(0,sid);self.inv(16);self.slot(1,sid);preview=self.state('grinder-preview')
        self.check('native-strength-one-extraction-previews-full-equipment-and-original-gem',without_gems(preview['cells'][2]['item'])==without_gems(linked)and len(preview['pendingReturnedGems'])==1 and stack(preview['pendingReturnedGems'][0])==stack(chosen))
        self.slot(0,sid);self.inv(9);self.slot(1,sid);self.inv(16);cancelled=self.state('grinder-cancelled')
        self.check('cancelled-grinding-keeps-all-original-ingredients-without-output',self.inventory(cancelled)==expected and all(self.empty(v['item'])for v in cancelled['cells'])and self.empty_cursors(cancelled))
        self.inv(9);self.slot(0,sid);self.inv(16);self.slot(1,sid);ready=self.state('grinder-ready')
        self.slot(2,sid);self.inv(9);picked=self.state('grinder-output-picked')
        self.check('grinding-takes-the-exact-preview-and-charges-the-native-tool',stack(picked['inventory'][9]['item'])==stack(ready['cells'][2]['item'])and stack(picked['cells'][1]['item'])==paid_tool(ready['cells'][1]['item'],ready['tool'])and self.empty(picked['cells'][0]['item']))
        if not self.empty(picked['cells'][1]['item']):self.slot(1,sid);self.inv(16)
        settled=self.state('grinder-settled');expected[9]=stack(ready['cells'][2]['item']);expected[16]=stack(picked['cells'][1]['item'])
        # Native player-inventory insertion may choose any empty cell. Remove
        # exactly one empty cell from the expected multiset and add the exact
        # recovered gem; no arbitrary item or data loss is allowed.
        bag=collections.Counter(expected);bag[stack('{}')]-=1;bag[stack(chosen)]+=1
        self.check('native-grinding-returns-the-exact-guaranteed-gem-once',collections.Counter(self.inventory(settled))==bag and all(self.empty(v['item'])for v in settled['cells'])and self.empty_cursors(settled))
        self.tab('smitherz:grinder',11);self.completed.append('smitherz-grinder');self.act('close',ticks=3);self.save(True)
        return dict(cases=2,failed=0,checks=len(self.checks),smitherz_features_checked=True)


def run(profile,mc,loader,*args,**kw):return Smither(profile,mc,loader).run()
