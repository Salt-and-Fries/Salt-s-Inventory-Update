"""Reject wrong native decoded fixtures and misleading remote-record agreement."""
import copy,unittest
from ae2_pattern_features import decoded_fixture,synchronized


class NativePatternData(unittest.TestCase):
    def test_native_decoder_and_exact_item_requirements(self):
        p=dict(item=dict(item='ae2:processing_pattern',count=1),inputMultiplier=1,
            decodedInput=dict(id='minecraft:stone',javaType='java.lang.Long',amount=1),
            decodedOutput=dict(id='minecraft:diamond',javaType='java.lang.Long',amount=1))
        self.assertTrue(decoded_fixture(p))
        for path,value in [('item.item','ae2:blank_pattern'),('item.count',2),('item.count',True),('inputMultiplier',True),('inputMultiplier',1.0),
                           ('decodedInput.id','minecraft:diamond'),('decodedOutput.id','minecraft:stone'),('decodedInput.javaType','java.lang.Double'),
                           ('decodedOutput.amount',True),('decodedOutput.amount',1.0),('decodedInput.amount',2)]:
            bad=copy.deepcopy(p);parts=path.split('.');parent=bad
            for key in parts[:-1]:parent=parent[key]
            parent[parts[-1]]=value;self.assertFalse(decoded_fixture(bad),(path,value))
    def test_client_server_and_actual_provider_full_typed_records_must_agree(self):
        item=dict(count=1,signature='{id:"ae2:processing_pattern",count:1,components:{"minecraft:custom_data":{typed:[B;3b,8b]}}}')
        record=dict(serverId=-9001,items=[copy.deepcopy(item)])
        meta=dict(session=4,menu='appeng.menu.implementations.PatternAccessTermMenu')
        p=dict(client=[dict(**meta,records=[copy.deepcopy(record)],shownProviders='VISIBLE')],
            server=[dict(**meta,records=[copy.deepcopy(record)],shownProviders='VISIBLE')],provider=[copy.deepcopy(item)])
        self.assertTrue(synchronized(p,meta))
        for change in ['session','menu','server-id','float-id','server-float-id','metadata','provider','duplicate','filter']:
            bad=copy.deepcopy(p)
            if change=='session':bad['server'][0]['session']=5
            if change=='menu':bad['server'][0]['menu']='appeng.menu.implementations.PatternProviderMenu'
            if change=='server-id':bad['server'][0]['records'][0]['serverId']=-9002
            if change=='float-id':bad['client'][0]['records'][0]['serverId']=-9001.0
            if change=='server-float-id':bad['server'][0]['records'][0]['serverId']=-9001.0
            if change=='metadata':bad['client'][0]['records'][0]['items'][0]['signature']=item['signature'].replace('[B;3b,8b]','[3b,8b]')
            if change=='provider':bad['provider'].append(copy.deepcopy(item))
            if change=='duplicate':bad['client'][0]['records'].append(copy.deepcopy(record))
            if change=='filter':bad['server'][0]['shownProviders']='ALL'
            self.assertFalse(synchronized(bad,meta),change)


if __name__=='__main__':unittest.main()
