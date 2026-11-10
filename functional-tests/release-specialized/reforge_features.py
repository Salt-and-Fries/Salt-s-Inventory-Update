"""Reforge with actual native materials and an unmodified random quality roll."""
from pathlib import Path
import copy,hashlib,json,sys
import nbtlib

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
from mowzie_features import stack,typed


def without_quality(text,kind):
    """Only publisher-documented quality fields may change; all else is exact."""
    value=copy.deepcopy(nbtlib.parse_nbt(text));assert str(value['id'])=='minecraft:diamond_sword'
    if 'Count'in value:
        tag=value.get('tag',nbtlib.Compound());tag.pop('Tiered',None)
        if kind=='tierify':tag.pop('itemborders_colors',None)
    else:value.get('components',nbtlib.Compound()).pop('tiered:tier',None)
    return typed(value)


class Reforge:
    def __init__(self,profile,mc,loader,kind):
        assert loader=='fabric'and mc in ['1.20.1','1.21.1']and kind in ['tiered','tierify']
        self.profile=Path(profile);self.mc=mc;self.kind=kind;self.checks=[];self.n=0
        self.out=self.profile/'reforge-release';self.out.mkdir()
    def act(self,command_type,**kw):return send(self.profile,command_type,**kw)
    def cmd(self,text):self.act('command',command=text,ticks=6)
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=[self.kind+'-reforge'],checks=self.checks,
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            scope='Original LibZ anvil tab, native readiness/refusal, complete ingredients, actual random reforge through moved UI, exact material consumption and non-quality data, result recovery/reopening and return to anvil. No RNG/config mutation or guarantee of a particular random quality.'),indent=2)+'\n')
    def check(self,name,value):
        self.checks.append(dict(name=name,passed=bool(value)));self.save();assert value,name;print('PASS',name,flush=True)
    def state(self,label):
        self.act('regression-reforge-state',ticks=4);d=json.loads((self.profile/'release-reforge-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(d,indent=2)+'\n');return d
    @staticmethod
    def empty(text):return not nbtlib.parse_nbt(text)
    @staticmethod
    def inventory(d):return [stack(v['item'])for v in d['inventory']]
    def inv(self,index):self.act('regression-player-click',slot=index,ticks=10)
    def slot(self,index,sid):self.act('regression-slot-click',session=sid,slot=index,ticks=10)
    def seed(self,index,item,count,marker,damage=False):
        data='{release_reforge_marker:'+str(marker)+',typed:[B;4b,8b]}'
        tail=(data[:-1]+',Damage:0}'if damage else data)if self.mc=='1.20.1'else('[minecraft:custom_data='+data+(',minecraft:damage=0'if damage else'')+']')
        self.cmd(f'item replace entity @p container.{index} with {item}{tail} {count}')
    def enter(self):
        self.act('client-use-block',ticks=16);self.tab('minecraft:anvil',37,-12)
    def tab(self,parent,x,y):
        state=self.act('wait',ticks=3)['client'];windows=state.get('windows',state.get('allWindows',[]))
        rows=[v for v in windows if v.get('menuId')==parent];assert len(rows)==1,('Missing native tab parent',parent,rows)
        sid=rows[0].get('sessionId',rows[0].get('session'));assert sid is not None
        self.act('regression-template-region-tap',session=sid,region='tabs',source_x=x,source_y=y,ticks=18)
    def run(self):
        self.act('close',ticks=3);self.act('fixture',block='minecraft:anvil',ticks=5);self.cmd('gamemode survival @p');self.cmd('clear @p');self.enter()
        initial=self.state('initial');sid=initial['client']['session']
        expected='elocindev.tierify.screen.client.ReforgeScreen'if self.kind=='tierify'else'draylar.tiered.reforge.ReforgeScreen'
        self.check('original-libz-tab-opens-the-native-three-cell-reforge',initial['client']['screen']==expected and len(initial['cells'])==3 and not initial['ready'])
        self.seed(9,'minecraft:diamond',3,71);self.seed(10,'minecraft:diamond_sword',1,72,damage=True)
        self.seed(11,'tiered:limestone_chunk'if self.kind=='tierify'else'minecraft:amethyst_shard',3,73)
        self.seed(12,'minecraft:dirt',2,74);seeded=self.state('seeded')
        self.inv(12);self.slot(2,sid);refused=self.state('foreign-refused')
        self.check('native-addition-slot-refuses-foreign-item-with-full-cursor',self.empty(refused['cells'][2]['item'])and any(stack(v['item'])==stack(seeded['inventory'][12]['item'])for v in refused['cursors']))
        self.inv(12);self.inv(9);self.slot(0,sid);self.inv(10);self.slot(1,sid);partial=self.state('missing-addition')
        self.check('incomplete-native-prerequisites-disable-reforge',not partial['ready']and len(partial['client']['buttons'])==1 and partial['client']['buttons'][0]['disabled'])
        self.inv(11);self.slot(2,sid);ready=self.state('ready')
        self.check('actual-native-materials-enable-reforge-with-no-forced-readiness',ready['ready']and len(ready['client']['buttons'])==1 and not ready['client']['buttons'][0]['disabled'])
        if ready['client']['locked']:self.act('regression-control',session=sid,control='LOCK',ticks=3)
        self.act('regression-move',session=sid,dx=18,dy=12,ticks=3);moved=self.state('moved')
        self.check('reforge-window-moves-before-the-native-action',moved['client']['bounds']!=ready['client']['bounds'])
        b=moved['client']['buttons'][0];self.act('regression-template-widget-tap',session=sid,widget_type=b['type'],message=b['message'],ticks=18)
        forged=self.state('forged')
        self.check('native-reforge-consumes-one-of-each-actual-material',stack(forged['cells'][0]['item'])==stack(ready['cells'][0]['item'],2)and stack(forged['cells'][2]['item'])==stack(ready['cells'][2]['item'],2))
        self.check('native-random-quality-is-registered-and-preserves-all-other-data',bool(forged['quality'])and forged['quality']in forged['knownQualities']and without_quality(forged['cells'][1]['item'],self.kind)==without_quality(ready['cells'][1]['item'],self.kind))
        self.check('reforge-does-not-touch-other-player-items-or-cursor',self.inventory(forged)==self.inventory(ready)and all(self.empty(v['item'])for v in forged['cursors']))
        for index,target in [(0,9),(1,10),(2,11)]:self.slot(index,sid);self.inv(target)
        recovered=self.state('recovered');expected_inventory=self.inventory(seeded)
        for index,target in [(0,9),(1,10),(2,11)]:expected_inventory[target]=stack(forged['cells'][index]['item'])
        self.check('exact-reforged-equipment-and-remainders-recover-once',self.inventory(recovered)==expected_inventory and all(self.empty(v['item'])for v in recovered['cells'])and all(self.empty(v['item'])for v in recovered['cursors']))
        self.act('close',ticks=6);self.enter();reopened=self.state('reopened')
        self.check('native-reopening-preserves-the-exact-reforged-result',self.inventory(reopened)==expected_inventory and all(self.empty(v['item'])for v in reopened['cells']))
        self.tab('tiered:reforge',11,-12)
        state=self.act('wait',ticks=6)['client'];self.check('first-libz-tab-returns-to-original-anvil',any(v.get('menuId')=='minecraft:anvil'for v in state.get('windows',state.get('allWindows',[]))))
        self.act('close',ticks=3);self.save(True)
        return dict(cases=1,failed=0,checks=len(self.checks),reforge_features_checked=True)


def run(profile,mc,loader,kind='tiered',*args,**kw):return Reforge(profile,mc,loader,kind).run()
