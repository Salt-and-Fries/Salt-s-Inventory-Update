"""Require the actual capability identity and complete physical equipment data."""
import copy,unittest
from blue_arcs_features import owned


def fixture():
    empty=dict(count=0,signature='{}');arc=dict(count=1,signature='{id:"blue_skies:aquatic_arc",Count:1b,tag:{typed:[B;2b,9b]},ForgeCaps:{native_marker:{value:137L}}}')
    equipped=[copy.deepcopy(arc)]+[copy.deepcopy(empty)for _ in range(3)];inventory=[dict(index=9,**empty)]
    slots=[dict(index=36+i,containerIndex=i,sameCapability=True,item=copy.deepcopy(v))for i,v in enumerate(equipped)]
    native=dict(client=[dict(session=7,menu='com.legacy.blue_skies.client.gui.menu.ArcsMenu')],server=[dict(session=7,menu='com.legacy.blue_skies.client.gui.menu.ArcsMenu',slots=slots)],capability=copy.deepcopy(equipped))
    data=dict(inventory=copy.deepcopy(inventory),cursor=copy.deepcopy(empty),physical={'arc':copy.deepcopy(arc)},menus=[dict(session=7,type='com.legacy.blue_skies.client.gui.menu.ArcsMenu',slots=[dict(index=36+i,type='com.legacy.blue_skies.client.gui.menu.ArcSlot',player=False,**v)for i,v in enumerate(equipped)])])
    return data,native,inventory,equipped


class NativeArcsData(unittest.TestCase):
    def test_full_typed_capability_item_requires_original_order_and_native_owner(self):
        data,native,inv,equipped=fixture();self.assertTrue(owned(data,native,inv,equipped))
        for change in ['owner','order','session','session-type','slot-count','cap-count']:
            n=copy.deepcopy(native)
            if change=='owner':n['server'][0]['slots'][0]['sameCapability']=False
            if change=='order':n['server'][0]['slots'][0]['containerIndex']=1
            if change=='session':n['client'][0]['session']=8
            if change=='session-type':n['client'][0]['session']=True
            if change=='slot-count':n['server'][0]['slots'].pop()
            if change=='cap-count':n['capability'].pop()
            self.assertFalse(owned(data,n,inv,equipped),change)
    def test_capability_metadata_and_forge_capabilities_cannot_disappear(self):
        data,native,inv,equipped=fixture()
        for change in ['typed-array','long','count','capability-copy']:
            n=copy.deepcopy(native)
            if change=='typed-array':n['capability'][0]['signature']=n['capability'][0]['signature'].replace('[B;','[')
            if change=='long':n['capability'][0]['signature']=n['capability'][0]['signature'].replace('137L','137')
            if change=='count':n['capability'][0]['count']=2
            if change=='capability-copy':n['server'][0]['slots'][0]['item']['signature']=n['capability'][0]['signature'].replace('native_marker','other_marker')
            self.assertFalse(owned(data,n,inv,equipped),change)
    def test_visible_menu_data_must_match_same_actual_capability_slots(self):
        data,native,inv,equipped=fixture()
        for change in ['menu','slot','item','player']:
            d=copy.deepcopy(data)
            if change=='menu':d['menus'][0]['session']=8
            if change=='slot':d['menus'][0]['slots'][0]['index']=37
            if change=='item':d['menus'][0]['slots'][0]['count']=0
            if change=='player':d['menus'][0]['slots'][0]['player']=True
            self.assertFalse(owned(d,native,inv,equipped),change)
    def test_no_hidden_owned_items_cursor_or_unrelated_inventory_mutation(self):
        data,native,inv,equipped=fixture()
        for change in ['hidden','cursor','inventory']:
            d=copy.deepcopy(data)
            if change=='hidden':d['physical']['duplicate']=copy.deepcopy(equipped[0])
            if change=='cursor':d['cursor']=copy.deepcopy(equipped[0])
            if change=='inventory':d['inventory'][0]=dict(index=9,**equipped[0])
            self.assertFalse(owned(d,native,inv,equipped),change)


if __name__=='__main__':unittest.main()
