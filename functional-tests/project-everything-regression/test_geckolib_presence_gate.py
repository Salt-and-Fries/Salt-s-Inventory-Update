import unittest
from geckolib_checks import applicable


class GeckoLibPresenceGateTests(unittest.TestCase):
    menu=['net.orcinus.galosphere.client.gui.CombustionTableMenu']
    def test_galosphere_without_optional_geckolib_needs_no_comparator_probe(self):
        self.assertFalse(applicable('1.20.1','fabric',{'galosphere'},self.menu))
        self.assertTrue(applicable('1.20.1','fabric',{'galosphere','geckolib'},self.menu))
    def test_forge_and_components_versions_do_not_claim_legacy_fabric_comparisons(self):
        self.assertFalse(applicable('1.20.1','forge',{'galosphere','geckolib'},self.menu))
        self.assertFalse(applicable('1.21.1','fabric',{'galosphere','geckolib'},self.menu))
    def test_loading_the_library_does_not_mean_the_menu_was_exercised(self):
        self.assertFalse(applicable('1.20.1','fabric',{'galosphere','geckolib'},['native.OtherMenu']))


if __name__=='__main__':unittest.main()
