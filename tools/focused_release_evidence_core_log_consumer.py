"""Consume the explicit final logger-only equivalence without relabeling runs.

Loading final15 retention repeats the producer's strict artifact/source/smoke
verification, including a fresh independent ASM compilation. Original native
execution, QA, publishers, assertions and latest-attempt checks remain the
caller's responsibility. No certificate exists for the current build14 yet.
"""
from pathlib import Path
import json,os,uuid
from focused_release_evidence import checked,reference,sha
from focused_release_evidence_core_log_transition import derive,load as module,production_unchanged,SCHEMA,FINAL_SCHEMA,REI_SCHEMA

QUEUES=('film-after-matrix-01','specialized-after-film-01','focused-tail-campaign-01','focused-native-campaign-02',
        'post-remote-publisher-variants-01','supplemental-ae2-cpu-controls-01','supplemental-ae2-cpu-status-01',
        'legacy-ae2-publisher-variants-01','final-core-diagnostics-01','supplemental-modern-powah-01',
        'final-installation-campaign-01')


def completed_queues(root,require_book=False,require_rei=False):
    """A final report cannot hide any later registered live/failed workflow."""
    root=Path(root).resolve();out=root/'output/release-readiness';refs=[]
    for name in (*QUEUES, *(['final-core-enchanting-book-01'] if require_book else []),*(['final-rei-workspace-policy-01']if require_rei else [])):
        path=out/(name+'.json');data=json.loads(path.read_text())
        assert data['status']=='passed'and data['runs'],'Final retention requires completed native queue: '+name
        runs={r['run']:r for r in data['runs']};assert len(runs)==len(data['runs'])
        order={r['run']:i for i,r in enumerate(data['runs'])}
        if name=='final-installation-campaign-01':
            required={'client-only':22,'server-only':18,'protocol-mismatch':16}
            assert all(r.get('mode')in required for r in data['runs']),'Unknown installation mode'
            latest_modes={r['mode']:r for r in data['runs']}
            assert set(latest_modes)==set(required),'Every final installation mode must actually run'
            for mode,run in latest_modes.items():
                assert run['target']=='26.3/fabric'and run['family']=='installation'and run['status']=='passed'
                assert type(run.get('exit_code'))is int and run['exit_code']==0
                assert type(run.get('checks'))is int and run['checks']==required[mode]
        for run in runs.values():
            if run['status']=='passed':continue
            replacement=runs.get(run.get('resolved_by'))
            assert run['status']=='failed'and replacement and replacement['status']=='passed','Unresolved or live native attempt: '+str(run)
            assert order[replacement['run']]>order[run['run']],'An earlier pass cannot resolve a later failure'
            assert replacement['target']==run['target']and replacement.get('family')==run.get('family')
            if name=='final-installation-campaign-01':assert replacement['mode']==run['mode']
            checked(root,run['resolved_evidence'])
        refs.append(reference(root,path))
    path=out/'readiness-final-publishers-01/publishers.json';campaign=json.loads(path.read_text())
    assert campaign['status']=='complete-mixed-publisher-qualification-needs-case-disposition'
    ids=[v['id']for v in campaign['plan']];assert len(ids)==len(set(ids))==65
    latest={v['job']:v for v in campaign['attempts']}
    assert set(latest)==set(ids)and all(v['status']=='passed'for v in latest.values()),'A newer publisher failure/live attempt cannot inherit an older pass'
    refs.append(reference(root,path))
    return refs


def subjects(value,families):
    """Normalize original metadata, without supplying missing/duplicate subjects."""
    families=set(families);assert families and families<={'core','compatibility','controlify'}
    if isinstance(value,list):
        rows=[v for v in value if v.get('family')in families]
        assert len(rows)==len(families)and {v['family']for v in rows}==families
        value={v['family']:(v['filename'],v['sha256'])for v in rows}
    assert isinstance(value,dict)and set(value)==families
    assert all(isinstance(v,(tuple,list))and len(v)==2 and all(isinstance(x,str)and x for x in v)for v in value.values())
    return {k:tuple(v)for k,v in value.items()}


