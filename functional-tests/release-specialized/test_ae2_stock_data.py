"""Keep native inaccessible copies distinct from complete owned-item ledgers."""
import copy,unittest
from ae2_stock_features import config_matches,owned,valid_config


def key():return dict(count=1,signature='{id:"minecraft:stone",count:1,components:{"minecraft:custom_data":{typed:[B;4b,6b]}}}')


def configured():return dict(empty=False,item=key(),amount=37,amountJavaType='java.lang.Long')


class NativeStockData(unittest.TestCase):
    def test_native_stock_config_requires_exact_key_types_amount_and_no_real_storage(self):
        p=dict(config=[configured(),dict(empty=True)],storage=[dict(empty=True)])
        self.assertTrue(config_matches(p,key(),37))
        for change in ['metadata','float','bool','java','extra','storage','empty-type']:
            bad=copy.deepcopy(p)
            if change=='metadata':bad['config'][0]['item']['signature']=key()['signature'].replace('[B;4b,6b]','[4b,6b]')
            if change=='float':bad['config'][0]['amount']=37.0
            if change=='bool':bad['config'][0]['amount']=True
            if change=='java':bad['config'][0]['amountJavaType']='java.lang.Integer'
            if change=='extra':bad['config'][1]=configured()
            if change=='storage':bad['storage'][0]=configured()
            if change=='empty-type':bad['storage'][0]['empty']=1
            self.assertFalse(config_matches(bad,key(),37),change)
    def test_dialog_copy_is_exact_declared_inaccessible_slot_not_a_hidden_owned_item(self):
        raw=dict(count=1,signature='{id:"ae2:wrapped_generic_stack",count:1,components:{"minecraft:custom_data":{number:37L}}}')
        mirror=dict(session=5,slot=0,raw=raw,decoded=configured(),mayPlace=False,mayPickup=False)
        inv=[dict(index=12,**key())];p=dict(inventory=inv,cursor=dict(count=0,signature='{}'),physical={'mirror':copy.deepcopy(raw)},
            menus=[dict(session=5,type='appeng.menu.implementations.SetStockAmountMenu',slots=[dict(index=0,type='appeng.menu.slot.InaccessibleSlot',player=False,**raw)])])
        native=dict(mirrors=[mirror]);self.assertTrue(owned(p,native,inv,key(),37))
        for change in ['extra','wrong-session','raw','cursor','inventory','pickup','amount']:
            bad=copy.deepcopy(p);n=copy.deepcopy(native)
            if change=='extra':bad['physical']['hidden-owned']=copy.deepcopy(raw)
            if change=='wrong-session':n['mirrors'][0]['session']=6
            if change=='raw':bad['menus'][0]['slots'][0]['count']=2
            if change=='cursor':bad['cursor']=key()
            if change=='inventory':bad['inventory'][0]['signature']=key()['signature'].replace('[B;4b,6b]','[4b,6b]')
            if change=='pickup':n['mirrors'][0]['mayPickup']=True
            if change=='amount':n['mirrors'][0]['decoded']['amount']=36
            self.assertFalse(owned(bad,n,inv,key(),37),change)
    def test_parent_has_no_copy_allowance_and_client_amount_types_are_checked(self):
        inv=[dict(index=9,**key())];p=dict(inventory=inv,cursor=dict(count=0,signature='{}'),physical={},menus=[])
        self.assertTrue(owned(p,dict(mirrors=[]),inv,key()))
        p['physical']['hidden']=key();self.assertFalse(owned(p,dict(mirrors=[]),inv,key()))
        for value in [True,37.0,0,-1]:
            item=configured();item['amount']=value;self.assertFalse(valid_config([item]))


if __name__=='__main__':unittest.main()
