"""Native process-cable settings and ghost recipe editing; no simulated production."""
from pathlib import Path
import copy,hashlib,json
import nbtlib
from ssn_filters_features import Filters,physical_inventory,no_cursor
from mowzie_features import stack

FACES=['DOWN','UP','NORTH','SOUTH','WEST','EAST']


def next_face(value):return FACES[(FACES.index(value)+1)%len(FACES)]
def exact_ghost(state,field,item,count):
    values=state['settings'][field]
    return bool(values)and stack(values[0])==stack(item,count)and all(not nbtlib.parse_nbt(v)for v in values[1:])


def changed_only(before,after,key,value):
    expected=copy.deepcopy(before['settings']);expected[key]=value
    return after['settings']==expected and after['client']['settings']==expected


class Process(Filters):
    def __init__(self,*args):
        super().__init__(*args);self.out.rmdir();self.out=self.profile/'ssn-process-release';self.out.mkdir();self.name='process_kabel'
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.cases,checks=self.checks,
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            scope='Native process-cable input/output ghost recipe editing, exact count/always/redstone/facing settings, moved controls, full physical inventory preservation, persistence and source invalidation. No formed processing network, reset-during-production or filter import from adjacent machines claim.'),indent=2)+'\n')
    def state(self,label):
        self.act('regression-ssn-process-state',ticks=5);data=json.loads((self.profile/'release-ssn-process-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(data,indent=2)+'\n');return data
    def run(self):
        self.act('close',ticks=3);self.cmd('gamemode survival @p');self.cmd('clear @p')
        self.act('fixture',block='storagenetwork:process_kabel',ticks=6);self.enter();initial=self.state('initial');sid=initial['client']['session']
        self.check('original-process-menu-opens-with-synchronized-native-state',initial['client']['menu']=='com.lothrazar.storagenetwork.block.cable.processing.ContainerCableProcess'and initial['client']['settings']==initial['settings'])
        for index,item,marker in [(9,'minecraft:iron_ingot',51),(10,'minecraft:gold_ingot',52)]:
            self.cmd(f'item replace entity @p container.{index} with {item}[minecraft:custom_data={{release_ssn_process_marker:{marker},typed:[B;2b,9b]}}] 16')
        seeded=self.state('seeded')
        for index,field,x in [(9,'getFilters',16),(10,'getFiltersOut',122)]:
            self.inv(index);self.point(seeded,x,44);copied=self.state(field+'-copied')
            self.check(field+'-copies-one-exact-native-ghost',exact_ghost(copied,field,seeded['inventory'][index]['item'],1)and copied['client']['settings']==copied['settings'])
            self.point(copied,x,44,1);incremented=self.state(field+'-incremented')
            self.check(field+'-right-click-changes-only-count',exact_ghost(incremented,field,seeded['inventory'][index]['item'],2)and any(stack(v['item'])==stack(seeded['inventory'][index]['item'])for v in incremented['cursors']))
            self.inv(index)
        configured=self.state('ghost-recipe-configured')
        self.check('both-ghost-recipes-preserve-all-physical-items',physical_inventory(configured)==physical_inventory(seeded)and no_cursor(configured))
        if configured['client']['locked']:self.act('regression-control',session=sid,control='LOCK',ticks=4)
        self.act('regression-move',session=sid,dx=16,dy=12,ticks=4);prior=self.state('moved')
        self.check('process-window-moves-before-configuration-input',prior['client']['bounds']!=configured['client']['bounds'])
        for button,key,expected in [('btnPlus','count',prior['settings']['count']+1),('btnMinus','count',prior['settings']['count']),
            ('btnAlways','always',not prior['settings']['always']),('btnRedstone','needsRedstone',not prior['settings']['needsRedstone']),
            ('btnInputFace','inputFace',next_face(prior['settings']['inputFace'])),('btnOutputFace','outputFace',next_face(prior['settings']['outputFace']))]:
            self.button(prior,button);after=self.state(button)
            self.check(button+'-changes-exact-native-setting-only',changed_only(prior,after,key,expected)and physical_inventory(after)==physical_inventory(seeded)and no_cursor(after))
            prior=after
        self.act('close',ticks=5);self.enter();reopened=self.state('reopened')
        self.check('all-process-configuration-persists-on-reopening',reopened['settings']==prior['settings']and reopened['client']['settings']==reopened['settings']and physical_inventory(reopened)==physical_inventory(seeded)and no_cursor(reopened))
        for field,x in [('getFilters',16),('getFiltersOut',122)]:
            self.point(reopened,x,44)
        cleared=self.state('cleared')
        self.check('empty-hand-clears-both-ghosts-without-changing-items',all(not nbtlib.parse_nbt(v)for key in ['getFilters','getFiltersOut']for v in cleared['settings'][key])and physical_inventory(cleared)==physical_inventory(seeded)and no_cursor(cleared))
        sid=cleared['client']['session'];self.act('remove-source',ticks=16);self.act('regression-state',ticks=3)
        final=json.loads((self.profile/'regression-state.json').read_text())
        self.check('native-process-source-removal-closes-owned-session',not any(v['session']==sid for v in final['menus']))
        self.cases=['process_kabel'];self.act('close',ticks=3);self.save(True)
        return dict(cases=1,failed=0,checks=len(self.checks),ssn_process_features_checked=True)


def run(profile,mc,loader,*args,**kw):return Process(profile,mc,loader).run()
