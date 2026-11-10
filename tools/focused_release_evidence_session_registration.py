"""Retain exact older scopes through the six-line session-registration correction.

This layer does not change the original consumers, native assertions or repair
certificates. A fresh exact shipping-byte comparison and the actual successful
page-change workflow are prerequisites. Original executions remain historical.
"""
from pathlib import Path
import hashlib,importlib.util,json,uuid,zipfile
from focused_release_evidence import checked,reference,sha
from focused_release_evidence_core_log_consumer import Consumer,subjects,completed_queues,verified as previous_verified
from focused_release_evidence_core_log_transition import derive,production_unchanged
from focused_release_evidence_repair_chain_transition import attach,PREPARATION
from focused_release_evidence_repair_chain_consumer import RepairConsumer

VERSIONS=('1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3')
SCHEMA='exact-Core-second-session-registration-removal-and-retained-prior-scopes-v1'
STATUS='exact-session-registration-correction-native-qualified-original-scopes-retained'

def read(path):return json.loads(Path(path).read_text())

def references(root,value):
    """Recheck every declared file identity in a cached immutable receipt."""
    if isinstance(value,dict):
        if isinstance(value.get('file'),str)and isinstance(value.get('sha256'),str):checked(root,value)
        for child in value.values():references(root,child)
    elif isinstance(value,list):
        for child in value:references(root,child)

def source_delta(root,build,current_identity):
    old=read(checked(root,build['before']['source_snapshot']))
    compiled=read(checked(root,build['candidate']['source_snapshot']))
    current=read(checked(root,current_identity['source_snapshot']))
    expected={f'versions/{v}/fabric/src/main/java/com/salts_inventory_update/client/InventoryDesktopScreen.java'for v in VERSIONS}
    a,b=old['source_files'],compiled['source_files']
    amendment=build['parity_check_amendment'];guard='functional-tests/scripts/Test-SourceFeatureParity.ps1'
    assert amendment['source']==guard
    assert a[guard]==amendment['before']['sha256'] and b[guard]==amendment['after']['sha256']
    checked(root,amendment['before'])
    assert {n for n in set(a)|set(b)if a.get(n)!=b.get(n)}==expected|{guard}
    assert len(build['source_changes'])==6 and {v['source']for v in build['source_changes']}==expected
    with zipfile.ZipFile(checked(root,old['review_source_archive']))as before,zipfile.ZipFile(checked(root,compiled['review_source_archive']))as after:
        for row in build['source_changes']:
            name=row['source'];x,y=before.read(name),after.read(name)
            assert hashlib.sha256(x).hexdigest()==a[name]==row['before']['sha256']
            assert hashlib.sha256(y).hexdigest()==b[name]==row['after']['sha256']
            anchor=b'        if (transitionReplacedSession != null) {\r\n            this.sessions.add(session);\r\n'
            if anchor not in x:anchor=anchor.replace(b'\r\n',b'\n')
            assert x.count(anchor)==1
            assert y==x.replace(anchor,anchor.split(b'            this.sessions.add(session);')[0],1)
            assert y.count(b'this.sessions.add(session);')==x.count(b'this.sessions.add(session);')-1
    production_unchanged(compiled,current)
    assert compiled['artifacts']==current['artifacts']
    return old,compiled

def native_page(root):
    summary_path=root/'output/release-readiness/readiness-final-smallships-1211-neoforge-05/summary.json'
    summary=read(summary_path);assert summary['status']=='passed'and summary['target']=='1.21.1/neoforge'
    accepted=read(checked(root,summary['acceptance']))
    assert accepted['complete']and len(accepted['checks'])==6 and all(v['passed']for v in accepted['checks'])
    actions_path=checked(root,summary['acceptance']).parent/'original-actions.json';actions=read(actions_path)
    observations=[];session_ids=[]
    for action in actions:
        state=read(checked(root,action['response']));client=state.get('client',{})
        if client.get('screen')!='com.salts_inventory_update.client.InventoryDesktopScreen':continue
        windows=client.get('windows',[]);ids=[v['sessionId']for v in windows]
        assert len(ids)==len(set(ids)), 'Actual duplicate client session record'
        assert client['sessions']==len(ids)==1,'Expected one real hosted cargo session'
        session_ids+=ids;observations.append(action['response'])
    assert len(observations)>=5 and len(set(session_ids))>=2,'Actual native replacement was not observed'
    return dict(summary=reference(root,summary_path),acceptance=summary['acceptance'],actions=reference(root,actions_path),unique_session_observations=observations)

