"""Negative tests for report-only native Tom workflow reconciliation."""
from pathlib import Path
import copy, importlib.util, json, sys, tempfile, unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT/'tools'))
spec = importlib.util.spec_from_file_location('toms_workflow_evidence',ROOT/'tools/reconcile-toms-release-evidence.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
sys.path.insert(0,str(ROOT/'functional-tests/release-specialized'))
from toms_observed_features import observed_class


class Base:
    def __init__(self, profile):
        self.out = Path(profile)/'toms-release'
        self.out.mkdir()

    def call(self,*args,**kwargs):
        return dict(client=dict(windows=[]))

    def state(self,name):
        self.call('regression-state',ticks=2)
        return dict(menus=[dict(type='com.tom.storagemod.menu.InventoryLinkMenu',valid=True)])

    def toms(self,session):
        self.call('regression-toms-state',session=session,ticks=3)
        return dict(menus=[dict(type='com.tom.storagemod.menu.InventoryLinkMenu')])


class TomWorkflowEvidenceTests(unittest.TestCase):
    def test_original_feature_name_multiplicities_are_required(self):
        self.assertEqual({g:len(v)for g,v in module.CHECK_NAMES.items()},dict(terminal=17,configurator=11,link=11,vending=10))
        for group,names in module.CHECK_NAMES.items():
            checks = [dict(check=name,passed=True)for name in names]
            self.assertEqual(module.validate_checks(group,checks),len(names))
            for altered in [checks[:-1],checks+[checks[0]],[dict(v,passed=1)for v in checks],
                            [dict(v,check='unrelated')for v in checks]]:
                with self.assertRaises(AssertionError):
                    module.validate_checks(group,altered)

    def test_vending_owner_labels_are_not_reclassified_as_configurators(self):
        owner = 'com.tom.trading.menu.VendingMachineConfigMenu'
        for case in ['toms-config','atlas-toms-config']:
            value = module.specification('1.21.1/neoforge',case,[owner])
            self.assertEqual(value['group'],'vending')
            self.assertFalse(value['corrected'])
            self.assertEqual(value['native_classes'],[owner])
        value = module.specification('1.21.1/neoforge','registry-item-toms_storage-inventory_configurator',[owner])
        self.assertEqual(value['group'],'configurator')
        self.assertTrue(value['corrected'])
        self.assertEqual(value['native_classes'],['com.tom.storagemod.menu.InventoryConfiguratorMenu'])

    def test_unknown_alias_target_and_unreviewed_class_correction_are_refused(self):
        for target,case,classes in [
            ('1.20.1/fabric','registry-item-toms_storage-inventory_configurator',['com.tom.trading.gui.VendingMachineConfigMenu']),
            ('1.21.1/neoforge','invented-ui',['com.tom.storagemod.menu.StorageTerminalMenu']),
            ('99/fabric','toms-storage-terminal',['com.tom.storagemod.menu.StorageTerminalMenu']),
            ('1.21.1/neoforge','atlas-toms-config',['com.tom.storagemod.menu.InventoryConfiguratorMenu']),
            ('1.21.1/neoforge','toms-customer',['com.tom.trading.menu.VendingMachineTradingMenu'])]:
            with self.assertRaises(AssertionError):
                module.specification(target,case,classes)
        screen = 'com.tom.trading.screen.VendingMachineTradingScreen'
        self.assertEqual(module.specification('1.21.1/neoforge','toms-customer',[screen])['required_screen'],screen)

    def make_observations(self, root):
        suite = observed_class(Base,'link')(root)
        suite.state('open')
        suite.toms(1)
        refs = [module.reference(root,p)for p in sorted(suite.out.rglob('*.json'))]
        return suite,refs

    def test_every_immediate_native_observation_is_hash_bound_and_sealed(self):
        with tempfile.TemporaryDirectory()as folder:
            root = Path(folder)
            suite,refs = self.make_observations(root)
            index,records = module.observations(root,suite.out,'link',refs)
            self.assertEqual(len(records),4)
            self.assertIn(index,refs)
            with self.assertRaises(AssertionError):
                module.observations(root,suite.out,'link',refs[:-1])
            first = suite.out/suite.observation_refs[0]['file']
            first.write_text(first.read_text()+' ')
            with self.assertRaises(AssertionError):
                module.observations(root,suite.out,'link',refs)

    def test_changed_group_ordinal_and_escaping_observation_path_are_refused(self):
        for mutation in ['group','ordinal','escape','missing-native']:
            with tempfile.TemporaryDirectory()as folder:
                root = Path(folder)
                suite,refs = self.make_observations(root)
                p = suite.observation_dir/'index.json'
                index = json.loads(p.read_text())
                if mutation in ['group','ordinal']:
                    row = index['observations'][0]
                    path = suite.out/row['file']
                    value = json.loads(path.read_text())
                    value['group'if mutation=='group'else'ordinal'] = 'vending'if mutation=='group'else True
                    path.write_text(json.dumps(value))
                    row['sha256'] = module.reference(root,path)['sha256']
                elif mutation == 'escape':
                    index['observations'][0]['file'] = '../outside.json'
                else:
                    index['observations'] = index['observations'][:-1]
                p.write_text(json.dumps(index))
                refs = [module.reference(root,path)for path in sorted(suite.out.rglob('*.json'))]
                with self.assertRaises(AssertionError):
                    module.observations(root,suite.out,'link',refs)


if __name__=='__main__':
    unittest.main()
