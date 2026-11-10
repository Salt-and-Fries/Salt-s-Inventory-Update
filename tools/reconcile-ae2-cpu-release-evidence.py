"""Review genuine job controls separately from the native idle CPU selector."""
from pathlib import Path
import argparse,datetime,hashlib,json,re,sys

ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'functional-tests/release-specialized')]
from focused_release_evidence import checked,reference,verify as verify_sealed
from focused_release_evidence_ae2_variants import additional_jobs
from native_publisher_index import index,matches
from ae2_cpu_controls_source import TARGETS,validate_controls
from ae2_cpu_status_features import MENU as STATUS_MENU,SCREEN as STATUS_SCREEN,NAMES,CASE as STATUS_CASE,SCOPE as STATUS_SCOPE,synchronized
from ae2_settings_features import ledger,marker_item,pair,stack

SCHEMA='ae2-native-cpu-controls-and-idle-selector-v1'
CPU_CASE='atlas-ae2-crafting-cpu';STATUS_ALIAS='ae2-crafting-status'
CPU_MENU='appeng.menu.me.crafting.CraftingCPUMenu';CPU_SCREEN='appeng.client.gui.me.crafting.CraftingCPUScreen'
TERMINAL_MENU='appeng.menu.me.common.MEStorageMenu';TERMINAL_SCREEN='appeng.client.gui.me.common.MEStorageScreen'
LABELS=['original-terminal','native-status-child','scrolled-last','selected-last','status-moved','moved-scrolled-first','moved-selected-first','ordinary-terminal-reopened','ordinary-status-reopened']
DRIVER='functional-tests/release-specialized/ae2_cpu_status_features.py'
CPU_SCOPE='Two genuine submitted native crafting jobs; actual pointer scheduling-mode cycles and wrapping, server/client agreement and desktop Cancel verified twice. Modern native Suspend/Resume includes release of the original suspended prerequisite while declared assembler removal holds the job. Legacy pins retain actual absence of Suspend. No client status assignment or input retry.'
LIMITS='Exact native UI workflow and publisher bytes only. Active job tests deliberately hold production and cancel; they do not prove completed crafting output, arbitrary job throughput, all resource restitution or whole-AE2 gameplay. Idle CPU selection is not active-job acceptance. No added assertion count or native execution is inferred from source preparation.'


def specification(target,case,original):
    if case==CPU_CASE:
        assert target in TARGETS and original==[CPU_MENU];return 'controls'
    assert case==STATUS_ALIAS and target=='1.21.1/neoforge'and original==[STATUS_SCREEN]
    return 'idle-selector'


def integer(value):return type(value)is int


