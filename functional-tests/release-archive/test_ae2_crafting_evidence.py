"""Offline controls reject invented plans, errors, resources and parent menus."""
from pathlib import Path
import copy,importlib.util,sys,unittest
import nbtlib

ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
spec=importlib.util.spec_from_file_location('review_ae2_crafting',ROOT/'tools/reconcile-ae2-crafting-release-evidence.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
DRIVER='a'*64


def fixture(target='1.21.1/neoforge'):
    mc,loader=target.split('/');air=dict(item='minecraft:air',count=0,signature='{}')
    def item(name,count=1,tag=None):
        return dict(item=name,count=count,signature=m.ordinary(mc,name)if tag is None else m.expected_item(mc,name,1,tag))
    def key(name,amount):return dict(empty=False,item=item(name),amount=amount,amountJavaType='java.lang.Long')
    decoded=dict(type='appeng.crafting.pattern.AECraftingPattern',inputs=[dict(multiplier=1,alternatives=[key('minecraft:oak_log',1)])],outputs=[key('minecraft:oak_planks',4)])
    pattern=item('ae2:crafting_pattern',1,'{native_recipe_seed:1b,typed:[B;2b,5b]}')
    cell=item('ae2:item_storage_cell_64k',1,m.CELL);logs=item('minecraft:oak_log',16)
    marker=m.marker_item(mc);marker_nbt=nbtlib.parse_nbt(marker);marker_nbt['Count'if mc=='1.20.1'else'count']=nbtlib.Byte(1)if mc=='1.20.1'else nbtlib.Int(1)
    inventory=[dict(index=i,**copy.deepcopy(air))for i in range(41)]
    for i,value in [(9,pattern),(10,cell),(11,logs),(12,dict(item='minecraft:diamond',count=7,signature=marker_nbt.snbt()))]:inventory[i]=dict(index=i,**copy.deepcopy(value))
    sessions=[1,1,2,2,3,3,4,4,5,5,5,6,7,7,8,9,10,10,11,12,12,11,11,11,12,12,11,13,12,13,13,12,12,13,14,14,14,2,2]
    frames=[]
    for i,label in enumerate(m.LABELS):
        kind=m.KINDS[i];sid=sessions[i];menu=m.MENUS[kind]
        screen=m.ALIASES['ae2-craft-error']if i in m.ERRORS else m.SCREENS[kind]
        widgets=[]
        if kind=='CraftAmountMenu':
            amount=(8 if i==17 else 12)if i in {7,13,17}else 1
            widgets=[dict(message='',value=str(amount),visible=True,active=True),dict(message='Next',visible=True,active=True)]
        elif kind=='CraftConfirmMenu':
            names=['Retry','Replan','Cancel']if i in m.ERRORS else['Start','Cancel']
            widgets=[dict(message=name,visible=True,active=True)for name in names]
            if i==10:widgets.append(dict(message='Crafting CPU: Automatic',visible=True,active=True))
        bounds=dict(x=26 if i in {9,10,23,26}else 10,y=32 if i in {9,10,23,26}else 20,width=176,height=180)
        values=dict(getCpuAvailableBytes=dict(value=65536,javaType='java.lang.Long'))
        client=dict(session=sid,menu=menu,screen=screen,widgets=widgets,bounds=bounds,values=copy.deepcopy(values))
        server=dict(session=sid,menu=menu,values=copy.deepcopy(values))
        nc=dict(session=sid,menu=menu,screen=screen);ns=dict(session=sid,menu=menu)
        if kind=='CraftAmountMenu':nc['what']=key('minecraft:oak_planks',1);ns['what']=copy.deepcopy(nc['what'])
        if kind=='CraftConfirmMenu':
            amount=12 if i<16 else 8;error='MISSING_INGREDIENT'if i in m.ERRORS else'none'
            summary=dict(ready=True,simulation=False,bytes=32,entries=[dict(item=item('minecraft:oak_planks'),missing=0,stored=0,craft=amount)])
            nc.update(summary=copy.deepcopy(summary),error=error,sameParentMenu=True)
            ns.update(summary=copy.deepcopy(summary),error=error,plan=dict(ready=True,simulation=False,bytes=32,
                output=key('minecraft:oak_planks',amount),used=[key('minecraft:oak_log',amount//4)],missing=[],emitted=[],patterns=[dict(pattern=copy.deepcopy(decoded),times=amount//4)]))
        inv=copy.deepcopy(inventory)
        if i>=1:inv[9]['count']=0
        if 3<=i<38:inv[10]['count']=0
        if i>=5:inv[11]['count']=0
        if 21<=i<=24 or 29<=i<=31:inv[11]=dict(inventory[11],count=13)
        if i>=36:inv[11]=dict(inventory[11],count=11)
        if i>=35:inv[13]=dict(index=13,**item('minecraft:oak_planks',20))
        cursor=dict(logs,count=13)if i==20 else copy.deepcopy(air)
        menus=[dict(session=sid,type=menu,slots=[])]
        if sid!=2 and i>=2:menus.append(dict(session=2,type=m.MENUS['DriveMenu'],slots=[]))
        if kind=='MEStorageMenu'and i in {19,20,24,25,28,31,32}:
            confirm=13 if i>=28 else 11;menus.append(dict(session=confirm,type=m.MENUS['CraftConfirmMenu'],slots=[]))
        mirrors=[];physical={}
        if 3<=i<38:physical['drive-cell']=copy.deepcopy(cell)
        if i==1:physical['provider-pattern']=copy.deepcopy(pattern)
        if kind=='CraftAmountMenu':
            raw=item('ae2:wrapped_generic_stack',1,'{amount:1L,key:"minecraft:oak_planks"}')
            mirror=dict(session=sid,slot=0,type='appeng.menu.slot.InaccessibleSlot',raw=raw,mayPlace=False,mayPickup=False)
            mirrors=[mirror];physical['inaccessible-copy']=copy.deepcopy(raw)
            menus[0]['slots']=[dict(index=0,type=mirror['type'],player=False,**copy.deepcopy(raw))]
        count=(0,0)if i<5 or i>=36 else(16,0)if i<15 else(13,12)if i<20 or 25<=i<=28 or 32<=i<=33 else(0,12)if i<34 else(11,20)if i==34 else(11,0)
        stored=[key(name,value)for name,value in zip(['minecraft:oak_log','minecraft:oak_planks'],count)if value]
        native=dict(client=[nc],server=[ns],mirrors=mirrors,provider=[copy.deepcopy(pattern)]if i>=1 else[],
            drive=[copy.deepcopy(cell)]if 3<=i<38 else[],powered=True,stored=stored,cpus=[dict(busy=False,active=False,bytes=65536,coProcessors=0)])
        frames.append((label,dict(native=native,widgets=dict(client=[client],server=[server]),ledger=dict(inventory=inv,cursor=cursor,physical=physical,menus=menus))))
    journal=dict(complete=True,cases=['native-crafting'],driver_sha256=DRIVER,
        checks=[dict(case='native-crafting',name=name,passed=True)for name in m.NAMES],
        original_entries=[dict(original=dict(id='atlas-ae2-terminal',minecraft=mc,loader=loader,expected_native_menu=m.MENUS['MEStorageMenu']),executed_world_actions=[dict(type='fixture',block='ae2:controller')])])
    seed=dict(pattern=pattern,decoded=decoded)
    closed=copy.deepcopy(frames[-1][1]['ledger']);closed.update(menus=[],physical={})
    return journal,frames,seed,closed


class CraftingEvidence(unittest.TestCase):
    def reject(self,change):
        j,f,s,end=fixture();change(j,f,s,end)
        with self.assertRaises((AssertionError,KeyError,ValueError,TypeError)):m.validate('1.21.1/neoforge',j,f,s,end,DRIVER)

    def test_all_six_targets_and_three_aliases_share_original_counts(self):
        for target in m.TARGETS:
            result,classes=m.validate(target,*fixture(target),DRIVER)
            self.assertEqual((result['assertions'],result['states'],result['native_seed']),(29,40,1))
            self.assertIn(m.MENUS['CraftConfirmMenu'],classes)
            for case,screen in m.ALIASES.items():m.specification(target,case,[screen])

    def test_original_journal_completion_driver_and_exact_checks_required(self):
        for change in [lambda j,f,s,e:j.update(complete=False),lambda j,f,s,e:j.update(driver_sha256='b'*64),
            lambda j,f,s,e:j['checks'].pop(),lambda j,f,s,e:j['checks'][0].update(passed=1)]:self.reject(change)

    def test_frames_native_seed_and_native_recipe_cannot_be_substituted(self):
        for change in [lambda j,f,s,e:f.pop(),lambda j,f,s,e:s['decoded']['outputs'][0].update(amount=5),
            lambda j,f,s,e:s['pattern'].update(count=2),lambda j,f,s,e:j['original_entries'][0]['executed_world_actions'][0].update(type='provider')]:self.reject(change)

    def test_real_plan_requires_input_output_iterations_and_authoritative_summary(self):
        for change in [lambda j,f,s,e:f[8][1]['native']['server'][0]['plan']['used'][0].update(amount=2),
            lambda j,f,s,e:f[14][1]['native']['server'][0]['plan'].update(simulation=True),
            lambda j,f,s,e:f[18][1]['native']['server'][0]['plan']['patterns'][0].update(times=3),
            lambda j,f,s,e:f[27][1]['native']['client'][0]['summary'].update(bytes=999)]:self.reject(change)

    def test_start_and_retry_must_consume_real_resources_and_create_outputs(self):
        for number in [15,34]:
            self.reject(lambda j,f,s,e,n=number:f[n][1]['native']['stored'][0].update(amount=16))
        self.reject(lambda j,f,s,e:f[35][1]['ledger']['inventory'][13].update(count=19))

    def test_cancel_replan_and_withdrawals_cannot_fake_inventory_changes(self):
        for number in [11,20,25,27,29,32]:
            self.reject(lambda j,f,s,e,n=number:f[n][1]['native']['stored'].append(dict(empty=False,item=m.key_item('1.21.1','minecraft:diamond'),amount=1,amountJavaType='java.lang.Long')))

    def test_errors_must_be_genuine_same_native_confirmation_menu(self):
        for change in [lambda j,f,s,e:f[22][1]['native']['server'][0].update(error='none'),
            lambda j,f,s,e:f[30][1]['native']['client'][0].update(sameParentMenu=False),
            lambda j,f,s,e:f[23][1]['widgets']['client'][0].update(menu='appeng.menu.me.crafting.CraftErrorMenu'),
            lambda j,f,s,e:f[19][1]['ledger']['menus'].pop()]:self.reject(change)

    def test_moved_controls_and_numeric_requests_cannot_be_lost(self):
        for change in [lambda j,f,s,e:f[9][1]['widgets']['client'][0]['bounds'].update(x=10),
            lambda j,f,s,e:f[23][1]['widgets']['client'][0]['widgets'].pop(),
            lambda j,f,s,e:f[17][1]['widgets']['client'][0]['widgets'][0].update(value='12'),
            lambda j,f,s,e:f[10][1]['widgets']['server'][0]['values']['getCpuAvailableBytes'].update(value=0)]:self.reject(change)

    def test_full_typed_metadata_mirrors_cursor_and_physical_ownership_required(self):
        for change in [lambda j,f,s,e:f[7][1]['native']['mirrors'][0].update(mayPickup=True),
            lambda j,f,s,e:f[20][1]['ledger']['cursor'].update(count=12),
            lambda j,f,s,e:f[8][1]['ledger']['physical'].update(hidden=copy.deepcopy(f[8][1]['native']['provider'][0])),
            lambda j,f,s,e:f[38][1]['ledger']['inventory'][10].update(signature=f[38][1]['ledger']['inventory'][10]['signature'].replace('101','102'))]:self.reject(change)

    def test_final_recovery_and_all_sessions_closed_required(self):
        for change in [lambda j,f,s,e:e['inventory'][11].update(count=12),
            lambda j,f,s,e:e['menus'].append(dict(session=99,type=m.MENUS['CraftConfirmMenu'],slots=[])),
            lambda j,f,s,e:e['physical'].update(hidden=copy.deepcopy(s['pattern']))]:self.reject(change)


if __name__=='__main__':unittest.main(verbosity=2)
