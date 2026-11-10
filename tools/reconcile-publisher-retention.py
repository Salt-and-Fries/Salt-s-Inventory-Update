"""Seal explicitly equivalent historical profiles; never fabricate fresh runs."""
from pathlib import Path
import argparse
import datetime
import hashlib
import json
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'functional-tests/project-everything-regression'))
from release_candidate_identity import current
from subject_artifacts import current_subjects
from focused_release_evidence_retention_policy import archive_pair, latest_state_allows_retention
from focused_release_evidence_publisher_retention import history, read, verify_equivalence
from focused_release_evidence import checked, reference


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--id', required=True)
    for name in ['original-campaign', 'delta-ledger', 'runtime-scope-audit', 'engine-semantics', 'absence-routing']:
        parser.add_argument('--' + name, required=True)
    args = parser.parse_args()
    assert args.id and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.id)
    candidate = current(ROOT, verify_worktree=True)
    destination = ROOT / 'output/release-readiness' / ('publisher-retention-' + args.id + '.json')
    directory = destination.with_suffix('')
    assert not destination.exists() and not directory.exists()
    original_path = ROOT / args.original_campaign
    original = read(original_path)
    campaign_path = ROOT / 'output/release-readiness/readiness-final-publishers-01/publishers.json'
    campaign = read(campaign_path)
    assert original['plan'] == campaign['plan'] and len(campaign['plan']) == 65
    refs = {name.replace('-', '_'): reference(ROOT, ROOT / getattr(args, name.replace('-', '_')))
        for name in ['original-campaign', 'delta-ledger', 'runtime-scope-audit', 'engine-semantics', 'absence-routing']}
    ledger = read(checked(ROOT, refs['delta_ledger']))
    assert len(ledger['artifacts']) == 36
    assert len({(v['target'], v['family']) for v in ledger['artifacts']}) == 36
    for delta in ledger['artifacts']:
        archive_pair(checked(ROOT, delta['before']), checked(ROOT, delta['after']), delta['family'])
    scope = read(checked(ROOT, refs['runtime_scope_audit']))
    eligible = {v['job'] for v in scope['records'] if v['traveler_absent'] is True}
    old_latest = {v['job']: v for v in original['attempts']}
    directory.mkdir()
    records, skipped, cache = [], [], {}
    for job in campaign['plan']:
        old = old_latest.get(job['id'])
        latest = next((v for v in reversed(campaign['attempts']) if v['job'] == job['id']), None)
        if not old or old['status'] != 'passed' or job['id'] not in eligible:
            skipped.append(dict(job=job['id'], reason='No complete eligible original profile; fresh qualification required'))
            continue
        subjects = current_subjects(job['target'])
        if not latest_state_allows_retention(latest, old, subjects):
            skipped.append(dict(job=job['id'], reason='Actual fresh or live replay takes precedence', latest_run=latest['run']))
            continue
        target_rows = [v for v in ledger['artifacts'] if v['target'] == job['target'] and v['family'] in {'core', 'compatibility'}]
        expected = {v['family']: [Path(v['before']['file']).name, v['before']['sha256']] for v in target_rows}
        archive = directory / (job['id'] + '.original-qa.zip')
        historical, inputs = history(ROOT, old, job, expected, archive=archive)
        record = dict(status='retained-unchanged-scope-not-a-new-execution', history=historical,
            archive=reference(ROOT, archive), archived_inputs={k: hashlib.sha256(v).hexdigest() for k, v in inputs.items()},
            **refs, candidate_identity=candidate,
            coverage_limit='Original recorded assertions only; entry-only and unresolved features remain unqualified.')
        verify_equivalence(ROOT, record, campaign, subjects, scan_cache=cache)
        file = directory / (job['id'] + '.json')
        file.write_text(json.dumps(record, indent=2) + '\n')
        records.append(reference(ROOT, file))
        print('RETAINED ORIGINAL SCOPE', job['id'], historical['measured'], flush=True)
    assert current(ROOT, verify_worktree=True) == candidate
    destination.write_text(json.dumps(dict(status='scoped-historical-equivalences-verified',
        created=datetime.datetime.now(datetime.timezone.utc).isoformat(), candidate_identity=candidate,
        preparer=reference(ROOT, Path(__file__)), plan=campaign['plan'], original_campaign=refs['original_campaign'],
        records=records, skipped=skipped,
        limits='These are original executions under original subjects, retained only through the explicitly verified absent-Traveler/accepted-Mixin-gate equivalence. No new gameplay runs, additional assertions, entry-only feature passes or changes to the 65-job plan are claimed.'), indent=2) + '\n')
    print('Verified', len(records), 'explicit retained scopes;', len(skipped), 'profiles remain fresh/affected/unrun.')


if __name__ == '__main__':
    main()
