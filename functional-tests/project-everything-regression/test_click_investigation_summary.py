import unittest
from summarize_click_investigation import measure


class ClickInvestigationSummaryTests(unittest.TestCase):
    def trial(self, status='passed', passed=True):
        return {'cases': [{'id': 'native-cell', 'status': status}],
                'checks': [{'case': 'native-cell', 'check': 'exact transfer', 'passed': passed}]}

    def test_later_pass_does_not_erase_an_earlier_dropped_click(self):
        result = measure([self.trial('failed', False), self.trial()])
        self.assertEqual('failure-reproduced', result['outcome'])
        self.assertEqual(1, result['case_statuses']['failed'])
        self.assertEqual(1, len(result['failed_assertions']))
        self.assertFalse(result['resolved'])

    def test_passing_repetitions_do_not_claim_a_causal_fix(self):
        result = measure([self.trial(), self.trial()])
        self.assertEqual('not-reproduced', result['outcome'])
        self.assertFalse(result['resolved'])

    def test_opening_only_cases_retain_their_actual_coverage(self):
        result = measure([self.trial('entry-and-policy-only')])
        self.assertNotIn('passed', result['case_statuses'])
        self.assertEqual(1, result['case_statuses']['entry-and-policy-only'])


if __name__ == '__main__':
    unittest.main()
