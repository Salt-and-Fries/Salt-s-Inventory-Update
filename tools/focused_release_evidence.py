"""Seal a completed focused run without assigning any UI coverage disposition.

The source archive preserves the actual hashed QA inputs and generated Java.
Runtime-copy verification is performed at sealing; its time and scope remain
explicit. Backlog acceptance still requires native-case and feature review.
"""
from pathlib import Path
import datetime,hashlib,json,zipfile


def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def local(root,value):
    root=Path(root).resolve();path=(root/value).resolve()
    assert path.is_relative_to(root),'Evidence reference escapes the workspace'
    assert path.is_file(),str(path)
    return path


def reference(root,path):
    path=local(root,path)
    return dict(file=path.relative_to(Path(root).resolve()).as_posix(),sha256=sha(path))


def checked(root,ref):
    path=local(root,ref['file']);assert sha(path)==ref['sha256'],ref['file'];return path


def subject_rows(artifacts,expected):
    subjects=[v for v in artifacts if v.get('family')in ['core','compatibility']]
    assert len(subjects)==2 and {v['family']for v in subjects}=={'core','compatibility'},'Exactly one of each shipping subject is required'
    assert {v['family']:(v['filename'],v['sha256'])for v in subjects}=={k:tuple(v)for k,v in expected.items()},'Wrong shipping subjects'


def completed(summary):
    assert summary['status']=='passed','Only completed passing runs can be sealed'
    assert len(summary['results'])==1
    row=summary['results'][0]
    assert row['status']=='passed'and type(row['exit_code'])is int and row['exit_code']==0,'Clean process exit required'
    assert row['target']==summary['target']
    assert type(row['results']['cases'])is int and row['results']['cases']>0
    assert type(row['results']['failed'])is int and row['results']['failed']==0
    return row


def summary_layout(root,path):
    """Read the frozen Tom launcher's old layout without rewriting its reports.

    Its clean process/check exits and fixed per-workflow assertion count supply
    completion; the replay receipt binds the original report and wrapper inputs.
    This is an archival view, not a new execution or a coverage disposition.
    """
    raw=json.loads(path.read_text(encoding='utf-8'))
    if 'results'in raw:return raw
    assert set(raw)=={'status','target','workflow','fixture','profile','started_at','subjects','qa_inputs','pid',
        'checks_exit_code','exit_code','finished_at'},'Unknown legacy summary layout'
    assert raw['status']=='passed'and raw['target']=='26.1.2/fabric'
    for key in ['checks_exit_code','exit_code']:assert type(raw[key])is int and raw[key]==0
    counts={'terminal':17,'configurator':11,'link':11,'vending':10}
    assert raw['workflow']in counts
    assert type(raw['pid'])is int and raw['pid']>0
    assert datetime.datetime.fromisoformat(raw['finished_at'])>=datetime.datetime.fromisoformat(raw['started_at'])
    profile=local(root,Path(raw['profile'])/'staged-artifacts.json').parent
    wanted=Path(root)/'build/remaining-ui-audit/gameplay'/('26.1.2-fabric-siu-'+path.parent.name)
    assert profile==wanted.resolve(),'Old Tom workflow must belong to this run profile'
    assert raw['fixture']==str(Path('build/blocked-setup-recovery/fixtures/polymer-without-bumblezone.json'))
    staged=json.loads((profile/'staged-artifacts.json').read_text(encoding='utf-8'))['artifacts']
    assert raw['subjects']==staged,'Original Tom staged artifact rows differ'
    subject_rows_raw=[v for v in raw['subjects']if v['family']in ['core','compatibility']]
    assert len(subject_rows_raw)==2 and {v['family']for v in subject_rows_raw}=={'core','compatibility'}
    replay=local(root,path.parent/'replay-driver.json');receipt=json.loads(replay.read_text(encoding='utf-8'))
    assert receipt['status']=='passed'and receipt['summary']['sha256']==sha(path)
    assert local(root,receipt['summary']['file'])==path.resolve()
    wanted_inputs={str(Path('functional-tests/release-specialized')/name)for name in
        ['toms_replay.py','run_toms_'+raw['workflow']+'.py']}
    wanted_inputs.add(str(Path('functional-tests/project-everything-regression/run_toms_release.py')))
    assert set(receipt['qa_inputs'])==wanted_inputs,'Missing or unknown Tom replay input'
    qa=dict(raw['qa_inputs'])
    for name,digest in receipt['qa_inputs'].items():
        assert name not in qa or qa[name]==digest,'Conflicting original/replay QA fingerprints'
        qa[name]=digest
    journal_path=local(root,profile/'toms-release/checks.json');journal=json.loads(journal_path.read_text(encoding='utf-8'))
    assert set(journal)=={'scope','driver_sha256','checks'}and len(journal['checks'])==counts[raw['workflow']]
    driver=str(Path('functional-tests/project-everything-regression/toms_release_workflows.py'))
    assert journal['driver_sha256']==qa[driver]
    assert all(v.get('passed')is True for v in journal['checks'])
    return dict(status=raw['status'],target=raw['target'],subjects={v['family']:(v['filename'],v['sha256'])for v in subject_rows_raw},
        qa_inputs=qa,acceptance=reference(root,journal_path),supporting_evidence=[reference(root,replay)],
        archival_layout='frozen-toms-native-workflow',workflow=raw['workflow'],
        scope='Original native Tom workflow; completion from its two clean exits and unchanged original assertion journal. Layout adapter only; no new feature or broader coverage claim.',
        results=[dict(status=raw['status'],exit_code=raw['exit_code'],target=raw['target'],profile=raw['profile'],
            results=dict(cases=1,failed=0,checks=counts[raw['workflow']],original_workflow=raw['workflow']))])


