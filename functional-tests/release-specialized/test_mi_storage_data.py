import copy,unittest
import nbtlib
from mi_storage_features import amount,cell_state,cursor_is,item_variant,physical_item,policies,resources
from mowzie_features import stack,typed


class NativeMachineStorage(unittest.TestCase):
    def test_native_client_resource_data_and_policy_types_must_match_server(self):
        cell='{amount:7L,key:{item:"minecraft:stone",nbt:{typed:[B;3b,6b]}},playerInsert:0b,playerExtract:1b}'
        server=dict(fixture='{items:['+cell+']}');client=dict(cells=dict(items=[dict(index=0,nbt=cell)]))
        self.assertEqual(amount(cell_state(server,client,'items')[0]),7)
        for wrong in [cell.replace('7L','7'),cell.replace('[B;3b,6b]','[3,6]'),cell.replace('playerInsert:0b','playerInsert:1b')]:
            changed=copy.deepcopy(client);changed['cells']['items'][0]['nbt']=wrong
            with self.assertRaises(AssertionError):cell_state(server,changed,'items')
        with self.assertRaises(AssertionError):amount(nbtlib.parse_nbt('{amount:7}'))

    def test_resource_ledger_rejects_hidden_other_cell_creation_and_flag_changes(self):
        original=nbtlib.parse_nbt('[{amount:7L,key:{id:"minecraft:water"},playerExtract:1b},{amount:0L,key:{},playerExtract:1b}]')
        changed=copy.deepcopy(original);changed[1]['amount']=nbtlib.Long(7);changed[1]['key']=original[0]['key']
        self.assertNotEqual(resources(original),resources(changed))
        self.assertEqual(policies(original),policies(changed))
        changed[1]['playerExtract']=nbtlib.Byte(0);self.assertNotEqual(policies(original),policies(changed))

    def test_full_cursor_data_and_hidden_carried_stacks_are_checked(self):
        expected='{id:"minecraft:stone",Count:7b,tag:{typed:[B;3b,6b]}}'
        state=dict(menus=[dict(carried=expected),dict(carried=expected)])
        self.assertTrue(cursor_is(state,expected));self.assertFalse(cursor_is(state))
        changed=copy.deepcopy(state);changed['menus'][1]['carried']=expected.replace('[B;3b,6b]','[3,6]')
        self.assertFalse(cursor_is(changed,expected));self.assertFalse(cursor_is(dict(menus=[])))

    def test_variants_and_physical_stacks_keep_native_typed_metadata(self):
        metadata='{release_mi_output_marker:19,typed:[B;4b,8b]}'
        for mc in ['1.20.1','1.21.1']:
            key=item_variant(mc,'minecraft:stone',metadata);physical=nbtlib.parse_nbt(physical_item(mc,'minecraft:stone',7,metadata))
            a=key['nbt']if mc=='1.20.1'else key['components']['minecraft:custom_data']
            b=physical['tag']if mc=='1.20.1'else physical['components']['minecraft:custom_data']
            self.assertEqual(typed(a),typed(b));self.assertNotEqual(stack(physical.snbt()),stack(physical.snbt().replace('[B;','[')))


if __name__=='__main__':unittest.main()
