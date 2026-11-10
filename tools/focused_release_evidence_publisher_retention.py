"""Preserve historical publisher evidence separately from an explicit equivalence.

No historical subject is relabeled as a new execution. Only the exact reviewed
build13 -> build14 transition with native Traveler absent can be retained.
"""
from pathlib import Path
import datetime
import hashlib
import json
import re
import zipfile

from focused_release_evidence import checked, reference, subject_rows, journal_scope, measured_counts
from native_publisher_index import index
from focused_release_evidence_retention_policy import (archive_pair, historical_completion,
    latest_state_allows_retention, minimum_comparisons, native_traveler_absent, sha)


def read(path):
    return json.loads(Path(path).read_text(encoding='utf-8'))


def method(text, signature):
    lines = text.splitlines()
    starts = [i for i, line in enumerate(lines) if signature in line]
    assert len(starts) == 1, signature
    start = starts[0]
    end = next((i for i in range(start + 1, len(lines))
        if lines[i].startswith('  ') and not lines[i].startswith('    ') and lines[i].strip()), len(lines))
    return '\n'.join(lines[start:end])


def normalized(text):
    return re.sub(r'#\d+', '#CP', text).replace('\r\n', '\n').strip()


def routing(root, row, delta):
    assert row['target'] == delta['target']
    assert row['before'] == delta['before'] and row['after'] == delta['after']
    texts = {}
    for item in row['classes']:
        key = (item['side'], item['label'])
        assert key not in texts
        jar = checked(root, row[item['side']])
        with zipfile.ZipFile(jar) as archive:
            member = item['class_name'].replace('.', '/') + '.class'
            assert hashlib.sha256(archive.read(member)).hexdigest() == item['class_sha256']
        texts[key] = checked(root, item['disassembly']).read_text()
    assert set(texts) == {('before', 'packets'), ('after', 'packets'),
        ('before', 'sync'), ('after', 'sync'), ('before', 'windows'),
        ('after', 'sender_mixin'), ('after', 'mixin_plugin')}
    windows_member = 'com/salts_inventory_update/compat/travelersbackpack/TravelersWindows.class'
    with zipfile.ZipFile(checked(root, row['before'])) as before, zipfile.ZipFile(checked(root, row['after'])) as after:
        assert before.read(windows_member) == after.read(windows_member)
    for signature in ['static boolean supported()', 'static {};']:
        assert normalized(method(texts['before', 'packets'], signature)) == normalized(method(texts['after', 'packets'], signature))
    assert normalized(texts['before', 'sync']) == normalized(texts['after', 'sync'])
    supported = method(texts['after', 'packets'], 'static boolean supported()')
    assert 'Class.forName:' in supported and 'ReflectiveOperationException' in supported
    assert 'iconst_0' in supported and 'ireturn' in supported
    for signature in ['private static synchronized void bind()', 'private static synchronized void install()']:
        body = method(texts['before', 'windows'], signature)
        instructions = [line for line in body.splitlines() if re.match(r'\s*\d+:', line)]
        assert 'TravelersPackets.supported:()Z' in instructions[0]
        assert 'ifne' in instructions[1] and instructions[2].strip().endswith('return')
    assert 'TravelersPackets.register:' in method(texts['before', 'windows'], 'private static synchronized void bind()')
    # Legacy uses a native-target pseudo mixin. Modern loaders use an always
    # present Connection hook: its send method must remain identical and leave
    # ordinary packets alone without a Traveler widget input scope.
    sender = texts['after', 'sender_mixin']
    plugin = method(texts['after', 'mixin_plugin'], 'public boolean shouldApplyMixin(')
    assert 'getClassNode:' in plugin and 'Class java/lang/ClassNotFoundException' in plugin
    assert 'Class java/io/IOException' in plugin and 'iconst_0' in plugin and 'ireturn' in plugin
    if 'org.spongepowered.asm.mixin.Pseudo' in sender:
        assert re.search(r'targets=\["com[./]tiviacz[./]travelersbackpack[./]util[./]PacketDistributorHelper"\]', sender)
    else:
        assert 'org.spongepowered.asm.mixin.Mixin(' in sender and 'TravelersPackets.send:' in sender
        send = method(texts['after', 'packets'], 'public static boolean send(')
        assert normalized(send) == normalized(method(texts['before', 'packets'], 'public static boolean send('))
        assert 'Field INPUT:Ljava/lang/ThreadLocal;' in send and 'ThreadLocal.get:' in send
        assert 'ifnull' in send and 'iconst_0' in send and 'ireturn' in send
    prefix = 'com/salts_inventory_update/compat/'
    expected_referrers = {prefix + 'nativewindows/mixin/TravelersPacketSenderMixin.class',
        prefix + 'travelersbackpack/TravelersPackets.class', prefix + 'travelersbackpack/TravelersPackets$Sync.class',
        prefix + 'travelersbackpack/TravelersWindows.class', prefix + 'travelersbackpack/TravelersWindows$3.class',
        prefix + 'travelersbackpack/TravelersWindows$Template.class'}
    with zipfile.ZipFile(checked(root, row['after'])) as archive:
        referrers = {name for name in archive.namelist() if name.endswith('.class') and
            b'com/salts_inventory_update/compat/travelersbackpack/TravelersPackets' in archive.read(name)}
        config = json.loads(archive.read('salts_inventory_compat.reviewed-native.mixins.json'))
        assert config['plugin'] == 'com.salts_inventory_update.compat.nativewindows.mixin.NativeWindowsMixinPlugin'
        assert 'TravelersPacketSenderMixin' in config['client']
    assert referrers == expected_referrers == set(row['packet_referrers'])


