"""Offline dependency-documentation guards; no native installation acceptance."""
from pathlib import Path
import importlib.util,json,sys,unittest
ROOT=Path(__file__).resolve().parents[2];p=ROOT/'tools/audit-release-upstream-dependencies.py'
spec=importlib.util.spec_from_file_location('upstream_dependency_auditor',p);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)

class DependencyDeclarations(unittest.TestCase):
    def fabric(self,**kw):return m.descriptor(json.dumps(dict(id='testmod',**kw)).encode(),'fabric')
    def toml(self,text,loader='neoforge'):return m.descriptor(('modLoader="javafml"\nloaderVersion="[1,)"\n[[mods]]\nmodId="testmod"\nversion="1"\n'+text).encode(),loader)
    def test_fabric_categories_and_version_alternatives_are_preserved(self):
        d=self.fabric(depends={'base':['>=1','2.x']},recommends={'recommended':'*'},suggests={'suggested':'*'},breaks={'bad':'<2'},conflicts={'warn':'*'})
        self.assertEqual([v['relation']for v in d['dependencies']],list(m.RELATIONS))
        self.assertEqual(d['dependencies'][0]['constraint'],['>=1','2.x'])
    def test_fabric_environment_is_preserved_and_unknown_value_rejected(self):
        self.assertEqual(self.fabric(environment='client')['environment'],'client')
        with self.assertRaises(AssertionError):self.fabric(environment='unknown')
    def test_bad_dependency_map_or_constraint_is_rejected(self):
        for value in [[],{'base':None},{'base':[True]},{'base':[]}]:
            with self.subTest(value=value):
                with self.assertRaises(AssertionError):self.fabric(depends=value)
    def test_neoforge_explicit_type_and_omitted_default_are_distinct(self):
        d=self.toml('[[dependencies.testmod]]\nmodId="base"\ntype="OPTIONAL"\nversionRange="[1,)"\nmandatory=true\nside="CLIENT"\n[[dependencies.testmod]]\nmodId="required_base"\n')
        self.assertEqual([(v['relation'],v['declared_type'])for v in d['dependencies']],[('optional','OPTIONAL'),('required','')])
        self.assertEqual(d['dependencies'][0]['side'],'CLIENT')
        self.assertIs(d['dependencies'][0]['declared_mandatory'],True)
    def test_neoforge_extra_required_flag_is_retained_without_changing_default(self):
        d=self.toml('[[dependencies.testmod]]\nmodId="base"\nrequired=true\n')['dependencies'][0]
        self.assertEqual(d['relation'],'required');self.assertEqual(d['declared_type'],'');self.assertIs(d['declared_required'],True)
        with self.assertRaises(AssertionError):self.toml('[[dependencies.testmod]]\nmodId="base"\ntype="other"\n')
    def test_forge_mandatory_requires_a_boolean(self):
        d=self.toml('[[dependencies.testmod]]\nmodId="base"\nmandatory=false\nversionRange="[1,)"\n','forge')['dependencies'][0]
        self.assertEqual(d['relation'],'optional');self.assertIs(d['declared_mandatory'],False)
        for declaration in ['','mandatory=1\n','mandatory="false"\n']:
            with self.subTest(declaration=declaration):
                with self.assertRaises(AssertionError):self.toml('[[dependencies.testmod]]\nmodId="base"\n'+declaration,'forge')
    def test_undeclared_owner_and_invalid_loader_are_rejected(self):
        with self.assertRaises(AssertionError):self.toml('[[dependencies.anothermod]]\nmodId="base"\ntype="required"\n')
        with self.assertRaises(AssertionError):m.descriptor(b'{}','unknown-loader')
    def test_empty_declarations_do_not_invent_an_installation_requirement(self):
        d=self.fabric();self.assertEqual(d['mod_ids'],['testmod']);self.assertEqual(d['dependencies'],[])
        self.assertEqual(self.toml('')['dependencies'],[])

if __name__=='__main__':unittest.main(verbosity=2)
