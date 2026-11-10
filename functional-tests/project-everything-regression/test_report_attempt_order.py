"""A later whole run supersedes old partial proof without deleting its history."""
import unittest
from collect_results import select_attempts


class ReportAttemptOrderTests(unittest.TestCase):
    def row(self,**changes):
        return dict({'target':'1.20.1/fabric','fixture':'pinned-cohort',
                     'scope':'complete-discovery-pass','status':'passed'},**changes)

    def test_old_failed_partial_cannot_invalidate_a_new_complete_run(self):
        old=self.row(status='failed')
        partial=self.row(scope='case-reproduction',status='failed',subject='old')
        current=self.row(subject='new')
        latest,_,history,active=select_attempts([old,partial,current])
        self.assertIs(current,latest[('1.20.1/fabric','pinned-cohort')])
        self.assertEqual([partial],history)
        self.assertEqual([],active[('1.20.1/fabric','pinned-cohort')])

    def test_a_later_failed_partial_remains_active(self):
        complete=self.row()
        failed=self.row(scope='case-reproduction',status='failed')
        latest,_,history,active=select_attempts([complete,failed])
        self.assertIs(complete,latest[('1.20.1/fabric','pinned-cohort')])
        self.assertEqual([failed],active[('1.20.1/fabric','pinned-cohort')])
        self.assertEqual([failed],history)

    def test_smoke_cannot_supersede_a_complete_failure_or_active_replay(self):
        complete=self.row(status='failed')
        partial=self.row(scope='case-reproduction')
        smoke=self.row(scope='smoke')
        latest,smokes,_,active=select_attempts([complete,partial,smoke])
        self.assertIs(complete,latest[('1.20.1/fabric','pinned-cohort')])
        self.assertEqual([partial],active[('1.20.1/fabric','pinned-cohort')])
        self.assertEqual([smoke],smokes)

    def test_an_unrelated_whole_run_does_not_erase_a_pending_replay(self):
        partial=self.row(scope='case-reproduction',status='failed')
        other=self.row(target='1.20.1/forge')
        latest,_,_,active=select_attempts([self.row(),partial,other])
        self.assertEqual(2,len(latest))
        self.assertEqual([partial],active[('1.20.1/fabric','pinned-cohort')])

    def test_partial_only_evidence_never_becomes_a_complete_cohort(self):
        partial=self.row(scope='case-reproduction')
        latest,_,history,active=select_attempts([partial])
        self.assertEqual({},latest)
        self.assertEqual([partial],history)
        self.assertEqual([partial],active[('1.20.1/fabric','pinned-cohort')])


if __name__=='__main__':unittest.main()