def engine_semantics(root, profile, engine_proof):
    key = (profile['engine_config_class_sha256'], profile['engine_version_class_sha256'])
    matches = [v for v in engine_proof['engines'] if
        (v['engine']['config_class_sha256'], v['engine']['version_class_sha256']) == key]
    assert len(matches) == 1
    proof = matches[0]
    engine = proof['engine']
    jar = Path(engine['file'])
    assert sha(jar) == engine['sha256']
    with zipfile.ZipFile(jar) as archive:
        for name, digest in [('mixin/transformer/MixinConfig', key[0]), ('util/VersionNumber', key[1])]:
            assert hashlib.sha256(archive.read('org/spongepowered/asm/' + name + '.class')).hexdigest() == digest
    text = checked(root, engine['classes'][0]['disassembly']).read_text()
    flow = method(text, 'private boolean checkVersion()')
    assert flow == proof['minimum_check_control_flow']
    assert 'VersionNumber.parse:' in flow and 'VersionNumber.compareTo:' in flow and 'ifle' in flow
    for line in text.splitlines():
        if line.startswith('  ') and not line.startswith('    ') and '(' in line and line.strip().endswith(';') and 'checkVersion(' not in line:
            assert 'Field version:' not in method(text, line.strip())
    verbose = checked(root, proof['verbose']).read_text()
    field = verbose[verbose.index('  private java.lang.String version;'):]
    field = field[:field.index('\n  private ', 1)]
    assert 'SerializedName' in field and 'minVersion' in field
    scenarios = {}
    for item in proof['comparisons']:
        assert item['scenario'] not in scenarios
        value = read(checked(root, item['stdout']))
        assert value == item['result']
        assert not checked(root, item['stderr']).read_bytes()
        wanted = item['scenario'] != 'synthetic-too-old'
        assert type(item['exit_code']) is int and item['exit_code'] == (0 if wanted else 2)
        scenarios[item['scenario']] = value
    assert set(scenarios) == {'actual-recorded-engine', 'synthetic-boundary', 'synthetic-too-old'}
    minimum_comparisons(scenarios['actual-recorded-engine'], scenarios['synthetic-boundary'], scenarios['synthetic-too-old'], key[1])
    # Bind the comparisons to the version actually reported by this runtime.
    actual = next(v for v in proof['comparisons'] if v['scenario'] == 'actual-recorded-engine')
    assert actual['compared_installed_version'] == profile['engine_log_line']['version'] == engine['native_version']
    line = profile['engine_log_line']
    assert checked(root, profile['log']).read_text(errors='replace').splitlines()[line['line'] - 1] == line['text']


def runtime_layout(root, attempt, staged):
    result = attempt['results'][0]
    if result.get('runtime_artifact_copies'):
        copies = result['runtime_artifact_copies']
        runtime = {Path(v['file']).name: Path(v['file']) for v in copies}
        assert len(runtime) == len(copies)
        for item in copies:
            assert sha(item['file']) == item['sha256']
    else:
        launch = read(root / result['profile'] / 'launch.json')
        runtime = {v['filename']: Path(launch['cwd']) / 'mods' / v['filename'] for v in staged}
    assert len(runtime) == len(staged) and set(runtime) == {v['filename'] for v in staged}
    return runtime


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
    for names, destination in [(attempt['qa_inputs'], qa), (row.get('generated_inputs', {}), generated)]:
        for name, digest in names.items():
            path = (root / name).resolve()
            assert path.is_relative_to(root) and sha(path) == digest, ('Changed measured source', name)
            key = path.relative_to(root).as_posix()
            inputs[key] = path.read_bytes()
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


def verify_historical_archive(root, record):
    path = checked(root, record['archive'])
    expected = {v['file']: v['sha256'] for v in
        record['history']['qa_inputs'] + record['history']['measured_generated_inputs'] + record['history']['executed_observers']}
    assert expected == record['archived_inputs']
    with zipfile.ZipFile(path) as bundle:
        assert bundle.testzip() is None
        assert len(bundle.namelist()) == len(set(bundle.namelist()))
        assert set(bundle.namelist()) == set(expected)
        for name, digest in expected.items():
            assert hashlib.sha256(bundle.read(name)).hexdigest() == digest


def verify_equivalence(root, record, campaign, current_subjects, *, scan_cache=None):
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
        archive_pair(checked(root, delta['before']), checked(root, delta['after']), delta['family'])
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
    routing(root, next(v for v in route['rows'] if v['target'] == job['target']), delta)
    assert record['coverage_limit'] == 'Original recorded assertions only; entry-only and unresolved features remain unqualified.'
    return dict(target=job['target'], report=actual['acceptance'], original_run=attempt['run'],
        equivalence='native-Traveler-absent-build13-to-build14', measured=actual['measured'])


def retained_for_campaign(root, campaign, subjects_for, *, pointer=None):
    """Read verified retained scopes while giving every newer attempt priority."""
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
        reviewed = verify_equivalence(root, record, campaign, subjects_for(record['history']['target']), scan_cache=cache)
        result.append(dict(job=job, execution_kind='retained-original-scope',
            retained_evidence=ref, original_subjects=record['history']['original_subjects'], **reviewed))
    return result
