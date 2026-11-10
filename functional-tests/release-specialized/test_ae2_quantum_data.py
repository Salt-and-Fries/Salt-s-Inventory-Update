"""Exact native frequency types and complete quantum-item conservation."""
import copy,unittest
import nbtlib
from ae2_quantum_features import expected_item,conserved,has_frequency,DATA,FREQUENCY


class QuantumData(unittest.TestCase):
    def test_original_frequency_and_typed_item_encodings(self):
        old=nbtlib.parse_nbt(expected_item('1.20.1','ae2:quantum_entangled_singularity',1,DATA,True))
        new=nbtlib.parse_nbt(expected_item('1.21.1','ae2:quantum_entangled_singularity',1,DATA,True))
        self.assertIsInstance(old['tag']['freq'],nbtlib.Long);self.assertEqual(int(old['tag']['freq']),FREQUENCY)
        self.assertIsInstance(new['components']['ae2:entangled_singularity_id'],nbtlib.Long)
        self.assertIsInstance(old['tag']['typed'],nbtlib.ByteArray)
        self.assertIsInstance(new['components']['minecraft:custom_data']['typed'],nbtlib.ByteArray)
        self.assertTrue(has_frequency(dict(frequency=0,frequencyJavaType='java.lang.Long'),0))
        self.assertFalse(has_frequency(dict(frequency=False,frequencyJavaType='java.lang.Long'),0))
        self.assertFalse(has_frequency(dict(frequency=float(FREQUENCY),frequencyJavaType='java.lang.Long'),FREQUENCY))
        self.assertFalse(has_frequency(dict(frequency=FREQUENCY,frequencyJavaType='java.lang.Integer'),FREQUENCY))
    def test_conservation_includes_cursor_hidden_cells_and_every_metadata_type(self):
        item=dict(count=1,signature=expected_item('1.20.1','ae2:quantum_entangled_singularity',1,DATA,True))
        empty=dict(count=0,signature='{}');inv=[dict(index=0,**empty)]
        d=dict(inventory=inv,physical={'native-cell':copy.deepcopy(item)},cursor=copy.deepcopy(empty))
        self.assertTrue(conserved(d,inv,stored=item))
        for change in ['frequency','metadata','duplicate','cursor','inventory']:
            bad=copy.deepcopy(d)
            if change=='frequency':bad['physical']['native-cell']['signature']=item['signature'].replace('123456L','123457L')
            if change=='metadata':bad['physical']['native-cell']['signature']=item['signature'].replace('[B; 7B, 1B]','[7B, 1B]')
            if change=='duplicate':bad['physical']['hidden-duplicate']=copy.deepcopy(item)
            if change=='cursor':bad['cursor']=copy.deepcopy(item)
            if change=='inventory':bad['inventory'][0]=dict(index=0,**item)
            self.assertFalse(conserved(bad,inv,stored=item),change)


if __name__=='__main__':unittest.main()
