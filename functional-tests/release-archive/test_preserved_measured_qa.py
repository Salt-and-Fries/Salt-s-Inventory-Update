"""Negative controls for the one explicit, archived QA-observer amendment."""
from pathlib import Path
import json
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'tools'))
import focused_release_evidence_preserved_qa as qa
from focused_release_evidence import reference
from full_suite_provenance import digest_inputs


class PreservedMeasuredQA(unittest.TestCase):
    def fixture(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        root = Path(temporary.name)
        start = 'prefix\n    private static void hudServerSelected(String label, int expected) {\n'
        end = '\n    }\n    private static void hudPointerOutside() {}\nsuffix\n'
        old_text = start + 'old observation' + end
        new_text = start + 'amended observation' + end
        current = root / qa.HARNESS
        current.parent.mkdir(parents=True)
        current.write_bytes(new_text.encode())
        inputs = {qa.HARNESS: old_text.encode(), **{'fixture/%d.txt' % i: b'unchanged' for i in range(1143)}}
        new_inputs = dict(inputs, **{qa.HARNESS: new_text.encode()})
        refs = []
        for n, values in [('old.zip', inputs), ('new.zip', new_inputs)]:
            p = root / n
            with zipfile.ZipFile(p, 'w', zipfile.ZIP_DEFLATED) as bundle:
                for name, raw in values.items(): bundle.writestr(name, raw)
            refs.append(dict(**reference(root, p), inputs_sha256=digest_inputs({k: qa.sha(v) for k, v in values.items()})))
        log = root / 'tests.log'
        log.write_text('Ran 8 tests\n\nOK\n')
        proof = dict(status='explicit-HUD-observation-amendment-original-QA-retained',
                     changed_native_source=qa.HARNESS, old_qa=dict(count=1144, archive=refs[0]),
                     new_qa=dict(count=1144, archive=refs[1]),
                     tests=dict(passed=8, exit_code=0, log=reference(root, log)), public_artifacts_unchanged=True)
        proof_path = root / 'proof.json'
        proof_path.write_text(json.dumps(proof))
        pointer = root / qa.POINTER
        pointer.parent.mkdir(parents=True)
        pointer.write_text(json.dumps(dict(status='explicit-recorded-QA-source-lookup-active', amendment=reference(root, proof_path))))
        return root, inputs, new_inputs, proof_path, pointer, proof

    def update(self, root, path, pointer, proof):
        path.write_text(json.dumps(proof))
        p = json.loads(pointer.read_text())
        p['amendment'] = reference(root, path)
        pointer.write_text(json.dumps(p))

    def test_old_recorded_bytes_are_returned_for_both_path_separators(self):
        root, old, new, *_ = self.fixture()
        for name in [qa.HARNESS, qa.HARNESS.replace('/', '\\')]:
            reader = qa.MeasuredSources(root)
            self.assertEqual(reader.read(name, qa.sha(old[qa.HARNESS])), old[qa.HARNESS])
            self.assertEqual(len(reader.archived_reads), 1)
            self.assertEqual((root / qa.HARNESS).read_bytes(), new[qa.HARNESS])

    def test_matching_current_source_needs_no_policy(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root / 'qa.java').write_bytes(b'current')
            self.assertEqual(qa.verify_inputs(root, {'qa.java': qa.sha(b'current')}), [])

    def test_missing_explicit_policy_does_not_accept_changed_source(self):
        root, old, _, _, pointer, _ = self.fixture()
        pointer.unlink()
        with self.assertRaises(FileNotFoundError): qa.MeasuredSources(root).read(qa.HARNESS, qa.sha(old[qa.HARNESS]))

    def test_another_changed_file_is_rejected(self):
        root, *_ = self.fixture()
        (root / 'other.java').write_bytes(b'new')
        with self.assertRaisesRegex(AssertionError, 'without scoped archive policy'):
            qa.MeasuredSources(root).read('other.java', qa.sha(b'old'))

    def test_unknown_original_helper_revision_is_rejected(self):
        root, *_ = self.fixture()
        with self.assertRaisesRegex(AssertionError, 'Unknown original'):
            qa.MeasuredSources(root).read(qa.HARNESS, '0' * 64)

    def test_unreviewed_current_revision_is_rejected(self):
        root, old, *_ = self.fixture()
        (root / qa.HARNESS).write_text('unreviewed')
        with self.assertRaisesRegex(AssertionError, 'Unreviewed current'):
            qa.MeasuredSources(root).read(qa.HARNESS, qa.sha(old[qa.HARNESS]))

    def test_changed_original_archive_is_rejected(self):
        root, old, *_ = self.fixture()
        (root / 'old.zip').write_bytes(b'corrupt')
        with self.assertRaises(AssertionError): qa.MeasuredSources(root).read(qa.HARNESS, qa.sha(old[qa.HARNESS]))

    def test_failed_tests_cannot_enable_archive_lookup(self):
        root, old, new, path, pointer, proof = self.fixture()
        proof['tests']['exit_code'] = 1
        self.update(root, path, pointer, proof)
        with self.assertRaises(AssertionError): qa.MeasuredSources(root).read(qa.HARNESS, qa.sha(old[qa.HARNESS]))

    def test_duplicate_windows_posix_spellings_are_rejected(self):
        root, old, *_ = self.fixture()
        with self.assertRaisesRegex(AssertionError, 'Duplicate normalized'):
            qa.verify_inputs(root, {qa.HARNESS: qa.sha(old[qa.HARNESS]), qa.HARNESS.replace('/', '\\'): qa.sha(old[qa.HARNESS])})

    def test_workspace_escape_is_rejected(self):
        root, *_ = self.fixture()
        with self.assertRaisesRegex(AssertionError, 'Invalid measured'):
            qa.MeasuredSources(root).read('../outside.java', '0' * 64)

    def rewrite_new_archive(self, root, values, proof):
        p = root / 'new.zip'
        with zipfile.ZipFile(p, 'w', zipfile.ZIP_DEFLATED) as bundle:
            for name, raw in values.items(): bundle.writestr(name, raw)
        proof['new_qa']['archive'] = dict(**reference(root, p), inputs_sha256=digest_inputs({k: qa.sha(v) for k, v in values.items()}))

    def test_change_outside_the_observer_is_rejected_even_with_new_archive(self):
        root, old, new, path, pointer, proof = self.fixture()
        new[qa.HARNESS] = new[qa.HARNESS].replace(b'prefix', b'changed prefix')
        (root / qa.HARNESS).write_bytes(new[qa.HARNESS])
        self.rewrite_new_archive(root, new, proof)
        self.update(root, path, pointer, proof)
        with self.assertRaisesRegex(AssertionError, 'outside the one observer'):
            qa.MeasuredSources(root).read(qa.HARNESS, qa.sha(old[qa.HARNESS]))

    def test_second_changed_native_input_is_rejected(self):
        root, old, new, path, pointer, proof = self.fixture()
        new['fixture/0.txt'] = b'another changed input'
        self.rewrite_new_archive(root, new, proof)
        self.update(root, path, pointer, proof)
        with self.assertRaises(AssertionError): qa.MeasuredSources(root).read(qa.HARNESS, qa.sha(old[qa.HARNESS]))

    def test_changed_artifacts_cannot_enable_the_QA_only_policy(self):
        root, old, new, path, pointer, proof = self.fixture()
        proof['public_artifacts_unchanged'] = False
        self.update(root, path, pointer, proof)
        with self.assertRaises(AssertionError): qa.MeasuredSources(root).read(qa.HARNESS, qa.sha(old[qa.HARNESS]))


if __name__ == '__main__':
    unittest.main(verbosity=2)
