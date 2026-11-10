"""Recheck the original Ministrosity/Moa cargo observations, without input replay."""
from pathlib import Path
import hashlib
import json
import uuid
import nbtlib
from focused_release_evidence import checked, reference, verify as verify_sealed
from native_publisher_index import index, matches
from sensor_feature_evidence import typed

SCHEMA = 'pet-original-cargo-and-equipment-observations-v1'
PETS = {
    'ministrosity': dict(
        case='cataclysm-netherite-ministrosity', targets=('1.20.1/forge', '1.21.1/neoforge'), cells=15,
        menu='com.github.L_Ender.cataclysm.inventory.MinistrostiyMenu',
        screen='com.github.L_Ender.cataclysm.client.gui.MinistrosityInventoryScreen',
        labels=('initial', 'seeded', 'blacklist-refused', 'fifteen-hits', 'sorted', 'sort-in', 'moved',
                'diamond-fixture', 'last-cell', 'reopened', 'cargo-recovered'),
        checks=('ordinary-awakened-companion-opens-original-fifteen-cargo-cells',
                'original-blacklist-refuses-shulker-with-exact-cursor',
                'all-fifteen-visual-hits-preserve-one-fully-tagged-stone',
                'container-sort-merges-all-fifteen-with-exact-data',
                'inventory-sort-into-pet-preserves-exact-matching-total-and-blacklist',
                'companion-window-moves-before-last-cell-input',
                'moved-last-cell-receives-exact-diamond-stack',
                'same-owner-retains-exact-cargo-on-ordinary-reopening',
                'all-stored-items-recover-once-with-full-data',
                'native-range-validity-closes-original-companion-session'),
        scope='Ordinary awakening/use; fifteen full-data cargo hits, shulker refusal, sorting and declared seven-stone transfer fixture, moved last-cell input, owner persistence, exact recovery and native range invalidation.'),
    'moa': dict(
        case='bf-moa-clean', targets=('1.20.1/forge', '1.21.1/fabric', '1.21.1/neoforge'), cells=17,
        menu='com.aetherteam.protect_your_moa.inventory.menu.MoaInventoryMenu',
        screen='com.aetherteam.protect_your_moa.client.gui.screen.inventory.MoaInventoryScreen',
        labels=('initial', 'seeded', 'equipment-refused-0', 'equipment-refused-1', 'equipped',
                'cargo-fifteen', 'moved', 'last-withdrawn', 'restored', 'reopened', 'recovered'),
        checks=('ordinary-chest-attachment-is-consumed-and-opens-seventeen-original-cells',
                'native-equipment-cell-0-refuses-foreign-item-with-exact-cursor',
                'native-equipment-cell-1-refuses-foreign-item-with-exact-cursor',
                'native-saddle-and-armor-apply-to-original-owner-once',
                'all-fifteen-native-cargo-hits-retain-complete-typed-stacks',
                'moa-window-moves-before-native-last-cell-input',
                'moved-last-cargo-cell-withdraws-exactly-one-tagged-stone',
                'moved-cargo-can-return-to-original-cell',
                'same-owner-equipment-and-cargo-persist-through-native-reopening',
                'all-equipment-and-cargo-recover-once-with-original-full-data',
                'native-range-validity-closes-moa-session'),
        scope='Ordinary adult chest attachment and inventory use; native saddle/armor effects and refusal, fifteen full-data cargo hits, moved last-cell withdrawal/return, owner persistence, exact recovery and native range invalidation.'),
}
LIMITS = ('Exact staged publisher and named target only. The saved initial state witnesses the consumed chest where applicable; '
          'ordinary entry/awakening retains its original executed assertion and driver. No invented pre-entry snapshot, pet combat, '
          'Moa flight, all armor/blacklist variants, extra native execution or full-mod gameplay claim.')
PAIRS = {(target, cfg['case']) for cfg in PETS.values() for target in cfg['targets']}


def specification(target, case, original_classes):
    found = [(kind, cfg) for kind, cfg in PETS.items()
             if case == cfg['case'] and target in cfg['targets'] and original_classes == [cfg['screen']]]
    assert len(found) == 1, 'Unreviewed pet target/alias/native class'
    return found[0]


def item(text, target):
    value = nbtlib.parse_nbt(text)
    assert isinstance(value, nbtlib.Compound)
    if not value:
        return value, 0
    assert isinstance(value.get('id'), nbtlib.String)
    key = 'Count' if target.startswith('1.20.1/') else 'count'
    assert ('count' if key == 'Count' else 'Count') not in value
    if key not in value:
        assert key == 'count', 'Legacy native count must be present'
        count = 1  # Modern ItemStack codecs may omit the default count.
    else:
        assert isinstance(value[key], nbtlib.Byte if key == 'Count' else nbtlib.Int)
        count = int(value[key])
    assert count > 0
    return value, count


