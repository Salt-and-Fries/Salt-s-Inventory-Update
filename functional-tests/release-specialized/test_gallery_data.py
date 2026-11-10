"""Negative controls for the exact native close-time painting edit."""
import unittest
from gallery_features import without_variant,expected_search


class PaintingDataTests(unittest.TestCase):
    def test_legacy_only_variant_is_exempt(self):
        original='{id:"minecraft:painting",Count:3b,tag:{marker:61,typed:[B;4b,8b]},ForgeCaps:{native:{flag:1b}}}'
        changed=original.replace('marker:61','EntityTag:{variant:"minecraft:kebab"},marker:61')
        self.assertEqual((without_variant(original)[0],'minecraft:kebab'),without_variant(changed))
        self.assertNotEqual(without_variant(original)[0],without_variant(changed.replace('flag:1b','flag:0b'))[0])
        self.assertNotEqual(without_variant(original)[0],without_variant(changed.replace('Count:3b','Count:2b'))[0])
    def test_modern_native_entity_identity_and_variant_only(self):
        original='{id:"minecraft:painting",count:3,components:{"minecraft:custom_data":{marker:61,typed:[B;4b,8b]}}}'
        changed=original.replace('components:{','components:{"minecraft:entity_data":{variant:"minecraft:kebab",id:"minecraft:painting"},')
        self.assertEqual((without_variant(original)[0],'minecraft:kebab'),without_variant(changed))
        self.assertNotEqual(without_variant(original)[0],without_variant(changed.replace('marker:61','marker:62'))[0])
        with self.assertRaises(AssertionError):without_variant(changed.replace('id:"minecraft:painting"},','id:"minecraft:item"},'))
    def test_other_entity_data_cannot_disappear(self):
        before='{id:"minecraft:painting",Count:1b,tag:{EntityTag:{variant:"minecraft:kebab",CustomName:"keep"}}}'
        after='{id:"minecraft:painting",Count:1b,tag:{EntityTag:{variant:"minecraft:aztec"}}}'
        self.assertNotEqual(without_variant(before)[0],without_variant(after)[0])
    def test_search_matches_title_author_or_native_dimensions_in_order(self):
        rows=[dict(id='a',title='Kebab',author='One',dimensions='1 x 1'),dict(id='b',title='Other',author='Kebab',dimensions='2 x 1'),dict(id='c',title='Else',author='Two',dimensions='4 x 4')]
        self.assertEqual(['a','b'],expected_search(rows,'KEBAB'));self.assertEqual(['c'],expected_search(rows,'4 x 4'))
        self.assertEqual([],expected_search(rows,'unmatched'));self.assertEqual(['a','b','c'],expected_search(rows,''))


if __name__=='__main__':unittest.main()
