"""Bind the three native crafting screens to real recipe/error/retry evidence."""
from pathlib import Path
import argparse,copy,datetime,hashlib,json,re,sys

ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'functional-tests/release-specialized')]
from focused_release_evidence import checked,reference,verify as verify_sealed
from native_publisher_index import index,matches
from focused_release_evidence_ae2_variants import combine,expected_subjects
from focused_release_evidence_remaining_pins import additional
from ae2_crafting_features import Crafting,CELL,cellshell,fixture_pattern,key_item,ordinary,plan_matches
from ae2_quantum_features import expected_item
from ae2_settings_features import item_cell,stack,marker_item

TARGETS=['1.20.1/fabric','1.20.1/forge','1.21.1/neoforge','26.1.2/neoforge','26.2/neoforge','26.3/neoforge']
BASE_MENU='appeng.menu.me.crafting.'
BASE_SCREEN='appeng.client.gui.me.crafting.'
ALIASES={'ae2-craft-amount':BASE_SCREEN+'CraftAmountScreen',
    'ae2-craft-confirm':BASE_SCREEN+'CraftConfirmScreen','ae2-craft-error':BASE_SCREEN+'CraftErrorScreen'}
MENUS={'PatternProviderMenu':'appeng.menu.implementations.PatternProviderMenu',
    'DriveMenu':'appeng.menu.implementations.DriveMenu','MEStorageMenu':'appeng.menu.me.common.MEStorageMenu',
    'CraftAmountMenu':BASE_MENU+'CraftAmountMenu','CraftConfirmMenu':BASE_MENU+'CraftConfirmMenu'}
SCREENS={'PatternProviderMenu':'appeng.client.gui.implementations.PatternProviderScreen',
    'DriveMenu':'appeng.client.gui.implementations.DriveScreen',
    'MEStorageMenu':'appeng.client.gui.me.common.MEStorageScreen',
    'CraftAmountMenu':ALIASES['ae2-craft-amount'],'CraftConfirmMenu':ALIASES['ae2-craft-confirm']}
DRIVER='functional-tests/release-specialized/ae2_crafting_features.py'
SCHEMA='ae2-real-crafting-amount-plan-cancel-missing-replan-retry-v1'
LABELS=['original-provider','pattern-inserted','original-drive','cell-inserted','original-terminal','logs-deposited',
    'first-amount','first-amount-edited','first-native-plan','confirmation-moved','native-cpu-selected','native-plan-cancelled',
    'accepted-amount','accepted-amount-edited','accepted-native-plan','first-job-complete','second-amount','second-amount-edited',
    'second-native-plan','second-real-terminal','planned-materials-withdrawn','withdrawal-stored','genuine-native-missing-error',
    'native-error-moved','second-terminal-restoration','ingredients-restored','native-error-replan-ready','native-error-replanned',
    'second-terminal-rewithdrawal','native-replanned-materials-withdrawn','second-genuine-native-error',
    'second-terminal-retry-restoration','native-retry-ingredients-restored','error-retry-ready','native-retry-complete',
    'crafted-output-recovered','remaining-inputs-recovered','empty-cell-recovery','native-empty-cell-recovered']
KINDS=['PatternProviderMenu']*2+['DriveMenu']*2+['MEStorageMenu']*2+['CraftAmountMenu']*2+['CraftConfirmMenu']*3+['MEStorageMenu']+\
    ['CraftAmountMenu']*2+['CraftConfirmMenu','MEStorageMenu']+['CraftAmountMenu']*2+['CraftConfirmMenu']+['MEStorageMenu']*2+\
    ['CraftConfirmMenu']*3+['MEStorageMenu']*2+['CraftConfirmMenu']*2+['MEStorageMenu']+['CraftConfirmMenu']*2+\
    ['MEStorageMenu']*2+['CraftConfirmMenu']+['MEStorageMenu']*3+['DriveMenu']*2
