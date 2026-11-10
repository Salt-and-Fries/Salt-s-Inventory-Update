"""Report safety: a targeted retry cannot erase unrelated failures or gaps."""
import unittest
from reconcile_results import merge,identity

def report(*rows):
    return {'cases':[{'id':name,'status':status} for name,status in rows],
            'checks':[{'case':name,'check':'measured assertion','passed':status=='passed'} for name,status in rows]}

class ReconciliationTests(unittest.TestCase):
    def test_alias_selector_replaces_the_earlier_atlas_failure_and_its_checks(self):
        old=report(('atlas-flower-pouch','failed'),('unrelated','failed'))
        result=merge(old,report(('flower-pouch','passed')),['flower-pouch'],'fresh-live-proof.json')
        self.assertEqual(['passed','failed'],[r['status'] for r in result['cases']])
        self.assertEqual('flower-pouch',result['cases'][0]['reproduction_case_id'])
        self.assertEqual('failed',result['cases'][0]['previous_attempt']['status'])
        self.assertEqual(1,sum(not c['passed'] for c in result['checks']))
    def test_alias_selector_also_propagates_a_new_failure_instead_of_preserving_an_old_pass(self):
        result=merge(report(('atlas-flower-pouch','passed')),report(('flower-pouch','failed')),
            ['flower-pouch'],'fresh-failure.json')
        self.assertEqual('failed',result['cases'][0]['status']);self.assertFalse(result['checks'][0]['passed'])
    def test_alias_selector_does_not_count_one_reproduction_twice(self):
        result=merge(report(('atlas-flower-pouch','failed'),('registry-flower_pouch','failed')),
            report(('flower-pouch','passed')),['flower-pouch'],'fresh-live-proof.json')
        self.assertEqual(['passed','superseded-duplicate-entry'],[r['status'] for r in result['cases']])
        self.assertEqual(1,len(result['checks']))
    def test_crab_trap_registry_alias_reuses_only_its_real_water_entry(self):
        self.assertEqual(identity('registry-crabbersdelight-crab_trap'),identity('atlas-crabbers-delight-crab-trap'))
        self.assertNotEqual(identity('atlas-crabbers-tackle-box'),identity('atlas-crabbers-delight-crab-trap'))
        result=merge(report(('registry-crabbersdelight-crab_trap','failed'),('atlas-crabbers-delight-crab-trap','passed')),
            report(('registry-crabbersdelight-crab_trap','passed')),
            ['registry-crabbersdelight-crab_trap','atlas-crabbers-delight-crab-trap'],'wet-native-proof.json')
        self.assertEqual(['passed','superseded-duplicate-entry'],[r['status'] for r in result['cases']])
        self.assertEqual(1,len(result['checks']))
    def test_retry_only_replaces_the_requested_case_and_its_checks(self):
        old=report(('a','failed'),('b','failed'),('c','passed'));new=report(('a','passed'))
        result=merge(old,new,['a'],'proof.json')
        self.assertEqual(['passed','failed','passed'],[r['status'] for r in result['cases']])
        self.assertEqual(1,sum(not c['passed'] for c in result['checks']))
        self.assertEqual('failed',old['cases'][0]['status'])
    def test_missing_reproduction_does_not_turn_a_failure_into_a_pass(self):
        result=merge(report(('missing','failed')),report(),['missing'],'proof.json')
        self.assertEqual('failed',result['cases'][0]['status']);self.assertFalse(result['checks'][0]['passed'])
    def test_proven_wrong_cohort_is_excluded_instead_of_counted_as_passing(self):
        new=report();new['rejected_reviewed_workflows']=[{'id':'uninstalled','reason':'Publisher is not installed in this exact cohort'}]
        result=merge(report(('uninstalled','failed')),new,['uninstalled'],'proof.json')
        self.assertEqual('invalid-fixture-workflow',result['cases'][0]['status']);self.assertEqual([],result['checks'])
    def test_two_retired_inferred_entries_do_not_count_one_retest_twice(self):
        result=merge(report(('registry-tradingpost-trading_post','failed'),('atlas-trading-post','failed')),
            report(('trading-post','passed')),['registry-tradingpost-trading_post','atlas-trading-post'],'proof.json')
        self.assertEqual(['passed','superseded-duplicate-entry'],[r['status'] for r in result['cases']]);self.assertEqual(1,len(result['checks']))
    def test_new_explicit_child_case_keeps_its_own_measurements(self):
        result=merge(report(('base','passed')),report(('atlas-ae2-priority','passed')),['atlas-ae2-priority'],'proof.json')
        self.assertEqual(['base','atlas-ae2-priority'],[r['id'] for r in result['cases']]);self.assertEqual(2,len(result['checks']))
    def test_unrequested_extra_cases_cannot_inflate_coverage(self):
        result=merge(report(('base','failed')),report(('base','passed'),('extra','passed')),['base'],'proof.json')
        self.assertEqual(['base'],[r['id'] for r in result['cases']]);self.assertEqual(1,len(result['checks']))

if __name__=='__main__':unittest.main()
