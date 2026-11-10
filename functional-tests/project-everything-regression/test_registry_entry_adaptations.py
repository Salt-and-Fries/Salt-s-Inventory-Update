import unittest
from registry_entry_adaptations import adapt


class RegistryEntryAdaptationTests(unittest.TestCase):
    def case(self):return {'id':'atlas-waystones-modifiers','expected_native_menu':'net.blay09.mods.waystones.menu.WaystoneModifierMenu','actions':[{'type':'fixture','block':'waystones:waystone'},{'type':'regression-native-widget','message':'Manage Modifiers'}]}
    def test_exact_supported_rename_preserves_source_and_remaining_actions(self):
        case=self.case();actions,proof=adapt(case,'26.1.2','fabric',{'waystones:andesite_waystone'})
        self.assertEqual('waystones:andesite_waystone',actions[0]['block']);self.assertIsNotNone(proof)
        self.assertEqual('waystones:waystone',case['actions'][0]['block']);self.assertEqual(case['actions'][1],actions[1])
    def test_existing_original_id_is_never_substituted(self):
        case=self.case();actions,proof=adapt(case,'26.1.2','fabric',{'waystones:waystone','waystones:andesite_waystone'})
        self.assertEqual(case['actions'],actions);self.assertIsNone(proof)
    def test_missing_replacement_still_requires_a_real_entry(self):
        case=self.case();actions,proof=adapt(case,'26.1.2','fabric',set())
        self.assertEqual(case['actions'],actions);self.assertIsNone(proof)
    def test_other_versions_loaders_and_interfaces_are_unchanged(self):
        for mc,loader in [('26.2','fabric'),('26.1.2','neoforge'),('1.21.1','fabric')]:
            case=self.case();actions,proof=adapt(case,mc,loader,{'waystones:andesite_waystone'})
            self.assertEqual(case['actions'],actions);self.assertIsNone(proof)
        case=self.case();case['id']='unrelated-waystone';actions,proof=adapt(case,'26.1.2','fabric',{'waystones:andesite_waystone'})
        self.assertEqual(case['actions'],actions);self.assertIsNone(proof)


if __name__=='__main__':unittest.main()
