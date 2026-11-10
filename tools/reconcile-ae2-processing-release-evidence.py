"""Review the native local processing-amount child and real pattern encoding."""
from pathlib import Path
import argparse,datetime,hashlib,json,re,sys

ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'functional-tests/release-specialized')]
from focused_release_evidence import checked,reference,verify as verify_sealed
from native_publisher_index import index,matches
from focused_release_evidence_ae2_variants import combine,expected_subjects
from focused_release_evidence_remaining_pins import additional
from ae2_processing_features import INPUT,OUTPUT,array_matches,amount_cell,decoded_matches,conserved,expected_item,item_cell,stack,marker_item,pair

TARGETS=['1.20.1/fabric','1.20.1/forge','1.21.1/neoforge','26.1.2/neoforge','26.2/neoforge','26.3/neoforge']
CASE='ae2-processing-pattern-amount'
MENU='appeng.menu.me.items.PatternEncodingTermMenu'
SCREEN='appeng.client.gui.me.items.SetProcessingPatternAmountScreen'
PARENT_SCREEN='appeng.client.gui.me.items.PatternEncodingTermScreen'
DRIVER='functional-tests/release-specialized/ae2_processing_features.py'
SCHEMA='ae2-native-local-processing-amount-and-real-encoding-v1'
LABELS=['native-parent','processing-mode','ghost-inputs','ghost-outputs','both-templates-restored',
    'original-input-amount-child','input-child-moved','input37-set','original-output-child','output-cancel-back',
    'output-child-reopened','output5-set','native-parent-reopened','real-blank-inserted','native-real-pattern-encoded',
    'encoded-pattern-recovered','input-zero-child','input-zero-cleared','output-ghost-cleared']
NAMES=['original parent opens with empty config and exact typed prerequisites',
    'original mode tab changes actual native processing logic',
    'original inputs ghost retains complete physical template and key',
    'original outputs ghost retains complete physical template and key',
    'both ghost selections leave the complete inventory intact',
    'original local child retains parent menu and exact input amount3',
    'local amount child moves without duplicating menu or owned items',
    'moved original Set sends actual input amount37 to native host',
    'output child selects original full output key and amount2',
    'original back discards unapplied amount41 and preserves output2',
    'ordinary child reopening still reads unchanged output2',
    'second original Set updates only output amount5',
    'original source reopening persists both full keys and amounts',
    'real blank pattern enters original physical input exactly once',
    'original Encode consumes blank and produces correctly decoded native pattern',
    'real encoded pattern returns to player with complete item data',
    'reopened input child reflects persisted amount37',
    'original zero Set clears only input config without consuming items',
    'empty-cursor native ghost clear removes output without owned changes',
    'all processing sessions close with recovered output and original templates']
SCOPE='Original processing-mode ghost inputs/outputs and local amount child retaining its actual parent menu/session. Native Set/back/cancel, movement, reopening and zero/ghost clear. Actual blank consumed by original Encode, native decoded complete input/output keys and amounts verified, encoded item recovered with full typed inventory/physical/cursor conservation.'
LIMITS='Exact staged publisher/target only. One real encoding workflow does not qualify downstream processing-machine production, fluid encoding or exhaustive network behavior. The local screen child retains the parent menu; it is not the separate SetStockAmountMenu.'


def specification(target,case,classes):
    assert target in TARGETS and case==CASE and classes==[SCREEN],'Unreviewed processing-amount alias/class/target'


