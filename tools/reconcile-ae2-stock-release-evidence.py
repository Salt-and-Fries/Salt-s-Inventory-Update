"""Review native Interface ghost configuration and the real stock-amount child."""
from pathlib import Path
import argparse,datetime,hashlib,json,re,sys

ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'functional-tests/release-specialized')]
from focused_release_evidence import checked,reference,verify as verify_sealed
from native_publisher_index import index,matches
from focused_release_evidence_ae2_variants import combine,expected_subjects
from focused_release_evidence_remaining_pins import additional
from ae2_stock_features import TEMPLATE,config_matches,owned,inventory_cells,counter,valid_config,item_cell,stack,marker_item,expected_item,number,pair

TARGETS=['1.20.1/fabric','1.20.1/forge','1.21.1/neoforge','26.1.2/neoforge','26.2/neoforge','26.3/neoforge']
CASE='ae2-stock-amount'
PARENT='appeng.menu.implementations.InterfaceMenu'
MENU='appeng.menu.implementations.SetStockAmountMenu'
SCREEN='appeng.client.gui.me.crafting.SetStockAmountScreen'
PARENT_SCREEN='appeng.client.gui.implementations.InterfaceScreen'
HOST='appeng.blockentity.misc.InterfaceBlockEntity'
DRIVER='functional-tests/release-specialized/ae2_stock_features.py'
SCHEMA='ae2-native-ghost-stock-amount-inaccessible-mirror-v1'
LABELS=['original-empty-interface','native-ghost-selection','template-restored','original-stock-amount-child',
    'stock-child-moved','native-stock37-return','stock-child-reopened','native-stock41-return',
    'interface-ordinary-reopened','native-config-cleared']
NAMES=['actual native interface opens with exact typed prerequisites and empty storage',
    'native ghost selection preserves held physical items and their complete key',
    'ghost selection leaves the whole original player inventory intact',
    'original stock child reflects the actual selected key and initial amount3',
    'stock child moves through original unlocked drag while preserving all items',
    'moved numeric input and original Set update the real stock config to37',
    'ordinary child reopening receives persisted native amount37',
    'second original numeric Set retains exact key and server amount41',
    'ordinary parent source reopening preserves native stock41 and full key data',
    'native empty-cursor ghost clear removes config without physical changes',
    'stock sessions close with original inventory and no hidden physical items']
SCOPE='Native Interface ghost template, real SetStockAmountMenu child, initial3 then original Set37/41, moved numeric input, parent/child reopening, exact native synchronized key/config, ghost clear and full typed physical inventory/cursor preservation. Native inaccessible one-item display keys are decoded and accounted separately from native Integer initial amounts and Long backend quantities.'
LIMITS='Exact staged publisher/target only. Real interface storage remains empty; ghost configuration and the amount child do not qualify positive network stocking, crafting or arbitrary network topology. A native inaccessible display copy is not an owned item or an excuse to ignore hidden physical stacks.'


native_display_owned=owned


def owned(data,native,inventory,key,initial=None,cursor=None):
    if initial is not None and not config_matches(native,key,initial):return False
    return native_display_owned(data,native,inventory,key,initial=1 if initial is not None else None,cursor=cursor)


def specification(target,case,classes):
    assert target in TARGETS and case==CASE and classes==[SCREEN],'Unreviewed stock-amount alias/class/target'


