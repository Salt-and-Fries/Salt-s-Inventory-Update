"""Typed full-item conservation controls for normal and cosmetic equipment."""
import copy,unittest
from accessories_equipment_features import ledger


class AccessoryDataTests(unittest.TestCase):
    def state(self):
        return dict(inventory=[dict(index=9,item='{id:"minecraft:stick",Count:1b,tag:{release_marker:51,typed:[B;2b,8b],ForgeCaps:{custom:{value:3s}}}}')],
            canonicalCursor='{}',groups=dict(ring=dict(normal=['{}'],cosmetic=['{}'])))

    def test_native_transfer_changes_owner_without_changing_ledger(self):
        state=self.state();moved=copy.deepcopy(state)
        moved['groups']['ring']['cosmetic'][0]=moved['inventory'][0]['item'];moved['inventory'][0]['item']='{}'
        self.assertEqual(ledger(state),ledger(moved))
        moved['canonicalCursor']=moved['groups']['ring']['cosmetic'][0];moved['groups']['ring']['cosmetic'][0]='{}'
        self.assertEqual(ledger(state),ledger(moved))

    def test_duplicate_loss_and_typed_native_metadata_are_rejected(self):
        state=self.state()
        for changed in [state['inventory'][0]['item'].replace('3s','3'),state['inventory'][0]['item'].replace('[B;2b,8b]','[2,8]'),'{}']:
            other=copy.deepcopy(state);other['inventory'][0]['item']=changed
            self.assertNotEqual(ledger(state),ledger(other))
        duplicate=copy.deepcopy(state);duplicate['groups']['ring']['normal'][0]=duplicate['inventory'][0]['item']
        self.assertNotEqual(ledger(state),ledger(duplicate))
        wrong_count=copy.deepcopy(state);wrong_count['inventory'][0]['item']=wrong_count['inventory'][0]['item'].replace('Count:1b','Count:1')
        with self.assertRaises(AssertionError):ledger(wrong_count)

    def test_modern_validation_component_is_preserved(self):
        state=self.state();state['inventory'][0]['item']='{id:"minecraft:feather",count:1,components:{"accessories:slot_validation":{valid_slots:["ring"],invalid_slots:[]},"minecraft:custom_data":{marker:52b}}}'
        other=copy.deepcopy(state);other['inventory'][0]['item']=other['inventory'][0]['item'].replace('"ring"','"head"')
        self.assertNotEqual(ledger(state),ledger(other))


if __name__=='__main__':unittest.main()
