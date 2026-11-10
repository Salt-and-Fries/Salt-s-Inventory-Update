"""Reconcile six native aliases from three original filter workflows with exact native pins."""
from pathlib import Path
import argparse,datetime,hashlib,importlib.util,json,re,sys

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--generation',required=True);args=parser.parse_args()
    assert re.fullmatch(r'build[0-9]+',args.generation)
    root=Path(__file__).resolve().parents[1]
    sys.path[:0]=[str(root/'tools'),str(root/'functional-tests/project-everything-regression')]
    from focused_release_evidence_ssn_process import PAIRS,derive,verify
    from focused_release_evidence_core_log_consumer import verified
    from focused_release_evidence import reference
    from release_candidate_identity import current
    from subject_artifacts import current_subjects
    out=root/'output/release-readiness';planpath=root/'build/release-readiness/focused-native-plan-02.json'
    assert hashlib.sha256(planpath.read_bytes()).hexdigest()=='9cc23717142dc649b56b8e445e86d1b900652bb580401c8de0a1750f0eb467b9'
    plan=json.loads(planpath.read_text());queuepath=out/'focused-native-campaign-02.json';queue=json.loads(queuepath.read_text())
    history=root/'build/release-readiness/release_queue_history_455.py'
    assert hashlib.sha256(history.read_bytes()).hexdigest()=='fb00a5e47131d9ca30263154439a17f13ea3661eef2651febe9a9fa75fa3affc'
    spec=importlib.util.spec_from_file_location('original_release_queue_history',history);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    resolved=module.completed(plan,queue);assert len(resolved)==174
    runs={v['target']:v for v in resolved if v['family']=='ssn_process'}
    assert set(runs)=={'1.21.1/neoforge','26.1.2/neoforge'}
    jobs={(target,case):runs[target]for target,case in PAIRS}
    backlog=json.loads((out/'coverage-backlog.json').read_text());rows=[v for v in backlog['records']if(v['target'],v['case'])in PAIRS]
    assert len(rows)==2 and {(v['target'],v['case'])for v in rows}==set(PAIRS)
    identity=current(root,verify_worktree=True);consumer=verified(root,args.generation,identity);records=[]
    for row in rows:
        target=row['target'];sealed=out/('focused-evidence-'+jobs[target,row['case']]['run']+'.json')
        evidence=json.loads(sealed.read_text());expected=consumer.expected(target,evidence,current_subjects(target))
        record=derive(root,sealed,expected,row['case'],row.get('historical_native_classes',row['native_classes']))
        assert row['current_exact_pins']and {v['sha512']for v in row['current_exact_pins']}<={v['sha512']for v in record['publishers']}
        verify(root,record,expected);records.append(record)
    result=out/f'coverage-ssn-process-{args.generation}-01.json';assert not result.exists()
    result.write_text(json.dumps(dict(status='reviewed-native-ssn-process-workflows-reconciled',created=datetime.datetime.now(datetime.timezone.utc).isoformat(),candidate_identity=identity,artifact_equivalence=consumer.report(),native_queue=reference(root,queuepath),resolved_jobs={str(k):v for k,v in jobs.items()},auditor_inputs=[reference(root,Path(__file__)),reference(root,root/'tools/focused_release_evidence_ssn_process.py')],records=records,limits='Two original process-configuration target executions. Each exact publisher has separate no-SIU source-lifetime and stale-redstone controls. Full server/native client values, typed physical ownership, moved input, persistence and ordinary close remain required. No formed-network, reset-during-production or adjacent import acceptance.'),indent=2)+'\n')
    print('Two SSN process configuration workflows reconciled from their actual native target executions.')

if __name__=='__main__':main()
