"""Negative controls against silently hiding new current discovery gaps."""
from pathlib import Path
import importlib.util, sys, unittest

path=Path(__file__).resolve().parents[2]/'tools/build-release-coverage-backlog.py'
sys.path.insert(0,str(path.parent))
spec=importlib.util.spec_from_file_location('release_backlog_discovery_oracle',path)
backlog=importlib.util.module_from_spec(spec);spec.loader.exec_module(backlog)


class DiscoveryGaps(unittest.TestCase):
    def setUp(self):
        self.old=[dict(target='1.20.1/fabric',case='known-menu',native_classes=['native.KnownMenu'])]
    @staticmethod
    def entry(case,cls='native.KnownMenu',status='entry-and-policy-only'):
        return dict(target='1.20.1/fabric',case=dict(id=case,status=status,native_menus=[cls]))
    def test_new_opening_case_cannot_disappear_into_historical_inventory(self):
        new=self.entry('new-menu');old=self.entry('known-menu')
        self.assertEqual(backlog.current_discovery_gaps(self.old,[old,new,new]),[new])
    def test_same_case_name_cannot_hide_a_different_native_workflow(self):
        changed=self.entry('known-menu','native.DifferentMenu')
        self.assertEqual(backlog.current_discovery_gaps(self.old,[changed]),[changed])
    def test_passed_case_does_not_create_an_opening_only_gap(self):
        self.assertEqual(backlog.current_discovery_gaps(self.old,[self.entry('new-menu',status='passed')]),[])
    def test_missing_native_observation_cannot_inherit_an_old_classification(self):
        missing=self.entry('known-menu');missing['case']['native_menus']=[]
        self.assertEqual(backlog.current_discovery_gaps(self.old,[missing]),[missing])

    def test_invalid_fixture_is_not_silently_dropped(self):
        invalid=self.entry('new-menu',status='invalid-fixture-workflow');invalid['case']['native_menus']=[]
        self.assertEqual(backlog.current_discovery_gaps(self.old,[invalid]),[invalid])

    def test_unrecognized_incomplete_result_requires_a_disposition(self):
        unknown=self.entry('new-menu',status='new-unqualified-result')
        self.assertEqual(backlog.current_discovery_gaps(self.old,[unknown]),[unknown])

    def test_entry_only_observation_is_attached_without_becoming_a_pass(self):
        entry=self.entry('known-menu')
        self.assertEqual([entry],backlog.matching_observations([entry],'1.20.1/fabric',['native.KnownMenu'],'known-menu'))
        self.assertEqual('entry-and-policy-only',entry['case']['status'])

    def test_scoped_observation_requires_exact_target_class_and_optional_case(self):
        entry=self.entry('known-menu')
        self.assertEqual([],backlog.matching_observations([entry],'1.21.1/fabric',['native.KnownMenu'],'known-menu'))
        self.assertEqual([],backlog.matching_observations([entry],'1.20.1/fabric',['native.OtherMenu'],'known-menu'))
        self.assertEqual([],backlog.matching_observations([entry],'1.20.1/fabric',['native.KnownMenu'],'different-menu'))

    def test_failed_entry_can_locate_declared_publisher_without_claiming_class_observation(self):
        entry=self.entry('new-menu',status='invalid-fixture-workflow')['case']
        entry['native_menus']=[]
        entry.update(expected_native_menu='native.DeclaredMenu',expected_native_screen='native.DeclaredScreen')
        result=backlog.discovery_class_evidence(entry)
        self.assertEqual(result,dict(observed=[],declared=['native.DeclaredMenu','native.DeclaredScreen'],
            pin_classes=['native.DeclaredMenu','native.DeclaredScreen'],pin_basis='declared-entry-contract-not-observed'))
        self.assertEqual(entry['native_menus'],[])
        self.assertEqual(entry['status'],'invalid-fixture-workflow')

    def test_expected_class_cannot_replace_an_observed_different_native_class(self):
        entry=self.entry('new-menu','native.ObservedMenu')['case']
        entry['expected_native_menu']='native.DeclaredMenu'
        result=backlog.discovery_class_evidence(entry)
        self.assertEqual(result['observed'],['native.ObservedMenu'])
        self.assertEqual(result['pin_classes'],['native.ObservedMenu'])
        self.assertEqual(result['declared'],['native.DeclaredMenu'])
        self.assertEqual(result['pin_basis'],'observed-native-class')
        del entry['expected_native_menu'];entry['native_menus']=[]
        self.assertEqual(backlog.discovery_class_evidence(entry)['pin_basis'],'no-native-class-observed-or-declared')
        for value in [True,'not a native class','native/Menu']:
            entry['expected_native_menu']=value
            with self.assertRaises(AssertionError):backlog.discovery_class_evidence(entry)


if __name__=='__main__':unittest.main()
