"""Offline rejection controls for the narrow unchanged-functionality policy."""
from pathlib import Path
import copy
import json
import sys
import tempfile
import unittest
import warnings
import zipfile

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import focused_release_evidence_retention_policy as policy


class PublisherRetentionPolicy(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)

    def pair(self, change=None):
        old = {name: b'old-callback' for name in policy.PACKET_CLASSES}
        old.update({'unchanged.class': b'same', 'native.png': b'art',
                    'salts_inventory_compat.reviewed.mixins.json': json.dumps({'required': True, 'mixins': ['X']}).encode()})
        new = dict(old)
        for name in policy.PACKET_CLASSES:
            new[name] = b'new-callback'
        new['salts_inventory_compat.reviewed.mixins.json'] = json.dumps({'required': True, 'mixins': ['X'], 'minVersion': '0.8'}).encode()
        if change:
            change(old, new)
        for name, contents in [('old.jar', old), ('new.jar', new)]:
            with zipfile.ZipFile(self.root / name, 'w') as archive:
                for entry, raw in contents.items():
                    archive.writestr(entry, raw)
        return self.root / 'old.jar', self.root / 'new.jar'

    def reject_pair(self, change):
        old, new = self.pair(change)
        with self.assertRaises((AssertionError, KeyError, ValueError)):
            policy.archive_pair(old, new, 'compatibility')

    def test_exact_reviewed_callback_and_minimum_transition(self):
        old, new = self.pair()
        result = policy.archive_pair(old, new, 'compatibility')
        self.assertEqual(set(result['changed']), policy.PACKET_CLASSES | {'salts_inventory_compat.reviewed.mixins.json'})

    def test_core_and_controlify_require_whole_archive_identity(self):
        old, new = self.pair()
        for family in ['core', 'controlify']:
            self.assertEqual(policy.archive_pair(old, old, family), {'changed': [], 'minimum_only': []})
            with self.assertRaises(AssertionError):
                policy.archive_pair(old, new, family)

    def test_added_removed_and_unrelated_class_payload_rejected(self):
        self.reject_pair(lambda a, b: b.update(extra=b'new'))
        self.reject_pair(lambda a, b: b.pop('native.png'))
        self.reject_pair(lambda a, b: b.update({'unchanged.class': b'new-business-code'}))

    def test_other_asset_and_descriptor_changes_rejected(self):
        self.reject_pair(lambda a, b: b.update({'native.png': b'cut-sprite'}))
        self.reject_pair(lambda a, b: b.update({'fabric.mod.json': b'{}'}))

    def test_mixin_behavior_change_beyond_minimum_rejected(self):
        name = 'salts_inventory_compat.reviewed.mixins.json'
        self.reject_pair(lambda a, b: b.update({name: json.dumps({'required': False, 'mixins': ['X'], 'minVersion': '0.8'}).encode()}))
        self.reject_pair(lambda a, b: b.update({name: json.dumps({'required': True, 'mixins': ['Y'], 'minVersion': '0.8'}).encode()}))

    def test_other_floor_or_preexisting_minimum_rejected(self):
        name = 'salts_inventory_compat.reviewed.mixins.json'
        self.reject_pair(lambda a, b: b.update({name: json.dumps({'required': True, 'mixins': ['X'], 'minVersion': '0.9'}).encode()}))
        self.reject_pair(lambda a, b: a.update({name: json.dumps({'required': True, 'mixins': ['X'], 'minVersion': '0.7'}).encode()}))

    def test_unreviewed_parent_inheritance_rejected(self):
        name = 'salts_inventory_compat.reviewed.mixins.json'
        def change(a, b):
            for value in [a, b]:
                data = json.loads(value[name]); data['parent'] = 'other.json'; value[name] = json.dumps(data).encode()
        self.reject_pair(change)

    def test_duplicate_zip_members_rejected(self):
        old, new = self.pair()
        with warnings.catch_warnings():
            warnings.simplefilter('ignore', UserWarning)
            with zipfile.ZipFile(new, 'a') as archive:
                archive.writestr('unchanged.class', b'same')
        with self.assertRaises(AssertionError):
            policy.archive_pair(old, new, 'compatibility')

    def indexed(self):
        return {'loader': 'fabric', 'sha512': 'a' * 128, 'nodes': [{'classes': {'other/Native.class'}, 'mods': [{'id': 'other'}]}]}

    def test_absence_requires_observed_active_classpath(self):
        self.assertTrue(policy.native_traveler_absent(self.indexed()))
        data = self.indexed(); data['nodes'] = []
        with self.assertRaises(AssertionError):
            policy.native_traveler_absent(data)

    def test_declared_or_embedded_native_traveler_cannot_be_reused(self):
        data = self.indexed(); data['nodes'][0]['mods'].append({'id': 'travelersbackpack'})
        self.assertFalse(policy.native_traveler_absent(data))
        data = self.indexed(); data['nodes'].append({'classes': {policy.NATIVE_PREFIX + 'network/Packet.class'}, 'mods': []})
        self.assertFalse(policy.native_traveler_absent(data))

    def test_actual_native_comparison_and_boundary_controls_required(self):
        value = dict(oldComparison=-1, newComparison=-1, oldAccepted=True, newAccepted=True,
                     loadedClassSha256='b' * 64, classSourceMatches=True)
        boundary = dict(value, newComparison=0)
        too_old = dict(value, newComparison=1, newAccepted=False)
        self.assertTrue(policy.minimum_comparisons(value, boundary, too_old, 'b' * 64))
        for key, bad in [('classSourceMatches', False), ('loadedClassSha256', 'c' * 64),
                         ('newComparison', True), ('newAccepted', 1)]:
            changed = dict(value); changed[key] = bad
            with self.assertRaises(AssertionError):
                policy.minimum_comparisons(changed, boundary, too_old, 'b' * 64)
        with self.assertRaises(AssertionError):
            policy.minimum_comparisons(value, value, too_old, 'b' * 64)

    def history(self):
        subjects = {'core': ('core.jar', 'a' * 64), 'compatibility': ('compat.jar', 'b' * 64)}
        job = dict(id='sample', target='1.20.1/fabric')
        attempt = dict(job='sample', run='old-pass', status='passed', exit_code=0,
                       qa_inputs_unchanged=True, subjects_unchanged=True, qa_inputs={'original.py': 'c' * 64},
                       subjects={k: list(v) for k, v in subjects.items()},
                       results=[dict(target=job['target'], status='passed', exit_code=0,
                                     results=dict(cases=3, assertions=30, failed=0))])
        return attempt, job, subjects

    def test_original_clean_completion_and_strict_count_types_required(self):
        attempt, job, subjects = self.history()
        self.assertEqual(policy.historical_completion(attempt, job, subjects)['target'], job['target'])
        for key, value in [('status', 'failed'), ('exit_code', False), ('qa_inputs_unchanged', 1), ('subjects_unchanged', False)]:
            changed = copy.deepcopy(attempt); changed[key] = value
            with self.assertRaises(AssertionError):
                policy.historical_completion(changed, job, subjects)
        changed = copy.deepcopy(attempt); changed['results'][0]['results']['cases'] = True
        with self.assertRaises(AssertionError):
            policy.historical_completion(changed, job, subjects)

    def test_original_subject_and_target_cannot_be_substituted(self):
        attempt, job, subjects = self.history()
        changed = copy.deepcopy(attempt); changed['subjects']['compatibility'][1] = 'd' * 64
        with self.assertRaises(AssertionError):
            policy.historical_completion(changed, job, subjects)
        changed = copy.deepcopy(attempt); changed['results'][0]['target'] = '1.20.1/forge'
        with self.assertRaises(AssertionError):
            policy.historical_completion(changed, job, subjects)

    def test_newer_failure_or_live_attempt_never_hidden_by_old_pass(self):
        old, _, subjects = self.history()
        self.assertTrue(policy.latest_state_allows_retention(old, old, subjects))
        newer = dict(old, run='new-pass')
        self.assertFalse(policy.latest_state_allows_retention(newer, old, subjects))
        self.assertFalse(policy.latest_state_allows_retention(dict(newer, status='running'), old, subjects))
        with self.assertRaises(AssertionError):
            policy.latest_state_allows_retention(dict(newer, status='failed'), old, subjects)
        with self.assertRaises(AssertionError):
            policy.latest_state_allows_retention(dict(newer, status='interrupted'), old, subjects)


if __name__ == '__main__':
    unittest.main(verbosity=2)
