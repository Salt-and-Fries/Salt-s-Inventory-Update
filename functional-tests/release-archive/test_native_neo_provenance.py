from pathlib import Path
import sys,unittest
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'functional-tests/release-specialized'))
from native_neo_provenance import private_init


class NativeNeoProvenanceTests(unittest.TestCase):
    def setUp(self):self.text=(ROOT/'functional-tests/aether-windows/native.init.gradle').read_text()
    def test_private_paths_are_per_run_and_actual_jar_is_in_ledger(self):
        result=private_init(self.text,'synthetic-one')
        self.assertIn("root.file('build/release-readiness/synthetic-one/native-observer/generated-metadata')",result)
        self.assertIn("root.layout.projectDirectory.dir('build/release-readiness/synthetic-one/native-observer/artifact')",result)
        self.assertLess(result.index("if(loader=='neoforge')root.copy{from(qaJar.get().archiveFile);into(mods)}"),result.index("inputs.add([file:qaJar.get().archiveFile.get().asFile,filename:'native-ui-reference.jar'])"))
        self.assertEqual(self.text.count('root.copy{'),result.count('root.copy{'))
    def test_future_run_does_not_reuse_the_previous_binary_or_metadata(self):
        first=private_init(self.text,'synthetic-one');second=private_init(self.text,'synthetic-two')
        self.assertNotEqual(first,second);self.assertNotIn('synthetic-one',second)
    def test_changed_staging_contract_and_invalid_run_refused(self):
        with self.assertRaises(AssertionError):private_init(self.text.replace("new File(output,'staged-artifacts.json').text=",''),'synthetic-one')
        with self.assertRaises(AssertionError):private_init(self.text,'../escape')
        with self.assertRaises(AssertionError):private_init(private_init(self.text,'synthetic-one'),'synthetic-two')


if __name__=='__main__':unittest.main()