def assertion_layout(root,path,journal,summary):
    if summary.get('archival_layout')=='frozen-toms-native-workflow':
        assert path==local(root,Path(summary['results'][0]['profile'])/'toms-release/checks.json')
        assert set(journal)=={'scope','driver_sha256','checks'}
        return dict(complete=True,checks=journal['checks'],scope=journal['scope']),[]
    if path.parent.name=='film-rename-release':
        # The frozen film writer reports status:passed rather than complete:true.
        # Read its exact original layout; never rewrite the gameplay journal.
        row=completed(summary)
        assert path==local(root,Path(row['profile'])/'film-rename-release/summary.json')
        assert set(journal)=={'status','minecraft','scope','driver_sha256','checks'}
        assert journal['status']=='passed'and journal['minecraft']is None
        assert journal['scope']=='Named film rename focus/apply/cancel/length and complete component checks on a moved SIU window; not every Exposure feature.'
        assert row['results']==dict(cases=1,failed=0,checks=27,film_rename_checked=True)
        qa={Path(k).as_posix():v for k,v in summary['qa_inputs'].items()}
        assert journal['driver_sha256']==qa['functional-tests/release-specialized/film_rename.py']
        checks=journal['checks']
        assert len(checks)==27 and all(set(v)=={'name','passed'}and v['passed']is True for v in checks)
        critical=['apply-persists-exact-native-name-and-preserves-all-other-data',
                  'native-apply-closes-the-rename-source','native-cancel-retains-the-complete-prior-stack',
                  'native-cancel-closes-the-source','native-fifty-character-limit-and-other-components-are-preserved']
        assert all(sum(v['name']==name for v in checks)==1 for name in critical)
        assert sum(v['name']=='rename-window-moves-before-native-controls-are-tested'for v in checks)==3
        assert all((path.parent/name).is_file()for name in ['original.json','opened-1.json','applied.json',
            'opened-10.json','cancelled.json','opened-19.json','bounded.json'])
        return dict(complete=True,checks=checks,scope=journal['scope']),[]
    return grouped_native_journal(root,path,journal)


