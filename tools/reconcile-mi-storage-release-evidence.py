"""Bind actual MI storage transfers and native controls to 34 exact UI records.

This rechecks the original typed observations, not just passing journal flags.
Declared stored resources are prerequisites, never recipe/generation evidence.
"""
from pathlib import Path
import argparse
import copy
import datetime
import hashlib
import json
import re
import sys

import nbtlib

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
sys.path.insert(0, str(ROOT / 'functional-tests/release-specialized'))
from focused_release_evidence import checked, reference, verify as verify_sealed
from mi_owned_geometry import check_owned
from native_publisher_index import index, matches
from mi_storage_features import (FLUIDS, OUTPUTS, amount, cell_state, cursor_is,
    item_variant, physical, physical_item, policies, resources, seeded, original, typed)
from mi_storage_controls import (button as _native_button, capabilities, expected_counts, extracting,
    lock_change, native_controls as _native_controls)
from mowzie_features import stack as _native_stack,item_count
from mi_storage_contracts import (transfer_policies,observed_stack,valid_carried_stack,
    output_examples,execution_cases as case_order,native_item_variant as item_variant)


def stack(text):return observed_stack(text,_native_stack)

def native_controls(client,mc,kind,observed):
    return check_owned(lambda scoped:_native_controls(scoped,mc,kind,observed),client)

def button(client,mc,kind,control,enabled,observed):
    return check_owned(lambda scoped:_native_button(scoped,mc,kind,control,enabled,observed),client)


TARGETS = ['1.20.1/fabric', '1.21.1/neoforge']
PREFIX = 'machine-modern_industrialization-'
CASES = [PREFIX + name for name in FLUIDS + OUTPUTS]
SERVER = 'aztech.modern_industrialization.machines.gui.MachineMenuServer'
DRIVER = 'functional-tests/release-specialized/mi_storage_features.py'
CONTROLS = 'functional-tests/release-specialized/mi_storage_controls.py'
SCHEMA = 'mi-native-storage-transfers-and-controls-v1'
STATUS = 'functional-pass'
SCOPE = ('Twelve native fluid panels and five output-item hatches: actual first/last-cell '
    'bucket and full-metadata item transfers, native insertion/refusal policy, exact '
    'resource and physical ledgers, native-provided lock/eject controls, restoration '
    'and ordinary reopening on the exact staged release.')
LIMITS = ('Storage UI workflow acceptance only. Stored water/items are declared pre-entry '
    'prerequisites. No recipe production, generated fluids/items, formed multiblocks, '
    'adjacent-container ejection automation or whole-mod gameplay claim. Absent native '
    'controls are recorded as absent, never clicked or counted as tested controls.')


def native_classes(target):
    assert target in TARGETS
    prefix = 'aztech.modern_industrialization.' + ('' if target == TARGETS[0] else 'client.') + 'machines.'
    return [SERVER, prefix + 'gui.MachineMenuClient', prefix + 'gui.MachineScreen']


def specification(target, case, classes):
    native_classes(target)
    assert case in CASES and classes == [SERVER], 'Unreviewed native variant/alias/class'
    return 'fluids' if case.removeprefix(PREFIX) in FLUIDS else 'items'


