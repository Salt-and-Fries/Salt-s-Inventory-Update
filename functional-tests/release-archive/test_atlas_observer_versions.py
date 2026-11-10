"""Only the two preserved metadata observers may retain old input hashes."""
from pathlib import Path
import copy,sys,unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'tools'))
from focused_release_evidence_ae2_drive_gutter import observer_versions,PRIOR,OBSERVER,DRIVER


class AtlasObserverVersions(unittest.TestCase):
    def setUp(self):
        self.root=Path(__file__).resolve().parents[2]
        from full_suite_provenance import sha
        self.expected={OBSERVER:sha(self.root/OBSERVER),DRIVER:sha(self.root/DRIVER),'functional-tests/frozen.java':'a'*64}
        self.recorded=dict(self.expected)
        for n,r in PRIOR.items():self.recorded[n]=r['sha256']
    def test_named_metadata_only_correction(self):
        self.assertEqual(observer_versions(self.root,dict(self.expected),self.recorded),self.recorded)
    def test_current_inputs_need_no_retention(self):
        self.assertEqual(observer_versions(self.root,dict(self.expected),self.expected),self.expected)
    def test_no_relabel_of_native_source_change(self):
        self.recorded['functional-tests/frozen.java']='b'*64
        with self.assertRaises(AssertionError):observer_versions(self.root,dict(self.expected),self.recorded)
    def test_unknown_observer_version(self):
        self.recorded[OBSERVER]='b'*64
        with self.assertRaises(AssertionError):observer_versions(self.root,dict(self.expected),self.recorded)
    def test_unknown_driver_version(self):
        self.recorded[DRIVER]='b'*64
        with self.assertRaises(AssertionError):observer_versions(self.root,dict(self.expected),self.recorded)
    def test_missing_input(self):
        self.recorded.pop('functional-tests/frozen.java')
        with self.assertRaises(AssertionError):observer_versions(self.root,dict(self.expected),self.recorded)
    def test_extra_input(self):
        self.recorded['extra']='a'*64
        with self.assertRaises(AssertionError):observer_versions(self.root,dict(self.expected),self.recorded)


if __name__=='__main__':unittest.main()
