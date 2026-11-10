"""Native plan/resource oracles must reject plausible-looking fabricated results."""
import copy,unittest
from ae2_crafting_features import Crafting,cellshell,counted,fixture_pattern,key_item,plan_matches,stored_matches,CELL
from ae2_quantum_features import expected_item


def entry(mc,item,amount):return dict(item=key_item(mc,item),amount=amount,amountJavaType='java.lang.Long',empty=False)


def pattern(mc):return dict(type='appeng.crafting.pattern.AECraftingPattern',inputs=[dict(multiplier=1,alternatives=[entry(mc,'minecraft:oak_log',1)])],outputs=[entry(mc,'minecraft:oak_planks',4)])


def plan(mc):
    return dict(ready=True,simulation=False,bytes=32,output=entry(mc,'minecraft:oak_planks',12),used=[entry(mc,'minecraft:oak_log',3)],missing=[],emitted=[],patterns=[dict(pattern=pattern(mc),times=3)])


class NativeCraftingData(unittest.TestCase):
    def test_owned_ledger_binds_native_inaccessible_copy_and_all_physical_items(self):
        run=Crafting.__new__(Crafting);run.mc='26.3';run.cell=dict(count=1,signature=expected_item(run.mc,'ae2:item_storage_cell_64k',1,CELL));run.pattern=dict(count=1,signature='{id:"ae2:crafting_pattern",count:1,components:{"minecraft:custom_data":{recipe:1b}}}')
        raw=dict(count=1,signature='{id:"ae2:wrapped_generic_stack",count:1,components:{"minecraft:custom_data":{amount:1L}}}')
        mirror=dict(session=8,slot=0,type='appeng.menu.slot.InaccessibleSlot',raw=raw,mayPlace=False,mayPickup=False)
        run.native=dict(provider=[run.pattern],drive=[run.cell],mirrors=[mirror]);inv=[dict(index=12,**key_item(run.mc,'minecraft:diamond'))]
        data=dict(inventory=copy.deepcopy(inv),cursor=dict(count=0,signature='{}'),physical={'cell':copy.deepcopy(run.cell),'copy':copy.deepcopy(raw)},menus=[dict(session=8,type='appeng.menu.me.crafting.CraftAmountMenu',slots=[dict(index=0,type=mirror['type'],player=False,**raw)]),dict(session=7,type='appeng.menu.implementations.DriveMenu',slots=[dict(index=0,type='appeng.menu.slot.RestrictedInputSlot',player=False,**run.cell)])])
        self.assertTrue(run.owned(data,inv))
        closed=copy.deepcopy(data);closed['menus']=[closed['menus'][0]];del closed['physical']['cell']
        self.assertTrue(run.owned(closed,inv),'Closed menu has no physical slot; directly observed native world cell remains required')
        unexpected=copy.deepcopy(closed);unexpected['physical']['cell']=copy.deepcopy(run.cell)
        self.assertFalse(run.owned(unexpected,inv),'Closed-menu physical copy must not be accepted')
        original=run.native;run.native=copy.deepcopy(original);run.native['drive']=[]
        try:self.assertFalse(run.owned(closed,inv),'Closing a menu cannot hide loss of its world cell')
        finally:run.native=original
        for change in ['extra','metadata','cursor','inventory','session','pickup','provider']:
            bad=copy.deepcopy(data);native=copy.deepcopy(run.native)
            if change=='extra':bad['physical']['hidden']=copy.deepcopy(raw)
            if change=='metadata':bad['physical']['cell']['signature']=run.cell['signature'].replace('101','102')
            if change=='cursor':bad['cursor']=raw
            if change=='inventory':bad['inventory'][0]['count']=2
            if change=='session':native['mirrors'][0]['session']=9
            if change=='pickup':native['mirrors'][0]['mayPickup']=True
            if change=='provider':native['provider'].append(copy.deepcopy(run.pattern))
            old=run.native;run.native=native
            try:self.assertFalse(run.owned(bad,inv),change)
            finally:run.native=old
    def test_real_plan_requires_exact_native_recipe_inputs_outputs_and_iterations(self):
        for mc in ['1.20.1','26.3']:
            value=plan(mc);self.assertTrue(plan_matches(value,mc,12));self.assertTrue(fixture_pattern(pattern(mc),mc))
            for change in ['simulation','bytes','used','extra','metadata','output','iterations','pattern','missing','ready']:
                bad=copy.deepcopy(value)
                if change=='simulation':bad['simulation']=True
                if change=='bytes':bad['bytes']=True
                if change=='used':bad['used'][0]['amount']=2
                if change=='extra':bad['used'].append(entry(mc,'minecraft:diamond',1))
                if change=='metadata':bad['output']['item']['signature']=expected_item(mc,'minecraft:oak_planks',1,'{foreign:1b}')
                if change=='output':bad['output']['amount']=8
                if change=='iterations':bad['patterns'][0]['times']=3.0
                if change=='pattern':bad['patterns'][0]['pattern']['outputs'][0]['amount']=5
                if change=='missing':bad['missing']=[entry(mc,'minecraft:oak_log',1)]
                if change=='ready':bad['ready']=1
                self.assertFalse(plan_matches(bad,mc,12),(mc,change))
    def test_cell_shell_allows_only_native_storage_payload_changes(self):
        for mc in ['1.20.1','26.3']:
            initial=dict(count=1,signature=expected_item(mc,'ae2:item_storage_cell_64k',1,CELL));filled=copy.deepcopy(initial)
            if mc=='1.20.1':filled['signature']=filled['signature'].replace('tag:{','tag:{keys:[{id:"minecraft:oak_log"}],amts:[L;16L],ic:16L,')
            else:filled['signature']=filled['signature'].replace('components:{','components:{"ae2:storage_cell_inv":[{"#t":"ae2:i",id:"minecraft:oak_log","#":16L}],')
            self.assertEqual(cellshell(initial,mc),cellshell(filled,mc))
            for change in ['byte-array','marker','foreign-component','item']:
                bad=copy.deepcopy(filled)
                if change=='byte-array':bad['signature']=bad['signature'].replace('[B; 5B, 9B]','[5b,9b]').replace('[B;5b,9b]','[5b,9b]')
                if change=='marker':bad['signature']=bad['signature'].replace('101','102')
                if change=='foreign-component':bad['signature']=bad['signature'].replace('id:', 'foreign:1b,id:',1)
                if change=='item':bad['signature']=bad['signature'].replace('item_storage_cell_64k','item_storage_cell_1k')
                self.assertNotEqual(cellshell(initial,mc),cellshell(bad,mc),(mc,change,bad))
    def test_exact_stored_key_counter_rejects_loss_extras_wrong_data_and_numeric_types(self):
        mc='26.3';value=[entry(mc,'minecraft:oak_log',13),entry(mc,'minecraft:oak_planks',12)];self.assertTrue(stored_matches(value,mc,13,12))
        for change in ['loss','extra','metadata','float','bool','java','count']:
            bad=copy.deepcopy(value)
            if change=='loss':bad[0]['amount']=12
            if change=='extra':bad.append(entry(mc,'minecraft:diamond',1))
            if change=='metadata':bad[0]['item']['signature']=expected_item(mc,'minecraft:oak_log',1,'{protected:[B;1b]}')
            if change=='float':bad[0]['amount']=13.0
            if change=='bool':bad[0]['amount']=True
            if change=='java':bad[0]['amountJavaType']='java.lang.Integer'
            if change=='count':bad[0]['item']['count']=2
            if change in ['float','bool','java','count']:
                with self.assertRaises(AssertionError,msg=change):counted(bad)
            else:self.assertFalse(stored_matches(bad,mc,13,12),change)


if __name__=='__main__':unittest.main()