def validate_status(target,journal,frames,closed,driver_hash):
    assert target=='1.21.1/neoforge'
    assert journal['complete']is True and journal['driver_sha256']==driver_hash and journal['cases']==[STATUS_CASE]
    assert [(v['case'],v['name'],v['passed'])for v in journal['checks']]==[(STATUS_CASE,v,True)for v in NAMES]
    assert all(v['passed']is True for v in journal['checks'])
    entries=journal['original_entries'];assert len(entries)==1
    entry=entries[0];capture=entry['original_capture']
    assert(capture['minecraft'],capture['loader'],capture['id'],capture['expected_native_menu'],capture['expected_native_screen'])==('1.21.1','neoforge','atlas-ae2-terminal',TERMINAL_MENU,TERMINAL_SCREEN)
    actions=entry['executed_actions'];assert actions and actions[-1]['type']=='use-block-ray'
    assert all(v['type']in{'fixture','server-command','use-block-ray'}for v in actions),'Forced entry or unknown fixture action'
    world=entry['additional_world_commands'];assert len(world)==28 and all(isinstance(v,str)and v.startswith('setblock ')for v in world)
    assert sum(' ae2:64k_crafting_storage'in v for v in world)==9
    assert [label for label,value in frames]==LABELS
    baseline=None;observed={};clients={}
    for label,value in frames:
        assert set(value)=={'widgets','native','ledger'}
        terminal=label in{'original-terminal','ordinary-terminal-reopened'}
        expected_menu=TERMINAL_MENU if terminal else STATUS_MENU
        client,server=pair(value['widgets'],expected_menu.rsplit('.',1)[-1])
        assert client['menu']==server['menu']==expected_menu and client['screen']==(TERMINAL_SCREEN if terminal else STATUS_SCREEN)
        assert integer(client['session'])and client['session']>0 and server['session']==client['session']
        bounds=client['bounds'];assert all(integer(bounds[k])for k in['x','y','width','height'])and bounds['width']>0 and bounds['height']>0
        data=value['ledger'];assert len(data['inventory'])>=36 and all(integer(v['index'])and v['index']==i for i,v in enumerate(data['inventory']))
        marker=data['inventory'][12];assert stack(marker['signature'],marker['count'])==stack(marker_item('1.21.1'))
        assert integer(data['cursor']['count'])and data['cursor']['count']==0
        assert len([v for v in data['menus']if v['session']==client['session']and v['type']==expected_menu])==1
        physical=ledger(data)
        if baseline is None:baseline=physical
        assert physical==baseline,'Native selector changed complete typed item/cursor ledger'
        clients[label]=client
        if terminal:
            assert value['native']is None
            buttons=[w for w in client['widgets']if w['visible']is True and w['active']is True and('Crafting Status'in[w['message'],*w.get('tooltips',[])]or w.get('icon')=='CRAFTING_STATUS')]
            assert len(buttons)==1,'Original terminal status control is missing'
            continue
        native=value['native'];assert synchronized(native,client)
        actual=next(v for v in native['client']if v['session']==client['session'])
        assert integer(actual['visibleRows'])and 0<actual['visibleRows']<9
        assert integer(actual['scroll'])and 0<=actual['scroll']<=9-actual['visibleRows']
        assert type(actual['narrow'])is bool and integer(actual['left'])and integer(actual['top'])
        rect=actual['selection'];assert all(integer(rect[k])for k in['x','y','width','height'])and rect['width']>0 and rect['height']>0
        observed[label]=actual
    initial=observed['native-status-child'];ids=initial['cpus'];rows=initial['visibleRows'];assert initial['scroll']==0
    assert all(v['cpus']==ids and v['visibleRows']==rows for v in observed.values())
    assert observed['scrolled-last']['scroll']==9-rows
    assert observed['selected-last']['selected']==ids[-1]and observed['selected-last']['scroll']==9-rows
    assert observed['status-moved']['selected']==ids[-1]and observed['status-moved']['scroll']==9-rows
    before=clients['selected-last']['bounds'];after=clients['status-moved']['bounds']
    assert after['x']==before['x']+16 and after['y']==before['y']+12
    assert observed['moved-scrolled-first']['scroll']==0 and observed['moved-scrolled-first']['selected']==ids[-1]
    assert observed['moved-selected-first']['scroll']==0 and observed['moved-selected-first']['selected']==ids[0]
    assert observed['ordinary-status-reopened']['scroll']==0
    assert not closed['menus']and integer(closed['cursor']['count'])and closed['cursor']['count']==0 and ledger(closed)==baseline
    return dict(assertions=10,states=10,native_cpus=9,contract='idle-native-scroll-selection-not-active-job-controls')


def publishers(root,artifacts,classes,loader):
    result=[]
    for artifact in artifacts:
        if artifact.get('family','upstream')!='upstream':continue
        path=Path(artifact['file']);path=path if path.is_absolute()else Path(root)/path;raw=path.read_bytes()
        assert hashlib.sha256(raw).hexdigest()==artifact['sha256'];digest=hashlib.sha512(raw).hexdigest()
        if 'sha512'in artifact:assert artifact['sha512']==digest
        locations=matches(index(path,digest,loader),classes)
        if not locations:continue
        assert {v[:-6].replace('/','.')for loc in locations for v in loc['classes']}==set(classes),'Native publisher split or incomplete'
        result.append(dict(filename=artifact['filename'],sha256=artifact['sha256'],sha512=digest,class_locations=locations))
    assert len(result)==1,'Expected one actual AE2 native publisher'
    return result


