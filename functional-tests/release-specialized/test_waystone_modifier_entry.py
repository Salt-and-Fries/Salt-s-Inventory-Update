"""Pure entry-correction controls, not Minecraft acceptance."""
import copy,unittest
from waystone_modifier_entry import adapt,TARGETS,CASE,MENU,SCREEN,OLD,NEW


class WaystoneEntryTests(unittest.TestCase):
    def case(self):return dict(id=CASE,expected_native_menu=MENU,expected_native_screen=SCREEN,expected_nonplayer_slot_count=5,
        actions=[dict(type='fixture',block=OLD,ticks=20),dict(type='regression-native-widget',message='Manage Modifiers',parent_menu='net.blay09.mods.waystones.menu.WaystoneEditMenu',ticks=3)])
    def test_only_the_registered_block_prerequisite_changes(self):
        for target in TARGETS:
            case=self.case();original=copy.deepcopy(case);actions,proof=adapt(case,*target.split('/'),{NEW})
            self.assertEqual(case,original);self.assertEqual(actions[1],original['actions'][1]);self.assertEqual(actions[0],dict(type='fixture',block=NEW,ticks=20));self.assertEqual(proof['previous_block'],OLD)
    def test_unknown_target_or_native_contract_is_rejected(self):
        for key,value in [('id','another-case'),('expected_native_menu','another-menu'),('expected_native_screen','another-screen'),('expected_nonplayer_slot_count',4)]:
            case=self.case();case[key]=value
            with self.assertRaises(AssertionError):adapt(case,'26.1.2','neoforge',{NEW})
        with self.assertRaises(AssertionError):adapt(self.case(),'1.21.1','neoforge',{NEW})
    def test_missing_ambiguous_or_still_old_registry_is_rejected(self):
        for blocks in [set(),{OLD},{OLD,NEW}]:
            with self.assertRaises(AssertionError):adapt(self.case(),'26.1.2','neoforge',blocks)
        for change in ['duplicate','unknown-button']:
            case=self.case()
            if change=='duplicate':case['actions'].append(copy.deepcopy(case['actions'][0]))
            else:case['actions'][1]['message']='other action'
            with self.assertRaises(AssertionError):adapt(case,'26.1.2','neoforge',{NEW})


if __name__=='__main__':unittest.main()
