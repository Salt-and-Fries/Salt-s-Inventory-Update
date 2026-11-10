"""Focused pin-selection corruption checks; these do not launch Minecraft."""
from pathlib import Path
import copy,hashlib,json,tempfile,unittest
import ae2_legacy_publisher_variant as variant
import run_ae2_cpu_controls_legacy_variant as cpu


class LegacyPublisherTests(unittest.TestCase):
    def fixture(self):
        return dict(minecraft='1.20.1',loader='fabric',scenario='ae2-atlas',runtime={'fabric_loader':'0.19.5'},
            artifacts=[dict(filename='appliedenergistics2-fabric-15.4.12.jar',path='unchanged-original.jar',sha512='original'),dict(filename='energy-3.0.0.jar',sha512='preserved')],
            versions=[dict(project_id='XxWD5pD3',version_id='old-id',version='15.4.12'),dict(project_id='other',version='keep')])

    def artifact(self):return dict(filename=variant.FILENAME,sha512=variant.SHA512,path='audited.jar')

    def test_only_declared_native_pin_and_version_metadata_change(self):
        original=self.fixture();before=copy.deepcopy(original);value=variant.substitute_manifest(original,self.artifact())
        self.assertEqual(original,before);self.assertEqual(value['artifacts'][1],original['artifacts'][1]);self.assertEqual(value['runtime'],original['runtime'])
        self.assertEqual(value['versions'][1],original['versions'][1]);self.assertIsNone(value['versions'][0]['version_id'])
        value['artifacts'][0]=original['artifacts'][0];value['versions'][0]=original['versions'][0];self.assertEqual(value,original)

    def test_unreviewed_pin_rejected(self):
        for key in['filename','sha512']:
            bad=self.artifact();bad[key]='unreviewed'
            with self.assertRaises(AssertionError):variant.substitute_manifest(self.fixture(),bad)

    def test_wrong_target_or_scenario_rejected(self):
        for key in['minecraft','loader','scenario']:
            bad=self.fixture();bad[key]='other'
            with self.assertRaises(AssertionError):variant.substitute_manifest(bad,self.artifact())

    def test_missing_duplicate_or_already_old_publisher_rejected(self):
        for mode in['missing','duplicate','already-old']:
            bad=self.fixture()
            if mode=='missing':bad['artifacts']=bad['artifacts'][1:]
            elif mode=='duplicate':bad['artifacts'].append(copy.deepcopy(bad['artifacts'][0]))
            else:bad['artifacts'][0]['filename']=variant.FILENAME
            with self.assertRaises(AssertionError):variant.substitute_manifest(bad,self.artifact())

    def test_false_descriptive_metadata_rejected(self):
        for mode in['missing','duplicate','wrong-version']:
            bad=self.fixture()
            if mode=='missing':bad['versions']=bad['versions'][1:]
            elif mode=='duplicate':bad['versions'].append(copy.deepcopy(bad['versions'][0]))
            else:bad['versions'][0]['version']='wrong'
            with self.assertRaises(AssertionError):variant.substitute_manifest(bad,self.artifact())

    def test_exact_already_selected_native_publisher_is_preserved(self):
        native=variant.native_artifact();original=self.fixture()
        original['artifacts'][0]=native
        original['versions'][0]=dict(project_id='XxWD5pD3',version_id='real-15.4.11-id',version='15.4.11')
        before=copy.deepcopy(original)
        self.assertEqual(variant.substitute_manifest(original,native),before)
        self.assertEqual(original,before)

    def test_already_selected_metadata_or_corrupt_bytes_are_rejected(self):
        native=variant.native_artifact();original=self.fixture()
        original['artifacts'][0]=native
        original['versions'][0]=dict(project_id='XxWD5pD3',version='15.4.11')
        for mode in ['hash','version','bytes']:
            bad=copy.deepcopy(original)
            if mode=='hash':bad['artifacts'][0]['sha512']='wrong'
            elif mode=='version':bad['versions'][0]['version']='wrong'
            else:
                with tempfile.TemporaryDirectory()as d:
                    p=Path(d)/variant.FILENAME;p.write_bytes((variant.ROOT/native['path']).read_bytes()+b'changed')
                    bad['artifacts'][0]['path']=str(p)
                    with self.assertRaises(AssertionError):variant.substitute_manifest(bad,native)
                continue
            with self.assertRaises(AssertionError):variant.substitute_manifest(bad,native)

    def test_actual_publisher_identity_and_hash(self):
        native=variant.native_artifact();self.assertEqual(native['filename'],variant.FILENAME)
        with tempfile.TemporaryDirectory()as d:
            p=Path(d)/variant.FILENAME;p.write_bytes((variant.ROOT/native['path']).read_bytes())
            row=dict(family='upstream',filename=p.name,file=p.name,sha256=hashlib.sha256(p.read_bytes()).hexdigest(),sha512=variant.SHA512)
            self.assertEqual(variant.verify_runtime_artifacts(d,[row])['sha512'],variant.SHA512)
            p.write_bytes(p.read_bytes()+b'changed')
            with self.assertRaises(AssertionError):variant.verify_runtime_artifacts(d,[row])

    def test_duplicate_or_renamed_staged_publishers_rejected(self):
        with tempfile.TemporaryDirectory()as d:
            row=dict(family='upstream',filename=variant.FILENAME,file='does-not-exist.jar',sha256='wrong')
            with self.assertRaises(AssertionError):variant.verify_runtime_artifacts(d,[row,row])
            row['filename']='appliedenergistics2-fabric-15.4.12.jar'
            with self.assertRaises(AssertionError):variant.verify_runtime_artifacts(d,[row])

    def test_cpu_original_derived_wrapper_and_java_preserved(self):
        parent=variant.ROOT/'build/release-readiness';parent.mkdir(exist_ok=True)
        with tempfile.TemporaryDirectory(dir=parent)as d:
            p=Path(d);original=p/'original';original.mkdir();result=cpu.ORIGINAL_PREPARE(original,'1.20.1')
            expected=result['script'].read_text().replace((original/'cpu-controls.init.gradle').relative_to(variant.ROOT).as_posix(),'DECLARED_INIT')
            actual=p/'variant';actual.mkdir();derived=cpu.prepare(actual,'1.20.1')
            text=derived['script'].read_text();start=text.index("\n& python '");end=text.index("\n$arguments=",start)
            stripped=(text[:start]+text[end:]).replace((actual/'cpu-controls.init.gradle').relative_to(variant.ROOT).as_posix(),'DECLARED_INIT')
            self.assertEqual(expected,stripped)
            for source in(original/'generated-shared').rglob('*.java'):
                self.assertEqual(source.read_bytes(),(actual/'generated-shared'/source.relative_to(original/'generated-shared')).read_bytes())
            self.assertIn(variant.ROOT/variant.FIXTURE,derived['inputs'])


if __name__=='__main__':unittest.main()
