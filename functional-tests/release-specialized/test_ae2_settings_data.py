"""Reject cross-session values, coercive types and hidden owned-item changes."""
import copy,unittest
from ae2_settings_features import ledger,pair,number


class NativeSettingsData(unittest.TestCase):
    def test_native_menu_and_session_must_both_match(self):
        p=dict(client=[dict(session=4,menu='appeng.menu.PriorityMenu')],server=[dict(session=4,menu='appeng.menu.PriorityMenu')])
        self.assertEqual(pair(p,'PriorityMenu')[0]['session'],4)
        for field,value in [('session',5),('menu','appeng.menu.StorageBusMenu')]:
            bad=copy.deepcopy(p);bad['server'][0][field]=value
            with self.assertRaises(AssertionError):pair(bad,'PriorityMenu')
        p['client'].append(copy.deepcopy(p['client'][0]))
        with self.assertRaises(AssertionError):pair(p,'PriorityMenu')
    def test_numeric_witness_rejects_wrong_types_and_wrong_exact_value(self):
        p=dict(values=dict(threshold=dict(javaType='java.lang.Long',value=41)))
        self.assertTrue(number(p,'threshold',41,'java.lang.Long'))
        for key,value in [('javaType','java.lang.Double'),('value',41.0),('value',True),('value',40)]:
            bad=copy.deepcopy(p);bad['values']['threshold'][key]=value;self.assertFalse(number(bad,'threshold',41,'java.lang.Long'))
    def test_full_owned_ledger_rejects_hidden_resource_and_nbt_changes(self):
        item=dict(item='minecraft:diamond',count=7,signature='{id:"minecraft:diamond",Count:1b,tag:{typed:[B;2b,8b]}}')
        p=dict(inventory=[dict(index=12,**item)],physical={},cursor=dict(count=0,signature='{}'))
        baseline=ledger(p)
        for action in ['inventory','metadata','physical','cursor']:
            bad=copy.deepcopy(p)
            if action=='inventory':bad['inventory'][0]['count']=6
            if action=='metadata':bad['inventory'][0]['signature']=item['signature'].replace('[B;2b,8b]','[2b,8b]')
            if action=='physical':bad['physical']['hidden']=copy.deepcopy(item)
            if action=='cursor':bad['cursor']=copy.deepcopy(item)
            self.assertNotEqual(ledger(bad),baseline,action)


if __name__=='__main__':unittest.main()
