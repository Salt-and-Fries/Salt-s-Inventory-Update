"""Offline rejection controls for native numeric and server-backed overlay evidence."""
from pathlib import Path
import copy,importlib.util,sys,unittest
import nbtlib

ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
spec=importlib.util.spec_from_file_location('review_ae2_native_settings',ROOT/'tools/reconcile-ae2-settings-release-evidence.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
DRIVER='a'*64


def value(number,kind='java.lang.Integer'):return dict(javaType=kind,value=number)


def fixture(target='1.21.1/neoforge'):
    mc,loader=target.split('/');tag=nbtlib.parse_nbt(m.marker_item(mc))
    tag['Count'if mc=='1.20.1'else'count']=nbtlib.Byte(1)if mc=='1.20.1'else nbtlib.Int(1)
    marker=dict(item='minecraft:diamond',count=7,signature=tag.snbt())
    empty=dict(item='minecraft:air',count=0,signature='{}')
    entries=[];states={};checks=[]
    for name in m.CASES:
        parent_screen='appeng.client.gui.implementations.'+{'priority':'StorageBusScreen','spatial-anchor':'SpatialAnchorScreen','energy-level-emitter':'EnergyLevelEmitterScreen'}[name]
        entries.append(dict(case=name,original_capture=dict(minecraft=mc,loader=loader,expected_native_menu=m.PARENTS[name],expected_native_screen=parent_screen),executed_actions=[dict(type='fixture',block='ae2:controller')]))
        frames=[]
        for i,label in enumerate(m.FRAMES[name]):
            parent=label in{'native-parent','storage-bus-reopened'}
            menu=m.PARENTS[name]if parent else m.MENUS[name]
            sid=10+i;vals={};server_vals={};widgets=[]
            if name=='priority':
                if parent:widgets=[dict(type='appeng.client.gui.widgets.TabButton',visible=True,active=True,message='Priority',tooltips=['Priority'])]
                else:
                    amount=0 if i==1 else 41 if i in[2,3]else 37
                    vals=dict(getPriorityValue=value(amount),nativeHostPriority=value(0))
                    server_vals=dict(getPriorityValue=value(amount),nativeHostPriority=value(amount))
                    widgets=[dict(type='net.minecraft.client.gui.components.EditBox',visible=True,active=True,value=str(amount))]
                moved=i in[3,4]
            elif name=='spatial-anchor':
                mode='YES'if i in[1,2]else'NO'
                vals={key:value(128 if key=='powerConsumption'else 3,'java.lang.Long'if key=='powerConsumption'else'java.lang.Integer')for key in m.STATS}
                server_vals=copy.deepcopy(vals)
                vals['getOverlayMode']=value(mode,'appeng.api.config.YesNo');server_vals['getOverlayMode']=value(mode,'appeng.api.config.YesNo')
                widgets=[dict(type='appeng.client.gui.widgets.SettingToggleButton',visible=True,active=True,currentValue=mode)]
                moved=i in[2,3]
            else:
                amount=0 if i==0 else 41 if i<4 else 42
                mode='LOW_SIGNAL'if i<2 else'HIGH_SIGNAL'
                vals=dict(getReportingValue=value(amount,'java.lang.Long'),getRedStoneMode=value(mode,'appeng.api.config.RedstoneMode'))
                server_vals=copy.deepcopy(vals);server_vals['nativeHostReportingValue']=value(amount,'java.lang.Long');server_vals['getReportingValue']=value(0,'java.lang.Long')
                widgets=[dict(type='net.minecraft.client.gui.components.EditBox',visible=True,active=True,value=str(amount)),
                    dict(type='appeng.client.gui.widgets.SettingToggleButton',visible=True,active=True,currentValue=mode)]
                moved=i in[3,4]
            screen='appeng.client.gui.implementations.PriorityScreen'if name=='priority'and not parent else parent_screen
            client=dict(session=sid,menu=menu,screen=screen,values=vals,widgets=widgets,locked=False,
                bounds=dict(x=26 if moved else 10,y=32 if moved else 20,width=176,height=120))
            server=dict(session=sid,menu=menu,values=server_vals)
            inventory=[dict(index=j,**copy.deepcopy(marker if j==12 else empty))for j in range(41)]
            data=dict(inventory=inventory,physical={},cursor=copy.deepcopy(empty),menus=[dict(session=sid,type=menu,slots=[])])
            frames.append((label,dict(native=dict(client=[client],server=[server]),ledger=data)))
        states[name]=frames
        checks.append(dict(case=name,name='exact native parent and typed pre-entry inventory',passed=True))
        checks.extend(dict(case=name,name=label,passed=True)for label in m.NAMES[name])
    checks.append(dict(case=m.CASES[-1],name='all native setting sessions close without changing owned items',passed=True))
    return dict(complete=True,cases=list(m.CASES),checks=checks,driver_sha256=DRIVER,original_entries=entries),states


class NativeSettingsEvidence(unittest.TestCase):
    def reject(self,mutate):
        j,s=fixture();mutate(j,s)
        with self.assertRaises((AssertionError,KeyError,ValueError,TypeError)):m.validate('1.21.1/neoforge',j,s,DRIVER)
    def test_six_targets_have_exact_twenty_checks_and_eighteen_real_frame_contracts(self):
        for target in m.TARGETS:
            j,s=fixture(target);v,classes=m.validate(target,j,s,DRIVER)
            self.assertEqual(v,dict(assertions=20,states=18,native_variants=3,contract='native-settings-and-server-overlay-not-redstone-or-chunk-endurance'))
            self.assertIn('appeng.client.gui.implementations.PriorityScreen',classes)
            self.assertEqual(m.specification(target,'atlas-ae2-spatial-anchor',[m.MENUS['spatial-anchor']])[1],'functional-pass')
    def test_complete_ordered_original_checks_driver_and_frame_set_required(self):
        for change in [lambda j,s:j.update(complete=1),lambda j,s:j.update(driver_sha256='b'*64),lambda j,s:j['checks'].pop(),
            lambda j,s:j['checks'][0].update(passed=1),lambda j,s:s['priority'].pop()]:self.reject(change)
    def test_exact_source_target_menu_screen_and_session_correspondence_required(self):
        self.reject(lambda j,s:j['original_entries'][0]['original_capture'].update(loader='fabric'))
        self.reject(lambda j,s:s['priority'][1][1]['native']['client'][0].update(screen='other.PriorityScreen'))
        self.reject(lambda j,s:s['priority'][2][1]['native']['server'][0].update(session=999))
        self.reject(lambda j,s:s['priority'][2][1]['ledger'].update(menus=[]))
        self.reject(lambda j,s:j['original_entries'][0]['executed_actions'][0].update(type='provider'))
    def test_priority_requires_exact_backing_host_typed_numbers_and_actual_text(self):
        self.reject(lambda j,s:s['priority'][2][1]['native']['server'][0]['values'].update(nativeHostPriority=value(40)))
        self.reject(lambda j,s:s['priority'][2][1]['native']['client'][0]['values'].update(getPriorityValue=value(41.0)))
        self.reject(lambda j,s:s['priority'][4][1]['native']['client'][0]['widgets'][0].update(value='41'))
        self.reject(lambda j,s:s['priority'][-1][1]['native']['server'][0]['values'].update(getPriorityValue=value(0)))
    def test_server_overlay_must_synchronize_change_and_restore(self):
        self.reject(lambda j,s:s['spatial-anchor'][1][1]['native']['server'][0]['values'].update(getOverlayMode=value('NO','appeng.api.config.YesNo')))
        self.reject(lambda j,s:s['spatial-anchor'][-1][1]['native']['client'][0]['values'].update(getOverlayMode=value('YES','appeng.api.config.YesNo')))
        self.reject(lambda j,s:s['spatial-anchor'][1][1]['native']['client'][0]['values'].update(getOverlayMode=value('NO','appeng.api.config.YesNo')))
    def test_nonvacuous_native_statistics_need_both_values_and_original_java_types(self):
        self.reject(lambda j,s:s['spatial-anchor'][0][1]['native']['client'][0]['values'].update(powerConsumption=value(128)))
        self.reject(lambda j,s:s['spatial-anchor'][0][1]['native']['server'][0]['values'].update(allChunks=value(0)))
        self.reject(lambda j,s:s['spatial-anchor'][0][1]['native']['client'][0]['values'].update(allWorlds=value(True)))
    def test_emitter_threshold_mode_persistence_and_moved_input_required(self):
        self.reject(lambda j,s:s['energy-level-emitter'][1][1]['native']['server'][0]['values'].update(nativeHostReportingValue=value(41)))
        self.reject(lambda j,s:s['energy-level-emitter'][-1][1]['native']['server'][0]['values'].update(getRedStoneMode=value('LOW_SIGNAL','appeng.api.config.RedstoneMode')))
        self.reject(lambda j,s:s['energy-level-emitter'][3][1]['native']['client'][0]['bounds'].update(x=10))
        self.reject(lambda j,s:s['priority'][3][1]['native']['client'][0]['bounds'].update(y=20))
    def test_native_emitter_host_is_required_even_when_menu_field_looks_correct(self):
        self.reject(lambda j,s:s['energy-level-emitter'][1][1]['native']['server'][0]['values'].update(nativeHostReportingValue=value(0,'java.lang.Long'),getReportingValue=value(41,'java.lang.Long')))
        self.reject(lambda j,s:s['energy-level-emitter'][1][1]['native']['server'][0]['values'].pop('nativeHostReportingValue'))
    def test_missing_or_hidden_real_controls_cannot_pass_numeric_or_overlay_evidence(self):
        self.reject(lambda j,s:s['priority'][1][1]['native']['client'][0].update(widgets=[]))
        self.reject(lambda j,s:s['priority'][0][1]['native']['client'][0]['widgets'][0].update(visible=False))
        self.reject(lambda j,s:s['spatial-anchor'][0][1]['native']['client'][0]['widgets'][0].update(active=False))
    def test_full_typed_foreign_items_hidden_physical_stacks_and_cursor_preserved(self):
        self.reject(lambda j,s:s['priority'][3][1]['ledger']['inventory'][12].update(count=6))
        self.reject(lambda j,s:s['energy-level-emitter'][3][1]['ledger']['inventory'][12].update(signature=s['energy-level-emitter'][3][1]['ledger']['inventory'][12]['signature'].replace('[B;','[I;')))
        self.reject(lambda j,s:s['priority'][2][1]['ledger']['physical'].update(hidden=copy.deepcopy(s['priority'][2][1]['ledger']['inventory'][12])))
        self.reject(lambda j,s:s['spatial-anchor'][3][1]['ledger']['cursor'].update(count=1))


if __name__=='__main__':unittest.main(verbosity=2)
