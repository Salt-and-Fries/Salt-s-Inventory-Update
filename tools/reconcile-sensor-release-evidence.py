"""Reconcile seven historical sensor aliases only after four real native runs.

Reporting only: no Minecraft launch, fixture edits, assertion changes or broad
coverage transfer. Plan-only output does not create any coverage acceptance.
"""
from pathlib import Path
import argparse,datetime,hashlib,json,re,sys

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
        assert row['family']==next_row['family']==job['family']=='sensor'
        row=next_row
    assert row['target']==job['target']and row['family']==job['family']=='sensor'
    assert type(row['exit_code'])is int and row['exit_code']==0 and row['archival']
    return {**job,'run':row['run']}


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--generation',required=True);p.add_argument('--plan-only',action='store_true')
    p.add_argument('--root',type=Path,default=Path(__file__).resolve().parents[1])
    a=p.parse_args();assert re.fullmatch(r'build[0-9]+',a.generation)
    root=a.root.resolve();assert(root/'RELEASE_READINESS.md').is_file()
    sys.path.insert(0,str(root/'tools'));sys.path.insert(0,str(root/'functional-tests/project-everything-regression'))
    from sensor_feature_evidence import ALIASES,TARGETS,derive,verify
    from subject_artifacts import current_subjects
    from release_candidate_identity import current
    from focused_release_evidence import reference
    from focused_release_evidence_core_log_consumer import verified
    out=root/'output/release-readiness'
    backlog=json.loads((out/'coverage-backlog.json').read_text())
    plan=json.loads((root/'build/release-readiness/focused-native-plan-02.json').read_text())
    jobs=[j for j in plan['jobs']if j['family']=='sensor']
    assert len(jobs)==4 and {j['target']for j in jobs}==set(TARGETS)
    jobs={j['target']:j for j in jobs}
    rows=[r for r in backlog['records']if r['case']in ALIASES]
    assert len(rows)==7
    assert len({(r['target'],r['case'])for r in rows})==7
    assert {(r['target'],r['case'])for r in rows}=={(target,case)for case,targets in ALIASES.items()for target in targets}
    if a.plan_only:
        for r in rows:print(r['target'],r['case'],'requires sealed actual native run',jobs[r['target']]['run'])
        print('Prepared7alias/4target reconciliation plan only. No gameplay or accepted case disposition.');return
    candidate=current(root,verify_worktree=True);consumer=verified(root,a.generation,candidate);records=[]
    queue_path=out/'focused-native-campaign-02.json';queue=json.loads(queue_path.read_text())
    original_jobs={target:dict(job)for target,job in jobs.items()}
    jobs={target:resolved_native_job(job,queue)for target,job in jobs.items()}
    for row in rows:
        job=jobs[row['target']];sealed=out/('focused-evidence-'+job['run']+'.json')
        raw=json.loads(sealed.read_text());expected=consumer.expected(row['target'],raw,current_subjects(row['target']))
        record=derive(root,sealed,expected,row['case'],row.get('historical_native_classes',row['native_classes']))
        pins=row['current_exact_pins'];assert pins
        assert {v['sha512']for v in pins}<={v['sha512']for v in record['publishers']},'Unqualified native sensor publisher pin'
        verify(root,record,expected);records.append(record)
    result=out/f'coverage-sensors-{a.generation}-01.json';assert not result.exists()
    inputs=[Path(__file__).resolve(),root/'tools/sensor_feature_evidence.py']
    document=dict(status='reviewed-native-sensor-feature-workflows-reconciled',
        created=datetime.datetime.now(datetime.timezone.utc).isoformat(),candidate_identity=candidate,artifact_equivalence=consumer.report(),native_queue=reference(root,queue_path),original_jobs=original_jobs,resolved_jobs=jobs,
        auditor_inputs=[reference(root,v)for v in inputs],records=records,
        limits='Four actual native workflows may qualify seven explicitly reviewed historical aliases. Shared aliases are not additional menu executions. All other features, targets, publishers and complete-release gates remain separate.')
    result.write_text(json.dumps(document,indent=2)+'\n')
    print('Reconciled',len(records),'reviewed UI aliases from four exact native workflows; no new gameplay.')

if __name__=='__main__':main()