def derive_controls(root,path,expected,original):
    root=Path(root).resolve();path=Path(path).resolve();summary=json.loads(path.read_text());target=summary['target']
    assert specification(target,CPU_CASE,original)=='controls'
    assert path.parent.parent==root/'output/release-readiness'and path.name=='summary.json'
    if 'publisher_variant'in summary:
        from run_ae2_cpu_controls_legacy_variant import verify_run
    else:
        from run_ae2_cpu_controls import verify_run
    verify_run(root,summary,expected)
    controls=json.loads(checked(root,summary['native_controls']).read_text());lifecycle=json.loads(checked(root,summary['native_lifecycle']).read_text())
    checks=validate_controls(target,controls,lifecycle,checked(root,summary['original_inner_log']).read_text(errors='replace'))
    classes=[CPU_MENU,CPU_SCREEN]
    pinned=publishers(root,summary['runtime_evidence']['dependency_evidence']['upstream'],classes,target.split('/')[1])
    return dict(schema=SCHEMA,target=target,case=CPU_CASE,disposition='functional-pass',original_native_classes=original,native_classes=classes,
        subjects={k:list(v)for k,v in expected.items()},evidence=reference(root,path),acceptance=summary['acceptance'],
        native_states=[summary['native_controls'],summary['native_lifecycle']],publishers=pinned,
        native_class_basis='Compiled and archived native-typed QA source and executed observer binary, with genuine server-job/control observations; not a fabricated runtime class-name field.',
        validation=dict(assertions=len(checks),native_job_submissions=2,native_server_cancellations=2,contract='genuine-job-controls-and-cancel-not-completed-crafting-output'),scope=CPU_SCOPE,limits=LIMITS)


def combine_controls(workflows):
    assert workflows and len({v['evidence']['file']for v in workflows})==len(workflows),'Each pin needs a distinct real CPU execution'
    first=workflows[0]
    for value in workflows:
        assert not value.get('additional_workflows')
        assert all(value[k]==first[k]for k in['schema','target','case','disposition','original_native_classes','native_classes','subjects','native_class_basis','scope','limits'])
    result=dict(first)
    if len(workflows)>1:
        seen={}
        for value in workflows:
            for pin in value['publishers']:
                if pin['sha512']in seen:assert seen[pin['sha512']]==pin
                seen[pin['sha512']]=pin
        result['publishers']=[seen[k]for k in sorted(seen)];result['additional_workflows']=workflows[1:]
    return result


def derive_status(root,path,expected,original):
    root=Path(root).resolve();path=Path(path).resolve();sealed=json.loads(path.read_text());target=sealed['target']
    assert specification(target,STATUS_ALIAS,original)=='idle-selector';verify_sealed(root,sealed,expected)
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0];result=row['results']
    assert result['ae2_cpu_status_features_checked']is True and result['cases']==1 and result['failed']==0 and result['checks']==10
    profile=root/row['profile'];journal=checked(root,sealed['acceptance']);assert journal==profile/'ae2-cpu-status-release/summary.json'
    frames=[];refs=[]
    for ordinal,state in enumerate(sorted(journal.parent.glob('[0-9][0-9][0-9]-*.json')),1):
        match=re.fullmatch(r'([0-9]{3})-([a-z0-9-]+)\.json',state.name);assert match and int(match[1])==ordinal
        ref=reference(root,state);assert ref in sealed['full_states'];refs.append(ref);frames.append((match[2],json.loads(state.read_text())))
    closed=journal.parent/'closed.json';ref=reference(root,closed);assert ref in sealed['full_states'];refs.append(ref)
    validation=validate_status(target,json.loads(journal.read_text()),frames,json.loads(closed.read_text()),sealed['archived_inputs'][DRIVER]);assert len(refs)==10
    classes=[STATUS_MENU,STATUS_SCREEN]
    pinned=publishers(root,json.loads(checked(root,sealed['staged']).read_text())['artifacts'],classes,'neoforge')
    return dict(schema=SCHEMA,target=target,case=STATUS_ALIAS,disposition='functional-pass',original_native_classes=original,native_classes=classes,
        subjects={k:list(v)for k,v in expected.items()},evidence=reference(root,path),acceptance=reference(root,journal),native_states=refs,
        native_class_basis='Actual native menu/screen/session classes captured on both sides; ordinary terminal entry and original status widget.',
        validation=validation,publishers=pinned,scope=STATUS_SCOPE,limits=LIMITS)


