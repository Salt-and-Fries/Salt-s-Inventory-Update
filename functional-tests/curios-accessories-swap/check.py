"""Focused real-input equipment switching checks; no complete gameplay suite."""
from pathlib import Path
import argparse, json, re, sys

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'functional-tests/all-target-rollout'))
from evidence import send


def run(profile):
    checks = []
    report = profile / 'equipment-swap-checks.json'
    curios = '.curios.CuriosCharacterButton'
    accessories = '.accessories.AccessoriesClient$Button'
    cloud = '.aether.AetherEquipmentClient$Cloud'
    kinds = {'curios': '.CuriosDesktopMenu', 'accessories': '.AccessoriesDesktopMenu', 'aether': '.AetherEquipmentMenu'}

    def act(kind, **args):
        return send(profile, kind, **args)

    def check(label, passed):
        checks.append({'check': label, 'passed': bool(passed)})
        report.write_text(json.dumps({'scope': 'Focused equipment launcher geometry, exclusive switching, tagged carried stack, Curios native equip/recovery, Aether switching and parent cleanup. Not a complete mod/gameplay suite; no item-effect or Accessories item-specific equip claim.', 'checks': checks}, indent=2) + '\n')
        assert passed, label
        print('PASS', label, flush=True)

    def snapshot(item='minecraft:diamond'):
        act('regression-state', item=item, snbt='{siu_regression_marker:42}', ticks=3)
        return json.loads((profile / 'regression-state.json').read_text())

    def character():
        act('regression-character-info', ticks=2)
        return json.loads((profile / 'regression-character.json').read_text())

    def click(button, ticks=14):
        # The observer clicks the center of the published hit region through
        # the normal desktop mouse callbacks, never the provider/open method.
        act('regression-attachment-click', attachment=button, ticks=ticks)

    def one(kind):
        data = snapshot()
        menus = [m for m in data['menus'] if m['type'].endswith(tuple(kinds.values()))]
        check('exactly one live equipment source: ' + kind, len(menus) == 1 and menus[0]['type'].endswith(kinds[kind]))
        return menus[0]

    def regions():
        rows = character()['attachments']
        def bounds(name):
            row = next(r for r in rows if r['type'].endswith(name))
            return tuple(int(v) for v in re.search(r'x=(-?\d+), y=(-?\d+), width=(\d+), height=(\d+)', row['regions']).groups())
        return bounds(curios), bounds(accessories)

    def held():
        cursor = snapshot()['cursor']
        check('switch preserves the tagged seven-item cursor', cursor['item'] == 'minecraft:diamond' and cursor['count'] == 7 and cursor['signature'] == signature)

    act('fixture', block='minecraft:stone', ticks=8)
    act('command', command='gamemode survival @a', ticks=4)
    act('regression-character', ticks=10)
    original = character()
    left, right = regions()
    check('normal portrait buttons occupy separate left and right corners', left[0] == 27 and right[0] == 64 and left[0] + left[2] <= right[0])
    click(curios)
    source = one('curios')

    item = 'cataclysm:sticky_gloves'
    data = snapshot(item)
    allowed = [s['index'] for s in data['menus'][0]['slots'] if s['active'] and s['mayPlace'] and not s['player'] and not s['count']]
    check('native Curios rules accept the fixture gloves', bool(allowed))
    slot = allowed[0]
    act('regression-seed-player', items=[{'slot': 9, 'item': item, 'count': 1, 'snbt': '{siu_regression_marker:42}'}, {'slot': 12, 'item': 'minecraft:diamond', 'count': 7, 'snbt': '{siu_regression_marker:42}'}], ticks=4)
    gloves_signature = next(s['signature'] for s in snapshot(item)['inventory'] if s['index'] == 9)
    act('regression-player-click', slot=9, ticks=4)
    act('regression-slot-click', session=source['session'], slot=slot, ticks=4)
    check('gloves equip through the actual Curios slot hit area', snapshot(item)['menus'][0]['slots'][slot]['count'] == 1)
    signature = next(s['signature'] for s in snapshot()['inventory'] if s['index'] == 12)
    act('regression-player-click', slot=12, ticks=4)

    for _ in range(3):
        click(accessories); one('accessories'); held()
        click(curios); source = one('curios'); held()
        gear = snapshot(item)['menus'][0]['slots'][slot]
        check('equipped gloves persist across switching with unchanged metadata', gear['item'] == item and gear['count'] == 1 and gear['signature'] == gloves_signature)
    check('switching retains the same Character parent', character()['instance'] == original['instance'])
    act('regression-player-click', slot=14, ticks=4)
    act('regression-slot-click', session=source['session'], slot=slot, ticks=4)
    act('regression-player-click', slot=10, ticks=4)
    gear = next(s for s in snapshot(item)['inventory'] if s['index'] == 10)
    check('previously equipped item returns to inventory once', gear['item'] == item and gear['count'] == 1 and gear['signature'] == gloves_signature)

    for button, kind in [(curios, 'curios'), (accessories, 'accessories')]:
        if kind == 'accessories': click(button); one(kind)
        click(button)
        check('same ' + kind + ' button closes its panel', not snapshot()['menus'])
        click(button); one(kind)
        act('capture', capture='siu-' + kind + '-exclusive', ticks=12)

    click(cloud); one('aether')
    left, right = regions()
    check('reformatted Aether portrait also keeps buttons in separate corners', left[0] == 9 and right[0] == 45 and left[0] + left[2] <= right[0])
    click(accessories); one('accessories')
    click(cloud); one('aether')
    click(curios); one('curios')
    act('regression-player-click', slot=14, ticks=4); held()
    act('regression-character-control', control='CLOSE', ticks=14)
    check('closing Character removes its equipment source', not snapshot()['menus'])
    held()
    act('regression-player-click', slot=15, ticks=4)
    data = snapshot()
    check('parent close preserves exactly one copy of the carried stack', data['cursor']['count'] == 0 and sum(s['count'] for s in data['inventory'] if s['item'] == 'minecraft:diamond') == 7)
    print('Focused checks passed:', len(checks), flush=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('profile', type=Path)
    run(parser.parse_args().profile)
