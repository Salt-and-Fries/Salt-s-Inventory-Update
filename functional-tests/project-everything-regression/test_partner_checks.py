"""The orchestration must obey a locked host and preserve existing storage."""
import copy,json,unittest
from partner_checks import open_partner,same_locked_host

def menu(sid,kind,key):
    return {'session':sid,'type':kind,'menuType':'test:'+kind,'sourceKey':key,
            'slots':[{'player':False,'virtual':False,'count':0}]}

class Protocol:
    def __init__(self,locked=True,leak=False,wrong_source=False,main_hand_only=False,main_hand_intercepted=False):
        self.source=menu(1,'HeldMenu','item:original');self.menus=[self.source];self.locked=locked
        self.leak=leak;self.wrong_source=wrong_source;self.pending='none';self.next_id=2;self.actions=[];self.resident=7
        self.main_hand_only=main_hand_only
        self.main_hand_intercepted=main_hand_intercepted
    def snapshot(self,item='minecraft:diamond'):
        result=copy.deepcopy({'candidate':item,'menus':self.menus,'cursor':{'count':0,'item':'minecraft:air'},
            'inputPolicy':{'selectedSourceLocked':self.locked,'mainHand':{'item':'test:bag','count':1,'signature':'host-data'},
                           'serverRejectsLockedBlockUse':True,'pendingBlockTarget':self.pending}})
        for m in result['menus']:
            if m['type']=='StorageMenu':m['slots'][0]['mayPlace']=item!='create:cardboard_package_12x12'
        return result
    def act(self,kind,**kw):
        self.actions.append((kind,kw))
        if kind=='regression-close':
            self.menus=[m for m in self.menus if m['session']!=kw['session']];self.locked=False
        elif kind=='regression-place':self.resident=0
        elif kind=='regression-open':
            if self.locked:
                if self.leak:self.pending='block:leaked'
                return
            if self.main_hand_only and not kw.get('allowHeldMainHand'):return
            if self.main_hand_intercepted and kw.get('allowHeldMainHand'):return
            self.menus.append(menu(self.next_id,'StorageMenu','block:storage'));self.next_id+=1
        elif kind=='client-use-item':
            self.menus.append(menu(self.next_id,'HeldMenu','block:wrong' if self.wrong_source else 'item:original'));self.next_id+=1;self.locked=True
        elif kind=='regression-select-empty-hand':
            self.locked=False;self.main_hand_intercepted=False
        else:raise AssertionError('Unexpected protocol command '+kind)

def expect(label,condition,**detail):
    if not condition:raise AssertionError(label)

