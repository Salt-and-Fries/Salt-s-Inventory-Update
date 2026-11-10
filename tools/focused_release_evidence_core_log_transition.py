"""Explicit build14->15 diagnostic-only equivalence, without granting UI coverage.

Original executions keep their original subjects. This certificate is useful
only after all twelve real detailed-render smokes pass and an independent
fresh compilation of the strict ASM inspector rechecks every actual jar pair.
"""
from pathlib import Path
import hashlib, importlib.util, json, re, sys, zipfile
from focused_release_evidence import checked, reference

SCHEMA = 'core-detailed-log-only-build14-to-build15-v1'
FINAL_SCHEMA = 'core-render-log-and-exact-12111-book-build14-to-build15-v1'
REI_SCHEMA = 'Core-render-book-and-exact-REI-workspace-policy-build14-to-build15-v1'
REI_PROPOSAL = {'file': 'output/release-readiness/rei-public-marker-recovery-1529-01/manifest.json', 'sha256': '48318bd2c186a70d07bc72dc4d3869e71c3b5abc52cf220c9b41a09e40d063b5'}
REI_PLAN=[dict(target='26.3/'+loader,driver='functional-tests/release-specialized/run_rei_workspace_policy.py',
    run='readiness-final-rei-workspace-'+loader+'-01',family='rei-workspace-policy')for loader in ['fabric','neoforge']]
FAMILIES = {'core', 'compatibility', 'controlify'}
PRODUCTION = ('versions/', 'compatibility/', 'controlify/', 'core-integrations/', 'artwork/', 'gradle/')
ROOT_BUILD = {'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradlew', 'gradlew.bat'}


def load(path):
    path = Path(path)
    spec = importlib.util.spec_from_file_location('_core_log_transition_' + path.stem.replace('-', '_'), path)
    module = importlib.util.module_from_spec(spec)
    previous = list(sys.path)
    sys.path.insert(0, str(path.parent))
    try:
        spec.loader.exec_module(module)
    finally:
        sys.path[:] = previous
    return module


def sources(before, after, prepared, rei=None, reporting=None):
    """Require exactly the six reviewed logger statements, not a broad carveout."""
    old, new = before['source_files'], after['source_files']
    changed = {name for name in set(old) | set(new) if old.get(name) != new.get(name)}
    rows = prepared['rows']
    assert len(rows) == 6 and len({r['source']['file'] for r in rows}) == 6
    rei_rows=[] if rei is None else rei['rows']
    if rei is not None:
        public_marker=rei['schema']=='exact-REI-public-workspace-marker-policy-v2'
        assert public_marker or rei['schema']=='exact-REI-controller-workspace-overlay-policy-v1'
        count=30 if public_marker else 18
        assert len(rei_rows)==count and len({r['source']['file']for r in rei_rows})==count
        assert sum(r['kind']=='new-exact-screen-policy'for r in rei_rows)==6
        assert sum(r['kind']=='existing-plugin-one-public-registration-method'for r in rei_rows)==12
        versions={'1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3'}
        required={(mc,loader)for mc in versions for loader in ['fabric','forge'if mc=='1.20.1'else'neoforge']}
        plugins=set();policies=set()
        assert sum(r['kind']=='new-public-recipe-browser-screen-marker'for r in rei_rows)==(6 if public_marker else 0)
        assert sum(r['kind']=='controller-public-marker-declaration-only'for r in rei_rows)==(6 if public_marker else 0)
        for row in rei_rows:
            if row['kind'] in {'new-public-recipe-browser-screen-marker','controller-public-marker-declaration-only'}:continue
            match=re.fullmatch(r'compatibility/versions/([^/]+)/([^/]+)/src/main/java/com/salts_inventory_update/compat/rei/([^/]+)\.java',row['source']['file'])
            assert match
            mc,loader,name=match.groups()
            if row['kind']=='new-exact-screen-policy':
                assert loader=='fabric'and name=='WorkspaceReiOverlayPolicy';policies.add(mc)
            else:
                assert name==('SaltsReiClientPlugin'if loader=='fabric'else'SaltsReiForgeClientPlugin');plugins.add((mc,loader))
        assert plugins==required and policies==versions
    expected = {r['source']['file'] for r in rows+rei_rows}
    reporting = reporting or []
    if reporting:
        required = {'tools/reconcile-rei-workspace-policy-api.py', 'tools/reconcile-final-render-rei-delta.py', 'tools/focused_release_evidence_core_log_transition.py'}
        assert len(reporting)==3 and {r['file']for r in reporting}==required
        assert all(new[r['file']]==r['sha256']for r in reporting)
    assert changed == expected | {r['file']for r in reporting}, 'Unreviewed source change in normal build transition'
    for row in rows:
        name = row['source']['file']
        assert name.startswith('versions/') and name.endswith('/client/InventoryDesktopScreen.java')
        assert old[name] == row['source']['sha256']
        assert new[name] == row['prepared_source']['sha256']
    for row in rei_rows:
        name=row['source']['file']
        if row['kind']=='new-public-recipe-browser-screen-marker':
            assert name.startswith('versions/')and name.endswith('/api/client/recipebrowser/DesktopRecipeBrowserScreen.java')and row['source']['sha256']is None and name not in old
            assert new[name]==row['prepared_source']['sha256'];continue
        if row['kind']=='controller-public-marker-declaration-only':
            assert name.startswith('controlify/versions/')and name.endswith('/common/src/main/java/com/salts_inventory_update/controlify/RadialInventoryScreen.java')
            assert old[name]==row['source']['sha256']and new[name]==row['prepared_source']['sha256'];continue
        assert name.startswith('compatibility/versions/')and '/compat/rei/'in name and name.endswith('.java')
        if row['kind']=='new-exact-screen-policy':
            assert name.endswith('/WorkspaceReiOverlayPolicy.java')and row['source']['sha256']is None and name not in old
        else:
            assert name.endswith(('/SaltsReiClientPlugin.java','/SaltsReiForgeClientPlugin.java'))
            assert old[name]==row['source']['sha256']
        assert new[name]==row['prepared_source']['sha256']
    return sorted(changed)


