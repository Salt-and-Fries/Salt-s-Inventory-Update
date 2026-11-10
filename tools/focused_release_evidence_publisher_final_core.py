"""Final-Core report adapter; keep the measured publisher coordinator frozen.

The native coordinator uses the original publisher-retention module unchanged.
This report-only adapter adds explicit preserved-artifact lookup for the final
logger certificate. It cannot grant that certificate or change recorded runs.
"""
from pathlib import Path
import hashlib
import types
import zipfile
import focused_release_evidence_publisher_retention as original
from focused_release_evidence_preserved_qa import MeasuredSources, active as archived_qa_active
from focused_release_evidence_publisher_retention import (
    read, verify_historical_archive, engine_semantics,
    checked, latest_state_allows_retention, archive_pair, native_traveler_absent, index,
    historical_completion, subject_rows, runtime_layout, journal_scope, measured_counts, reference,
    sha,
)


def routing(root, row, delta, *, artifact_locator=None):
    """Run the frozen routing verifier against hash-checked preserved archives.

    Only the two archive reads are relocated. Disassemblies and all original
    class/member, absence-routing and referrer assertions stay unchanged.
    A local globals copy avoids mutating the frozen policy's module state.
    """
    if artifact_locator is None:
        return original.routing(root, row, delta)
    root = Path(root).resolve()
    def located_checked(owner, ref):
        if ref not in [row['before'], row['after']]:
            return checked(owner, ref)
        path = Path(artifact_locator(ref)).resolve()
        assert path.is_relative_to(root) and path.is_file(), 'Preserved routing archive outside workspace'
        assert sha(path) == ref['sha256'], 'Preserved routing archive hash mismatch'
        return path
    namespace = dict(original.routing.__globals__)
    namespace['checked'] = located_checked
    verifier = types.FunctionType(original.routing.__code__, namespace,
                                  original.routing.__name__, original.routing.__defaults__,
                                  original.routing.__closure__)
    return verifier(root, row, delta)


def history(root, attempt, job, expected, *, archive=None):
    """Validate original completion and preserve original, measured provenance.

    Production jobs did not fingerprint generated Java separately. Their exact
    executed observer binary and original generator inputs are preserved; this
    adapter never supplies a retrospective generated-source execution hash.
    """
    root = Path(root).resolve()
    row = historical_completion(attempt, job, expected)
    profile = root / row['profile']
    staging_path = profile / 'staged-artifacts.json'
    staged = read(staging_path)['artifacts']
    subject_rows(staged, expected)
    runtime = runtime_layout(root, attempt, staged)
    qa, generated, binaries, runtime_rows = [], [], [], []
    inputs = {}
    measured_sources = MeasuredSources(root)
    for names, destination in [(attempt['qa_inputs'], qa), (row.get('generated_inputs', {}), generated)]:
        for name, digest in names.items():
            path = (root / name).resolve()
            assert path.is_relative_to(root), ('Measured source outside workspace', name)
            raw = measured_sources.read(name, digest)
            key = path.relative_to(root).as_posix()
            inputs[key] = raw
            destination.append(dict(file=key, sha256=digest))
    for item in staged:
        path = runtime[item['filename']]
        assert sha(path) == item['sha256'], ('Changed actual old runtime copy', str(path))
        runtime_rows.append(dict(filename=item['filename'], family=item['family'], sha256=item['sha256'], file=str(path)))
        if item['family'] == 'qa':
            key = 'executed-runtime-observers/' + item['filename']
            assert key not in inputs
            inputs[key] = path.read_bytes()
            binaries.append(dict(file=key, sha256=item['sha256'], actual_runtime_path=str(path)))
    assert binaries, 'Actual executed observer jar required'
    report = profile / 'mod-interaction-checks.json'
    journal = read(report)
    assert journal_scope(journal, report.name) == 'publisher-discovery-and-recorded-interactions'
    measured_counts(row, journal, report.name)
    # Preserve every observed state and compressed history already in this
    # disposable profile. Logs and launch provenance are referenced separately.
    states = [reference(root, p) for p in sorted(profile.rglob('*'))
        if p.is_file() and p != report and (p.suffix == '.json' or p.name.endswith('.jsonl.gz'))]
    assert states and any(Path(v['file']).name == 'staged-artifacts.json' for v in states)
    if attempt.get('private_evidence'):
        log = root / attempt['private_evidence'] / 'launch.log'
    else:
        log = Path(read(profile / 'launch.json')['log'])
        if not log.is_absolute():
            log = root / log
    result = dict(job=job['id'], original_run=attempt['run'], target=job['target'],
        original_subjects=expected, measured=row['results'], acceptance=reference(root, report),
        states=states, log=reference(root, log), qa_inputs=qa,
        measured_generated_inputs=generated, executed_observers=binaries, runtime_copies=runtime_rows,
        generated_source_limit='Only generated inputs measured by the original run are claimed as such. Exact executed QA jars and measured generator inputs remain preserved for every run.')
    if archive is not None:
        assert not Path(archive).exists()
        with zipfile.ZipFile(archive, 'x', zipfile.ZIP_DEFLATED) as bundle:
            for name, raw in sorted(inputs.items()):
                bundle.writestr(name, raw)
    return result, inputs



