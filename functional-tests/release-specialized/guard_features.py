"""Ordinary Hero-permitted equipment, follow/patrol actions and native lifetime."""
from pathlib import Path
import hashlib,json,sys
import nbtlib

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
from mowzie_features import stack

ITEMS=['iron_helmet','iron_chestplate','iron_leggings','iron_boots','shield','iron_sword']


class Guard:
    def __init__(self,profile,mc,loader):
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.checks=[];self.n=0
        self.command='server-command'if loader=='neoforge'else'command'
        self.out=self.profile/'guard-release';self.out.mkdir()
    def act(self,kind,**kw):return send(self.profile,kind,**kw)
    def cmd(self,command,ticks=6):return self.act(self.command,command=command,ticks=ticks)
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,checks=self.checks,cases=['guard-villager'],
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            scope='Ordinary Hero-of-the-Village permitted native entry; all six equipment cells, exact data, rejected armor input, native follow/patrol actions, moved geometry, close/reopen and entity invalidation. Not combat AI, patrolling travel or every reputation configuration.'),indent=2)+'\n')
    def check(self,name,value):
        self.checks.append(dict(name=name,passed=bool(value)));self.save();assert value,name
        print('PASS',name,flush=True)
    def state(self,label):
        self.act('regression-guard-state',ticks=4);d=json.loads((self.profile/'release-guard-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(d,indent=2)+'\n');return d
    @staticmethod
    def empty(text):return not nbtlib.parse_nbt(text)
    def cursors_empty(self,d):return all(self.empty(v['item'])for v in d['cursors'])
    def slot(self,index,sid):self.act('regression-slot-click',session=sid,slot=index,ticks=10)
    def inv(self,index):self.act('regression-player-click',slot=index,ticks=10)
    def enter(self):
        self.act('sneak',down=False,ticks=3)
        self.act('client-use-entity',entity='guardvillagers:guard',shift=False,ticks=20)
    def seed(self,index,item,marker):
        data='{release_guard_marker:'+str(marker)+',typed:[B;2b,8b]}'
        tail=('{release_guard_marker:'+str(marker)+',typed:[B;2b,8b],Damage:2}')if self.mc=='1.20.1'else('[minecraft:custom_data='+data+',minecraft:damage=2]')
        self.cmd(f'item replace entity @p container.{index} with minecraft:{item}{tail}')
    def button(self,d,follow):
        rows=[b for b in d['client']['buttons']if b['follow']==follow];assert len(rows)==1 and rows[0]['active']and rows[0]['visible']
        b=rows[0];self.act('regression-template-widget-tap',session=d['client']['session'],widget_type=b['type'],message=b['message'],ordinal=b['ordinal'],ticks=16)
    def run(self):
        self.act('close',ticks=3);self.act('fixture',block='minecraft:air',ticks=4)
        speed='minecraft:generic.movement_speed'if self.mc in ['1.20.1','1.21.1']else'minecraft:movement_speed'
        commands=['kill @e[type=!minecraft:player]','gamemode survival @p','clear @p','difficulty normal','time set day',
            'effect give @p minecraft:hero_of_the_village 600 0 true','fill 6 64 6 12 64 13 minecraft:stone',
            'summon guardvillagers:guard 8.5 65 8.5 {NoAI:0b,NoGravity:1b,Invulnerable:1b,PersistenceRequired:1b}',
            f'attribute @e[type=guardvillagers:guard,limit=1] {speed} base set 0','tp @p 8.5 65 11.5 180 5',
            'item replace entity @p weapon.mainhand with minecraft:air']
        commands += [f'item replace entity @e[type=guardvillagers:guard,limit=1] {slot} with minecraft:air'for slot in ['armor.head','armor.chest','armor.legs','armor.feet','weapon.mainhand','weapon.offhand']]
        for command in commands:self.cmd(command)
        self.enter();initial=self.state('initial');sid=initial['client']['session']
        self.check('ordinary-native-guard-entry-has-six-real-equipment-cells',len(initial['cells'])==6 and all(self.empty(v['item'])for v in initial['cells']))
        self.check('both-original-native-behavior-buttons-retained',len(initial['client']['buttons'])==2 and {v['follow']for v in initial['client']['buttons']}=={False,True})
        for index,item in enumerate(ITEMS):self.seed(9+index,item,40+index)
        seeded=self.state('seeded')
        for i in range(6):self.inv(9+i);self.slot(initial['cells'][i]['index'],sid)
        inserted=self.state('inserted')
        self.check('all-six-guard-equipment-cells-preserve-their-full-stacks',all(stack(inserted['cells'][i]['item'])==stack(seeded['inventory'][9+i]['item'])for i in range(6)))
        self.check('equipment-insertion-consumes-each-player-reference-once',all(self.empty(inserted['inventory'][9+i]['item'])for i in range(6))and self.cursors_empty(inserted))
        if inserted['client']['locked']:self.act('regression-control',session=sid,control='LOCK',ticks=3)
        self.act('regression-move',session=sid,dx=16,dy=12,ticks=3);moved=self.state('moved')
        self.check('guard-window-moves-with-equipment-and-native-buttons',moved['client']['bounds']!=inserted['client']['bounds'])
        self.button(moved,True);follow=self.state('follow-on')
        self.check('native-follow-action-reaches-the-server-and-client',follow['following']!=moved['following']and follow['client']['following']==follow['following'])
        self.button(follow,True);unfollow=self.state('follow-off')
        self.check('native-follow-can-be-restored-through-moved-window',unfollow['following']==moved['following']and unfollow['client']['following']==unfollow['following'])
        self.button(unfollow,False);patrol=self.state('patrol-on')
        self.check('native-patrol-action-reaches-the-server-and-position',patrol['patrolling']!=unfollow['patrolling']and patrol['client']['patrolling']==patrol['patrolling']and patrol['patrolPos']!='null')
        self.act('close',ticks=4);self.enter();reopened=self.state('reopened');sid=reopened['client']['session']
        self.check('equipment-and-patrol-state-persist-through-ordinary-reopening',reopened['patrolling']==patrol['patrolling']and all(stack(reopened['cells'][i]['item'])==stack(seeded['inventory'][9+i]['item'])for i in range(6)))
        self.button(reopened,False);restored=self.state('patrol-off')
        self.check('native-patrol-can-be-restored-after-reopening',restored['patrolling']==unfollow['patrolling']and restored['client']['patrolling']==restored['patrolling'])
        for i in range(6):self.slot(restored['cells'][i]['index'],sid);self.inv(9+i)
        returned=self.state('returned')
        self.check('every-native-equipment-stack-recovers-with-full-data',all(stack(returned['inventory'][9+i]['item'])==stack(seeded['inventory'][9+i]['item'])for i in range(6))and all(self.empty(v['item'])for v in returned['cells'])and self.cursors_empty(returned))
        self.cmd('item replace entity @p container.20 with minecraft:diamond 3');foreign=self.state('foreign')
        self.inv(20);self.slot(returned['cells'][0]['index'],sid);refused=self.state('foreign-refused')
        self.check('native-helmet-slot-refuses-foreign-item-with-exact-cursor',self.empty(refused['cells'][0]['item'])and any(stack(v['item'])==stack(foreign['inventory'][20]['item'])for v in refused['cursors']))
        self.inv(20);self.cmd('kill @e[type=guardvillagers:guard]',ticks=20)
        self.act('regression-state',ticks=3);closed=json.loads((self.profile/'regression-state.json').read_text())
        self.check('guard-source-invalidation-removes-its-session',not any('GuardContainer'in v['type']or 'GuardVillagerScreenHandler'in v['type']for v in closed['menus']))
        self.act('close',ticks=3);self.save(True)
        return dict(cases=1,failed=0,checks=len(self.checks),guard_features_checked=True)


def run(profile,mc,loader,*args,**kw):return Guard(profile,mc,loader).run()
