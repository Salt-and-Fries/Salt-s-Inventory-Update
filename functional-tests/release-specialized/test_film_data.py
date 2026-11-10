import unittest
from film_rename import without_name,plain_name


class FullDataTests(unittest.TestCase):
    def test_legacy_only_name_may_change(self):
        original='{id:"exposure:black_and_white_film",Count:1b,tag:{marker:42,bytes:[B;1b,-2b]}}'
        renamed='{id:"exposure:black_and_white_film",Count:1b,tag:{marker:42,bytes:[B;1b,-2b],display:{Name:\'{"text":"Film"}\'}}}'
        changed='{id:"exposure:black_and_white_film",Count:1b,tag:{marker:43,bytes:[B;1b,-2b],display:{Name:\'{"text":"Film"}\'}}}'
        a,_=without_name(original);b,name=without_name(renamed)
        self.assertEqual(a,b);self.assertEqual(plain_name(name),'Film')
        self.assertNotEqual(a,without_name(changed)[0])

    def test_modern_components_and_count_preserved(self):
        original='{"id":"exposure:black_and_white_film","count":1,"components":{"minecraft:custom_data":{"marker":42}}}'
        renamed='{"id":"exposure:black_and_white_film","count":1,"components":{"minecraft:custom_data":{"marker":42},"minecraft:custom_name":{"text":"Film"}}}'
        a,_=without_name(original);b,name=without_name(renamed)
        self.assertEqual(a,b);self.assertEqual(plain_name(name),'Film')
        self.assertNotEqual(a,without_name(renamed.replace('"count":1','"count":2'))[0])

    def test_unrelated_display_edit_cannot_hide_as_name_change(self):
        a='{id:"test:item",Count:1b,tag:{display:{Name:\'{"text":"A"}\',color:10}}}'
        b='{id:"test:item",Count:1b,tag:{display:{Name:\'{"text":"B"}\',color:11}}}'
        self.assertNotEqual(without_name(a)[0],without_name(b)[0])


if __name__=='__main__':unittest.main()
