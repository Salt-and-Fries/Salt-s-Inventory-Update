"""Native codec adaptation must reject wrong quantities, keys and fixtures."""
import unittest
import nbtlib as n
from create_codec_1211 import component_contains,fixture_command,schematic_stack


class NativeCreateCodecs(unittest.TestCase):
    def test_native_stack_count_is_measured_not_a_lookalike_field(self):
        self.assertTrue(component_contains('{id:"minecraft:stone",count:64}','Count:64b'))
        self.assertFalse(component_contains('{id:"minecraft:stone",count:63,components:{"minecraft:custom_data":{count:64}}}','Count:64b'))
        self.assertTrue(component_contains('{}','id:"minecraft:air"'))
        self.assertFalse(component_contains('{id:"minecraft:stone",count:1}','id:"minecraft:air"'))
        self.assertFalse(component_contains('{components:{"minecraft:custom_data":{}}}','id:"minecraft:air"'))
    def test_filter_boolean_component_and_native_default(self):
        self.assertTrue(component_contains('{id:"create:filter",count:1,components:{"create:filter_items_blacklist":1b}}','Blacklist:1b'))
        self.assertFalse(component_contains('{id:"create:filter",count:1}','Blacklist:1b'))
        self.assertTrue(component_contains('{id:"create:filter",count:1}','Blacklist:0b'))
        self.assertFalse(component_contains('{id:"minecraft:stone",count:1}','Blacklist:0b'))
    def test_attribute_enum_uses_the_registered_component(self):
        self.assertTrue(component_contains('{id:"create:attribute_filter",count:1,components:{"create:attribute_filter_whitelist_mode":"whitelist_conj"}}','WhitelistMode:1'))
        self.assertFalse(component_contains('{id:"create:attribute_filter",count:1,components:{"create:attribute_filter_whitelist_mode":"blacklist"}}','WhitelistMode:1'))
        self.assertTrue(component_contains('{id:"create:attribute_filter",count:1}','WhitelistMode:0'))
        self.assertFalse(component_contains('{id:"minecraft:stone",count:1}','WhitelistMode:0'))
    def test_controller_requires_twelve_distinct_real_references(self):
        values=[f'{{slot:{i},item:{{id:"minecraft:stone",count:1}}}}'for i in range(12)]
        def data(items):return '{id:"create:linked_controller",count:1,components:{"create:linked_controller_items":['+','.join(items)+']}}'
        self.assertTrue(component_contains(data(values),'Size:12'))
        self.assertFalse(component_contains(data(values[:-1]),'Size:12'))
        self.assertFalse(component_contains(data(values[:-1]+[values[0]]),'Size:12'))
    def test_address_is_not_satisfied_by_unrelated_custom_data(self):
        self.assertTrue(component_contains('{id:"create:package_filter",count:1,components:{"create:package_address":"Foundry"}}','Address:"Foundry"'))
        self.assertFalse(component_contains('{id:"create:package_filter",count:1,components:{"minecraft:custom_data":{Address:"Foundry"}}}','Address:"Foundry"'))
        self.assertTrue(component_contains('{id:"create:package_filter",count:1}','Address:""'))
    def test_chest_fixture_preserves_native_quantity_and_typed_data(self):
        command='setblock 8 65 5 minecraft:chest{Items:[{Slot:0b,id:"minecraft:stone",Count:64b,tag:{marker:42L}}]}'
        native=fixture_command(command);v=n.parse_nbt(native[native.index('{'):])['Items'][0]
        self.assertEqual(int(v['count']),64);self.assertEqual(v['Slot'],n.Byte(0))
        self.assertIsInstance(v['components']['minecraft:custom_data']['marker'],n.Long)
        self.assertNotIn('Count',v)
    def test_category_filter_uses_native_container_component(self):
        command='data merge block 8 65 8 {Categories:[{Count:1b,id:"create:filter",tag:{Blacklist:0b,Items:{Items:[{Count:1b,Slot:0,id:"minecraft:stone"}],Size:18},RespectNBT:0b}}]}'
        native=fixture_command(command);v=n.parse_nbt(native[native.index('{'):])['Categories'][0]
        item=v['components']['create:filter_items'][0]
        self.assertEqual(int(item['slot']),0);self.assertEqual(str(item['item']['id']),'minecraft:stone')
        self.assertNotIn('Slot',item['item']);self.assertNotIn('tag',v)
    def test_unreviewed_legacy_mod_item_and_command_are_refused(self):
        with self.assertRaises(AssertionError):fixture_command('data merge block 8 65 8 {Items:[{Count:1b,id:"minecraft:stone"}]}')
        with self.assertRaises(AssertionError):fixture_command('setblock 8 65 5 minecraft:chest{Items:[{id:"create:filter",Count:1b,tag:{unknown:1}}]}')
    def test_schematic_filename_is_a_real_native_component(self):
        good='{id:"create:schematic",count:1,components:{"create:schematic_file":"siu-create-functional-stone.nbt"}}'
        self.assertTrue(component_contains(good,'File:"siu-create-functional-stone.nbt"'))
        self.assertFalse(component_contains(good.replace('create:schematic_file','minecraft:custom_data'),'File:"siu-create-functional-stone.nbt"'))
        self.assertFalse(component_contains(good.replace('id:"create:schematic"','id:"minecraft:stone"'),'File:"siu-create-functional-stone.nbt"'))
    def test_cannon_options_require_the_native_record_and_types(self):
        good='{Options:{replace_mode:3,skip_missing:1b,replace_block_entities:0b}}'
        self.assertTrue(component_contains(good,'ReplaceMode:3'))
        self.assertTrue(component_contains(good,'SkipMissing:1b'))
        self.assertFalse(component_contains(good.replace('replace_mode:3','replace_mode:2'),'ReplaceMode:3'))
        self.assertFalse(component_contains(good.replace('skip_missing:1b','skip_missing:0b'),'SkipMissing:1b'))
        self.assertFalse(component_contains('{ReplaceMode:3,SkipMissing:1b}','ReplaceMode:3'))
        self.assertFalse(component_contains(good.replace('Options','lookalike'),'SkipMissing:1b'))
    def test_deployed_schematic_fixture_preserves_native_geometry_and_identity(self):
        old='{Anchor:{X:12,Y:65,Z:4},Bounds:[1,1,16],Deployed:1b,File:"siu-create-functional-line.nbt",Mirror:"NONE",Owner:"UIAudit",Rotation:"NONE"}'
        value=n.parse_nbt(schematic_stack(old));components=value['components']
        self.assertEqual(str(value['id']),'create:schematic');self.assertEqual(value['count'],n.Int(1))
        self.assertEqual(list(map(int,components['create:schematic_anchor'])),[12,65,4])
        self.assertIsInstance(components['create:schematic_anchor'],n.IntArray)
        self.assertEqual(list(map(int,components['create:schematic_bounds'])),[1,1,16])
        self.assertEqual(str(components['create:schematic_mirror']),'none')
        self.assertEqual(str(components['create:schematic_owner']),'UIAudit')
        self.assertNotIn('minecraft:custom_data',components)
        with self.assertRaises(AssertionError):schematic_stack(old.replace('Bounds:[1,1,16]','Bounds:[1,1,0]'))
        with self.assertRaises(AssertionError):schematic_stack(old.replace('X:12','X:12.5d'))
        with self.assertRaises(AssertionError):schematic_stack(old.replace('Mirror:"NONE"','Mirror:"OTHER"'))


if __name__=='__main__':unittest.main()
