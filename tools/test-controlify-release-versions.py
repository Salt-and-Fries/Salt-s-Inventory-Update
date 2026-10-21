"""Regression tests for independently versioned companion dependencies."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from zipfile import ZipFile

spec = importlib.util.spec_from_file_location('controller_runtime', Path(__file__).with_name('verify-controlify-runtime.py'))
runtime = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runtime)


class CompanionReleaseVersions(unittest.TestCase):
    def exercise(self, expected_core, wrong_label=False):
        for loader in ('fabric', 'forge', 'neoforge'):
            for mod_id, version in (('salts_inventory_controlify', '0.1.1'), ('salts_inventory_compat', '0.1.2')):
                with self.subTest(loader=loader, mod_id=mod_id), tempfile.TemporaryDirectory() as folder:
                    path = Path(folder) / 'companion.jar'
                    with ZipFile(path, 'w') as archive:
                        if loader == 'fabric':
                            archive.writestr('fabric.mod.json', json.dumps({'id': mod_id, 'version': version,
                                'depends': {'salts_inventory_update': '=0.1.5'}}))
                        else:
                            resource = 'META-INF/mods.toml' if loader == 'forge' else 'META-INF/neoforge.mods.toml'
                            archive.writestr(resource, f'[[mods]]\nmodId="{mod_id}"\nversion="{version}"\n'
                                f'[[dependencies.{mod_id}]]\nmodId="salts_inventory_update"\nversionRange="[0.1.5]"\n')
                    action = lambda: runtime.companion_versions(path, loader, expected_core,
                                                               '9.9.9' if wrong_label else version, mod_id)
                    if expected_core != '0.1.5' or wrong_label:
                        with self.assertRaises(AssertionError): action()
                    else:
                        action()

    def test_independent_labels_match_the_required_core(self):
        self.exercise('0.1.5')

    def test_wrong_core_version_is_rejected(self):
        self.exercise('0.1.4')

    def test_wrong_loaded_companion_label_is_rejected(self):
        self.exercise('0.1.5', wrong_label=True)


if __name__ == '__main__': unittest.main()