def production_unchanged(compiled, current):
    old, new = compiled['source_files'], current['source_files']
    # This repository-root release changelog is reporting, not a compilation
    # input. Do not exclude other Markdown files: resources remain protected.
    selected = lambda n: n != 'compatibility/CHANGELOG.md' and (n.startswith(PRODUCTION) or n in ROOT_BUILD)
    assert {n: d for n, d in old.items() if selected(n)} == {n: d for n, d in new.items() if selected(n)}, 'Production sources changed after the compiled candidate'
    assert compiled['artifacts'] == current['artifacts'], 'The final distribution changed after the diagnostic build'


def ledger(artifacts, targets):
    assert len(artifacts) == 36 and len(set(targets)) == 12
    rows = {(v['minecraft'] + '/' + v['loader'], v['family']): v for v in artifacts}
    assert len(rows) == 36 and set(rows) == {(t, f) for t in targets for f in FAMILIES}
    assert len({v['file'] for v in artifacts}) == 36
    assert all(re.fullmatch('[a-f0-9]{64}', v['sha256']) for v in artifacts)
    return rows


def smokes(queue, targets):
    assert queue['status'] == 'passed' and len(queue['runs']) == 12
    rows = {r['target']: r for r in queue['runs']}
    assert len(rows) == 12 and set(rows) == set(targets), 'Missing/duplicate final target smoke'
    for row in rows.values():
        assert row['status'] == 'passed' and type(row['exit_code']) is int and row['exit_code'] == 0
        assert row.get('summary') and row.get('acceptance'), 'Real smoke evidence is required'
    return rows


def book_smokes(queue, prepared):
    """No book amendment acceptance without both actual named native runs."""
    assert queue['status'] == 'passed' and len(queue['runs']) == 2
    expected = prepared['book_workflows']
    assert len(expected) == 2 and {r['target'] for r in expected} == {'1.21.11/fabric', '1.21.11/neoforge'}
    assert queue['plan'] == expected
    rows = {r['target']: r for r in queue['runs']}
    assert len(rows) == 2 and set(rows) == {r['target'] for r in expected}
    for job in expected:
        row = rows[job['target']]
        assert all(row[k] == v for k, v in job.items())
        assert row['status'] == 'passed' and type(row['exit_code']) is int and row['exit_code'] == 0
        assert row.get('summary') and row.get('acceptance')
    return rows