def validate(target,journal,frames,closed,driver_hash):
    specification(target,CASE,[SCREEN]);mc=target.split('/')[0]
    assert journal['complete']is True and journal['cases']==['stock-amount']and journal['driver_sha256']==driver_hash
    assert [(v['case'],v['name'])for v in journal['checks']]==[('stock-amount',name)for name in NAMES]
    assert all(v.get('passed')is True for v in journal['checks'])
    assert len(journal['original_entries'])==1
    entry=journal['original_entries'][0];original=entry['original']
    assert (original['minecraft'],original['loader'])==tuple(target.split('/'))
    assert original['expected_native_menu']==PARENT and original['expected_native_screen']==PARENT_SCREEN
    assert entry['executed_world_actions']and all(v['type']in{'fixture','command','server-command'}for v in entry['executed_world_actions'])
    assert [v[0]for v in frames]==LABELS
    inventory=frames[0][1]['ledger']['inventory']
    assert isinstance(inventory,list)and len(inventory)>=36 and all(type(v['index'])is int and v['index']==i for i,v in enumerate(inventory))
    key=inventory[9]
    assert stack(key['signature'],key['count'])==stack(expected_item(mc,'minecraft:stone',3,TEMPLATE))
    assert stack(inventory[12]['signature'],inventory[12]['count'])==stack(marker_item(mc))
    empty9=[dict(v)for v in inventory];empty9[9]['count']=0;clients=[]
    amounts=[None,3,3,3,3,37,37,41,41,None]
    for i,(_,frame)in enumerate(frames):
        assert set(frame)=={'native','widgets','ledger'}
        native=frame['native'];assert native['host']==HOST
        child=i in{3,4,6};menu=MENU if child else PARENT;screen=SCREEN if child else PARENT_SCREEN
        c,s=pair(frame['widgets'],menu.rsplit('.',1)[-1])
        assert c['menu']==s['menu']==menu and c['screen']==screen
        assert type(c['session'])is int and c['session']>0 and s['session']==c['session']
        assert all(type(c['bounds'][k])is int for k in ['x','y','width','height'])and c['bounds']['width']>0 and c['bounds']['height']>0
        data=frame['ledger'];assert len([v for v in data['menus']if v['session']==c['session']and v['type']==menu])==1
        assert config_matches(native,key,amounts[i]),'Native config/key/typed amount or empty storage mismatch'
        assert owned(data,native,empty9 if i==1 else inventory,key,initial=3 if i in{3,4}else 37 if i==6 else None,cursor=key if i==1 else None)
        if child:
            initial=37 if i==6 else 3
            assert all(number(v,'getInitialAmount',initial,'java.lang.Integer')for v in [c,s])
            assert len(native['mirrors'])==1 and native['mirrors'][0]['session']==c['session']
            numeric=[v for v in c['widgets']if 'value'in v and v['visible']is True and v['active']is True]
            assert any(v['value']==str(initial)for v in numeric),'Original native numeric input missing or stale'
            assert len([v for v in c['widgets']if v['message']=='Set'and v['visible']is True and v['active']is True])==1
        else:
            rows=[v for v in native['client']if v['session']==c['session']];assert len(rows)==1
            cells=rows[0]['cells'];assert len(cells)==len(native['config'])and len({v['index']for v in cells})==len(cells)
            assert all(type(v['index'])is int and v['index']==j and v['decoded']==native['config'][j]for j,v in enumerate(sorted(cells,key=lambda v:v['index'])))
            assert valid_config([v['decoded']for v in cells])and cells[0]['visible']is True
            if amounts[i]is not None:
                assert len([v for v in c['widgets']if v['type'].endswith('.InterfaceScreen$SetAmountButton')and v['visible']is True and v['active']is True])==1
        clients.append(c)
    assert clients[4]['bounds']['x']==clients[3]['bounds']['x']+16 and clients[4]['bounds']['y']==clients[3]['bounds']['y']+12
    assert closed['menus']==[]and inventory_cells(closed['inventory'])==inventory_cells(inventory)and item_cell(closed['cursor'])==(0,None)and not counter(closed['physical'].values())
    return dict(assertions=11,states=11,native_variants=1,contract='native-ghost-stock-child-not-positive-network-stocking'),[PARENT,MENU,PARENT_SCREEN,SCREEN,HOST,'appeng.menu.slot.InaccessibleSlot']


