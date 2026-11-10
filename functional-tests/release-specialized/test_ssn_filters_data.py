"""Negative controls for exact, nonconsuming native ghost-filter observations."""
import copy,unittest
from ssn_filters_features import exact_ghosts,physical_inventory,no_cursor


class GhostDataTests(unittest.TestCase):
    def setUp(self):
        self.original='{id:"minecraft:diamond",Count:16b,tag:{marker:41,typed:[B;3b,6b]},ForgeCaps:{native_cap:{active:1b}}}'
        self.copy=self.original.replace('Count:16b','Count:17b')
        self.state=dict(settings=dict(ghosts=[self.copy,'{}']),inventory=[dict(item=self.original)],cursors=[dict(item='{}')])
    def test_exact_full_native_copy_and_count(self):
        self.assertTrue(exact_ghosts(self.state,self.original,[17]))
    def test_wrong_count_or_changed_capability_rejected(self):
        self.assertFalse(exact_ghosts(self.state,self.original,[16]))
        changed=copy.deepcopy(self.state);changed['settings']['ghosts'][0]=self.copy.replace('active:1b','active:0b')
        self.assertFalse(exact_ghosts(changed,self.original,[17]))
    def test_unexpected_additional_ghost_rejected(self):
        changed=copy.deepcopy(self.state);changed['settings']['ghosts'][1]=self.copy
        self.assertFalse(exact_ghosts(changed,self.original,[17]))
    def test_physical_ledger_preserves_types_and_detects_cursor(self):
        changed=copy.deepcopy(self.state);changed['inventory'][0]['item']=self.original.replace('typed:[B;3b,6b]','typed:[I;3,6]')
        self.assertNotEqual(physical_inventory(self.state),physical_inventory(changed))
        self.assertTrue(no_cursor(self.state));changed['cursors'][0]['item']=self.original
        self.assertFalse(no_cursor(changed))


if __name__=='__main__':unittest.main()