NAMES=['genuine world recipe ingredient and powered empty native network prerequisites',
    'original provider accepts the real pattern with complete item data',
    'real drive cell enters through original slot and preserves full typed shell',
    'real repository deposit stores exactly sixteen ordinary logs',
    'first real amount child edits original numeric input without owned changes',
    'first real calculation delivers matching complete original plan',
    'native computed plan and controls remain intact after window drag',
    'original CPU cycle selects actual available capacity without item mutation',
    'original confirmation Cancel returns without consuming ingredients or starting job',
    'accepted real amount child edits original numeric input without owned changes',
    'accepted real calculation delivers matching complete original plan',
    'original Start really consumes three logs and creates twelve planks',
    'second real amount child edits original numeric input without owned changes',
    'second real calculation delivers matching complete original plan',
    'independent ordinary terminal opens while original confirmation stays alive',
    'real second terminal withdraws required ingredients without native plan assignment',
    'planned ingredients move to actual player inventory while calculated plan stays native',
    'original Start receives genuine missing-ingredient local error on same server menu',
    'real error window moves with intact original Retry Replan Cancel controls',
    'original repository restores exact withdrawn ingredients before retry',
    'original Replan calculates a fresh complete plan after actual ingredient restoration',
    'actual second withdrawal removes the newly replanned ingredients without changing plan',
    'second original Start independently reaches genuine missing-ingredient error',
    'original second repository restores every ingredient before genuine Retry',
    'original Retry consumes two more logs and creates eight more planks',
    'all twenty real crafted planks recover to player with exact item data',
    'eleven unconsumed original logs recover with no extra stored keys',
    'actual empty storage cell recovers preserving all non-storage metadata',
    'all real crafting and child sessions close with exact final resources']
ERRORS={22,23,26,30,33}
SCOPE='One real powered native crafting network: player-only encoded recipe ingredient, actual provider/drive insertion and sixteen-log deposit; original amount inputs and calculated plan; moved confirmation/CPU selection/Cancel; real Start producing twelve planks. Two actual ingredient withdrawals induce genuine missing-ingredient errors on the existing confirmation menu. Original Cancel returns to the amount child and its native return tab restores the terminal; a retained Drive session is used when present, otherwise ordinary world use reopens the same Drive after native transitions closed it. Native Replan does not consume resources; native Retry then produces eight more planks. Exact recovery, cell shell and complete typed player/physical/cursor ledgers. The three aliases share each target execution and do not add executions or assertions.'
LIMITS='Exact staged publisher/target only. No fabricated error menu: CraftErrorScreen retains CraftConfirmMenu. This workflow does not certify all recipes, processing-machine outputs, exhaustive CPU topology or a transport network. Each of six executions has29 original assertions,40 original states and3 ordinary placement/native Cancel-return/Drive-reentry observations, plus any sealed original Items-filter prerequisite observations and one native pre-entry seed receipt; eighteen coverage records are aliases of those six executions.'


def specification(target,case,classes):
    assert target in TARGETS and case in ALIASES and classes==[ALIASES[case]],'Unreviewed native crafting alias/class/target'


def selected(frame,kind):
    menu=MENUS[kind]
    clients=[v for v in frame['widgets']['client']if v['menu']==menu]
    assert len(clients)==1,'Expected one original visible native menu'
    client=clients[0];sid=client['session'];assert type(sid)is int and sid>0
    servers=[v for v in frame['widgets']['server']if v['session']==sid and v['menu']==menu];assert len(servers)==1
    native=frame['native'];cs=[v for v in native['client']if v['session']==sid];ss=[v for v in native['server']if v['session']==sid]
    assert len(cs)==len(ss)==1 and cs[0]['menu']==ss[0]['menu']==menu
    assert cs[0]['screen']==client['screen']
    assert len([v for v in frame['ledger']['menus']if v['session']==sid and v['type']==menu])==1
    assert all(type(client['bounds'][k])is int for k in ['x','y','width','height'])
    assert client['bounds']['width']>0 and client['bounds']['height']>0
    return client,servers[0],cs[0],ss[0]


def visible(client,message):
    return [v for v in client['widgets']if v['message']==message and v['visible']is True and v['active']is True]


