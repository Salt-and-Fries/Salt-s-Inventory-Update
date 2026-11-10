"""Review actual native provider insertion and remote pattern-access inputs."""
from pathlib import Path
import argparse,datetime,hashlib,json,re,sys

ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'functional-tests/release-specialized')]
from focused_release_evidence import checked,reference,verify as verify_sealed
from native_publisher_index import index,matches
from focused_release_evidence_ae2_variants import additional_jobs,combine,expected_subjects
from ae2_pattern_features import REFUSAL,decoded_fixture,synchronized,contents,conserved,expected_item,item_cell,stack,marker_item,pair

TARGETS=['1.20.1/fabric','1.20.1/forge','1.21.1/neoforge','26.1.2/neoforge','26.2/neoforge','26.3/neoforge']
CASE='atlas-ae2-pattern-access-terminal'
MENU='appeng.menu.implementations.PatternAccessTermMenu'
SCREEN='appeng.client.gui.me.patternaccess.PatternAccessTermScreen'
PROVIDER_MENU='appeng.menu.implementations.PatternProviderMenu'
PROVIDER_SCREEN='appeng.client.gui.implementations.PatternProviderScreen'
DRIVER='functional-tests/release-specialized/ae2_pattern_features.py'
SCHEMA='ae2-native-provider-remote-pattern-search-filter-transfers-v1'
LABELS=['original-empty-provider','native-pattern-picked-up','native-provider-inserted','actual-populated-pattern-access',
    'decoded-output-search','impossible-pattern-search','native-search-cleared','native-incompatible-remote-input',
    'native-remote-pattern-recovered','native-remote-pattern-returned','pattern-access-moved','moved-remote-pattern-pickup',
    'encoded-item-restored-to-player','moved-remote-pattern-insertion','native-shown-provider-filter',
    'pattern-access-ordinary-reopening','final-complete-native-recovery']
NAMES=['genuine native encoder ingredient decodes with exact native input and output',
    'actual provider starts empty with original restricted pattern cells',
    'original pickup preserves the complete encoded pattern and typed foreign items',
    'native restricted provider slot accepts the complete encoded item exactly once',
    'ordinary original terminal receives actual populated provider records',
    'original search matches the native decoded output name',
    'impossible search hides rows without changing native records or owned items',
    'clearing original search restores the same complete remote pattern',
    'native remote-slot filter refuses incompatible items without losing the pattern',
    'original remote pickup removes exactly the native encoded provider item',
    'original remote placement returns the complete pattern to its actual provider',
    'pattern terminal moves through actual unlocked drag with unchanged records',
    'moved original remote slot still addresses the correct native provider',
    'real remote recovery restores every original player slot and clears cursor',
    'moved original remote input can insert the full pattern again',
    'original shown-provider control changes its real synchronized native setting',
    'ordinary terminal reopening retains native filter and complete physical pattern',
    'final original remote withdrawal settles all items once',
    'native pattern sessions close with original inventory and empty cursor']
SCOPE='One native-encoded declared pre-entry player ingredient, actual restricted provider insertion, populated real remote pattern records, original native search behavior (vintage output-name limitation explicitly retained), positive provider-name/output-name and impossible searches, native incompatible refusal, full typed remote recovery/return/moved input, synchronized shown-provider filter and ordinary reopening.'
LIMITS='Exact staged publisher/target only. The fixture seeds only one encoded player ingredient before entry; it does not fill provider/client records. This workflow does not qualify downstream processing output, exhaustive provider topology or large-list scrolling. Vintage processing-pattern output-name search retains the upstream empty vanilla ItemStack parse and no-match; an additional original provider-name positive search is required. No SIU repair of upstream filtering is claimed. Counts are20checks/19states for vintage and19checks/18states for modern.'


def specification(target,case,classes):
    assert target in TARGETS and case==CASE and classes==[MENU],'Unreviewed pattern-access alias/class/target'


