import unittest
from nifty_inventory_cases import cases

class NiftyCases(unittest.TestCase):
    def inputs(self,**changes):
        return dict(dict(mc='1.20.1',loader='fabric',installed={'niftycarts'},items=set(),menus=set(),origin=(8,65,8),
            entities={'niftycarts:plow','niftycarts:seed_drill'},hashes={'native-pin'},
            gaps=[dict(minecraft='1.20.1',loader='fabric',ui='niftycarts-'+kind,
                       screen='native.'+kind,publisher_sha256=['native-pin']) for kind in ['plow','seed-drill']]),**changes)
    def test_both_native_cart_menus_require_matching_publishers_and_entity_registry(self):
        rows=cases(**self.inputs());self.assertEqual(2,len(rows))
        self.assertTrue(all(any(a['type']=='client-use-entity' and a['shift'] for a in row['actions']) for row in rows))
        self.assertTrue(all('create-menu' not in str(row) for row in rows))
        self.assertEqual([],cases(**self.inputs(hashes={'different-release'})))
        self.assertEqual([],cases(**self.inputs(installed=set())))
        self.assertEqual(1,len(cases(**self.inputs(entities={'niftycarts:plow'}))))
    def test_wrong_target_does_not_inherit_an_old_capture(self):
        self.assertEqual([],cases(**self.inputs(mc='1.21.11')))

if __name__=='__main__':unittest.main()
