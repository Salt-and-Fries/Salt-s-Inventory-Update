"""Focused archival reader tests; original gameplay is never re-executed."""
from pathlib import Path
from copy import deepcopy
import importlib.util,json,sys,unittest
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
import focused_release_evidence_full_phase_history as h
spec=importlib.util.spec_from_file_location('full_history_test_plan',ROOT/'tools/run-full-validation.py')
full=importlib.util.module_from_spec(spec);spec.loader.exec_module(full)


class FullPhaseHistoryTest(unittest.TestCase):
    def test_actual_ordinary_and_all_three_derivation_families(self):
        selected=[('core','1.20.1:fabric:complete-core'),('compatibility','1.20.1:fabric:toms'),
                  ('controller','all:controller'),('ae2-atlas','1.20.1:fabric:ae2-atlas'),
                  ('ae2-atlas','1.21.1:neoforge:ae2-atlas')]
        for phase,key in selected:
            with self.subTest(case=key):
                data=json.loads((ROOT/'build/full-validation-20261023/readiness-final-20261106'/phase/'results.json').read_text())
                case=next(v for v in data['cases']if v['key']==key)
                trace=h.verify_phase(ROOT,case,case['attempts'][-1],full)
                self.assertEqual(trace['original_qa_source_archive'],case['attempts'][-1]['qa_source_archive'])

    def test_unknown_added_removed_and_changed_inputs_rejected(self):
        data=h.history(str(ROOT));current=dict(data['base_inputs'],**data['later_standalone_or_reporting_tools'])
        for row in data['reviewed_source_changes']:current[row['file']]=row['current_sha256']
        h.validate_lookup(data,current)
        for mode in ['added','removed','changed']:
            mutated=dict(current)
            if mode=='added':mutated['tools/unreviewed.py']='0'*64
            elif mode=='removed':mutated.pop(next(iter(mutated)))
            else:mutated[next(iter(mutated))]='0'*64
            with self.subTest(mode=mode),self.assertRaises(AssertionError):h.validate_lookup(data,mutated)

    def test_lookup_restores_live_readers_even_on_failure(self):
        before=[m.sources for m in(h.controller,h.legacy,h.modern,h.toms)]
        with self.assertRaises(RuntimeError):
            with h.archived_source_lookup(ROOT,h.history(str(ROOT))['base_inputs']):raise RuntimeError('test')
        self.assertEqual(before,[m.sources for m in(h.controller,h.legacy,h.modern,h.toms)])


if __name__=='__main__':unittest.main(verbosity=2)
