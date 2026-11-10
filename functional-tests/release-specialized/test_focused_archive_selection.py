"""Focused seal-versus-context selection checks; no gameplay replay."""
from pathlib import Path
from copy import deepcopy
import importlib.util,json,sys,unittest
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
spec=importlib.util.spec_from_file_location('final_collector_archive_test',ROOT/'tools/collect-release-qualification.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)

class ArchiveSelectionTest(unittest.TestCase):
    def test_actual_sidecars_and_seals_remain_separate(self):
        for stem in ['1211-neoforge-06','2612-neoforge-01']:
            p=ROOT/'output/release-readiness'/('focused-evidence-readiness-powah-modern-'+stem+'-retained-sealing-context.json')
            data=json.loads(p.read_text());context=module.archival_context(ROOT,p,data)
            self.assertIs(context['counts_as_native_execution'],False)
            seal=ROOT/data['seal']['file'];self.assertIsNone(module.archival_context(ROOT,seal,json.loads(seal.read_text())))

    def test_unknown_sidecar_or_mixed_subjects_rejected(self):
        p=ROOT/'output/release-readiness/focused-evidence-readiness-powah-modern-1211-neoforge-06-retained-sealing-context.json';data=json.loads(p.read_text())
        with self.assertRaises(AssertionError):module.archival_context(ROOT,p.with_name('unknown-retained-sealing-context.json'),data)
        mutated=deepcopy(data);mutated['artifact_binding']['original_subjects']['core'][1]='0'*64
        with self.assertRaises(AssertionError):module.archival_context(ROOT,p,mutated)
        with self.assertRaises(AssertionError):module.archival_context(ROOT,p.with_name('focused-evidence-not-a-seal.json'),data)

if __name__=='__main__':unittest.main(verbosity=2)