def validate(target,journal,frames,seed,closed,driver_hash):
    assert target in TARGETS;mc,loader=target.split('/')
    assert journal['complete']is True and journal['cases']==['native-crafting']and journal['driver_sha256']==driver_hash
    assert [(v['case'],v['name'])for v in journal['checks']]==[('native-crafting',name)for name in NAMES]
    assert all(v.get('passed')is True for v in journal['checks'])
    assert len(journal['original_entries'])==1
    entry=journal['original_entries'][0];original=entry['original']
    assert (original['minecraft'],original['loader'])==(mc,loader)
    assert original['id']=='atlas-ae2-terminal' and original['expected_native_menu']==MENUS['MEStorageMenu']
    assert entry['executed_world_actions']and all(v['type']in {'fixture','command','server-command'}for v in entry['executed_world_actions'])
    assert len(frames)==len(KINDS)==39 and [v[0]for v in frames]==LABELS
    inventory=copy.deepcopy(frames[0][1]['ledger']['inventory'])
    assert isinstance(inventory,list)and len(inventory)>=36 and all(type(v['index'])is int and v['index']==i for i,v in enumerate(inventory))
    pattern,cell,logs=seed['pattern'],inventory[10],inventory[11]
    assert pattern['item']=='ae2:crafting_pattern'and type(pattern['count'])is int and pattern['count']==1
    assert fixture_pattern(seed['decoded'],mc)and item_cell(inventory[9])==item_cell(pattern)
    assert stack(cell['signature'],cell['count'])==stack(expected_item(mc,'ae2:item_storage_cell_64k',1,CELL))
    assert stack(logs['signature'],logs['count'])==stack(ordinary(mc,'minecraft:oak_log',16))
    assert stack(inventory[12]['signature'],inventory[12]['count'])==stack(marker_item(mc))
    recovered=copy.deepcopy(frames[38][1]['ledger']['inventory'][10])
    assert cellshell(recovered,mc)==cellshell(cell,mc)
    probe=Crafting.__new__(Crafting);probe.mc=mc;probe.pattern=pattern;probe.cell=cell;probe.logs=logs
    clients=[]
    for i,(_,frame)in enumerate(frames):
        assert set(frame)=={'native','widgets','ledger'}
        client,server,nc,ns=selected(frame,KINDS[i]);clients.append(client)
        if i in ERRORS:
            assert KINDS[i]=='CraftConfirmMenu'and client['screen']==ALIASES['ae2-craft-error']
            assert nc['sameParentMenu']is True and nc['error']==ns['error']=='MISSING_INGREDIENT'
            assert all(len(visible(client,name))==1 for name in ['Retry','Replan','Cancel'])
        elif KINDS[i]=='CraftAmountMenu':
            assert client['screen']==SCREENS['CraftAmountMenu']and nc['what']==ns['what']
            assert nc['what']['empty']is False and item_cell(nc['what']['item'])[1]==item_cell(key_item(mc,'minecraft:oak_planks'))[1]
            assert len(visible(client,'Next'))==1
        elif KINDS[i]=='CraftConfirmMenu':
            assert client['screen']==SCREENS['CraftConfirmMenu']and nc['summary']==ns['summary']
            assert nc['summary']['ready']is True and nc['summary']['simulation']is False
            assert nc['error']==ns['error']=='none'
            assert plan_matches(ns['plan'],mc,12 if i<16 else 8)
            assert len(visible(client,'Start'))==len(visible(client,'Cancel'))==1
        else:
            assert client['screen']==SCREENS[KINDS[i]]
        expected=copy.deepcopy(inventory)
        if i>=1:expected[9]['count']=0
        if 3<=i<38:expected[10]['count']=0
        if i>=38:expected[10]=copy.deepcopy(recovered)
        if i>=5:expected[11]['count']=0
        if 21<=i<=24 or 29<=i<=31:expected[11]=dict(logs,count=13)
        if i>=36:expected[11]=dict(logs,count=11)
        if i>=35:expected[13]=dict(expected[13],item='minecraft:oak_planks',count=20,signature=key_item(mc,'minecraft:oak_planks')['signature'])
        probe.native=frame['native'];cursor=dict(logs,count=13)if i==20 else None
        assert probe.owned(frame['ledger'],expected,cursor=cursor,provider=i>=1,cell=3<=i<38),'Owned item/cursor/mirror ledger changed'
        stock=(0,0)if i<5 or i>=36 else(16,0)if i<15 else(13,12)if i<20 or 25<=i<=28 or 32<=i<=33 else(0,12)if i<34 else(11,20)if i==34 else(11,0)
        assert probe.stock(*stock),'Actual network item counter disagrees with resource transition'
        if i in {0,1,3,5,11,15,22,27,30,34,36}:assert probe.idle(),'Actual native CPU not idle at the original completion boundary'
        if i in {7,13,17}:
            amount=8 if i==17 else 12
            assert len([v for v in client['widgets']if v.get('value')==str(amount)and v['visible']is True and v['active']is True])==1
        if i==10:
            assert len([v for v in client['widgets']if v['visible']is True and v['active']is True and v['message'].startswith('Crafting CPU:')])==1
            assert all(type(v['values']['getCpuAvailableBytes']['value'])is int and v['values']['getCpuAvailableBytes']['value']>=65536 for v in [client,server])
    for before,after in [(0,1),(2,3),(6,7),(12,13),(16,17),(8,9),(9,10),(18,21),(21,22),(22,23),(23,26),(27,29),(29,30),(30,33),(37,38)]:
        assert clients[before]['session']==clients[after]['session'],'Original native parent/session was replaced'
    for before,after in [(8,9),(22,23)]:
        assert clients[after]['bounds']['x']==clients[before]['bounds']['x']+16 and clients[after]['bounds']['y']==clients[before]['bounds']['y']+12
    confirm=clients[18]['session'];other=clients[19]['session'];assert other!=confirm
    assert any(v['session']==confirm and v['type']==MENUS['CraftConfirmMenu']for v in frames[19][1]['ledger']['menus'])
    assert all(clients[i]['session']==other for i in [20,24,25,28,31,32])
    final=copy.deepcopy(inventory);final[9]['count']=0;final[10]=recovered;final[11]=dict(logs,count=11)
    final[13]=dict(final[13],item='minecraft:oak_planks',count=20,signature=key_item(mc,'minecraft:oak_planks')['signature'])
    assert closed['menus']==[]and probe.owned(closed,final,cell=False)
    classes=sorted(set([*ALIASES.values(),MENUS['CraftAmountMenu'],MENUS['CraftConfirmMenu'],
        MENUS['MEStorageMenu'],MENUS['PatternProviderMenu'],MENUS['DriveMenu'],
        'appeng.crafting.pattern.AECraftingPattern','appeng.server.testplots.CraftingPatternHelper']))
    return dict(assertions=29,states=40,native_seed=1,native_variants=3,contract='real-native-crafting-cancel-two-genuine-errors-replan-retry'),classes



