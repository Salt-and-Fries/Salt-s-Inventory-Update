import unittest
from entity_inventory_cases import cases


class EntityInventoryCaseTests(unittest.TestCase):
    def inputs(self,**changes):
        return dict(dict(mc='1.21.11',loader='fabric',installed={'guardvillagers'},
            items={'guardvillagers:guard_spawn_egg'},menus={'guardvillagers:guard_screen'},
            origin=[12,-60,10],hashes={'actual-publisher'},entities={'guardvillagers:guard'},
            gaps=[dict(minecraft='1.21.11',loader='fabric',ui='guard-villager',
                screen='native.GuardScreen',publisher_sha256=['actual-publisher'])]),**changes)

    def test_requires_exact_platform_and_captured_publisher(self):
        self.assertEqual(1,len(cases(**self.inputs())))
        for changes in [dict(mc='26.3'),dict(loader='neoforge'),dict(hashes={'other'})]:
            self.assertEqual([],cases(**self.inputs(**changes)))

    def test_missing_registry_or_mod_does_not_create_a_fake_case(self):
        for changes in [dict(installed=set()),dict(items=set()),dict(menus=set()),dict(entities=set())]:
            self.assertEqual([],cases(**self.inputs(**changes)))

    def test_entry_keeps_ai_and_hero_permissions_and_uses_ordinary_input(self):
        row=cases(**self.inputs())[0]
        commands=[a['command'] for a in row['actions'] if a['type']=='command']
        self.assertTrue(any('hero_of_the_village' in v for v in commands))
        self.assertTrue(any('NoAI:0b' in v for v in commands))
        self.assertEqual({'type':'client-use-entity','entity':'guardvillagers:guard','shift':False,'ticks':20},row['actions'][-1])
        self.assertFalse(any(a['type'] in {'provider','open','profile'} for a in row['actions']))

    def test_all_world_coordinates_follow_actual_origin(self):
        commands=[a['command'] for a in cases(**self.inputs())[0]['actions'] if a['type']=='command']
        self.assertIn('summon guardvillagers:guard 12.5 -60 10.5 {NoAI:0b,NoGravity:1b,Invulnerable:1b,PersistenceRequired:1b}',commands)
        self.assertIn('tp @a 12.5 -60 13.5 180 5',commands)

    def test_version_specific_attribute_and_readonly_planning_registry(self):
        args=self.inputs(mc='1.20.1',entities=None)
        args['gaps']=[dict(args['gaps'][0],minecraft='1.20.1')]
        commands=[a['command'] for a in cases(**args)[0]['actions'] if a['type']=='command']
        self.assertTrue(any('minecraft:generic.movement_speed' in v for v in commands))
        self.assertTrue(any('minecraft:movement_speed' in a.get('command','') for a in cases(**self.inputs())[0]['actions']))


if __name__=='__main__':unittest.main()
