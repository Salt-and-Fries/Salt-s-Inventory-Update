import unittest
from ae2_fixture import normalize

class Ae2FixtureTests(unittest.TestCase):
    def seed(self,part='terminal'):
        return 'setblock 8 65 8 ae2:cable_bus{cable:{id:"ae2:fluix_glass_cable"},south:{id:"ae2:'+part+'"}}'
    def test_all_three_native_terminal_parts_get_explicit_native_key_types(self):
        for part in ['terminal','crafting_terminal','pattern_encoding_terminal']:
            self.assertIn('enabledKeyTypes:["ae2:i","ae2:f"]',normalize(self.seed(part),'26.3'))
    def test_legacy_seed_is_preserved(self):
        self.assertEqual(self.seed(),normalize(self.seed(),'1.20.1'))
    def test_explicit_filter_is_never_changed(self):
        command=self.seed().replace('id:"ae2:terminal"','id:"ae2:terminal",enabledKeyTypes:["ae2:f"]')
        self.assertEqual(command,normalize(command,'1.21.1'))
    def test_normalization_is_idempotent(self):
        once=normalize(self.seed(),'26.1.2')
        self.assertEqual(once,normalize(once,'26.1.2'))
    def test_other_blocks_and_non_setup_commands_are_preserved(self):
        for command in [self.seed().replace('ae2:cable_bus{','minecraft:chest{'),
                        self.seed().replace('setblock 8 65 8','data merge block 8 65 8')]:
            self.assertEqual(command,normalize(command,'26.3'))
    def test_other_parts_and_custom_part_nbt_are_preserved(self):
        for command in [self.seed('pattern_access_terminal'),self.seed().replace('id:"ae2:terminal"','id:"ae2:terminal",custom:1b')]:
            self.assertEqual(command,normalize(command,'26.3'))
    def test_execute_setup_and_other_faces_are_supported(self):
        command='execute as @a run '+self.seed().replace('south:', 'north:')
        self.assertIn('enabledKeyTypes:',normalize(command,'1.21.11'))

if __name__=='__main__':unittest.main()
