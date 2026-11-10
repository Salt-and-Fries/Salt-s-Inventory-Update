from pathlib import Path
import copy,hashlib,json,sys,tempfile,unittest,zipfile
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
from full_suite_provenance import SCHEMA,bundle,digest_inputs,loaded_code_source,ordinary_dependencies,runtime_subject,sha,sources,verify_attempt,verify_bundle


class FullSuiteProvenance(unittest.TestCase):
    def fixture(self,root):
        for name,text in {'functional-tests/chest-close-qa.init.gradle':'actual native launcher',
            'functional-tests/packaged-java/GameplayQaInitializer.java':'actual initializer',
            'functional-tests/src/main/java/Harness.java':'actual assertions',
            'functional-tests/release-specialized/not-used.py':'focused only',
            'tools/prepare-compatibility-qa.py':'fixture compiler',
            'tools/prepare-public-release-matrix.py':'documentation only'}.items():
            p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
        return dict(script='functional-tests/chest-close-qa.init.gradle')

    def test_actual_launchers_and_initializers_are_included_but_release_reports_are_separate(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);inputs=sources(root,self.fixture(root))
            self.assertIn('functional-tests/chest-close-qa.init.gradle',inputs)
            self.assertIn('functional-tests/packaged-java/GameplayQaInitializer.java',inputs)
            self.assertIn('tools/prepare-compatibility-qa.py',inputs)
            self.assertNotIn('functional-tests/release-specialized/not-used.py',inputs)
            self.assertNotIn('tools/prepare-public-release-matrix.py',inputs)

    def test_immutable_archive_survives_later_source_changes_but_new_fingerprint_changes(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);case=self.fixture(root);inputs=sources(root,case);ref=bundle(root,inputs)
            (root/'functional-tests/chest-close-qa.init.gradle').write_text('changed actual launcher')
            self.assertTrue(verify_bundle(root,ref,inputs));self.assertNotEqual(inputs,sources(root,case))
            with self.assertRaises(AssertionError):bundle(root,sources(root,case)|{'functional-tests/chest-close-qa.init.gradle':inputs['functional-tests/chest-close-qa.init.gradle']})

    def test_added_archive_member_is_refused_even_when_container_hash_is_refreshed(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);inputs=sources(root,self.fixture(root));ref=bundle(root,inputs);path=root/ref['file']
            with zipfile.ZipFile(path,'a') as z:z.writestr('extra.py','unrecorded input')
            ref['sha256']=sha(path)
            with self.assertRaises(AssertionError):verify_bundle(root,ref,inputs)

    def test_changed_log_failed_exit_and_old_schema_are_not_accepted(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);inputs=sources(root,self.fixture(root));ref=bundle(root,inputs);log=root/'run.log';log.write_text('PASS')
            entry=dict(qa_provenance_schema=SCHEMA,exit_code=0,qa_inputs=inputs,qa_source_archive=ref,log='run.log',log_sha256=sha(log),runtime_evidence={})
            self.assertTrue(verify_attempt(root,entry))
            for patch in [dict(exit_code=False),dict(exit_code=1),dict(qa_provenance_schema='legacy-five-files')]:
                with self.assertRaises(AssertionError):verify_attempt(root,entry|patch)
            log.write_text('changed')
            with self.assertRaises(AssertionError):verify_attempt(root,entry)

    def forge(self,root):
        tool=root/'tools/remap-controlify-forge-qa.py';tool.parent.mkdir();tool.write_bytes((ROOT/'tools/remap-controlify-forge-qa.py').read_bytes())
        runtime=root/'runtime';folder=root/'report';folder.mkdir();(runtime/'original-mods').mkdir(parents=True);(runtime/'mods').mkdir()
        for name,value in [('installer-tools.jar',b'tool'),('mappings.zip',b'mappings')]: (root/name).write_bytes(value)
        source=runtime/'original-mods/core.jar';mapped=runtime/'mods/core.jar'
        for p,body in [(source,b'original class'),(mapped,b'named class')]:
            with zipfile.ZipFile(p,'w') as z:z.writestr('Main.class',body);z.writestr('assets/pixel.png',b'actual resource')
        data=dict(schema_version=1,operation='SRG_TO_MCP',installer_tools=str(root/'installer-tools.jar'),installer_tools_sha256=sha(root/'installer-tools.jar'),
            mappings=str(root/'mappings.zip'),mappings_sha256=sha(root/'mappings.zip'),
            artifacts=[dict(original=str(source),original_sha256=sha(source),mapped=str(mapped),mapped_sha256=sha(mapped))])
        path=folder/'forge-remap-lineage.json';path.write_text(json.dumps(data));return folder,runtime,source,mapped,path,data

    def test_declared_forge_namespace_copy_requires_exact_preserved_shipping_source(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);folder,runtime,source,mapped,path,data=self.forge(root)
            result=runtime_subject(root,folder,runtime,'dist/core/core.jar',sha(source),'forge')
            self.assertEqual(result['mode'],'declared-Forge-SRG_TO_MCP-QA-copy')
            self.assertEqual(result['subject_sha256'],sha(source));self.assertEqual(result['sha256'],sha(mapped))
            with self.assertRaises(AssertionError):runtime_subject(root,folder,runtime,'core.jar',sha(source),'fabric')
            with self.assertRaises(AssertionError):runtime_subject(root,folder,runtime,'core.jar','0'*64,'forge')

    def test_changed_forge_tool_output_or_operation_is_refused(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);folder,runtime,source,mapped,path,data=self.forge(root);expected=sha(source)
            for patch in [dict(operation='other'),dict(mappings_sha256='0'*64)]:
                path.write_text(json.dumps(data|patch))
                with self.assertRaises(AssertionError):runtime_subject(root,folder,runtime,'core.jar',expected,'forge')
            path.write_text(json.dumps(data));mapped.write_bytes(b'changed')
            with self.assertRaises(AssertionError):runtime_subject(root,folder,runtime,'core.jar',expected,'forge')

    def test_forge_resource_loss_is_refused_even_when_mapped_hash_is_updated(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);folder,runtime,source,mapped,path,data=self.forge(root);expected=sha(source)
            with zipfile.ZipFile(mapped,'w') as z:z.writestr('Main.class',b'named class')
            data['artifacts'][0]['mapped_sha256']=sha(mapped);path.write_text(json.dumps(data))
            with self.assertRaises(AssertionError):runtime_subject(root,folder,runtime,'core.jar',expected,'forge')

    def test_actual_loader_code_source_must_be_a_jar_inside_the_runtime(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);runtime=root/'runtime';runtime.mkdir();jar=runtime/'processed.jar';jar.write_bytes(b'loader processed')
            result=loaded_code_source(root,runtime,jar.as_uri());self.assertEqual(result['sha256'],sha(jar))
            outside=root/'outside.jar';outside.write_bytes(b'other runtime')
            for url in [outside.as_uri(),'https://example.test/core.jar',(runtime/'classes').as_uri()]:
                with self.assertRaises(AssertionError):loaded_code_source(root,runtime,url)
            union=jar.as_uri().replace('file:','union:')+'%23189!/'
            self.assertEqual(loaded_code_source(root,runtime,union)['sha256'],sha(jar))

    def test_dependency_manifest_binds_target_pins_and_actual_runtime_copy(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);folder=root/'report';folder.mkdir();runtime=root/'runtime';(runtime/'mods').mkdir(parents=True)
            source=root/'publisher.jar';source.write_bytes(b'publisher bytes');copied=runtime/'mods/publisher.jar';copied.write_bytes(source.read_bytes())
            case=dict(minecraft='1.20.1',loader='fabric');row=dict(path=str(source),filename=source.name,coordinate='publisher:version',sha512=hashlib.sha512(source.read_bytes()).hexdigest())
            data=dict(**case,scenario='ae2',artifacts=[row]);manifest=folder/'upstream-fixtures.json';manifest.write_text(json.dumps(data))
            result=ordinary_dependencies(root,folder,runtime,case,'ae2');self.assertEqual(result['upstream'][0]['runtime_copy']['sha256'],sha(source))
            copied.write_bytes(b'other publisher')
            with self.assertRaises(AssertionError):ordinary_dependencies(root,folder,runtime,case,'ae2')
            copied.write_bytes(source.read_bytes());source.write_bytes(b'changed cache')
            with self.assertRaises(AssertionError):ordinary_dependencies(root,folder,runtime,case,'ae2')

    def test_dependency_manifest_from_another_target_is_refused(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);folder=root/'report';folder.mkdir();runtime=root/'runtime';(runtime/'mods').mkdir(parents=True)
            (folder/'upstream-fixtures.json').write_text(json.dumps(dict(minecraft='1.21.1',loader='fabric',scenario='core',artifacts=[])))
            with self.assertRaises(AssertionError):ordinary_dependencies(root,folder,runtime,dict(minecraft='1.20.1',loader='fabric'),'core')


if __name__=='__main__':unittest.main()
