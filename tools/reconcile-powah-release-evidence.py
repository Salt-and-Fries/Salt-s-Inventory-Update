"""Qualify thirteen historical Powah UI records from four exact native workflows."""
from pathlib import Path
import argparse,datetime,json,re,sys

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--generation',required=True)
    parser.add_argument('--runs',required=True,type=Path,help='Four exact target -> sealed evidence references; no implicit old-pass selection')
    args=parser.parse_args();assert re.fullmatch(r'build[0-9]+',args.generation)
    root=Path(__file__).resolve().parents[1]
    sys.path[:0]=[str(root/'tools'),str(root/'functional-tests/release-specialized'),str(root/'functional-tests/project-everything-regression')]
    from focused_release_evidence import checked,reference
    from release_candidate_identity import current
    from subject_artifacts import current_subjects
    from focused_release_evidence_session_registration import verified
    from powah_native_release_evidence import TARGETS,derive
    from additional_native_release_evidence import publishers
    out=root/'output/release-readiness';candidate=current(root,verify_worktree=True)
    consumer=verified(root,args.generation,candidate)
    runs=json.loads(args.runs.read_text());assert set(runs)==set(TARGETS)
    workflows={};seals={};expected={}
    for target,ref in runs.items():
        seal=checked(root,ref);record=json.loads(seal.read_text());assert record['target']==target
        expected[target]=consumer.expected(target,record,current_subjects(target))
        workflows[target]=derive(root,seal,expected[target]);seals[target]=seal
    backlog=json.loads((out/'coverage-backlog.json').read_text())
    rows=[v for v in backlog['records']if v['case'].startswith('registry-powah-')]
    assert len(rows)==13 and {v['target']for v in rows}=={'1.20.1/fabric','1.21.1/neoforge','26.1.2/neoforge'}
    records=[]
    for row in rows:
        target=row['target'];case=row['case'];assert case.endswith('_starter')
        name=case[len('registry-powah-'):-len('_starter')]
        q=workflows[target];classes=q['validation']['native_classes'][name]
        original=row.get('historical_native_classes',row['native_classes'])
        assert original==[classes[0]],('Unreviewed native Powah class identity',target,case,original,classes)
        pins=publishers(root,seals[target],target,classes)
        assert row['current_exact_pins'] and {v['sha512']for v in row['current_exact_pins']}<={v['sha512']for v in pins}
        validation=next(v for v in q['validation']['cases']if v['case']==name)
        records.append(dict(target=target,case=case,original_native_classes=original,native_classes=classes,subjects=q['subjects'],
            workflow_target=target,sealed_evidence=q['seal'],acceptance=q['acceptance'],validation=validation,publishers=pins,
            scope=q['scope'],limits=q['limits']))
    result=out/f'coverage-powah-{args.generation}-01.json';assert not result.exists()
    data=dict(schema='powah-four-native-workflows-thirteen-historical-records-v1',status='reviewed-native-powah-workflows-reconciled',
        created=datetime.datetime.now(datetime.timezone.utc).isoformat(),candidate_identity=candidate,
        auditor_inputs=[reference(root,Path(__file__)),reference(root,root/'tools/powah_native_release_evidence.py')],
        run_selection=reference(root,args.runs.resolve()),workflows=workflows,records=records,
        unique_executions=4,assertions=sum(v['validation']['assertions']for v in workflows.values()),
        states=sum(v['validation']['states']for v in workflows.values()),artifact_equivalence=consumer.report(),
        limits='Four real native executions, not thirteen executions. Legacy Forge is independently qualified even though the historical backlog has no Forge alias. The exact publisher/feature/cell/energy scopes remain separate from arbitrary power networks, transmitter charging and endurance.')
    assert data['assertions']==332
    result.write_text(json.dumps(data,indent=2)+'\n')
    print('Thirteen historical Powah records reconciled from four exact native workflows /332originalassertions.')

if __name__=='__main__':main()
