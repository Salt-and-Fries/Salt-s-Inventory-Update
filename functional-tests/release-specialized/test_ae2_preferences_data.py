"""Do not substitute widget appearance for actual native configuration values."""
import copy,unittest
from ae2_preferences_features import values,controls_match,FIELDS
from ae2_settings_features import ledger


def state():
    raw={k:dict(value=False,javaType='java.lang.Boolean')for k in list(FIELDS.values())+['isUseExternalSearch']}
    controls={k:dict(selected=False,visible=k!='clearExternalCheckbox',active=True)for k in FIELDS}
    controls['useInternalSearchRadio']=dict(selected=True,visible=True,active=True);controls['useExternalSearchRadio']=dict(selected=False,visible=True,active=False)
    return dict(values=raw,controls=controls,browserEnabled=False)


class NativePreferencesData(unittest.TestCase):
    def test_actual_boolean_contract_rejects_numeric_truthiness_wrong_java_type_and_missing_fields(self):
        native=state();self.assertEqual(len(values(native)),8);self.assertTrue(controls_match(native))
        for change in ['number','java','missing','extra']:
            bad=copy.deepcopy(native)
            if change=='number':bad['values']['isAutoFocusSearch']['value']=1
            if change=='java':bad['values']['isAutoFocusSearch']['javaType']='java.lang.Integer'
            if change=='missing':bad['values'].pop('isAutoFocusSearch')
            if change=='extra':bad['values']['unknown']=dict(value=False,javaType='java.lang.Boolean')
            with self.assertRaises(AssertionError,msg=change):values(bad)
    def test_checked_widget_cannot_replace_native_setting_or_wrong_radio_availability(self):
        native=state();self.assertTrue(controls_match(native))
        for change in ['selected','radio','availability','visibility','external-clear']:
            bad=copy.deepcopy(native)
            if change=='selected':bad['controls']['autoFocusCheckbox']['selected']=True
            if change=='radio':bad['controls']['useInternalSearchRadio']['selected']=False
            if change=='availability':bad['controls']['useExternalSearchRadio']['active']=True
            if change=='visibility':bad['controls']['rememberCheckbox']['visible']=False
            if change=='external-clear':bad['controls']['clearExternalCheckbox']['visible']=True
            self.assertFalse(controls_match(bad),change)
    def test_native_external_mode_has_independent_visibility_and_restore_contract(self):
        native=state();native['browserEnabled']=True;native['values']['isUseExternalSearch']['value']=True
        native['controls']['useInternalSearchRadio']['selected']=False;native['controls']['useExternalSearchRadio'].update(selected=True,active=True)
        for name in ['rememberCheckbox','autoFocusCheckbox','syncWithExternalCheckbox']:native['controls'][name]['visible']=False
        native['controls']['clearExternalCheckbox']['visible']=True;self.assertTrue(controls_match(native))
        bad=copy.deepcopy(native);bad['controls']['useExternalSearchRadio']['selected']=1;self.assertFalse(controls_match(bad))
        bad=copy.deepcopy(native);bad['values']['isUseExternalSearch']['value']=False;self.assertFalse(controls_match(bad))
    def test_local_configuration_does_not_allow_owned_metadata_cursor_or_hidden_slot_changes(self):
        item=dict(count=7,signature='{id:"minecraft:diamond",count:1,components:{"minecraft:custom_data":{typed:[B;2b,8b]}}}')
        native=dict(inventory=[dict(index=12,**item)],physical={},cursor=dict(count=0,signature='{}'));baseline=ledger(native)
        for change in ['metadata','count','cursor','physical']:
            bad=copy.deepcopy(native)
            if change=='metadata':bad['inventory'][0]['signature']=item['signature'].replace('[B;','[')
            if change=='count':bad['inventory'][0]['count']=6
            if change=='cursor':bad['cursor']=copy.deepcopy(item)
            if change=='physical':bad['physical']['unexpected']=copy.deepcopy(item)
            self.assertNotEqual(ledger(bad),baseline,change)


if __name__=='__main__':unittest.main()
