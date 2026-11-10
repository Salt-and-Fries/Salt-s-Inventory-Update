import unittest
from bumblezone_checks import stored_bees

class BeeData(unittest.TestCase):
    def test_both_publisher_storage_formats_preserve_health_age_and_flags(self):
        legacy='{id:"the_bumblezone:buzzing_briefcase",Count:1b,tag:{BeesStored:[{Health:4.0f,Age:-12000,HasStung:1b,HasNectar:0b,UUID:[I;1,2,3,4]}]}}'
        modern='{"id":"the_bumblezone:buzzing_briefcase","count":1,"components":{"the_bumblezone:buzzing_briefcase_data":{"BeesStored":[{"Health":4.0,"Age":-12000,"HasStung":1,"HasNectar":0,"UUID":[1,2,3,4]}]}}}'
        self.assertEqual(stored_bees(legacy),stored_bees(modern));self.assertEqual([1,2,3,4],stored_bees(legacy)[0]['UUID'])
        self.assertEqual([],stored_bees('{id:"the_bumblezone:buzzing_briefcase",Count:1b}'))
        self.assertEqual([],stored_bees('{"components":{"the_bumblezone:buzzing_briefcase_data":{"BeesStored":[]}}}'))
    def test_unrelated_components_are_never_mistaken_for_stored_bees(self):
        self.assertEqual([],stored_bees('{"components":{"other:bees":{"BeesStored":[{}]}}}'))

if __name__=='__main__':unittest.main()
