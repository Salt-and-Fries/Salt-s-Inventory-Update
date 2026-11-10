"""Corrupted real archive fixtures must fail the release descriptor auditor."""
from pathlib import Path
import importlib.util, json, tempfile, unittest, zipfile

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('metadata_audit', ROOT/'tools/audit-release-metadata.py')
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)


class ArchiveMutationTests(unittest.TestCase):
    def fixture(self, family='core', loader='fabric', mutate=lambda entries: None):
        row = next(v for v in json.loads((ROOT/'output/release-readiness/candidate-integrity.json').read_text())['artifacts']
                   if v['family'] == family and v['loader'] == loader and v['minecraft'] == '1.20.1')
        with zipfile.ZipFile(ROOT/row['file']) as source:
            entries = {n: source.read(n) for n in source.namelist()}
        mutate(entries)
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder)/'test.jar'
            with zipfile.ZipFile(path, 'w', zipfile.ZIP_DEFLATED) as output:
                for n, value in entries.items(): output.writestr(n, value)
            return audit.inspect(path, row)

    @staticmethod
    def metadata(entries, edit):
        value = json.loads(entries['fabric.mod.json'])
        edit(value)
        entries['fabric.mod.json'] = json.dumps(value).encode()

    def test_optional_icon_and_required_core_resource_provider(self):
        self.assertIsNone(self.fixture('controlify')['icon'])
        companion = self.fixture('compatibility')
        self.assertTrue(companion['icon_provider'].startswith('dist'))
        self.assertIn('core', companion['icon_provider'])

    def test_required_target_cannot_become_an_optional_companion_dependency(self):
        with self.assertRaises(AssertionError):
            self.fixture('compatibility', mutate=lambda e: self.metadata(e, lambda m: m['depends'].update(ae2='*')))

    def test_mismatched_core_version_refused(self):
        with self.assertRaises(AssertionError):
            self.fixture('compatibility', mutate=lambda e: self.metadata(e, lambda m: m['depends'].update(salts_inventory_update='=0.1.5')))

    def test_missing_entrypoint_refused(self):
        with self.assertRaises(AssertionError):
            self.fixture(mutate=lambda e: self.metadata(e, lambda m: m['entrypoints'].update(main=['missing.Example'])))

    def test_missing_mixin_class_refused(self):
        def edit(entries):
            config = json.loads(entries['salts_inventory_update.mixins.json'])
            name = config['package']+'.'+config['mixins'][0]
            del entries[name.replace('.', '/')+'.class']
        with self.assertRaises(AssertionError): self.fixture(mutate=edit)

    def test_missing_mixin_minimum_refused(self):
        def edit(entries):
            config = json.loads(entries['salts_inventory_update.mixins.json'])
            del config['minVersion']
            entries['salts_inventory_update.mixins.json'] = json.dumps(config).encode()
        with self.assertRaises(AssertionError): self.fixture(mutate=edit)

    def test_malformed_mixin_minimum_refused(self):
        for bad in [None, '', False, 0.8, '0.8 broken']:
            with self.subTest(minimum=bad):
                def edit(entries):
                    config = json.loads(entries['salts_inventory_update.mixins.json'])
                    config['minVersion'] = bad
                    entries['salts_inventory_update.mixins.json'] = json.dumps(config).encode()
                with self.assertRaises(AssertionError): self.fixture(mutate=edit)

    def test_supported_mixin_minimum_is_recorded(self):
        result = self.fixture()
        config = next(v for v in result['mixin_references'] if v['config'] == 'salts_inventory_update.mixins.json')
        self.assertEqual(config['minimum_version'], '0.8')

    def test_required_forge_refmap_refused_when_missing(self):
        def edit(entries):
            config = json.loads(entries['salts_inventory_update.mixins.json'])
            del entries[config['refmap']]
        with self.assertRaises(AssertionError): self.fixture(loader='forge', mutate=edit)

    def test_unsupported_class_version_refused(self):
        def edit(entries):
            key = next(n for n in entries if n.endswith('.class'))
            entries[key] = entries[key][:6] + (70).to_bytes(2, 'big') + entries[key][8:]
        with self.assertRaises(AssertionError): self.fixture(mutate=edit)

    def test_wrong_pack_format_refused(self):
        def edit(entries):
            value = json.loads(entries['pack.mcmeta'])
            value['pack']['pack_format'] = 999
            entries['pack.mcmeta'] = json.dumps(value).encode()
        with self.assertRaises(AssertionError): self.fixture(mutate=edit)


if __name__ == '__main__': unittest.main()
