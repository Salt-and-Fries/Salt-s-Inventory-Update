"""Offline corruption controls for native pump/tank evidence, not gameplay."""
from pathlib import Path
import copy, importlib.util, sys, unittest
import nbtlib

ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
spec=importlib.util.spec_from_file_location('review_mi_infrastructure',ROOT/'tools/reconcile-mi-infrastructure-release-evidence.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
DRIVER='a'*64


def fixture(target='1.21.1/neoforge'):
    mc=target.split('/')[0];unit=81000 if mc=='1.20.1'else 1000
    classes=m.storage.native_classes(target);key='fluid'if mc=='1.20.1'else'id'
    water=nbtlib.parse_nbt('{'+key+':"minecraft:water"}')
    steam=nbtlib.parse_nbt('{'+key+':"modern_industrialization:steam"}')
    seed=m.physical_item(mc,'minecraft:diamond',7,'{release_mi_status_marker:41,typed:[B;3b,6b]}')
    states={};checks=[]
    def frame(case,cells,slot9='{}',cursor='{}',extra=None,tank=False):
        tag=nbtlib.Compound(dict(id=nbtlib.String('modern_industrialization:'+case.removeprefix(m.PREFIX)),
            x=nbtlib.Int(8),y=nbtlib.Int(65),z=nbtlib.Int(8)))
        if cells is not None:tag['fluids']=copy.deepcopy(cells)
        if extra:tag.update(copy.deepcopy(extra))
        inv=[dict(index=i,item=seed if i==12 else slot9 if i==9 else'{}')for i in range(41)]
        window=dict(definition='com.salts_inventory_update.client.automatic.NativeTemplateWindow',menuId='modern_industrialization:machine',nativeSlots=[],nativeWidgets=[],
            nativeBounds=dict(x=0,y=0,width=176,height=166),template=dict(regions=[dict(source=dict(x=0,y=0,width=176,height=166))]),
            nativeValues=dict(miComponentBounds=[dict(component='LargeTankFluidDisplayClient',x=70,y=12,width=46,height=62)]if tank else[]))
        visible=[]if cells is None else[dict(index=i,x=40+20*i,y=50,nbt=c.snbt())for i,c in enumerate(cells)]
        return dict(client=dict(windows=[window]),server=dict(fixture=tag.snbt(),inventory=inv,
            menus=[{'class':'player.Menu','carried':cursor},{'class':m.SERVER,'carried':cursor}]),
            observed=dict(menu=classes[1],screen=classes[2],cells=dict(items=[],fluids=visible)))
    for case,multiplier in zip(m.CASES[:2],[1,2]):
        cells=nbtlib.parse_nbt('[{amount:0L,key:{},playerInsert:1b,playerExtract:1b},{amount:0L,key:{},playerInsert:0b,playerExtract:1b}]')
        active=copy.deepcopy(cells);active[0]['amount']=nbtlib.Long(unit//20);active[0]['key']=steam
        done=copy.deepcopy(cells);done[1]['amount']=nbtlib.Long(multiplier*unit);done[1]['key']=water
        remaining=copy.deepcopy(cells)
        if multiplier>1:remaining[1]['amount']=nbtlib.Long((multiplier-1)*unit);remaining[1]['key']=water
        bucket=m.physical_item(mc,'minecraft:bucket',1);filled=m.physical_item(mc,'minecraft:water_bucket',1)
        frames=[('empty',frame(case,cells)),('active-native-cycle',frame(case,active,bucket)),
            ('completed-native-cycle',frame(case,done,bucket)),('native-generated-water-withdrawn',frame(case,remaining,cursor=filled)),
            ('native-pump-output-refuses-reinsertion',frame(case,remaining,cursor=filled)),('native-pump-reopened',frame(case,remaining,filled))]
        probes=[]
        for label,f,ticks,progress in [('active-native-cycle',frames[1][1],35,.35),('completed-native-cycle',frames[2][1],0,0)]:
            tag=nbtlib.parse_nbt(f['server']['fixture']);tag['pumpingTicks']=nbtlib.Int(ticks)
            probes.append((label,dict(fixture=tag.snbt(),blockEntity=m.PUMP_BE,client=dict(menu=classes[1],components=dict(progress=progress)))))
        states[case]=dict(frames=frames,probes=probes)
        checks.extend(dict(case=case,name=name,passed=True)for name in m.PUMP_NAMES)
    case=m.CASES[-1]
    blueprint=[dict(x=x,y=y,z=z,block='minecraft:glass')for x in range(7,10)for y in range(65,68)for z in range(8,11)
        if(x,y,z)not in{(8,65,8),(8,66,9)}]
    frames=[];probes=[]
    for stage,label in enumerate(['empty','native-formed-empty-tank','native-populated-formed-tank','native-populated-tank-reopened']):
        amount=0 if stage<2 else unit;capacity=27*64*unit if stage else 0
        f=frame(case,None,extra=dict(amount=nbtlib.Long(amount),capacity=nbtlib.Long(capacity),fluid=water if amount else nbtlib.Compound()),tank=True)
        frames.append((label,f))
        probes.append(('original-unformed-blueprint'if stage==0 else label,dict(fixture=f['server']['fixture'],blockEntity=m.TANK_BE,
            shapeValid=stage>0,blueprint=copy.deepcopy(blueprint),client=dict(menu=classes[1],components=dict(tank=dict(amount=amount,capacity=capacity,fluid='minecraft:water'if amount else'minecraft:empty'))))))
    states[case]=dict(frames=frames,probes=probes)
    checks.extend(dict(case=case,name=name,passed=True)for name in m.TANK_NAMES)
    checks.append(dict(case=case,name='native infrastructure closes all owned sessions',passed=True))
    return dict(complete=True,cases=list(m.CASES),driver_sha256=DRIVER,checks=checks),states


class InfrastructureEvidence(unittest.TestCase):
    def reject(self,mutate):
        j,s=fixture();mutate(j,s)
        with self.assertRaises((AssertionError,KeyError,ValueError,TypeError)):m.validate('1.21.1/neoforge',j,s,DRIVER)
    def test_exact_two_native_target_contracts_and_distinct_tank_scope(self):
        for target in m.TARGETS:
            j,s=fixture(target);self.assertEqual(m.validate(target,j,s,DRIVER),dict(assertions=18,states=24,native_variants=3,contract='two-real-pump-cycles-and-formed-tank-status-not-tank-production'))
            self.assertEqual(m.specification(target,m.CASES[-1],[m.SERVER]),'read-only-status-pass')
            self.assertEqual(m.specification(target,m.CASES[0],[m.SERVER]),'functional-pass')
    def test_exact_complete_original_assertions_and_driver_required(self):
        for mutation in [lambda j,s:j.update(complete=1),lambda j,s:j.update(driver_sha256='b'*64),lambda j,s:j['checks'].pop(),
            lambda j,s:j['checks'][0].update(passed=1),lambda j,s:j['checks'].append(copy.deepcopy(j['checks'][0]))]:self.reject(mutation)
    def test_actual_active_progress_steam_and_generated_amount_required(self):
        for value in [0,1,True,float('nan')]:self.reject(lambda j,s:s[m.CASES[0]]['probes'][0][1]['client']['components'].update(progress=value))
        self.reject(lambda j,s:s[m.CASES[0]]['probes'][0][1].update(fixture=s[m.CASES[0]]['probes'][0][1]['fixture'].replace('pumpingTicks: 35','pumpingTicks: 0')))
        self.reject(lambda j,s:s[m.CASES[1]]['probes'][1][1].update(fixture=s[m.CASES[1]]['probes'][1][1]['fixture'].replace('2000L','1000L')))
    def test_missing_frames_or_wrong_native_source_rejected(self):
        self.reject(lambda j,s:s[m.CASES[0]]['frames'].pop())
        self.reject(lambda j,s:s[m.CASES[0]]['probes'].pop())
        self.reject(lambda j,s:s[m.CASES[0]]['probes'][0][1].update(blockEntity='other.Machine'))
        self.reject(lambda j,s:s[m.CASES[0]]['frames'][1][1]['observed'].update(screen='other.Screen'))
    def test_refusal_and_reopened_physical_result_required(self):
        self.reject(lambda j,s:[v.update(carried='{}')for v in s[m.CASES[0]]['frames'][4][1]['server']['menus']])
        self.reject(lambda j,s:s[m.CASES[0]]['frames'][5][1]['server']['inventory'][9].update(item='{}'))
        self.reject(lambda j,s:s[m.CASES[0]]['frames'][5][1]['server']['inventory'][12].update(item=s[m.CASES[0]]['frames'][5][1]['server']['inventory'][12]['item'].replace('[B;','[I;')))
    def test_true_tank_formation_capacity_types_and_matching_populated_state_required(self):
        self.reject(lambda j,s:s[m.CASES[-1]]['probes'][1][1].update(shapeValid=False))
        self.reject(lambda j,s:s[m.CASES[-1]]['probes'][1][1]['client']['components']['tank'].update(capacity=1000))
        self.reject(lambda j,s:s[m.CASES[-1]]['probes'][2][1].update(fixture=s[m.CASES[-1]]['probes'][2][1]['fixture'].replace('amount: 1000L','amount: 1000')))
        self.reject(lambda j,s:s[m.CASES[-1]]['probes'][2][1]['client']['components']['tank'].update(fluid='minecraft:lava'))
    def test_exact_original_blueprint_and_gauge_geometry_required(self):
        self.reject(lambda j,s:s[m.CASES[-1]]['probes'][0][1]['blueprint'].pop())
        self.reject(lambda j,s:s[m.CASES[-1]]['probes'][2][1]['blueprint'][0].update(block='minecraft:stone'))
        self.reject(lambda j,s:s[m.CASES[-1]]['frames'][2][1]['client']['windows'][0]['nativeValues'].update(miComponentBounds=[]))
        self.reject(lambda j,s:s[m.CASES[-1]]['frames'][2][1]['client']['windows'][0]['template']['regions'][0]['source'].update(height=35))
    def test_tank_reopening_inventory_cursors_and_exact_variant_required(self):
        self.reject(lambda j,s:s[m.CASES[-1]]['frames'][3][1]['server']['menus'][0].update(carried=m.physical_item('1.21.1','minecraft:diamond',1)))
        self.reject(lambda j,s:s[m.CASES[-1]]['frames'][3][1]['server']['inventory'][13].update(item=m.physical_item('1.21.1','minecraft:stone',1)))
        self.reject(lambda j,s:s[m.CASES[-1]]['probes'][3][1].update(fixture=s[m.CASES[-1]]['probes'][3][1]['fixture'].replace('large_tank','configurable_tank')))
        with self.assertRaises(AssertionError):m.specification('1.21.1/fabric',m.CASES[-1],[m.SERVER])


if __name__=='__main__':unittest.main(verbosity=2)
