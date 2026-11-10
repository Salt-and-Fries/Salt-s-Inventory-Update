import copy
import unittest
from inventory_ledger import after_seed,tagged_total,with_closed_source

class InventoryLedgerTests(unittest.TestCase):
    def state(self):
        return {'physical':{},'inventory':[{'item':'minecraft:snow','count':6,'signature':'exact marker42'}],
                'cursor':{'item':'minecraft:air','count':0,'signature':'empty'}}
    def test_preexisting_initialized_fuel_remains_in_the_exact_metadata_ledger(self):
        before=self.state();after=copy.deepcopy(before)
        after['physical']['native/input']={'item':'minecraft:snow','count':7,'signature':'exact marker42'}
        self.assertEqual(13,after_seed(before,'minecraft:snow','exact marker42',7))
        self.assertEqual(13,tagged_total(after,'minecraft:snow','exact marker42'))
    def test_empty_baseline_still_requires_exact_added_quantity(self):
        before=self.state();before['inventory']=[]
        self.assertEqual(7,after_seed(before,'minecraft:snow','exact marker42',7))
    def test_same_item_with_changed_data_is_not_credited(self):
        state=self.state();state['physical']['native/input']={'item':'minecraft:snow','count':7,'signature':'modified marker'}
        self.assertEqual(6,tagged_total(state,'minecraft:snow','exact marker42'))
        self.assertNotEqual(after_seed(self.state(),'minecraft:snow','exact marker42',7),tagged_total(state,'minecraft:snow','exact marker42'))
    def test_cursor_and_native_cells_are_counted_without_mutating_observation(self):
        state=self.state();state['cursor']={'item':'minecraft:snow','count':1,'signature':'exact marker42'}
        original=copy.deepcopy(state)
        self.assertEqual(7,tagged_total(state,'minecraft:snow','exact marker42'))
        self.assertEqual(original,state)
    def test_negative_seed_is_rejected(self):
        with self.assertRaises(ValueError):after_seed(self.state(),'minecraft:snow','exact marker42',-1)
    def test_closed_world_source_stock_remains_owned_without_mutating_snapshot(self):
        state=self.state();state['menus']=[];original=copy.deepcopy(state)
        combined=with_closed_source(state,7,[{'index':0,'item':'minecraft:snow','count':1,'signature':'exact marker42'}])
        self.assertEqual(7,tagged_total(combined,'minecraft:snow','exact marker42'))
        self.assertEqual(original,state)
    def test_open_source_cannot_be_counted_twice(self):
        state=self.state();state['menus']=[{'session':7}]
        with self.assertRaises(ValueError):with_closed_source(state,7,[])
    def test_repeated_closed_source_accounting_is_rejected(self):
        state=self.state();state['menus']=[];cells=[{'index':0,'item':'minecraft:snow','count':1,'signature':'exact marker42'}]
        combined=with_closed_source(state,7,cells)
        with self.assertRaises(ValueError):with_closed_source(combined,7,cells)

if __name__=='__main__':unittest.main()
