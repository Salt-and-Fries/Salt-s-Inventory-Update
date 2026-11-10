"""Focused oracle/button negative controls; no live menu outcome is assigned."""
import copy,unittest
import nbtlib
from mi_storage_controls import button,extracting,lock_change,native_controls,expected_counts

def client(mc='1.21.1',kind='items'):
    prefix='aztech.modern_industrialization.'+(''if mc=='1.20.1'else'client.')+'machines.'
    widgets=[dict(type=prefix+'gui.MachineScreen$MachineButton',message='',active=True,visible=True,x=x,y=100,width=20,height=20)for x in [120,100]]
    window=dict(definition='com.salts_inventory_update.client.automatic.NativeTemplateWindow',nativeSlots=[],nativeWidgets=widgets,nativeBounds=dict(x=0,y=0,width=176,height=166),template=dict(regions=[dict(source=dict(x=0,y=0,width=176,height=166))]),nativeValues={})
    observed=dict(controls=dict(hasLockButton=True,lockingMode=False,autoExtract=[dict(type=prefix+'guicomponents.AutoExtractClient',hasExtractItems=kind=='items',hasExtractFluids=kind=='fluids',extractItems=False,extractFluids=False)]))
    return dict(windows=[window]),observed

def cells():
    return nbtlib.parse_nbt('[{amount:0L,key:{id:"minecraft:air"},playerLocked:0b,playerLockable:1b,playerExtract:1b},{amount:0L,key:{id:"minecraft:air"},playerLocked:0b,playerLockable:1b,playerExtract:1b}]')

class NativeStorageControls(unittest.TestCase):
    def test_pinned_vintage_modern_state_and_button_geometry_with_blank_labels(self):
        for mc in ['1.20.1','1.21.1']:
            for kind in ['items','fluids']:
                value,observed=client(mc,kind)
                self.assertEqual(button(value,mc,kind,'lock',False,observed)[1]['x'],120)
                self.assertEqual(button(value,mc,kind,'eject',False,observed)[1]['x'],100)
                observed['controls']['lockingMode']=True
                self.assertEqual(button(value,mc,kind,'lock',True,observed)[1]['x'],120)
        for args in [('other','items','lock',True),('1.20.1','other','lock',True),('1.20.1','items','other',True),('1.20.1','items','lock',1)]:
            value,observed=client('1.20.1')
            with self.assertRaises(AssertionError):button(value,*args,observed)
    def test_exact_visible_unique_native_button_required(self):
        for mutation in [lambda v:v['windows'][0]['nativeWidgets'][0].update(visible=False),lambda v:v['windows'][0]['nativeWidgets'][0].update(active=False),lambda v:v['windows'][0]['nativeWidgets'].append(copy.deepcopy(v['windows'][0]['nativeWidgets'][0])),lambda v:v['windows'][0]['nativeWidgets'][0].update(type='another.Button')]:
            value,observed=client();mutation(value)
            with self.assertRaises(AssertionError):button(value,'1.21.1','items','lock',False,observed)
        value,observed=client();observed['controls']['lockingMode']=1
        with self.assertRaises(AssertionError):button(value,'1.21.1','items','lock',False,observed)
        value,observed=client();observed['controls']['autoExtract'][0]['hasExtractItems']=False
        with self.assertRaises(AssertionError):button(value,'1.21.1','items','eject',False,observed)
    def test_absent_native_controls_are_proven_not_invented_or_clicked(self):
        for mc in ['1.20.1','1.21.1']:
            for lock,eject,counts in [(True,True,(9,10)),(True,False,(7,8)),(False,True,(5,5)),(False,False,(3,3))]:
                value,observed=client(mc,'fluids');observed['controls']['hasLockButton']=lock
                if not eject:observed['controls']['autoExtract']=[]
                value['windows'][0]['nativeWidgets']=[w for i,w in enumerate(value['windows'][0]['nativeWidgets'])if(lock if i==0 else eject)]
                _,roles,caps=native_controls(value,mc,'fluids',observed)
                self.assertEqual(set(roles),{name for name,present in [('lock',lock),('eject',eject)]if present})
                self.assertEqual(expected_counts(caps),counts)
                for role,present in [('lock',lock),('eject',eject)]:
                    if present:button(value,mc,'fluids',role,False,observed)
                    else:
                        with self.assertRaises(AssertionError):button(value,mc,'fluids',role,False,observed)
        value,observed=client('1.20.1','fluids');observed['controls']['autoExtract']=[]
        with self.assertRaises(AssertionError):native_controls(value,'1.20.1','fluids',observed)
        with self.assertRaises(AssertionError):expected_counts(dict(lock=1,eject=False,kind='fluids'))
    def test_native_ejection_flag_type_kind_and_omitted_false(self):
        self.assertFalse(extracting(dict(fixture='{}'),'items'))
        self.assertTrue(extracting(dict(fixture='{extractItems:1b}'),'items'))
        self.assertFalse(extracting(dict(fixture='{extractItems:1b}'),'fluids'))
        for value in ['{extractItems:1}','{extractItems:2b}']:
            with self.assertRaises(AssertionError):extracting(dict(fixture=value),'items')
    def test_only_selected_native_lock_fields_may_change(self):
        original=cells();changed=copy.deepcopy(original);changed[0]['playerLocked']=nbtlib.Byte(1);changed[0]['locked']=nbtlib.String('minecraft:air')
        self.assertTrue(lock_change(original,changed,0));self.assertEqual(int(original[0]['playerLocked']),0)
        wrong=copy.deepcopy(changed);wrong[1]['playerLocked']=nbtlib.Byte(1);self.assertFalse(lock_change(original,wrong,0))
        wrong=copy.deepcopy(changed);wrong[0]['playerExtract']=nbtlib.Byte(0);self.assertFalse(lock_change(original,wrong,0))
    def test_resources_and_native_policy_types_cannot_be_hidden(self):
        original=cells();changed=copy.deepcopy(original);changed[0]['playerLocked']=nbtlib.Byte(1);changed[0]['amount']=nbtlib.Long(1)
        self.assertFalse(lock_change(original,changed,0))
        changed=copy.deepcopy(original);changed[0]['playerLocked']=nbtlib.Int(1)
        with self.assertRaises(AssertionError):lock_change(original,changed,0)
        with self.assertRaises(AssertionError):lock_change(original,original,True)
    def test_non_lockable_native_cell_requires_exact_refusal(self):
        original=cells();original[0]['playerLockable']=nbtlib.Byte(0)
        self.assertTrue(lock_change(original,copy.deepcopy(original),0))
        changed=copy.deepcopy(original);changed[0]['playerLocked']=nbtlib.Byte(1)
        self.assertFalse(lock_change(original,changed,0))

if __name__=='__main__':unittest.main(verbosity=2)
