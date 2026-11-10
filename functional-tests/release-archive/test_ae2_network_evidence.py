"""Offline rejection controls for native populated network-status evidence."""
from pathlib import Path
import copy,importlib.util,json,sys,tempfile,unittest,zipfile
import nbtlib

ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
spec=importlib.util.spec_from_file_location('review_ae2_network',ROOT/'tools/reconcile-ae2-network-release-evidence.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
DRIVER='a'*64


def fixture(target='1.21.1/neoforge'):
    mc,loader=target.split('/');modern=mc!='1.20.1'
    def number(value,kind='java.lang.Double'):return dict(javaType=kind,value=value)
    air=dict(item='minecraft:air',count=0,signature='{}');tag=nbtlib.parse_nbt(m.marker_item(mc))
    tag['Count'if mc=='1.20.1'else'count']=nbtlib.Byte(1)if mc=='1.20.1'else nbtlib.Int(1)
    inventory=[dict(index=i,**copy.deepcopy(air))for i in range(41)]
    inventory[12]=dict(index=12,item='minecraft:diamond',count=7,signature=tag.snbt())
    frames=[]
    for i,label in enumerate(m.LABELS):
        status={k:number(10.0)for k in ['getAveragePowerInjection','getAveragePowerUsage','getStoredPower','getMaxStoredPower','getChannelPower']}
        status['getChannelsUsed']=number(3 if i==4 else 2,'java.lang.Integer')
        status['groups']=[dict(item='ae2:drive',display=dict(count=1,signature='{id:"ae2:drive",count:1}'),
            getCount=number(3 if i==4 else 2,'java.lang.Integer'),isMissingChannel=number(False,'java.lang.Boolean'),
            getIdlePowerUsage=number(1.0),getPowerGenerationCapacity=number(0.0))]
        sid=17 if i<6 else 18;unit='AE'if i==0 else'EU'if i<3 else'FE'
        c=dict(session=sid,menu=m.MENU,screen=m.SCREEN,status=copy.deepcopy(status),selectedEnergyUnit=unit)
        server=dict(session=sid,menu=m.MENU,status=copy.deepcopy(status))
        widgets=[dict(type='appeng.client.gui.widgets.SettingToggleButton',message='',currentValue=unit,visible=True,active=True)]
        if modern:
            c['canExportGrid']=True;server.update(gridSerial=71,serverDirectory='actual/native/server')
            widgets.append(dict(type='appeng.client.gui.widgets.ActionButton',message='Export Grid',visible=True,active=True))
        else:c['exportUnavailableOnPinnedRelease']=True
        w=dict(session=sid,menu=m.MENU,screen=m.SCREEN,widgets=widgets,
            bounds=dict(x=26 if i>=2 else 10,y=32 if i>=2 else 20,width=176,height=120))
        data=dict(inventory=copy.deepcopy(inventory),physical={},cursor=copy.deepcopy(air),menus=[dict(session=sid,type=m.MENU,slots=[])])
        frames.append((label,dict(native=dict(client=[c],server=[server]),widgets=dict(client=[w],server=[dict(session=sid,menu=m.MENU)]),ledger=data)))
    journal=dict(complete=True,cases=['network-status'],driver_sha256=DRIVER,
        checks=[dict(case='network-status',name=name,passed=True)for name in m.NAMES+(m.MODERN if modern else m.LEGACY)],
        original_entries=[dict(original=dict(minecraft=mc,loader=loader,expected_native_menu=m.MENU,expected_native_screen=m.SCREEN),
            executed_world_actions=[dict(type='fixture',block='ae2:controller')])],
        export=dict(file='actual-native-export.zip',sha256='b'*64,members=['grid.json'],grid_serial=71)if modern else None)
    end=dict(inventory=copy.deepcopy(inventory),physical={},cursor=copy.deepcopy(air),menus=[])if modern else None
    return journal,frames,end


class NetworkEvidence(unittest.TestCase):
    def reject(self,change):
        j,s,end=fixture();change(j,s,end)
        with self.assertRaises((AssertionError,KeyError,ValueError,TypeError)):m.validate('1.21.1/neoforge',j,s,end,DRIVER)
    def test_six_target_contracts_preserve_legacy_and_modern_native_capabilities(self):
        for target in m.TARGETS:
            j,s,end=fixture(target);v,classes=m.validate(target,j,s,end,DRIVER)
            self.assertEqual(v['assertions'],10 if target.startswith('1.20.1/')else 11)
            self.assertEqual(v['states'],7 if target.startswith('1.20.1/')else 8)
            self.assertEqual(classes,[m.MENU,m.SCREEN])
    def test_ordered_original_checks_driver_and_immediate_frames_required(self):
        for change in [lambda j,s,e:j.update(complete=1),lambda j,s,e:j.update(driver_sha256='b'*64),
            lambda j,s,e:j['checks'].pop(),lambda j,s,e:j['checks'][0].update(passed=1),
            lambda j,s,e:j['checks'].reverse(),lambda j,s,e:s.pop()]:self.reject(change)
    def test_exact_native_entry_screen_menu_session_and_ledger_correspondence(self):
        self.reject(lambda j,s,e:j['original_entries'][0]['original'].update(loader='fabric'))
        self.reject(lambda j,s,e:j['original_entries'][0]['executed_world_actions'][0].update(type='provider'))
        self.reject(lambda j,s,e:s[0][1]['native']['client'][0].update(screen='other.NetworkStatusScreen'))
        self.reject(lambda j,s,e:s[2][1]['widgets']['server'][0].update(session=81))
        self.reject(lambda j,s,e:s[2][1]['ledger'].update(menus=[]))
    def test_native_status_must_be_typed_finite_nonempty_and_powered(self):
        self.reject(lambda j,s,e:s[0][1]['native']['client'][0]['status']['getAveragePowerUsage'].update(value=float('nan')))
        self.reject(lambda j,s,e:s[0][1]['native']['server'][0]['status']['getChannelsUsed'].update(value=2.0))
        self.reject(lambda j,s,e:s[0][1]['native']['server'][0]['status'].update(groups=[]))
        self.reject(lambda j,s,e:s[0][1]['native']['client'][0]['status']['getStoredPower'].update(value=0))
    def test_independent_packet_and_backend_topology_and_real_two_three_two_transition(self):
        self.reject(lambda j,s,e:s[4][1]['native']['client'][0]['status']['groups'][0]['getCount'].update(value=2))
        def omit_add(j,s,e):
            for side in ['client','server']:
                s[4][1]['native'][side][0]['status']['groups'][0]['getCount']['value']=2
        self.reject(omit_add)
        self.reject(lambda j,s,e:s[5][1]['native']['server'][0]['status']['getMaxStoredPower'].update(value=11))
        j,s,end=fixture();s[1][1]['native']['client'][0]['status']['getAveragePowerUsage']['value']=7.5
        m.validate('1.21.1/neoforge',j,s,end,DRIVER) # Different native observation ticks may legitimately change averages.
    def test_real_local_unit_changes_moved_controls_and_reopening_required(self):
        self.reject(lambda j,s,e:s[1][1]['native']['client'][0].update(selectedEnergyUnit='AE'))
        self.reject(lambda j,s,e:s[6][1]['native']['client'][0].update(selectedEnergyUnit='EU'))
        self.reject(lambda j,s,e:s[2][1]['widgets']['client'][0]['bounds'].update(x=10))
        self.reject(lambda j,s,e:s[2][1]['widgets']['client'][0]['bounds'].update(y=20))
    def test_visible_original_setting_and_export_controls_must_exist(self):
        self.reject(lambda j,s,e:s[1][1]['widgets']['client'][0]['widgets'][0].update(active=False))
        self.reject(lambda j,s,e:s[-1][1]['widgets']['client'][0]['widgets'][1].update(visible=False))
        self.reject(lambda j,s,e:s[-1][1]['native']['client'][0].update(canExportGrid=False))
    def test_complete_typed_foreign_inventory_physical_items_and_cursor_preserved(self):
        self.reject(lambda j,s,e:s[0][1]['ledger']['inventory'][12].update(signature=s[0][1]['ledger']['inventory'][12]['signature'].replace('[B;','[I;')))
        self.reject(lambda j,s,e:s[2][1]['ledger']['inventory'][12].update(count=6))
        self.reject(lambda j,s,e:s[3][1]['ledger']['physical'].update(hidden=copy.deepcopy(s[3][1]['ledger']['inventory'][12])))
        self.reject(lambda j,s,e:s[4][1]['ledger'].update(cursor=copy.deepcopy(s[4][1]['ledger']['inventory'][12])))
    def test_modern_export_requires_same_native_grid_and_closed_unchanged_final_state(self):
        self.reject(lambda j,s,e:j['export'].update(grid_serial=72))
        self.reject(lambda j,s,e:s[3][1]['native']['server'][0].update(gridSerial=72))
        self.reject(lambda j,s,e:e.update(menus=copy.deepcopy(s[-1][1]['ledger']['menus'])))
        self.reject(lambda j,s,e:e['inventory'][12].update(count=6))
        j,s,end=fixture('1.20.1/fabric');j['export']=dict(file='invented.zip')
        with self.assertRaises(AssertionError):m.validate('1.20.1/fabric',j,s,end,DRIVER)
    def test_actual_native_zip_receipt_crc_hash_members_and_workspace_path_bound(self):
        with tempfile.TemporaryDirectory()as folder:
            root=Path(folder);path=root/'native.zip'
            value=dict(id=71,disposed=False,nodes=[{}]*4,machines=[{}]*4)
            def write(data):
                with zipfile.ZipFile(path,'w')as z:z.writestr('grid.json',json.dumps(data))
            write(value);receipt=m.exported_grid(path,71);ref=m.review_export(root,receipt)
            self.assertEqual(ref['sha256'],receipt['sha256'])
            for key,bad in [('sha256','c'*64),('members',[]),('grid_serial',72)]:
                r=copy.deepcopy(receipt);r[key]=bad
                with self.assertRaises(AssertionError):m.review_export(root,r)
            write(dict(value,nodes=[]))
            with self.assertRaises(AssertionError):m.review_export(root,receipt)
            with self.assertRaises(AssertionError):m.review_export(root/'missing',receipt)


if __name__=='__main__':unittest.main(verbosity=2)
