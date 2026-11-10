"""Real native steam pump cycles and a genuinely formed tank's populated UI."""
from pathlib import Path
import copy,hashlib,json,math
import nbtlib
from mowzie_features import stack,typed
from mi_storage_features import Storage,physical,cursor_is,cell_state,resources,policies,physical_item
from mi_energy_features import original,INPUTS

PUMPS=['bronze_water_pump','steel_water_pump']


def tank_approach(blueprint):
    xs=[v['x']for v in blueprint];zs=[v['z']for v in blueprint]
    options=[]
    if min(xs)==8<max(xs):options.append((5.5,8.5,-90))
    if min(xs)<8==max(xs):options.append((11.5,8.5,90))
    if min(zs)==8<max(zs):options.append((8.5,5.5,0))
    if min(zs)<8==max(zs):options.append((8.5,11.5,180))
    assert len(options)==1,'Native controller must have one clear exterior horizontal face'
    x,z,yaw=options[0];return f'tp @a {x} 65 {z} {yaw} 21'


def tank_matches(probe,unit,expected_amount):
    tag=nbtlib.parse_nbt(probe['fixture']);tank=probe['client']['components']['tank'];capacity=27*64*unit
    return (probe['shapeValid']is True and isinstance(tag['amount'],nbtlib.Long)and isinstance(tag['capacity'],nbtlib.Long)
        and int(tag['amount'])==expected_amount and int(tag['capacity'])==capacity
        and type(tank['amount'])is int and tank['amount']==expected_amount and type(tank['capacity'])is int and tank['capacity']==capacity
        and (expected_amount==0 or tank['fluid']=='minecraft:water'))


