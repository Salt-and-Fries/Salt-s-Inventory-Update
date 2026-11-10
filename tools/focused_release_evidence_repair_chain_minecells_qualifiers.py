"""Revalidate original scoped qualifiers and the native Mine Cells recipe-ID resolution without broadening their scope."""
from pathlib import Path
import json,sys
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from focused_release_evidence import checked,reference,sha,verify
from focused_release_evidence_core_log_consumer import Consumer,subjects
from focused_release_evidence_repair_chain_previous_consumer import RepairConsumer as PreviousConsumer
from focused_release_evidence_repair_chain_previous_qualifiers import qualify as prior_qualify

def qualify(root,graph,consumer):
    root=Path(root).resolve();ready=root/'output/release-readiness'
    previous=json.loads(checked(root,graph['previous_preparation']).read_text())
    oldconsumer=PreviousConsumer(root,Consumer(root),previous,graph['previous_preparation'],None)
    prior=prior_qualify(root,previous,oldconsumer)
    path=ready/'readiness-minecells-1211-fabric-02/summary.json';summary=json.loads(path.read_text())
    assert summary['status']=='passed'and summary['target']=='1.21.1/fabric'
    expected=consumer._stage_subjects('431',summary['target'],{'core','compatibility'})
    assert subjects(summary['subjects'],expected)==expected
    assert len(summary['results'])==1
    row=summary['results'][0];assert row['status']=='passed'and row['exit_code']==0 and row['results']['cases']==2 and row['results']['checks']==29
    evidence=ready/'focused-evidence-readiness-minecells-1211-fabric-02.json';verify(root,json.loads(evidence.read_text()),expected)
    failure=ready/'readiness-minecells-1211-fabric-01/summary.json';failed=json.loads(failure.read_text())
    assert failed['status']=='failed'and failed['qa_inputs']==summary['qa_inputs']
    oldprofile=root/'build/remaining-ui-audit/gameplay/1.21.1-fabric-siu-readiness-minecells-1211-fabric-01-1'
    profile=root/row['profile']
    assert sha(oldprofile/'minecells-release/replayed-check.py')==sha(profile/'minecells-release/replayed-check.py')
    negative=ready/'MineCells-ID-inspector-negative-433-01/manifest.json';neg=json.loads(negative.read_text())
    assert neg['status']=='twelve-negative-compiled-delta-mutations-refused'and len(neg['checks'])==12
    for case in neg['checks']:assert case['refused']is True and case['exit_code']!=0 and sha(root/case['log'])==case['log_sha256']
    prior['receipts'].append(dict(kind='native-MineCells-recipe-ID-resolution',summary=reference(root,path),seal=reference(root,evidence),original_failure=reference(root,failure),
        checks=29,unchanged_assertions_sha256=sha(profile/'minecells-release/replayed-check.py'),negative_inspector=reference(root,negative),scope=summary['scope']))
    return prior
