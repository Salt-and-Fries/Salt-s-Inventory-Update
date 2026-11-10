"""Revalidate the original scoped native qualifiers for the seven prior production repairs."""
from pathlib import Path
import importlib.util,json,sys
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from focused_release_evidence import checked,reference,sha,verify
from full_suite_provenance import verify_attempt,verify_bundle
from focused_release_evidence_core_log_consumer import subjects

def qualify(root,graph,consumer):
    root=Path(root).resolve();out=root/'output/release-readiness';receipts=[]
    path=root/'build/full-validation-20261023/readiness-final-20261106/dedicated-server/results.json'
    servers=json.loads(path.read_text());cases=servers['cases']
    spec=importlib.util.spec_from_file_location('repair_original_server_plan',root/'tools/run-full-validation.py')
    full=importlib.util.module_from_spec(spec);spec.loader.exec_module(full)
    plan=full.plan('dedicated-server','readiness-final-20261106')
    assert len(plan)==len(cases)==58 and {v['key']for v in plan}=={v['key']for v in cases}
    for case in cases:
        expected=next(v for v in plan if v['key']==case['key'])
        assert all(case[k]==v for k,v in expected.items())
        assert case['status']=='PASS'and case['attempts']
        attempt=case['attempts'][-1];assert attempt['exit_code']==0 and not attempt.get('input_changed_during_profile')
        verify_attempt(root,attempt,case)
        # Identity validation against one complete reviewed stage, no mixed jars.
        current={name:consumer.distributions['381'][name]for name in case['artifacts']}
        consumer.fingerprints(case['artifacts'],current)
    receipts.append(dict(kind='original-dedicated-server-phase',evidence=reference(root,path),cases=58,
        scope='Original server runner scope, recorded QA/process/runtime evidence and latest attempts. Existing35 and actual23 continuation executions remain distinct; no claim of58fresh current-byte launches.'))
    path=out/'readiness-ae2-client-isolation-callbacks-01/summary.json';client=json.loads(path.read_text())
    assert client['status']=='passed-31-focused-native-client-callback-checks'and client['target']=='26.1.2/neoforge'
    expected=consumer._stage_subjects('250',client['target'],{'core','compatibility'})
    assert subjects(client['subjects'],expected)==expected
    assert len(client['results'])==1
    row=client['results'][0];assert row['status']=='passed'and row['exit_code']==0
    assert row['results']['cases']==1 and row['results']['failed']==0 and row['results']['checks']==31
    verify_bundle(root,client['qa_source_archive'],client['qa_inputs'])
    for name,digest in row['generated_inputs'].items():assert sha(root/name)==digest
    copies=row['runtime_artifact_copies'];seen={}
    for copy in copies:
        assert sha(Path(copy['file']))==copy['sha256']
        if copy['family']in expected:
            assert copy['family']not in seen
            seen[copy['family']]=(Path(copy['file']).name,copy['sha256'])
    assert seen==expected
    checks=root/row['results']['evidence'];journal=json.loads(checks.read_text())
    assert len(journal)==31 and all(v.get('passed')is True for v in journal)
    required=['Native mod filter reaches server and synchronizes','Configuration numeric entry reaches server','Renamer native confirm closes its window','Void Cell mode reaches native menu']
    assert all(any(v['name']==name for v in journal)for name in required)
    receipts.append(dict(kind='affected-AE2-native-client-callbacks',summary=reference(root,path),assertions=reference(root,checks),checks=31,scope=client['limits']))
    for run,groups,count,stage in [
        ('readiness-create-1201-fabric-03',19,190,'279'),
        ('readiness-create-1201-forge-03',19,190,'314'),
        ('readiness-create-1211-neoforge-06',19,190,'381')]:
        path=out/run/'summary.json';summary=json.loads(path.read_text());assert summary['status']=='passed'
        expected=consumer._stage_subjects(stage,summary['target'],{'core','compatibility'})
        assert subjects(summary['subjects'],expected)==expected
        evidence=out/('focused-evidence-'+run+'.json');verify(root,json.loads(evidence.read_text()),expected)
        row=summary['results'][0];assert row['exit_code']==0 and row['status']=='passed'
        assert row['results']['checks']==count and len(row['results']['feature_groups'])==groups
        receipts.append(dict(kind='complete-original-Create-scoped-workflow',summary=reference(root,path),seal=reference(root,evidence),groups=groups,checks=count,original_stage=stage,scope=summary['scope']))
    queue=out/'film-after-matrix-01.json';film=json.loads(queue.read_text())
    assert film['status']=='passed'and len(film['runs'])==4 and not film['archival_pending']
    for row in film['runs']:
        assert row['status']=='passed'and row['exit_code']==0
        run=row['run'];path=out/run/'summary.json';summary=json.loads(path.read_text())
        assert summary['status']=='passed'
        assert consumer.bind(summary['target'],summary['subjects'],consumer._stage_subjects('381',summary['target'],{'core','compatibility'}))
        evidence=out/('focused-evidence-'+run+'.json');verify(root,json.loads(evidence.read_text()),summary['subjects'])
        assert summary['results'][0]['results']['checks']==27
        receipts.append(dict(kind='native-film-full-text-and-burst-workflow',summary=reference(root,path),seal=reference(root,evidence),checks=27,scope=summary['scope']))
    path=out/'Create-stock-close-native-resolution-384-01.json';stock=json.loads(path.read_text())
    assert stock['status']=='focused-native-category-persistence-passed'and stock['native_checks']==2
    for key in ['native_summary','native_attempt','journal','isolated_receiver','isolated_snapshot','compiled_delta','candidate']:checked(root,stock[key])
    journal=json.loads(checked(root,stock['journal']).read_text());assert all(v['passed']is True for v in journal['checks'])
    receipts.append(dict(kind='stock-close-settings-resolution',evidence=reference(root,path),checks=2,scope=stock['limits']))
    path=out/'player-publication-unit-312-01.json';publication=json.loads(path.read_text())
    assert publication['status']=='six-mapping-publication-and-strict-rejection-checks-passed'
    assert publication['checks']==84 and len(publication['rows'])==6
    for row in publication['rows']:
        assert row['checks']==14
        assert (root/row['log']).is_file()
        source=root/'versions'/row['version']/'fabric/src/main/java/com/salts_inventory_update/server/DesktopContainerSessions.java'
        assert sha(source)==row['source_sha256']
    receipts.append(dict(kind='six-mapping-isolated-publication-guards',evidence=reference(root,path),checks=84,scope=publication['limits']))
    return dict(status='specific-repair-native-qualifiers-revalidated-not-release-complete',receipts=receipts,
        limits='Only the named qualifiers and their original scopes are revalidated. Source/compiled checks remain separate. Original174 menus, multiplayer, final render/REI/native paging/installation checks and independent review remain mandatory.')
