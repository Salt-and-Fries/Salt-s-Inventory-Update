"""Ordinary awakening and all fifteen real companion cargo cells on SIU."""
from pathlib import Path
import hashlib,json,sys
import nbtlib

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
from mowzie_features import stack,item_count


class Ministrosity:
    def __init__(self,profile,mc,loader):
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.checks=[];self.n=0
        self.command='server-command'if loader=='neoforge'else'command'
        self.out=self.profile/'ministrosity-release';self.out.mkdir()
    def act(self,command_type,**kw):return send(self.profile,command_type,**kw)
    def cmd(self,text,ticks=6):self.act(self.command,command=text,ticks=ticks)
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=['cataclysm-netherite-ministrosity'],checks=self.checks,
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            scope='Ordinary native awakening/use, all fifteen cargo hits, full-data split/recovery, native blacklist, SIU sort/transfer, moved last cell, owner persistence and range invalidation. Not pet combat or every blacklisted item.'),indent=2)+'\n')
    def check(self,name,value):
        self.checks.append(dict(name=name,passed=bool(value)));self.save();assert value,name;print('PASS',name,flush=True)
    def state(self,label):
        self.act('regression-ministrosity-state',ticks=4);d=json.loads((self.profile/'release-ministrosity-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(d,indent=2)+'\n');return d
    @staticmethod
    def empty(text):return not nbtlib.parse_nbt(text)
    @staticmethod
    def inventory(d):return [stack(v['item'])for v in d['inventory']]
    @staticmethod
    def cargo(d):return tuple(stack(v['item'])for v in d['cells'])
    def slot(self,index,sid,button=0):self.act('regression-slot-click',session=sid,slot=index,button=button,ticks=10)
    def inv(self,index):self.act('regression-player-click',slot=index,ticks=10)
    def seed(self,index,item,count,marker):
        data='{release_pet_marker:'+str(marker)+',typed:[B;1b,7b]}'
        suffix=data if self.mc=='1.20.1'else'[minecraft:custom_data='+data+']'
        self.cmd(f'item replace entity @p container.{index} with {item}{suffix} {count}')
    def enter(self):self.act('regression-use-entity',entity='cataclysm:netherite_ministrosity',shift=False,ticks=30)
    def run(self):
        self.act('close',ticks=3);self.act('fixture',block='minecraft:air',ticks=4)
        for text in ['kill @e[type=!minecraft:player]','gamemode survival @p','clear @p',
            'fill 5 64 6 16 64 35 minecraft:stone',
            'summon cataclysm:netherite_ministrosity 8.5 65 10.5 {NoAI:1b,NoGravity:1b,PersistenceRequired:1b}',
            'tp @p 8.5 65 12.5 180 5','item replace entity @p weapon.mainhand with cataclysm:lava_power_cell']:
            self.cmd(text)
        self.enter();self.cmd('item replace entity @p weapon.mainhand with minecraft:air');self.enter()
        initial=self.state('initial');sid=initial['client']['session']
        self.check('ordinary-awakened-companion-opens-original-fifteen-cargo-cells',initial['client']['menu']=='com.github.L_Ender.cataclysm.inventory.MinistrostiyMenu'and initial['client']['screen']=='com.github.L_Ender.cataclysm.client.gui.MinistrosityInventoryScreen'and len(initial['cells'])==15 and all(self.empty(v['item'])for v in initial['cells']))
        self.seed(9,'minecraft:stone',15,51);self.seed(10,'minecraft:black_shulker_box',1,52);seeded=self.state('seeded')
        self.inv(10);self.slot(initial['cells'][0]['index'],sid);blocked=self.state('blacklist-refused')
        self.check('original-blacklist-refuses-shulker-with-exact-cursor',self.empty(blocked['cells'][0]['item'])and any(stack(v['item'])==stack(seeded['inventory'][10]['item'])for v in blocked['cursors']))
        self.inv(10);self.inv(9)
        for cell in initial['cells']:self.slot(cell['index'],sid,1)
        filled=self.state('fifteen-hits')
        self.check('all-fifteen-visual-hits-preserve-one-fully-tagged-stone',all(item_count(v['item'])==('minecraft:stone',1)and stack(v['item'])==stack(seeded['inventory'][9]['item'],1)for v in filled['cells'])and self.empty(filled['inventory'][9]['item'])and all(self.empty(v['item'])for v in filled['cursors']))
        self.act('regression-control',session=sid,control='SORT_INVENTORY',ticks=16);sorted_state=self.state('sorted')
        nonempty=[v for v in sorted_state['cells']if not self.empty(v['item'])]
        self.check('container-sort-merges-all-fifteen-with-exact-data',len(nonempty)==1 and stack(nonempty[0]['item'])==stack(seeded['inventory'][9]['item'])and self.inventory(sorted_state)==self.inventory(filled))
        self.seed(9,'minecraft:stone',7,51)
        self.act('regression-builtin-control',kind='INVENTORY',control='SORT_WINDOWS',ticks=16);sorted_in=self.state('sort-in')
        nonempty=[v for v in sorted_in['cells']if not self.empty(v['item'])]
        self.check('inventory-sort-into-pet-preserves-exact-matching-total-and-blacklist',len(nonempty)==1 and stack(nonempty[0]['item'])==stack(seeded['inventory'][9]['item'],22)and self.empty(sorted_in['inventory'][9]['item'])and stack(sorted_in['inventory'][10]['item'])==stack(seeded['inventory'][10]['item']))
        if sorted_in['client']['locked']:self.act('regression-control',session=sid,control='LOCK',ticks=3)
        self.act('regression-move',session=sid,dx=17,dy=12,ticks=3);moved=self.state('moved')
        self.check('companion-window-moves-before-last-cell-input',moved['client']['bounds']!=sorted_in['client']['bounds'])
        self.seed(11,'minecraft:diamond',3,53);before_diamond=self.state('diamond-fixture');self.inv(11);self.slot(initial['cells'][-1]['index'],sid)
        last=self.state('last-cell')
        self.check('moved-last-cell-receives-exact-diamond-stack',stack(last['cells'][-1]['item'])==stack(before_diamond['inventory'][11]['item'])and self.empty(last['inventory'][11]['item'])and all(self.empty(v['item'])for v in last['cursors']))
        self.act('close',ticks=12);self.enter();reopened=self.state('reopened');sid=reopened['client']['session']
        self.check('same-owner-retains-exact-cargo-on-ordinary-reopening',reopened['owner']==initial['owner']and self.cargo(reopened)==self.cargo(last)and self.inventory(reopened)==self.inventory(last))
        self.slot(reopened['cells'][-1]['index'],sid);self.inv(11)
        occupied=next(v for v in reopened['cells']if item_count(v['item'])[0]=='minecraft:stone');self.slot(occupied['index'],sid);self.inv(9)
        recovered=self.state('cargo-recovered')
        expected=self.inventory(seeded);expected[9]=stack(seeded['inventory'][9]['item'],22);expected[11]=stack(before_diamond['inventory'][11]['item'])
        self.check('all-stored-items-recover-once-with-full-data',all(self.empty(v['item'])for v in recovered['cells'])and self.inventory(recovered)==expected and all(self.empty(v['item'])for v in recovered['cursors']))
        self.cmd('tp @p 8.5 65 30.5 180 5',ticks=18);self.act('regression-state',ticks=3)
        closed=json.loads((self.profile/'regression-state.json').read_text())
        self.check('native-range-validity-closes-original-companion-session',not any(v['session']==sid for v in closed['menus']))
        self.act('close',ticks=3);self.save(True)
        return dict(cases=1,failed=0,checks=len(self.checks),ministrosity_features_checked=True)


def run(profile,mc,loader,*args,**kw):return Ministrosity(profile,mc,loader).run()
