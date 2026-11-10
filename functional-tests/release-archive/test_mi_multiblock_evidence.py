"""Offline corruption controls for original native MI controller evidence."""
from pathlib import Path
import copy,importlib.util,sys,unittest
import nbtlib
import test_mi_infrastructure_evidence as base_fixture

ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
spec=importlib.util.spec_from_file_location('review_mi_multiblock',ROOT/'tools/reconcile-mi-multiblock-release-evidence.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
DRIVER='a'*64


def fixture(target='1.21.1/neoforge',creosote=True):
    mc=target.split('/')[0];unit=81000 if mc=='1.20.1'else 1000
    _,base=base_fixture.fixture(target);template=base[base_fixture.m.CASES[-1]]['frames'][0][1]
    blueprint=copy.deepcopy(base[base_fixture.m.CASES[-1]]['probes'][0][1]['blueprint'])
    for row in blueprint:row['allows']=['modern_industrialization:'+h.lower()for h in m.HATCHES]
    selected=m.select_hatches(blueprint);states={};checks=[]
    def resource(item,number,kind):
        key=('item'if kind=='items'else'fluid')if mc=='1.20.1'else'id'
        return nbtlib.Compound(dict(amount=nbtlib.Long(number),key=nbtlib.parse_nbt('{'+key+':"'+item+'"}')if number else nbtlib.Compound()))
    for case in m.CASES:
        machine=case.removeprefix(m.PREFIX)
        labels=['empty','native-shape-tray-opened','native-formed-idle','native-active-recipe']
        if machine!='steam_quarry':labels.append('native-recipe-completed')
        labels+=['native-structure-invalidated','native-invalid-controller-reopened']
        frames=[];probes=[];native_pieces=[]
        for index,label in enumerate(labels):
            frame=copy.deepcopy(template)
            tag=nbtlib.parse_nbt(frame['server']['fixture']);tag['id']=nbtlib.String('modern_industrialization:'+machine)
            frame['server']['fixture']=tag.snbt()
            window=frame['client']['windows'][0]
            window['template']['regions'][0]['id']='native-working-body'
            window['nativeValues']['miComponentBounds']=[dict(component='CraftingMultiblockGuiClient',reserved=True,x=4,y=16,width=166,height=80)]
            if label=='native-shape-tray-opened':window['template']['regions'].append(dict(id='native-shape-tray',source=dict(x=5,y=100,width=60,height=30)))
            frames.append((label,frame))
            if label=='native-formed-idle':
                native_pieces=[]
                for wanted in selected:
                    kind='items'if wanted['kind'].startswith('ITEM')else'fluids'
                    piece=nbtlib.Compound(dict(id=nbtlib.String(wanted['block']),**{k:nbtlib.Int(wanted[k])for k in ['x','y','z']}))
                    piece[kind]=nbtlib.List[nbtlib.Compound]([resource('minecraft:air',0,kind)])
                    native_pieces.append(dict(x=wanted['x'],y=wanted['y'],z=wanted['z'],nbt=piece.snbt()))
            elif label=='native-active-recipe':
                piece=nbtlib.parse_nbt(native_pieces[2]['nbt']);piece['fluids'][0]=resource('modern_industrialization:steam',3*unit,'fluids');native_pieces[2]['nbt']=piece.snbt()
            elif label=='native-recipe-completed':
                piece=nbtlib.parse_nbt(native_pieces[1]['nbt']);piece['items'][0]=resource('modern_industrialization:'+('coke'if machine=='coke_oven'else'steel_ingot'),1,'items');native_pieces[1]['nbt']=piece.snbt()
                if creosote and machine=='coke_oven':
                    piece=nbtlib.parse_nbt(native_pieces[3]['nbt']);piece['fluids'][0]=resource('modern_industrialization:creosote',unit//2,'fluids');native_pieces[3]['nbt']=piece.snbt()
            formed=label in ['native-formed-idle','native-active-recipe','native-recipe-completed']
            active=label=='native-active-recipe'
            client=dict(shapeValid=formed,active=active)
            if active:client['progress']=.28
            probe=dict(controller=tag.snbt(),shapeValid=formed,active=active,progress=.3 if active else 0,
                client=client,operatingState='WORKING'if active else'IDLE'if formed else'INVALID',blueprint=copy.deepcopy(blueprint),pieces=copy.deepcopy(native_pieces))
            probes.append(('native-unformed'if label=='empty'else label,probe))
        states[case]=dict(frames=frames,probes=probes)
        checks.extend(dict(case=case,name=name,passed=True)for name in m.NAMES if machine!='steam_quarry'or name!=m.NAMES[5])
    checks.append(dict(case=m.CASES[-1],name='native multiblock windows close all owned sessions',passed=True))
    return dict(complete=True,cases=list(m.CASES),driver_sha256=DRIVER,checks=checks),states


def change_piece(probe,index,rewrite):
    row=probe['pieces'][index];tag=nbtlib.parse_nbt(row['nbt']);rewrite(tag);row['nbt']=tag.snbt()


class MultiblockEvidence(unittest.TestCase):
    def reject(self,mutate):
        j,s=fixture();mutate(j,s)
        with self.assertRaises((AssertionError,KeyError,ValueError,TypeError)):m.validate('1.21.1/neoforge',j,s,DRIVER)
    def test_both_native_families_and_probabilistic_creosote_keep_exact_scopes(self):
        for target in m.TARGETS:
            for creosote in [True,False]:
                j,s=fixture(target,creosote);v=m.validate(target,j,s,DRIVER)
                self.assertEqual(v['assertions'],24);self.assertEqual(v['states'],40);self.assertEqual(v['completed_guaranteed_recipes'],2)
                self.assertTrue(v['contract'].endswith('not-quarry-completion'))
    def test_all_original_assertions_order_boolean_completion_and_driver_required(self):
        for change in [lambda j,s:j.update(complete=1),lambda j,s:j.update(driver_sha256='b'*64),lambda j,s:j['checks'].pop(),
            lambda j,s:j['checks'][0].update(passed=1),lambda j,s:j['checks'].reverse()]:self.reject(change)
    def test_exact_controller_variant_native_classes_and_menu_required(self):
        self.reject(lambda j,s:s[m.CASES[0]]['probes'][0][1].update(controller=s[m.CASES[0]]['probes'][0][1]['controller'].replace('coke_oven','steam_quarry')))
        self.reject(lambda j,s:s[m.CASES[0]]['frames'][0][1]['observed'].update(screen='other.Screen'))
        self.reject(lambda j,s:s[m.CASES[0]]['frames'][0][1]['server'].update(menus=[]))
        with self.assertRaises(AssertionError):m.specification('1.21.1/fabric',m.CASES[0],[m.SERVER])
    def test_both_sides_must_retain_real_formation_and_active_progress(self):
        self.reject(lambda j,s:s[m.CASES[0]]['probes'][2][1].update(shapeValid=False))
        for value in [0,True,float('nan')]:self.reject(lambda j,s:s[m.CASES[0]]['probes'][3][1]['client'].update(progress=value))
        self.reject(lambda j,s:s[m.CASES[0]]['probes'][3][1].update(active=False))
    def test_native_blueprint_and_distinct_allowed_hatch_placements_required(self):
        self.reject(lambda j,s:s[m.CASES[0]]['probes'][0][1]['blueprint'].clear())
        self.reject(lambda j,s:s[m.CASES[0]]['probes'][2][1]['pieces'].__setitem__(0,copy.deepcopy(s[m.CASES[0]]['probes'][2][1]['pieces'][1])))
        self.reject(lambda j,s:s[m.CASES[0]]['probes'][3][1]['blueprint'][0].update(allows=[]))
    def test_exact_guaranteed_output_and_bounded_native_byproduct_required(self):
        self.reject(lambda j,s:change_piece(s[m.CASES[0]]['probes'][4][1],1,lambda t:t['items'][0].update(amount=nbtlib.Long(2))))
        self.reject(lambda j,s:change_piece(s[m.CASES[1]]['probes'][4][1],1,lambda t:t['items'][0].update(key=nbtlib.parse_nbt('{id:"minecraft:diamond"}'))))
        self.reject(lambda j,s:change_piece(s[m.CASES[0]]['probes'][4][1],3,lambda t:t['fluids'][0].update(amount=nbtlib.Long(501))))
    def test_source_invalidation_and_full_typed_hatch_persistence_required(self):
        self.reject(lambda j,s:s[m.CASES[-1]]['probes'][-2][1]['client'].update(shapeValid=True))
        self.reject(lambda j,s:change_piece(s[m.CASES[0]]['probes'][-1][1],1,lambda t:t['items'][0].update(amount=nbtlib.Int(1))))
        self.reject(lambda j,s:change_piece(s[m.CASES[-1]]['probes'][-1][1],2,lambda t:t.update(added=nbtlib.Byte(1))))
    def test_native_shape_tray_and_complete_status_geometry_required(self):
        self.reject(lambda j,s:s[m.CASES[0]]['frames'][1][1]['client']['windows'][0]['template']['regions'][1]['source'].update(width=32))
        self.reject(lambda j,s:s[m.CASES[0]]['frames'][2][1]['client']['windows'][0]['nativeValues'].update(miComponentBounds=[]))
        self.reject(lambda j,s:s[m.CASES[0]]['frames'][3][1]['client']['windows'][0]['template']['regions'][0]['source'].update(height=45))
    def test_quarry_cannot_inherit_a_completed_recipe_or_omit_its_reopen_states(self):
        self.reject(lambda j,s:s[m.CASES[-1]]['probes'].pop())
        self.reject(lambda j,s:s[m.CASES[-1]]['frames'].insert(4,copy.deepcopy(s[m.CASES[0]]['frames'][4])))
        self.reject(lambda j,s:j['checks'].insert(-1,dict(case=m.CASES[-1],name=m.NAMES[5],passed=True)))
    def test_full_foreign_item_data_and_cursor_ownership_remain_conserved(self):
        self.reject(lambda j,s:s[m.CASES[0]]['frames'][3][1]['server']['inventory'][12].update(item=s[m.CASES[0]]['frames'][3][1]['server']['inventory'][12]['item'].replace('[B;','[I;')))
        self.reject(lambda j,s:s[m.CASES[1]]['frames'][4][1]['server']['menus'][0].update(carried=m.infra.physical_item('1.21.1','minecraft:stone',1)))


if __name__=='__main__':unittest.main(verbosity=2)
