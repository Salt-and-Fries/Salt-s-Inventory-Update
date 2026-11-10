"""Exact native SSN ghost filters, settings, full-data nonconsumption and reopening."""
from pathlib import Path
import hashlib,json,sys
import nbtlib

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
from mowzie_features import stack


def physical_inventory(state):return tuple(stack(v['item'])for v in state['inventory'])
def ghosts(state):return tuple(stack(v)for v in state['settings']['ghosts'])
def no_cursor(state):return all(not nbtlib.parse_nbt(v['item'])for v in state['cursors'])


def exact_ghosts(state, original, counts):
    values=state['settings']['ghosts']
    return len(values)>=len(counts) and all(stack(values[i])==stack(original,n)for i,n in enumerate(counts)) and all(not nbtlib.parse_nbt(v)for v in values[len(counts):])


class Filters:
    def __init__(self,profile,mc,loader):
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.n=0;self.checks=[];self.cases=[];self.name=''
        self.command='server-command'if loader=='neoforge'else'command'
        self.out=self.profile/'ssn-filters-release';self.out.mkdir()
    def act(self,command_type,**kw):return send(self.profile,command_type,**kw)
    def cmd(self,text):self.act(self.command,command=text,ticks=6)
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.cases,checks=self.checks,
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            scope='Two native SSN ghost-filter menus: full-data copy/count/clear without consuming physical items, exact priority/allowlist changes, moved controls, reopening and source invalidation. Processing automation and network routing are separate.'),indent=2)+'\n')
    def check(self,name,value):
        self.checks.append(dict(case=self.name,name=name,passed=bool(value)));self.save();assert value,(self.name,name);print('PASS',self.name,name,flush=True)
    def state(self,label):
        self.act('regression-ssn-filters-state',ticks=5);data=json.loads((self.profile/'release-ssn-filters-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+self.name+'-'+label+'.json')).write_text(json.dumps(data,indent=2)+'\n');return data
    def point(self,state,x,y,button=0):
        self.act('regression-template-region-tap',session=state['client']['session'],region='native-working-panel',source_x=x,source_y=y,button=button,ticks=15)
    def button(self,state,name):
        button=state['client']['buttons'][name];assert button['visible']and button['active']
        self.point(state,button['x'],button['y'])
    def inv(self,index):self.act('regression-player-click',slot=index,ticks=10)
    def enter(self):self.act('client-use-block',ticks=20)
    def menu(self,path):
        self.name=path;self.act('close',ticks=4);self.cmd('gamemode survival @p');self.cmd('clear @p')
        self.act('fixture',block='storagenetwork:'+path,ticks=5);self.enter();initial=self.state('initial');sid=initial['client']['session']
        kind='ContainerCableFilter'if path=='filter_kabel'else'ContainerCollectionFilter'
        self.check('original-native-zero-inventory-filter-opens',initial['client']['menu'].endswith('.'+kind)and initial['client']['settings']==initial['settings']and all(not nbtlib.parse_nbt(v)for v in initial['settings']['ghosts']))
        data='{release_ssn_filter_marker:41,typed:[B;3b,6b]}'
        suffix=data if self.mc=='1.20.1'else'[minecraft:custom_data='+data+']'
        self.cmd('item replace entity @p container.9 with minecraft:diamond'+suffix+' 16');seeded=self.state('seeded');original=seeded['inventory'][9]['item']
        self.inv(9);self.point(seeded,16,43);copied=self.state('copied');count=16 if path=='filter_kabel'else 1
        self.check('native-filter-copies-exact-data-and-native-count',exact_ghosts(copied,original,[count])and copied['client']['settings']==copied['settings'])
        self.check('ghost-copy-keeps-full-physical-cursor',not nbtlib.parse_nbt(copied['inventory'][9]['item'])and any(stack(v['item'])==stack(original)for v in copied['cursors']))
        self.point(copied,16,43,1);incremented=self.state('incremented')
        self.check('right-click-increments-ghost-count-only',exact_ghosts(incremented,original,[count+1])and physical_inventory(incremented)==physical_inventory(copied)and [stack(v['item'])for v in incremented['cursors']]==[stack(v['item'])for v in copied['cursors']])
        self.inv(9);returned=self.state('physical-returned')
        self.check('filter-does-not-consume-or-duplicate-physical-item',physical_inventory(returned)==physical_inventory(seeded)and no_cursor(returned))
        if returned['client']['locked']:self.act('regression-control',session=sid,control='LOCK',ticks=4)
        self.act('regression-move',session=sid,dx=16,dy=12,ticks=5);moved=self.state('moved')
        self.check('filter-window-moves-before-native-input',moved['client']['bounds']!=returned['client']['bounds'])
        if path=='filter_kabel':
            self.button(moved,'btnPlus');plus=self.state('priority-plus')
            self.check('priority-increases-by-exactly-one',plus['settings']['priority']==initial['settings']['priority']+1 and plus['client']['settings']==plus['settings']and ghosts(plus)==ghosts(moved))
            self.button(plus,'btnMinus');minus=self.state('priority-minus')
            self.check('priority-minus-restores-initial-value',minus['settings']['priority']==initial['settings']['priority']and minus['client']['settings']==minus['settings'])
            self.button(minus,'btnAllowIgn');toggled=self.state('allowlist')
            self.check('allowlist-click-toggles-only-native-flag',toggled['settings']['allowList']is not initial['settings']['allowList']and toggled['settings']['priority']==initial['settings']['priority']and ghosts(toggled)==ghosts(moved)and toggled['client']['settings']==toggled['settings'])
            expected=toggled
        else:expected=moved
        self.act('close',ticks=5);self.enter();reopened=self.state('reopened')
        self.check('all-native-settings-persist-through-ordinary-reopening',reopened['settings']==expected['settings']and reopened['client']['settings']==reopened['settings']and physical_inventory(reopened)==physical_inventory(seeded)and no_cursor(reopened))
        self.point(reopened,16,43);cleared=self.state('cleared')
        self.check('empty-hand-click-clears-only-ghost',all(not nbtlib.parse_nbt(v)for v in cleared['settings']['ghosts'])and physical_inventory(cleared)==physical_inventory(seeded)and no_cursor(cleared))
        sid=cleared['client']['session'];self.act('remove-source',ticks=18);self.act('regression-state',ticks=3)
        final=json.loads((self.profile/'regression-state.json').read_text())
        self.check('native-source-removal-closes-owned-filter',not any(v['session']==sid for v in final['menus']))
        self.cases.append(path);self.save()
    def run(self):
        for path in ['filter_kabel','collector']:self.menu(path)
        self.act('close',ticks=3);self.save(True)
        return dict(cases=2,failed=0,checks=len(self.checks),ssn_filters_features_checked=True)


def run(profile,mc,loader,*args,**kw):return Filters(profile,mc,loader).run()
