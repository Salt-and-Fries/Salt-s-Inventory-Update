import unittest
from entry_requirements import requirements,missing

class EntryRequirementsTests(unittest.TestCase):
    def test_optional_terminal_inside_component_command_is_required(self):
        actions=[{'type':'command','command':'item replace entity @a weapon.mainhand with merequester:wireless_requester_terminal[ae2:stored_energy=1000000.0d]'}]
        self.assertEqual({'items':['merequester:wireless_requester_terminal']},missing(actions,items=[],blocks=[],entities=[]))
        self.assertEqual({},missing(actions,items=['merequester:wireless_requester_terminal'],blocks=[],entities=[]))
    def test_nested_commands_and_legacy_item_data_are_recognized(self):
        actions=[{'command':'execute as @p run item replace entity @s weapon.mainhand with test:bag{Data:1b} 1'},
                 {'command':'execute at @p run summon test:ship ~ ~ ~ {NoAI:1b}'}]
        result=requirements(actions)
        self.assertEqual({'test:bag'},result['items']);self.assertEqual({'test:ship'},result['entities'])
    def test_block_states_and_relative_positions_do_not_enter_registry_ids(self):
        result=requirements([{'command':'setblock ~ ~-1 ~ ae2:cable_bus[facing=north]'},
            {'command':'fill 1 2 3 4 5 6 minecraft:air replace'}])
        self.assertEqual({'ae2:cable_bus','minecraft:air'},result['blocks'])
    def test_direct_entries_and_give_keep_registry_kinds_separate(self):
        result=requirements([{'item':'test:tool','block':'test:machine','entity':'test:mount'},
            {'command':'give @p test:supply[custom_data={a:1}]'}])
        self.assertEqual({'test:tool','test:supply'},result['items']);self.assertEqual({'test:machine'},result['blocks'])
    def test_item_identifiers_in_arbitrary_nbt_are_not_assumed_requirements(self):
        self.assertEqual({'items':set(),'blocks':set(),'entities':set()},
            requirements([{'command':'data merge block 1 2 3 {Label:"test:tool"}'}]))
    def test_missing_reports_only_absent_ids(self):
        self.assertEqual({'entities':['test:mount']},missing([{'item':'test:tool','entity':'test:mount'}],
            items=['test:tool'],blocks=[],entities=[]))

if __name__=='__main__':unittest.main()
