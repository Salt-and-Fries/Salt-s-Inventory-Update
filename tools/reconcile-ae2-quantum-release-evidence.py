"""Review actual native quantum chamber transfers, refusal and source lifetime."""
from pathlib import Path
import argparse,datetime,hashlib,json,re,sys

ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'functional-tests/release-specialized')]
from focused_release_evidence import checked,reference,verify as verify_sealed
from native_publisher_index import index,matches
from focused_release_evidence_ae2_variants import additional_jobs,combine,expected_subjects
from ae2_quantum_features import DATA,REFUSAL,SENTINEL,FREQUENCY,expected_item,conserved,has_frequency,item_cell,pair,stack

TARGETS=['1.20.1/fabric','1.20.1/forge','1.21.1/neoforge','26.1.2/neoforge','26.2/neoforge','26.3/neoforge']
CASE='atlas-ae2-quantum-network-bridge'
MENU='appeng.menu.implementations.QNBMenu'
HOST='appeng.blockentity.qnb.QuantumBridgeBlockEntity'
DRIVER='functional-tests/release-specialized/ae2_quantum_features.py'
SCHEMA='ae2-native-quantum-frequency-slot-and-source-lifetime-v1'
LABELS=['original-empty-chamber','frequency-item-picked-up','frequency-item-inserted','incompatible-item-refused',
    'native-item-recovered','physical-item-restored','quantum-window-moved','moved-slot-input','quantum-reopened','final-recovery']
NAMES=['original formed quantum chamber opens through its actual top ray',
    'native frequency item and complete typed prerequisites are exact',
    'pickup preserves full native item data and foreign inventory',
    'native slot accepts the actual nonzero-frequency item exactly once',
    'native quantum filter refuses the complete incompatible stack',
    'native pickup recovers the original frequency and all metadata',
    'physical recovery returns all exact inventory slots and clears cursor',
    'quantum window moves through actual unlocked drag input',
    'moved native slot keeps its original frequency input',
    'ordinary native reopening preserves exact stored frequency and items',
    'final native recovery settles once before source invalidation',
    'real empty-source invalidation closes native editing without item changes']
SCOPE='One genuine formed native quantum chamber per target: nonzero-frequency item, original restricted slot, actual pickup/insertion/incompatible refusal/recovery, moved input, ordinary reopening, full typed inventory/physical/cursor conservation and empty-source invalidation.'
LIMITS='Exact staged target/publisher only. Frequency item is a declared native pre-entry prerequisite. A single formed chamber and its UI do not qualify two-endpoint quantum-network transport, arbitrary remote dimensions or whole-AE2 gameplay.'


def specification(target,case,classes):
    assert target in TARGETS and case==CASE and classes==[MENU],'Unreviewed quantum alias/class/target'