def create(root,current_identity,path):
    out=root/'output/release-readiness';build_ref=reference(root,out/'session-registration-build16-1655-01/manifest.json');build=read(checked(root,build_ref))
    assert build['status']=='normal-build16-exact-session-registration-fix-native-qualification-pending'
    assert len(build['actions'])==6 and all(v['status']=='passed'and v['exit_code']==0 for v in build['actions'])
    assert build['actions'][0]['command']==['cmd.exe','/d','/c','gradlew.bat','build']
    old,compiled=source_delta(root,build,current_identity)
    proof_ref=reference(root,out/'session-registration-bytecode-build16-1654-01.json');proof=read(checked(root,proof_ref))
    assert proof['status']=='passed-exact-12-Core-registration-removals-and-24-byte-identical-modules'
    assert len(proof['records'])==36 and len({(v['target'],v['family'])for v in proof['records']})==36
    assert all(v['exit_code']==0 for v in proof['records']);references(root,proof['records'])
    checked(root,proof['inspector'])
    spec=importlib.util.spec_from_file_location('retained_session_inspector',checked(root,proof['inspector']));inspector=importlib.util.module_from_spec(spec);spec.loader.exec_module(inspector)
    assert Path(proof['tool']['source']).read_text()==inspector.base.JAVA
    for name in ['java','javac','source','binary']:assert sha(proof['tool'][name])==proof['tool'][name+'_sha256']
    for item in proof['tool']['libraries']:assert sha(item['file'])==item['sha256']
    native=native_page(root)
    wanted={v['family']:(Path(v['file']).name,v['sha256'])for v in compiled['artifacts']if v['minecraft']=='1.21.1'and v['loader']=='neoforge'and v['family']in {'core','compatibility'}}
    assert subjects(read(checked(root,native['summary']))['subjects'],wanted)==wanted
    gui=read(out/'final-minimum-gui-543-01/manifest.json');assert gui['status']=='passed'and len(gui['runs'])==12 and all(v['status']=='passed'and v['exit_code']==0 for v in gui['runs'])
    queues=completed_queues(root,require_book=True,require_rei=True)
    historical_path=out/'core-log-evidence-transition-final-01.json'
    assert not historical_path.exists(),'Historical transition is created once, never overwritten'
    historical=derive(root,root/'build/release-readiness/historical-Core15-retained-1660-01',reference(root,out/'final-core-log-build15-01.json'),reference(root,out/'final-core-diagnostics-01.json'),build['before'],retained_final=build_ref)
    historical['scope_note']='Historical build14-to15 artifact correction only. Build15 later exposed duplicate replacement registration; this receipt does not claim build15 release readiness.'
    historical['auditor_inputs']=[reference(root,root/'tools/focused_release_evidence_core_log_transition.py'),reference(root,Path(__file__))]
    historical_path.write_text(json.dumps(historical,indent=2)+'\n')
    original={(v['target'],v['family']):v for v in historical['archive_pairs']}
    pairs=[]
    for item in proof['records']:
        prior=original[item['target'],item['family']]
        assert prior['final']==item['before'],'Historical and current artifact edges do not join'
        pairs.append(dict(target=item['target'],family=item['family'],original=prior['original'],intermediate=item['before'],final=item['after']))
    record=dict(schema=SCHEMA,status=STATUS,before=historical['before'],after=build['candidate'],current_candidate=current_identity,
        build=build_ref,exact_registration_delta=proof_ref,historical_transition=reference(root,historical_path),archive_pairs=pairs,
        native_replacement=native,minimum_GUI=reference(root,out/'final-minimum-gui-543-01/manifest.json'),closed_native_queues=queues,
        auditor_inputs=[reference(root,Path(__file__)),reference(root,root/'tools/focused_release_evidence_core_log_consumer.py'),reference(root,root/'tools/focused_release_evidence_core_log_transition.py'),reference(root,root/'functional-tests/release-specialized/run_rei_workspace_policy.py')],
        limits='One new native cargo replacement workflow and12 focused Core GUI checks. Earlier executions retain original artifact hashes, assertion counts, pins, failures and scope. Exact shipping-byte transformations retain unrelated behavior; no full campaign replay or new feature coverage is claimed.')
    path.write_text(json.dumps(record,indent=2)+'\n')
    return record

