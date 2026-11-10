"""Reconcile explicit Tom UI aliases from sealed original native workflows.

This is a reporting tool. It never launches Minecraft, changes a case result,
or treats a different publisher pin as equivalent.
"""
from pathlib import Path, PurePosixPath
from collections import Counter
import argparse, datetime, hashlib, json, sys, re
from focused_release_evidence import checked, reference, verify as verify_sealed
from native_publisher_index import index, matches

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'functional-tests/project-everything-regression'))
from subject_artifacts import current_subjects

SCHEMA = 'toms-original-observations-v1'
DRIVER = 'functional-tests/project-everything-regression/toms_release_workflows.py'
TARGETS = ['1.20.1/fabric','1.20.1/forge', *[mc+'/'+loader for mc in
    ['1.21.1','1.21.11','26.1.2','26.2','26.3'] for loader in ['fabric','neoforge']]]
ALIASES = {
    'toms-storage-terminal': 'terminal', 'atlas-toms-storage-terminal': 'terminal',
    'registry-item-toms_storage-inventory_configurator': 'configurator',
    'atlas-toms-inventory-configurator': 'configurator',
    'atlas-toms-inventory-link': 'link',
    'toms-config': 'vending', 'atlas-toms-config': 'vending',
    'atlas-toms-customer': 'vending', 'toms-customer': 'vending',
}
CHECK_NAMES = {
    'terminal': [
        'real-connected-network-publishes-its-initial-stock',
        'native-window-is-moved-before-its-controls-are-exercised',
        'terminal-search-filters-the-real-catalogue', 'unmatched-search-hides-all-catalogue-cells',
        'clearing-search-restores-connected-stock',
        *['terminal-requested-cell-is-in-the-visible-grid'] * 4,
        'terminal-deposit-reaches-the-real-chest-once-with-complete-data',
        'terminal-receives-the-native-stock-increase',
        'terminal-withdrawal-preserves-exact-count-and-components',
        'terminal-withdrawal-returns-once-to-separate-inventory',
        'terminal-reopening-restores-persisted-native-stock',
        'live-terminal-refreshes-after-an-external-native-storage-update',
        'closing-terminal-with-withdrawal-keeps-every-item-on-the-shared-cursor',
        'closed-terminal-withdrawal-is-deposited-once',
    ],
    'configurator': [
        'native-window-is-moved-before-its-controls-are-exercised',
        *['configurator-' + key + '-updates-the-native-block-filter' for key in ['priority','side','skip','keepLast']],
        'remove-connected-retains-the-main-block-filter',
        'configurator-real-filter-slot-accepts-the-full-tagged-item',
        'configurator-settings-and-filter-persist-through-native-reopening',
        'configurator-filter-returns-once-with-its-complete-data',
        'empty-filter-cannot-open-a-native-filter-editor', 'clear-configuration-restores-native-defaults',
    ],
    'link': [
        'real-beacon-enables-the-native-channel-interface',
        'native-window-is-moved-before-its-controls-are-exercised',
        'name-entry-and-add-create-eight-native-owned-channels',
        'native-channel-snapshot-matches-the-rendered-list', 'wheel-scroll-reaches-the-next-channel-row',
        'scrolled-list-click-selects-the-channel-drawn-there', 'native-scrollbar-drag-restores-the-first-row',
        'visible-channel-selection-updates-the-real-connector',
        'native-privacy-toggle-keeps-owner-and-updates-channel-access',
        'delete-removes-only-the-selected-native-channel', 'native-channel-list-and-selection-persist-through-reopening',
    ],
    'vending': [
        'vending-uses-real-stock-and-noncreative-offer', 'native-trading-view-control-visible',
        'native-trading-view-opens-one-customer-source-for-the-same-stock',
        'customer-window-moves-before-the-native-trade-click', *['native-trade-control-visible'] * 2,
        'purchase-consumes-payment-and-stock-on-the-server', 'purchase-retains-complete-native-stack-data',
        'native-refused-purchase-cannot-create-output-or-consume-stock',
        'offer-payment-and-stock-persist-through-native-reopening',
    ],
}
SCOPES = {
    'terminal': 'Connected native chest/network stock; real search, deposit, withdrawal, external-stock refresh, moved input, full typed item recovery and source close/reopen.',
    'configurator': 'Original configurator item entry; real block-filter priority, side, skip, keep-last, connected removal, tagged filter ownership, missing-editor prerequisite, defaults and persistence.',
    'link': 'Real beacon prerequisite; eight owned native channels, wheel and scrollbar navigation, actual selection, privacy flag, selected deletion and persistence.',
    'vending': 'Original owner and Trading View child; noncreative stocked offer, one real payment/purchase, full item data, no-payment refusal, moved input and persisted owner stock/configuration.',
}