def validate(target,journal,frames,closed,driver_hash):
    specification(target,CASE,[SCREEN]);mc=target.split('/')[0]
    assert journal['complete']is True and journal['cases']==['processing-amount']and journal['driver_sha256']==driver_hash
    assert [(v['case'],v['name'])for v in journal['checks']]==[('processing-amount',name)for name in NAMES]
    assert all(v.get('passed')is True for v in journal['checks'])
    assert len(journal['original_entries'])==1
    entry=journal['original_entries'][0];original=entry['original']
    assert (original['minecraft'],original['loader'])==tuple(target.split('/'))and original['expected_native_menu']==MENU and original['expected_native_screen']==PARENT_SCREEN
    assert entry['executed_world_actions']and all(v['type']in{'fixture','command','server-command'}for v in entry['executed_world_actions'])
    assert [v[0]for v in frames]==LABELS
    inventory=frames[0][1]['ledger']['inventory']
    assert isinstance(inventory,list)and len(inventory)>=36 and all(type(v['index'])is int and v['index']==i for i,v in enumerate(inventory))
    inp,out,blank=inventory[9:12]
    assert stack(inp['signature'],inp['count'])==stack(expected_item(mc,'minecraft:stone',3,INPUT))
    assert stack(out['signature'],out['count'])==stack(expected_item(mc,'minecraft:diamond',2,OUTPUT))
    assert stack(inventory[12]['signature'],inventory[12]['count'])==stack(marker_item(mc))
    assert blank['item']=='ae2:blank_pattern'and type(blank['count'])is int and blank['count']==1
    encoded_rows=frames[14][1]['native']['server'];assert len(encoded_rows)==1
    encoded=encoded_rows[0]['encoded'];assert encoded['item']=='ae2:processing_pattern'and type(encoded['count'])is int and encoded['count']==1
    recovered=[dict(v)for v in inventory];recovered[11]=dict(index=11,**encoded)
    clients=[]
    amounts=[(None,None),(None,None),(3,None),(3,2),(3,2),(3,2),(3,2),
        (37,2),(37,2),(37,2),(37,2),(37,5),(37,5),(37,5),(37,5),(37,5),(37,5),(None,5),(None,None)]
    children={5:(inp,3),6:(inp,3),8:(out,2),10:(out,2),16:(inp,37)}
    for i,(_,frame)in enumerate(frames):
        assert set(frame)=={'native','widgets','ledger'}
        c,s=pair(frame['widgets'],'PatternEncodingTermMenu');screen=SCREEN if i in children else PARENT_SCREEN
        assert c['menu']==s['menu']==MENU and c['screen']==screen
        assert type(c['session'])is int and c['session']>0 and c['session']==s['session']
        native=frame['native'];cs=[v for v in native['client']if v['session']==c['session']];ss=[v for v in native['server']if v['session']==c['session']]
        assert len(cs)==len(ss)==1;nc,ns=cs[0],ss[0]
        assert nc['screen']==screen and nc['sameParentMenu']is True
        assert nc['mode']==ns['mode']==('CRAFTING'if i==0 else'PROCESSING')
        assert array_matches(ns['inputs'],inp,amounts[i][0])and array_matches(ns['outputs'],out,amounts[i][1])
        assert all([v['decoded']for v in nc[k]]==ns[k]for k in ['inputs','outputs'])
        data=frame['ledger'];assert len([v for v in data['menus']if v['session']==c['session']and v['type']==MENU])==1
        expected=[dict(v)for v in(recovered if i>=15 else inventory)];cursor=None;stored=None
        if i in{2,3}:expected[9 if i==2 else 10]['count']=0;cursor=inp if i==2 else out
        if i in{13,14}:expected[11]['count']=0;stored=blank if i==13 else encoded
        assert conserved(data,expected,cursor=cursor,stored=stored),'Complete real player/physical/cursor ledger changed'
        assert item_cell(ns['blank'])==(item_cell(blank)if i==13 else(0,None))
        assert item_cell(ns['encoded'])==(item_cell(encoded)if i==14 else(0,None))
        if i==14:assert decoded_matches(ns.get('decoded',{}),inp,out),'Real encoded pattern does not decode to complete input37/output5'
        if i in children:
            key,amount=children[i];assert amount_cell(nc['current'],key,amount)
            numeric=[v for v in c['widgets']if 'value'in v and v['visible']is True and v['active']is True]
            assert any(v['value']==str(amount)for v in numeric)
            assert len([v for v in c['widgets']if v['message']=='Set'and v['visible']is True and v['active']is True])==1
            if i==8:assert len([v for v in c['widgets']if v['type'].endswith('.TabButton')and v['visible']is True and v['active']is True])==1
        if i==0:assert len([v for v in nc['modes']if v['mode']=='PROCESSING'and v['visible']is True and v['active']is True])==1
        if i==13:assert len([v for v in c['widgets']if v['type'].endswith('.ActionButton')and v['visible']is True and v['active']is True and any('Encode'in text for text in v.get('tooltips',[]))])==1
        assert all(type(c['bounds'][k])is int for k in ['x','y','width','height'])and c['bounds']['width']>0 and c['bounds']['height']>0
        clients.append(c)
    for child,parent in [(5,4),(6,5),(8,7),(10,9),(16,15)]:assert clients[child]['session']==clients[parent]['session'],'Local child replaced its native parent menu/session'
    assert clients[6]['bounds']['x']==clients[5]['bounds']['x']+16 and clients[6]['bounds']['y']==clients[5]['bounds']['y']+12
    assert closed['menus']==[]and conserved(closed,recovered)
    return dict(assertions=20,states=20,native_variants=1,contract='native-local-processing-child-real-encoding-not-downstream-production'),[MENU,PARENT_SCREEN,SCREEN,'appeng.crafting.pattern.AEProcessingPattern']


