"""Regression checks for missing eligible adapters, optional hooks and target coverage."""
from pathlib import Path
from unittest import TestCase, main, mock
import contextlib, copy, importlib.util, io, json
spec=importlib.util.spec_from_file_location('coverage_audit',Path(__file__).with_name('audit-compatibility-coverage.py'))
audit=importlib.util.module_from_spec(spec);spec.loader.exec_module(audit)
ROOT=audit.ROOT
READ=Path.read_text
EXISTS=Path.is_file
LOCK=ROOT/'compatibility/coverage-targets.json'

class CoverageRegression(TestCase):
    def run_audit(self):
        with contextlib.redirect_stdout(io.StringIO()): audit.audit()
    def test_current_sources_cover_the_reviewed_matrix(self):
        self.run_audit()
    def test_missing_legacy_emi_adapter_fails(self):
        missing=ROOT/'compatibility/versions/1.20.1/fabric/src/main/java/com/salts_inventory_update/compat/emi/RuntimeEmiRecipeBrowserAccess.java'
        with mock.patch.object(Path,'is_file',lambda path: False if path==missing else EXISTS(path)):
            with self.assertRaisesRegex(AssertionError,'missing adapter source emi/RuntimeEmiRecipeBrowserAccess'):
                self.run_audit()
    def test_unregistered_fabric_hooks_fail(self):
        metadata=ROOT/'compatibility/versions/1.21.1/fabric/src/main/resources/fabric.mod.json'
        changed=json.loads(READ(metadata));changed['mixins']=[]
        def read(path,*args,**kwargs):return json.dumps(changed) if path==metadata else READ(path,*args,**kwargs)
        with mock.patch.object(Path,'read_text',read):
            with self.assertRaisesRegex(AssertionError,'Fabric hooks not registered'):self.run_audit()
    def test_unregistered_emi_provider_fails_even_when_classes_exist(self):
        metadata=ROOT/'compatibility/versions/1.20.1/fabric/src/main/resources/fabric.mod.json'
        changed=json.loads(READ(metadata));changed['entrypoints'].pop('emi')
        def read(path,*args,**kwargs):return json.dumps(changed) if path==metadata else READ(path,*args,**kwargs)
        with mock.patch.object(Path,'read_text',read):
            with self.assertRaisesRegex(AssertionError,'Fabric provider entrypoint not registered'):self.run_audit()
    def test_newly_available_upstream_requires_an_adapter(self):
        changed=json.loads(READ(LOCK))
        target=next(t for t in changed['targets'] if t['minecraft']=='1.21.11' and t['loader']=='fabric')
        self.assertFalse(target['integrations']['emi']['available'])
        target['integrations']['emi'].update(available=True,version_id='future-upstream',sha512='0'*128)
        def read(path,*args,**kwargs):return json.dumps(changed) if path==LOCK else READ(path,*args,**kwargs)
        with mock.patch.object(Path,'read_text',read):
            with self.assertRaisesRegex(AssertionError,'missing adapter source emi/RuntimeEmiRecipeBrowserAccess'):self.run_audit()
    def test_missing_loader_target_fails(self):
        changed=json.loads(READ(LOCK));changed['targets'].pop()
        def read(path,*args,**kwargs):return json.dumps(changed) if path==LOCK else READ(path,*args,**kwargs)
        with mock.patch.object(Path,'read_text',read):
            with self.assertRaisesRegex(AssertionError,'target matrix differs'):self.run_audit()

if __name__=='__main__':main()