def validate_checks(group, checks):
    assert group in CHECK_NAMES and isinstance(checks, list)
    assert all(v.get('passed') is True for v in checks), 'Every original assertion must explicitly pass'
    assert Counter(v['check'] for v in checks) == Counter(CHECK_NAMES[group]), 'Missing, duplicated or changed original feature assertion'
    return len(checks)


def specification(target, case, original_classes):
    assert target in TARGETS, 'Unreviewed Tom target'
    assert case in ALIASES, 'Unreviewed Tom UI alias'
    assert isinstance(original_classes, list) and original_classes and len(original_classes) == len(set(original_classes))
    mc, loader = target.split('/')
    assert loader in ['fabric','forge','neoforge']
    old = mc == '1.20.1'
    group = ALIASES[case]
    assert not (old and group == 'configurator'), 'Legacy native Tom has no inventory configurator'
    storage = 'com.tom.storagemod.' + ('gui.' if old else 'menu.')
    trading = 'com.tom.trading.' + ('gui.' if old else 'menu.')
    owner, customer = trading + 'VendingMachineConfigMenu', trading + 'VendingMachineTradingMenu'
    menus = {'terminal':[storage+'StorageTerminalMenu'], 'configurator':[storage+'InventoryConfiguratorMenu'],
             'link':[storage+'InventoryLinkMenu'], 'vending':[owner,customer]}[group]
    corrected = False
    if case == 'toms-customer':
        native = ['com.tom.trading.' + ('gui.' if old else 'screen.') + 'VendingMachineTradingScreen']
    elif case in ['toms-config','atlas-toms-config']:
        native = [owner]
    elif case == 'atlas-toms-customer':
        native = [owner,customer]
    else:
        native = menus
    if original_classes != native:
        assert case == 'registry-item-toms_storage-inventory_configurator' and original_classes == [owner], 'Unreviewed native-class correction'
        corrected = True
    return dict(group=group, native_classes=native, required_menus=menus, corrected=corrected,
                required_screen=native[0] if case == 'toms-customer' else None)


def observations(root, folder, group, sealed_refs):
    """Verify every immediate getter/transport archive belongs to the sealed run."""
    root, folder = Path(root).resolve(), Path(folder).resolve()
    index_path = folder / 'observations/index.json'
    index_ref = reference(root, index_path)
    assert index_ref in sealed_refs, 'Native observation index is not sealed'
    value = json.loads(index_path.read_text(encoding='utf-8'))
    assert set(value) == {'schema','group','observations'} and value['schema'] == SCHEMA and value['group'] == group
    refs = value['observations']
    assert isinstance(refs,list) and refs and len({v['file'] for v in refs}) == len(refs)
    results = []
    for ordinal, ref in enumerate(refs, 1):
        key = PurePosixPath(ref['file'])
        assert not key.is_absolute() and len(key.parts) == 2 and key.parts[0] == 'observations' and '..' not in key.parts
        path = folder / key
        current = reference(root, path)
        assert current['sha256'] == ref['sha256'] and current in sealed_refs, 'Native observation is changed or unsealed'
        data = json.loads(path.read_text(encoding='utf-8'))
        assert set(data) == {'schema','ordinal','group','kind','request','result'}
        assert data['schema'] == SCHEMA and data['group'] == group and type(data['ordinal']) is int and data['ordinal'] == ordinal
        assert data['kind'] in ['reply','server-state','native-state']
        assert key.name == str(ordinal).zfill(5) + '-' + data['kind'] + '.json'
        assert isinstance(data['request'],dict) and isinstance(data['result'],dict)
        results.append(data)
    assert {'reply','server-state'} <= {v['kind'] for v in results}
    if group != 'vending':
        assert 'native-state' in {v['kind'] for v in results}
    return index_ref, results


