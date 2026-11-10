"""A finished Core suite must not hide a pending current-jar reproduction."""
import unittest
from finalize_audit import replay_finished


class FinalReplayBarrierTests(unittest.TestCase):
    def test_a_partial_or_missing_replay_cannot_finalize_the_audit(self):
        plan=[{'target':'1.20.1/fabric'},{'target':'1.21.1/neoforge'}]
        self.assertFalse(replay_finished(None,plan))
        self.assertFalse(replay_finished([{'status':'passed'}],plan))
        self.assertFalse(replay_finished([{'status':'passed'},{'status':'running'}],plan))
    def test_a_finished_failure_is_terminal_but_is_not_converted_into_a_pass(self):
        rows=[{'status':'passed'},{'status':'failed'}]
        self.assertTrue(replay_finished(rows,[{},{}]))
        self.assertEqual('failed',rows[1]['status'])


if __name__=='__main__':unittest.main()