def derive(root,sealed_path,expected,case,original_classes):
    root=Path(root).resolve();sealed=json.loads(Path(sealed_path).read_text());target=sealed['target']
    specification(target,case,original_classes);verify_sealed(root,sealed,expected)
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0];result=row['results']
    assert result.get('ae2_stock_features_checked')is True
    assert type(result['cases'])is int and result['cases']==1 and type(result['failed'])is int and result['failed']==0
    assert type(result['checks'])is int and result['checks']==11
    profile=(root/row['profile']).resolve();path=checked(root,sealed['acceptance']);assert path==profile/'ae2-stock-release/summary.json'
    frames=[];refs=[]
    for number,state in enumerate(sorted(path.parent.glob('[0-9][0-9][0-9]-*.json')),1):
        match=re.fullmatch(r'([0-9]{3})-([a-z0-9-]+)\.json',state.name);assert match and int(match[1])==number
        ref=reference(root,state);assert ref in sealed['full_states'];refs.append(ref);frames.append((match[2],json.loads(state.read_text())))
    closed=path.parent/'closed.json';ref=reference(root,closed);assert ref in sealed['full_states'];refs.append(ref)
    validation,classes=validate(target,json.loads(path.read_text()),frames,json.loads(closed.read_text()),sealed['archived_inputs'][DRIVER]);assert len(refs)==11
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
        subjects={k:list(v)for k,v in expected.items()},sealed_evidence=reference(root,sealed_path),acceptance=reference(root,path),native_states=refs,
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
    plan=json.loads((ROOT/'build/release-readiness/focused-native-plan-02.json').read_text());jobs=[v for v in plan['jobs']if v['family']=='ae2_stock']
    assert len(jobs)==6 and {v['target']for v in jobs}==set(TARGETS);jobs={v['target']:v for v in jobs}
    rows=[r for r in backlog['records']if r['case']==CASE];assert len(rows)==6 and {v['target']for v in rows}==set(TARGETS)
    if args.plan_only:
        for row in rows:print(row['target'],row['case'],'requires actual sealed',jobs[row['target']]['run'])
        print('Plan only; no native ghost or stock-child acceptance.');return
    sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
    import importlib.util
    history_source=ROOT/'build/release-readiness/release_queue_history_455.py'
    assert hashlib.sha256(history_source.read_bytes()).hexdigest()=='fb00a5e47131d9ca30263154439a17f13ea3661eef2651febe9a9fa75fa3affc'
    assert hashlib.sha256((ROOT/'build/release-readiness/focused-native-plan-02.json').read_bytes()).hexdigest()=='9cc23717142dc649b56b8e445e86d1b900652bb580401c8de0a1750f0eb467b9'
    spec=importlib.util.spec_from_file_location('AE2_original_native_history',history_source);history=importlib.util.module_from_spec(spec);spec.loader.exec_module(history)
    resolved=history.completed(plan,json.loads((out/'focused-native-campaign-02.json').read_text()));assert len(resolved)==174
    jobs={v['target']:v for v in resolved if v['family']=='ae2_stock'};assert set(jobs)==set(TARGETS)

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
        workflows=[accepted(job,row)for job in[jobs[row['target']],*additional(ROOT,'stock',row['target'])]]
        record=combine(workflows,consumer=consumer,current=current_subjects(row['target']))
        assert row['current_exact_pins']and {v['sha512']for v in row['current_exact_pins']}<={v['sha512']for v in record['publishers']}
        records.append(record)
    destination=out/f'coverage-ae2-stock-{args.generation}-02.json';assert not destination.exists()
    destination.write_text(json.dumps(dict(status='reviewed-native-ae2-stock-workflows-reconciled',created=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        candidate_identity=candidate,artifact_equivalence=consumer.report(),auditor_inputs=[reference(ROOT,history_source),reference(ROOT,Path(__file__)),reference(ROOT,ROOT/'tools/focused_release_evidence_remaining_pins.py'),reference(ROOT,ROOT/'tools/focused_release_evidence_ae2_variants.py')],previous_report=reference(ROOT,out/f'coverage-ae2-stock-{args.generation}-01.json'),records=records,limits=LIMITS),indent=2)+'\n')
    print('Reconciled six native stock-child records; no positive network-stocking claim.')


if __name__=='__main__':main()