def progressing_cell_state(server,client,kind,case,mc):
    assert kind=='fluids' and case in {PREFIX+'bronze_water_pump',PREFIX+'steel_water_pump'}
    stored=nbtlib.parse_nbt(server['fixture'])[kind];visible=client['cells'][kind]
    assert len(stored)==len(visible)==2
    observed=[]
    for i,row in enumerate(visible):
        assert row['index']==i
        value=nbtlib.parse_nbt(row['nbt']);observed.append(value)
        assert typed(nbtlib.Compound({k:v for k,v in value.items()if k!='amount'}))==typed(nbtlib.Compound({k:v for k,v in stored[i].items()if k!='amount'}))
    unit=81000 if mc=='1.20.1'else 1000;key='fluid'if mc=='1.20.1'else'id'
    steam=nbtlib.parse_nbt('{'+key+':"modern_industrialization:steam"}')
    assert typed(stored[0]['key'])==typed(steam)
    assert 0<amount(observed[0])<=amount(stored[0])<unit//10
    assert amount(stored[0])%(unit//1000)==amount(observed[0])%(unit//1000)==0
    assert amount(stored[1])==amount(observed[1])==0
    return stored


def common(frame, target, case, kind, active_pump=False):
    assert type(active_pump)is bool and (not active_pump or kind=='fluids'and case in {PREFIX+'bronze_water_pump',PREFIX+'steel_water_pump'})
    assert set(frame) == {'client', 'server', 'observed'}
    client, server, observed = (frame[key] for key in ['client', 'server', 'observed'])
    window, _ = check_owned(original.geometry.check,client)
    assert window['menuId'] == 'modern_industrialization:machine'
    classes = native_classes(target)
    assert observed['menu'] == classes[1] and observed['screen'] == classes[2]
    tag = nbtlib.parse_nbt(server['fixture'])
    assert isinstance(tag, nbtlib.Compound) and tag.get('id') == nbtlib.String('modern_industrialization:' + case.removeprefix(PREFIX))
    assert all(isinstance(tag.get(key), nbtlib.Int) and int(tag[key]) == value
        for key, value in [('x', 8), ('y', 65), ('z', 8)])
    inventory = server['inventory']
    assert isinstance(inventory, list) and len(inventory) >= 36
    assert all(type(row['index']) is int and row['index'] == i for i, row in enumerate(inventory))
    assert seeded(server, target.split('/')[0]), 'Missing original full typed foreign seed'
    menus = server['menus']
    assert isinstance(menus, list) and sum(v.get('class') == SERVER for v in menus) == 1
    assert all(isinstance(v.get('class'), str) and valid_carried_stack(v['carried'],item_count) for v in menus)
    assert set(observed['cells']) == {'items', 'fluids'}
    for resource_kind in ['items', 'fluids']:
        visible = observed['cells'][resource_kind]
        assert isinstance(visible, list)
        for cell in visible:
            assert type(cell['x']) is int and type(cell['y']) is int
            assert original.geometry.retained(window['template']['regions'],
                dict(x=cell['x'] - 1, y=cell['y'] - 1, width=18, height=18)), 'Cut native virtual cell rim'
        if visible:
            other = progressing_cell_state(server,observed,resource_kind,case,target.split('/')[0])if active_pump and resource_kind=='fluids'else cell_state(server, observed, resource_kind)
            if resource_kind != kind:
                assert resources(other) == (), 'Unexpected resource in other native storage kind'
        else:
            assert len(tag.get(resource_kind, [])) == 0, 'Hidden native resources'
    return server, observed, (progressing_cell_state(server,observed,kind,case,target.split('/')[0])if active_pump else cell_state(server, observed, kind)) if kind is not None else None


def probe_contract(probe, kind):
    assert set(probe) == {'kind', 'native_count', 'indices'} and probe['kind'] == kind
    count = probe['native_count']
    assert type(count) is int and count > 0
    wanted = [0, count - 1] if count > 1 else [0]
    assert isinstance(probe['indices'], list) and all(type(v) is int for v in probe['indices']) and probe['indices'] == wanted
    return wanted


def fluid_transfers(target, case, probe, frames):
    indices = probe_contract(probe, 'fluids')
    assert len(frames) == 5 * len(indices)
    mc = target.split('/')[0]
    water = nbtlib.parse_nbt('{fluid:"minecraft:water"}' if mc == '1.20.1' else '{id:"minecraft:water"}')
    unit = 81000 if mc == '1.20.1' else 1000
    checks = []
    for cycle, index_ in enumerate(indices):
        group = frames[cycle * 5:(cycle + 1) * 5]
        server, observed, cells = common(group[0][1], target, case, 'fluids')
        assert len(cells) == probe['native_count'] and resources(cells) == () and cursor_is(server)
        assert bool(cells[index_].get('playerExtract', False))
        inserts = bool(cells[index_].get('playerInsert', False))
        labels = ['empty', 'prerequisites', 'native-first-transfer',
            'water-recovered' if inserts else 'reinsertion-refused', 'reopened']
        assert [v[0] for v in group] == labels
        baseline, policy = physical(server, exclude=(9,)), transfer_policies(cells,case,mc,policies)
        filled = ((index_, unit, typed(water)),)
        for stage, (_, frame) in enumerate(group):
            data, observed, actual = common(frame, target, case, 'fluids')
            assert len(actual) == len(cells) and transfer_policies(actual,case,mc,policies) == policy and physical(data, exclude=(9,)) == baseline
            wanted = filled if (stage == 1 and not inserts) or (stage == 2 and inserts) else ()
            assert resources(actual) == wanted, 'Native fluid amount/key or other-cell ledger changed'
            if stage in [0, 1, 4]:
                assert cursor_is(data)
                item = None if stage == 0 else ('minecraft:water_bucket' if inserts or stage == 4 else 'minecraft:bucket')
                assert stack(data['inventory'][9]['item']) == stack(physical_item(mc, item, 1) if item else '{}')
            else:
                expected = 'minecraft:bucket' if stage == 2 and inserts else 'minecraft:water_bucket'
                assert cursor_is(data, physical_item(mc, expected, 1))
                assert stack(data['inventory'][9]['item']) == stack('{}')
        checks += ['exact foreign typed fixture and empty cursor', 'native flags and declared water prerequisite',
            'actual native bucket transfer accounts for water and container',
            'native inverse transfer recovers exactly one water bucket' if inserts else 'output policy refuses water reinsertion without loss',
            'fluid result and full physical ledger persist']
    checks.append('native first and last fluid cells both completed')
    return checks


def item_transfers(target, case, probe, frames):
    indices = probe_contract(probe, 'items')
    examples = output_examples(probe['native_count'])
    assert [v[0]for v in examples]==indices
    destinations=tuple(v[3]for v in examples)
    assert [v[0] for v in frames] == ['empty', 'declared-stored-output', *[label + '-' + str(i)
        for i in indices for label in ['picked', 'refused', 'reopened']]]
    mc = target.split('/')[0]
    initial, observed, cells = common(frames[0][1], target, case, 'items')
    assert len(cells) == probe['native_count'] and cursor_is(initial)
    assert all(not bool(v.get('playerInsert', False)) and bool(v.get('playerExtract', False)) and amount(v) == 0 for v in cells)
    baseline, policy = physical(initial, exclude=destinations), policies(cells)
    assert all(stack(initial['inventory'][i]['item']) == stack('{}') for i in [9, 10])
    metadata = '{release_mi_output_marker:19,typed:[B;4b,8b]}'
    seeded_cells = copy.deepcopy(cells)
    for i, item, number, _ in examples:
        seeded_cells[i]['amount'] = nbtlib.Long(number)
        seeded_cells[i]['key'] = item_variant(mc, item, metadata)
    data, observed, actual = common(frames[1][1], target, case, 'items')
    assert typed(actual) == typed(seeded_cells) and cursor_is(data) and physical(data) == physical(initial)
    remaining = dict((row[0], row) for row in resources(seeded_cells))
    recovered = {9: '{}', 10: '{}'}
    checks = ['exact foreign typed fixture and empty cursor', 'stored output metadata and all native policy flags are exact']
    for cycle, (i, item, number, destination) in enumerate(examples):
        expected = physical_item(mc, item, number, metadata)
        del remaining[i]
        for stage, (_, frame) in enumerate(frames[2 + cycle * 3:5 + cycle * 3]):
            data, observed, actual = common(frame, target, case, 'items')
            assert len(actual) == len(cells) and resources(actual) == tuple(remaining.values()) and policies(actual) == policy
            assert physical(data, exclude=destinations) == baseline
            if stage == 2:
                recovered[destination] = expected
                assert cursor_is(data)
            else:
                assert cursor_is(data, expected)
            assert all(stack(data['inventory'][slot]['item']) == stack(value) for slot, value in recovered.items())
        checks += ['native pickup preserves exact output ' + str(i), 'native output-only reinsertion is refused ' + str(i),
            'recovered full output persists ' + str(i)]
    checks.append('all declared output recovered and first and last cells completed')
    return checks


def control_contract(target, case, kind, frames, transfer_end, caps):
    mc = target.split('/')[0]
    counts = expected_counts(caps)
    assert caps['kind'] == kind and len(frames) == counts[1]
    labels = ['initial']
    names = ['native storage exposes exactly its declared controls']
    if caps['lock']:
        labels += ['lock-editor-enabled', 'first-cell-lock-policy']
        names += ['native lock editor enables without changing resources', 'native first storage cell respects its player-lock policy']
    if caps['eject']:
        labels.append('auto-eject-toggled')
        names.append('native auto-eject changes exact server option conservatively')
    labels.append('locked-option-reopened')
    names.append('native available storage controls persist on reopening')
    if caps['lock']:
        labels += ['restoring-lock-editor-enabled', 'first-cell-lock-restored', 'lock-editor-disabled']
        names += ['native lock editor restores only the selected empty cell', 'native lock editor disables without changing storage']
    if caps['eject']:
        labels.append('auto-eject-restored')
        names.append('native auto-eject restores original option and full item ledger')
    labels.append('controls-restored-reopened')
    names.append('native storage controls remain restored after reopening')
    assert [v[0] for v in frames] == labels and len(names) == counts[0]
    initial, observed, initial_cells = common(frames[0][1], target, case, kind)
    previous, _, previous_cells = common(transfer_end, target, case, kind)
    assert typed(initial_cells) == typed(previous_cells) and physical(initial) == physical(previous)
    assert resources(initial_cells) == () and cursor_is(initial)
    if caps['lock']:
        assert not bool(initial_cells[0].get('playerLocked', False))
    initial_extract = extracting(initial, kind)
    locked_cells = None
    before_reopen = {'lock-editor-enabled', 'first-cell-lock-policy', 'auto-eject-toggled'}
    for label, frame in frames:
        data, observed, cells = common(frame, target, case, kind)
        assert physical(data) == physical(initial) and cursor_is(data) and resources(cells) == ()
        _, _, actual_caps = native_controls(frame['client'], mc, kind, observed)
        assert actual_caps == caps, 'Native control capability changed between observations'
        locked_mode = label in before_reopen or label in {'restoring-lock-editor-enabled', 'first-cell-lock-restored'}
        if caps['lock']:
            button(frame['client'], mc, kind, 'lock', locked_mode, observed)
        active = caps['eject'] and label in {'auto-eject-toggled', 'locked-option-reopened',
            'restoring-lock-editor-enabled', 'first-cell-lock-restored', 'lock-editor-disabled'}
        expected_extract = not initial_extract if active else initial_extract
        assert extracting(data, kind) is expected_extract
        if caps['eject']:
            button(frame['client'], mc, kind, 'eject', expected_extract, observed)
        if label == 'first-cell-lock-policy':
            assert lock_change(initial_cells, cells, 0), 'Native cell lock policy/typed flags changed incorrectly'
            locked_cells = cells
        elif label in {'auto-eject-toggled', 'locked-option-reopened', 'restoring-lock-editor-enabled'} and caps['lock']:
            assert locked_cells is not None and typed(cells) == typed(locked_cells)
        else:
            assert typed(cells) == typed(initial_cells)
    return names


def validate(target, journal, transfers, controls, driver_hash, controls_hash):
    native_classes(target)
    assert journal['complete'] is True and journal['driver_sha256'] == driver_hash and journal['controls_driver_sha256'] == controls_hash
    execution_cases=case_order(FLUIDS,OUTPUTS)
    assert journal['cases'] == execution_cases and journal['control_cases'] == execution_cases
    assert all(set(v) == set(CASES) for v in [journal['probed_cells'], journal['control_capabilities'], journal['control_counts'], transfers, controls])
    expected = []
    for case in execution_cases:
        kind = specification(target, case, [SERVER])
        probe = journal['probed_cells'][case]
        names = (fluid_transfers if kind == 'fluids' else item_transfers)(target, case, probe, transfers[case])
        expected += [(case, name) for name in names]
        caps = journal['control_capabilities'][case]
        counts = expected_counts(caps)
        assert journal['control_counts'][case] == dict(checks=counts[0], frames=counts[1])
        names = control_contract(target, case, kind, controls[case], transfers[case][-1][1], caps)
        expected += [(case, name) for name in names]
    expected.append((execution_cases[-1], 'all native MI storage sessions close'))
    assert [(v['case'], v['name']) for v in journal['checks']] == expected
    assert all(v.get('passed') is True for v in journal['checks'])
    frames = sum(len(v) for v in controls.values())
    assert type(journal['control_frames']) is int and journal['control_frames'] == frames
    return dict(assertions=len(expected), transfer_states=sum(len(v) for v in transfers.values()),
        control_states=frames, native_variants=17, contract='native-storage-transfers-and-available-controls-not-production')


def observations(root, folder, sealed):
    grouped = {case: [] for case in CASES}
    refs = []
    for number, path in enumerate(sorted(folder.glob('[0-9][0-9][0-9]-*.json')), 1):
        match = re.fullmatch(r'([0-9]{3})-(machine-modern_industrialization-[a-z_]+)-([a-z0-9-]+)\.json', path.name)
        assert match and int(match[1]) == number and match[2] in grouped, 'Unordered/unknown native state'
        ref = reference(root, path)
        assert ref in sealed['full_states']
        refs.append(ref)
        grouped[match[2]].append((match[3], json.loads(path.read_text())))
    assert refs, 'Native observations missing'
    return grouped, refs


def native_publishers(root, sealed, classes=None):
    root = Path(root).resolve()
    target = sealed['target']
    publishers = []
    classes = native_classes(target) if classes is None else classes
    staged = json.loads(checked(root, sealed['staged']).read_text())['artifacts']
    for artifact in staged:
        if artifact['family'] != 'upstream' or not artifact['filename'].lower().startswith('modern-industrialization-'):
            continue
        path = Path(artifact['file'])
        path = path if path.is_absolute() else root / path
        raw = path.read_bytes()
        assert hashlib.sha256(raw).hexdigest() == artifact['sha256']
        digest = hashlib.sha512(raw).hexdigest()
        if 'sha512' in artifact:
            assert artifact['sha512'] == digest
        locations = matches(index(path, digest, target.split('/')[1]), classes)
        assert {name[:-6].replace('/', '.') for loc in locations for name in loc['classes']} == set(classes)
        publishers.append(dict(filename=artifact['filename'], sha256=artifact['sha256'], sha512=digest, class_locations=locations))
    assert len(publishers) == 1, 'Exactly one actual native MI publisher required'
    return publishers


def derive(root, sealed_path, expected, case, original_classes):
    root = Path(root).resolve()
    sealed = json.loads(Path(sealed_path).read_text())
    target = sealed['target']
    specification(target, case, original_classes)
    verify_sealed(root, sealed, expected)
    summary = json.loads(checked(root, sealed['summary']).read_text())
    row = summary['results'][0]
    result = row['results']
    assert result.get('mi_storage_features_checked') is True and result.get('mi_storage_controls_checked') is True
    assert type(result['cases']) is int and result['cases'] == 17 and type(result['failed']) is int and result['failed'] == 0
    profile = (root / row['profile']).resolve()
    journal_path = checked(root, sealed['acceptance'])
    assert journal_path == profile / 'mi-storage-release/summary.json'
    transfers, transfer_refs = observations(root, journal_path.parent, sealed)
    controls, control_refs = observations(root, journal_path.parent / 'native-controls', sealed)
    journal = json.loads(journal_path.read_text())
    validation = validate(target, journal, transfers, controls, sealed['archived_inputs'][DRIVER], sealed['archived_inputs'][CONTROLS])
    assert type(result['checks']) is int and result['checks'] == validation['assertions']
    publishers = native_publishers(root, sealed)
    classes = native_classes(target)
    return dict(schema=SCHEMA, target=target, case=case, disposition=STATUS, original_native_classes=original_classes,
        native_classes=classes, subjects={k: list(v) for k, v in expected.items()}, sealed_evidence=reference(root, sealed_path),
        acceptance=reference(root, journal_path), native_states=transfer_refs + control_refs,
        validation=validation, publishers=publishers, scope=SCOPE, limits=LIMITS)


def verify(root, record, expected):
    assert record['schema'] == SCHEMA and record['disposition'] == STATUS
    assert derive(root, checked(root, record['sealed_evidence']), expected, record['case'], record['original_native_classes']) == record
    return True


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--generation', required=True)
    parser.add_argument('--plan-only', action='store_true')
    args = parser.parse_args()
    assert re.fullmatch(r'build[0-9]+', args.generation)
    out = ROOT / 'output/release-readiness'
    backlog = json.loads((out / 'coverage-backlog.json').read_text())
    plan = json.loads((ROOT / 'build/release-readiness/focused-native-plan-02.json').read_text())
    jobs = [v for v in plan['jobs'] if v['family'] == 'mi_storage']
    assert len(jobs) == 2 and {v['target'] for v in jobs} == set(TARGETS)
    jobs = {v['target']: v for v in jobs}
    rows = [r for r in backlog['records'] if r['case'] in CASES]
    assert len(rows) == 34 and {(r['target'], r['case']) for r in rows} == {(t, c) for t in TARGETS for c in CASES}
    if args.plan_only:
        for row in rows:
            print(row['target'], row['case'], 'requires actual sealed', jobs[row['target']]['run'])
        print('Storage workflow plan only; no coverage or recipe/generation acceptance.')
        return
    sys.path.insert(0, str(ROOT / 'functional-tests/project-everything-regression'))
    import hashlib,importlib.util
    helper=ROOT/'build/release-readiness/release_queue_history_455.py'
    assert hashlib.sha256(helper.read_bytes()).hexdigest()=='fb00a5e47131d9ca30263154439a17f13ea3661eef2651febe9a9fa75fa3affc'
    sp=importlib.util.spec_from_file_location('MI_original_native_history',helper);history=importlib.util.module_from_spec(sp);sp.loader.exec_module(history)
    resolved=history.completed(plan,json.loads((out/'focused-native-campaign-02.json').read_text()));assert len(resolved)==174
    jobs={v['target']:v for v in resolved if v['family']=='mi_storage'};assert set(jobs)==set(TARGETS)
    from subject_artifacts import current_subjects
    from release_candidate_identity import current
    from focused_release_evidence_core_log_consumer import verified
    candidate = current(ROOT, verify_worktree=True)
    records = []
    consumer=verified(ROOT,args.generation,candidate)
    for row in rows:
        sealed = out / ('focused-evidence-' + jobs[row['target']]['run'] + '.json')
        record = derive(ROOT, sealed, consumer.expected(row['target'],json.loads((out/('focused-evidence-'+jobs[row['target']]['run']+'.json')).read_text()),current_subjects(row['target'])), row['case'], row.get('historical_native_classes', row['native_classes']))
        assert row['current_exact_pins'] and {v['sha512'] for v in row['current_exact_pins']} <= {v['sha512'] for v in record['publishers']}, 'Unqualified native MI publisher patch'
        records.append(record)
    destination = out / f'coverage-mi-storage-{args.generation}-01.json'
    assert not destination.exists()
    destination.write_text(json.dumps(dict(status='reviewed-native-mi-storage-workflows-reconciled',
        created=datetime.datetime.now(datetime.timezone.utc).isoformat(), candidate_identity=candidate,
        artifact_equivalence=consumer.report(),auditor_inputs=[reference(ROOT, ROOT/'tools/mi_storage_contracts.py'),reference(ROOT, ROOT/'tools/mi_owned_geometry.py'),reference(ROOT,helper),reference(ROOT, Path(__file__))], records=records, limits=LIMITS), indent=2) + '\n')
    print('Reconciled34 storage UI records from two actual17-variant workflows; no new gameplay execution or production claim.')


if __name__ == '__main__':
    main()
