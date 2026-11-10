"""Reconcile nine native Guard UIs only after their original feature runs pass."""
from pathlib import Path
import argparse,datetime,json,re,sys


def resolved_native_job(job,queue):
    assert queue['status']=='passed','Original native queue must be complete'
    rows=queue['runs'];lookup={v['run']:v for v in rows};assert len(lookup)==len(rows)
    order={v['run']:i for i,v in enumerate(rows)};row=lookup[job['run']];seen=set()
    while row['status']!='passed':
        assert row['status']=='failed'and row['run']not in seen
        seen.add(row['run']);next_row=lookup[row['resolved_by']]
        assert order[next_row['run']]>order[row['run']]
        assert row['resolved_evidence']==next_row['archival'],'Resolution must name actual final seal'
        assert row['target']==next_row['target']==job['target']
        assert row['family']==next_row['family']==job['family']=='guard'
        row=next_row
    assert row['target']==job['target']and row['family']==job['family']=='guard'
    assert type(row['exit_code'])is int and row['exit_code']==0 and row['archival']
    return {**job,'run':row['run']}


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--generation',required=True);p.add_argument('--plan-only',action='store_true');a=p.parse_args()
    assert re.fullmatch(r'build[0-9]+',a.generation)
    root=Path(__file__).resolve().parents[1];sys.path[:0]=[str(root/'tools'),str(root/'functional-tests/project-everything-regression')]
    from focused_release_evidence_guard import CASE,TARGETS,derive,verify
    from subject_artifacts import current_subjects
    from release_candidate_identity import current
    from focused_release_evidence import reference
    from focused_release_evidence_core_log_consumer import verified
    out=root/'output/release-readiness';backlog=json.loads((out/'coverage-backlog.json').read_text())
    plan=json.loads((root/'build/release-readiness/focused-native-plan-02.json').read_text())
    jobs=[v for v in plan['jobs']if v['family']=='guard'];assert len(jobs)==9 and {v['target']for v in jobs}==set(TARGETS)
    jobs={v['target']:v for v in jobs};rows=[r for r in backlog['records']if r['case']==CASE]
    assert len(rows)==9 and {r['target']for r in rows}==set(TARGETS)
    if a.plan_only:
        for row in rows:print(row['target'],CASE,'requires original sealed run',jobs[row['target']]['run'])
        print('Nine target/UI records; original native execution and exact publisher pins remain required. No acceptance.');return
    candidate=current(root,verify_worktree=True);consumer=verified(root,a.generation,candidate);records=[]
    queue_path=out/'focused-native-campaign-02.json';queue=json.loads(queue_path.read_text())
    original_jobs={target:dict(job)for target,job in jobs.items()}
    jobs={target:resolved_native_job(job,queue)for target,job in jobs.items()}
    for row in rows:
        sealed=out/('focused-evidence-'+jobs[row['target']]['run']+'.json')
        raw=json.loads(sealed.read_text());expected=consumer.expected(row['target'],raw,current_subjects(row['target']))
        record=derive(root,sealed,expected,CASE,row.get('historical_native_classes',row['native_classes']))
        assert row['current_exact_pins']
        assert {v['sha512']for v in row['current_exact_pins']}<={v['sha512']for v in record['publishers']},'Unqualified native Guard publisher pin'
        verify(root,record,expected);records.append(record)
    result=out/f'coverage-guard-{a.generation}-01.json';assert not result.exists()
    result.write_text(json.dumps(dict(status='reviewed-native-guard-feature-workflows-reconciled',
        created=datetime.datetime.now(datetime.timezone.utc).isoformat(),candidate_identity=candidate,artifact_equivalence=consumer.report(),native_queue=reference(root,queue_path),original_jobs=original_jobs,resolved_jobs=jobs,unique_executions=9,
        auditor_inputs=[reference(root,Path(__file__)),reference(root,root/'tools/focused_release_evidence_guard.py')],records=records,
        limits='Nine distinct original native runs on their exact targets/publishers. Thirteen original assertions and the exact twelve original plus one explicitly modeled native-latch state witness per adapted target; no combat, travel, reputation-matrix or extra gameplay claim.'),indent=2)+'\n')
    print('Nine native Guard target/UI records reconciled from original completed feature runs.')


if __name__=='__main__':main()
