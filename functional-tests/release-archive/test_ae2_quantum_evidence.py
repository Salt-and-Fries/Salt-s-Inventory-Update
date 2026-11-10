"""Offline rejection controls for native quantum chamber evidence; no gameplay."""
from pathlib import Path
import copy,importlib.util,sys,unittest
import nbtlib

ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
spec=importlib.util.spec_from_file_location('review_ae2_quantum',ROOT/'tools/reconcile-ae2-quantum-release-evidence.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
DRIVER='a'*64


def fixture(target='1.21.1/neoforge'):
    mc,loader=target.split('/');air=dict(item='minecraft:air',count=0,signature='{}')
    def cell(item,count,metadata,quantum=False):
        tag=nbtlib.parse_nbt(m.expected_item(mc,item,1,metadata,quantum))
        return dict(item=item,count=count,signature=tag.snbt())
    inventory=[dict(index=i,**copy.deepcopy(air))for i in range(41)]
    for i,item,count,metadata,quantum in [(9,'ae2:quantum_entangled_singularity',1,m.DATA,True),
        (10,'minecraft:stone',4,m.REFUSAL,False),(12,'minecraft:diamond',7,m.SENTINEL,False)]:
        inventory[i]=dict(index=i,**cell(item,count,metadata,quantum))
    frames=[]
    for i,label in enumerate(m.LABELS):
        inv=copy.deepcopy(inventory);stored=i in{2,3,7,8}
        if i in{1,2,3,4,7,8}:inv[9]['count']=0
        if i==3:inv[10]['count']=0
        item=copy.deepcopy(inventory[9]if stored else air)
        cursor=copy.deepcopy(inventory[10]if i==3 else inventory[9]if i in{1,4}else air)
        slot=dict(index=0,player=False,virtual=False,active=True,**{k:v for k,v in item.items()if k!='index'})
        client=dict(session=17 if i<8 else 18,menu=m.MENU,screen='appeng.client.gui.implementations.QNBScreen',
            bounds=dict(x=26 if i>=6 else 10,y=32 if i>=6 else 20,width=176,height=120))
        server=dict(session=client['session'],menu=m.MENU)
        ledger=dict(inventory=inv,physical={'quantum':copy.deepcopy(item)}if stored else{},cursor=cursor,
            menus=[dict(session=client['session'],type=m.MENU,slots=[slot])])
        native=dict(host=m.HOST,formed=True,position='8, 65, 8',frequencyJavaType='java.lang.Long',
            frequency=m.FREQUENCY if stored else 0,item=item)
        frames.append((label,dict(native=native,client=dict(client=[client],server=[server]),ledger=ledger)))
    invalidated=dict(inventory=copy.deepcopy(inventory),physical={},cursor=copy.deepcopy(air),menus=[])
    journal=dict(complete=True,cases=['quantum-link'],driver_sha256=DRIVER,
        checks=[dict(case='quantum-link',name=name,passed=True)for name in m.NAMES],
        original_entries=[dict(original=dict(minecraft=mc,loader=loader,expected_native_menu=m.MENU,
            expected_native_screen='appeng.client.gui.implementations.QNBScreen'),executed_world_actions=[dict(type='fixture',block='ae2:quantum_link')])])
    return journal,frames,invalidated


class QuantumEvidence(unittest.TestCase):
    def reject(self,change):
        j,s,end=fixture();change(j,s,end)
        with self.assertRaises((AssertionError,KeyError,ValueError,TypeError)):
            m.validate('1.21.1/neoforge',j,s,end,DRIVER)
    def test_six_targets_retain_twelve_original_checks_and_eleven_states(self):
        for target in m.TARGETS:
            j,s,end=fixture(target);v,classes=m.validate(target,j,s,end,DRIVER)
            self.assertEqual(v,dict(assertions=12,states=11,native_variants=1,contract='native-single-chamber-frequency-transfer-not-two-endpoint-transport'))
            self.assertEqual(classes,[m.MENU,m.HOST,'appeng.client.gui.implementations.QNBScreen'])
    def test_journal_must_be_complete_exact_order_and_original_driver(self):
        for change in [lambda j,s,e:j.update(complete=1),lambda j,s,e:j.update(driver_sha256='b'*64),
            lambda j,s,e:j['checks'].pop(),lambda j,s,e:j['checks'][0].update(passed=1),
            lambda j,s,e:j['checks'].reverse(),lambda j,s,e:s.pop()]:self.reject(change)
    def test_exact_target_entry_screen_menu_and_client_server_session_required(self):
        self.reject(lambda j,s,e:j['original_entries'][0]['original'].update(loader='fabric'))
        self.reject(lambda j,s,e:j['original_entries'][0]['executed_world_actions'][0].update(type='provider'))
        self.reject(lambda j,s,e:s[1][1]['client']['client'][0].update(screen='other.QNBScreen'))
        self.reject(lambda j,s,e:s[2][1]['client']['server'][0].update(session=91))
        self.reject(lambda j,s,e:s[2][1]['ledger'].update(menus=[]))
    def test_native_formation_host_and_source_position_must_match(self):
        self.reject(lambda j,s,e:s[1][1]['native'].update(formed=False))
        self.reject(lambda j,s,e:s[4][1]['native'].update(host='other.Host'))
        self.reject(lambda j,s,e:s[7][1]['native'].update(position='9, 65, 8'))
        self.reject(lambda j,s,e:s[0][1]['native'].update(position='unknown'))
    def test_frequency_requires_exact_long_and_expected_transition(self):
        self.reject(lambda j,s,e:s[2][1]['native'].update(frequencyJavaType='java.lang.Integer'))
        self.reject(lambda j,s,e:s[2][1]['native'].update(frequency=float(m.FREQUENCY)))
        self.reject(lambda j,s,e:s[8][1]['native'].update(frequency=0))
        self.reject(lambda j,s,e:s[9][1]['native'].update(frequency=m.FREQUENCY))
    def test_real_restricted_slot_and_backing_chamber_must_correspond(self):
        self.reject(lambda j,s,e:s[2][1]['ledger']['menus'][0]['slots'][0].update(virtual=True))
        self.reject(lambda j,s,e:s[2][1]['ledger']['menus'][0]['slots'][0].update(active=False))
        self.reject(lambda j,s,e:s[2][1]['ledger']['menus'][0]['slots'][0].update(count=0))
        self.reject(lambda j,s,e:s[3][1]['native']['item'].update(signature=s[3][1]['ledger']['cursor']['signature']))
    def test_exact_typed_item_prerequisites_and_full_foreign_data_required(self):
        self.reject(lambda j,s,e:s[0][1]['ledger']['inventory'][9].update(count=2))
        self.reject(lambda j,s,e:s[0][1]['ledger']['inventory'][10].update(signature=s[0][1]['ledger']['inventory'][10]['signature'].replace('[B;','[I;')))
        self.reject(lambda j,s,e:s[5][1]['ledger']['inventory'][12].update(count=6))
        self.reject(lambda j,s,e:s[0][1]['ledger']['inventory'][12].update(index=13))
    def test_refusal_and_recovery_must_preserve_cursor_and_no_duplicate_physical_item(self):
        self.reject(lambda j,s,e:s[3][1]['ledger']['cursor'].update(count=0))
        self.reject(lambda j,s,e:s[4][1]['ledger']['cursor'].update(count=0))
        self.reject(lambda j,s,e:s[2][1]['ledger']['physical'].update(hidden=copy.deepcopy(s[2][1]['native']['item'])))
        self.reject(lambda j,s,e:s[5][1]['ledger']['inventory'][9].update(count=0))
    def test_move_and_reopened_stored_state_are_required(self):
        self.reject(lambda j,s,e:s[6][1]['client']['client'][0]['bounds'].update(x=10))
        self.reject(lambda j,s,e:s[6][1]['client']['client'][0]['bounds'].update(y=20))
        self.reject(lambda j,s,e:s[8][1]['native']['item'].update(count=0))
    def test_invalidated_source_must_close_all_menus_and_preserve_every_owned_item(self):
        self.reject(lambda j,s,e:e.update(menus=copy.deepcopy(s[-1][1]['ledger']['menus'])))
        self.reject(lambda j,s,e:e['inventory'][9].update(count=0))
        self.reject(lambda j,s,e:e.update(cursor=copy.deepcopy(e['inventory'][9])))
        self.reject(lambda j,s,e:e['physical'].update(hidden=copy.deepcopy(e['inventory'][12])))


if __name__=='__main__':unittest.main(verbosity=2)