def stack(text, target, count=None):
    value, actual = item(text, target)
    payload = nbtlib.Compound(value)
    payload.pop('Count', None)
    payload.pop('count', None)
    return typed(payload), actual if count is None else count


def inventory(state, target):
    rows = state['inventory']
    assert isinstance(rows, list) and len(rows) >= 36
    assert all(type(v['index']) is int and v['index'] == i for i, v in enumerate(rows))
    return [stack(v['item'], target) for v in rows]


def cargo(state, target, count):
    rows = state['cells']
    assert isinstance(rows, list) and len(rows) == count
    assert all(type(v['index']) is int and v['index'] >= 0 for v in rows)
    assert len({v['index'] for v in rows}) == count
    return [stack(v['item'], target) for v in rows]


def cursors(state, target, held=None):
    rows = state['cursors']
    assert isinstance(rows, list) and rows and all(isinstance(v['menu'], str) and v['menu'] for v in rows)
    nonempty = [stack(v['item'], target) for v in rows if item(v['item'], target)[1]]
    assert not nonempty if held is None else nonempty and all(v == held for v in nonempty)


def fixture(text, target, identifier, count, marker, kind, damage=None):
    value, amount = item(text, target)
    assert value['id'] == nbtlib.String(identifier) and amount == count
    data = value['tag'] if target.startswith('1.20.1/') else value['components']['minecraft:custom_data']
    tag = 'release_pet_marker' if kind == 'ministrosity' else 'release_moa_marker'
    assert isinstance(data[tag], nbtlib.Int) and int(data[tag]) == marker
    assert isinstance(data['typed'], nbtlib.ByteArray)
    assert list(data['typed']) == ([1, 7] if kind == 'ministrosity' else [3, 9])
    if damage is not None:
        actual = data['Damage'] if target.startswith('1.20.1/') else value['components']['minecraft:damage']
        assert isinstance(actual, nbtlib.Int) and int(actual) == damage
    return stack(text, target)