def derive(root,sealed_path,expected,case,original_classes):
    root=Path(root).resolve();sealed=json.loads(Path(sealed_path).read_text());target=sealed['target']
    specification(target,case,original_classes);verify_sealed(root,sealed,expected)
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0];result=row['results']
    assert result.get('ae2_processing_features_checked')is True
    assert type(result['cases'])is int and result['cases']==1 and type(result['failed'])is int and result['failed']==0
    assert type(result['checks'])is int and result['checks']==20
    profile=(root/row['profile']).resolve();path=checked(root,sealed['acceptance']);assert path==profile/'ae2-processing-release/summary.json'
    frames=[];refs=[]
    for number,state in enumerate(sorted(path.parent.glob('[0-9][0-9][0-9]-*.json')),1):
        match=re.fullmatch(r'([0-9]{3})-([a-z0-9-]+)\.json',state.name);assert match and int(match[1])==number
        ref=reference(root,state);assert ref in sealed['full_states'];refs.append(ref);frames.append((match[2],json.loads(state.read_text())))
    closed=path.parent/'closed.json';ref=reference(root,closed);assert ref in sealed['full_states'];refs.append(ref)
    validation,classes=validate(target,json.loads(path.read_text()),frames,json.loads(closed.read_text()),sealed['archived_inputs'][DRIVER]);assert len(refs)==20
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
    plan=json.loads((ROOT/'build/release-readiness/focused-native-plan-02.json').read_text());jobs=[v for v in plan['jobs']if v['family']=='ae2_processing']
    assert len(jobs)==6 and {v['target']for v in jobs}==set(TARGETS);jobs={v['target']:v for v in jobs}
    rows=[r for r in backlog['records']if r['case']==CASE];assert len(rows)==6 and {v['target']for v in rows}==set(TARGETS)
    if args.plan_only:
        for row in rows:print(row['target'],row['case'],'requires actual sealed',jobs[row['target']]['run'])
        print('Plan only; no processing-child or encoding acceptance.');return
    sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
    import importlib.util
    history_source=ROOT/'build/release-readiness/release_queue_history_455.py'
    assert hashlib.sha256(history_source.read_bytes()).hexdigest()=='fb00a5e47131d9ca30263154439a17f13ea3661eef2651febe9a9fa75fa3affc'
    assert hashlib.sha256((ROOT/'build/release-readiness/focused-native-plan-02.json').read_bytes()).hexdigest()=='9cc23717142dc649b56b8e445e86d1b900652bb580401c8de0a1750f0eb467b9'
    spec=importlib.util.spec_from_file_location('AE2_original_native_history',history_source);history=importlib.util.module_from_spec(spec);spec.loader.exec_module(history)
    resolved=history.completed(plan,json.loads((out/'focused-native-campaign-02.json').read_text()));assert len(resolved)==174
    jobs={v['target']:v for v in resolved if v['family']=='ae2_processing'};assert set(jobs)==set(TARGETS)

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
        workflows=[accepted(job,row)for job in[jobs[row['target']],*additional(ROOT,'processing',row['target'])]]
        record=combine(workflows,consumer=consumer,current=current_subjects(row['target']))
        assert row['current_exact_pins']and {v['sha512']for v in row['current_exact_pins']}<={v['sha512']for v in record['publishers']}
        records.append(record)
    destination=out/f'coverage-ae2-processing-{args.generation}-02.json';assert not destination.exists()
    destination.write_text(json.dumps(dict(status='reviewed-native-ae2-processing-workflows-reconciled',created=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        candidate_identity=candidate,artifact_equivalence=consumer.report(),auditor_inputs=[reference(ROOT,history_source),reference(ROOT,Path(__file__)),reference(ROOT,ROOT/'tools/focused_release_evidence_remaining_pins.py'),reference(ROOT,ROOT/'tools/focused_release_evidence_ae2_variants.py')],previous_report=reference(ROOT,out/f'coverage-ae2-processing-{args.generation}-01.json'),records=records,limits=LIMITS),indent=2)+'\n')
    print('Reconciled six local processing-child records with actual encoding; no downstream production claim.')


if __name__=='__main__':main()
