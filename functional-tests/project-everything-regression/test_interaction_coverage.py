"""Interaction coverage requires an actually successful transfer scenario."""
import unittest
from collect_results import interaction_pair

class InteractionCoverageTests(unittest.TestCase):
    def test_old_capture_prefix_cannot_own_a_later_held_menu_interaction(self):
        self.assertEqual(('botania','minecraft'),interaction_pair({'status':'passed','menu_ids':['botania:hand_of_ender'],
            'actions':[{'type':'fixture','block':'spawn:pigment_shifter'},
                       {'type':'client-use-block'}, {'type':'close'},
                       {'type':'held','item':'botania:hand_of_ender'}, {'type':'client-use-item'}],
            'cross_mod_partner':{'block':'minecraft:barrel'}}))
    def test_attached_entity_is_the_source_after_an_unrelated_setup_block(self):
        self.assertEqual(('railways','minecraft'),interaction_pair({'status':'passed','menu_ids':['create:toolbox'],
            'actions':[{'type':'fixture','block':'create:depot'},
                       {'type':'client-use-entity','entity':'railways:conductor'}],
            'cross_mod_partner':{'block':'minecraft:barrel'}}))
    def test_co_loaded_mods_and_entry_only_do_not_prove_interaction(self):
        self.assertIsNone(interaction_pair({'status':'passed','menu_ids':['ae2:terminal']}))
        self.assertIsNone(interaction_pair({'status':'entry-and-policy-only','menu_ids':['ae2:terminal'],
                                           'cross_mod_partner':{'block':'netherchested:nether_chest'}}))
        self.assertIsNone(interaction_pair({'status':'failed','menu_ids':['ae2:terminal'],
                                           'cross_mod_partner':{'block':'netherchested:nether_chest'}}))
    def test_passed_source_and_partner_are_recorded_in_their_actual_direction(self):
        self.assertEqual(('ae2','netherchested'),interaction_pair({'status':'passed','menu_ids':['ae2:terminal'],
                         'cross_mod_partner':{'block':'netherchested:nether_chest'}}))
    def test_unregistered_native_menu_uses_its_real_entry_owner(self):
        self.assertEqual(('quark','minecraft'),interaction_pair({'status':'passed','menu_ids':[None],
                         'actions':[{'type':'fixture','block':'quark:crate'}],
                         'cross_mod_partner':{'block':'minecraft:barrel'}}))
    def test_addon_entry_keeps_its_owner_when_using_a_base_mod_menu(self):
        self.assertEqual(('megacells','netherchested'),interaction_pair({'status':'passed','menu_ids':['ae2:portable_cell'],
                         'actions':[{'type':'command','command':'item replace entity @a weapon.mainhand with megacells:portable_cell'}],
                         'cross_mod_partner':{'block':'netherchested:nether_chest'}}))
    def test_native_quantity_scenario_with_historical_string_partner(self):
        self.assertEqual(('wands','minecraft'),interaction_pair({'status':'passed','menu_ids':['wands:magic_bag'],
                         'cross_mod_partner':'minecraft:barrel'}))
        self.assertIsNone(interaction_pair({'status':'passed','cross_mod_partner':None}))

if __name__=='__main__':unittest.main()
