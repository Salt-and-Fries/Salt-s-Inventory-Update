"""Corrupted current-candidate pointers must not certify an inconsistent ledger."""
from pathlib import Path
import hashlib,json,sys,tempfile,unittest,zipfile

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
from release_candidate_identity import sha,validate


class CandidateIdentity(unittest.TestCase):
    def fixture(self,root):
        artifacts=[]
        for index in range(36):
            path=root/('candidate-'+str(index)+'.jar');path.write_bytes(('example archive '+str(index)).encode())
            artifacts.append(dict(file=path.name,sha256=sha(path)))
        integrity=root/'integrity.json';integrity.write_text(json.dumps(dict(status='passed',artifacts=artifacts)))
        raw=b'Reviewed example source\n';files={'README.md':hashlib.sha256(raw).hexdigest()}
        tree=hashlib.sha256(json.dumps(files,sort_keys=True,separators=(',',':')).encode()).hexdigest()
        archive=root/'source.zip'
        with zipfile.ZipFile(archive,'w')as bundle:
            bundle.writestr('README.md',raw)
            bundle.writestr('CANDIDATE-SOURCE-IDENTITY.json',json.dumps(dict(source_tree_sha256=tree,source_files=files)))
        source=root/'source.json';value=dict(status='candidate-recorded-qualification-pending',source_tree_sha256=tree,
            source_files=files,review_source_archive=dict(file=archive.name,sha256=sha(archive)),
            artifacts=artifacts,integrity_sha256=sha(integrity))
        source.write_text(json.dumps(value))
        return dict(status='recorded-qualification-pending',source_tree_sha256=tree,
            source_snapshot=dict(file=source.name,sha256=sha(source)),artifact_integrity=dict(file=integrity.name,sha256=sha(integrity)))

    def test_exact_source_and_thirty_six_artifacts_are_accepted(self):
        with tempfile.TemporaryDirectory()as folder:
            root=Path(folder);pointer=self.fixture(root)
            self.assertEqual(pointer['source_tree_sha256'],validate(root,pointer,verify_archive=True)['source_tree_sha256'])

    def test_changed_production_bytes_are_refused(self):
        with tempfile.TemporaryDirectory()as folder:
            root=Path(folder);pointer=self.fixture(root);(root/'candidate-0.jar').write_bytes(b'changed candidate')
            with self.assertRaises(AssertionError):validate(root,pointer)

    def test_changed_source_snapshot_is_refused(self):
        with tempfile.TemporaryDirectory()as folder:
            root=Path(folder);pointer=self.fixture(root);(root/'source.json').write_text('{}')
            with self.assertRaises(AssertionError):validate(root,pointer)

    def test_false_source_tree_digest_is_refused_even_with_matching_reference_hash(self):
        with tempfile.TemporaryDirectory()as folder:
            root=Path(folder);pointer=self.fixture(root);path=root/'source.json';data=json.loads(path.read_text())
            data['source_files']['README.md']='0'*64;path.write_text(json.dumps(data));pointer['source_snapshot']['sha256']=sha(path)
            with self.assertRaises(AssertionError):validate(root,pointer)

    def test_extra_source_archive_member_is_refused_after_reference_hashes_match(self):
        with tempfile.TemporaryDirectory()as folder:
            root=Path(folder);pointer=self.fixture(root);archive=root/'source.zip'
            with zipfile.ZipFile(archive,'a')as bundle:bundle.writestr('unrecorded.java',b'unrecorded source')
            path=root/'source.json';data=json.loads(path.read_text());data['review_source_archive']['sha256']=sha(archive)
            path.write_text(json.dumps(data));pointer['source_snapshot']['sha256']=sha(path)
            with self.assertRaises(AssertionError):validate(root,pointer,verify_archive=True)

    def test_reference_cannot_escape_workspace(self):
        with tempfile.TemporaryDirectory()as folder:
            root=Path(folder);pointer=self.fixture(root);pointer['source_snapshot']['file']='../outside-source.json'
            with self.assertRaises(AssertionError):validate(root,pointer)


if __name__=='__main__':unittest.main()