class Infrastructure(Storage):
    def __init__(self,profile,mc,loader):
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.ui=original.Ui(self.profile.name);self.checks=[];self.cases=[];self.probes={};self.case='';self.n=0
        self.out=self.profile/'mi-infrastructure-release';self.out.mkdir();self.unit=81000 if mc=='1.20.1'else 1000
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.cases,checks=self.checks,
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            scope='Two native water pumps: actual adjacent source water, declared one-cycle steam prerequisite, real progress/consumption/generated water, native bucket extraction/refusal and reopening. LargeTank: ordinary world blocks from its read-only original blueprint, native-computed formation/capacity, then declared pre-entry stored water for exact populated status and geometry. No injected progress/output/formation/capacity, long production chain or tank-fluid-transfer claim.'),indent=2)+'\n')
    def infra(self,label):
        self.ui.send('regression-mi-infrastructure-state',ticks=4);probe=json.loads((self.profile/'release-mi-infrastructure-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+self.case+'-'+label+'-infrastructure.json')).write_text(json.dumps(probe,indent=2)+'\n');return probe
    def pump(self,name,multiplier):
        initial,client=self.place(name);baseline=physical(initial,exclude=(9,));cells=cell_state(initial,client,'fluids');assert len(cells)==2 and resources(cells)==()
        initial_policies=policies(cells);assert not bool(cells[1].get('playerInsert',False))and bool(cells[1].get('playerExtract',False))
        for command in ['fill 6 65 6 10 66 6 minecraft:stone','fill 6 65 10 10 66 10 minecraft:stone','fill 6 65 7 6 66 9 minecraft:stone','fill 10 65 7 10 66 9 minecraft:stone']:
            self.cmd(command)
        for dx in [-1,0,1]:
            for dz in [-1,0,1]:
                if dx or dz:self.cmd(f'setblock {8+dx} 65 {8+dz} minecraft:water')
        self.cmd('tp @a 8.5 67 10.5 180 58');self.cmd('item replace entity @a container.9 with minecraft:bucket 1')
        modified=copy.deepcopy(cells);modified[0]['amount']=nbtlib.Long(self.unit//10)
        modified[0]['key']=nbtlib.parse_nbt('{fluid:"modern_industrialization:steam"}'if self.mc=='1.20.1'else'{id:"modern_industrialization:steam"}')
        self.prerequisite('fluids',modified)
        data,client=self.state('active-native-cycle');probe=self.infra('active-native-cycle');progress=probe['client']['components']['progress'];native=nbtlib.parse_nbt(probe['fixture'])
        self.check('real native pumping consumes steam and exposes active progress',type(progress)in {int,float}and math.isfinite(progress)and 0<progress<1 and 0<int(native['pumpingTicks'])<100 and 0<int(native['fluids'][0]['amount'])<self.unit//10 and physical(data,exclude=(9,))==baseline and cursor_is(data))
        self.ui.send('wait',ticks=115);data,client=self.state('completed-native-cycle');actual=cell_state(data,client,'fluids');probe=self.infra('completed-native-cycle');native=nbtlib.parse_nbt(probe['fixture'])
        water=nbtlib.parse_nbt('{fluid:"minecraft:water"}'if self.mc=='1.20.1'else'{id:"minecraft:water"}')
        expected=((1,multiplier*self.unit,typed(water)),)
        self.check('one real cycle consumes exact steam and generates exact water',resources(actual)==expected and policies(actual)==initial_policies and int(native['pumpingTicks'])==0 and resources(native['fluids'])==expected and physical(data,exclude=(9,))==baseline and cursor_is(data))
        self.ui.inv(9);self.click(client,'fluids',1);data,client=self.state('native-generated-water-withdrawn');actual=cell_state(data,client,'fluids')
        remaining=()if multiplier==1 else((1,(multiplier-1)*self.unit,typed(water)),)
        self.check('native bucket withdraws genuinely generated water exactly once',resources(actual)==remaining and policies(actual)==initial_policies and cursor_is(data,physical_item(self.mc,'minecraft:water_bucket',1))and physical(data,exclude=(9,))==baseline)
        self.click(client,'fluids',1);data,client=self.state('native-pump-output-refuses-reinsertion');actual=cell_state(data,client,'fluids')
        self.check('original pump output refuses water reinsertion',resources(actual)==remaining and cursor_is(data,physical_item(self.mc,'minecraft:water_bucket',1))and physical(data,exclude=(9,))==baseline)
        self.ui.inv(9);self.reopen();data,client=self.state('native-pump-reopened');actual=cell_state(data,client,'fluids')
        self.check('generated fluid and full physical items persist after reopening',resources(actual)==remaining and policies(actual)==initial_policies and cursor_is(data)and stack(data['inventory'][9]['item'])==stack(physical_item(self.mc,'minecraft:water_bucket',1))and physical(data,exclude=(9,))==baseline)
        self.cases.append(self.case);self.save()
    def tank(self):
        initial,client=self.place('large_tank');baseline=physical(initial);probe=self.infra('original-unformed-blueprint');blueprint=probe['blueprint']
        self.check('original unformed blueprint is genuine and complete',probe['shapeValid']is False and len(blueprint)==25 and len({(v['x'],v['y'],v['z'])for v in blueprint})==25 and not any((v['x'],v['y'],v['z'])==(8,65,8)for v in blueprint))
        self.ui.send('close',ticks=3)
        for entry in blueprint:self.cmd(f"setblock {entry['x']} {entry['y']} {entry['z']} {entry['block']}")
        # The newer native orientation transform differs from the legacy one.
        # Approach the real exterior controller face, never aim through its
        # completed shell or manufacture a block-use hit.
        self.cmd(tank_approach(blueprint))
        self.ui.send('wait',ticks=20);self.ui.send('open-ray',ticks=12);data,client=self.state('native-formed-empty-tank');probe=self.infra('native-formed-empty-tank')
        self.check('ordinary native structure computes its real empty capacity',tank_matches(probe,self.unit,0)and physical(data)==baseline and cursor_is(data))
        self.ui.send('close',ticks=4)
        fluid='{fluid:"minecraft:water"}'if self.mc=='1.20.1'else'{id:"minecraft:water"}'
        self.cmd('data merge block 8 65 8 {amount:'+str(self.unit)+'L,fluid:'+fluid+'}')
        self.ui.send('open-ray',ticks=14);data,client=self.state('native-populated-formed-tank');probe=self.infra('native-populated-formed-tank')
        self.check('populated native gauge retains real formation and capacity',tank_matches(probe,self.unit,self.unit)and physical(data)==baseline and cursor_is(data))
        self.reopen();data,client=self.state('native-populated-tank-reopened');probe=self.infra('native-populated-tank-reopened')
        self.check('formed populated native status persists on ordinary reopening',tank_matches(probe,self.unit,self.unit)and physical(data)==baseline and cursor_is(data))
        self.cases.append(self.case);self.save()
    def run(self):
        for name,multiplier in zip(PUMPS,[1,2]):self.pump(name,multiplier)
        self.tank();self.ui.send('close',ticks=4);self.check('native infrastructure closes all owned sessions',self.ui.state()['sessions']==0);self.save(True)
        return dict(cases=len(self.cases),failed=0,checks=len(self.checks),mi_infrastructure_features_checked=True)


def run(profile,mc,loader,*args,**kwargs):return Infrastructure(profile,mc,loader).run()
