"""Full-data and native-tool-cost negative controls for the gem workflows."""
import unittest
from smitherz_features import without_gems,paid_tool
from mowzie_features import stack


class GemData(unittest.TestCase):
    def test_legacy_socket_projection_exempts_only_contained_gems(self):
        before='{id:"minecraft:iron_sword",Count:1b,tag:{GemSlots:3,Gems:[],marker:7b,Damage:0}}'
        linked=before.replace('Gems:[]','Gems:[{Gem:{id:"smitherz:strength_1_gem",Count:1b}}]')
        self.assertEqual(without_gems(before),without_gems(linked))
        for old,new in [('GemSlots:3','GemSlots:2'),('marker:7b','marker:7'),('Damage:0','Damage:1')]:
            self.assertNotEqual(without_gems(before),without_gems(linked.replace(old,new)))

    def test_modern_socket_count_and_typed_custom_data_are_required(self):
        before='{id:"minecraft:iron_sword",count:1,components:{"smitherz:gems":{gems:[],size:3},"minecraft:custom_data":{typed:[B;5b,9b]}}}'
        linked=before.replace('gems:[]','gems:[{id:"smitherz:gem",count:1}]')
        self.assertEqual(without_gems(before),without_gems(linked))
        self.assertNotEqual(without_gems(before),without_gems(linked.replace('5b,9b','5b,8b')))
        with self.assertRaises(AssertionError):without_gems(linked.replace('size:3','size:2'))

    def test_native_consumable_hammer_cost_is_exactly_one(self):
        value='{id:"smitherz:smither_hammer_4",Count:2b,tag:{marker:7b}}'
        self.assertEqual(paid_tool(value,dict(damageable=False)),stack(value,1))
        self.assertEqual(paid_tool(value.replace('Count:2b','Count:1b'),dict(damageable=False)),stack('{}'))

    def test_native_damageable_hammer_cost_preserves_all_other_components(self):
        value='{id:"smitherz:smither_hammer_4",count:1,components:{"minecraft:damage":0,"minecraft:custom_data":{marker:7b}}}'
        expected=value.replace('"minecraft:damage":0','"minecraft:damage":1')
        result=paid_tool(value,dict(damageable=True,damage=0,maxDamage=64))
        self.assertEqual(result,stack(expected))
        self.assertNotEqual(result,stack(expected.replace('marker:7b','marker:7')))

    def test_inappropriate_broken_or_wrong_equipment_fixtures_are_refused(self):
        with self.assertRaises(AssertionError):paid_tool('{id:"smitherz:smither_hammer_4",count:1}',dict(damageable=True,damage=63,maxDamage=64))
        with self.assertRaises(AssertionError):without_gems('{id:"minecraft:stick",count:1}')

    def test_native_legacy_capability_data_cannot_disappear_during_gem_actions(self):
        gear='{id:"minecraft:iron_sword",Count:1b,tag:{GemSlots:3,Gems:[],marker:7b},ForgeCaps:{"example:energy":{stored:41L}}}'
        self.assertNotEqual(without_gems(gear),without_gems(gear.replace('stored:41L','stored:42L')))
        tool='{id:"smitherz:smither_hammer_4",Count:2b,ForgeCaps:{"example:typed":{marker:7b}}}'
        self.assertEqual(paid_tool(tool,dict(damageable=False)),stack(tool,1))
        self.assertNotEqual(paid_tool(tool,dict(damageable=False)),stack(tool.replace('marker:7b','marker:7'),1))


if __name__=='__main__':unittest.main()