def validate(target,journal,frames,invalidated,driver_hash):
    specification(target,CASE,[MENU]);mc=target.split('/')[0]
    assert journal['complete']is True and journal['cases']==['quantum-link']and journal['driver_sha256']==driver_hash
    assert [(v['case'],v['name'])for v in journal['checks']]==[('quantum-link',name)for name in NAMES]
    assert all(v.get('passed')is True for v in journal['checks'])
    entries=journal['original_entries'];assert isinstance(entries,list)and len(entries)==1
    entry=entries[0];original=entry['original']
    assert (original['minecraft'],original['loader'])==tuple(target.split('/'))and original['expected_native_menu']==MENU
    screen=original['expected_native_screen'];assert isinstance(screen,str)and screen.startswith('appeng.client.gui.')
    assert entry['executed_world_actions']and all(v['type']in{'fixture','command','server-command'}for v in entry['executed_world_actions'])
    assert [v[0]for v in frames]==LABELS
    initial=frames[0][1]['ledger'];inventory=initial['inventory']
    assert isinstance(inventory,list)and len(inventory)>=36
    assert all(type(v['index'])is int and v['index']==i for i,v in enumerate(inventory))
    for slot,item,count,metadata,quantum in [(9,'ae2:quantum_entangled_singularity',1,DATA,True),(10,'minecraft:stone',4,REFUSAL,False),(12,'minecraft:diamond',7,SENTINEL,False)]:
        actual=inventory[slot]
        assert stack(actual['signature'],actual['count'])==stack(expected_item(mc,item,count,metadata,quantum))
    qes=inventory[9];stone=inventory[10]
    empty=[dict(v)for v in inventory];empty[9]['count']=0
    refused=[dict(v)for v in empty];refused[10]['count']=0
    source_position=None;clients=[]
    for i,(_,frame)in enumerate(frames):
        assert set(frame)=={'native','client','ledger'}
        native,widgets,data=frame['native'],frame['client'],frame['ledger']
        assert native['host']==HOST and native['formed']is True
        position=native['position'];assert isinstance(position,str)and re.fullmatch(r'-?\d+,\s*-?\d+,\s*-?\d+',position)
        if source_position is None:source_position=position
        assert position==source_position,'Different native quantum source used mid-workflow'
        c,s=pair(widgets,'QNBMenu');assert c['menu']==s['menu']==MENU and c['screen']==screen
        assert type(c['session'])is int and c['session']>0 and s['session']==c['session']
        assert all(type(c['bounds'][k])is int for k in ['x','y','width','height'])and c['bounds']['width']>0 and c['bounds']['height']>0
        menus=[v for v in data['menus']if v['session']==c['session']and v['type']==MENU];assert len(menus)==1
        slots=[v for v in menus[0]['slots']if v['player']is False and v['virtual']is False and v['active']is True]
        assert len(slots)==1 and type(slots[0]['index'])is int and slots[0]['index']>=0
        assert item_cell(slots[0])==item_cell(native['item']),'Owned native slot differs from actual backing chamber'
        clients.append(c)
        stored=i in{2,3,7,8};assert has_frequency(native,FREQUENCY if stored else 0)
        if stored:assert item_cell(native['item'])==item_cell(qes)
        else:assert native['item']['count']==0 and type(native['item']['count'])is int
        expected_inventory=refused if i==3 else empty if i in{1,2,4,7,8}else inventory
        cursor=stone if i==3 else qes if i in{1,4}else None
        assert conserved(data,expected_inventory,cursor=cursor,stored=qes if stored else None),'Full physical inventory/cursor/frequency item ledger changed'
    assert clients[6]['bounds']['x']==clients[5]['bounds']['x']+16 and clients[6]['bounds']['y']==clients[5]['bounds']['y']+12
    assert invalidated['menus']==[]and conserved(invalidated,inventory),'Native empty-source invalidation did not close/settle once'
    return dict(assertions=12,states=11,native_variants=1,contract='native-single-chamber-frequency-transfer-not-two-endpoint-transport'),[MENU,HOST,screen]


def derive(root,sealed_path,expected,case,original_classes):
    root=Path(root).resolve();sealed=json.loads(Path(sealed_path).read_text());target=sealed['target']
    specification(target,case,original_classes);verify_sealed(root,sealed,expected)
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0];result=row['results']
    assert result.get('ae2_quantum_features_checked')is True
    assert type(result['cases'])is int and result['cases']==1 and type(result['failed'])is int and result['failed']==0
    assert type(result['checks'])is int and result['checks']==12
    profile=(root/row['profile']).resolve();journal_path=checked(root,sealed['acceptance']);assert journal_path==profile/'ae2-quantum-release/summary.json'
    frames=[];refs=[]
    for number,path in enumerate(sorted(journal_path.parent.glob('[0-9][0-9][0-9]-*.json')),1):
        match=re.fullmatch(r'([0-9]{3})-([a-z0-9-]+)\.json',path.name);assert match and int(match[1])==number
        ref=reference(root,path);assert ref in sealed['full_states'];refs.append(ref);frames.append((match[2],json.loads(path.read_text())))
    invalidated=journal_path.parent/'source-invalidated.json';ref=reference(root,invalidated);assert ref in sealed['full_states'];refs.append(ref)
    assert len(refs)==11
    validation,classes=validate(target,json.loads(journal_path.read_text()),frames,json.loads(invalidated.read_text()),sealed['archived_inputs'][DRIVER])
    publishers=[]
    for artifact in json.loads(checked(root,sealed['staged']).read_text())['artifacts']:
        if artifact['family']!='upstream':continue
        path=Path(artifact['file']);path=path if path.is_absolute()else root/path;raw=path.read_bytes()
        assert hashlib.sha256(raw).hexdigest()==artifact['sha256'];digest=hashlib.sha512(raw).hexdigest()
        if 'sha512'in artifact:assert artifact['sha512']==digest
        locations=matches(index(path,digest,target.split('/')[1]),classes)
        if not locations:continue
        assert {v[:-6].replace('/','.')for loc in locations for v in loc['classes']}==set(classes),'Split or incomplete native AE2 publisher'
        publishers.append(dict(filename=artifact['filename'],sha256=artifact['sha256'],sha512=digest,class_locations=locations))
    assert len(publishers)==1
    return dict(schema=SCHEMA,target=target,case=case,disposition='functional-pass',original_native_classes=original_classes,native_classes=classes,
        subjects={k:list(v)for k,v in expected.items()},sealed_evidence=reference(root,sealed_path),acceptance=reference(root,journal_path),native_states=refs,
        validation=validation,publishers=publishers,scope=SCOPE,limits=LIMITS)


