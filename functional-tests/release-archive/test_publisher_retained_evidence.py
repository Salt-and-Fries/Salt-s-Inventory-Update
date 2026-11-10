"""Historical evidence must keep its actual subjects, runtime and assertions."""
from pathlib import Path
import copy
import hashlib
import json
import sys
import tempfile
import unittest
import warnings
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'tools'))
import focused_release_evidence_publisher_retention as retained
from focused_release_evidence import reference


class HistoricalPublisherArchive(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.profile = self.root / 'old-profile'
        self.profile.mkdir()
        self.runtime = self.root / 'actual-runtime'
        (self.runtime / 'mods').mkdir(parents=True)
        self.expected = {}
        staged = []
        for family, filename in [('core', 'core.jar'), ('compatibility', 'compat.jar'), ('qa', 'observer.jar')]:
            path = self.runtime / 'mods' / filename
            with zipfile.ZipFile(path, 'w') as bundle:
                bundle.writestr(family + '.class', (family + '-original-bytes').encode())
            digest = retained.sha(path)
            # Original staging source is deliberately gone. The old runtime
            # copy, not a possibly reused build-output path, is authoritative.
            staged.append(dict(family=family, filename=filename, file='gone-source/' + filename, sha256=digest))
            if family != 'qa':
                self.expected[family] = [filename, digest]
        self.write('old-profile/staged-artifacts.json', dict(artifacts=staged))
        self.write('old-profile/launch.json', dict(cwd=str(self.runtime), log='old-profile/launch.log'))
        (self.profile / 'launch.log').write_text('original native runtime log')
        self.write('old-profile/observations.json', dict(observed='real old state'))
        self.write('old-profile/mod-interaction-checks.json', dict(cases=[dict(id='inventory', status='passed')],
            checks=[dict(case='inventory', check='native insertion', passed=True)]))
        (self.root / 'original-generator.py').write_text('original generation input')
        (self.root / 'measured-observer.java').write_text('original generated observer')
        self.job = dict(id='test-job', target='1.20.1/fabric')
        self.attempt = dict(job='test-job', run='old-clean-run', status='passed', exit_code=0,
            qa_inputs_unchanged=True, subjects_unchanged=True, subjects=self.expected,
            qa_inputs={'original-generator.py': retained.sha(self.root / 'original-generator.py')},
            results=[dict(target='1.20.1/fabric', status='passed', exit_code=0, profile='old-profile',
                generated_inputs={'measured-observer.java': retained.sha(self.root / 'measured-observer.java')},
                results=dict(cases=1, assertions=1, failed=0))])

    def write(self, name, value):
        (self.root / name).write_text(json.dumps(value))

    def sealed(self):
        archive = self.root / 'historical-inputs.zip'
        history, inputs = retained.history(self.root, self.attempt, self.job, self.expected, archive=archive)
        return dict(history=history, archive=reference(self.root, archive),
            archived_inputs={k: hashlib.sha256(v).hexdigest() for k, v in inputs.items()})

    def test_original_subjects_and_actual_executed_observer_are_preserved(self):
        record = self.sealed()
        self.assertEqual(record['history']['original_subjects'], self.expected)
        self.assertEqual(record['history']['original_run'], 'old-clean-run')
        self.assertEqual(len(record['history']['executed_observers']), 1)
        retained.verify_historical_archive(self.root, record)
        with zipfile.ZipFile(self.root / record['archive']['file']) as bundle:
            self.assertEqual(bundle.read('executed-runtime-observers/observer.jar'),
                (self.runtime / 'mods/observer.jar').read_bytes())

    def test_changed_measured_generator_cannot_be_replaced_retrospectively(self):
        (self.root / 'original-generator.py').write_text('different generator')
        with self.assertRaises(AssertionError):
            self.sealed()

    def test_changed_measured_generated_source_rejected(self):
        (self.root / 'measured-observer.java').write_text('different observer source')
        with self.assertRaises(AssertionError):
            self.sealed()

    def test_changed_actual_runtime_binary_rejected(self):
        (self.runtime / 'mods/observer.jar').write_bytes(b'different executed bytes')
        with self.assertRaises(AssertionError):
            self.sealed()

    def test_assertion_failure_and_count_substitution_rejected(self):
        for value in [dict(cases=[dict(id='inventory', status='passed')], checks=[dict(passed=False)]),
            dict(cases=[dict(id='inventory', status='passed')], checks=[dict(passed=True), dict(passed=True)])]:
            self.write('old-profile/mod-interaction-checks.json', value)
            with self.assertRaises(AssertionError):
                retained.history(self.root, self.attempt, self.job, self.expected)

    def test_unmeasured_generated_source_is_not_claimed_measured(self):
        self.attempt['results'][0].pop('generated_inputs')
        record = self.sealed()
        self.assertEqual(record['history']['measured_generated_inputs'], [])
        self.assertNotIn('measured-observer.java', record['archived_inputs'])
        retained.verify_historical_archive(self.root, record)

    def test_archive_extra_missing_duplicate_and_payload_changes_rejected(self):
        record = self.sealed()
        original = self.root / record['archive']['file']
        with zipfile.ZipFile(original) as bundle:
            members = {name: bundle.read(name) for name in bundle.namelist()}
        for mode in ['extra', 'missing', 'duplicate', 'payload']:
            path = self.root / (mode + '.zip')
            changed = dict(members)
            first = next(iter(changed))
            if mode == 'extra':
                changed['unaccounted.java'] = b'extra'
            elif mode == 'missing':
                changed.pop(first)
            elif mode == 'payload':
                changed[first] = b'changed'
            with warnings.catch_warnings():
                warnings.simplefilter('ignore', UserWarning)
                with zipfile.ZipFile(path, 'w') as bundle:
                    for name, value in changed.items():
                        bundle.writestr(name, value)
                    if mode == 'duplicate':
                        bundle.writestr(first, changed[first])
            mutated = copy.deepcopy(record)
            mutated['archive'] = reference(self.root, path)
            with self.assertRaises(AssertionError, msg=mode):
                retained.verify_historical_archive(self.root, mutated)

    def retention_index(self, *, duplicates=False):
        plan = [dict(id='job-' + str(i), target='1.20.1/fabric') for i in range(65)]
        record = dict(history=dict(job='job-0', original_run='old-run', target='1.20.1/fabric'))
        self.write('retained-record.json', record)
        ref = reference(self.root, self.root / 'retained-record.json')
        self.write('index.json', dict(status='scoped-historical-equivalences-verified', plan=plan,
            records=[ref, ref] if duplicates else [ref]))
        self.write('pointer.json', dict(status='explicit-scoped-retention-active',
            index=reference(self.root, self.root / 'index.json')))
        return dict(plan=plan, attempts=[])

    def test_newer_failed_running_interrupted_and_fresh_runs_have_no_old_acceptance(self):
        campaign = self.retention_index()
        for status in ['failed', 'running', 'interrupted', 'passed']:
            campaign['attempts'] = [dict(job='job-0', run='new-run', status=status)]
            self.assertEqual(retained.retained_for_campaign(self.root, campaign,
                lambda target: self.expected, pointer=self.root / 'pointer.json'), [])

    def test_changed_plan_cannot_drop_original_jobs(self):
        campaign = self.retention_index()
        campaign['plan'].pop()
        with self.assertRaises(AssertionError):
            retained.retained_for_campaign(self.root, campaign, lambda target: self.expected,
                pointer=self.root / 'pointer.json')

    def test_mutated_index_not_accepted(self):
        campaign = self.retention_index()
        (self.root / 'index.json').write_text('{}')
        with self.assertRaises(AssertionError):
            retained.retained_for_campaign(self.root, campaign, lambda target: self.expected,
                pointer=self.root / 'pointer.json')

    def test_duplicate_record_rejected_even_when_a_newer_failure_exists(self):
        campaign = self.retention_index(duplicates=True)
        campaign['attempts'] = [dict(job='job-0', run='new-run', status='failed')]
        with self.assertRaises(AssertionError):
            retained.retained_for_campaign(self.root, campaign, lambda target: self.expected,
                pointer=self.root / 'pointer.json')


if __name__ == '__main__':
    unittest.main(verbosity=2)
