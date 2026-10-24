"""Audit the current release's saved physical Computer Use playthrough (read-only)."""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path


def read(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))


def slots(snapshot, inventory):
    result = {}
    for entry in snapshot[inventory]:
        index, stack = entry.split(':', 1)
        item, count = stack.rsplit('=', 1)
        result[int(index)] = (item, int(count))
    return result


def cursor(snapshot):
    count, item = snapshot['cursor'].split(' ', 1)
    return item, int(count)


def contents(snapshot):
    return {k: v for k, v in snapshot.items() if k != 'time'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('evidence', type=Path)
    args = parser.parse_args()
    folder = args.evidence
    snapshots = {p.stem: read(p) for p in sorted(folder.glob('[0-9][0-9]-*.json'))}
    assert len(snapshots) == 17, 'Expected every preserved checkpoint'
    baseline = snapshots['00-baseline']
    for name, snapshot in snapshots.items():
        total = Counter()
        for inventory in ('player', 'container0', 'container1', 'container2'):
            for item, quantity in slots(snapshot, inventory).values():
                assert 0 < quantity <= 64, (name, inventory, quantity)
                total[item] += quantity
        item, quantity = cursor(snapshot)
        assert 0 <= quantity <= 64, name
        total[item] += quantity
        assert total['minecraft:stone'] == 152, (name, total)
        assert total['minecraft:dirt'] == 42, (name, total)
        assert total['minecraft:iron_ingot'] == 3, (name, total)
        assert total['minecraft:oak_planks'] == 12, (name, total)
    assert cursor(snapshots['01-chest-drag']) == ('minecraft:stone', 36)
    assert cursor(snapshots['02-native-cross-window']) == ('minecraft:stone', 36)
    assert 1 not in slots(snapshots['02-native-cross-window'], 'container0')
    assert 0 not in slots(snapshots['02-native-cross-window'], 'container1')
    assert cursor(snapshots['03-api-cross-window']) == ('minecraft:stone', 36)
    assert 1 not in slots(snapshots['03-api-cross-window'], 'container1')
    assert 0 not in slots(snapshots['03-api-cross-window'], 'container2')
    craft = snapshots['09-crafting-exhausted']
    assert cursor(craft)[1] == 0
    assert slots(craft, 'player')[9] == ('minecraft:stone', 21)
    assert slots(craft, 'player')[13] == ('minecraft:crafting_table', 1)
    assert contents(craft) == contents(snapshots['10-virtual-wheel-excluded'])
    assert cursor(snapshots['11-right-split']) == ('minecraft:stone', 18)
    assert cursor(snapshots['12-right-single-api']) == ('minecraft:stone', 17)
    assert slots(snapshots['12-right-single-api'], 'container2')[0] == ('minecraft:stone', 1)
    assert cursor(snapshots['13-double-click-limit']) == ('minecraft:stone', 64)
    assert contents(snapshots['14-before-overlap-wheel']) == contents(snapshots['15-overlap-blocked'])
    assert cursor(snapshots['16-final'])[1] == 0
    # An ordinary click may pick up the virtual fixture's real backing stack.
    # Virtual means excluded from Mouse Tweaks gestures, not globally immutable.
    assert slots(snapshots['16-final'], 'container2')[2] == slots(baseline, 'container2')[2]
    report = read(folder / 'results.json')
    assert report['scope'] == 'mouse-tweaks-computer-use-fixture' and report['failures'] == 0
    assert 'mousetweaks' in report['loadedMods']
    assert not {'salts_inventory_compat', 'salts_inventory_controlify'} & report['loadedMods'].keys()
    core = next(a for a in read(folder / 'staged-artifacts.json')['artifacts'] if a['family'] == 'core')
    assert hashlib.sha256(Path(core['file']).read_bytes()).hexdigest() == core['sha256']
    result = dict(status='PASS', checkpoints=len(snapshots), core_sha256=core['sha256'],
                  input='Windows Computer Use through @oai/sky',
                  checks=['item conservation and stack limits', 'native collection',
                          'native-to-native collection', 'native-to-API collection',
                          'crafting exhaustion with occupied inventory', 'virtual-slot wheel exclusion',
                          'right-click split and API single placement', 'double-click stack limit',
                          'covered-slot wheel exclusion', 'empty final cursor'],
                  limits=['One current 26.3 NeoForge playthrough; other targets use separate automated profiles.',
                          'Checkpoint labels 05-08 retain exploratory input and are not successful virtual/crafting checks.',
                          'Sky drag samples did not visit every intervening slot.'])
    (folder / 'computer-use-audit.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
