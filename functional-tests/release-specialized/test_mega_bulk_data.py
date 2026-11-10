"""Negative oracles for single native partitions, cutoff types and complete ownership."""
import copy,unittest
import nbtlib
from mega_bulk_features import cutoff,owned,cell_configuration,plain,SOURCE,CELL,CELL_FIELDS
from mega_workbench_features import SOURCE_FIELDS
from test_mega_workbench_data import item


def fixture():
    source=item('megacells:portable_cell_workbench',SOURCE);cell=item('megacells:bulk_item_cell',CELL);empty=dict(count=0,signature='{}')
    inv=[dict(index=0,**source),dict(index=1,**empty)];cfg=[dict(empty=True)for _ in range(63)]
    fields={'minecraft:container':nbtlib.List[nbtlib.Compound]([nbtlib.Compound(slot=nbtlib.Int(0),item=nbtlib.parse_nbt(cell['signature']))]),'ae2:exported_config_inv':nbtlib.List[nbtlib.Compound]([nbtlib.Compound()for _ in range(63)]),'ae2:exported_settings':nbtlib.Compound(copy_mode=nbtlib.String('CLEAR_ON_REMOVE'))}
    raw=nbtlib.parse_nbt(source['signature']);raw['components'].update(fields);actual=dict(count=1,signature=raw.snbt())
    n=dict(source=actual,sourceSlot=0,cell=cell,sourceMutable={k:v.snbt()for k,v in fields.items()},cellMutable={},copyMode='CLEAR_ON_REMOVE',fuzzyMode='IGNORE_ALL',cellFuzzy='IGNORE_ALL',config=cfg,cellConfig=[dict(empty=True)],sourceConfig=copy.deepcopy(cfg),sourceSettings={'copy_mode':'CLEAR_ON_REMOVE'},sourceContained=[cell],upgrades=[],cellUpgrades=[],bulk={'variants':[]})
    data=dict(inventory=[dict(index=0,**actual),dict(index=1,**empty)],cursor=empty,physical={'native-cell':copy.deepcopy(cell)})
    return data,n,inv,source,cell


def chain():
    return [dict(item=dict(count=count,signature=plain(name)),unitFactor=units)for name,count,units in [('iron_nugget',1,'1'),('iron_ingot',9,'9'),('iron_block',9,'81')]]


class BulkControlData(unittest.TestCase):
    def test_native_single_partition_rejects_padded_rows_metadata_loss_and_wrong_numeric_type(self):
        key=dict(count=5,signature=plain('iron_nugget'));value=dict(empty=False,item=dict(count=1,signature=plain('iron_nugget')),amount=0,amountJavaType='java.lang.Long')
        self.assertTrue(cell_configuration([value],key));self.assertTrue(cell_configuration([dict(empty=True)],None))
        for change in ['padded','float','bool','java','nonzero','key','count','empty']:
            values=[copy.deepcopy(value)]
            if change=='padded':values.extend(dict(empty=True)for _ in range(62))
            if change=='float':values[0]['amount']=0.0
            if change=='bool':values[0]['amount']=False
            if change=='java':values[0]['amountJavaType']='java.lang.Integer'
            if change=='nonzero':values[0]['amount']=1
            if change=='key':values[0]['item']['signature']='{id:"minecraft:iron_nugget",count:1,components:{"minecraft:custom_data":{typed:1b}}}'
            if change=='count':values[0]['item']['count']=5
            if change=='empty':values[0]['empty']=1
            self.assertFalse(cell_configuration(values,key),change)
    def test_cutoff_requires_raw_native_integer_and_exact_recipe_variants_units_and_counts(self):
        _,n,*_=fixture();key=dict(count=5,signature=plain('iron_nugget'));n['bulk']={'variants':chain(),'BULK_CELL_COMPRESSION_CUTOFF':{'javaType':'java.lang.Integer','value':'-1'}};n['cellMutable']['megacells:compression_cutoff']='-1'
        self.assertTrue(cutoff(n,-1,key))
        for change in ['float','java','sentinel','variant','units','quantity','extra','mutable','stored','count','factor']:
            bad=copy.deepcopy(n)
            if change=='float':bad['bulk']['BULK_CELL_COMPRESSION_CUTOFF']['value']=-1.0
            if change=='java':bad['bulk']['BULK_CELL_COMPRESSION_CUTOFF']['javaType']='java.lang.Long'
            if change=='sentinel':bad['bulk']['BULK_CELL_COMPRESSION_CUTOFF']['value']='0'
            if change=='variant':bad['bulk']['variants'][1]['item']['signature']=plain('gold_ingot')
            if change=='units':bad['bulk']['variants'][2]['unitFactor']='9'
            if change=='quantity':bad['bulk']['variants'][1]['item']['count']=1
            if change=='extra':bad['bulk']['variants'].append(copy.deepcopy(bad['bulk']['variants'][0]))
            if change=='mutable':bad['cellMutable'].pop('megacells:compression_cutoff')
            if change=='stored':bad['bulk']['BULK_CELL_ITEM']={'javaType':'appeng.api.stacks.AEItemKey','value':'minecraft:diamond'}
            if change=='count':bad['bulk']['BULK_CELL_UNIT_COUNT']={'javaType':'java.math.BigInteger','value':'5'}
            if change=='factor':bad['bulk']['BULK_CELL_UNIT_FACTOR']={'javaType':'java.math.BigInteger','value':'9'}
            self.assertFalse(cutoff(bad,-1,key),change)
    def test_complete_native_owned_ledger_rejects_extra_items_and_changed_typed_shells(self):
        data,n,inv,source,cell=fixture();self.assertTrue(owned(data,n,inv,source,cell,[]))
        for change in ['hidden','cursor','inventory','source','cell','contained','units','extra-config']:
            d=copy.deepcopy(data);native=copy.deepcopy(n)
            if change=='hidden':d['physical']['duplicate']=copy.deepcopy(cell)
            if change=='cursor':d['cursor']=copy.deepcopy(cell)
            if change=='inventory':d['inventory'][1]=copy.deepcopy(cell)
            if change=='source':native['source']['signature']=native['source']['signature'].replace('release_mega_bulk_source: 149','release_mega_bulk_source: 150')
            if change=='cell':native['cell']['signature']=native['cell']['signature'].replace('release_mega_bulk_cell: 151','release_mega_bulk_cell: 152')
            if change=='contained':native['sourceContained'].append(copy.deepcopy(cell))
            if change=='units':native['bulk']['BULK_CELL_UNIT_COUNT']={'javaType':'java.math.BigInteger','value':'1'}
            if change=='extra-config':native['cellConfig'].append(dict(empty=True))
            self.assertFalse(owned(d,native,inv,source,cell,[]),change)
    def test_empty_cell_cannot_inherit_stored_item_or_unbound_cutoff_components(self):
        _,native,*_=fixture();self.assertTrue(cutoff(native,None,None))
        for change in ['raw','codec','chain']:
            n=copy.deepcopy(native)
            if change=='raw':n['bulk']['BULK_CELL_COMPRESSION_CUTOFF']={'javaType':'java.lang.Integer','value':'2'}
            if change=='codec':n['cellMutable']['megacells:compression_cutoff']='2'
            if change=='chain':n['bulk']['variants']=chain()
            self.assertFalse(cutoff(n,None,None),change)


if __name__=='__main__':unittest.main()
