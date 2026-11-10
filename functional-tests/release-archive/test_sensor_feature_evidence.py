"""Offline negative controls for sensor evidence; no gameplay pass is assigned."""
from pathlib import Path
import copy,hashlib,importlib.util,sys,unittest
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
p=ROOT/'tools/sensor_feature_evidence.py'
spec=importlib.util.spec_from_file_location('sensor_feature_evidence_proposal',p);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
DRIVER='a'*64

def fixture():
    item='{id:"minecraft:diamond",count:3,components:{"minecraft:custom_data":{release_sensor_marker:17,typed:[B;2b,5b]}}}'
    base=dict(inverted=False,sensingType='addon:nearby',inventory=[dict(index=i,item=item if i==9 else'{}')for i in range(41)],cursors=[dict(menu=m.MENU,item='{}')],
        client=dict(menu=m.MENU,screen=m.SCREEN,session=17,inverted=False,sensingType='addon:nearby',bounds=dict(x=12,y=24,width=160,height=80),checkbox=dict(active=True,visible=True),choices=[dict(type='addon:nearby'),dict(type='addon:found')]))
    states=[copy.deepcopy(base)for _ in m.LABELS]
    for i in [1,2,3]:states[i]['inverted']=states[i]['client']['inverted']=True
    for i in range(2,6):states[i]['sensingType']=states[i]['client']['sensingType']='addon:found'
    for i in range(3,6):states[i]['client']['bounds']['x']+=16;states[i]['client']['bounds']['y']+=12
    states[5]['client']['session']=18
    journal=dict(complete=True,cases=['rocket-sensor'],driver_sha256=DRIVER,checks=[dict(name=n,passed=True)for n in m.REQUIRED])
    removed=dict(menus=[dict(type='player',session=0)])
    return journal,states,removed

class SensorEvidence(unittest.TestCase):
    def accepted(self,j,s,r):return m.validate('1.21.1/fabric',j,s,r,DRIVER)
    def rejected(self,change):
        j,s,r=fixture();change(j,s,r)
        with self.assertRaises((AssertionError,KeyError,ValueError,TypeError)):self.accepted(j,s,r)
    def test_exact_original_modern_and_legacy_state_contracts(self):
        j,s,r=fixture();self.assertEqual(self.accepted(j,s,r)['assertions'],9)
        old='{id:"minecraft:diamond",Count:3b,tag:{release_sensor_marker:17,typed:[B;2b,5b]}}'
        for v in s:v['inventory'][9]['item']=old
        self.assertEqual(m.validate('1.20.1/forge',j,s,r,DRIVER)['states'],6)
    def test_missing_duplicate_or_unrelated_assertions_are_rejected(self):
        for mutation in [lambda j:j['checks'].pop(),lambda j:j['checks'].append(copy.deepcopy(j['checks'][0])),lambda j:j['checks'][0].update(name='other-feature')]:
            with self.subTest(mutation=mutation):self.rejected(lambda j,s,r:mutation(j))
    def test_incomplete_or_nonboolean_success_cannot_qualify(self):
        for mutation in [lambda j:j.update(complete=False),lambda j:j.update(complete=1),lambda j:j['checks'][0].update(passed=False),lambda j:j['checks'][0].update(passed=1)]:
            with self.subTest(mutation=mutation):self.rejected(lambda j,s,r:mutation(j))
    def test_changed_driver_or_different_feature_cannot_qualify(self):
        for mutation in [lambda j:j.update(driver_sha256='b'*64),lambda j:j.update(cases=['other-sensor'])]:
            with self.subTest(mutation=mutation):self.rejected(lambda j,s,r:mutation(j))
    def test_missing_state_or_wrong_native_surface_is_rejected(self):
        self.rejected(lambda j,s,r:s.pop())
        self.rejected(lambda j,s,r:s[0]['client'].update(menu='another.Menu'))
        self.rejected(lambda j,s,r:s[0]['client'].update(screen='another.Screen'))
    def test_client_only_or_ineffective_click_is_rejected(self):
        self.rejected(lambda j,s,r:s[1]['client'].update(inverted=False))
        def ineffective(j,s,r):s[1]['inverted']=s[1]['client']['inverted']=False
        self.rejected(ineffective)
        self.rejected(lambda j,s,r:s[2].update(sensingType='addon:nearby'))
    def test_exact_found_option_and_movement_are_required(self):
        self.rejected(lambda j,s,r:s[1]['client'].update(choices=[]))
        self.rejected(lambda j,s,r:s[3]['client'].update(bounds=copy.deepcopy(s[2]['client']['bounds'])))
        self.rejected(lambda j,s,r:s[3]['client']['checkbox'].update(visible=False))
    def test_reopen_persistence_and_source_removal_are_required(self):
        self.rejected(lambda j,s,r:s[5].update(inverted=True))
        self.rejected(lambda j,s,r:r['menus'].append(dict(type=m.MENU,session=18)))
    def test_typed_metadata_and_full_inventory_must_survive_every_step(self):
        self.rejected(lambda j,s,r:s[3]['inventory'][9].update(item=s[3]['inventory'][9]['item'].replace('[B;2b,5b]','[I;2,5]')))
        self.rejected(lambda j,s,r:s[2]['inventory'][10].update(item='{id:"minecraft:stone",count:1}'))
        self.rejected(lambda j,s,r:s[0]['inventory'][9].update(item=s[0]['inventory'][9]['item'].replace('count:3','count:3s')))
    def test_nonempty_or_missing_cursor_cannot_be_hidden(self):
        self.rejected(lambda j,s,r:s[4]['cursors'][0].update(item='{id:"minecraft:diamond",count:3}'))
        self.rejected(lambda j,s,r:s[1].update(cursors=[]))
    def test_boolean_ids_and_bounds_are_rejected(self):
        self.rejected(lambda j,s,r:s[0]['inventory'][0].update(index=False))
        self.rejected(lambda j,s,r:s[0]['client'].update(session=True))
        self.rejected(lambda j,s,r:s[3]['client']['bounds'].update(x=True))
    def test_only_reviewed_alias_target_and_native_class_are_allowed(self):
        self.assertEqual(m.specification('1.20.1/forge','registry-ad_astra_giselle_addon-rocket_sensor',[m.MENU]),(m.MENU,))
        for t,c,cl in [('26.3/fabric','registry-ad_astra_giselle_addon-rocket_sensor',[m.MENU]),('1.21.1/fabric','another-case',[m.MENU]),('1.21.1/fabric','atlas-ad-astra-giselle-addon-rocket-sensor',['other.Menu']),('1.21.1/fabric','atlas-ad-astra-giselle-addon-rocket-sensor',[m.MENU,m.MENU])]:
            with self.subTest(target=t,case=c):
                with self.assertRaises(AssertionError):m.specification(t,c,cl)

if __name__=='__main__':unittest.main(verbosity=2)
