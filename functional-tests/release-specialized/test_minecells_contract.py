"""Native-codec and recipe-contract negative controls; no live gameplay claim."""
from pathlib import Path
import json, tempfile, unittest
from minecells_features import native_stack, recipe_contract


class MineCellsContract(unittest.TestCase):
    def test_native_codec_empty_and_real_stacks(self):
        self.assertEqual(native_stack('{id:"minecells:balanced_blade",Count:1b}', '1.20.1'), ('minecells:balanced_blade', 1))
        self.assertEqual(native_stack('{id:"minecells:balanced_blade",count:1}', '1.21.1'), ('minecells:balanced_blade', 1))
        self.assertEqual(native_stack('{}', '1.21.1'), ('minecraft:air', 0))
        self.assertEqual(native_stack('{id:"minecraft:air",Count:0b}', '1.20.1'), ('minecraft:air', 0))

    def test_wrong_count_schema_and_invalid_quantity_are_refused(self):
        for text, mc in [('{id:"minecells:balanced_blade",count:1}', '1.20.1'),
                         ('{id:"minecells:balanced_blade",Count:1b}', '1.21.1'),
                         ('{id:"minecells:balanced_blade",count:0}', '1.21.1'),
                         ('{id:"minecells:balanced_blade"}', '1.21.1')]:
            with self.subTest(text=text, mc=mc), self.assertRaises(AssertionError): native_stack(text, mc)

    def test_exact_native_recipe_counts_and_costs(self):
        # Read the actual pinned native archives and make a temporary stage
        # ledger, without preparing or launching a Minecraft runtime.
        root = Path(__file__).resolve().parents[2]
        for mc, fixture, count in [('1.20.1', '1201-fabric-specialized-v1', 79),
                                   ('1.21.1', '1211-fabric-specialized-v3', 80)]:
            manifest = json.loads((root / 'build/all-target-rollout/fixtures' / fixture / 'fixture.json').read_text())
            native = next(v for v in manifest['artifacts'] if v['filename'].startswith('minecells-'))
            path = root / native['path']
            import hashlib
            with tempfile.TemporaryDirectory() as folder:
                profile = Path(folder)
                row = dict(filename=native['filename'], file=str(path), sha256=hashlib.sha256(path.read_bytes()).hexdigest())
                (profile / 'staged-artifacts.json').write_text(json.dumps(dict(artifacts=[row])))
                contract = recipe_contract(profile, mc)
                self.assertEqual(contract['native_recipe_count'], count)
                row['sha256'] = '0' * 64
                (profile / 'staged-artifacts.json').write_text(json.dumps(dict(artifacts=[row])))
                with self.assertRaises(AssertionError): recipe_contract(profile, mc)


if __name__ == '__main__': unittest.main()