def verify(root,record,expected):
    assert record['schema']==SCHEMA and record['disposition']=='functional-pass'
    kind=specification(record['target'],record['case'],record['original_native_classes'])
    if kind=='controls':
        actual=[derive_controls(root,checked(root,v['evidence']),expected,record['original_native_classes'])for v in[record,*record.get('additional_workflows',[])]]
        assert combine_controls(actual)==record
    else:assert derive_status(root,checked(root,record['evidence']),expected,record['original_native_classes'])==record
    return True


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--generation',required=True);p.add_argument('--plan-only',action='store_true');a=p.parse_args();assert re.fullmatch(r'build[0-9]+',a.generation)
    out=ROOT/'output/release-readiness';backlog=json.loads((out/'coverage-backlog.json').read_text())
    controls=json.loads((ROOT/'build/release-readiness/supplemental-ae2-cpu-controls-plan-1429-01.json').read_text());assert controls['sealed']is True and len(controls['jobs'])==6
    accepted=json.loads((out/'supplemental-ae2-cpu-controls-01.json').read_text())
    assert accepted['status']=='passed'and len(accepted['runs'])==6
    assert {(v['target'],v['run'])for v in accepted['runs']}=={(v['target'],v['run'])for v in controls['jobs']}
    assert all(v['status']=='passed'and v['exit_code']==0 and v['runtime_provenance_verified']is True for v in accepted['runs'])
    jobs={v['target']:[v]for v in controls['jobs']};assert set(jobs)==set(TARGETS)
    variants=json.loads((out/'legacy-ae2-publisher-variants-01.json').read_text())
    assert variants['status']=='passed'and len(variants['runs'])==5 and all(v['status']=='passed'and v['exit_code']==0 for v in variants['runs'])
    extra=[v for v in variants['runs']if v['family']=='cpu-controls']
    assert len(extra)==1 and extra[0]['target']=='1.20.1/fabric'and extra[0]['publisher']=='appliedenergistics2-fabric-15.4.11.jar'and extra[0]['runtime_provenance_verified']is True
    jobs['1.20.1/fabric']+=extra
    additional_path=out/'CPU-exact15412-native-1479-01/manifest.json';additional=json.loads(additional_path.read_text())
    assert additional['status']=='passed'and len(additional['runs'])==2
    assert {v['target']for v in additional['runs']}=={'1.20.1/fabric','1.20.1/forge'}
    for actual in additional['runs']:
        assert actual['status']=='passed'and actual['exit_code']==0 and actual['runtime_provenance_verified']is True
        checked(ROOT,actual['summary']);jobs[actual['target']].append(actual)
    status=json.loads((ROOT/'build/release-readiness/supplemental-ae2-cpu-status-plan-1440-01.json').read_text());assert status['sealed']is True and len(status['jobs'])==1
    status_job=status['jobs'][0];assert status_job['target']=='1.21.1/neoforge'
    rows=[r for r in backlog['records']if r['case']in{CPU_CASE,STATUS_ALIAS}];assert len(rows)==7
    assert {(r['target'],r['case'])for r in rows}=={*[(t,CPU_CASE)for t in TARGETS],('1.21.1/neoforge',STATUS_ALIAS)}
    for row in rows:specification(row['target'],row['case'],row.get('historical_native_classes',row['native_classes']))
    if a.plan_only:
        for row in rows:print(row['target'],row['case'],'requires',','.join(v['run']for v in(jobs[row['target']]if row['case']==CPU_CASE else[status_job])))
        print('Plan only: seven UI aliases require ten actual native executions; controls and idle selector remain separate.');return
    sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
    from subject_artifacts import current_subjects
    from release_candidate_identity import current
    identity=current(ROOT,verify_worktree=True);records=[]
    for row in rows:
        expected=current_subjects(row['target']);original=row.get('historical_native_classes',row['native_classes'])
        if row['case']==CPU_CASE:record=combine_controls([derive_controls(ROOT,out/job['run']/'summary.json',expected,original)for job in jobs[row['target']]])
        else:record=derive_status(ROOT,out/('focused-evidence-'+status_job['run']+'.json'),expected,original)
        assert row['current_exact_pins']and {v['sha512']for v in row['current_exact_pins']}<={v['sha512']for v in record['publishers']},'Unqualified exact native CPU publisher'
        records.append(record)
    destination=out/f'coverage-ae2-cpu-{a.generation}-01.json';assert not destination.exists()
    destination.write_text(json.dumps(dict(status='reviewed-native-ae2-cpu-workflows-reconciled',created=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        candidate_identity=identity,auditor_inputs=[reference(ROOT,Path(__file__)),reference(ROOT,ROOT/'build/release-readiness/supplemental-ae2-cpu-controls-plan-1429-01.json'),reference(ROOT,out/'supplemental-ae2-cpu-controls-01.json'),reference(ROOT,additional_path)],records=records,unique_executions=10,limits=LIMITS),indent=2)+'\n')
    print('Reconciled seven native UI aliases from nine genuine-job executions and one distinct idle selector workflow; both legacy patch pins retained with actual native executions.')


if __name__=='__main__':main()
