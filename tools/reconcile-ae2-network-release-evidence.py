"""Review native AE2 network status, local units, topology and modern export."""
from pathlib import Path
import argparse,datetime,hashlib,json,re,sys

ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'functional-tests/release-specialized')]
from focused_release_evidence import checked,reference,verify as verify_sealed,local
from native_publisher_index import index,matches
from focused_release_evidence_ae2_variants import additional_jobs,combine,expected_subjects
from ae2_network_features import valid_status,topology,drive_count,exported_grid,ledger,stack,marker_item,pair

TARGETS=['1.20.1/fabric','1.20.1/forge','1.21.1/neoforge','26.1.2/neoforge','26.2/neoforge','26.3/neoforge']
CASE='atlas-ae2-controller-network-status'
MENU='appeng.menu.me.networktool.NetworkStatusMenu'
SCREEN='appeng.client.gui.me.networktool.NetworkStatusScreen'
DRIVER='functional-tests/release-specialized/ae2_network_features.py'
SCHEMA='ae2-native-populated-status-topology-units-export-v1'
LABELS=['actual-populated-status','power-unit-changed','status-moved','moved-unit-input',
    'native-topology-added','native-topology-removed','ordinary-status-reopened']
NAMES=['actual-ray native populated status and exact typed foreign item',
    'actual packet status matches stable native topology with two drives',
    'original power-unit input changes the actual native local config',
    'status moves through actual unlocked drag without changing data',
    'moved native power-unit control remains usable',
    'original status packet refreshes after a real connected drive is added',
    'original status refreshes after the extra native drive is removed',
    'ordinary native reopening retains units and populated original grid']
LEGACY=['pinned legacy status correctly has no invented export button','legacy native close preserves all owned items and cursor']
MODERN=['original modern export button is available with native operator permission',
    'native export produces a valid populated archive for the exact backing grid',
    'native export closes its original editing session and preserves all owned items']
SCOPE='Real powered controller/grid, populated native packet status against independently observed backend topology, two/three/two drives, native local power-unit input, movement and ordinary reopening. Modern native Export Grid and its actual populated ZIP are separately required. Full typed owned-item/cursor preservation.'
LIMITS='Exact staged publisher/target only. Read-only status/local configuration is not item transfer, exhaustive grid topology, large-list scrolling, arbitrary remote export or complete AE2 gameplay. Dynamic power averages are finite/bounded, not falsely equated across different observation ticks. Pinned legacy publishers have no Export Grid button.'


def specification(target,case,classes):
    assert target in TARGETS and case==CASE and classes==[MENU],'Unreviewed native network-status target/alias/class'


EXPORT_PRESERVATION={'file': 'output/release-readiness/AE2-native-export-preservation-1108-01/manifest.json', 'sha256': '41b13719a6b1a67034a06f1864842a94fb32a1a4e7645e9e2d595cb08cf35528'}


def review_export(root,receipt):
    root=Path(root).resolve();original=(root/receipt['file']).resolve()
    if original.is_relative_to(root):
        path=local(root,receipt['file'])
        assert exported_grid(path,receipt['grid_serial'])==receipt,'Native populated export changed or is not the original receipt'
    else:
        catalog=json.loads(checked(root,EXPORT_PRESERVATION).read_text())
        assert catalog['status']=='four-exact-native-export-archives-preserved-byte-for-byte'
        candidates=[v for v in catalog['rows']if v['original_receipt']==receipt];assert len(candidates)==1,'Unreviewed external native export receipt'
        row=candidates[0];path=checked(root,row['preserved']);actual=exported_grid(path,receipt['grid_serial'])
        assert {k:v for k,v in actual.items()if k!='file'}=={k:v for k,v in receipt.items()if k!='file'},'Preserved native export contents changed'
        checked(root,row['seal']);checked(root,row['original_acceptance']);checked(root,row['actual_server_directory_observation'])
    return reference(root,path)


