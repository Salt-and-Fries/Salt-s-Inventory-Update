"""An older clean profile cannot hide the result of its newest replay."""
from pathlib import Path
import copy,importlib.util,sys,unittest

path=Path(__file__).resolve().parents[2]/'tools/build-release-coverage-backlog.py'
sys.path.insert(0,str(path.parent))
spec=importlib.util.spec_from_file_location('release_backlog_latest_oracle',path)
backlog=importlib.util.module_from_spec(spec);spec.loader.exec_module(backlog)

class LatestAttemptTests(unittest.TestCase):
    @staticmethod
    def campaign(last):
        return dict(plan=[dict(id='a'),dict(id='b')],attempts=[
            dict(job='a',run='a1',status='passed'),dict(job='b',run='b1',status='passed'),
            dict(job='a',run='a2',status=last)])

    def test_latest_failure_cannot_inherit_older_pass(self):
        data=self.campaign('failed');before=copy.deepcopy(data)
        self.assertEqual(['b1'],[v['run']for v in backlog.latest_clean_publisher_attempts(data)])
        self.assertEqual(before,data)

    def test_running_replay_has_no_profile_acceptance(self):
        self.assertEqual(['b1'],[v['run']for v in backlog.latest_clean_publisher_attempts(self.campaign('running'))])

    def test_clean_followup_supersedes_history_in_original_plan_order(self):
        data=self.campaign('passed');data['attempts'][0]['status']='failed'
        self.assertEqual(['a2','b1'],[v['run']for v in backlog.latest_clean_publisher_attempts(data)])

    def test_unreviewed_or_duplicate_plan_entries_are_refused(self):
        data=self.campaign('passed');data['attempts'].append(dict(job='unknown',run='x',status='passed'))
        with self.assertRaises(AssertionError):backlog.latest_clean_publisher_attempts(data)
        data=self.campaign('passed');data['plan'].append(dict(id='a'))
        with self.assertRaises(AssertionError):backlog.latest_clean_publisher_attempts(data)

if __name__=='__main__':unittest.main()
