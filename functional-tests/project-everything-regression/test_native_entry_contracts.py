"""Partner checks use native registry identity across loader mappings."""
import unittest
from check_mod_interactions import native_barrel_menu

class NativeEntryContractTests(unittest.TestCase):
    def test_vanilla_registry_identity_survives_both_class_namespaces(self):
        for name in ['net.minecraft.world.inventory.ChestMenu','net.minecraft.class_1707']:
            self.assertTrue(native_barrel_menu({'type':name,'menuType':'minecraft:generic_9x3'}))
    def test_missing_registry_evidence_is_not_a_pass(self):
        self.assertFalse(native_barrel_menu({'type':'net.minecraft.world.inventory.ChestMenu'}))
    def test_held_item_reopening_is_not_a_barrel(self):
        self.assertFalse(native_barrel_menu({'type':'appeng.menu.me.networktool.NetworkToolMenu','menuType':'ae2:networktool'}))

if __name__=='__main__':unittest.main()