def native_return_observations(root, sealed, folder, frames, mc):
    from copy import deepcopy
    paths = [folder/name for name in ['ordinary-parent-placement.json', 'native-cancel-return.json', 'ordinary-drive-reentry.json']]
    refs = [reference(root, path) for path in paths]
    assert all(ref in sealed['full_states'] for ref in refs), 'Every ordinary placement/native return/reentry observation must be sealed'
    placement, cancel, reentry = [json.loads(path.read_text()) for path in paths]
    before, after = placement['before'], placement['after']
    assert before['menu'] == after['menu'] == MENUS['MEStorageMenu']
    assert before['session'] == after['session']
    assert after['bounds'] == dict(before['bounds'], y=40)
    assert placement['action'] == dict(type='regression-move', session=before['session'], dx=0, dy=40-before['bounds']['y'])
    assert after in placement['observed']['client']
    assert selected(frames[6][1], 'CraftAmountMenu')[0]['bounds']['y'] == 40
    assert selected(frames[8][1], 'CraftConfirmMenu')[0]['bounds']['y'] == 40
    client, server, nc, ns = selected(cancel, 'CraftAmountMenu')
    assert client['screen'] == SCREENS['CraftAmountMenu'] and nc['what'] == ns['what']
    what = nc['what']
    assert what['empty'] is False and what['amountJavaType'] == 'java.lang.Long' and type(what['amount']) is int and what['amount'] == 12
    assert item_cell(what['item'])[1] == item_cell(key_item(mc, 'minecraft:oak_planks'))[1]
    assert any(v.get('value') == '12' and v['visible'] is True and v['active'] is True for v in client['widgets'])
    tabs = [v for v in client['widgets'] if v['type'].endswith('.TabButton') and v['visible'] is True and v['active'] is True]
    assert tabs == [cancel['return_tab']]
    inventory = deepcopy(frames[0][1]['ledger']['inventory'])
    probe = Crafting.__new__(Crafting); probe.mc = mc; probe.pattern = deepcopy(inventory[9]); probe.cell = deepcopy(inventory[10])
    for index in [9, 10, 11]: inventory[index]['count'] = 0
    probe.native = cancel['native']
    assert probe.owned(cancel['ledger'], inventory) and probe.stock(16, 0) and probe.idle(), 'Native Cancel consumed or changed original resources'
    assert client['session'] != selected(frames[10][1], 'CraftConfirmMenu')[0]['session']
    old_protocol='build/release-readiness/ae2_crafting_native_returns_1228.py' in sealed['archived_inputs']
    new_protocol='build/release-readiness/ae2_crafting_native_returns_1243.py' in sealed['archived_inputs']
    assert old_protocol != new_protocol
    if new_protocol:
        reopened=reentry['reopened'];assert type(reopened) is bool
    else:
        assert mc=='1.20.1' and 'reopened' not in reentry
        reopened=True
    before_drives=[v for v in reentry['before']['menus'] if v['type']==MENUS['DriveMenu']]
    drives=[v for v in reentry['after']['client'] if v['menu']==MENUS['DriveMenu']]
    assert len(drives)==1 and drives[0]['session']==selected(frames[37][1], 'DriveMenu')[0]['session']
    if reopened:
        assert not before_drives
        assert drives[0]['session']!=selected(frames[2][1], 'DriveMenu')[0]['session']
        assert reentry['ray']==dict(dx=-.5,dy=2,dz=.5,yaw=0,pitch=90)
    else:
        assert len(before_drives)==1 and reentry['ray'] is None
        assert before_drives[0]['session']==drives[0]['session']==selected(frames[2][1], 'DriveMenu')[0]['session']
    return refs

