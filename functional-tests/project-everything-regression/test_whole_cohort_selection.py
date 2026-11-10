import unittest
from run_reconciled_followups import latest_whole

class WholeCohortSelectionTests(unittest.TestCase):
    def row(self,**overrides):
        return dict({'target':'1.20.1/fabric','fixture':'1201-fabric-inventory-batch-v2','scope':'complete-discovery-pass','status':'failed'},**overrides)
    def test_a_partial_pass_cannot_erase_another_failure_in_the_whole_cohort(self):
        original=self.row()
        chosen=latest_whole([[original],[self.row(scope='case-reproduction',status='passed'),self.row(scope='smoke',status='passed')]])
        self.assertEqual([original],chosen)
    def test_old_fixture_alias_and_current_name_are_one_cohort_without_mutating_evidence(self):
        old=self.row(fixture='1201-fabric-regression-inventory-no-guard')
        newer=self.row(status='passed')
        self.assertEqual([newer],latest_whole([[old],[newer]]))
        self.assertEqual('1201-fabric-regression-inventory-no-guard',old['fixture'])
    def test_the_same_fixture_label_on_different_platforms_is_not_conflated(self):
        self.assertEqual(2,len(latest_whole([[self.row(),self.row(target='1.20.1/forge')]])))