def validate(target, kind, journal, states, removed, driver_hash):
    cfg = PETS[kind]
    specification(target, cfg['case'], [cfg['screen']])
    assert journal['complete'] is True and journal['cases'] == [cfg['case']]
    assert journal['driver_sha256'] == driver_hash
    assert [v['name'] for v in journal['checks']] == list(cfg['checks'])
    assert all(v.get('passed') is True for v in journal['checks'])
    assert isinstance(states, list) and len(states) == len(cfg['labels'])
    inv, cells = [], []
    for state in states:
        client = state['client']
        assert client['menu'] == cfg['menu'] and client['screen'] == cfg['screen']
        assert type(client['session']) is int and client['session'] > 0 and type(client['locked']) is bool
        bounds = client['bounds']
        assert set(bounds) == {'x', 'y', 'width', 'height'} and all(type(v) is int for v in bounds.values())
        assert bounds['width'] > 0 and bounds['height'] > 0
        assert str(uuid.UUID(state['owner'])) == state['owner'] == states[0]['owner']
        assert [v['index'] for v in state['cells']] == [v['index'] for v in states[0]['cells']]
        inv.append(inventory(state, target))
        cells.append(cargo(state, target, cfg['cells']))
    empty = stack('{}', target)
    assert cells[0] == [empty] * cfg['cells']
    assert all(v == empty for v in inv[0]), 'Ordinary cleared fixture baseline required'
    seed = list(inv[0])
    if kind == 'ministrosity':
        seed[9] = fixture(states[1]['inventory'][9]['item'], target, 'minecraft:stone', 15, 51, kind)
        seed[10] = fixture(states[1]['inventory'][10]['item'], target, 'minecraft:black_shulker_box', 1, 52, kind)
        assert inv[1] == seed and cells[1] == cells[0]
        refused = list(seed); refused[10] = empty
        assert inv[2] == refused and cells[2] == cells[0]; cursors(states[2], target, seed[10])
        stored = list(seed); stored[9] = empty
        one = stack(states[1]['inventory'][9]['item'], target, 1)
        assert cells[3] == [one] * 15 and inv[3] == stored
        for idx, amount in [(4, 15), (5, 22), (6, 22), (7, 22)]:
            assert [v for v in cells[idx] if v != empty] == [stack(states[1]['inventory'][9]['item'], target, amount)]
            assert inv[idx] == stored if idx != 7 else all(v == stored[i] for i, v in enumerate(inv[idx]) if i != 11)
        assert cells[6] == cells[5] and cells[7] == cells[6]
        diamond = fixture(states[7]['inventory'][11]['item'], target, 'minecraft:diamond', 3, 53, kind)
        expected_last = list(cells[7]); assert expected_last[-1] == empty; expected_last[-1] = diamond
        assert cells[8] == expected_last and inv[8] == stored
        assert cells[9] == cells[8] and inv[9] == inv[8]
        recovered = list(seed); recovered[9] = stack(states[1]['inventory'][9]['item'], target, 22); recovered[11] = diamond
        assert cells[10] == [empty] * 15 and inv[10] == recovered
        moved_before, moved_after = states[5]['client'], states[6]['client']
        occupied = {2}
    else:
        for slot, identifier, amount, marker, damage in [
            (9, 'minecraft:saddle', 1, 61, None), (10, 'aether_protect_your_moa:iron_moa_armor', 1, 62, 2),
            (11, 'minecraft:diamond', 3, 63, None), (12, 'minecraft:stone', 15, 64, None)]:
            seed[slot] = fixture(states[1]['inventory'][slot]['item'], target, identifier, amount, marker, kind, damage)
        assert inv[1] == seed and cells[1] == cells[0]
        refused = list(seed); refused[11] = empty
        for idx in [2, 3]:
            assert inv[idx] == refused and cells[idx] == cells[0]; cursors(states[idx], target, seed[11])
        equipped = [seed[9], seed[10]] + [empty] * 15
        equipment_inv = list(seed); equipment_inv[9] = equipment_inv[10] = empty
        assert cells[4] == equipped and inv[4] == equipment_inv
        full = equipped[:2] + [stack(states[1]['inventory'][12]['item'], target, 1)] * 15
        full_inv = list(equipment_inv); full_inv[12] = empty
        for idx in [5, 6, 8, 9]:
            assert cells[idx] == full and inv[idx] == full_inv
        withdrawn = list(full); withdrawn[-1] = empty
        withdrawn_inv = list(full_inv); withdrawn_inv[13] = full[-1]
        assert cells[7] == withdrawn and inv[7] == withdrawn_inv
        assert cells[10] == [empty] * 17 and inv[10] == seed
        for idx, state in enumerate(states):
            assert type(state['saddled']) is bool
            active = 4 <= idx <= 9
            assert state['saddled'] is active
            assert stack(state['armor'], target) == (seed[10] if active else empty)
        moved_before, moved_after = states[5]['client'], states[6]['client']
        occupied = {2, 3}
    for idx, state in enumerate(states):
        if idx not in occupied:
            cursors(state, target)
    old, new = moved_before['bounds'], moved_after['bounds']
    assert (old['width'], old['height']) == (new['width'], new['height'])
    assert (old['x'], old['y']) != (new['x'], new['y']) and moved_after['locked'] is False
    assert all(v['client']['session'] == states[0]['client']['session'] for v in states[:9])
    assert states[9]['client']['session'] == states[10]['client']['session']
    assert isinstance(removed['menus'], list)
    assert not any(v.get('session') == states[9]['client']['session'] or v.get('type') == cfg['menu'] for v in removed['menus'])
    return dict(assertions=len(cfg['checks']), states=len(states), cargo_cells=15,
                equipment_cells=0 if kind == 'ministrosity' else 2, native_variants=1)


