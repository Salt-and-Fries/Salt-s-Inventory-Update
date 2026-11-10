"""Rederive the four real clean crafting/error transitions qualifying the native layout repair."""
from pathlib import Path
import importlib.util,json
from focused_release_evidence import checked,reference
from focused_release_evidence_repair_chain_requester_qualifiers import qualify as prior_qualify

def qualify(root,graph,consumer):
    root=Path(root).resolve();previous=json.loads(checked(root,graph['previous_preparation']).read_text())
    prior=prior_qualify(root,previous,consumer);edge=graph['edges'][-1]
    assert(edge['before'],edge['after'])==('1159','1253')
    path=checked(root,edge['native_qualification']);data=json.loads(path.read_text())
    assert data['status']=='all-six-native-crafting-cancel-errors-replan-retry-and-typed-recovery-workflows-strictly-qualified'
    assert data['checks']==174 and len(data['runs'])==6
    failed=json.loads(checked(root,edge['native_failure']).read_text())
    journal=json.loads(checked(root,failed['native_journal']).read_text())
    errors=json.loads(checked(root,failed['runtime_results']).read_text())
    assert len(journal['checks'])==29 and all(v['passed']is True for v in journal['checks'])
    assert errors['failures']==4
    assert [v for v in errors['results']if v['status']=='FAIL']==failed['native_errors']
    assert {v['name']for v in failed['native_errors']}=={'api-error-render','api-error-render-overlay'}
    checked(root,failed['failed_run']);checked(root,failed['log'])
    reader_path=Path(__file__).with_name('reconcile-ae2-crafting-release-evidence.py')
    spec=importlib.util.spec_from_file_location('layout_native_crafting_reader',reader_path)
    reader=importlib.util.module_from_spec(spec);spec.loader.exec_module(reader);reader.ROOT=root
    rows=[]
    for entry in data['runs']:
        if entry['target'].startswith('1.20.1/'):continue
        proof=json.loads(checked(root,entry['proof']).read_text());expected=consumer._stage_subjects('1253',entry['target'],{'core','compatibility'})
        q=proof['qualification'];case='ae2-craft-confirm'
        actual=reader.derive(root,checked(root,q['sealed_evidence']),expected,case,[reader.ALIASES[case]])
        assert actual==q and actual['validation']['assertions']==29 and actual['validation']['states']>=43
        seal=json.loads(checked(root,q['sealed_evidence']).read_text());summary=json.loads(checked(root,seal['summary']).read_text())
        result=summary['results'][0];assert summary['status']==result['status']=='passed' and result['exit_code']==0
        native_result=(root/result['profile']/'results.json').resolve();assert native_result.is_relative_to(root)
        runtime=json.loads(native_result.read_text());assert runtime['failures']==0 and all(v['status']!='FAIL'for v in runtime['results'])
        assert not any(v['name'].startswith('api-error-')for v in runtime['results'])
        rows.append(dict(target=entry['target'],seal=q['sealed_evidence'],checks=29,states=q['validation']['states'],runtime=reference(root,native_result)))
    assert {v['target']for v in rows}=={'1.21.1/neoforge','26.1.2/neoforge','26.2/neoforge','26.3/neoforge'}
    prior['receipts'].append(dict(kind='AE2-native-local-view-layout-stability',campaign=reference(root,path),runs=rows,native_failure=edge['native_failure'],scope='Four clean actual29check native crafting workflows include two real missing-ingredient transitions each, Cancel/Replan/Retry, moved controls and complete typed recovery. Zero caught API rendering failures; no expanded recipe/topology claim.'))
    return prior