def validate(target,journal,frames,export_end,driver_hash):
    specification(target,CASE,[MENU]);mc=target.split('/')[0];modern=mc!='1.20.1'
    assert journal['complete']is True and journal['cases']==['network-status']and journal['driver_sha256']==driver_hash
    assert [(v['case'],v['name'])for v in journal['checks']]==[('network-status',name)for name in NAMES+(MODERN if modern else LEGACY)]
    assert all(v.get('passed')is True for v in journal['checks'])
    assert len(journal['original_entries'])==1
    entry=journal['original_entries'][0];original=entry['original']
    assert (original['minecraft'],original['loader'])==tuple(target.split('/'))
    assert original['expected_native_menu']==MENU and original['expected_native_screen']==SCREEN
    assert entry['executed_world_actions']and all(v['type']in{'fixture','command','server-command'}for v in entry['executed_world_actions'])
    assert [v[0]for v in frames]==LABELS
    initial=frames[0][1]['ledger'];inventory=initial['inventory']
    assert isinstance(inventory,list)and len(inventory)>=36 and all(type(v['index'])is int and v['index']==i for i,v in enumerate(inventory))
    sentinel=inventory[12];assert stack(sentinel['signature'],sentinel['count'])==stack(marker_item(mc))
    assert initial['cursor']['count']==0 and all(type(v['count'])is int and v['count']==0 for v in initial['physical'].values())
    baseline=ledger(initial);clients=[];servers=[];controls=[]
    for i,(_,frame)in enumerate(frames):
        assert set(frame)=={'native','widgets','ledger'}
        c,s=pair(frame['native'],'NetworkStatusMenu');w,ws=pair(frame['widgets'],'NetworkStatusMenu')
        assert c['menu']==s['menu']==w['menu']==ws['menu']==MENU and c['screen']==w['screen']==SCREEN
        assert type(c['session'])is int and c['session']>0 and c['session']==s['session']==w['session']==ws['session']
        assert all(type(w['bounds'][k])is int for k in ['x','y','width','height'])and w['bounds']['width']>0 and w['bounds']['height']>0
        data=frame['ledger'];assert ledger(data)==baseline and data['cursor']['count']==0
        assert len([v for v in data['menus']if v['session']==c['session']and v['type']==MENU])==1
        assert all(valid_status(v['status'])for v in [c,s])
        assert topology(c['status'])==topology(s['status'])and drive_count(s['status'])==(3 if i==4 else 2)
        assert c['status']['getMaxStoredPower']==s['status']['getMaxStoredPower']
        unit=c['selectedEnergyUnit'];assert isinstance(unit,str)and unit
        buttons=[v for v in w['widgets']if v.get('currentValue')is not None and v['visible']is True and v['active']is True]
        assert len(buttons)==1 and buttons[0]['currentValue']==unit,'Actual native local unit control missing or mismatched'
        clients.append(c);servers.append(s);controls.append(w)
    units=[v['selectedEnergyUnit']for v in clients]
    assert units[1]!=units[0]and units[2]==units[1]and units[3]!=units[2]and all(v==units[3]for v in units[4:])
    assert controls[2]['bounds']['x']==controls[1]['bounds']['x']+16 and controls[2]['bounds']['y']==controls[1]['bounds']['y']+12
    if modern:
        serial=servers[-1]['gridSerial'];assert type(serial)is int
        assert all(type(v['gridSerial'])is int and v['gridSerial']==serial for v in servers)
        assert clients[-1]['canExportGrid']is True
        buttons=[v for v in controls[-1]['widgets']if v['message']=='Export Grid'and v['visible']is True and v['active']is True];assert len(buttons)==1
        export=journal['export'];assert type(export['grid_serial'])is int and export['grid_serial']==serial
        assert export_end is not None and export_end['menus']==[]and ledger(export_end)==baseline
    else:
        assert journal['export']is None and export_end is None
        assert clients[-1]['exportUnavailableOnPinnedRelease']is True
        assert not any(v['message']=='Export Grid'for w in controls for v in w['widgets'])
    return dict(assertions=11 if modern else 10,states=8 if modern else 7,native_variants=1,
        contract='populated-native-status-local-units-and-modern-export-not-complete-grid-gameplay'),[MENU,SCREEN]