def journal_scope(journal,filename):
    checks=journal.get('checks')
    assert isinstance(checks,list)and checks,'Nonempty explicit assertion journal required'
    assert all(v.get('passed')is True for v in checks),'Every assertion must explicitly pass'
    if filename=='mod-interaction-checks.json':
        assert journal.get('cases')and not any(v.get('status')in ['running','failed','needs-observer-reproduction']for v in journal['cases'])
        return 'publisher-discovery-and-recorded-interactions'
    assert journal.get('complete')is True,'Focused feature journal must explicitly be complete'
    return 'named-focused-feature-assertions'


def measured_counts(row,journal,filename):
    for key in ['checks','assertions']:
        if key in row['results']:
            value=row['results'][key]
            assert type(value)is int and value==len(journal['checks']),'Reported assertion count does not match the journal'
    if filename=='mod-interaction-checks.json':
        assert row['results']['cases']==len(journal['cases']),'Reported discovery case count does not match the journal'


def grouped_native_journal(root,path,journal):
    """Bind Prominent's four original journals; do not invent aggregate results."""
    if path.parent.name!='prominent-release':return journal,[]
    assert set(journal)=={'complete','groups','scope'}and journal['complete']is True
    assert [v['group']for v in journal['groups']]==['upgrade','armor','visuals','shop'],'Exact native Prominent groups required'
    checks=[];states=[];profile=path.parent.parent
    for group in journal['groups']:
        assert set(group)=={'group','checks','journal','sha256'}
        action=checked(root,dict(file=group['journal'],sha256=group['sha256']))
        assert action==(profile/'focused-evidence'/group['group']/'results.json').resolve(),'Native group journal must belong to this exact profile and group'
        actual=json.loads(action.read_text(encoding='utf-8'))
        assert isinstance(actual,list)and type(group['checks'])is int and group['checks']>=5 and len(actual)==group['checks']
        assert all(v.get('passed')is True and isinstance(v.get('name'),str)and v['name']for v in actual),'Every original grouped assertion must pass'
        observations=[p for p in sorted(action.parent.rglob('*.json'))if p!=action]
        assert observations,'Each native group needs its actual state observations'
        checks.extend(dict(group=group['group'],**value)for value in actual)
        states.extend(reference(root,p)for p in [action,*observations])
    return dict(journal,checks=checks),states