class SessionConsumer(Consumer):
    def __init__(self,root,certificate,record):
        super().__init__(root,certificate,certificate);self.record=record
        self.retention_kind='retained-exact-session-registration-and-prior-Core-REI-corrections'
        for pair in self.pairs.values():
            self.locators[Path(pair['final']['file']).as_posix(),pair['intermediate']['sha256']]=pair['intermediate']

    def intermediate_subjects(self,target,current):
        self.prior_subjects(target,current)
        return {f:(Path(self.pairs[target,f]['intermediate']['file']).name,self.pairs[target,f]['intermediate']['sha256'])for f in current}

    def bind(self,target,recorded,current):
        result=super().bind(target,recorded,current)
        if result:return result
        current=subjects(current,current)
        try:original=subjects(recorded,current)
        except (AssertionError,KeyError,TypeError):return None
        if original==self.intermediate_subjects(target,current):return dict(execution_kind=self.retention_kind,original_subjects=original,final_subjects=current,artifact_equivalence=self.certificate)
        return None

    def fingerprints(self,recorded,current):
        assert set(recorded)==set(current)
        intermediate={Path(p['final']['file']).as_posix():p['intermediate']['sha256']for p in self.pairs.values()}
        if recorded!={n:intermediate[n]for n in recorded}:return super().fingerprints(recorded,current)
        for name,digest in current.items():
            pair=next(p for p in self.pairs.values()if Path(p['final']['file']).as_posix()==Path(name).as_posix())
            assert pair['final']['sha256']==digest
            self.artifact_path(dict(file=name,sha256=recorded[name]))
        return dict(execution_kind=self.retention_kind,original_artifacts=dict(recorded),final_artifacts=dict(current),artifact_equivalence=self.certificate)

class FinalRepairConsumer(RepairConsumer):
    def fingerprints(self,recorded,current):
        intermediate={Path(p['final']['file']).as_posix():p['intermediate']['sha256']for p in self.base.pairs.values()}
        if recorded==current or recorded=={n:intermediate[n]for n in recorded}:return self.base.fingerprints(recorded,current)
        return super().fingerprints(recorded,current)

    def coverage_path(self,out,prefix,generation,suffix='01'):
        path=Path(out)/f'{prefix}-{generation}-{suffix}.json'
        if path.exists()or generation!='build16':return path
        for old in ['build15','build14']:
            candidate=Path(out)/f'{prefix}-{old}-{suffix}.json'
            if candidate.exists():
                row=dict(requested_generation=generation,original_generation=old,original_proof=reference(self.root,candidate),repair_chain=self.prefix_certificate,final_transition=self.base.report())
                if row not in self.prior_proofs:self.prior_proofs.append(row)
                return candidate
        return path

def verified(root,generation,current_identity):
    if generation!='build16':return previous_verified(root,generation,current_identity)
    root=Path(root).resolve();out=root/'output/release-readiness';path=out/'session-registration-evidence-build16-01.json'
    if not path.exists():create(root,current_identity,path)
    certificate=reference(root,path);record=read(path);assert record['schema']==SCHEMA and record['status']==STATUS
    for item in record['auditor_inputs']:checked(root,item)
    checked(root,record['minimum_GUI']);checked(root,record['historical_transition']);checked(root,record['exact_registration_delta'])
    build=read(checked(root,record['build']));source_delta(root,build,current_identity)
    references(root,record['archive_pairs']);references(root,record['native_replacement'])
    completed_queues(root,require_book=True,require_rei=True)
    base=SessionConsumer(root,certificate,record)
    cache=out/'session-registration-prefix-revalidation-01.json'
    if not cache.exists():
        original=attach(root,'build15',current_identity,base)
        data=dict(status='exact-original-repair-prefix-revalidated-on-final16',certificate=original.prefix_certificate,verification=original.verification)
        cache.write_text(json.dumps(data,indent=2)+'\n')
    data=read(cache);assert data['status']=='exact-original-repair-prefix-revalidated-on-final16';references(root,data)
    prefix=read(checked(root,data['certificate']))
    for item in prefix['auditor_inputs']:checked(root,item)
    return FinalRepairConsumer(root,base,read(checked(root,PREPARATION)),data['certificate'],data['verification'])


QA_INPUT_HISTORY = {'file': 'output/release-readiness/qa-input-source-history-1686-01/manifest.json', 'sha256': '8b9b30f827f68e82455225e245dff3bf408fc5aeaf6a2df2fa72a51af8577ec1'}

def verify_scoped_qa_inputs(root,summary):
    """Keep the live-input guard, allowing only nine pinned archived originals."""
    root=Path(root).resolve();history=read(checked(root,QA_INPUT_HISTORY))
    assert history['status']=='nine-exact-historical-QA-input-deltas-reviewed-no-new-native-execution'
    allowed={row['file']:row for row in history['records']};assert len(allowed)==len(history['records'])==9
    retained=[]
    for name,original in summary.get('qa_inputs',{}).items():
        path=(root/name).resolve();assert path.is_relative_to(root)
        current=sha(path)
        if current==original:continue
        key=path.relative_to(root).as_posix();row=allowed.get(key)
        assert row and row['original_sha256']==original and row['current_sha256']==current,('Unreviewed focused QA input change',key)
        archive=checked(root,row['original_archive'])
        with zipfile.ZipFile(archive)as bundle:
            names={n.replace('\\','/'):n for n in bundle.namelist()}
            assert key in names and hashlib.sha256(bundle.read(names[key])).hexdigest()==original
        retained.append(dict(file=key,original_sha256=original,current_sha256=current,original_archive=row['original_archive'],input_history=QA_INPUT_HISTORY))
    return retained
