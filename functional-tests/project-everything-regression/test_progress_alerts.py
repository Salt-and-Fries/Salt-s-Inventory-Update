"""Completed/crashed clients must not evade the read-only failure watcher."""
import unittest
from watch_progress import ROOT,failure_alerts

class ProgressAlertsTests(unittest.TestCase):
    def setUp(self):
        self.profile=ROOT/'build/test-profile';self.seen=set()
        self.row={'target':'1.21.1/fabric','status':'running','log':'build/test.log'}
    def alerts(self,checks):
        return failure_alerts(self.profile,self.row,checks,self.seen)
    def test_running_case_failure(self):
        self.assertEqual('late-slot',self.alerts({'cases':[{'id':'late-slot','status':'failed','error':'missing item'}]})[0]['case'])
    def test_last_case_failure_after_coordinator_finishes(self):
        self.row['status']='failed'
        self.assertEqual('last',self.alerts({'cases':[{'id':'last','status':'failed'}]})[0]['case'])
    def test_startup_failure_without_report(self):
        self.row['status']='failed'
        self.assertEqual('cohort-runtime',self.alerts({})[0]['case'])
    def test_crash_after_passing_assertions_still_alerts(self):
        self.row['status']='failed'
        self.assertEqual('cohort-runtime',self.alerts({'cases':[{'id':'done','status':'passed'}]})[0]['case'])
    def test_previously_delivered_failure_does_not_loop(self):
        report={'cases':[{'id':'same','status':'failed'}]}
        self.assertEqual(1,len(self.alerts(report)));self.assertEqual([],self.alerts(report))
    def test_running_startup_is_not_a_failure(self):
        self.assertEqual([],self.alerts({}))

if __name__=='__main__':unittest.main()
