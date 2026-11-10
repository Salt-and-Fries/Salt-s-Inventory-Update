"""Offline rejection of weak/wrong native status evidence, not gameplay."""
from pathlib import Path
import copy,importlib.util,sys,unittest
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
spec=importlib.util.spec_from_file_location('review_mi_energy_workflow',ROOT/'tools/reconcile-mi-energy-release-evidence.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
DRIVER='a'*64

def fixture(target='1.21.1/neoforge'):
    classes=m.native_classes(target)
    item=('{id:"minecraft:diamond",Count:7b,tag:{release_mi_status_marker:41,typed:[B;3b,6b]}}'if target=='1.20.1/fabric'else'{id:"minecraft:diamond",count:7,components:{"minecraft:custom_data":{release_mi_status_marker:41,typed:[B;3b,6b]}}}')
    states={}
    for hatch in m.HATCHES:
        frames=[]
        for energy in [0,1000,1000]:
            window=dict(definition='com.salts_inventory_update.client.automatic.NativeTemplateWindow',menuId='modern_industrialization:machine',nativeSlots=[],nativeWidgets=[],nativeBounds=dict(x=10,y=20,width=176,height=166),
                template=dict(regions=[dict(source=dict(x=0,y=0,width=176,height=80))]),nativeValues=dict(miComponentBounds=[dict(component='EnergyBarClient',x=76,y=39,width=13,height=18)]))
            server=dict(fixture='{id:"modern_industrialization:'+hatch+'",storedEu:'+str(energy)+'L,x:8,y:65,z:8}',
                inventory=[dict(index=i,item=item if i==12 else'{}')for i in range(41)],menus=[{'class':'player.Menu','carried':'{}'},{'class':m.SERVER,'carried':'{}'}])
            frames.append(dict(client=dict(windows=[window]),server=server,observed=dict(menu=classes[1],screen=classes[2],energy=[dict(type=classes[3],eu=energy,maxEu=2048)])))
        states[hatch]=frames
    journal=dict(complete=True,cases=[m.PREFIX+h for h in m.HATCHES],driver_sha256=DRIVER,
        checks=[dict(case=case,name=name,passed=True)for(case,name),count in m.REQUIRED.items()for _ in range(count)])
    return journal,states

class EnergyWorkflowEvidence(unittest.TestCase):
    def accepted(self,j,s):return m.validate('1.21.1/neoforge',j,s,DRIVER)
    def rejected(self,change):
        j,s=fixture();change(j,s)
        with self.assertRaises((AssertionError,KeyError,ValueError,TypeError)):self.accepted(j,s)
    def test_both_native_layout_families_have_exact_readonly_scope(self):
        for target in m.TARGETS:
            j,s=fixture(target);result=m.validate(target,j,s,DRIVER)
            self.assertEqual(result,dict(assertions=32,states=30,native_variants=10,contract='read-only-status-not-power-generation'))
        self.assertEqual(m.STATUS,'read-only-status-pass')
    def test_original_complete_boolean_assertion_set_and_driver_required(self):
        self.rejected(lambda j,s:j.update(complete=1));self.rejected(lambda j,s:j['checks'].pop())
        self.rejected(lambda j,s:j['checks'].append(copy.deepcopy(j['checks'][0])))
        self.rejected(lambda j,s:j['checks'][0].update(passed=1));self.rejected(lambda j,s:j.update(driver_sha256='b'*64))
    def test_generic_menu_does_not_substitute_for_exact_variant_or_native_screen(self):
        self.rejected(lambda j,s:s[m.HATCHES[0]][0]['server'].update(fixture='{id:"modern_industrialization:other_machine",storedEu:0L,x:8,y:65,z:8}'))
        self.rejected(lambda j,s:s[m.HATCHES[0]][0]['observed'].update(screen='other.Screen'))
        self.rejected(lambda j,s:s[m.HATCHES[0]][0]['observed']['energy'][0].update(type='other.Component'))
    def test_exact_energy_native_type_and_both_views_required(self):
        self.rejected(lambda j,s:s[m.HATCHES[0]][1]['observed']['energy'][0].update(eu=999))
        self.rejected(lambda j,s:s[m.HATCHES[0]][1]['server'].update(fixture='{id:"modern_industrialization:'+m.HATCHES[0]+'",storedEu:1000,x:8,y:65,z:8}'))
        self.rejected(lambda j,s:s[m.HATCHES[0]][0]['observed']['energy'][0].update(eu=False))
    def test_missing_frame_or_changed_full_foreign_inventory_rejected(self):
        self.rejected(lambda j,s:s[m.HATCHES[0]].pop())
        self.rejected(lambda j,s:s[m.HATCHES[1]][2]['server']['inventory'][13].update(item='{id:"minecraft:stone",count:1}'))
    def test_nonempty_cursor_missing_native_menu_or_vacuous_state_rejected(self):
        self.rejected(lambda j,s:s[m.HATCHES[0]][1]['server']['menus'][0].update(carried='{id:"minecraft:stone",count:1}'))
        self.rejected(lambda j,s:s[m.HATCHES[0]][0]['server'].update(menus=[]))
        self.rejected(lambda j,s:s[m.HATCHES[0]][0]['server']['menus'][1].update({'class':'other.Menu'}))
    def test_cut_or_missing_energy_component_geometry_rejected(self):
        self.rejected(lambda j,s:s[m.HATCHES[0]][0]['client']['windows'][0]['template']['regions'][0]['source'].update(height=45))
        self.rejected(lambda j,s:s[m.HATCHES[0]][0]['client']['windows'][0]['nativeValues'].update(miComponentBounds=[]))
    def test_typed_seed_and_explicit_target_classification_required(self):
        self.rejected(lambda j,s:s[m.HATCHES[0]][0]['server']['inventory'][12].update(item=s[m.HATCHES[0]][0]['server']['inventory'][12]['item'].replace('[B;3b,6b]','[I;3,6]')))
        self.assertEqual(m.specification('1.20.1/fabric',m.PREFIX+m.HATCHES[0],[m.SERVER]),m.HATCHES[0])
        for target,case,classes in [('1.21.1/fabric',m.PREFIX+m.HATCHES[0],[m.SERVER]),('1.20.1/fabric','other-case',[m.SERVER]),('1.20.1/fabric',m.PREFIX+m.HATCHES[0],['other.Menu'])]:
            with self.subTest(target=target,case=case):
                with self.assertRaises(AssertionError):m.specification(target,case,classes)

if __name__=='__main__':unittest.main(verbosity=2)