def collect(root,run,expected):
    root=Path(root).resolve();summary_path=local(root,'output/release-readiness/'+run+'/summary.json')
    summary=summary_layout(root,summary_path);row=completed(summary)
    assert {k:tuple(v)for k,v in summary['subjects'].items()}=={k:tuple(v)for k,v in expected.items()}
    profile=(root/row['profile']).resolve();assert profile.is_relative_to(root)and profile.is_dir()
    staged=local(root,profile/'staged-artifacts.json');metadata=json.loads(staged.read_text(encoding='utf-8'));artifacts=metadata['artifacts']
    assert artifacts and len({v['filename']for v in artifacts})==len(artifacts),'Duplicate runtime filename'
    subject_rows(artifacts,expected)
    archive={};source_refs=[]
    def source(path,expected_hash=None):
        path=local(root,path);raw=path.read_bytes();digest=hashlib.sha256(raw).hexdigest()
        if expected_hash is not None:assert digest==expected_hash,('Changed QA input',str(path))
        key=path.relative_to(root).as_posix();assert key not in archive or archive[key]==raw,'Conflicting QA archive member'
        archive[key]=raw;return dict(file=key,sha256=digest)
    assert summary.get('qa_inputs'),'Original QA-input fingerprints are required'
    for name,digest in summary['qa_inputs'].items():source_refs.append(source(name,digest))
    generated=[]
    for name,digest in row.get('generated_inputs',{}).items():generated.append(source(name,digest))
    qa=[v for v in artifacts if v.get('family')=='qa'];assert qa,'Excluded runtime observer must be identified'
    for artifact in qa:
        qa_path=local(root,artifact['file']);assert qa_path.suffix=='.jar'
        generated.append(source(qa_path,artifact['sha256']))
        for path in sorted((qa_path.parent/'source').rglob('*.java')):generated.append(source(path))
        lineage=qa_path.parent/'lineage.json'
        if lineage.exists():generated.append(source(lineage))
    assert any(v['file'].endswith('.java')for v in generated),'Actual generated observer Java is required'
    acceptance=checked(root,summary['acceptance']);journal=json.loads(acceptance.read_text(encoding='utf-8'))
    journal,group_states=assertion_layout(root,acceptance,journal,summary)
    scope=journal_scope(journal,acceptance.name)
    measured_counts(row,journal,acceptance.name)
    states=[reference(root,p)for p in sorted(acceptance.parent.rglob('*.json'))if p!=acceptance]
    for ref in group_states:
        if ref not in states:states.append(ref)
    for ref in journal.get('state_inputs',[]):
        path=checked(root,ref);canonical=reference(root,path)
        if canonical not in states:states.append(canonical)
    assert states,'Full state observations must accompany the assertion journal'
    runtime_record=None
    if row.get('runtime_artifact_copies'):
        copies=row['runtime_artifact_copies'];assert len(copies)==len(artifacts)
        runtime={Path(v['file']).name:Path(v['file'])for v in copies};assert len(runtime)==len(copies)
        for copy in copies:assert sha(copy['file'])==copy['sha256'],'Changed recorded runtime copy'
    else:
        runtime_record=local(root,profile/'launch.json');launch=json.loads(runtime_record.read_text(encoding='utf-8'))
        runtime={v['filename']:Path(launch['cwd'])/'mods'/v['filename']for v in artifacts}
    assert set(runtime)=={v['filename']for v in artifacts}
    runtime_hashes=[]
    for artifact in artifacts:
        original=Path(artifact['file']);original=original if original.is_absolute()else root/original
        assert original.suffix=='.jar'and sha(original)==artifact['sha256'],'Changed staged artifact'
        assert sha(runtime[artifact['filename']])==artifact['sha256'],('Wrong actual runtime copy',artifact['filename'])
        runtime_hashes.append(dict(filename=artifact['filename'],family=artifact['family'],sha256=artifact['sha256'],file=str(runtime[artifact['filename']])))
    references=[reference(root,summary_path),reference(root,staged),reference(root,acceptance),*states]
    supporting=[reference(root,checked(root,ref))for ref in summary.get('supporting_evidence',[])]
    if journal.get('original_journal'):
        original=reference(root,checked(root,journal['original_journal']))
        if original not in supporting:supporting.append(original)
    references+=supporting
    if runtime_record:references.append(reference(root,runtime_record))
    return dict(run=run,target=summary['target'],subjects=expected,scope=scope,archival_layout=summary.get('archival_layout'),
        coverage_disposition='not-assigned-requires-native-case-review',
        original_scope=journal.get('scope',summary.get('scope')),
        process=dict(status=row['status'],exit_code=row['exit_code'],measured=row['results']),
        assertions=len(journal['checks']),summary=reference(root,summary_path),staged=reference(root,staged),
        acceptance=reference(root,acceptance),full_states=states,supporting_evidence=supporting,evidence=references,qa_inputs=source_refs,
        generated_inputs=generated,runtime_copies=runtime_hashes,
        limits='Sealed QA source/binary, staged-runtime and state provenance for these existing assertions only. Runtime-copy hashes were verified at sealing. No complete gameplay, unrecorded feature, different publisher or backlog acceptance is established.'),archive