def rei_smokes(queue):
    assert queue['status']=='passed'and queue['plan']==REI_PLAN and len(queue['runs'])==2
    rows={row['target']:row for row in queue['runs']}
    assert len(rows)==2 and set(rows)=={v['target']for v in REI_PLAN}
    for job in REI_PLAN:
        row=rows[job['target']]
        assert all(row[k]==value for k,value in job.items())
        assert row['status']=='passed'and type(row['exit_code'])is int and row['exit_code']==0
        assert row.get('summary')and row.get('acceptance')
    return rows


def derive(root, folder, build_record, diagnostic_record, current_identity, retained_final=None):
    """Recheck real artifacts and smokes; never rewrite historical results."""
    root, folder = Path(root).resolve(), Path(folder).resolve()
    assert folder.is_relative_to(root) and not folder.exists()
    build_path, diagnostics_path = checked(root, build_record), checked(root, diagnostic_record)
    build, diagnostics = json.loads(build_path.read_text()), json.loads(diagnostics_path.read_text())
    book_amendment = build.get('book_amendment')
    rei_amendment=build.get('rei_amendment')
    rei=None
    if rei_amendment:
        assert book_amendment,'The combined mode includes the already confirmed book correction'
        assert rei_amendment==dict(schema='exact-REI-public-workspace-marker-policy-v2',source_proposal=REI_PROPOSAL)
        rei=json.loads(checked(root,REI_PROPOSAL).read_text())
    if book_amendment:
        assert book_amendment['schema'] == 'final-Core-render-corrections-with-exact-12111-book-amendment-v1'
        assert book_amendment['source_proposal'] == build['source_proposal']
        assert build['status'] == ('build15-recorded-Core-render-and-REI-corrections-qualification-pending'if rei else 'build15-recorded-final-render-corrections-and-evidence-reconciliation-pending')
    else:
        assert build['status'] == 'build15-recorded-final-diagnostic-and-evidence-reconciliation-pending'
    assert len(build['actions']) == 6 and all(v['status'] == 'passed' and type(v['exit_code']) is int and v['exit_code'] == 0 for v in build['actions'])
    assert [v['name'] for v in build['actions']] == ['normal-build', 'integrity', 'compiled-delta', 'metadata', 'record-candidate', 'activate-candidate']
    assert build['actions'][0]['command'] == ['cmd.exe', '/d', '/c', 'gradlew.bat', 'build']
    preserved = json.loads(checked(root, build['preserved_build14']).read_text())
    assert preserved['status'] == 'all-build14-distribution-bytes-preserved'
    old_identity, new_identity = preserved['source_candidate'], build['candidate_after']
    before = json.loads(checked(root, old_identity['source_snapshot']).read_text())
    after = json.loads(checked(root, new_identity['source_snapshot']).read_text())
    current = json.loads(checked(root, current_identity['source_snapshot']).read_text())
    for snapshot, identity in [(before, old_identity), (after, new_identity)]:
        assert snapshot['review_source_archive'] == identity['source_archive']
        with zipfile.ZipFile(checked(root, identity['source_archive'])) as archive:
            names = archive.namelist()
            assert len(names) == len(set(names)) and set(names) == set(snapshot['source_files']) | {'CANDIDATE-SOURCE-IDENTITY.json'}
            archived = json.loads(archive.read('CANDIDATE-SOURCE-IDENTITY.json'))
            assert archived == dict(source_tree_sha256=snapshot['source_tree_sha256'], source_files=snapshot['source_files'])
            assert hashlib.sha256(json.dumps(snapshot['source_files'], sort_keys=True, separators=(',', ':')).encode()).hexdigest() == snapshot['source_tree_sha256']
            for name, digest in snapshot['source_files'].items():
                assert hashlib.sha256(archive.read(name)).hexdigest() == digest
    production_unchanged(after, current)
    prepared = json.loads(checked(root, build['source_proposal']).read_text())
    if book_amendment:
        assert prepared['schema'] == book_amendment['schema']
        finding = json.loads(checked(root, prepared['additional_book_finding']).read_text())
        assert finding['targets'] == ['1.21.11/fabric', '1.21.11/neoforge']
        assert finding['current_literal'] == 'textures/entity/enchantment/enchanting_table_book.png'
        assert finding['corrected_literal'] == 'textures/entity/enchanting_table_book.png'
    changed_sources = sources(before, after, prepared,rei,build.get('reporting_source_amendment'))
    applied_rows=prepared['rows']+([]if rei is None else rei['rows'])
    assert sorted(build['applied_sources'], key=lambda v: v['file']) == sorted([r['prepared_source'] | {'file': r['source']['file']} for r in applied_rows], key=lambda v: v['file'])
    retained_paths = {}
    if retained_final is not None:
        retained_build = json.loads(checked(root, retained_final).read_text())
        assert retained_build['status'] == 'normal-build16-exact-session-registration-fix-native-qualification-pending'
        preservation_path = checked(root, retained_build['preservation'])
        preservation = json.loads(preservation_path.read_text())
        assert preservation['status'] == 'all-build15-distribution-bytes-preserved'
        prior_ledger = json.loads((preservation_path.parent / 'candidate-integrity.json').read_text())
        assert prior_ledger['status'] == 'passed' and prior_ledger['artifacts'] == after['artifacts']
        originals = {row['original']: row for row in preservation['files']}
        assert len(originals) == len(preservation['files'])
        for artifact in after['artifacts']:
            item = originals[Path(artifact['file']).as_posix()]
            assert item['sha256'] == artifact['sha256']
            retained_paths[artifact['file']] = checked(root, dict(file=item['copy'], sha256=item['sha256']))
        changes = retained_build['source_changes']
        expected = {f'versions/{mc}/fabric/src/main/java/com/salts_inventory_update/client/InventoryDesktopScreen.java'
                    for mc in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']}
        assert len(changes) == 6 and {row['source'] for row in changes} == expected
        for row in changes:
            assert row['before']['sha256'] == after['source_files'][row['source']]
            retained_paths[row['source']] = checked(root, row['before'])
    for row in build['applied_sources']:
        if row['file'] in retained_paths:
            assert hashlib.sha256(retained_paths[row['file']].read_bytes()).hexdigest() == row['sha256']
        else:
            checked(root, row)
    diagnostic = load(root / 'functional-tests/release-specialized/run_core_render_diagnostic.py')
    targets = diagnostic.TARGETS
    old, new = ledger(before['artifacts'], targets), ledger(after['artifacts'], targets)
    smoke_rows = smokes(diagnostics, targets)
    # Compile the known inspector anew. A stored status string or reused helper
    # binary cannot establish that the actual archive payloads are equivalent.
    inspector_path = root / ('tools/reconcile-final-render-rei-delta.py'if rei else 'tools/reconcile-core-final-delta.py' if book_amendment else 'tools/reconcile-core-diagnostic-delta.py')
    inspector = load(inspector_path)
    tool = inspector.prepare(folder, Path('C:/Program Files/Java/jdk-26'))
    preserved_root = checked(root, build['preserved_build14']).parent
    old_ledger = reference(root, preserved_root / 'candidate-integrity.json')
    assert old_ledger['sha256'] == before['integrity_sha256']
    assert json.loads(checked(root, old_ledger).read_text())['artifacts'] == before['artifacts']
    pairs, accepted_smokes = [], []
    for target in targets:
        for family in sorted(FAMILIES):
            x, y = old[target, family], new[target, family]
            original, replacement = preserved_root / x['file'], retained_paths.get(y['file'], root / y['file'])
            assert inspector.sha(original) == x['sha256'] and inspector.sha(replacement) == y['sha256']
            if family != 'core' and not rei:
                assert x['sha256'] == y['sha256'], 'Compatibility and Controlify must remain whole-byte identical'
            scope = inspector.mode(target, family) if book_amendment else ('core' if family == 'core' else 'unchanged')
            check = inspector.execute(tool, 'audit-jar', original, replacement, scope)
            log = folder / (target.replace('/', '-') + '-' + family + '.log')
            log.write_text(check.stdout + check.stderr)
            assert check.returncode == 0, 'Actual archive changed beyond the reviewed correction scope: ' + str(log)
            pairs.append(dict(target=target, family=family, original=reference(root, original), final=reference(root, replacement),
                              exit_code=check.returncode, log=reference(root, log)))
        row = smoke_rows[target]
        summary = json.loads(checked(root, row['summary']).read_text())
        assert summary['target'] == target and summary['acceptance'] == row['acceptance']
        wanted = {'core': (Path(new[target, 'core']['file']).name, new[target, 'core']['sha256'])}
        diagnostic.verify_run(root, summary, wanted)
        accepted_smokes.append(dict(target=target, summary=row['summary'], acceptance=row['acceptance']))
    additional = {}
    if book_amendment:
        book_queue = reference(root, root / 'output/release-readiness/final-core-enchanting-book-01.json')
        rows = book_smokes(json.loads(checked(root, book_queue).read_text()), prepared)
        driver = load(root / 'functional-tests/release-specialized/run_core_enchanting_book.py')
        accepted_books = []
        for target, row in sorted(rows.items()):
            summary = json.loads(checked(root, row['summary']).read_text())
            assert summary['target'] == target and summary['acceptance'] == row['acceptance']
            wanted = {'core': (Path(new[target, 'core']['file']).name, new[target, 'core']['sha256'])}
            driver.verify_run(root, summary, wanted)
            accepted_books.append(dict(target=target, summary=row['summary'], acceptance=row['acceptance']))
        additional = dict(book_amendment=book_amendment, actual_book_queue=book_queue, real_book_render_reload_runs=accepted_books,
            original_strict_inspector=reference(root, root / 'tools/reconcile-core-diagnostic-delta.py'))
    if rei:
        queue=reference(root,root/'output/release-readiness/final-rei-workspace-policy-01.json')
        native=json.loads(checked(root,queue).read_text())
        native_rows=rei_smokes(native)
        driver=load(root/'functional-tests/release-specialized/run_rei_workspace_policy.py')
        accepted=[]
        for job in REI_PLAN:
            row=native_rows[job['target']]
            summary=json.loads(checked(root,row['summary']).read_text());target=row['target']
            expected={family:(Path(new[target,family]['file']).name,new[target,family]['sha256'])for family in FAMILIES}
            if retained_final is None:
                driver.verify_run(root,summary,expected)
            else:
                driver.verify_retained_run(root,summary,expected,preservation_path.parent/'dist')
            assert summary['acceptance']==row['acceptance']
            accepted.append(dict(target=target,summary=row['summary'],acceptance=row['acceptance']))
        additional.update(rei_amendment=rei_amendment,actual_REI_queue=queue,real_REI_browser_runs=accepted)
    return dict(schema=REI_SCHEMA if rei else FINAL_SCHEMA if book_amendment else SCHEMA,
                status='render-and-REI-corrections-artifact-equivalence-verified-no-coverage-assigned'if rei else 'render-corrections-artifact-equivalence-verified-no-coverage-assigned' if book_amendment else 'diagnostic-only-artifact-equivalence-verified-no-coverage-assigned',
                before=old_identity, after=new_identity, current_candidate=current_identity,
                preserved_original_subject_ledger=old_ledger,
                build=build_record, diagnostics=diagnostic_record, changed_source_paths=changed_sources,
                inspector=reference(root, inspector_path), fresh_inspector=tool,
                archive_pairs=pairs, real_detailed_render_smokes=accepted_smokes,
                retention_rule='Original passes retain their original subjects, source/runtime archives, failures and scope. Each caller must separately validate original completion, unchanged QA, exact native publisher and latest-attempt precedence. No additional execution, assertion or feature is created.',
                limits=('Only the known logger/book corrections and exact optional REI workspace policy, with two book/reload and two real REI/browser workflows. 'if rei else 'Only the known logger correction and exact1.21.11book-field identifier, with two real book/reload checks. ' if book_amendment else 'Only the known render logger format/argument correction. ')
                    + 'This is not UI coverage, a gameplay-suite pass, independent approval or public-release acceptance. Existing13->14retention and all native case-specific evidence must still be independently verified.', **additional)