def validate(target,journal,frames,seed,closed,driver_hash):
    specification(target,CASE,[MENU]);mc=target.split('/')[0]
    legacy=mc=='1.20.1'
    names=list(NAMES)
    if legacy:names[5:6]=['vintage output-name search agrees with the unchanged native upstream filter','original provider-name search retains the same complete native pattern']
    assert journal['complete']is True and journal['cases']==['pattern-access']and journal['driver_sha256']==driver_hash
    assert [(v['case'],v['name'])for v in journal['checks']]==[('pattern-access',name)for name in names]
    assert all(v.get('passed')is True for v in journal['checks'])
    assert len(journal['original_entries'])==1
    entry=journal['original_entries'][0];original=entry['original']
    assert (original['minecraft'],original['loader'])==tuple(target.split('/'))and original['expected_native_menu']==MENU and original['expected_native_screen']==SCREEN
    assert entry['executed_world_actions']and all(v['type']in{'fixture','command','server-command'}for v in entry['executed_world_actions'])
    expected_labels=list(LABELS)
    if legacy:expected_labels[4:5]=['native-output-search-baseline','provider-name-search']
    assert [v[0]for v in frames]==expected_labels and decoded_fixture(seed)
    if legacy:
        baseline_frame=frames[4][1];native=baseline_frame['native'];c,_=pair(baseline_frame['widgets'],'PatternAccessTermMenu')
        meta=next(v for v in native['client']if v['session']==c['session'])
        assert meta['nativeOutputSearchContract']['query']=='diamond'and meta['nativeOutputSearchContract']['matches']is False
        assert meta['search']=='diamond'and meta['rows']==0 and meta['slots']==[]and synchronized(native,meta)
        before=frames[3][1];assert contents(native['provider'])==contents(before['native']['provider'])
        assert {k:v for k,v in baseline_frame['ledger'].items()if k!='gameTime'}=={k:v for k,v in before['ledger'].items()if k!='gameTime'},'Upstream no-match search altered owned/menu state'
        frames=frames[:4]+[('decoded-output-search',frames[5][1])]+frames[6:]
    inventory=frames[0][1]['ledger']['inventory']
    assert isinstance(inventory,list)and len(inventory)>=36 and all(type(v['index'])is int and v['index']==i for i,v in enumerate(inventory))
    pattern=seed['item'];assert item_cell(inventory[9])==item_cell(pattern)
    assert stack(inventory[10]['signature'],inventory[10]['count'])==stack(expected_item(mc,'minecraft:stone',3,REFUSAL))
    assert stack(inventory[12]['signature'],inventory[12]['count'])==stack(marker_item(mc))
    stone=inventory[10];empty=[dict(v)for v in inventory];empty[9]['count']=0
    refused=[dict(v)for v in empty];refused[10]['count']=0
    provider_empty=frames[0][1]['native']['provider'];assert provider_empty and all(item_cell(v)==(0,None)for v in provider_empty)
    provider_full=[dict(v)for v in provider_empty];provider_full[0]=pattern
    clients=[];metadata=[]
    full={2,3,4,5,6,7,9,10,13,14,15}
    for i,(_,frame)in enumerate(frames):
        assert set(frame)=={'native','widgets','ledger'}
        provider=i<3;menu=PROVIDER_MENU if provider else MENU;screen=PROVIDER_SCREEN if provider else SCREEN
        c,s=pair(frame['widgets'],menu.rsplit('.',1)[-1]);assert c['menu']==s['menu']==menu and c['screen']==screen
        assert type(c['session'])is int and c['session']>0 and c['session']==s['session']
        assert all(type(c['bounds'][k])is int for k in ['x','y','width','height'])and c['bounds']['width']>0 and c['bounds']['height']>0
        native=frame['native'];rows=[v for v in native['client']if(v['session'],v['menu'])==(c['session'],menu)];assert len(rows)==1;meta=rows[0]
        assert native['providerVisible']is True and contents(native['provider'])==contents(provider_full if i in full else provider_empty)
        expected=inventory if i in{0,12,16}else refused if i==7 else empty
        cursor=stone if i==7 else pattern if i in{1,8,11}else None
        data=frame['ledger'];assert len([v for v in data['menus']if(v['session'],v['type'])==(c['session'],menu)])==1
        assert conserved(data,expected,cursor=cursor,stored=pattern if i==2 else None)
        if provider:
            slots=[v for v in meta['slots']if v['type'].endswith('.RestrictedInputSlot')and v['containerIndex']==0 and v['visible']is True]
            assert len(slots)==1 and item_cell(slots[0]['item'])==item_cell(native['provider'][0])
        else:
            assert synchronized(native,meta),'Native remote record packets differ from actual backing provider'
            assert type(meta['rows'])is int and meta['rows']>=0
            assert isinstance(meta['shownProviders'],str)and meta['shownProviders']
            wanted=('pattern provider'if legacy else'diamond')if i==4 else'siu_no_pattern_985'if i==5 else''
            assert meta['search']==wanted
            boxes=[v for v in c['widgets']if 'value'in v and v['visible']is True and v['active']is True]
            assert any(v['value']==wanted for v in boxes),'Actual native search box absent or stale'
            if i==5:assert meta['rows']==0 and meta['slots']==[]
            elif i in{3,4,6,15}:
                slots=[v for v in meta['slots']if v['type'].endswith('.PatternSlot')and v['containerIndex']==0 and v['visible']is True]
                assert meta['rows']>0 and len(slots)==1 and item_cell(slots[0]['item'])==item_cell(pattern)
                assert type(slots[0]['serverId'])is int and slots[0]['serverId']==meta['records'][0]['serverId']
        clients.append(c);metadata.append(meta)
    assert clients[10]['bounds']['x']==clients[9]['bounds']['x']+16 and clients[10]['bounds']['y']==clients[9]['bounds']['y']+12
    before=metadata[13]['shownProviders'];after=metadata[14]['shownProviders'];assert before!=after and metadata[15]['shownProviders']==after
    assert len([v for v in metadata[13]['settings']if v['currentValue']==before and v['visible']is True and v['active']is True])==1
    assert closed['menus']==[]and conserved(closed,inventory)
    return dict(assertions=20 if legacy else 19,states=19 if legacy else 18,native_seed=1,native_variants=1,contract='native-provider-remote-input-and-search-not-processing-production'),[MENU,SCREEN,PROVIDER_MENU,PROVIDER_SCREEN,'appeng.blockentity.crafting.PatternProviderBlockEntity']