def seal(root,run,expected,*,candidate_identity=None):
    root=Path(root).resolve();assert run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-'for c in run)
    output=root/'output/release-readiness'/('focused-evidence-'+run+'.json');archive=output.with_suffix('.qa-inputs.zip')
    assert not output.exists()and not archive.exists(),'Never overwrite sealed evidence'
    record,inputs=collect(root,run,expected)
    with zipfile.ZipFile(archive,'x',zipfile.ZIP_DEFLATED)as bundle:
        for name,raw in sorted(inputs.items()):bundle.writestr(name,raw)
    record.update(status='completed-scoped-evidence-sealed',sealed_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        candidate_identity=candidate_identity,qa_source_archive=reference(root,archive),
        archived_inputs={name:hashlib.sha256(raw).hexdigest()for name,raw in inputs.items()})
    verify(root,record,expected)
    output.write_text(json.dumps(record,indent=2)+'\n',encoding='utf-8')
    return output


def verify(root,record,expected):
    assert record['status']=='completed-scoped-evidence-sealed'
    assert record['coverage_disposition']=='not-assigned-requires-native-case-review'
    assert {k:tuple(v)for k,v in record['subjects'].items()}=={k:tuple(v)for k,v in expected.items()}
    for ref in record['evidence']:checked(root,ref)
    for ref in [record['summary'],record['staged'],record['acceptance'],*record['full_states'],*record['supporting_evidence']]:
        assert ref in record['evidence'],'Required evidence is not in the sealed reference list'
        checked(root,ref)
    assert record['full_states']
    summary=summary_layout(root,checked(root,record['summary']));row=completed(summary)
    assert record.get('archival_layout')==summary.get('archival_layout'),'Changed archival summary layout'
    assert summary['target']==record['target']and {k:tuple(v)for k,v in summary['subjects'].items()}=={k:tuple(v)for k,v in expected.items()}
    assert record['process']==dict(status=row['status'],exit_code=row['exit_code'],measured=row['results'])
    acceptance=checked(root,record['acceptance']);journal=json.loads(acceptance.read_text(encoding='utf-8'))
    journal,group_states=assertion_layout(root,acceptance,journal,summary)
    for ref in group_states:assert ref in record['full_states'],'Every original grouped journal and state must be sealed'
    supporting=[reference(root,checked(root,ref))for ref in summary.get('supporting_evidence',[])]
    if journal.get('original_journal'):
        original=reference(root,checked(root,journal['original_journal']))
        if original not in supporting:supporting.append(original)
    assert record['supporting_evidence']==supporting
    for ref in journal.get('state_inputs',[]):assert reference(root,checked(root,ref))in record['full_states'],'Declared native state is not sealed'
    assert journal_scope(journal,Path(record['acceptance']['file']).name)==record['scope']
    measured_counts(row,journal,Path(record['acceptance']['file']).name)
    assert record['assertions']==len(journal['checks'])
    assert record['acceptance']==summary['acceptance']or(Path(record['acceptance']['file']).as_posix()==Path(summary['acceptance']['file']).as_posix()and record['acceptance']['sha256']==summary['acceptance']['sha256'])
    staging=json.loads(checked(root,record['staged']).read_text(encoding='utf-8'))['artifacts'];subject_rows(staging,expected)
    assert len(record['runtime_copies'])==len(staging)
    assert {v['filename']:v['sha256']for v in record['runtime_copies']}=={v['filename']:v['sha256']for v in staging}
    assert len({v['filename']for v in record['runtime_copies']})==len(record['runtime_copies'])
    qa={Path(v['file']).as_posix():v['sha256']for v in record['qa_inputs']}
    assert qa=={Path(k).as_posix():v for k,v in summary['qa_inputs'].items()}
    archive=checked(root,record['qa_source_archive'])
    with zipfile.ZipFile(archive)as bundle:
        assert len(bundle.namelist())==len(set(bundle.namelist())),'Duplicate source archive member'
        assert set(bundle.namelist())==set(record['archived_inputs']),'Unexpected or missing QA archive member'
        for name,digest in record['archived_inputs'].items():assert hashlib.sha256(bundle.read(name)).hexdigest()==digest,name
    required={v['file']:v['sha256']for v in record['qa_inputs']+record['generated_inputs']}
    assert required==record['archived_inputs'],'Unaccounted QA source'
    return True
