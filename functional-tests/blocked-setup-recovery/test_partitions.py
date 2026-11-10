"""Require disclosed, dependency-complete partitions of the exact failed inputs."""
from pathlib import Path
import hashlib, json, unittest, zipfile

ROOT=Path(__file__).resolve().parents[2]


def metadata(row):
    with zipfile.ZipFile(row['path']) as jar:
        return json.loads(jar.read('fabric.mod.json').decode('utf-8-sig'),strict=False) if 'fabric.mod.json' in jar.namelist() else {}


class PartitionTests(unittest.TestCase):
    def test_every_partition_preserves_all_retained_publisher_bytes_and_platform_pins(self):
        plan=json.loads((ROOT/'output/blocked-setup-recovery/fixture-isolation-plan.json').read_text())
        for entry in plan:
            with self.subTest(fixture=entry['fixture']):
                partition=json.loads((ROOT/entry['fixture']).read_text())
                source=json.loads((ROOT/'build/all-target-rollout/fixtures'/partition['setup_recovery']['source_fixture']/'fixture.json').read_text())
                self.assertEqual(source['runtime'],partition['runtime'])
                removed=dict(partition['setup_recovery']['removed'])
                self.assertTrue(removed)
                self.assertEqual({r['filename'] for r in source['artifacts']}-{r['filename'] for r in partition['artifacts']},set(removed))
                original={r['filename']:r for r in source['artifacts']}
                for row in partition['artifacts']:
                    self.assertEqual(original[row['filename']],row)
                    self.assertEqual(row['sha512'],hashlib.sha512(Path(row['path']).read_bytes()).hexdigest())

    def test_removed_libraries_do_not_leave_required_dependants_behind(self):
        plan=json.loads((ROOT/'output/blocked-setup-recovery/fixture-isolation-plan.json').read_text())
        for entry in plan:
            with self.subTest(fixture=entry['fixture']):
                partition=json.loads((ROOT/entry['fixture']).read_text())
                source=json.loads((ROOT/'build/all-target-rollout/fixtures'/partition['setup_recovery']['source_fixture']/'fixture.json').read_text())
                kept={r['filename'] for r in partition['artifacts']}
                removed_ids={metadata(row).get('id') for row in source['artifacts'] if row['filename'] not in kept}
                for row in partition['artifacts']:
                    mod=metadata(row)
                    self.assertFalse(set(mod.get('depends',{})) & removed_ids,(mod.get('id'),removed_ids))

    def test_split_bumblezone_profiles_together_retain_every_original_publisher(self):
        folder=ROOT/'build/blocked-setup-recovery/fixtures'
        bee=json.loads((folder/'bumblezone-without-polymer-v2.json').read_text())
        polymer=json.loads((folder/'polymer-without-bumblezone.json').read_text())
        original=json.loads((ROOT/'build/all-target-rollout/fixtures/regression-fill-2612-fabric-inventory-batch/fixture.json').read_text())
        self.assertEqual({r['filename'] for r in original['artifacts']},{r['filename'] for r in bee['artifacts']+polymer['artifacts']})
        self.assertIn('the_bumblezone',{metadata(r).get('id') for r in bee['artifacts']})
        self.assertIn('universal-graves',{metadata(r).get('id') for r in polymer['artifacts']})

    def test_shipping_jars_remain_identical_to_the_start_of_recovery(self):
        before=json.loads((ROOT/'output/blocked-setup-recovery/shipping-subjects-before.json').read_text())
        self.assertEqual(24,len(before))
        for name,expected in before.items():
            self.assertEqual(expected,hashlib.sha256((ROOT/name).read_bytes()).hexdigest(),name)


if __name__=='__main__':unittest.main()
