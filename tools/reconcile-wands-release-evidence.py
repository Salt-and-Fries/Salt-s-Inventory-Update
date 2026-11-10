"""Reconcile native selector aliases from ten actual tool-source workflows."""
from pathlib import Path
import argparse,datetime,json,re,sys

def resolved_native_job(job,queue):
    assert queue['status']=='passed'
    rows=queue['runs'];lookup={v['run']:v for v in rows};assert len(lookup)==len(rows)
    order={v['run']:i for i,v in enumerate(rows)};row=lookup[job['run']];seen=set()
    while row['status']!='passed':
        assert row['status']=='failed'and row['run']not in seen;seen.add(row['run']);later=lookup[row['resolved_by']]
        assert order[later['run']]>order[row['run']]and row['resolved_evidence']==later['archival']
        assert row['target']==later['target']==job['target']and row['family']==later['family']==job['family']=='wands';row=later
    assert row['target']==job['target']and row['family']==job['family']=='wands'and type(row['exit_code'])is int and row['exit_code']==0
    return {**job,'run':row['run']}

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--generation',required=True);p.add_argument('--plan-only',action='store_true');a=p.parse_args();assert re.fullmatch(r'build[0-9]+',a.generation)
    root=Path(__file__).resolve().parents[1];sys.path[:0]=[str(root/'tools'),str(root/'functional-tests/project-everything-regression')]
    from focused_release_evidence_wands import CASES,TARGETS,classes,derive,verify
    from focused_release_evidence import reference
    from focused_release_evidence_core_log_consumer import verified
    from subject_artifacts import current_subjects
    from release_candidate_identity import current
    out=root/'output/release-readiness';backlog=json.loads((out/'coverage-backlog.json').read_text());plan=json.loads((root/'build/release-readiness/focused-native-plan-02.json').read_text())
    jobs=[v for v in plan['jobs']if v['family']=='wands'];assert len(jobs)==10 and {v['target']for v in jobs}==set(TARGETS)
    rows=[v for v in backlog['records']if v['case']in CASES];assert len(rows)==8
    if a.plan_only:
        print('Eight retained aliases; ten distinct exact target executions required. No acceptance.');return
    candidate=current(root,verify_worktree=True);consumer=verified(root,a.generation,candidate)
    queue_path=out/'focused-native-campaign-02.json';queue=json.loads(queue_path.read_text());original_jobs={v['target']:v for v in jobs}
    resolved={target:resolved_native_job(job,queue)for target,job in original_jobs.items()};executions=[];records=[]
    for target,job in resolved.items():
        sealed=out/('focused-evidence-'+job['run']+'.json');raw=json.loads(sealed.read_text());expected=consumer.expected(target,raw,current_subjects(target))
        actual=derive(root,sealed,expected,CASES[1],[classes(target)[1]]);verify(root,actual,expected);executions.append(actual)
        for row in [v for v in rows if v['target']==target]:
            record=derive(root,sealed,expected,row['case'],row.get('historical_native_classes',row['native_classes']))
            assert row['current_exact_pins']and {v['sha512']for v in row['current_exact_pins']}<={v['sha512']for v in record['publishers']}
            verify(root,record,expected);records.append(record)
    assert len(records)==8
    result=out/f'coverage-wands-{a.generation}-01.json';assert not result.exists()
    result.write_text(json.dumps(dict(status='reviewed-native-wands-tool-selector-workflows-reconciled',created=datetime.datetime.now(datetime.timezone.utc).isoformat(),candidate_identity=candidate,
        artifact_equivalence=consumer.report(),native_queue=reference(root,queue_path),original_jobs=original_jobs,resolved_jobs=resolved,unique_executions=10,
        auditor_inputs=[reference(root,Path(__file__)),reference(root,root/'tools/focused_release_evidence_wands.py')],records=records,all_target_executions=executions,
        limits='Ten exact publisher/target executions, each with ten original assertions and nine full states. Eight historical aliases reuse the corresponding execution. Native tool-source configuration and conservation only; no physical transfer, world building or extra gameplay claim.'),indent=2)+'\n')
    print('Eight historical aliases reconciled; ten exact native selector executions retained.')

if __name__=='__main__':main()
