import unittest
from entity_adapter_checks import aircraft_source_preserved,TYPE,CLASS,NATIVE

def pane(indices,vehicle='native-vehicle',native=NATIVE):
    return {'menuType':TYPE,'type':CLASS,'slots':[{'player':False} for _ in indices],
        'nativeAdapter':{'vehicle':vehicle,'originalMenuClass':native,'originalNonplayerSlots':6,'nativeSlotIndices':indices}}

class AircraftSourceTests(unittest.TestCase):
    def test_three_disjoint_panes_preserve_one_native_inventory(self):
        self.assertTrue(aircraft_source_preserved([pane([0,1]),pane([2,3]),pane([4,5])],NATIVE,6))
    def test_one_pane_can_contain_the_complete_native_airship(self):
        self.assertTrue(aircraft_source_preserved([pane(list(range(6)))],NATIVE,6))
    def test_duplicate_native_cells_cannot_hide_missing_slots(self):
        self.assertFalse(aircraft_source_preserved([pane([0,1]),pane([1,2,3,4])],NATIVE,6))
    def test_different_vehicles_cannot_be_combined_into_one_passing_shape(self):
        self.assertFalse(aircraft_source_preserved([pane([0,1]),pane([2,3,4,5],vehicle='other')],NATIVE,6))
    def test_wrong_native_provider_is_not_an_approved_adapter(self):
        self.assertFalse(aircraft_source_preserved([pane(list(range(6)),native='fake.Provider')],NATIVE,6))
    def test_extra_or_absent_cells_and_missing_evidence_fail(self):
        for value in [list(range(5)),list(range(7))]:self.assertFalse(aircraft_source_preserved([pane(value)],NATIVE,6))
        self.assertFalse(aircraft_source_preserved([{'menuType':TYPE,'type':CLASS,'slots':[]}],NATIVE,6))

if __name__=='__main__':unittest.main()
