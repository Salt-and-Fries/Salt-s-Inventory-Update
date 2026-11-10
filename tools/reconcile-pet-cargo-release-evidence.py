"""Reconcile five pet inventory records only from their real sealed native runs."""
from pathlib import Path
import argparse
import datetime
import json
import re
import sys


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
        assert row['family']==next_row['family']==job['family']in {'ministrosity','moa'}
        row=next_row
    assert row['target']==job['target']and row['family']==job['family']in {'ministrosity','moa'}
    assert type(row['exit_code'])is int and row['exit_code']==0 and row['archival']
    return {**job,'run':row['run']}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--generation', required=True)
    parser.add_argument('--plan-only', action='store_true')
    args = parser.parse_args()
    assert re.fullmatch(r'build[0-9]+', args.generation)
    root = Path(__file__).resolve().parents[1]
    sys.path[:0] = [str(root / 'tools'), str(root / 'functional-tests/project-everything-regression')]
    from focused_release_evidence_pet_cargo import PAIRS, PETS, derive, verify
    from focused_release_evidence_core_log_consumer import verified
    from focused_release_evidence import reference
    from release_candidate_identity import current
    from subject_artifacts import current_subjects
    out = root / 'output/release-readiness'
    backlog = json.loads((out / 'coverage-backlog.json').read_text())
    plan = json.loads((root / 'build/release-readiness/focused-native-plan-02.json').read_text())
    jobs = [v for v in plan['jobs'] if v['family'] in PETS]
    assert len(jobs) == 5 and len({(v['target'], v['family']) for v in jobs}) == 5
    assert {(v['target'], PETS[v['family']]['case']) for v in jobs} == PAIRS
    jobs = {(v['target'], PETS[v['family']]['case']):v for v in jobs}
    rows = [v for v in backlog['records'] if (v['target'], v['case']) in PAIRS]
    assert len(rows) == 5 and {(v['target'], v['case']) for v in rows} == PAIRS
    if args.plan_only:
        for row in rows:
            print(row['target'], row['case'], 'requires original sealed run', jobs[row['target'], row['case']]['run'])
        print('Five target/UI records; original native executions, assertions, states and exact pins remain required. No acceptance.')
        return
    identity = current(root, verify_worktree=True)
    consumer = verified(root, args.generation, identity)
    records = []
    queue_path=out/'focused-native-campaign-02.json';queue=json.loads(queue_path.read_text())
    original_jobs={str(key):dict(job)for key,job in jobs.items()}
    jobs={key:resolved_native_job(job,queue)for key,job in jobs.items()}
    for row in rows:
        target = row['target']
        sealed = out / ('focused-evidence-' + jobs[target, row['case']]['run'] + '.json')
        original = json.loads(sealed.read_text())
        expected = consumer.expected(target, original, current_subjects(target))
        record = derive(root, sealed, expected, row['case'], row.get('historical_native_classes', row['native_classes']))
        assert row['current_exact_pins']
        assert {v['sha512'] for v in row['current_exact_pins']} <= {v['sha512'] for v in record['publishers']}
        verify(root, record, expected)
        records.append(record)
    destination = out / f'coverage-pet-cargo-{args.generation}-01.json'
    assert not destination.exists(), 'Preserve earlier reconciliation evidence'
    destination.write_text(json.dumps(dict(status='reviewed-native-pet-cargo-workflows-reconciled',
        created=datetime.datetime.now(datetime.timezone.utc).isoformat(), candidate_identity=identity,
        artifact_equivalence=consumer.report(),native_queue=reference(root,queue_path),original_jobs=original_jobs,resolved_jobs={str(k):v for k,v in jobs.items()},
        auditor_inputs=[reference(root, Path(__file__)), reference(root, root / 'tools/focused_release_evidence_pet_cargo.py')],
        records=records, limits='Five original native executions only, with their original ten or eleven assertions and eleven full states per target. No extra gameplay or flight/combat claim.'), indent=2) + '\n')
    print('Five native pet target/UI records reconciled from their original sealed executions.')


if __name__ == '__main__':
    main()
