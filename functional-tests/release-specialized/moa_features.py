"""Ordinary native adult Moa equipment, chest attachment and cargo interactions."""
from pathlib import Path
import hashlib,json,sys
import nbtlib

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
from mowzie_features import stack,item_count


class Moa:
    def __init__(self,profile,mc,loader):
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.n=0;self.checks=[]
        self.command='server-command'if loader=='neoforge'else'command'
        self.out=self.profile/'moa-release';self.out.mkdir()
    def act(self,command_type,**kw):return send(self.profile,command_type,**kw)
    def cmd(self,text,ticks=6):self.act(self.command,command=text,ticks=ticks)
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=['bf-moa-clean'],checks=self.checks,
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            scope='Ordinary adult Moa chest attachment and inventory entry; saddle/armor refusal and server equipment effect, fifteen cargo hits, moved input, exact recovery, same-owner reopening and range invalidation. No flight/combat or every armor variant.'),indent=2)+'\n')
    def check(self,name,value):
        self.checks.append(dict(name=name,passed=bool(value)));self.save();assert value,name;print('PASS',name,flush=True)
    def state(self,label):
        self.act('regression-moa-state',ticks=4);d=json.loads((self.profile/'release-moa-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(d,indent=2)+'\n');return d
    @staticmethod
    def empty(text):return not nbtlib.parse_nbt(text)
    @staticmethod
    def inventory(d):return [stack(v['item'])for v in d['inventory']]
    @staticmethod
    def cells(d):return [stack(v['item'])for v in d['cells']]
    def inv(self,index):self.act('regression-player-click',slot=index,ticks=10)
    def slot(self,index,sid,button=0):self.act('regression-slot-click',session=sid,slot=index,button=button,ticks=10)
    def seed(self,index,item,count,marker,damage=None):
        data='{release_moa_marker:'+str(marker)+',typed:[B;3b,9b]}'
        if self.mc=='1.20.1':tail=data if damage is None else data[:-1]+',Damage:'+str(damage)+'}'
        else:tail='[minecraft:custom_data='+data+(''if damage is None else',minecraft:damage='+str(damage))+']'
        self.cmd(f'item replace entity @p container.{index} with {item}{tail} {count}')
    def enter(self,shift=True):self.act('regression-use-entity',entity='aether:moa',shift=shift,ticks=24)
    def run(self):
        self.act('close',ticks=3);self.act('fixture',block='minecraft:air',ticks=4)
        for text in ['kill @e[type=!minecraft:player]','gamemode survival @p','clear @p','difficulty normal',
            'fill 5 64 6 16 64 35 minecraft:stone',
            'summon aether:moa 8.5 65 10.5 {PlayerGrown:1b,Age:0,NoAI:1b,NoGravity:1b,PersistenceRequired:1b}',
            'tp @p 8.5 65 12.5 180 5','item replace entity @p weapon.mainhand with minecraft:chest']:
            self.cmd(text)
        # The addon attaches a real chest on ordinary non-sneaking use and
        # consumes it in survival. No capability or inventory setter is used.
        self.enter(False);self.enter();initial=self.state('initial');sid=initial['client']['session']
        self.check('ordinary-chest-attachment-is-consumed-and-opens-seventeen-original-cells',initial['client']['menu']=='com.aetherteam.protect_your_moa.inventory.menu.MoaInventoryMenu'and initial['client']['screen']=='com.aetherteam.protect_your_moa.client.gui.screen.inventory.MoaInventoryScreen'and len(initial['cells'])==17 and all(self.empty(v['item'])for v in initial['cells'])and self.empty(initial['inventory'][0]['item']))
        self.seed(9,'minecraft:saddle',1,61)
        self.seed(10,'aether_protect_your_moa:iron_moa_armor',1,62,damage=2)
        self.seed(11,'minecraft:diamond',3,63);self.seed(12,'minecraft:stone',15,64);seeded=self.state('seeded')
        self.inv(11)
        for i in [0,1]:
            self.slot(initial['cells'][i]['index'],sid);refused=self.state('equipment-refused-'+str(i))
            self.check('native-equipment-cell-'+str(i)+'-refuses-foreign-item-with-exact-cursor',self.empty(refused['cells'][i]['item'])and any(stack(v['item'])==stack(seeded['inventory'][11]['item'])for v in refused['cursors']))
        self.inv(11)
        for i,source in [(0,9),(1,10)]:self.inv(source);self.slot(initial['cells'][i]['index'],sid)
        equipped=self.state('equipped')
        self.check('native-saddle-and-armor-apply-to-original-owner-once',equipped['saddled']and stack(equipped['armor'])==stack(seeded['inventory'][10]['item'])and all(stack(equipped['cells'][i]['item'])==stack(seeded['inventory'][9+i]['item'])for i in [0,1])and all(self.empty(equipped['inventory'][i]['item'])for i in [9,10])and all(self.empty(v['item'])for v in equipped['cursors']))
        self.inv(12)
        for cell in initial['cells'][2:]:self.slot(cell['index'],sid,1)
        cargo=self.state('cargo-fifteen')
        self.check('all-fifteen-native-cargo-hits-retain-complete-typed-stacks',all(item_count(v['item'])==('minecraft:stone',1)and stack(v['item'])==stack(seeded['inventory'][12]['item'],1)for v in cargo['cells'][2:])and self.empty(cargo['inventory'][12]['item'])and all(self.empty(v['item'])for v in cargo['cursors']))
        if cargo['client']['locked']:self.act('regression-control',session=sid,control='LOCK',ticks=3)
        self.act('regression-move',session=sid,dx=17,dy=11,ticks=3);moved=self.state('moved')
        self.check('moa-window-moves-before-native-last-cell-input',moved['client']['bounds']!=cargo['client']['bounds'])
        self.slot(initial['cells'][-1]['index'],sid);self.inv(13);withdrawn=self.state('last-withdrawn')
        self.check('moved-last-cargo-cell-withdraws-exactly-one-tagged-stone',self.empty(withdrawn['cells'][-1]['item'])and stack(withdrawn['inventory'][13]['item'])==stack(seeded['inventory'][12]['item'],1))
        self.inv(13);self.slot(initial['cells'][-1]['index'],sid);restored=self.state('restored')
        self.check('moved-cargo-can-return-to-original-cell',self.cells(restored)==self.cells(cargo)and self.inventory(restored)==self.inventory(cargo))
        self.act('close',ticks=8);self.enter();reopened=self.state('reopened');sid=reopened['client']['session']
        self.check('same-owner-equipment-and-cargo-persist-through-native-reopening',reopened['owner']==initial['owner']and self.cells(reopened)==self.cells(restored)and self.inventory(reopened)==self.inventory(restored)and reopened['saddled']and stack(reopened['armor'])==stack(seeded['inventory'][10]['item']))
        for i,target in [(0,9),(1,10)]:self.slot(initial['cells'][i]['index'],sid);self.inv(target)
        for cell in initial['cells'][2:]:self.slot(cell['index'],sid);self.inv(12)
        recovered=self.state('recovered')
        self.check('all-equipment-and-cargo-recover-once-with-original-full-data',all(self.empty(v['item'])for v in recovered['cells'])and self.inventory(recovered)==self.inventory(seeded)and not recovered['saddled']and self.empty(recovered['armor'])and all(self.empty(v['item'])for v in recovered['cursors']))
        self.cmd('tp @p 8.5 65 30.5 180 5',ticks=18);self.act('regression-state',ticks=3)
        closed=json.loads((self.profile/'regression-state.json').read_text())
        self.check('native-range-validity-closes-moa-session',not any(v['session']==sid for v in closed['menus']))
        self.act('close',ticks=3);self.save(True)
        return dict(cases=1,failed=0,checks=len(self.checks),moa_features_checked=True)


def run(profile,mc,loader,*args,**kw):return Moa(profile,mc,loader).run()
