import unittest
from entry_gap_coverage import classify

class EntryGapCoverageTests(unittest.TestCase):
    gap={'minecraft':'1.21.1','loader':'neoforge','ui':'settings','screen':'native.SettingsScreen','publisher_sha256':['publisher-a']}
    def case(self,**overrides):
        return dict({'target':'1.21.1/neoforge','native_screens':['native.SettingsScreen'],'publisher_sha256':['publisher-a'],
                     'subjects_match_current_jars':True,'status':'passed','id':'native-settings','coverage':'Return with carried item.'},**overrides)
    def test_stale_subject_or_different_publisher_does_not_cover_an_atlas_entry(self):
        for case in [self.case(subjects_match_current_jars=False),self.case(publisher_sha256=['publisher-b']),self.case(target='1.21.1/fabric')]:
            self.assertEqual('not-entered-on-current-subjects',classify(self.gap,[case])['status'])
    def test_entry_only_never_becomes_a_functional_scenario_pass(self):
        self.assertEqual('entry-only',classify(self.gap,[self.case(status='entry-and-policy-only')])['status'])
    def test_pass_retains_its_actual_scope_instead_of_certifying_all_features(self):
        row=classify(self.gap,[self.case(status='failed'),self.case()])
        self.assertEqual('recorded-scenario-passed',row['status'])
        self.assertEqual('Return with carried item.',row['coverage'])
