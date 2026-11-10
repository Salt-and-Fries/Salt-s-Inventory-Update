"""Duplicate runtime declarations may share bytes, never replace a binary."""
import hashlib,importlib.util,tempfile,unittest
from pathlib import Path
from collect_results import publisher_artifact_signature
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('staged_input_contract',ROOT/'tools/prepare-rollout-production-modern.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)

class StagedInputTests(unittest.TestCase):
    def test_identical_api_keeps_both_roles_without_a_second_jar(self):
        with tempfile.TemporaryDirectory() as folder:
            file=Path(folder)/'api.jar';file.write_bytes(b'exact publisher')
            rows=[{'family':'upstream','filename':file.name,'sha512':hashlib.sha512(file.read_bytes()).hexdigest()}]
            self.assertTrue(module.retain_identical_input(rows,'runtime-api',file,file.name))
            self.assertEqual(1,len(rows));self.assertEqual('runtime-api',rows[0]['family'])
            self.assertEqual(['upstream','runtime-api'],rows[0]['also_families'])
    def test_filename_collision_cannot_replace_different_publisher_bytes(self):
        with tempfile.TemporaryDirectory() as folder:
            file=Path(folder)/'api.jar';file.write_bytes(b'different')
            rows=[{'family':'upstream','filename':file.name,'sha512':hashlib.sha512(b'original').hexdigest()}]
            with self.assertRaisesRegex(AssertionError,'Conflicting'):module.retain_identical_input(rows,'runtime-api',file,file.name)
            self.assertNotIn('also_families',rows[0])
    def test_new_filename_is_not_treated_as_a_duplicate(self):
        self.assertFalse(module.retain_identical_input([], 'upstream',Path('not-read.jar'),'not-read.jar'))
    def test_report_reconciliation_retains_roles_and_exact_binary_identity(self):
        before=[{'family':f,'filename':'api.jar','sha512':'same'} for f in ['upstream','runtime-api']]
        after=[{'family':'runtime-api','also_families':['upstream','runtime-api'],'filename':'api.jar','sha512':'same'}]
        self.assertEqual(publisher_artifact_signature(before),publisher_artifact_signature(after))
        self.assertNotEqual(publisher_artifact_signature(before),publisher_artifact_signature([{**after[0],'sha512':'other'}]))

if __name__=='__main__':unittest.main()