def derive(root,sealed_path,expected,case,original_classes):
    root=Path(root).resolve();sealed=json.loads(Path(sealed_path).read_text());target=sealed['target']
    specification(target,case,original_classes);verify_sealed(root,sealed,expected)
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0];result=row['results']
    assert result.get('ae2_pattern_features_checked')is True
    assert type(result['cases'])is int and result['cases']==1 and type(result['failed'])is int and result['failed']==0
    assert type(result['checks'])is int and result['checks']==(20 if target.startswith('1.20.1/')else 19)
    profile=(root/row['profile']).resolve();path=checked(root,sealed['acceptance']);assert path==profile/'ae2-pattern-release/summary.json'
    frames=[];refs=[]
    for number,state in enumerate(sorted(path.parent.glob('[0-9][0-9][0-9]-*.json')),1):
        match=re.fullmatch(r'([0-9]{3})-([a-z0-9-]+)\.json',state.name);assert match and int(match[1])==number
        ref=reference(root,state);assert ref in sealed['full_states'];refs.append(ref);frames.append((match[2],json.loads(state.read_text())))
    closed=path.parent/'closed.json';ref=reference(root,closed);assert ref in sealed['full_states'];refs.append(ref)
    seed=profile/'release-ae2-pattern-fixture.json';seed_ref=reference(root,seed)
    if target.startswith('1.20.1/'):
        baseline=profile/'release-ae2-pattern-native-output-baseline.json';value=json.loads(baseline.read_text())
        import nbtlib
        tag=nbtlib.parse_nbt(value['nativeEncodedOutputTag'])
        assert str(tag['id'])=='minecraft:diamond'and int(tag['#'])==1 and 'Count'not in tag
        assert value['nativeItemStackReader']['count']==0
        baseline_ref=reference(root,baseline)
    validation,classes=validate(target,json.loads(path.read_text()),frames,json.loads(seed.read_text()),json.loads(closed.read_text()),sealed['archived_inputs'][DRIVER]);assert len(refs)==(19 if target.startswith('1.20.1/')else 18)
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
        subjects={k:list(v)for k,v in expected.items()},sealed_evidence=reference(root,sealed_path),acceptance=reference(root,path),native_states=refs,native_seed=seed_ref,native_upstream_baseline=baseline_ref if target.startswith('1.20.1/')else None,
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
    plan=json.loads((ROOT/'build/release-readiness/focused-native-plan-02.json').read_text());jobs=[v for v in plan['jobs']if v['family']=='ae2_pattern']
    assert len(jobs)==6 and {v['target']for v in jobs}==set(TARGETS);jobs={v['target']:v for v in jobs}
    rows=[r for r in backlog['records']if r['case']==CASE];assert len(rows)==6 and {v['target']for v in rows}==set(TARGETS)
    for row in rows:specification(row['target'],row['case'],row.get('historical_native_classes',row['native_classes']))
    if args.plan_only:
        for row in rows:print(row['target'],row['case'],'requires actual sealed',jobs[row['target']]['run'])
        print('Plan only; no native remote-pattern acceptance.');return
    sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
    import importlib.util
    history_source=ROOT/'build/release-readiness/release_queue_history_455.py'
    assert hashlib.sha256(history_source.read_bytes()).hexdigest()=='fb00a5e47131d9ca30263154439a17f13ea3661eef2651febe9a9fa75fa3affc'
    assert hashlib.sha256((ROOT/'build/release-readiness/focused-native-plan-02.json').read_bytes()).hexdigest()=='9cc23717142dc649b56b8e445e86d1b900652bb580401c8de0a1750f0eb467b9'
    spec=importlib.util.spec_from_file_location('AE2_original_native_history',history_source);history=importlib.util.module_from_spec(spec);spec.loader.exec_module(history)
    resolved=history.completed(plan,json.loads((out/'focused-native-campaign-02.json').read_text()));assert len(resolved)==174
    jobs={v['target']:v for v in resolved if v['family']=='ae2_pattern'};assert set(jobs)==set(TARGETS)

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
        executions=[jobs[row['target']],*additional_jobs(ROOT,'pattern',row['target'])]
        workflows=[accepted(job,row)for job in executions]
        record=combine(workflows,consumer=consumer,current=current_subjects(row['target']))
        assert row['current_exact_pins']and {v['sha512']for v in row['current_exact_pins']}<={v['sha512']for v in record['publishers']}
        records.append(record)
    destination=out/f'coverage-ae2-pattern-{args.generation}-01.json';assert not destination.exists()
    destination.write_text(json.dumps(dict(status='reviewed-native-ae2-pattern-workflows-reconciled',created=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        candidate_identity=candidate,artifact_equivalence=consumer.report(),auditor_inputs=[reference(ROOT,history_source),reference(ROOT,Path(__file__)),reference(ROOT,ROOT/'tools/focused_release_evidence_ae2_variants.py')],records=records,limits=LIMITS),indent=2)+'\n')
    print('Reconciled six remote pattern-access records; no downstream production claim.')


if __name__=='__main__':main()
