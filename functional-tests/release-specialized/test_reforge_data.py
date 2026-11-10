"""Reforging may change quality without hiding item/data loss in its oracle."""
import unittest
from reforge_features import without_quality


class ReforgeData(unittest.TestCase):
    def test_legacy_native_quality_change_is_allowed_but_not_damage_or_marker(self):
        before='{id:"minecraft:diamond_sword",Count:1b,tag:{Tiered:{Tier:"tiered:a"},Damage:0,marker:7b}}'
        changed=before.replace('tiered:a','tiered:b')
        self.assertEqual(without_quality(before,'tiered'),without_quality(changed,'tiered'))
        for old,new in [('Count:1b','Count:2b'),('Damage:0','Damage:1'),('marker:7b','marker:7')]:
            self.assertNotEqual(without_quality(before,'tiered'),without_quality(changed.replace(old,new),'tiered'))

    def test_modern_only_registered_tier_component_is_exempted(self):
        before='{id:"minecraft:diamond_sword",count:1,components:{"tiered:tier":{id:"tiered:a"},"minecraft:damage":0,"minecraft:custom_data":{typed:[B;4b,8b]}}}'
        changed=before.replace('tiered:a','tiered:b')
        self.assertEqual(without_quality(before,'tiered'),without_quality(changed,'tiered'))
        self.assertNotEqual(without_quality(before,'tiered'),without_quality(changed.replace('4b,8b','4b,9b'),'tiered'))

    def test_tierify_border_exemption_is_not_given_to_other_publishers(self):
        before='{id:"minecraft:diamond_sword",Count:1b,tag:{Tiered:{Tier:"tiered:a"},itemborders_colors:{top:1,bottom:2},marker:7b}}'
        changed=before.replace('top:1','top:2')
        self.assertEqual(without_quality(before,'tierify'),without_quality(changed,'tierify'))
        self.assertNotEqual(without_quality(before,'tiered'),without_quality(changed,'tiered'))

    def test_wrong_output_item_is_refused(self):
        with self.assertRaises(AssertionError):without_quality('{id:"minecraft:stick",count:1}','tiered')


if __name__=='__main__':unittest.main()