def derive(root, sealed_path, expected, case, original_classes):
    root = Path(root).resolve()
    sealed = json.loads(Path(sealed_path).read_text(encoding='utf-8'))
    verify_sealed(root, sealed, expected)
    spec = specification(sealed['target'], case, original_classes)
    summary_path = checked(root, sealed['summary'])
    summary = json.loads(summary_path.read_text(encoding='utf-8'))
    assert summary['results'][0]['results']['toms_existing_observations_archived'] is True
    acceptance = checked(root, sealed['acceptance'])
    assert acceptance.parent.name == 'toms-all-release' and acceptance.name == 'summary.json'
    journal = json.loads(acceptance.read_text(encoding='utf-8'))
    groups = ['terminal','link','vending'] if sealed['target'].startswith('1.20.1/') else ['terminal','configurator','link','vending']
    assert journal['complete'] is True and journal['groups'] == groups
    assert journal['retained_driver']['file'] == DRIVER
    driver_hash = sealed['archived_inputs'][DRIVER]
    assert journal['retained_driver']['sha256'] == driver_hash
    all_checks = []
    selected_observations = None
    selected_index = None
    for group in groups:
        folder = acceptance.parent / group
        action = reference(root, folder / 'checks.json')
        assert action in sealed['full_states'], 'Original workflow assertion journal is not sealed'
        data = json.loads((folder/'checks.json').read_text(encoding='utf-8'))
        assert data['driver_sha256'] == driver_hash
        validate_checks(group, data['checks'])
        all_checks.extend(dict(group=group, **check) for check in data['checks'])
        index_ref, raw = observations(root, folder, group, sealed['full_states'])
        index_rows = [r for r in journal['observation_indices'] if r['group'] == group]
        assert len(index_rows) == 1
        recorded = index_rows[0]
        assert type(recorded['count']) is int
        assert recorded == dict(group=group, file=group+'/observations/index.json', sha256=index_ref['sha256'], count=len(raw))
        if group == spec['group']:
            selected_index, selected_observations = index_ref, raw
    assert journal['checks'] == all_checks
    assert [v['group'] for v in journal['observation_indices']] == groups
    states = [v['result'] for v in selected_observations if v['kind'] == 'server-state']
    for cls in spec['required_menus']:
        assert any(any(m.get('type') == cls and m.get('valid') is True for m in state.get('menus',[])) for state in states), ('Missing actual native menu',cls)
    if spec['required_screen']:
        replies = [v['result'].get('client',{}) for v in selected_observations if v['kind'] == 'reply']
        assert any(w.get('nativeScreen') == spec['required_screen'] for client in replies
            for w in client.get('allWindows',[]) + client.get('windows',[])), 'Original customer Screen must actually be observed; class presence alone is insufficient'
    staged = json.loads(checked(root, sealed['staged']).read_text(encoding='utf-8'))['artifacts']
    publishers = []
    wanted_classes = list(dict.fromkeys(spec['native_classes'] + spec['required_menus']))
    covered = set()
    for artifact in staged:
        if artifact['family'] != 'upstream':
            continue
        if not artifact['filename'].startswith(('toms_storage-', 'toms_storage_', 'toms_trading_network')):
            continue
        path = Path(artifact['file'])
        path = path if path.is_absolute() else root/path
        raw = path.read_bytes()
        assert hashlib.sha256(raw).hexdigest() == artifact['sha256']
        sha512 = hashlib.sha512(raw).hexdigest()
        if 'sha512' in artifact:
            assert sha512 == artifact['sha512']
        locations = matches(index(path,sha512,sealed['target'].split('/')[1]),wanted_classes)
        if locations:
            publishers.append(dict(filename=artifact['filename'],sha256=artifact['sha256'],sha512=sha512,class_locations=locations))
            covered.update(name[:-6].replace('/','.') for loc in locations for name in loc['classes'])
    assert set(wanted_classes) <= covered, 'Every qualified native class must exist in the exact staged publisher'
    return dict(target=sealed['target'],case=case,disposition='functional-pass',native_classes=spec['native_classes'],
        original_native_classes=original_classes,corrected_historical_classification=spec['corrected'],
        classification_note=('The historical registry-item row pictured a vending owner. This original item-use workflow observes the actual InventoryConfiguratorMenu. The historical capture is retained.' if spec['corrected'] else None),
        group=spec['group'],native_menu_classes=spec['required_menus'],sealed_evidence=reference(root,sealed_path),
        acceptance=reference(root,acceptance),observation_index=selected_index,checks=len(CHECK_NAMES[spec['group']]),
        publishers=publishers,subjects={k:list(v) for k,v in expected.items()},scope=SCOPES[spec['group']],
        limits='One unchanged original feature group on this target and exact staged native publishers. Aliased checklist rows share these assertions; their counts are not extra test executions. No arbitrary topology, multiplayer channel visibility or other upstream patch acceptance.')


