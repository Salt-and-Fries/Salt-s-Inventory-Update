"""Focused report selection checks; no Minecraft or new gameplay assertions."""
from copy import deepcopy
from pathlib import Path
import json
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from accessories_current_pin_evidence import (PAIRS, checked, receipt_for,
                                              select_current_pin, validate_selection)


class AccessoriesPublisherScopeTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.receipt = receipt_for(str(ROOT))
        proof = json.loads(checked(ROOT, cls.receipt['original_feature_proof']).read_text())
        cls.records = {(v['target'], v['case']): v for v in proof['records']}

    def test_actual_three_cases_select_advertised_loaded_pin_and_keep_history(self):
        for row in self.receipt['records']:
            with self.subTest(target=row['target'], case=row['case']):
                reviewed = self.records[row['target'], row['case']]
                selected = select_current_pin(ROOT, reviewed, row['historical_catalogue_candidates'])
                self.assertEqual(selected['pins'], [row['selected_current_pin']])
                self.assertEqual(selected['historical_catalogue_publisher_candidates'], row['historical_catalogue_candidates'])
                self.assertLessEqual({v['sha512'] for v in selected['pins']}, {v['sha512'] for v in reviewed['publishers']})

    def test_changed_record_or_catalogue_is_rejected(self):
        for row in self.receipt['records']:
            reviewed = deepcopy(self.records[row['target'], row['case']])
            reviewed['scope'] += ' unsupported extension'
            with self.assertRaises(AssertionError):
                select_current_pin(ROOT, reviewed, row['historical_catalogue_candidates'])
            with self.assertRaises(AssertionError):
                select_current_pin(ROOT, self.records[row['target'], row['case']], row['historical_catalogue_candidates'][:-1])

    def test_mixed_pin_or_unloaded_version_cannot_be_substituted(self):
        row = self.receipt['records'][0]
        reviewed = self.records[row['target'], row['case']]
        for field, value in [('sha512', '0' * 128), ('filename', 'unloaded.jar'), ('class_locations', [])]:
            mutated = deepcopy(self.receipt)
            mutated['records'][0]['selected_current_pin'][field] = value
            with self.subTest(field=field), self.assertRaises(AssertionError):
                validate_selection(ROOT, reviewed, row['historical_catalogue_candidates'], mutated)

    def test_neoforge_failed_control_cannot_be_reported_as_functional_pass(self):
        row = self.receipt['records'][0]
        mutated = deepcopy(self.receipt)
        mutated['neoforge_beta48']['functional_acceptance'] = True
        with self.assertRaises(AssertionError):
            validate_selection(ROOT, self.records[row['target'], row['case']], row['historical_catalogue_candidates'], mutated)

    def test_unlisted_cases_receive_no_selection(self):
        for pair, reviewed in self.records.items():
            if pair not in PAIRS:
                self.assertIsNone(select_current_pin(ROOT, reviewed, []))


if __name__ == '__main__':
    unittest.main(verbosity=2)
