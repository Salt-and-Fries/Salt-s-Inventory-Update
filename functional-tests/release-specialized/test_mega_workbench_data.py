"""Reject lost metadata, unbound component changes and hidden owned resources."""
import copy,unittest
import nbtlib
from mega_workbench_features import configuration,encoded_components,owned,source_components,SOURCE_FIELDS,CELL_FIELDS,SOURCE,CELL
from ae2_quantum_features import expected_item


def item(name,tag,count=1):return dict(count=count,signature=expected_item('1.21.1',name,1,tag))


def fixture():
    source=item('megacells:portable_cell_workbench',SOURCE);cell=item('ae2:item_storage_cell_1k',CELL);empty=dict(count=0,signature='{}')
    inv=[dict(index=0,**source),dict(index=1,**empty)];actual=copy.deepcopy(source);raw=nbtlib.parse_nbt(actual['signature'])
    fields={'minecraft:container':nbtlib.List[nbtlib.Compound]([nbtlib.Compound(slot=nbtlib.Int(0),item=nbtlib.parse_nbt(cell['signature']))]),'ae2:exported_config_inv':nbtlib.List[nbtlib.Compound]([nbtlib.Compound()for _ in range(63)]),'ae2:exported_settings':nbtlib.Compound(copy_mode=nbtlib.String('CLEAR_ON_REMOVE'))}
    raw['components'].update(fields);actual['signature']=raw.snbt();mutable={k:v.snbt()for k,v in fields.items()}
    cfg=[dict(empty=True)for _ in range(63)]
    native=dict(sourceSlot=0,source=actual,cell=cell,copyMode='CLEAR_ON_REMOVE',fuzzyMode='IGNORE_ALL',cellFuzzy='IGNORE_ALL',config=copy.deepcopy(cfg),sourceConfig=copy.deepcopy(cfg),cellConfig=copy.deepcopy(cfg),upgrades=[],cellUpgrades=[],sourceContained=[cell],sourceSettings={'copy_mode':'CLEAR_ON_REMOVE'},sourceMutable=mutable,cellMutable={})
    data=dict(inventory=[dict(index=0,**actual),dict(index=1,**empty)],cursor=empty,physical={'native-cell':copy.deepcopy(cell)})
    return data,native,inv,source,cell


class PortableWorkbenchData(unittest.TestCase):
    def test_all_partition_rows_require_full_key_exact_java_long_zero_and_no_extras(self):
        key=item('minecraft:stone','{typed:[B;2b,7b]}');values=[dict(empty=True)for _ in range(63)];values[0]=dict(empty=False,item=key,amount=0,amountJavaType='java.lang.Long')
        self.assertTrue(configuration(values,key))
        for change in ['float','bool','java','amount','key','extra','length']:
            bad=copy.deepcopy(values)
            if change=='float':bad[0]['amount']=0.0
            if change=='bool':bad[0]['amount']=False
            if change=='java':bad[0]['amountJavaType']='java.lang.Integer'
            if change=='amount':bad[0]['amount']=1
            if change=='key':bad[0]['item']['signature']=bad[0]['item']['signature'].replace('[B;','[')
            if change=='extra':bad[1]=copy.deepcopy(bad[0])
            if change=='length':bad.pop()
            self.assertFalse(configuration(bad,key),change)
    def test_mutable_components_remain_bound_to_complete_persistent_native_codec_data(self):
        data,native,*_=fixture();self.assertTrue(encoded_components(native['source'],SOURCE_FIELDS,native['sourceMutable']))
        bad=copy.deepcopy(native['sourceMutable']);bad['ae2:exported_settings']='{copy_mode:"KEEP_ON_REMOVE"}'
        self.assertFalse(encoded_components(native['source'],SOURCE_FIELDS,bad))
        bad=copy.deepcopy(native['sourceMutable']);bad.pop('minecraft:container');self.assertFalse(encoded_components(native['source'],SOURCE_FIELDS,bad))
        bad=copy.deepcopy(native['source']);raw=nbtlib.parse_nbt(bad['signature']);raw['components']['minecraft:container'][0]['item']['components']['minecraft:custom_data']['typed']=nbtlib.List[nbtlib.Byte]([5,3]);bad['signature']=raw.snbt()
        self.assertFalse(encoded_components(bad,SOURCE_FIELDS,native['sourceMutable']))
    def test_owned_ledger_rejects_extra_physical_items_cursors_source_or_cell_metadata_loss(self):
        data,native,inv,source,cell=fixture();self.assertTrue(owned(data,native,inv,source,cell,[]))
        for change in ['hidden','cursor','inventory','source','cell','contained','settings','component','source-slot']:
            d=copy.deepcopy(data);n=copy.deepcopy(native)
            if change=='hidden':d['physical']['duplicate']=copy.deepcopy(cell)
            if change=='cursor':d['cursor']=copy.deepcopy(cell)
            if change=='inventory':d['inventory'][1]=copy.deepcopy(cell)
            if change=='source':n['source']['signature']=n['source']['signature'].replace('release_mega_source: 107','release_mega_source: 108')
            if change=='cell':n['cell']['signature']=n['cell']['signature'].replace('[B;5B,3B]','[5B,3B]').replace('[B; 5B, 3B]','[5B,3B]')
            if change=='contained':n['sourceContained'].append(copy.deepcopy(cell))
            if change=='settings':n['sourceSettings']['copy_mode']='KEEP_ON_REMOVE'
            if change=='component':n['sourceMutable']['minecraft:container']='[]'
            if change=='source-slot':n['sourceSlot']=True
            self.assertFalse(owned(d,n,inv,source,cell,[]),change)
    def test_native_source_contents_cannot_be_replaced_by_empty_or_different_owner_settings(self):
        _,native,*_=fixture();self.assertTrue(source_components(native))
        for change in ['container','copy','settings-extra','config']:
            n=copy.deepcopy(native)
            if change=='container':n['sourceMutable'].pop('minecraft:container')
            if change=='copy':n['copyMode']='KEEP_ON_REMOVE'
            if change=='settings-extra':n['sourceSettings']['unreviewed']='value'
            if change=='config':n['sourceConfig'][5]=dict(empty=False,item=item('minecraft:stone','{typed:1b}'),amount=0,amountJavaType='java.lang.Long')
            self.assertFalse(source_components(n),change)


if __name__=='__main__':unittest.main()
