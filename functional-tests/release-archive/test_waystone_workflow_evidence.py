"""Offline negative oracles for the report-only modifier-entry reconciliation."""
from pathlib import Path
import copy, importlib.util, sys, unittest

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
spec=importlib.util.spec_from_file_location('waystone_workflow_evidence',ROOT/'tools/reconcile-waystone-release-evidence.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)


class WaystoneWorkflowEvidenceTests(unittest.TestCase):
    def report(self):
        button=dict(type='regression-native-widget',message='Manage Modifiers',ordinal=0,button=0,ticks=3,
            parent_menu='net.blay09.mods.waystones.menu.WaystoneEditMenu',
            parent_screen='net.blay09.mods.waystones.client.gui.screen.WaystoneEditScreen')
        before=[dict(type='fixture',block=m.OLD,ticks=20),button]
        after=copy.deepcopy(before);after[0]['block']=m.NEW
        case=dict(id=m.CASE,status='passed',source='original-native-atlas',expected_native_menu=m.MENU,
            expected_native_screen=m.SCREEN,expected_nonplayer_slot_count=5,native_menus=[m.MENU],native_screens=[m.SCREEN],
            native_menu_types=['waystones:waystone_modifiers'],slot_access_contract='public-desktop-menu-bindings',
            virtual_policy_query_contract=True,item_serialization_contract='native-item-codec',
            original_entry_actions=before,actions=after,session=1,slot=0,sampled_input_slots=[0,2,4],
            item='minecraft:diamond',amount=7,preexisting_tagged_count=0,expected_tagged_count=7,
            registry_entry_adaptation=dict(previous_block=m.OLD,registered_block=m.NEW,helper_sha256='a'*64))
        checks=[dict(case=m.CASE,check=name,passed=True)for name in m.REQUIRED.elements()]
        return dict(minecraft='26.2',loader='fabric',test_driver_sha256='b'*64,
            selection=dict(only=[m.CASE],max_cases=None,mods=[]),cases=[case],checks=checks)

    def validate(self,report,target='26.2/fabric'):
        return m.validate_case(target,report,'a'*64,'b'*64)

    def test_complete_original_entry_and_all_socket_assertions_are_required(self):
        report=self.report()
        self.assertEqual(self.validate(report),20)
        report['checks'].append(dict(case=m.CASE,check='extra original compound gesture',passed=True))
        self.assertEqual(self.validate(report),21)
        for key in ['native_menus','native_screens','sampled_input_slots']:
            bad=self.report();bad['cases'][0][key]=bad['cases'][0][key][:-1]
            with self.assertRaises(AssertionError):self.validate(bad)

    def test_missing_duplicate_failed_and_unrelated_case_checks_are_refused(self):
        for change in ['missing','duplicate','failed','truthy','unrelated']:
            bad=self.report()
            if change=='missing':bad['checks'].pop()
            elif change=='duplicate':bad['checks'].append(copy.deepcopy(bad['checks'][0]))
            elif change=='failed':bad['checks'][0]['passed']=False
            elif change=='truthy':bad['checks'][0]['passed']=1
            else:bad['checks'][0]['case']='another-container'
            with self.assertRaises(AssertionError):self.validate(bad)

    def test_only_the_disclosed_block_prerequisite_may_change(self):
        for change in ['block','button','parent','ordinal','extra','helper']:
            bad=self.report();case=bad['cases'][0]
            if change=='block':case['actions'][0]['block']='minecraft:chest'
            elif change=='button':case['actions'][1]['message']='Force Modifiers'
            elif change=='parent':case['actions'][1]['parent_menu']='unrelated.Parent'
            elif change=='ordinal':case['actions'][1]['ordinal']=True
            elif change=='extra':case['actions'].append(dict(type='forced-provider'))
            else:case['registry_entry_adaptation']['helper_sha256']='c'*64
            with self.assertRaises(AssertionError):self.validate(bad)

    def test_wrong_target_driver_selection_and_native_contract_are_refused(self):
        with self.assertRaises(AssertionError):self.validate(self.report(),'1.21.1/fabric')
        for key,value in [('test_driver_sha256','c'*64),('selection',dict(only=[],max_cases=None,mods=[])),('loader','neoforge')]:
            bad=self.report();bad[key]=value
            with self.assertRaises(AssertionError):self.validate(bad)
        for key,value in [('status','entry-and-policy-only'),('expected_nonplayer_slot_count',4),
                          ('virtual_policy_query_contract',1),('expected_native_menu','another.Menu')]:
            bad=self.report();bad['cases'][0][key]=value
            with self.assertRaises(AssertionError):self.validate(bad)

    def test_full_typed_resource_and_slot_sample_contract_cannot_be_dropped(self):
        for key,value in [('slot',True),('amount',1),('expected_tagged_count',1),('preexisting_tagged_count',7),
                          ('item','minecraft:air'),('item_serialization_contract','display-only'),('session',0)]:
            bad=self.report();bad['cases'][0][key]=value
            with self.assertRaises(AssertionError):self.validate(bad)

    def test_real_five_slot_shape_all_transfer_witnesses_and_closed_cursor_are_required(self):
        case=self.report()['cases'][0]
        signature='{"id":"minecraft:diamond","count":1,"components":{"minecraft:custom_data":{"siu_regression_marker":42,"typed":17}}}'
        resource=dict(item='minecraft:diamond',count=7,signature=signature)
        empty=dict(item='minecraft:air',count=0,signature='empty')
        slots=[dict(index=i,player=False,virtual=False,**empty)for i in range(5)]
        menu=dict(type=m.MENU,session=case['session'],valid=True,slots=slots)
        states=[dict(inventory=[resource],menus=[copy.deepcopy(menu)],cursor=empty)]
        for slot in [0,2,4]:
            inserted=copy.deepcopy(menu);inserted['slots'][slot].update(resource)
            states.append(dict(inventory=[empty],menus=[inserted],cursor=empty))
        states.append(dict(inventory=[empty],menus=[],cursor=resource))
        self.assertEqual(m.validate_native_states(case,states),dict(nonplayer_slots=5,sampled_slots=[0,2,4],closed_cursor=True,states=5))
        for change in ['shape','wrong-source','virtual','missing-insertion','altered-components','no-close']:
            bad=copy.deepcopy(states)
            if change=='shape':bad[0]['menus'][0]['slots'].pop()
            elif change=='wrong-source':
                for state in bad:
                    for current in state['menus']:current['type']='unrelated.Menu'
            elif change=='virtual':bad[0]['menus'][0]['slots'][1]['virtual']=True
            elif change=='missing-insertion':bad.pop(2)
            elif change=='altered-components':bad[2]['menus'][0]['slots'][2]['signature']=signature+' '
            else:bad.pop()
            with self.assertRaises(AssertionError):m.validate_native_states(case,bad)


if __name__=='__main__':unittest.main()
