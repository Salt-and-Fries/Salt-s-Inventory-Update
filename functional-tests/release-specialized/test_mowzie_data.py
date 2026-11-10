"""Payment/result ledger corruption controls, separate from live gameplay."""
import unittest
from mowzie_features import item_count, stack


class MobTradeData(unittest.TestCase):
    def test_legacy_result_and_remainder_retain_full_metadata(self):
        native = '{id:"minecraft:gold_nugget",Count:4b,tag:{marker:7b,bytes:[B;2b,4b]}}'
        self.assertEqual(stack(native, 5), stack(native.replace('Count:4b', 'Count:5b')))
        self.assertNotEqual(stack(native), stack(native.replace('2b,4b', '2b,5b')))
        self.assertNotEqual(stack(native), stack(native.replace('marker:7b', 'marker:7')))

    def test_modern_result_requires_exact_item_count_and_components(self):
        native = '{id:"mowziesmobs:blowgun",count:1,components:{"minecraft:custom_data":{owner:"test"}}}'
        for bad in [native.replace('count:1', 'count:2'), native.replace('owner:"test"', 'owner:"other"'),
                    native.replace('mowziesmobs:blowgun', 'minecraft:stick')]:
            with self.subTest(bad=bad): self.assertNotEqual(stack(native), stack(bad))

    def test_native_quantity_schema_does_not_accept_lookalike_numeric_tags(self):
        self.assertEqual(item_count('{}'), ('minecraft:air', 0))
        self.assertEqual(item_count('{id:"minecraft:emerald",Count:5b}'), ('minecraft:emerald', 5))
        self.assertEqual(item_count('{id:"minecraft:emerald",count:5}'), ('minecraft:emerald', 5))
        for bad in ['{id:"minecraft:emerald",Count:5}', '{id:"minecraft:emerald",count:5b}', '{count:5}']:
            with self.subTest(bad=bad), self.assertRaises(AssertionError): item_count(bad)


if __name__ == '__main__': unittest.main()
