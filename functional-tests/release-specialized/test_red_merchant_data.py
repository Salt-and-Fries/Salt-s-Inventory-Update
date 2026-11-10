import unittest
from red_merchant_features import plain_emerald,inventory,empty_cursors


class MerchantDataTests(unittest.TestCase):
    def test_only_plain_native_currency_is_used_for_command_fixture(self):
        self.assertTrue(plain_emerald('{id:"minecraft:emerald",Count:7b}'))
        self.assertTrue(plain_emerald('{id:"minecraft:emerald",count:7,components:{}}'))
        self.assertFalse(plain_emerald('{id:"minecraft:emerald",Count:7b,tag:{special:1b}}'))
        self.assertFalse(plain_emerald('{id:"minecraft:emerald",Count:7b,ForgeCaps:{cap:{special:1b}}}'))
        self.assertFalse(plain_emerald('{id:"minecraft:diamond",Count:7b}'))
        self.assertFalse(plain_emerald('{id:"minecraft:emerald",Count:62b}'))
    def test_wrong_native_count_type_is_refused(self):
        with self.assertRaises(AssertionError):plain_emerald('{id:"minecraft:emerald",Count:7}')
    def test_inventory_and_cursor_preserve_full_native_data(self):
        a=dict(inventory=[dict(item='{id:"minecraft:diamond",Count:3b,tag:{typed:[B;1b,5b]}}')],cursors=[dict(item='{}')])
        b=dict(inventory=[dict(item='{id:"minecraft:diamond",Count:3b,tag:{typed:[I;1,5]}}')],cursors=[dict(item='{id:"minecraft:diamond",Count:1b}')])
        self.assertNotEqual(inventory(a),inventory(b));self.assertTrue(empty_cursors(a));self.assertFalse(empty_cursors(b))


if __name__=='__main__':unittest.main()