class Consumer:
    def __init__(self,root,certificate=None,verification=None):
        self.root=Path(root).resolve();self.certificate=certificate;self.verification=verification
        self.retention_kind='retained-core-log-equivalence'
        self.pairs={};self.locators={};self.resolved=[];self.prior_proofs=[]
        if certificate is not None:
            data=json.loads(checked(self.root,certificate).read_text())
            if data.get('schema')==FINAL_SCHEMA:self.retention_kind='retained-final-Core-render-equivalence'
            if data.get('schema')==REI_SCHEMA:self.retention_kind='retained-final-Core-render-and-REI-policy-equivalence'
            self.pairs={(v['target'],v['family']):v for v in data['archive_pairs']}
            assert len(data['archive_pairs'])==len(self.pairs)==36
            for pair in self.pairs.values():
                key=(Path(pair['final']['file']).as_posix(),pair['original']['sha256'])
                assert key not in self.locators;self.locators[key]=pair['original']

    def prior_subjects(self,target,current):
        current=subjects(current,current)
        if not self.certificate:return current
        result={}
        for family,value in current.items():
            pair=self.pairs[target,family]
            assert (Path(pair['final']['file']).name,pair['final']['sha256'])==value,'Final subject is outside the verified certificate'
            result[family]=(Path(pair['original']['file']).name,pair['original']['sha256'])
        return result

    def bind(self,target,recorded,current):
        current=subjects(current,current)
        try:original=subjects(recorded,current)
        except (AssertionError,KeyError,TypeError):return None
        if original==current:
            return dict(execution_kind='fresh-current-artifact',original_subjects=original,final_subjects=current,artifact_equivalence=None)
        if self.certificate and original==self.prior_subjects(target,current):
            return dict(execution_kind=self.retention_kind,original_subjects=original,final_subjects=current,artifact_equivalence=self.certificate)
        return None

    def artifact_path(self,ref):
        """Resolve only a certificate's exact old dist/hash to preserved bytes."""
        path=(self.root/ref['file']).resolve();assert path.is_relative_to(self.root)
        if path.is_file()and sha(path)==ref['sha256']:return path
        key=(Path(ref['file']).as_posix(),ref['sha256'])
        assert self.certificate and key in self.locators,'Unreviewed artifact locator: '+str(ref)
        original=self.locators[key];result=checked(self.root,original)
        change=dict(original_reference=ref,preserved_byte_locator=original,artifact_equivalence=self.certificate)
        if change not in self.resolved:self.resolved.append(change)
        return result

    def fingerprints(self,recorded,current):
        assert set(recorded)==set(current),'Changed complete-suite artifact scope'
        changed=[]
        for name,digest in recorded.items():
            if digest==current[name]:continue
            key=(Path(name).as_posix(),digest)
            assert self.certificate and key in self.locators,'Unqualified complete-suite artifact change'
            pair=next(v for v in self.pairs.values()if Path(v['final']['file']).as_posix()==Path(name).as_posix())
            assert pair['final']['sha256']==current[name]
            self.artifact_path(dict(file=name,sha256=digest));changed.append(name)
        return dict(execution_kind=self.retention_kind if changed else'fresh-current-artifact',
                    original_artifacts=dict(recorded),final_artifacts=dict(current),artifact_equivalence=self.certificate if changed else None)

    def coverage_path(self,out,prefix,generation,suffix='01'):
        """Current proof wins; prior proof remains named and hashed as build14."""
        out=Path(out);current=out/f'{prefix}-{generation}-{suffix}.json'
        if current.exists()or generation!='build15'or not self.certificate:return current
        previous=out/f'{prefix}-build14-{suffix}.json'
        if previous.exists():
            record=dict(requested_generation=generation,original_generation='build14',original_proof=reference(self.root,previous),artifact_equivalence=self.certificate)
            if record not in self.prior_proofs:self.prior_proofs.append(record)
            return previous
        return current

    def expected(self,target,record,current):
        binding=self.bind(target,record['subjects'],current)
        assert binding,'Feature proof subjects are outside the current/exact render-correction transition'
        return binding['original_subjects']

    def chained_retention(self,retained,current):
        if not self.certificate:return None
        assert retained['execution_kind']=='retained-original-scope'
        assert retained['equivalence']=='native-Traveler-absent-build13-to-build14'
        checked(self.root,retained['retained_evidence'])
        prior=self.prior_subjects(retained['target'],current)
        return dict(execution_kind='retained-original-scope-and-final-Core-render-and-REI-policy-equivalence'if self.retention_kind=='retained-final-Core-render-and-REI-policy-equivalence'else'retained-original-scope-and-final-Core-render-equivalence'if self.retention_kind=='retained-final-Core-render-equivalence'else'retained-original-scope-and-core-log-equivalence',
                    original_subjects=retained['original_subjects'],intermediate_subjects=prior,
                    final_subjects=subjects(current,current),original_scope_equivalence=retained['equivalence'],
                    original_retained_evidence=retained['retained_evidence'],artifact_equivalence=self.certificate)

    def report(self):
        if not self.certificate:return None
        return dict(certificate=self.certificate,consumer_verification=self.verification,
                    original_artifact_locators=self.resolved,original_generation_proofs=self.prior_proofs,
                    scope='Explicit artifact equivalence only. Every original result keeps its subjects, native scope and assertion count; no fresh execution or feature coverage is created.')