def derive(root, sealed_path, expected, case, original_classes):
    root = Path(root).resolve(); sealed = json.loads(Path(sealed_path).read_text()); target = sealed['target']
    kind, cfg = specification(target, case, original_classes); verify_sealed(root, sealed, expected)
    summary = json.loads(checked(root, sealed['summary']).read_text()); row = summary['results'][0]; result = row['results']
    assert result.get(kind + '_features_checked') is True
    assert all(type(result[k]) is int and result[k] == v for k, v in [('cases', 1), ('failed', 0), ('checks', len(cfg['checks']))])
    profile = (root / row['profile']).resolve(); acceptance = checked(root, sealed['acceptance'])
    assert acceptance == profile / (kind + '-release') / 'summary.json'
    expected_files = {f'{n:03}-{label}.json' for n, label in enumerate(cfg['labels'], 1)}
    assert {v.name for v in acceptance.parent.glob('[0-9][0-9][0-9]-*.json')} == expected_files
    refs, states = [], []
    for n, label in enumerate(cfg['labels'], 1):
        path = acceptance.parent / f'{n:03}-{label}.json'; ref = reference(root, path)
        assert ref in sealed['full_states']; refs.append(ref); states.append(json.loads(path.read_text()))
    removed = reference(root, profile / 'regression-state.json')
    driver = 'functional-tests/release-specialized/' + kind + '_features.py'
    private='build/release-readiness/moa_specific_entry_features_622.py'
    if private in sealed['archived_inputs']:
        assert kind=='moa'and target in {'1.20.1/forge','1.21.1/fabric','1.21.1/neoforge'}
        variant='legacy'if target=='1.20.1/forge'else'portable'
        for name,digest in {'legacy': {'build/release-readiness/moa_specific_entry_features_622.py': '2e06990f92a2855848682f94923d9bf9361422e45cfb73272ebc51accf8f8347', 'build/release-readiness/named_air_fixture_566.py': '0f799ff55e3a2a0da275ba5304800798dcee755ac37077a4e4704607f11c0430', 'build/release-readiness/moa_entry_facts_features_625.py': '3d145633aed0b5ce3df42f963dc99b8ee7e90f10453488f2520271ace2b6599e', 'build/release-readiness/moa_entry_facts_observer_625.py': '629c047cdfc03affee56c068737081dee7b4b11e4268af9b6405cb5371a71d2f', 'build/release-readiness/moa_modifier_features_626.py': 'e4d573624f1277056a642152507db9111affa325dbc45b9a4824b89464548d85', 'build/release-readiness/moa_modifier_observer_626.py': '507736b576d5ac747272a2f4507bc6fca45b526b8526e5e78ae3a99ebc6eaf40', 'build/release-readiness/prepare_moa_modifier_626.py': '9307e2a8c2e3221e2ee0e1c4d8b190dcdf0b4c349be21f559fd59cdc3646ccd4', 'build/release-readiness/run_moa_modifier_626.py': 'c4bce2600c4193ae505763f937da64a624415fc533bb0965af310eaa0b18bdf7'}, 'portable': {'build/release-readiness/moa_specific_entry_features_622.py': '2e06990f92a2855848682f94923d9bf9361422e45cfb73272ebc51accf8f8347', 'build/release-readiness/named_air_fixture_566.py': '0f799ff55e3a2a0da275ba5304800798dcee755ac37077a4e4704607f11c0430', 'build/release-readiness/moa_native_modifier_features_627.py': '0f69727b4884289d1ee7a92b8f4ab14cf0cc24d610fa4a0e70f601fdcc41f79e', 'build/release-readiness/moa_native_modifier_observer_627.py': 'e96aea6be2fca19614ca883ee4633ea9dd91fe51932350e886c4d5b83d7a6712', 'build/release-readiness/prepare_moa_native_modifier_627.py': 'fb9e81cb22dedef2d8aea1371d3c142e0268db77fcba14a6fb906ad5fa32aaa0', 'build/release-readiness/run_moa_native_modifier_627.py': 'b606d0870ea543900753b4b96531293c5a52686565d5028e79c050df21f7ef53'}}[variant].items():
            assert sealed['archived_inputs'].get(name)==digest,'Unreviewed native Moa modifier input'
        driver=private
    validation = validate(target, kind, json.loads(acceptance.read_text()), states,
                          json.loads(checked(root, removed).read_text()), sealed['archived_inputs'][driver])
    publishers = []; classes = (cfg['menu'], cfg['screen'])
    for artifact in json.loads(checked(root, sealed['staged']).read_text())['artifacts']:
        if artifact['family'] != 'upstream': continue
        path = Path(artifact['file']); path = path if path.is_absolute() else root / path; raw = path.read_bytes()
        assert hashlib.sha256(raw).hexdigest() == artifact['sha256']; digest = hashlib.sha512(raw).hexdigest()
        if 'sha512' in artifact: assert artifact['sha512'] == digest
        locations = matches(index(path, digest, target.split('/')[1]), classes)
        if not locations: continue
        assert {v[:-6].replace('/', '.') for loc in locations for v in loc['classes']} == set(classes)
        publishers.append(dict(filename=artifact['filename'], sha256=artifact['sha256'], sha512=digest, class_locations=locations))
    assert len(publishers) == 1, 'Exactly one original native pet publisher required'
    return dict(schema=SCHEMA, target=target, case=case, kind=kind, disposition='functional-pass', original_native_classes=original_classes,
                native_classes=list(classes), subjects={k:list(v) for k,v in expected.items()}, sealed_evidence=reference(root, sealed_path),
                acceptance=reference(root, acceptance), native_states=refs, range_invalidation=removed, validation=validation,
                publishers=publishers, scope=cfg['scope'], limits=LIMITS)


def verify(root, record, expected):
    assert record['schema'] == SCHEMA and record['disposition'] == 'functional-pass'
    checked(root, record['range_invalidation'])
    assert derive(root, checked(root, record['sealed_evidence']), expected, record['case'], record['original_native_classes']) == record
    return True
