"""XP-pouch data oracle negative controls; no live gameplay claim."""
import unittest
from pouch_features import pouch_item


class PouchData(unittest.TestCase):
    def test_only_stored_xp_is_allowed_to_change(self):
        before = '{id:"archon:experience_pouch",Count:1b,tag:{archon:{xp:0},marker:{typed:7b,label:"owner"}}}'
        after = before.replace('xp:0', 'xp:50')
        a, a_data = pouch_item(before); b, b_data = pouch_item(after)
        self.assertEqual((a, b), (0, 50)); self.assertEqual(a_data, b_data)

    def test_unrelated_numeric_type_or_item_edit_is_detected(self):
        native = '{id:"archon:experience_pouch",Count:1b,tag:{archon:{xp:50},marker:{typed:7b}}}'
        self.assertNotEqual(pouch_item(native)[1], pouch_item(native.replace('typed:7b', 'typed:7'))[1])
        self.assertNotEqual(pouch_item(native)[1], pouch_item(native.replace('archon:experience_pouch', 'archon:super_experience_pouch'))[1])

    def test_invalid_native_pouch_or_xp_schema_is_refused(self):
        native = '{id:"archon:experience_pouch",Count:1b,tag:{archon:{xp:50}}}'
        for bad in [native.replace('Count:1b', 'Count:2b'), native.replace('xp:50', 'xp:-1'),
                    native.replace('xp:50', 'xp:50b'), native.replace('archon:experience_pouch', 'minecraft:stone')]:
            with self.subTest(bad=bad), self.assertRaises(AssertionError): pouch_item(bad)


if __name__ == '__main__': unittest.main()
