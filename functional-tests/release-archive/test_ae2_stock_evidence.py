"""Offline rejection controls for native stock-child and inaccessible-copy evidence."""
from pathlib import Path
import copy,importlib.util,sys,unittest
import nbtlib

ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
spec=importlib.util.spec_from_file_location('review_ae2_stock',ROOT/'tools/reconcile-ae2-stock-release-evidence.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
DRIVER='a'*64


def fixture(target='1.21.1/neoforge'):
    mc,loader=target.split('/');air=dict(item='minecraft:air',count=0,signature='{}')
    def item(name,count,text):
        tag=nbtlib.parse_nbt(text);tag['Count'if mc=='1.20.1'else'count']=nbtlib.Byte(1)if mc=='1.20.1'else nbtlib.Int(1)
        return dict(item=name,count=count,signature=tag.snbt())
    inventory=[dict(index=i,**copy.deepcopy(air))for i in range(41)]
    inventory[9]=dict(index=9,**item('minecraft:stone',3,m.expected_item(mc,'minecraft:stone',3,m.TEMPLATE)))
    inventory[12]=dict(index=12,**item('minecraft:diamond',7,m.marker_item(mc)))
    key=copy.deepcopy(inventory[9]);key['count']=1;frames=[]
    amounts=[None,3,3,3,3,37,37,41,41,None]
    for i,label in enumerate(m.LABELS):
        amount=amounts[i];child=i in{3,4,6};sid=17 if not child else 31 if i<5 else 32
        config=[dict(empty=True)for _ in range(9)];storage=[dict(empty=True)for _ in range(9)]
        if amount is not None:config[0]=dict(empty=False,item=copy.deepcopy(key),amount=amount,amountJavaType='java.lang.Long')
        mirrors=[];physical={};slots=[];widgets=[];values={}
        if child:
            # Native SetStockAmountMenu displays one immutable key; requested
            # quantity remains in getInitialAmount and the real ghost config.
            raw=copy.deepcopy(key);raw.pop('index',None)
            decoded=copy.deepcopy(config[0]);decoded['amount']=1
            mirrors=[dict(session=sid,slot=0,raw=copy.deepcopy(raw),decoded=decoded,mayPlace=False,mayPickup=False)]
            physical['native-display-copy']=copy.deepcopy(raw)
            slots=[dict(index=0,type='appeng.menu.slot.InaccessibleSlot',player=False,virtual=False,active=True,**raw)]
            widgets=[dict(type='net.minecraft.client.gui.components.EditBox',message='',value=str(amount),visible=True,active=True),
                dict(type='appeng.client.gui.widgets.ActionButton',message='Set',visible=True,active=True)]
            values=dict(getInitialAmount=dict(javaType='java.lang.Integer',value=amount))
        elif amount is not None:widgets=[dict(type=m.PARENT_SCREEN+'$SetAmountButton',message='',visible=True,active=True)]
        menu=m.MENU if child else m.PARENT;screen=m.SCREEN if child else m.PARENT_SCREEN
        c=dict(session=sid,menu=menu,screen=screen,widgets=widgets,values=copy.deepcopy(values),
            bounds=dict(x=26 if i==4 else 10,y=32 if i==4 else 20,width=176,height=120))
        server=dict(session=sid,menu=menu,values=copy.deepcopy(values))
        inv=copy.deepcopy(inventory);cursor=copy.deepcopy(air)
        if i==1:inv[9]['count']=0;cursor=copy.deepcopy(inventory[9])
        cells=[dict(index=j,x=j*18,y=0,visible=True,decoded=copy.deepcopy(v))for j,v in enumerate(config)]
        native=dict(host=m.HOST,config=config,storage=storage,mirrors=mirrors,client=[]if child else[dict(session=sid,left=10,top=20,cells=cells)])
        data=dict(inventory=inv,physical=physical,cursor=cursor,menus=[dict(session=sid,type=menu,slots=slots)])
        frames.append((label,dict(native=native,widgets=dict(client=[c],server=[server]),ledger=data)))
    journal=dict(complete=True,cases=['stock-amount'],driver_sha256=DRIVER,
        checks=[dict(case='stock-amount',name=name,passed=True)for name in m.NAMES],
        original_entries=[dict(original=dict(minecraft=mc,loader=loader,expected_native_menu=m.PARENT,expected_native_screen=m.PARENT_SCREEN),
            executed_world_actions=[dict(type='fixture',block='ae2:interface')])])
    closed=dict(inventory=copy.deepcopy(inventory),physical={},cursor=copy.deepcopy(air),menus=[])
    return journal,frames,closed


class StockEvidence(unittest.TestCase):
    def reject(self,change):
        j,s,end=fixture();change(j,s,end)
        with self.assertRaises((AssertionError,KeyError,ValueError,TypeError)):m.validate('1.21.1/neoforge',j,s,end,DRIVER)
    def test_six_targets_require_real_child_and_eleven_checks_and_states(self):
        for target in m.TARGETS:
            j,s,end=fixture(target);v,classes=m.validate(target,j,s,end,DRIVER)
            self.assertEqual(v,dict(assertions=11,states=11,native_variants=1,contract='native-ghost-stock-child-not-positive-network-stocking'))
            self.assertIn('appeng.menu.slot.InaccessibleSlot',classes)
    def test_complete_original_order_driver_and_all_immediate_states_required(self):
        for change in [lambda j,s,e:j.update(complete=1),lambda j,s,e:j.update(driver_sha256='b'*64),
            lambda j,s,e:j['checks'].pop(),lambda j,s,e:j['checks'][0].update(passed=1),
            lambda j,s,e:j['checks'].reverse(),lambda j,s,e:s.pop()]:self.reject(change)
    def test_exact_parent_child_screen_menu_host_target_and_sessions(self):
        self.reject(lambda j,s,e:j['original_entries'][0]['original'].update(loader='fabric'))
        self.reject(lambda j,s,e:j['original_entries'][0]['executed_world_actions'][0].update(type='provider'))
        self.reject(lambda j,s,e:s[3][1]['widgets']['client'][0].update(menu=m.PARENT))
        self.reject(lambda j,s,e:s[3][1]['widgets']['client'][0].update(screen=m.PARENT_SCREEN))
        self.reject(lambda j,s,e:s[4][1]['native'].update(host='other.InterfaceBlockEntity'))
        self.reject(lambda j,s,e:s[4][1]['widgets']['server'][0].update(session=99))
    def test_typed_native_config_initial_values_and_real_storage_exact(self):
        self.reject(lambda j,s,e:s[5][1]['native']['config'][0].update(amount=36))
        self.reject(lambda j,s,e:s[5][1]['native']['config'][0].update(amountJavaType='java.lang.Integer'))
        self.reject(lambda j,s,e:s[3][1]['widgets']['client'][0]['values']['getInitialAmount'].update(value=3.0))
        self.reject(lambda j,s,e:s[5][1]['native']['storage'].__setitem__(0,copy.deepcopy(s[5][1]['native']['config'][0])))
    def test_ghost_selection_conserves_physical_template_and_complete_typed_foreign_data(self):
        self.reject(lambda j,s,e:s[0][1]['ledger']['inventory'][9].update(count=2))
        self.reject(lambda j,s,e:s[0][1]['ledger']['inventory'][12].update(signature=s[0][1]['ledger']['inventory'][12]['signature'].replace('[B;','[I;')))
        self.reject(lambda j,s,e:s[1][1]['ledger']['cursor'].update(count=0))
        self.reject(lambda j,s,e:s[2][1]['ledger']['inventory'][9].update(count=0))
    def test_dialog_mirror_must_be_native_inaccessible_and_not_owned_or_editable(self):
        self.reject(lambda j,s,e:s[3][1]['native']['mirrors'][0].update(mayPickup=True))
        self.reject(lambda j,s,e:s[3][1]['native']['mirrors'][0].update(mayPlace=True))
        self.reject(lambda j,s,e:s[3][1]['native']['mirrors'][0].update(session=99))
        self.reject(lambda j,s,e:s[3][1]['ledger']['menus'][0]['slots'][0].update(type='appeng.menu.slot.FakeSlot'))
        self.reject(lambda j,s,e:s[3][1]['ledger']['menus'][0]['slots'][0].update(count=2))
    def test_hidden_owned_stacks_and_unaccounted_dialog_copies_cannot_pass(self):
        self.reject(lambda j,s,e:s[4][1]['ledger']['physical'].update(hidden=copy.deepcopy(s[4][1]['native']['mirrors'][0]['raw'])))
        self.reject(lambda j,s,e:s[2][1]['ledger']['physical'].update(hidden=copy.deepcopy(s[2][1]['ledger']['inventory'][9])))
        self.reject(lambda j,s,e:s[6][1]['native']['mirrors'][0]['decoded'].update(amount=3))
    def test_parent_ghost_packet_sync_and_visible_native_controls_required(self):
        self.reject(lambda j,s,e:s[5][1]['native']['client'][0]['cells'][0]['decoded'].update(amount=36))
        self.reject(lambda j,s,e:s[2][1]['native']['client'][0]['cells'][0].update(visible=False))
        self.reject(lambda j,s,e:s[3][1]['widgets']['client'][0]['widgets'][0].update(active=False))
        self.reject(lambda j,s,e:s[6][1]['widgets']['client'][0]['widgets'][0].update(value='3'))
        self.reject(lambda j,s,e:s[3][1]['widgets']['client'][0]['widgets'][1].update(visible=False))
    def test_moved_child_input_reopening_and_original_clear_are_required(self):
        self.reject(lambda j,s,e:s[4][1]['widgets']['client'][0]['bounds'].update(x=10))
        self.reject(lambda j,s,e:s[4][1]['widgets']['client'][0]['bounds'].update(y=20))
        self.reject(lambda j,s,e:s[8][1]['native']['config'][0].update(amount=37))
        self.reject(lambda j,s,e:s[9][1]['native']['config'].__setitem__(0,copy.deepcopy(s[8][1]['native']['config'][0])))
    def test_close_must_clear_all_sessions_and_preserve_every_owned_item(self):
        self.reject(lambda j,s,e:e.update(menus=copy.deepcopy(s[-1][1]['ledger']['menus'])))
        self.reject(lambda j,s,e:e['inventory'][9].update(count=0))
        self.reject(lambda j,s,e:e.update(cursor=copy.deepcopy(e['inventory'][9])))
        self.reject(lambda j,s,e:e['physical'].update(hidden=copy.deepcopy(e['inventory'][12])))


if __name__=='__main__':unittest.main(verbosity=2)
