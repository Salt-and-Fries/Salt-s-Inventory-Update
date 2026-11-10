"""Negative controls for original Powah5 values and full native item ownership."""
import copy,unittest
import nbtlib
from powah_legacy_features import item,items,bound_card,conserved,next_transfer,next_faces,settings_equal,ray_side,DIAMOND,CARD
from ae2_quantum_features import expected_item


class PowahLegacyData(unittest.TestCase):
    def server(self):
        text=expected_item('1.20.1','minecraft:diamond',7,DIAMOND);inventory=[dict(index=i,item='{}')for i in range(41)];inventory[12]['item']=text
        world=dict(inventoryHolder=True,items=[dict(index=0,item='{}')])
        return dict(inventory=inventory,world=world,menus=[],ownedSessions=0,cursor='{}'),items([text])
    def test_full_typed_inventory_cursor_world_and_view_are_counted_once(self):
        server,expected=self.server();self.assertTrue(conserved(server,expected));text=server['inventory'][12]['item']
        server['inventory'][12]['item']='{}';server['world']['items'][0]['item']=text
        server['menus']=[dict(sameWorldTile=True,cursor='{}',slots=[dict(index=0,containerIndex=0,virtual=False,item=text)])];server['ownedSessions']=1
        self.assertTrue(conserved(server,expected))
        for changed in ['duplicate','metadata','view','mirror','owner','index','virtual','boolean-count']:
            data=copy.deepcopy(server)
            if changed=='duplicate':data['inventory'][13]['item']=text
            if changed=='metadata':
                value=nbtlib.parse_nbt(text);value['tag']['typed']=nbtlib.List[nbtlib.Int]([2,7])
                data['world']['items'][0]['item']=value.snbt();data['menus'][0]['slots'][0]['item']=value.snbt()
            if changed=='view':data['menus'][0]['slots'][0]['item']='{}'
            if changed=='mirror':data['menus'][0]['cursor']=text
            if changed=='owner':data['menus'][0]['sameWorldTile']=False
            if changed=='index':data['inventory'][40]['index']=39
            if changed=='virtual':data['menus'][0]['slots'][0]['virtual']=1
            if changed=='boolean-count':data['ownedSessions']=True
            self.assertFalse(conserved(data,expected),changed)
        server['menus']=[];server['ownedSessions']=0;self.assertTrue(conserved(server,expected),'Closed source remains owned by native world tile')
    def test_only_original_binding_uuid_and_name_are_added_to_full_card(self):
        player='12345678-1234-5678-9abc-def012345678';value=nbtlib.parse_nbt(bound_card(player,'NativePlayer'))
        self.assertEqual([305419896,305419896,-1698898192,305419896],list(map(int,value['tag']['bound_player_id'])))
        self.assertIsInstance(value['tag']['bound_player_id'],nbtlib.IntArray);self.assertIsInstance(value['tag']['bound_player_name'],nbtlib.String)
        del value['tag']['bound_player_id'];del value['tag']['bound_player_name']
        self.assertEqual(item(expected_item('1.20.1','powah:binding_card',1,CARD)),item(value.snbt()))
    def test_exact_native_direction_cycles_and_face_applicability(self):
        self.assertEqual(['EXTRACT','RECEIVE','NONE','ALL'],[next_transfer(v,'ALL')for v in ['ALL','EXTRACT','RECEIVE','NONE']])
        self.assertEqual('NONE',next_transfer('EXTRACT','EXTRACT'));self.assertEqual('RECEIVE',next_transfer('NONE','RECEIVE'))
        before=dict(faces=['ALL']*6,available=[True]*6,transfer='ALL')
        self.assertEqual(['ALL']*5+['EXTRACT'],next_faces(before,5));self.assertEqual(['EXTRACT']*6,next_faces(before))
        before['available'][5]=False;self.assertEqual(['ALL']*6,next_faces(before,5))
        before['available'][5]=1
        with self.assertRaises(AssertionError):next_faces(before,5)
    def test_typed_native_values_cannot_be_replaced_by_truthy_numbers_or_desynced_states(self):
        value=dict(faces=['ALL']*6,available=[True]*6,transfer='ALL',redstone='IGNORE',stored=0,capacity=1000)
        self.assertTrue(settings_equal(value,value))
        for key,replacement in [('stored',False),('capacity',1000.0),('available',[1]*6),('redstone','UNKNOWN'),('faces',['NONE']*6)]:
            altered=copy.deepcopy(value);altered[key]=replacement;self.assertFalse(settings_equal(value,altered),key)
        self.assertEqual('east',ray_side(dict(position=[8,65,8],hit=[8.85,65.5,8.59])))
        self.assertEqual('south',ray_side(dict(position=[8,65,8],hit=[8.5,65.5,8.85])))
        self.assertIsNone(ray_side(dict(position=[8,65,8],hit=[8.5,65.5,8.5])))


if __name__=='__main__':unittest.main()