def derive(root,sealed_path,expected,case,original_classes):
    root=Path(root).resolve();sealed=json.loads(Path(sealed_path).read_text());target=sealed['target']
    specification(target,case,original_classes);verify_sealed(root,sealed,expected)
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0];result=row['results']
    assert result.get('ae2_crafting_features_checked')is True and type(result['cases'])is int and result['cases']==1
    assert type(result['failed'])is int and result['failed']==0 and type(result['checks'])is int and result['checks']==29
    profile=(root/row['profile']).resolve();path=checked(root,sealed['acceptance']);assert path==profile/'ae2-crafting-release/summary.json'
    frames=[];refs=[]
    for number,state in enumerate(sorted(path.parent.glob('[0-9][0-9][0-9]-*.json')),1):
        match=re.fullmatch(r'([0-9]{3})-([a-z0-9-]+)\.json',state.name);assert match and int(match[1])==number
        ref=reference(root,state);assert ref in sealed['full_states'];refs.append(ref);frames.append((match[2],json.loads(state.read_text())))
    closed=path.parent/'closed.json';ref=reference(root,closed);assert ref in sealed['full_states'];refs.append(ref)
    seed=profile/'release-ae2-crafting-fixture.json';seed_ref=reference(root,seed)
    validation,classes=validate(target,json.loads(path.read_text()),frames,json.loads(seed.read_text()),json.loads(closed.read_text()),sealed['archived_inputs'][DRIVER]);assert len(refs)==40
    extras=native_return_observations(root,sealed,path.parent,frames,target.split('/')[0]);refs.extend(extras)
    from ae2_native_item_filter_evidence import derive as native_filter_observations
    filters=native_filter_observations(root,sealed,path.parent);refs.extend(filters)
    validation.update(states=43+len(filters),original_states=40,native_return_observations=3,native_item_filter_observations=len(filters))
    publishers=[]
    for artifact in json.loads(checked(root,sealed['staged']).read_text())['artifacts']:
        if artifact['family']!='upstream':continue
        upstream=Path(artifact['file']);upstream=upstream if upstream.is_absolute()else root/upstream;raw=upstream.read_bytes()
        assert hashlib.sha256(raw).hexdigest()==artifact['sha256'];digest=hashlib.sha512(raw).hexdigest()
        if 'sha512'in artifact:assert artifact['sha512']==digest
        locations=matches(index(upstream,digest,target.split('/')[1]),classes)
        if not locations:continue
        assert {v[:-6].replace('/','.')for loc in locations for v in loc['classes']}==set(classes)
        publishers.append(dict(filename=artifact['filename'],sha256=artifact['sha256'],sha512=digest,class_locations=locations))
    assert len(publishers)==1
    return dict(schema=SCHEMA,target=target,case=case,disposition='functional-pass',original_native_classes=original_classes,native_classes=classes,
        subjects={k:list(v)for k,v in expected.items()},sealed_evidence=reference(root,sealed_path),acceptance=reference(root,path),native_states=refs,native_seed=seed_ref,
        validation=validation,publishers=publishers,scope=SCOPE,limits=LIMITS)


