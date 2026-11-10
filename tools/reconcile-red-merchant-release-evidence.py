"""Reconcile all four original native Red Merchant workflows with exact publisher pins."""
from pathlib import Path
import argparse,datetime,hashlib,importlib.util,json,re,sys

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--generation',required=True);args=parser.parse_args()
    assert re.fullmatch(r'build[0-9]+',args.generation)
    root=Path(__file__).resolve().parents[1]
    sys.path[:0]=[str(root/'tools'),str(root/'functional-tests/project-everything-regression')]
    from focused_release_evidence_red_merchant import PAIRS,derive,verify,combine_records
    from focused_release_evidence_core_log_consumer import verified
    from focused_release_evidence import reference
    from focused_release_evidence_remaining_pins import additional
    from release_candidate_identity import current
    from subject_artifacts import current_subjects
    out=root/'output/release-readiness';planpath=root/'build/release-readiness/focused-native-plan-02.json'
    assert hashlib.sha256(planpath.read_bytes()).hexdigest()=='9cc23717142dc649b56b8e445e86d1b900652bb580401c8de0a1750f0eb467b9'
    plan=json.loads(planpath.read_text());queuepath=out/'focused-native-campaign-02.json';queue=json.loads(queuepath.read_text())
    history=root/'build/release-readiness/release_queue_history_455.py'
    assert hashlib.sha256(history.read_bytes()).hexdigest()=='fb00a5e47131d9ca30263154439a17f13ea3661eef2651febe9a9fa75fa3affc'
    spec=importlib.util.spec_from_file_location('original_release_queue_history',history);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    resolved=module.completed(plan,queue);assert len(resolved)==174
    runs={v['target']:v for v in resolved if v['family']=='red_merchant'}
    assert set(runs)=={'1.20.1/fabric','1.20.1/forge','1.21.1/fabric','1.21.1/neoforge'}
    jobs={(target,case):runs[target]for target,case in PAIRS}
    backlog=json.loads((out/'coverage-backlog.json').read_text());rows=[v for v in backlog['records']if(v['target'],v['case'])in PAIRS]
    assert len(rows)==4 and {(v['target'],v['case'])for v in rows}==set(PAIRS)
    identity=current(root,verify_worktree=True);consumer=verified(root,args.generation,identity);records=[]
    for row in rows:
        target=row['target'];sealed=out/('focused-evidence-'+jobs[target,row['case']]['run']+'.json')
        evidence=json.loads(sealed.read_text());expected=consumer.expected(target,evidence,current_subjects(target))
        record=derive(root,sealed,expected,row['case'],row.get('historical_native_classes',row['native_classes']))
        workflows=[record]
        for job in additional(root,'red-merchant',target):
            extra_path=out/('focused-evidence-'+job['run']+'.json');extra_evidence=json.loads(extra_path.read_text())
            extra_expected=consumer.expected(target,extra_evidence,current_subjects(target))
            workflows.append(derive(root,extra_path,extra_expected,row['case'],row.get('historical_native_classes',row['native_classes'])))
        record=combine_records(workflows,consumer=consumer,current=current_subjects(target))
        assert row['current_exact_pins']and {v['sha512']for v in row['current_exact_pins']}<={v['sha512']for v in record['publishers']}
        verify(root,record,expected,consumer=consumer,current=current_subjects(target));records.append(record)
    result=out/f'coverage-red-merchant-{args.generation}-02.json';assert not result.exists()
    result.write_text(json.dumps(dict(status='reviewed-native-red-merchant-workflows-reconciled',created=datetime.datetime.now(datetime.timezone.utc).isoformat(),candidate_identity=identity,artifact_equivalence=consumer.report(),native_queue=reference(root,queuepath),resolved_jobs={str(k):v for k,v in jobs.items()},auditor_inputs=[reference(root,Path(__file__)),reference(root,root/'tools/focused_release_evidence_red_merchant.py'),reference(root,root/'tools/focused_release_evidence_remaining_pins.py'),reference(root,root/'tools/focused_release_evidence_ae2_variants.py')],previous_report=reference(root,out/f'coverage-red-merchant-{args.generation}-01.json'),records=records,limits='Five native Red Merchant publisher executions on four targets,13assertions and14full states each. Randomized native offers, actual scrolling/selection/refusal/payment/output/uses, complete typed ownership, moved input, owner reopening and invalidation. Not every trade, restocking or progression.'),indent=2)+'\n')
    print('All four Red Merchant workflows reconciled from their actual native target executions.')

if __name__=='__main__':main()
