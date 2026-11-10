import unittest
from native_screen_contracts import dockyard_management

class NativeScreenContractTests(unittest.TestCase):
    screen='com.talhanation.smallships.client.gui.screens.inventory.DockyardScreen'
    def menu(self):return {'class':'com.talhanation.smallships.world.inventory.DockyardMenu','menuType':'smallships:dockyard','slotCount':0}
    def test_real_zero_slot_native_dockyard_is_management_ui(self):
        self.assertTrue(dockyard_management(self.screen,self.menu()))
    def test_a_future_inventory_slot_requires_reclassification(self):
        self.assertFalse(dockyard_management(self.screen,{**self.menu(),'slotCount':1}))
    def test_missing_evidence_is_not_compared_as_two_null_values(self):
        for menu in [{},{'slotCount':0}]:self.assertFalse(dockyard_management(self.screen,menu))
    def test_unrelated_screen_or_menu_is_not_whitelisted(self):
        self.assertFalse(dockyard_management('other.Screen',self.menu()))
        self.assertFalse(dockyard_management(self.screen,{**self.menu(),'menuType':'other:dockyard'}))

if __name__=='__main__':unittest.main()
