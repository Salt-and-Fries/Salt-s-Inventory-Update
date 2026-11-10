"""Reject component loss, duplicated owners and incorrect modern native contracts."""
import copy, hashlib, inspect, unittest
import nbtlib
import powah_legacy_features as original
import powah_modern_features as modern
from powah_modern_observer import enhance
from ae2_quantum_features import expected_item


class ModernPowahData(unittest.TestCase):
    def server(self):
        text = expected_item('1.21.1', 'minecraft:diamond', 7, original.DIAMOND)
        inventory = [dict(index=i, item='{}') for i in range(41)]
        inventory[12]['item'] = text
        return dict(inventory=inventory, world=dict(inventoryHolder=True, items=[dict(index=0, item='{}')]),
                    menus=[], ownedSessions=0, cursor='{}'), modern.base.items([text])

    def test_modern_count_type_and_data_are_never_flattened(self):
        text = expected_item('1.21.1', 'minecraft:diamond', 7, original.DIAMOND)
        self.assertEqual(7, modern.item(text)[0])
        value = nbtlib.parse_nbt(text)
        value['components']['minecraft:custom_data']['typed'] = nbtlib.List[nbtlib.Int]([2, 7])
        self.assertNotEqual(modern.item(text), modern.item(value.snbt()))
        for wrong in [text.replace('count:7', 'count:7b'), text.replace('count:7', 'Count:7b'), text.replace('count:7', 'count:0')]:
            with self.assertRaises(AssertionError):
                modern.item(wrong)

    def test_bound_player_component_has_exact_uuid_name_and_keeps_custom_data(self):
        value = nbtlib.parse_nbt(modern.bound_card('12345678-1234-5678-9abc-def012345678', 'NativePlayer'))
        bound = value['components'].pop('powah:bound_player')
        self.assertIsInstance(bound['gameProfileId'], nbtlib.IntArray)
        self.assertEqual([305419896, 305419896, -1698898192, 305419896], list(map(int, bound['gameProfileId'])))
        self.assertEqual('NativePlayer', str(bound['name']))
        self.assertEqual(modern.item(expected_item('1.21.1', 'powah:binding_card', 1, original.CARD)), modern.item(value.snbt()))

    def test_each_modern_owner_is_counted_once_and_metadata_must_match(self):
        server, expected = self.server()
        text = server['inventory'][12]['item']
        self.assertTrue(modern.base.conserved(server, expected))
        server['inventory'][12]['item'] = '{}'
        server['world']['items'][0]['item'] = text
        server['menus'] = [dict(sameWorldTile=True, cursor='{}', slots=[dict(index=0, containerIndex=0, virtual=False, item=text)])]
        server['ownedSessions'] = 1
        self.assertTrue(modern.base.conserved(server, expected))
        for mutation in ['duplicate', 'menu', 'cursor', 'owner', 'index', 'virtual']:
            data = copy.deepcopy(server)
            if mutation == 'duplicate': data['inventory'][9]['item'] = text
            if mutation == 'menu': data['menus'][0]['slots'][0]['item'] = '{}'
            if mutation == 'cursor': data['cursor'] = text
            if mutation == 'owner': data['menus'][0]['sameWorldTile'] = False
            if mutation == 'index': data['world']['items'][0]['index'] = 1
            if mutation == 'virtual': data['menus'][0]['slots'][0]['virtual'] = 1
            self.assertFalse(modern.base.conserved(data, expected), mutation)
        server['menus'], server['ownedSessions'] = [], 0
        self.assertTrue(modern.base.conserved(server, expected))

    def test_battery_energy_is_long_and_only_expected_field_changes(self):
        full, empty = [nbtlib.parse_nbt(modern.battery(v)) for v in [1000, 0]]
        self.assertIsInstance(full['components']['powah:energy_stored'], nbtlib.Long)
        self.assertEqual(0, int(empty['components']['powah:energy_stored']))
        full['components']['powah:energy_stored'] = nbtlib.Long(0)
        self.assertEqual(modern.item(full.snbt()), modern.item(empty.snbt()))
        for invalid in [True, -1, 1001, 1000.0]:
            with self.assertRaises(AssertionError): modern.battery(invalid)

    def test_original_assertion_methods_and_module_remain_unchanged(self):
        self.assertEqual(modern.LEGACY_SHA256, hashlib.sha256(modern.LEGACY.read_bytes()).hexdigest())
        self.assertIsNot(original, modern.base)
        self.assertIsNot(original.item, modern.item)
        self.assertIsNot(original.next_faces, modern.next_faces)
        for name in ['begin', 'settings', 'face', 'cards', 'solar', 'finish', 'source']:
            self.assertEqual(inspect.getsource(getattr(original.LegacyPowah, name)), inspect.getsource(getattr(modern.ModernPowah, name)))

    def test_native_all_faces_equality_uses_only_available_modern_faces(self):
        before = dict(faces=['ALL', 'NONE', 'EXTRACT', 'EXTRACT', 'EXTRACT', 'EXTRACT'],
                      available=[True, False, True, True, True, True], transfer='ALL')
        self.assertEqual(['EXTRACT', 'NONE', 'RECEIVE', 'RECEIVE', 'RECEIVE', 'RECEIVE'], modern.next_faces(before))
        self.assertEqual(before['faces'], modern.next_faces(before, 1))
        before['available'][1] = 1
        with self.assertRaises(AssertionError): modern.next_faces(before)

    def test_only_declared_frozen_fixture_commands_are_translated(self):
        for tag in [original.DIAMOND, original.CARD, original.BAD]:
            value = 'item replace entity @a container.12 with minecraft:diamond' + tag + ' 7'
            self.assertIn('[minecraft:custom_data=' + tag + ']', modern.command_components(value))
        self.assertEqual('time set 6000', modern.command_components('time set 6000'))
        with self.assertRaises(AssertionError): modern.command_components('say ' + original.DIAMOND)

    def test_observer_uses_exact_native_field_inventory_api_and_read_only_owners(self):
        skeleton = 'class Test {void probe(){if(kind.equals("regression-world"))return true;}}'
        for mc, field, size in [('1.21.1', 'te', 'getSlots'), ('26.1.2', 'blockEntity', 'size')]:
            text = enhance(skeleton, mc, True)
            self.assertIn('field(menu,"' + field + '")', text)
            self.assertIn('call(machine,"' + size + '")', text)
            self.assertIn('hasOpenSessionMatching', text)
            self.assertIn('ItemStack.CODEC.encodeStart', text)
            for forbidden in ['setStored', 'setRedstoneMode', 'setStackInSlot', 'TILE_FIELD', 'INVENTORY_SIZE', 'SUFFIX', '"PLAYERS"']:
                self.assertNotIn(forbidden, text)
        with self.assertRaises(AssertionError): enhance(skeleton, '1.20.1', True)


if __name__ == '__main__':
    unittest.main()