def verify_equivalence(root, record, campaign, current_subjects, *, scan_cache=None, artifact_locator=None):
    """Return the existing report at its original scope, never a new attempt."""
    root = Path(root).resolve()
    assert record['status'] == 'retained-unchanged-scope-not-a-new-execution'
    original_campaign = read(checked(root, record['original_campaign']))
    assert original_campaign['plan'] == campaign['plan'], 'Original 65-job scope changed'
    job = next(v for v in campaign['plan'] if v['id'] == record['history']['job'])
    attempt = next(v for v in reversed(original_campaign['attempts']) if v['job'] == job['id'])
    latest = next(v for v in reversed(campaign['attempts']) if v['job'] == job['id'])
    assert latest_state_allows_retention(latest, attempt, current_subjects), 'Fresh/live replay takes precedence'
    deltas = read(checked(root, record['delta_ledger']))
    rows = [v for v in deltas['artifacts'] if v['target'] == job['target']]
    assert len(rows) == 3 and {v['family'] for v in rows} == {'core', 'compatibility', 'controlify'}
    for delta in rows:
        before=checked(root,delta['before'])if artifact_locator is None else artifact_locator(delta['before'])
        after=checked(root,delta['after'])if artifact_locator is None else artifact_locator(delta['after'])
        archive_pair(before,after,delta['family'])
    expected_old = {v['family']: [Path(v['before']['file']).name, v['before']['sha256']]
        for v in rows if v['family'] in {'core', 'compatibility'}}
    expected_new = {v['family']: [Path(v['after']['file']).name, v['after']['sha256']]
        for v in rows if v['family'] in {'core', 'compatibility'}}
    assert expected_new == {k: list(v) for k, v in current_subjects.items()}
    actual, _ = history(root, attempt, job, expected_old)
    assert actual == record['history'], 'Historical process/source/runtime/state provenance changed'
    verify_historical_archive(root, record)
    scope = read(checked(root, record['runtime_scope_audit']))
    profile = next(v for v in scope['records'] if v['job'] == job['id'])
    assert profile['target'] == job['target'] and profile['original_attempt'] == attempt['run']
    assert profile['traveler_absent'] is True and not profile['traveler_native_definitions']
    assert profile['log'] == actual['log']
    cache = {} if scan_cache is None else scan_cache
    scanned = []
    loader = job['target'].split('/')[1]
    for item in actual['runtime_copies']:
        if item['family'] in {'core', 'compatibility'}:
            continue
        raw = Path(item['file']).read_bytes()
        digest = hashlib.sha512(raw).hexdigest()
        key = (digest, loader)
        if key not in cache:
            cache[key] = native_traveler_absent(index(Path(item['file']), digest, loader))
        assert cache[key], 'Native Traveler loaded; fresh gameplay required'
        scanned.append((item['filename'], item['sha256'], digest))
    assert set(scanned) == {(v['filename'], v['sha256'], v['sha512']) for v in profile['runtime_mods']}
    engine_semantics(root, profile, read(checked(root, record['engine_semantics'])))
    route = read(checked(root, record['absence_routing']))
    delta = next(v for v in rows if v['family'] == 'compatibility')
    routing(root, next(v for v in route['rows'] if v['target'] == job['target']), delta,
            artifact_locator=artifact_locator)
    assert record['coverage_limit'] == 'Original recorded assertions only; entry-only and unresolved features remain unqualified.'
    return dict(target=job['target'], report=actual['acceptance'], original_run=attempt['run'],
        equivalence='native-Traveler-absent-build13-to-build14', measured=actual['measured'])


def retained_for_campaign(root, campaign, subjects_for, *, pointer=None, artifact_locator=None):
    """Read verified retained scopes while giving every newer attempt priority."""
    if artifact_locator is None and not archived_qa_active(root):
        return original.retained_for_campaign(root, campaign, subjects_for, pointer=pointer)
    root = Path(root).resolve()
    pointer = root / 'output/release-readiness/publisher-retention-current.json' if pointer is None else Path(pointer)
    if not pointer.exists():
        return []
    receipt = read(pointer)
    assert receipt['status'] == 'explicit-scoped-retention-active'
    index_path = checked(root, receipt['index'])
    manifest = read(index_path)
    assert manifest['status'] == 'scoped-historical-equivalences-verified'
    assert manifest['plan'] == campaign['plan'] and len(campaign['plan']) == 65
    result, jobs, cache = [], set(), {}
    latest = {v['job']: v for v in campaign['attempts']}
    for ref in manifest['records']:
        record = read(checked(root, ref))
        job = record['history']['job']
        assert job not in jobs, 'Duplicate retained publisher job'
        jobs.add(job)
        attempt = latest.get(job)
        assert attempt, 'Retained job is absent from the execution journal'
        if attempt['run'] != record['history']['original_run']:
            # A newer failure/live/partial execution remains visible and has
            # no acceptance here. A successful fresh execution is counted by
            # its own normal current-subject validator instead.
            continue
        reviewed = verify_equivalence(root, record, campaign, subjects_for(record['history']['target']), scan_cache=cache,artifact_locator=artifact_locator)
        result.append(dict(job=job, execution_kind='retained-original-scope',
            retained_evidence=ref, original_subjects=record['history']['original_subjects'], **reviewed))
    return result
