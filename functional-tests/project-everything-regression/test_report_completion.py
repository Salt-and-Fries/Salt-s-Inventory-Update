"""A green integration subtotal cannot hide missing or stale Core verification."""
import unittest
from collect_results import complete_matrix,audit_status,qualify_case

class MatrixCompletionTests(unittest.TestCase):
    def ready(self):
        return ({'status':'passed'},[{'core_full_suite':'passed','core_full_suite_matches_current_jar':True,'missing_cohorts':[]}],
                {'passed_cohorts':2,'planned_cohorts':2,'failed_cohorts':0,'failed_cases':0,'failed_checks':0},[])
    def test_all_measured_layers_are_required(self):
        self.assertTrue(complete_matrix(*self.ready()))
        for field,value in [('core_full_suite','failed'),('core_full_suite_matches_current_jar',False),('missing_cohorts',['not-run'])]:
            args=self.ready();args[1][0][field]=value
            self.assertFalse(complete_matrix(*args),field)
    def test_running_or_unexecuted_cohort_cannot_be_complete(self):
        args=self.ready();args[2]['passed_cohorts']=1
        self.assertFalse(complete_matrix(*args))
    def test_failed_assertion_or_reconciliation_cannot_be_hidden(self):
        for field in ['failed_cases','failed_checks','failed_cohorts']:
            args=self.ready();args[2][field]=1;self.assertFalse(complete_matrix(*args),field)
        args=self.ready();args[3].append({'error':'missing proof'});self.assertFalse(complete_matrix(*args))
        args=self.ready();args[0]['status']='needs-attention';self.assertFalse(complete_matrix(*args))
    def test_passing_runtime_matrix_does_not_hide_a_failed_repository_check(self):
        self.assertEqual('incomplete-or-needs-attention',audit_status(True,[{'status':'failed-preexisting'}]))
        self.assertEqual('incomplete-or-needs-attention',audit_status(True,[{'status':'not-run'}]))
        self.assertEqual('incomplete-or-needs-attention',audit_status(False,[{'status':'passed'}]))
        self.assertEqual('complete-measured-matrix',audit_status(True,[{'status':'passed'}]))
    def test_observer_side_effect_invalidates_only_unreproduced_proof(self):
        original={'status':'passed','native_screens':['moze_intel.projecte.gameObjs.gui.CollectorScreen']}
        self.assertEqual('needs-observer-reproduction',qualify_case(original)['status'])
        self.assertEqual('passed',original['status'])
        self.assertEqual('passed',qualify_case({**original,'virtual_policy_query_contract':True})['status'])
        self.assertEqual('passed',qualify_case({'status':'passed','native_screens':[None]})['status'])
        args=self.ready();args[2]['unverified_cases']=1;self.assertFalse(complete_matrix(*args))
    def test_null_menu_ids_do_not_certify_native_screen_reopening(self):
        raw={'status':'passed','coverage':'Reviewed native-only screen preservation','menu_ids':[None]}
        self.assertEqual('needs-observer-reproduction',qualify_case(raw)['status'])
        self.assertEqual('passed',raw['status'])
        proved={**raw,'native_screen_roundtrip':{'screen':'wraith.fwaystones.screen.WaystoneBlockScreen','menu':'waystone'}}
        self.assertEqual('passed',qualify_case(proved)['status'])
    def test_older_shipping_subjects_are_not_current_release_proof(self):
        for status in ['passed','entry-and-policy-only']:
            old={'status':status,'id':'native-menu'}
            self.assertEqual('needs-observer-reproduction',qualify_case(old,False)['status'])
            self.assertEqual(status,old['status'])
            self.assertEqual(status,qualify_case(old,True)['status'])

if __name__=='__main__':unittest.main()
