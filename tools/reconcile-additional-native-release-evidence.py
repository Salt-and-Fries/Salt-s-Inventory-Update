"""Reconcile fifteen sealed native executions into thirteen original backlog records."""
from pathlib import Path
import argparse,datetime,hashlib,importlib.util,json,re,sys

ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'functional-tests/release-specialized'),str(ROOT/'functional-tests/project-everything-regression')]
from additional_native_release_evidence import contract,derive,verify,workflow,COUNTS,MODULES
from focused_release_evidence import reference
from release_candidate_identity import current
from subject_artifacts import current_subjects

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--generation',required=True);args=parser.parse_args();assert re.fullmatch(r'build[0-9]+',args.generation)
    out=ROOT/'output/release-readiness';candidate=current(ROOT,verify_worktree=True)
    source=ROOT/'build/release-readiness/release_queue_history_455.py'
    assert hashlib.sha256(source.read_bytes()).hexdigest()=='fb00a5e47131d9ca30263154439a17f13ea3661eef2651febe9a9fa75fa3affc'
    sp=importlib.util.spec_from_file_location('reviewed_native_history',source);history=importlib.util.module_from_spec(sp);sp.loader.exec_module(history)
    planpath=ROOT/'build/release-readiness/focused-native-plan-02.json';assert hashlib.sha256(planpath.read_bytes()).hexdigest()=='9cc23717142dc649b56b8e445e86d1b900652bb580401c8de0a1750f0eb467b9'
    jobs=history.completed(json.loads(planpath.read_text()),json.loads((out/'focused-native-campaign-02.json').read_text()));assert len(jobs)==174
    selected={(v['family'],v['target']):v for v in jobs if v['family']in MODULES};assert len(selected)==15
    assert {name:sum(f==name for f,_ in selected)for name in MODULES}=={'requester':4,'ae2_preferences':6,'blue_arcs':1,'mega_workbench':1,'requester_wireless':2,'mega_bulk':1}
    qualifications=[]
    for (family,target),job in selected.items():
        seal=out/('focused-evidence-'+job['run']+'.json');qualifications.append(workflow(ROOT,family,seal,current_subjects(target)))
    aliases={'own-ae-part-requester-terminal','merequester-requester','ae2-terminal-settings','blue-skies-arcs','megacells-portable-cell-workbench','merequester-wireless-terminal'}
    rows=[v for v in json.loads((out/'coverage-backlog.json').read_text())['records']if re.sub(r'^(?:(?:atlas|registry|machine|item)-)+','',v['case']).replace('_','-')in aliases]
    assert len(rows)==13 and len({(v['target'],v['case'])for v in rows})==13
    records=[]
    for row in rows:
        original=row.get('historical_native_classes',row['native_classes']);family,_=contract(row['target'],row['case'],original);job=selected[(family,row['target'])]
        extras=[]
        if family=='mega_workbench':extras=[out/('focused-evidence-'+selected[('mega_bulk',row['target'])]['run']+'.json')]
        q=derive(ROOT,out/('focused-evidence-'+job['run']+'.json'),current_subjects(row['target']),row['target'],row['case'],original,extras)
        assert row['current_exact_pins']and {v['sha512']for v in row['current_exact_pins']}<={v['sha512']for v in q['publishers']}
        verify(ROOT,q,current_subjects(row['target']));records.append(q)
    checks=sum(COUNTS[v['family']]for v in qualifications);assert checks==354
    destination=out/f'coverage-additional-native-{args.generation}-01.json';assert not destination.exists()
    destination.write_text(json.dumps(dict(status='reviewed-additional-native-workflows-reconciled',created=datetime.datetime.now(datetime.timezone.utc).isoformat(),candidate_identity=candidate,
        unique_executions=15,checks=checks,original_backlog_records=13,records=records,workflow_qualifications=qualifications,
        auditor_inputs=[reference(ROOT,Path(__file__)),reference(ROOT,ROOT/'tools/additional_native_release_evidence.py')],
        limits='Thirteen original backlog identities use fifteen actual native executions. Two Requester views share each block/terminal run; six preferences targets are verified despite one historical preferences backlog record; two MEGA workbench workflows cover the same recorded interface. No duplicate gameplay or whole-mod acceptance claim.'),indent=2)+'\n')
    print('Reconciled13 original backlog records from15 unique native executions/354checks; scopes preserved.')

if __name__=='__main__':main()
