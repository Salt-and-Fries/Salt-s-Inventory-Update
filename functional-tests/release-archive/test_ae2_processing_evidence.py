"""Offline rejection controls for same-parent processing child and real encoding."""
from pathlib import Path
import copy,importlib.util,sys,unittest
import nbtlib

ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
spec=importlib.util.spec_from_file_location('review_ae2_processing',ROOT/'tools/reconcile-ae2-processing-release-evidence.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
DRIVER='a'*64


def fixture(target='1.21.1/neoforge'):
    mc,loader=target.split('/');air=dict(item='minecraft:air',count=0,signature='{}')
    def cell(name,count,tag):
        parsed=nbtlib.parse_nbt(m.expected_item(mc,name,1,tag))
        return dict(item=name,count=count,signature=parsed.snbt())
    inventory=[dict(index=i,**copy.deepcopy(air))for i in range(41)]
    for i,name,count,tag in [(9,'minecraft:stone',3,m.INPUT),(10,'minecraft:diamond',2,m.OUTPUT),(11,'ae2:blank_pattern',1,'{}')]:
        inventory[i]=dict(index=i,**cell(name,count,tag))
    marker=nbtlib.parse_nbt(m.marker_item(mc));marker['Count'if mc=='1.20.1'else'count']=nbtlib.Byte(1)if mc=='1.20.1'else nbtlib.Int(1)
    inventory[12]=dict(index=12,item='minecraft:diamond',count=7,signature=marker.snbt())
    inp,out,blank=[{k:v for k,v in x.items()if k!='index'}for x in inventory[9:12]]
    encoded=cell('ae2:processing_pattern',1,'{native_encoded_fixture:1b,typed:[B;4b,7b]}')
    def amount(key,value):
        if value is None:return dict(empty=True)
        single=copy.deepcopy(key);single['count']=1
        return dict(empty=False,item=single,amount=value,amountJavaType='java.lang.Long')
    amounts=[(None,None),(None,None),(3,None),(3,2),(3,2),(3,2),(3,2),(37,2),(37,2),(37,2),(37,2),
        (37,5),(37,5),(37,5),(37,5),(37,5),(37,5),(None,5),(None,None)]
    children={5:(inp,3),6:(inp,3),8:(out,2),10:(out,2),16:(inp,37)};frames=[]
    for i,label in enumerate(m.LABELS):
        sid=17 if i<12 else 18;screen=m.SCREEN if i in children else m.PARENT_SCREEN
        a,b=amounts[i];inputs=[amount(inp,a)]+[dict(empty=True)for _ in range(8)];outputs=[amount(out,b)]+[dict(empty=True)for _ in range(2)]
        nc=dict(session=sid,screen=screen,sameParentMenu=True,mode='CRAFTING'if i==0 else'PROCESSING',
            inputs=[dict(decoded=copy.deepcopy(v))for v in inputs],outputs=[dict(decoded=copy.deepcopy(v))for v in outputs],
            modes=[dict(mode='PROCESSING',visible=True,active=True)])
        ns=dict(session=sid,mode=nc['mode'],inputs=inputs,outputs=outputs,blank=copy.deepcopy(blank if i==13 else air),encoded=copy.deepcopy(encoded if i==14 else air))
        if i==14:ns['decoded']=dict(type='appeng.crafting.pattern.AEProcessingPattern',
            inputs=[dict(multiplier=37,alternatives=[amount(inp,1)])],outputs=[amount(out,5)])
        widgets=[]
        if i in children:
            key,value=children[i];nc['current']=amount(key,value)
            widgets=[dict(type='net.minecraft.client.gui.components.EditBox',message='',value=str(value),visible=True,active=True),
                dict(type='appeng.client.gui.widgets.ActionButton',message='Set',visible=True,active=True)]
            if i==8:widgets.append(dict(type='appeng.client.gui.widgets.TabButton',message='',visible=True,active=True))
        if i==13:widgets=[dict(type='appeng.client.gui.widgets.ActionButton',message='',visible=True,active=True,tooltips=['Encode Pattern'])]
        c=dict(session=sid,menu=m.MENU,screen=screen,widgets=widgets,bounds=dict(x=26 if i==6 else 10,y=32 if i==6 else 20,width=176,height=120))
        server=dict(session=sid,menu=m.MENU);inv=copy.deepcopy(inventory);cursor=copy.deepcopy(air);physical={}
        if i in{2,3}:inv[9 if i==2 else 10]['count']=0;cursor=copy.deepcopy(inp if i==2 else out)
        if i in{13,14}:inv[11]['count']=0;physical['native-pattern-slot']=copy.deepcopy(blank if i==13 else encoded)
        if i>=15:inv[11]=dict(index=11,**copy.deepcopy(encoded))
        data=dict(inventory=inv,physical=physical,cursor=cursor,menus=[dict(session=sid,type=m.MENU,slots=[])])
        frames.append((label,dict(native=dict(client=[nc],server=[ns]),widgets=dict(client=[c],server=[server]),ledger=data)))
    journal=dict(complete=True,cases=['processing-amount'],driver_sha256=DRIVER,
        checks=[dict(case='processing-amount',name=name,passed=True)for name in m.NAMES],
        original_entries=[dict(original=dict(minecraft=mc,loader=loader,expected_native_menu=m.MENU,expected_native_screen=m.PARENT_SCREEN),
            executed_world_actions=[dict(type='fixture',block='ae2:controller')])])
    closed=copy.deepcopy(frames[-1][1]['ledger']);closed['menus']=[]
    return journal,frames,closed


class ProcessingEvidence(unittest.TestCase):
    def reject(self,change):
        j,s,end=fixture();change(j,s,end)
        with self.assertRaises((AssertionError,KeyError,ValueError,TypeError)):m.validate('1.21.1/neoforge',j,s,end,DRIVER)
    def test_six_targets_require_twenty_original_checks_and_states(self):
        for target in m.TARGETS:
            j,s,end=fixture(target);v,classes=m.validate(target,j,s,end,DRIVER)
            self.assertEqual(v,dict(assertions=20,states=20,native_variants=1,contract='native-local-processing-child-real-encoding-not-downstream-production'))
            self.assertIn('appeng.crafting.pattern.AEProcessingPattern',classes)
    def test_exact_ordered_checks_driver_complete_and_frame_set_required(self):
        for change in [lambda j,s,e:j.update(complete=1),lambda j,s,e:j.update(driver_sha256='b'*64),
            lambda j,s,e:j['checks'].pop(),lambda j,s,e:j['checks'][0].update(passed=1),
            lambda j,s,e:j['checks'].reverse(),lambda j,s,e:s.pop()]:self.reject(change)
    def test_exact_entry_and_actual_native_parent_child_correspondence(self):
        self.reject(lambda j,s,e:j['original_entries'][0]['original'].update(loader='fabric'))
        self.reject(lambda j,s,e:j['original_entries'][0]['executed_world_actions'][0].update(type='provider'))
        self.reject(lambda j,s,e:s[5][1]['widgets']['client'][0].update(screen=m.PARENT_SCREEN))
        self.reject(lambda j,s,e:s[5][1]['native']['client'][0].update(sameParentMenu=False))
        self.reject(lambda j,s,e:s[5][1]['widgets']['server'][0].update(session=99))
    def test_local_child_cannot_replace_the_parent_menu_session(self):
        def replace(j,s,e):
            for row in s[5][1]['widgets']['client']+s[5][1]['widgets']['server']+s[5][1]['native']['client']+s[5][1]['native']['server']+s[5][1]['ledger']['menus']:row['session']=91
        self.reject(replace)
        self.reject(lambda j,s,e:s[6][1]['widgets']['client'][0]['bounds'].update(x=10))
        self.reject(lambda j,s,e:s[6][1]['widgets']['client'][0]['bounds'].update(y=20))
    def test_full_typed_prerequisites_and_ghost_template_cursor_conservation(self):
        self.reject(lambda j,s,e:s[0][1]['ledger']['inventory'][9].update(count=2))
        self.reject(lambda j,s,e:s[0][1]['ledger']['inventory'][12].update(signature=s[0][1]['ledger']['inventory'][12]['signature'].replace('[B;','[I;')))
        self.reject(lambda j,s,e:s[2][1]['ledger']['cursor'].update(count=0))
        self.reject(lambda j,s,e:s[3][1]['ledger']['inventory'][10].update(count=2))
    def test_original_mode_config_types_and_packet_values_required(self):
        self.reject(lambda j,s,e:s[1][1]['native']['server'][0].update(mode='CRAFTING'))
        self.reject(lambda j,s,e:s[7][1]['native']['server'][0]['inputs'][0].update(amount=36))
        self.reject(lambda j,s,e:s[7][1]['native']['server'][0]['inputs'][0].update(amountJavaType='java.lang.Integer'))
        self.reject(lambda j,s,e:s[7][1]['native']['client'][0]['inputs'][0]['decoded'].update(amount=36))
    def test_cancel_reopen_set_zero_and_clear_retain_exact_native_semantics(self):
        self.reject(lambda j,s,e:s[9][1]['native']['server'][0]['outputs'][0].update(amount=41))
        self.reject(lambda j,s,e:s[10][1]['native']['client'][0]['current'].update(amount=41))
        self.reject(lambda j,s,e:s[12][1]['native']['server'][0]['inputs'][0].update(amount=3))
        self.reject(lambda j,s,e:s[17][1]['native']['server'][0]['inputs'].__setitem__(0,copy.deepcopy(s[16][1]['native']['server'][0]['inputs'][0])))
    def test_real_blank_consumption_native_decoding_and_exact_encoded_recovery(self):
        self.reject(lambda j,s,e:s[13][1]['ledger']['physical'].update(hidden=copy.deepcopy(s[13][1]['native']['server'][0]['blank'])))
        self.reject(lambda j,s,e:s[14][1]['native']['server'][0]['blank'].update(count=1))
        self.reject(lambda j,s,e:s[14][1]['native']['server'][0]['decoded']['inputs'][0].update(multiplier=36))
        self.reject(lambda j,s,e:s[14][1]['native']['server'][0]['decoded']['outputs'][0].update(amount=4))
        self.reject(lambda j,s,e:s[15][1]['ledger']['inventory'][11].update(count=0))
    def test_original_controls_and_initial_child_values_must_be_visible(self):
        self.reject(lambda j,s,e:s[0][1]['native']['client'][0]['modes'][0].update(active=False))
        self.reject(lambda j,s,e:s[8][1]['widgets']['client'][0]['widgets'][2].update(visible=False))
        self.reject(lambda j,s,e:s[16][1]['widgets']['client'][0]['widgets'][0].update(value='3'))
        self.reject(lambda j,s,e:s[13][1]['widgets']['client'][0]['widgets'][0].update(tooltips=[]))
    def test_final_close_requires_recovered_pattern_and_no_hidden_owned_stacks(self):
        self.reject(lambda j,s,e:e.update(menus=copy.deepcopy(s[-1][1]['ledger']['menus'])))
        self.reject(lambda j,s,e:e['inventory'][11].update(count=0))
        self.reject(lambda j,s,e:e.update(cursor=copy.deepcopy(e['inventory'][11])))
        self.reject(lambda j,s,e:e['physical'].update(hidden=copy.deepcopy(e['inventory'][11])))


if __name__=='__main__':unittest.main(verbosity=2)