class PartnerOrderingTests(unittest.TestCase):
    def test_locked_powered_host_allows_only_native_energy_drain(self):
        def host(energy=100, **components):
            return dict(item='ae2:wireless_crafting_terminal',count=1,signature=json.dumps(dict(
                id='ae2:wireless_crafting_terminal',count=1,components={
                    'ae2:stored_energy':energy,'ae2:crafting_inv':[],
                    'ae2:wireless_link_target':{'pos':[8,65,8]},**components})))
        self.assertTrue(same_locked_host(host(),host(99)))
        self.assertFalse(same_locked_host(host(),host(101)))
        self.assertFalse(same_locked_host(host(),host(0)))
        self.assertFalse(same_locked_host(host(),host(float('nan'))))
        self.assertFalse(same_locked_host(host(),host(99,**{'ae2:crafting_inv':[{'id':'minecraft:diamond'}]})))
        self.assertFalse(same_locked_host(host(),host(99,**{'ae2:wireless_link_target':{'pos':[9,65,8]}})))
        changed=host(99);changed['count']=2
        self.assertFalse(same_locked_host(host(),changed))
        changed=host(99);changed['item']='test:other'
        self.assertFalse(same_locked_host(host(),changed))
        self.assertFalse(same_locked_host(dict(item='test:other',signature='a'),dict(item='test:other',signature='b')))
    def test_main_hand_only_storage_opens_after_the_host_lock_is_released(self):
        p=Protocol(main_hand_only=True)
        state,sid,other=open_partner({'actions':[{'type':'client-use-item'}]},p.snapshot(),1,
            'sophisticatedstorage:barrel',p.act,expect,p.snapshot)
        self.assertNotEqual(sid,other['session'])
        self.assertTrue(state['inputPolicy']['selectedSourceLocked'])
    def test_other_legacy_partners_keep_the_path_that_avoids_a_held_tool_interceptor(self):
        state,sid,other=self.run_case(Protocol(main_hand_intercepted=True))
        self.assertNotEqual(sid,other['session'])
    def run_case(self,p,**kwargs):
        return open_partner({'actions':[{'type':'client-use-item'}]},p.snapshot(),1,'test:storage',p.act,expect,p.snapshot,**kwargs)
    def test_locked_host_is_closed_before_allowed_world_use_and_reopened_afterwards(self):
        p=Protocol();state,sid,other=self.run_case(p)
        self.assertNotEqual(1,sid);self.assertNotEqual(other['session'],sid);self.assertTrue(state['inputPolicy']['selectedSourceLocked'])
        kinds=[k for k,_ in p.actions]
        self.assertLess(kinds.index('regression-close'),len(kinds)-1)
        self.assertEqual('client-use-item',kinds[-1])
    def test_unlocked_subject_is_never_closed_or_reopened(self):
        p=Protocol(locked=False);state,sid,other=self.run_case(p)
        self.assertEqual(1,sid);self.assertNotIn('regression-close',[k for k,_ in p.actions]);self.assertNotIn('client-use-item',[k for k,_ in p.actions])
    def test_reusing_populated_storage_never_replaces_its_block(self):
        p=Protocol();state,sid,other=self.run_case(p,place=False)
        self.assertEqual(7,p.resident);self.assertNotIn('regression-place',[k for k,_ in p.actions])
    def test_a_different_reopened_source_cannot_count_as_the_original_bag(self):
        with self.assertRaisesRegex(AssertionError,'same native source identity'):self.run_case(Protocol(wrong_source=True))
    def test_denied_packet_leaking_a_block_target_is_a_failure(self):
        with self.assertRaisesRegex(AssertionError,'stale block identity'):self.run_case(Protocol(leak=True))
    def test_blacklisted_package_policy_is_not_replaced_by_accepted_diamond_policy(self):
        p=Protocol(locked=False);item='create:cardboard_package_12x12'
        state,sid,other=open_partner({'actions':[]},p.snapshot(item),1,'test:storage',p.act,expect,p.snapshot)
        self.assertEqual(item,state['candidate']);self.assertFalse(other['slots'][0]['mayPlace'])
        self.assertTrue(p.snapshot('minecraft:diamond')['menus'][-1]['slots'][0]['mayPlace'])
    def test_held_source_reopening_preserves_the_actual_partner_policy_candidate(self):
        p=Protocol();item='create:cardboard_package_12x12'
        state,sid,other=open_partner({'actions':[{'type':'client-use-item'}]},p.snapshot(item),1,
            'test:storage',p.act,expect,p.snapshot)
        self.assertNotEqual(1,sid);self.assertEqual(item,state['candidate'])
        self.assertFalse(other['slots'][0]['mayPlace'])
    def test_unlocked_network_tool_still_selects_an_empty_hand_before_block_use(self):
        p=Protocol(locked=False,main_hand_only=True,main_hand_intercepted=True)
        p.source['type']='appeng.menu.me.networktool.NetworkToolMenu'
        state,sid,other=open_partner({'actions':[]},p.snapshot(),1,'sophisticatedstorage:barrel',p.act,expect,p.snapshot)
        self.assertEqual(1,sid);self.assertEqual('StorageMenu',other['type'])
        self.assertEqual('regression-select-empty-hand',p.actions[0][0])
        self.assertIn(p.source['sourceKey'],[m['sourceKey'] for m in state['menus']])

if __name__=='__main__':unittest.main()
