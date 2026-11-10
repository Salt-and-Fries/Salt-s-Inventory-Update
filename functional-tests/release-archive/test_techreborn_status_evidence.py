"""Offline rejection checks for status evidence; these are not gameplay passes."""
from pathlib import Path
import copy,importlib.util,sys,unittest
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
spec=importlib.util.spec_from_file_location('review_techreborn_status',ROOT/'tools/reconcile-techreborn-status-release-evidence.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
DRIVER='a'*64

def fixture():
    item='{id:"minecraft:diamond",count:3,components:{"minecraft:custom_data":{release_tr_status_marker:29,typed:[B;3b,8b]}}}'
    states={}
    for ordinal,(key,(_,screen,entity,labels))in enumerate(m.GROUPS.items()):
        native=dict(type='techreborn.blockentity.'+entity,energy=0,capacity=10000)
        if key in ['launchpad','player_detector']:native.update(selection=1,minimum=0 if key=='launchpad'else 1,maximum=32)
        if key=='basic_solar_panel':native.update(generating=True,rate=1)
        base=dict(physical_slots=0,native=native,position=dict(x=ordinal,y=64,z=0),
            inventory=[dict(index=i,item=item if i==9 else'{}')for i in range(41)],cursors=[dict(menu=m.MENU,item='{}')],
            client=dict(menu=m.MENU,screen='techreborn.client.gui.'+screen,native=copy.deepcopy(native),session=ordinal+10,
                bounds=dict(x=12,y=20,width=150,height=90),buttons=[dict(source_x=i*10,visible=True,active=True)for i in range(4)]))
        frames=[copy.deepcopy(base)for _ in labels]
        for frame in frames[1:]:frame['client']['bounds']['x']+=18
        frames[-1]['client']['session']+=100
        def change(i,**values):
            frames[i]['native'].update(values);frames[i]['client']['native'].update(values)
        if key=='elevator':
            for i in [2,3,4]:change(i,energy=5)
        elif key in ['launchpad','player_detector']:
            previous=native['selection'];bulk=32 if key=='launchpad'else 16
            for i,delta in enumerate([-bulk,1,-1,bulk,-bulk],2):
                previous=max(native['minimum'],min(native['maximum'],previous+delta));change(i,selection=previous)
            change(7,selection=previous)
        else:
            change(2,energy=5);change(3,energy=5,generating=False,rate=0);change(4,energy=5,generating=False,rate=0)
            change(5,energy=8);change(6,energy=10)
        states[key]=frames
    journal=dict(complete=True,cases=list(m.GROUPS),driver_sha256=DRIVER,
        checks=[dict(case=key,name=name,passed=True)for(key,name),count in m.REQUIRED.items()for _ in range(count)])
    return journal,states,dict(menus=[dict(session=0,type='player')])

class StatusEvidence(unittest.TestCase):
    def accepted(self,j,s,r):return m.validate('1.21.1/fabric',j,s,r,DRIVER)
    def rejected(self,change):
        j,s,r=fixture();change(j,s,r)
        with self.assertRaises((AssertionError,KeyError,ValueError,TypeError)):self.accepted(j,s,r)
    def test_reviewed_modern_and_legacy_typed_contracts(self):
        j,s,r=fixture();self.assertEqual(self.accepted(j,s,r),dict(assertions=38,states=28,native_panels=4))
        for frames in s.values():
            for frame in frames:frame['inventory'][9]['item']='{id:"minecraft:diamond",Count:3b,tag:{release_tr_status_marker:29,typed:[B;3b,8b]}}'
        self.assertEqual(m.validate('1.20.1/fabric',j,s,r,DRIVER)['native_panels'],4)
    def test_missing_or_duplicate_original_assertion_rejected(self):
        self.rejected(lambda j,s,r:j['checks'].pop())
        self.rejected(lambda j,s,r:j['checks'].append(copy.deepcopy(j['checks'][0])))
    def test_incomplete_nonboolean_or_changed_driver_rejected(self):
        self.rejected(lambda j,s,r:j.update(complete=1))
        self.rejected(lambda j,s,r:j['checks'][0].update(passed=1))
        self.rejected(lambda j,s,r:j.update(driver_sha256='b'*64))
    def test_generic_menu_alone_does_not_identify_machine(self):
        self.rejected(lambda j,s,r:s['launchpad'][0]['client'].update(screen='another.Screen'))
        self.rejected(lambda j,s,r:s['elevator'][0]['native'].update(type='another.BlockEntity'))
        self.rejected(lambda j,s,r:s['elevator'][0].update(physical_slots=1))
    def test_all_immediate_frames_and_movement_required(self):
        self.rejected(lambda j,s,r:s['launchpad'].pop())
        self.rejected(lambda j,s,r:s['elevator'][1]['client'].update(bounds=copy.deepcopy(s['elevator'][0]['client']['bounds'])))
    def test_selector_click_must_change_exact_native_value(self):
        self.rejected(lambda j,s,r:s['launchpad'][3]['client']['native'].update(selection=0))
        def wrong(j,s,r):
            for values in [s['player_detector'][4]['native'],s['player_detector'][4]['client']['native']]:values['selection']=2
        self.rejected(wrong)
    def test_hidden_duplicate_buttons_and_lost_persistence_rejected(self):
        self.rejected(lambda j,s,r:s['launchpad'][1]['client']['buttons'][0].update(visible=False))
        self.rejected(lambda j,s,r:s['launchpad'][1]['client']['buttons'][1].update(source_x=0))
        self.rejected(lambda j,s,r:s['launchpad'][-1]['native'].update(selection=4))
    def test_native_solar_obstruction_and_restoration_required(self):
        self.rejected(lambda j,s,r:s['basic_solar_panel'][3]['native'].update(generating=True))
        self.rejected(lambda j,s,r:s['basic_solar_panel'][5]['native'].update(energy=5))
    def test_elevator_energy_must_change_and_synchronize(self):
        self.rejected(lambda j,s,r:s['elevator'][2]['native'].update(energy=0))
        self.rejected(lambda j,s,r:s['elevator'][4]['client']['native'].update(energy=4))
    def test_typed_item_conservation_in_every_frame(self):
        self.rejected(lambda j,s,r:s['elevator'][1]['inventory'][9].update(item=s['elevator'][1]['inventory'][9]['item'].replace('[B;3b,8b]','[I;3,8]')))
        self.rejected(lambda j,s,r:s['launchpad'][2]['inventory'][10].update(item='{id:"minecraft:stone",count:1}'))
    def test_missing_cursor_and_final_source_removal_required(self):
        self.rejected(lambda j,s,r:s['launchpad'][0].update(cursors=[]))
        self.rejected(lambda j,s,r:r['menus'].append(dict(session=s['basic_solar_panel'][-1]['client']['session'])))
    def test_only_explicit_targets_aliases_and_classes_accepted(self):
        self.assertEqual(m.specification('26.3/fabric','registry-techreborn-launchpad',[m.MENU]),'launchpad')
        for target,case,classes in [('26.3/neoforge','registry-techreborn-launchpad',[m.MENU]),('26.3/fabric','another-case',[m.MENU]),('26.3/fabric','registry-techreborn-launchpad',['other.Menu'])]:
            with self.subTest(target=target,case=case):
                with self.assertRaises(AssertionError):m.specification(target,case,classes)
    def workflows(self):
        common=dict(schema=m.SCHEMA,target='1.20.1/fabric',case='registry-techreborn-elevator',disposition='functional-pass',
            original_native_classes=[m.MENU],native_classes=[m.MENU,'techreborn.client.gui.GuiElevator'],subjects={'core':['core.jar','a'*64]},scope=m.SCOPE,limits=m.LIMITS)
        return [dict(copy.deepcopy(common),sealed_evidence=dict(file=name+'.json',sha256=name*64),publishers=[dict(filename='TechReborn-'+name+'.jar',sha512=name*128)])for name in ['a','b']]
    def test_alternate_publisher_evidence_keeps_both_original_executions(self):
        workflows=self.workflows();original=copy.deepcopy(workflows);record=m.combine(workflows)
        self.assertEqual(workflows,original)
        self.assertEqual(record['additional_workflows'],[workflows[1]])
        self.assertEqual(record['publishers'],[v['publishers'][0]for v in workflows])
    def test_duplicate_or_nested_workflow_cannot_count_as_second_release(self):
        workflows=self.workflows()
        with self.assertRaises(AssertionError):m.combine([workflows[0],copy.deepcopy(workflows[0])])
        nested=m.combine(workflows)
        with self.assertRaises(AssertionError):m.combine([nested])
    def test_different_target_or_machine_cannot_be_merged(self):
        for field,value in [('target','1.21.1/fabric'),('case','registry-techreborn-launchpad'),('native_classes',['another.Menu']),('subjects',{'core':['other.jar','b'*64]})]:
            workflows=self.workflows();workflows[1][field]=value
            with self.subTest(field=field):
                with self.assertRaises(AssertionError):m.combine(workflows)
    def test_conflicting_metadata_for_same_publisher_digest_is_rejected(self):
        workflows=self.workflows();workflows[1]['publishers'][0]['sha512']=workflows[0]['publishers'][0]['sha512']
        with self.assertRaises(AssertionError):m.combine(workflows)

if __name__=='__main__':unittest.main(verbosity=2)
