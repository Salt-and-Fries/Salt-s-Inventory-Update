from pathlib import Path
import hashlib,io,json,sys,tempfile,unittest,zipfile

ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
from native_publisher_index import index,matches


def jar(entries):
    result=io.BytesIO()
    with zipfile.ZipFile(result,'w')as z:
        for name,value in entries.items():z.writestr(name,value)
    return result.getvalue()


class NativePublisherIndexTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup);self.path=Path(self.tmp.name)/'publisher.jar'
    def indexed(self,entries,loader='fabric'):
        raw=jar(entries);self.path.write_bytes(raw);return index(self.path,hashlib.sha512(raw).hexdigest(),loader)
    def test_direct_class_is_attributed_without_embedded_claim(self):
        found=matches(self.indexed({'native/Menu.class':b'fixture'}),['native.Menu'])
        self.assertEqual([],found[0]['embedded_chain'])
    def test_declared_fabric_child_has_exact_path_hash_and_native_mod(self):
        child=jar({'native/Menu.class':b'fixture','fabric.mod.json':json.dumps(dict(id='child',version='2.0'))})
        root={'fabric.mod.json':json.dumps(dict(id='parent',version='1.0',jars=[dict(file='META-INF/jars/child.jar')])),'META-INF/jars/child.jar':child}
        found=matches(self.indexed(root),['native.Menu']);self.assertEqual(1,len(found))
        self.assertEqual([dict(id='child',version='2.0')],found[0]['declared_mods'])
        self.assertEqual(hashlib.sha256(child).hexdigest(),found[0]['embedded_chain'][0]['sha256'])
    def test_undeclared_embedded_jar_is_not_available_classpath(self):
        self.assertEqual([],matches(self.indexed({'hidden.jar':jar({'native/Menu.class':b'fixture'})}),['native.Menu']))
    def test_declared_jarjar_child_is_followed(self):
        child=jar({'native/Menu.class':b'fixture'})
        meta=dict(jars=[dict(path='META-INF/jarjar/lib.jar',identifier=dict(group='fixture',artifact='lib'))])
        found=matches(self.indexed({'META-INF/jarjar/metadata.json':json.dumps(meta),'META-INF/jarjar/lib.jar':child},'neoforge'),['native.Menu'])
        self.assertEqual('META-INF/jarjar/metadata.json',found[0]['embedded_chain'][0]['declared_by'])
    def test_wrong_outer_pin_is_refused(self):
        self.path.write_bytes(jar({'native/Menu.class':b'fixture'}))
        with self.assertRaises(AssertionError):index(self.path,'0'*128,'fabric')
    def test_missing_or_escaping_declaration_is_refused(self):
        for entry in ['missing.jar','../escape.jar','/absolute.jar']:
            with self.assertRaises(AssertionError):self.indexed({'fabric.mod.json':json.dumps(dict(id='parent',jars=[dict(file=entry)]))})
    def test_native_literal_description_line_break_does_not_hide_declared_child(self):
        child=jar({'native/Menu.class':b'fixture'})
        meta='{"id":"parent","description":"native line\nbreak","jars":[{"file":"child.jar"}]}'
        self.assertEqual(1,len(matches(self.indexed({'fabric.mod.json':meta,'child.jar':child}),['native.Menu'])))
        with self.assertRaises(json.JSONDecodeError):self.indexed({'fabric.mod.json':'{broken syntax'})
    def test_inactive_loader_template_does_not_hide_real_fabric_dependency(self):
        child=jar({'native/Menu.class':b'fixture','fabric.mod.json':json.dumps(dict(id='child')),
            'META-INF/neoforge.mods.toml':'[[dependencies.${mod_id}]]'})
        root={'fabric.mod.json':json.dumps(dict(id='parent',jars=[dict(file='child.jar')])),'child.jar':child}
        self.assertEqual(1,len(matches(self.indexed(root),['native.Menu'])))
        import tomllib
        with self.assertRaises(tomllib.TOMLDecodeError):self.indexed({'META-INF/neoforge.mods.toml':'[[dependencies.${mod_id}]]'},'neoforge')
    def test_only_active_loader_nested_declarations_are_followed(self):
        child=jar({'native/Menu.class':b'fixture'})
        root={'fabric.mod.json':json.dumps(dict(id='parent',jars=[dict(file='child.jar')])),'child.jar':child}
        self.assertEqual([],matches(self.indexed(root,'forge'),['native.Menu']))
        root={'META-INF/jarjar/metadata.json':json.dumps(dict(jars=[dict(path='child.jar')])),'child.jar':child}
        self.assertEqual([],matches(self.indexed(root),['native.Menu']))
    def test_native_loader_must_be_explicit_and_supported(self):
        self.path.write_bytes(jar({}))
        for loader in ['',None,'unknown']:
            with self.assertRaises(AssertionError):index(self.path,hashlib.sha512(self.path.read_bytes()).hexdigest(),loader)


if __name__=='__main__':unittest.main()