def verify(root,record,expected,consumer=None,current=None):
    assert record['schema']==SCHEMA and record['disposition']=='functional-pass'
    values=[derive(root,checked(root,v['sealed_evidence']),expected_subjects(record,v,expected,consumer,current),record['case'],record['original_native_classes'])for v in[record,*record.get('additional_workflows',[])]]
    assert combine(values,consumer=consumer,current=current)==record
    return True


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--generation',required=True);parser.add_argument('--plan-only',action='store_true');args=parser.parse_args()
    assert re.fullmatch(r'build[0-9]+',args.generation)
    out=ROOT/'output/release-readiness';backlog=json.loads((out/'coverage-backlog.json').read_text())
    plan=json.loads((ROOT/'build/release-readiness/focused-native-plan-02.json').read_text());jobs=[v for v in plan['jobs']if v['family']=='ae2_crafting']
    assert len(jobs)==6 and {v['target']for v in jobs}==set(TARGETS);jobs={v['target']:v for v in jobs}
    rows=[r for r in backlog['records']if r['case']in ALIASES]
    assert len(rows)==18 and {(v['target'],v['case'])for v in rows}=={(t,c)for t in TARGETS for c in ALIASES}
    for row in rows:specification(row['target'],row['case'],row.get('historical_native_classes',row['native_classes']))
    if args.plan_only:
        for row in rows:print(row['target'],row['case'],'requires actual sealed',jobs[row['target']]['run'])
        print('Plan only; no native crafting/error acceptance.');return
    sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
    import importlib.util
    history_source=ROOT/'build/release-readiness/release_queue_history_455.py'
    assert hashlib.sha256(history_source.read_bytes()).hexdigest()=='fb00a5e47131d9ca30263154439a17f13ea3661eef2651febe9a9fa75fa3affc'
    assert hashlib.sha256((ROOT/'build/release-readiness/focused-native-plan-02.json').read_bytes()).hexdigest()=='9cc23717142dc649b56b8e445e86d1b900652bb580401c8de0a1750f0eb467b9'
    spec=importlib.util.spec_from_file_location('AE2_original_native_history',history_source);history=importlib.util.module_from_spec(spec);spec.loader.exec_module(history)
    resolved=history.completed(plan,json.loads((out/'focused-native-campaign-02.json').read_text()));assert len(resolved)==174
    jobs={v['target']:v for v in resolved if v['family']=='ae2_crafting'};assert set(jobs)==set(TARGETS)

    from subject_artifacts import current_subjects
    from release_candidate_identity import current
    from focused_release_evidence_core_log_consumer import verified
    candidate=current(ROOT,verify_worktree=True);records=[]
    consumer=verified(ROOT,args.generation,candidate)
    def accepted(job,row):
        sealed=out/('focused-evidence-'+job['run']+'.json')
        expected=consumer.expected(row['target'],json.loads(sealed.read_text()),current_subjects(row['target']))
        return derive(ROOT,sealed,expected,row['case'],row.get('historical_native_classes',row['native_classes']))

    for row in rows:
        workflows=[accepted(job,row)for job in[jobs[row['target']],*additional(ROOT,'crafting',row['target'])]]
        record=combine(workflows,consumer=consumer,current=current_subjects(row['target']))
        assert row['current_exact_pins']and {v['sha512']for v in row['current_exact_pins']}<={v['sha512']for v in record['publishers']}
        records.append(record)
    destination=out/f'coverage-ae2-crafting-{args.generation}-02.json';assert not destination.exists()
    destination.write_text(json.dumps(dict(status='reviewed-native-ae2-crafting-workflows-reconciled',created=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        candidate_identity=candidate,artifact_equivalence=consumer.report(),auditor_inputs=[reference(ROOT,history_source),reference(ROOT,Path(__file__)),reference(ROOT,ROOT/'tools/focused_release_evidence_remaining_pins.py'),reference(ROOT,ROOT/'tools/focused_release_evidence_ae2_variants.py')],previous_report=reference(ROOT,out/f'coverage-ae2-crafting-{args.generation}-01.json'),records=records,unique_executions=7,limits=LIMITS),indent=2)+'\n')
    print('Reconciled18 native screen records from seven original executions; no added assertions or synthetic error menu.')


if __name__=='__main__':main()
