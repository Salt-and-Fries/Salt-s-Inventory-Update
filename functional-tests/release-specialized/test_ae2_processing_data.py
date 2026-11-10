"""Reject clipped key data, false numeric identities and counterfeit encodings."""
import copy,unittest
from ae2_processing_features import amount_cell,array_matches,decoded_matches
from ae2_quantum_features import conserved


def key(name='stone'):
    return dict(count=1,signature='{id:"minecraft:'+name+'",count:1,components:{"minecraft:custom_data":{typed:[B;1b,7b]}}}')


def cell(amount,name='stone'):
    return dict(empty=False,item=key(name),amount=amount,amountJavaType='java.lang.Long')


class NativeProcessingData(unittest.TestCase):
    def test_ghost_amount_and_array_require_exact_native_key_amount_and_empty_remainder(self):
        values=[cell(37),dict(empty=True)];self.assertTrue(array_matches(values,key(),37))
        for change in ['metadata','bool','float','java','count','extra','empty']:
            bad=copy.deepcopy(values)
            if change=='metadata':bad[0]['item']['signature']=key()['signature'].replace('[B;1b,7b]','[1b,7b]')
            if change=='bool':bad[0]['amount']=True
            if change=='float':bad[0]['amount']=37.0
            if change=='java':bad[0]['amountJavaType']='java.lang.Integer'
            if change=='count':bad[0]['item']['count']=True
            if change=='extra':bad[1]=cell(5)
            if change=='empty':bad[1]['empty']=1
            self.assertFalse(array_matches(bad,key(),37),change)
        self.assertTrue(amount_cell(dict(empty=True),key(),None))
        self.assertFalse(amount_cell(dict(empty=1),key(),None))
    def test_real_native_encoding_separates_unit_input_key_from_multiplier(self):
        value=dict(type='appeng.crafting.pattern.AEProcessingPattern',inputs=[dict(multiplier=37,alternatives=[cell(1)])],outputs=[cell(5,'diamond')])
        self.assertTrue(decoded_matches(value,key(),key('diamond')))
        for change in ['type','multiplier','unit','output','metadata','extra']:
            bad=copy.deepcopy(value)
            if change=='type':bad['type']='appeng.crafting.pattern.AECraftingPattern'
            if change=='multiplier':bad['inputs'][0]['multiplier']=37.0
            if change=='unit':bad['inputs'][0]['alternatives'][0]['amount']=37
            if change=='output':bad['outputs'][0]['amount']=4
            if change=='metadata':bad['outputs'][0]['item']['signature']=key('diamond')['signature'].replace('7b','8b')
            if change=='extra':bad['inputs'].append(copy.deepcopy(bad['inputs'][0]))
            self.assertFalse(decoded_matches(bad,key(),key('diamond')),change)
    def test_blank_to_encoded_resource_ledger_rejects_loss_duplicates_and_cursor_leaks(self):
        inv=[dict(index=11,count=0,signature='{}'),dict(index=12,**key('diamond'))]
        encoded=dict(count=1,signature='{id:"ae2:processing_pattern",count:1,components:{"minecraft:custom_data":{amount:37L}}}')
        data=dict(inventory=copy.deepcopy(inv),cursor=dict(count=0,signature='{}'),physical={'output':copy.deepcopy(encoded)})
        self.assertTrue(conserved(data,inv,stored=encoded))
        for change in ['duplicate','loss','cursor','metadata','inventory']:
            bad=copy.deepcopy(data)
            if change=='duplicate':bad['physical']['hidden']=copy.deepcopy(encoded)
            if change=='loss':bad['physical']={}
            if change=='cursor':bad['cursor']=encoded
            if change=='metadata':bad['physical']['output']['signature']=encoded['signature'].replace('37L','37')
            if change=='inventory':bad['inventory'][1]['count']=2
            self.assertFalse(conserved(bad,inv,stored=encoded),change)


if __name__=='__main__':unittest.main()