def _verified_final(root,generation,current_identity):
    """No final15 retention until every actual producer gate can be rechecked."""
    root=Path(root).resolve()
    if generation!='build15':return Consumer(root)
    path=root/'output/release-readiness/core-log-evidence-transition-final-01.json'
    assert path.is_file(),'Final15 retention requires the actual completed Core logging certificate'
    certificate=reference(root,path);record=json.loads(checked(root,certificate).read_text())
    expected_status={SCHEMA:'diagnostic-only-artifact-equivalence-verified-no-coverage-assigned',
        FINAL_SCHEMA:'render-corrections-artifact-equivalence-verified-no-coverage-assigned',
        REI_SCHEMA:'render-and-REI-corrections-artifact-equivalence-verified-no-coverage-assigned'}
    assert record.get('schema')in expected_status and record['status']==expected_status[record['schema']]
    queues=completed_queues(root,require_book=record['schema']in {FINAL_SCHEMA,REI_SCHEMA},require_rei=record['schema']==REI_SCHEMA)
    assert len(record['auditor_inputs'])==2
    for ref in record['auditor_inputs']:checked(root,ref)
    folder=root/'build/release-readiness'/('core-log-consumer-'+uuid.uuid4().hex)
    actual=derive(root,folder,record['build'],record['diagnostics'],current_identity)
    production_unchanged(json.loads(checked(root,record['after']['source_snapshot']).read_text()),
                         json.loads(checked(root,record['current_candidate']['source_snapshot']).read_text()))
    for key,value in actual.items():
        if key not in {'fresh_inspector','current_candidate','archive_pairs'}:
            assert record[key]==value,'Changed certificate field: '+key
    old_pairs={(v['target'],v['family']):v for v in record['archive_pairs']}
    new_pairs={(v['target'],v['family']):v for v in actual['archive_pairs']}
    assert len(record['archive_pairs'])==len(old_pairs)==len(new_pairs)==36 and set(old_pairs)==set(new_pairs)
    for key,before in old_pairs.items():
        after=new_pairs[key]
        assert {k:v for k,v in before.items()if k!='log'}=={k:v for k,v in after.items()if k!='log'}
        assert checked(root,before['log']).read_bytes()==checked(root,after['log']).read_bytes(),'Changed original strict-inspection output'
    inspector=module(checked(root,record['inspector']))
    previous,new=record['fresh_inspector'],actual['fresh_inspector']
    for name in ['java','javac','source','binary']:
        assert sha(previous[name])==previous[name+'_sha256']==new[name+'_sha256']
    assert Path(previous['source']).read_text()==inspector.JAVA
    assert all(sha(v['file'])==v['sha256']for v in previous['libraries'])
    assert [(Path(v['file']).name,v['sha256'])for v in previous['libraries']]==[(Path(v['file']).name,v['sha256'])for v in new['libraries']]
    assert previous['classpath']==os.pathsep.join([str(Path(previous['binary']).parent),*[v['file']for v in previous['libraries']]])
    old_compile=Path(previous['compile_log']).read_text().replace(previous['source'],'<INSPECTOR_SOURCE>')
    new_compile=Path(new['compile_log']).read_text().replace(new['source'],'<INSPECTOR_SOURCE>')
    assert old_compile==new_compile
    receipt=folder/'consumer-verification.json'
    receipt.write_text(json.dumps(dict(status='final-core-artifact-consumer-revalidated-no-native-coverage-assigned',
        certificate=certificate,reinspection=actual,closed_native_queues=queues,original_current_candidate=record['current_candidate'],
        limits='Independent reinspection and12 diagnostic receipt validation only; original native scopes and latest-attempt precedence remain separate.'),indent=2)+'\n')
    return Consumer(root,certificate,reference(root,receipt))


def verified(root,generation,current_identity):
    """Compose only explicit independently revalidated repair and final edges."""
    base=_verified_final(root,generation,current_identity)
    from focused_release_evidence_repair_chain_transition import attach
    return attach(root,generation,current_identity,base)
