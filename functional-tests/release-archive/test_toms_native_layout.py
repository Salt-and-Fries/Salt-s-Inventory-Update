"""Reject changes beyond the approved Tom-only native geometry amendment."""
from pathlib import Path
import copy
import json
import sys
import tempfile
import unittest
from unittest.mock import Mock

REPO = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO / 'tools'))
import focused_release_evidence_toms_layout as qa
from focused_release_evidence import reference
from full_suite_provenance import sources, sha


class TomNativeLayout(unittest.TestCase):
    def fixture(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        root = Path(temp.name).resolve()
        for name in [qa.JAVA, qa.BASE]:
            target = root / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes((REPO / name).read_bytes())
        folder = root / 'build/release-readiness/toms-native-layout-137-01'
        java = folder / 'java/com/salts_inventory_update/functionaltest/InWorldGameplayHarness.java'
        java.parent.mkdir(parents=True)
        java.write_bytes(qa.derive_java((root / qa.JAVA).read_text()).encode())
        init = folder / 'native-layout.init.gradle'
        init.write_bytes(qa.init_text().encode())
        wrapper = folder / 'Run-Toms-NativeLayout.ps1'
        wrapper.write_bytes(qa.wrapper_text((root / qa.BASE).read_text(), root, folder, java.read_bytes(), init.read_bytes()).encode())
        manifest = dict(status='derived-Tom-oracles-prepared-not-run',
            original=reference(root, root / qa.JAVA), original_wrapper=reference(root, root / qa.BASE),
            derived=reference(root, java), init=reference(root, init), wrapper=reference(root, wrapper),
            geometry_substitutions=[dict(before=a, after=b) for a, b in qa.CHANGES],
            original_assertions_and_server_values_preserved=True, all_eight_adjustment_label_regions=True,
            original_pixel_threshold_preserved=True, no_input_retry_or_server_override=True)
        path = root / 'derivation.json'
        path.write_text(json.dumps(manifest))
        case = dict(key='1.20.1:fabric:toms', minecraft='1.20.1', loader='fabric', name='toms',
            script=wrapper.relative_to(root).as_posix(), gradle=False, kinds=['core', 'compatibility'],
            args=['-Minecraft', '1.20.1', '-Loader', 'fabric', '-Scenario', 'toms', '-IntegrationOnly', '-RunId', '{runid}'],
            qa_derivation=dict(base_script=qa.BASE, manifest=reference(root, path)))
        return root, case, manifest, path

    def update(self, root, case, manifest, path):
        path.write_text(json.dumps(manifest))
        case['qa_derivation']['manifest'] = reference(root, path)

    def test_exact_geometry_keeps_original_server_predicate_and_threshold(self):
        root, case, _, _ = self.fixture()
        value = qa.canonical_case(root, case)
        self.assertEqual(value['script'], qa.BASE)
        self.assertEqual(case['script'].split('/')[-1], 'Run-Toms-NativeLayout.ps1')
        original = (root / qa.JAVA).read_text()
        derived = qa.derive_java(original)
        for marker in ['expected.set(((Number)field(state,"count")).intValue()+1)',
                       'actual.join().equals(expected.get())', '"getCount"', 'pixels>8']:
            self.assertGreater(original.count(marker), 0)
            self.assertEqual(original.count(marker), derived.count(marker))

    def test_non_derived_case_uses_original_verifier_unchanged(self):
        case = dict(name='core')
        verify = Mock()
        self.assertTrue(qa.verify_attempt(REPO, {}, case, verify))
        self.assertIs(verify.call_args.args[2], case)

    def test_another_scenario_or_extra_flags_cannot_use_override(self):
        root, case, _, _ = self.fixture()
        for field, value in [('name', 'core'), ('gradle', True), ('kinds', ['core']),
                             ('args', case['args'] + ['-CompleteCore'])]:
            altered = copy.deepcopy(case)
            altered[field] = value
            with self.subTest(field=field), self.assertRaises(AssertionError):
                qa.canonical_case(root, altered)

    def test_unreviewed_java_is_rejected_even_with_updated_hash(self):
        root, case, manifest, path = self.fixture()
        java = root / manifest['derived']['file']
        java.write_bytes(java.read_bytes().replace(b'pixels>8', b'pixels>0'))
        manifest['derived'] = reference(root, java)
        self.update(root, case, manifest, path)
        with self.assertRaisesRegex(AssertionError, 'Unreviewed Java'):
            qa.canonical_case(root, case)

    def test_original_wrapper_flags_cannot_be_removed(self):
        root, case, manifest, path = self.fixture()
        wrapper = root / manifest['wrapper']['file']
        wrapper.write_text(wrapper.read_text().replace('$IntegrationOnly', '$UpstreamOnly'))
        manifest['wrapper'] = reference(root, wrapper)
        self.update(root, case, manifest, path)
        with self.assertRaisesRegex(AssertionError, 'wrapper flags'):
            qa.canonical_case(root, case)

    def test_production_or_other_qa_override_is_rejected(self):
        root, case, manifest, path = self.fixture()
        init = root / manifest['init']['file']
        init.write_text(init.read_text() + "\nroot.tasks.named('compileJava') { enabled=false }\n")
        manifest['init'] = reference(root, init)
        self.update(root, case, manifest, path)
        with self.assertRaisesRegex(AssertionError, 'Gradle override'):
            qa.canonical_case(root, case)

    def passing_entry(self, root, case):
        report = root / 'report.json'
        checks = [dict(name='toms-emitter-adjustment-' + str(i) + '-visible', status='PASS') for i in range(8)]
        checks += [dict(name=n, status='PASS') for n in ['legacy-emitter-increase-click', 'legacy-emitter-native-count-updated']]
        report.write_text(json.dumps(dict(scenario='toms', failures=0, results=checks)))
        canonical = dict(case, script=qa.BASE)
        entry = dict(qa_inputs=sources(root, canonical), runtime_evidence=dict(report=reference(root, report)))
        entry['qa_inputs'][case['script']] = sha(root / case['script'])
        return entry, report, checks

    def test_extra_labels_and_server_predicate_are_required(self):
        root, case, _, _ = self.fixture()
        entry, report, checks = self.passing_entry(root, case)
        verify = Mock()
        self.assertTrue(qa.verify_attempt(root, entry, case, verify))
        self.assertEqual(verify.call_count, 1)
        self.assertEqual(verify.call_args.args[2]['script'], qa.BASE)
        for missing in [checks[7]['name'], 'legacy-emitter-native-count-updated']:
            report.write_text(json.dumps(dict(scenario='toms', failures=0, results=[v for v in checks if v['name'] != missing])))
            entry['runtime_evidence']['report'] = reference(root, report)
            with self.subTest(missing=missing), self.assertRaises(AssertionError):
                qa.verify_attempt(root, entry, case, verify)

    def test_measured_wrapper_cannot_be_missing(self):
        root, case, _, _ = self.fixture()
        entry, _, _ = self.passing_entry(root, case)
        del entry['qa_inputs'][case['script']]
        with self.assertRaisesRegex(AssertionError, 'measured input missing'):
            qa.verify_attempt(root, entry, case, Mock())

    def test_original_runtime_rejection_is_never_overridden(self):
        root, case, _, _ = self.fixture()
        entry, _, _ = self.passing_entry(root, case)
        with self.assertRaisesRegex(AssertionError, 'wrong runtime'):
            qa.verify_attempt(root, entry, case, Mock(side_effect=AssertionError('wrong runtime')))


if __name__ == '__main__':
    unittest.main()
