"""Reject quantity/component corruption in the native conversion oracle."""
import unittest
from chipped_features import stack


class NativeConversionLedger(unittest.TestCase):
    def test_legacy_only_the_requested_output_count_changes(self):
        prototype='{id:"chipped:test_stone",Count:1b,tag:{marker:42,bytes:[B;1b,2b]}}'
        actual='{tag:{bytes:[B;1b,2b],marker:42},Count:16b,id:"chipped:test_stone"}'
        self.assertEqual(stack(actual),stack(prototype,16))
        self.assertNotEqual(stack(actual.replace('16b','15b')),stack(prototype,16))
        self.assertNotEqual(stack(actual.replace('2b]','3b]')),stack(prototype,16))
    def test_modern_components_and_numeric_types_are_retained(self):
        native='{id:"chipped:test_stone",count:1,components:{"minecraft:custom_data":{marker:42L}}}'
        actual='{id:"chipped:test_stone",count:8,components:{"minecraft:custom_data":{marker:42L}}}'
        self.assertEqual(stack(actual),stack(native,8))
        self.assertNotEqual(stack(actual.replace('42L','42')),stack(native,8))
        self.assertNotEqual(stack('{id:"chipped:test_stone",count:8}'),stack(native,8))
    def test_replacing_a_different_item_cannot_pass_quantity_only(self):
        self.assertNotEqual(stack('{id:"minecraft:stone",Count:16b}'),stack('{id:"chipped:test_stone",Count:1b}',16))


if __name__=='__main__':unittest.main()