def derive(root,sealed_path,expected,case,original_classes):
    root=Path(root).resolve();sealed=json.loads(Path(sealed_path).read_text());target=sealed['target'];modern=target.split('/')[0]!='1.20.1'
    specification(target,case,original_classes);verify_sealed(root,sealed,expected)
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0];result=row['results']
    assert result.get('ae2_network_features_checked')is True
    assert type(result['cases'])is int and result['cases']==1 and type(result['failed'])is int and result['failed']==0
    assert type(result['checks'])is int and result['checks']==(11 if modern else 10)
    profile=(root/row['profile']).resolve();path=checked(root,sealed['acceptance']);assert path==profile/'ae2-network-release/summary.json'
    journal=json.loads(path.read_text());frames=[];refs=[]
    for number,state in enumerate(sorted(path.parent.glob('[0-9][0-9][0-9]-*.json')),1):
        match=re.fullmatch(r'([0-9]{3})-([a-z0-9-]+)\.json',state.name);assert match and int(match[1])==number
        ref=reference(root,state);assert ref in sealed['full_states'];refs.append(ref);frames.append((match[2],json.loads(state.read_text())))
    export_end=None;export_ref=None
    if modern:
        end=path.parent/'after-native-export.json';ref=reference(root,end);assert ref in sealed['full_states'];refs.append(ref);export_end=json.loads(end.read_text())
        export_ref=review_export(root,journal['export'])
    validation,classes=validate(target,journal,frames,export_end,sealed['archived_inputs'][DRIVER]);assert len(refs)==validation['states']
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
    return dict(schema=SCHEMA,target=target,case=case,disposition='read-only-status-pass',original_native_classes=original_classes,native_classes=classes,
        subjects={k:list(v)for k,v in expected.items()},sealed_evidence=reference(root,sealed_path),acceptance=reference(root,path),native_states=refs,
        native_export=export_ref,validation=validation,publishers=publishers,scope=SCOPE,limits=LIMITS)


def verify(root,record,expected,consumer=None,current=None):
    assert record['schema']==SCHEMA and record['disposition']=='read-only-status-pass'
    actual=[derive(root,checked(root,v['sealed_evidence']),expected_subjects(record,v,expected,consumer,current),record['case'],record['original_native_classes'])for v in[record,*record.get('additional_workflows',[])]]
    assert combine(actual,consumer=consumer,current=current)==record
    return True


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--generation',required=True);parser.add_argument('--plan-only',action='store_true');args=parser.parse_args()
    assert re.fullmatch(r'build[0-9]+',args.generation)
    out=ROOT/'output/release-readiness';backlog=json.loads((out/'coverage-backlog.json').read_text())
    plan=json.loads((ROOT/'build/release-readiness/focused-native-plan-02.json').read_text());jobs=[v for v in plan['jobs']if v['family']=='ae2_network']
    assert len(jobs)==6 and {v['target']for v in jobs}==set(TARGETS);jobs={v['target']:v for v in jobs}
    rows=[r for r in backlog['records']if r['case']==CASE];assert len(rows)==6 and {v['target']for v in rows}==set(TARGETS)
    if args.plan_only:
        for row in rows:print(row['target'],row['case'],'requires actual sealed',jobs[row['target']]['run'])
        print('Plan only; no status, topology or export acceptance.');return
    sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
    import importlib.util
    history_source=ROOT/'build/release-readiness/release_queue_history_455.py'
    assert hashlib.sha256(history_source.read_bytes()).hexdigest()=='fb00a5e47131d9ca30263154439a17f13ea3661eef2651febe9a9fa75fa3affc'
    assert hashlib.sha256((ROOT/'build/release-readiness/focused-native-plan-02.json').read_bytes()).hexdigest()=='9cc23717142dc649b56b8e445e86d1b900652bb580401c8de0a1750f0eb467b9'
    spec=importlib.util.spec_from_file_location('AE2_original_native_history',history_source);history=importlib.util.module_from_spec(spec);spec.loader.exec_module(history)
    resolved=history.completed(plan,json.loads((out/'focused-native-campaign-02.json').read_text()));assert len(resolved)==174
    jobs={v['target']:v for v in resolved if v['family']=='ae2_network'};assert set(jobs)==set(TARGETS)

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
        executions=[jobs[row['target']],*additional_jobs(ROOT,'network',row['target'])]
        workflows=[accepted(job,row)for job in executions]
        record=combine(workflows,consumer=consumer,current=current_subjects(row['target']))
        assert row['current_exact_pins']and {v['sha512']for v in row['current_exact_pins']}<={v['sha512']for v in record['publishers']}
        records.append(record)
    destination=out/f'coverage-ae2-network-{args.generation}-01.json';assert not destination.exists()
    destination.write_text(json.dumps(dict(status='reviewed-native-ae2-network-workflows-reconciled',created=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        candidate_identity=candidate,artifact_equivalence=consumer.report(),auditor_inputs=[reference(ROOT,history_source),reference(ROOT,Path(__file__)),reference(ROOT,ROOT/'tools/focused_release_evidence_ae2_variants.py')],records=records,limits=LIMITS),indent=2)+'\n')
    print('Reconciled six native status records; legacy without export, modern with exact populated native ZIP.')


if __name__=='__main__':main()
