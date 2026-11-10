"""Offline rejection controls for real provider and native remote pattern evidence."""
from pathlib import Path
import copy,importlib.util,sys,unittest
import nbtlib

ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
spec=importlib.util.spec_from_file_location('review_ae2_pattern',ROOT/'tools/reconcile-ae2-pattern-release-evidence.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
DRIVER='a'*64


def fixture(target='1.21.1/neoforge'):
    mc,loader=target.split('/');air=dict(item='minecraft:air',count=0,signature='{}')
    def cell(name,count,text):
        tag=nbtlib.parse_nbt(text);tag['Count'if mc=='1.20.1'else'count']=nbtlib.Byte(1)if mc=='1.20.1'else nbtlib.Int(1)
        return dict(item=name,count=count,signature=tag.snbt())
    pattern=cell('ae2:processing_pattern',1,m.expected_item(mc,'ae2:processing_pattern',1,'{native_fixture:1b,typed:[B;2b,4b]}'))
    inventory=[dict(index=i,**copy.deepcopy(air))for i in range(41)]
    inventory[9]=dict(index=9,**pattern)
    inventory[10]=dict(index=10,**cell('minecraft:stone',3,m.expected_item(mc,'minecraft:stone',3,m.REFUSAL)))
    inventory[12]=dict(index=12,**cell('minecraft:diamond',7,m.marker_item(mc)))
    seed=dict(item=copy.deepcopy(pattern),inputMultiplier=1,
        decodedInput=dict(id='minecraft:stone',amount=1,javaType='java.lang.Long'),decodedOutput=dict(id='minecraft:diamond',amount=1,javaType='java.lang.Long'))
    frames=[];full={2,3,4,5,6,7,9,10,13,14,15}
    for i,label in enumerate(m.LABELS):
        provider=i<3;menu=m.PROVIDER_MENU if provider else m.MENU;screen=m.PROVIDER_SCREEN if provider else m.SCREEN
        sid=17 if provider else 18 if i<15 else 19;shown='ALL'if i<14 else'NOT_FULL'
        items=[copy.deepcopy(pattern if i in full else air)]+[copy.deepcopy(air)for _ in range(8)]
        slot=dict(type='appeng.menu.slot.RestrictedInputSlot'if provider else'appeng.client.gui.me.patternaccess.PatternSlot',
            containerIndex=0,visible=True,item=copy.deepcopy(items[0]),serverId=71)
        search='diamond'if i==4 else'release_no_matching_pattern_985'if i==5 else''
        meta=dict(session=sid,menu=menu,slots=[]if i==5 else[slot],settings=[dict(currentValue=shown,visible=True,active=True)])
        server=dict(session=sid,menu=menu)
        if not provider:
            meta.update(shownProviders=shown,records=[dict(serverId=71,items=copy.deepcopy(items),searchName='Native pattern provider')],
                rows=0 if i==5 else 1,search=search)
            server.update(shownProviders=shown,records=[dict(serverId=71,items=copy.deepcopy(items))])
        widgets=[]if provider else[dict(type='net.minecraft.client.gui.components.EditBox',value=search,message='',visible=True,active=True)]
        c=dict(session=sid,menu=menu,screen=screen,widgets=widgets,bounds=dict(x=26 if i==10 else 10,y=32 if i==10 else 20,width=176,height=120))
        inv=copy.deepcopy(inventory);cursor=copy.deepcopy(air);physical={}
        if i not in{0,12,16}:inv[9]['count']=0
        if i==7:inv[10]['count']=0;cursor=copy.deepcopy(inventory[10])
        if i in{1,8,11}:cursor=copy.deepcopy(pattern)
        if i==2:physical['native-provider-slot']=copy.deepcopy(pattern)
        data=dict(inventory=inv,cursor=cursor,physical=physical,menus=[dict(session=sid,type=menu,slots=[])])
        native=dict(client=[meta],server=[]if provider else[server],provider=items,providerVisible=True)
        frames.append((label,dict(native=native,widgets=dict(client=[c],server=[dict(session=sid,menu=menu)]),ledger=data)))
    journal=dict(complete=True,cases=['pattern-access'],driver_sha256=DRIVER,
        checks=[dict(case='pattern-access',name=name,passed=True)for name in m.NAMES],
        original_entries=[dict(original=dict(minecraft=mc,loader=loader,expected_native_menu=m.MENU,expected_native_screen=m.SCREEN),
            executed_world_actions=[dict(type='fixture',block='ae2:controller')])])
    closed=dict(inventory=copy.deepcopy(inventory),cursor=copy.deepcopy(air),physical={},menus=[])
    return journal,frames,seed,closed


class PatternEvidence(unittest.TestCase):
    def reject(self,change):
        j,s,seed,end=fixture();change(j,s,seed,end)
        with self.assertRaises((AssertionError,KeyError,ValueError,TypeError)):m.validate('1.21.1/neoforge',j,s,seed,end,DRIVER)
    def test_six_targets_require_nineteen_checks_eighteen_states_and_native_seed(self):
        for target in m.TARGETS:
            j,s,seed,end=fixture(target);v,classes=m.validate(target,j,s,seed,end,DRIVER)
            self.assertEqual(v,dict(assertions=19,states=18,native_seed=1,native_variants=1,contract='native-provider-remote-input-and-search-not-processing-production'))
            self.assertIn(m.PROVIDER_MENU,classes)
    def test_exact_order_original_driver_complete_and_frame_set(self):
        for change in [lambda j,s,k,e:j.update(complete=1),lambda j,s,k,e:j.update(driver_sha256='b'*64),
            lambda j,s,k,e:j['checks'].pop(),lambda j,s,k,e:j['checks'][0].update(passed=1),
            lambda j,s,k,e:j['checks'].reverse(),lambda j,s,k,e:s.pop()]:self.reject(change)
    def test_exact_entry_native_screens_menus_and_sessions_required(self):
        self.reject(lambda j,s,k,e:j['original_entries'][0]['original'].update(loader='fabric'))
        self.reject(lambda j,s,k,e:j['original_entries'][0]['executed_world_actions'][0].update(type='provider'))
        self.reject(lambda j,s,k,e:s[0][1]['widgets']['client'][0].update(screen=m.SCREEN))
        self.reject(lambda j,s,k,e:s[3][1]['widgets']['server'][0].update(session=99))
        self.reject(lambda j,s,k,e:s[4][1]['ledger'].update(menus=[]))
    def test_preentry_native_seed_requires_real_decoding_and_exact_full_pattern(self):
        self.reject(lambda j,s,k,e:k.update(inputMultiplier=2))
        self.reject(lambda j,s,k,e:k['decodedInput'].update(javaType='java.lang.Integer'))
        self.reject(lambda j,s,k,e:k['decodedOutput'].update(id='minecraft:stone'))
        self.reject(lambda j,s,k,e:k['item'].update(count=2))
    def test_provider_must_start_empty_and_accept_actual_restricted_slot(self):
        self.reject(lambda j,s,k,e:s[0][1]['native']['provider'].__setitem__(0,copy.deepcopy(k['item'])))
        self.reject(lambda j,s,k,e:s[0][1]['native']['client'][0]['slots'][0].update(type='other.RegularSlot'))
        self.reject(lambda j,s,k,e:s[2][1]['native']['client'][0]['slots'][0]['item'].update(count=0))
        self.reject(lambda j,s,k,e:s[2][1]['ledger']['physical'].update(hidden=copy.deepcopy(k['item'])))
    def test_remote_records_must_match_actual_provider_and_server_ids(self):
        self.reject(lambda j,s,k,e:s[3][1]['native']['server'][0]['records'][0]['items'][0].update(count=0))
        self.reject(lambda j,s,k,e:s[4][1]['native']['client'][0]['records'][0].update(serverId=72))
        self.reject(lambda j,s,k,e:s[4][1]['native']['client'][0]['slots'][0].update(serverId=72))
        self.reject(lambda j,s,k,e:s[6][1]['native'].update(providerVisible=False))
    def test_decoded_output_search_impossible_search_and_clear_are_real(self):
        self.reject(lambda j,s,k,e:s[4][1]['native']['client'][0].update(search='stone'))
        self.reject(lambda j,s,k,e:s[5][1]['native']['client'][0].update(rows=1))
        self.reject(lambda j,s,k,e:s[5][1]['native']['client'][0].update(slots=[copy.deepcopy(s[4][1]['native']['client'][0]['slots'][0])]))
        self.reject(lambda j,s,k,e:s[6][1]['widgets']['client'][0]['widgets'][0].update(value='diamond'))
    def test_typed_foreign_inventory_refusal_recovery_and_no_hidden_owned_items(self):
        self.reject(lambda j,s,k,e:s[0][1]['ledger']['inventory'][12].update(signature=s[0][1]['ledger']['inventory'][12]['signature'].replace('[B;','[I;')))
        self.reject(lambda j,s,k,e:s[7][1]['ledger']['cursor'].update(count=0))
        self.reject(lambda j,s,k,e:s[8][1]['ledger']['cursor'].update(count=0))
        self.reject(lambda j,s,k,e:s[9][1]['ledger']['physical'].update(hidden=copy.deepcopy(k['item'])))
    def test_real_movement_filter_change_and_persistence_required(self):
        self.reject(lambda j,s,k,e:s[10][1]['widgets']['client'][0]['bounds'].update(x=10))
        self.reject(lambda j,s,k,e:s[10][1]['widgets']['client'][0]['bounds'].update(y=20))
        self.reject(lambda j,s,k,e:s[13][1]['native']['client'][0]['settings'][0].update(active=False))
        def no_change(j,s,k,e):
            for i in [14,15]:
                for side in ['client','server']:s[i][1]['native'][side][0]['shownProviders']='ALL'
        self.reject(no_change)
    def test_final_close_has_original_pattern_foreign_items_and_no_open_menus(self):
        self.reject(lambda j,s,k,e:e.update(menus=copy.deepcopy(s[-1][1]['ledger']['menus'])))
        self.reject(lambda j,s,k,e:e['inventory'][9].update(count=0))
        self.reject(lambda j,s,k,e:e.update(cursor=copy.deepcopy(k['item'])))
        self.reject(lambda j,s,k,e:e['physical'].update(hidden=copy.deepcopy(k['item'])))


if __name__=='__main__':unittest.main(verbosity=2)