def verify(root,record,expected,consumer=None,current=None):
    assert record['schema']==SCHEMA and record['disposition']=='functional-pass'
    actual=[derive(root,checked(root,v['sealed_evidence']),expected_subjects(record,v,expected,consumer,current),record['case'],record['original_native_classes'])for v in[record,*record.get('additional_workflows',[])]]
    assert combine(actual,consumer=consumer,current=current)==record
    return True


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--generation',required=True);parser.add_argument('--plan-only',action='store_true');args=parser.parse_args()
    assert re.fullmatch(r'build[0-9]+',args.generation)
    out=ROOT/'output/release-readiness';backlog=json.loads((out/'coverage-backlog.json').read_text())
    plan=json.loads((ROOT/'build/release-readiness/focused-native-plan-02.json').read_text());jobs=[v for v in plan['jobs']if v['family']=='ae2_quantum']
    assert len(jobs)==6 and {v['target']for v in jobs}==set(TARGETS);jobs={v['target']:v for v in jobs}
    rows=[r for r in backlog['records']if r['case']==CASE]
    assert len(rows)==6 and {v['target']for v in rows}==set(TARGETS)
    if args.plan_only:
        for row in rows:print(row['target'],row['case'],'requires actual sealed',jobs[row['target']]['run'])
        print('Plan only; no quantum-slot or network-transport acceptance.');return
    sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
    import importlib.util
    history_source=ROOT/'build/release-readiness/release_queue_history_455.py'
    assert hashlib.sha256(history_source.read_bytes()).hexdigest()=='fb00a5e47131d9ca30263154439a17f13ea3661eef2651febe9a9fa75fa3affc'
    assert hashlib.sha256((ROOT/'build/release-readiness/focused-native-plan-02.json').read_bytes()).hexdigest()=='9cc23717142dc649b56b8e445e86d1b900652bb580401c8de0a1750f0eb467b9'
    spec=importlib.util.spec_from_file_location('AE2_original_native_history',history_source);history=importlib.util.module_from_spec(spec);spec.loader.exec_module(history)
    resolved=history.completed(plan,json.loads((out/'focused-native-campaign-02.json').read_text()));assert len(resolved)==174
    jobs={v['target']:v for v in resolved if v['family']=='ae2_quantum'};assert set(jobs)==set(TARGETS)

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
        executions=[jobs[row['target']],*additional_jobs(ROOT,'quantum',row['target'])]
        workflows=[accepted(job,row)for job in executions]
        record=combine(workflows,consumer=consumer,current=current_subjects(row['target']))
        assert row['current_exact_pins']and {v['sha512']for v in row['current_exact_pins']}<={v['sha512']for v in record['publishers']},'Unqualified native AE2 publisher patch'
        records.append(record)
    destination=out/f'coverage-ae2-quantum-{args.generation}-01.json';assert not destination.exists()
    destination.write_text(json.dumps(dict(status='reviewed-native-ae2-quantum-workflows-reconciled',created=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        candidate_identity=candidate,artifact_equivalence=consumer.report(),auditor_inputs=[reference(ROOT,history_source),reference(ROOT,Path(__file__)),reference(ROOT,ROOT/'tools/focused_release_evidence_ae2_variants.py')],records=records,limits=LIMITS),indent=2)+'\n')
    print('Reconciled6nativequantumUIrecords from six actual workflows; no two-endpoint transport claim.')


if __name__=='__main__':main()