def verify(root, record, expected):
    path = checked(root, record['sealed_evidence'])
    assert derive(root,path,expected,record['case'],record['original_native_classes']) == record
    return True


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--id',required=True)
    parser.add_argument('--generation',default='build14')
    parser.add_argument('--run',action='append',required=True)
    args = parser.parse_args()
    assert re.fullmatch(r'build[0-9]+',args.generation)
    assert args.id and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.id)
    output = ROOT/'output/release-readiness'/('coverage-toms-'+args.id+'.json')
    assert not output.exists(), 'Preserve earlier reconciliation attempts'
    backlog = ROOT/'output/release-readiness/coverage-backlog.json'
    rows = json.loads(backlog.read_text())['records']
    from release_candidate_identity import current
    from focused_release_evidence_core_log_consumer import verified
    candidate=current(ROOT,verify_worktree=True);consumer=verified(ROOT,args.generation,candidate)
    records, targets = [], set()
    for run in args.run:
        assert run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in run)
        path = ROOT/'output/release-readiness'/('focused-evidence-'+run+'.json')
        sealed=json.loads(path.read_text());target=sealed['target']
        expected=consumer.expected(target,sealed,current_subjects(target))
        assert target not in targets, 'A target must have one reviewed observed workflow, not competing attempts'
        targets.add(target)
        for row in rows:
            if row['target'] == target and row['case'] in ALIASES:
                original = row.get('historical_native_classes',row['native_classes'])
                records.append(derive(ROOT,path,expected,row['case'],original))
    assert records and len({(r['target'],r['case']) for r in records}) == len(records)
    output.write_text(json.dumps(dict(status='reviewed-toms-native-workflows-reconciled',
        generated_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),backlog_input=reference(ROOT,backlog),
        candidate_identity=candidate,artifact_equivalence=consumer.report(),
        records=records,limits='Existing sealed observations and original feature assertions only. Does not execute new gameplay or close unrelated release gates.'),indent=2)+'\n')
    print(len(records),'explicit Tom case dispositions from',len(targets),'actual target runs')


if __name__ == '__main__':
    main()
