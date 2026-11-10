import unittest
from native_entries import translate

class EntryCoordinateTests(unittest.TestCase):
    def test_access_point_and_its_wireless_link_move_together(self):
        old=[8,65,8];new=[10,-60,-8]
        setup='setblock 8 65 8 ae2:wireless_access_point'
        item='item replace entity @a weapon.mainhand with test:terminal[ae2:wireless_link_target={dimension:"minecraft:overworld",pos:[I;8,65,8]}]'
        self.assertEqual('setblock 10 -60 -8 ae2:wireless_access_point',translate(setup,old,new))
        self.assertIn('pos:[I;10,-60,-8]',translate(item,old,new))
    def test_unrelated_integer_arrays_and_other_dimensions_are_preserved(self):
        source='give @a test:thing[custom_data={pos:[I;8,65,8]},ae2:wireless_link_target={dimension:"minecraft:the_nether",pos:[I;8,65,8]}]'
        self.assertEqual(source,translate(source,[8,65,8],[10,-60,-8]))
    def test_same_origin_is_byte_identical(self):
        source='give @a test:thing[ae2:wireless_link_target={dimension:"minecraft:overworld",pos:[I;8,65,8]}]'
        self.assertEqual(source,translate(source,[8,65,8],[8,65,8]))

if __name__=='__main__':unittest.main()
