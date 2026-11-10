"""Check that the remote ledger cannot hide changed NBT types or nested values."""
import importlib.util
from pathlib import Path
import unittest
import nbtlib

spec=importlib.util.spec_from_file_location('release_remote_ledger',Path(__file__).with_name('run.py'))
ledger=importlib.util.module_from_spec(spec);spec.loader.exec_module(ledger)

class LedgerTest(unittest.TestCase):
    def test_saved_player_identity_is_the_native_offline_uuid_not_an_arbitrary_dat_file(self):
        self.assertEqual('ce700ba2-9155-302c-99e0-8cb62fd36a82',str(ledger.offline_uuid('SIUPlayerA')))
        self.assertEqual('88df844b-7898-35cd-b629-7f5698797316',str(ledger.offline_uuid('SIUPlayerB')))

    def test_compound_order_is_irrelevant_but_every_native_type_and_value_matters(self):
        a=nbtlib.parse_nbt('{a:1,b:[I;1,2,3],nested:{x:2b,y:"text"}}')
        b=nbtlib.parse_nbt('{nested:{y:"text",x:2b},b:[I;1,2,3],a:1}')
        self.assertEqual(ledger.typed_nbt(a),ledger.typed_nbt(b))
        for text in ['{a:1b,b:[I;1,2,3],nested:{x:2b,y:"text"}}',
                     '{a:1,b:[1,2,3],nested:{x:2b,y:"text"}}',
                     '{a:1,b:[I;1,2,4],nested:{x:2b,y:"text"}}',
                     '{a:1,b:[I;1,2,3],nested:{x:2b,y:"changed"}}']:
            self.assertNotEqual(ledger.typed_nbt(a),ledger.typed_nbt(nbtlib.parse_nbt(text)))

    def test_authoritative_count_and_full_component_fingerprint_are_distinct(self):
        tag=nbtlib.parse_nbt('[{Slot:9b,id:"minecraft:diamond",count:32,components:{"minecraft:custom_data":{values:[I;1,2,3]}}}]')
        item=ledger.native_items(tag)[0]
        self.assertEqual(32,item['count'])
        self.assertEqual([1,2,3],item['components']['minecraft:custom_data']['values'])
        self.assertIn('IntArray',str(item['_native_components']))
        self.assertEqual({'minecraft:diamond':32},ledger.Campaign.counts([item]))

if __name__=='__main__':unittest.main()
