"""Keep source-runtime initialization separate from packaged-native evidence."""
from pathlib import Path
import copy
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'tools'))
from focused_release_evidence_runtime_scope import PROJECT_STARTUP, phase_binding


class RuntimeScopeTests(unittest.TestCase):
    def setUp(self):
        self.case = dict(script=PROJECT_STARTUP, key='all:startup', name='startup',
            minecraft=None, loader=None, kinds=['core'], args=['-Runtime'])
        self.entry = dict(exit_code=0, runtime_evidence=dict(
            scope='QA-source archive and outer process log; inner scope remains in the named runner evidence'),
            qa_source_archive=dict(file='original-source.zip', sha256='abc'), log='original.log')
        self.identity = dict(execution_kind='fresh-current-artifact',
            original_artifacts={'core.jar': 'abc'}, final_artifacts={'core.jar': 'abc'}, artifact_equivalence=None)

    def test_project_runtime_is_explicit(self):
        value = phase_binding(self.case, self.entry, self.identity)
        self.assertEqual(value['execution_kind'], 'project-runtime-initialization')
        self.assertEqual(value['distribution_identity_kind'], 'fresh-current-artifact')
        self.assertEqual(value['original_artifacts'], self.identity['original_artifacts'])
        self.assertEqual(value['qa_source_archive'], self.entry['qa_source_archive'])
        self.assertEqual(self.identity['execution_kind'], 'fresh-current-artifact')

    def test_retained_identity_is_not_invented_fresh_execution(self):
        self.identity['execution_kind'] = 'retained-final-Core-render-and-REI-policy-equivalence'
        value = phase_binding(self.case, self.entry, self.identity)
        self.assertEqual(value['execution_kind'], 'project-runtime-initialization')
        self.assertEqual(value['distribution_identity_kind'], self.identity['execution_kind'])

    def test_other_original_profiles_unchanged(self):
        self.case['script'] = 'functional-tests/scripts/Run-ReleaseGameplayQa.ps1'
        self.assertEqual(phase_binding(self.case, self.entry, self.identity), self.identity)

    def reject(self):
        with self.assertRaises(AssertionError): phase_binding(self.case, self.entry, self.identity)

    def test_wrong_macro_key(self):
        self.case['key'] = 'other'; self.reject()

    def test_wrong_target(self):
        self.case['minecraft'] = '26.3'; self.reject()

    def test_nonruntime_compile_only(self):
        self.case['args'] = []; self.reject()

    def test_failed_process(self):
        self.entry['exit_code'] = 1; self.reject()

    def test_boolean_exit_code(self):
        self.entry['exit_code'] = False; self.reject()

    def test_changed_runtime_evidence_contract(self):
        self.entry['runtime_evidence']['report'] = 'unverified-report'; self.reject()

    def test_source_receipt_missing(self):
        self.entry.pop('qa_source_archive'); self.reject()


if __name__ == '__main__': unittest.main()
