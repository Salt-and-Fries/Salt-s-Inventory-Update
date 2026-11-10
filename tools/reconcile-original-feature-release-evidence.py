"""Reconcile already completed scoped features; never run or invent gameplay."""
from pathlib import Path
import argparse,datetime,json,sys
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
from release_candidate_identity import current
from subject_artifacts import current_subjects
from focused_release_evidence import reference
from focused_release_evidence_session_registration import verified
from focused_release_evidence_original_features import plan,derive,PLAN

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--generation',required=True,choices=['build16']);a=p.parse_args()
    candidate=current(ROOT,verify_worktree=True);consumer=verified(ROOT,a.generation,candidate)
    specs=plan(ROOT);rows=[]
    for spec in specs['records']:
        rows.append(derive(ROOT,spec['target'],spec['case'],consumer,current_subjects(spec['target'])))
    assert len(rows)==88
    executions={e['run']:e for row in rows for e in row['executions']}
    assert len(executions)==30
    out=ROOT/'output/release-readiness'/('coverage-original-features-'+a.generation+'-01.json');assert not out.exists()
    value=dict(status='original-completed-native-features-reconciled',created=datetime.datetime.now(datetime.timezone.utc).isoformat(),candidate=candidate,plan=PLAN,
        unique_executions=30,original_checks=sum(v['checks']for v in executions.values()),records=rows,
        artifact_equivalence=consumer.report(),limits='88 UI aliases share24original focused native runs and6original Curios target runs. Original publishers, scopes, failures, QA and runtime subjects remain intact. No new gameplay or whole-mod scope is created.')
    out.write_text(json.dumps(value,indent=2)+'\n');print(len(rows),'historical aliases reconciled from',len(executions),'existing native executions; no gameplay rerun')

if __name__=='__main__':main()
