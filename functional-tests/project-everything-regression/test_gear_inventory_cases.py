import unittest
from gear_inventory_cases import cases

class GearCases(unittest.TestCase):
    def inputs(self,**changes):
        return dict(dict(mc='1.20.1',loader='fabric',installed={'smitherz'},items=set(),
            menus={'smitherz:smither','smitherz:grinder'},origin=(8,65,8),hashes={'publisher'},
            gaps=[dict(minecraft='1.20.1',loader='fabric',ui='smitherz-'+kind,
                screen='net.smitherz.screen.'+kind,publisher_sha256=['publisher']) for kind in ['smither','grinder']]),**changes)
    def test_native_menu_registry_publisher_and_target_are_all_required(self):
        rows=cases(**self.inputs());self.assertEqual(2,len(rows))
        self.assertEqual(['minecraft:smithing_table','minecraft:grindstone'],[r['actions'][0]['block'] for r in rows])
        self.assertEqual([],cases(**self.inputs(hashes={'other'})))
        self.assertEqual([],cases(**self.inputs(mc='1.21.1')))
        self.assertEqual([],cases(**self.inputs(menus=set())))
        self.assertEqual(1,len(cases(**self.inputs(menus={'smitherz:smither'}))))
    def test_child_transition_uses_live_region_not_a_provider_or_old_screen_pixels(self):
        action=cases(**self.inputs())[0]['actions'][-1]
        self.assertEqual('regression-template-region-tap',action['type'])
        self.assertEqual('tabs',action['region'])
        self.assertEqual((37,-12),(action['source_x'],action['source_y']))
        self.assertNotIn('x',action);self.assertNotIn('y',action)

if __name__=='__main__':unittest.main()
