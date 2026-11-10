"""Pair-only smoke check for the older publisher releases, without gear addons."""
from pathlib import Path
import argparse, json, re, sys

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'functional-tests/all-target-rollout'))
from evidence import send


def run(profile):
    checks = []
    def act(kind, **args): return send(profile, kind, **args)
    def snapshot():
        act('regression-state', item='minecraft:diamond', snbt='{siu_regression_marker:42}', ticks=3)
        return json.loads((profile / 'regression-state.json').read_text())
    def check(label, passed):
        checks.append({'check': label, 'passed': bool(passed)})
        (profile / 'equipment-pair-checks.json').write_text(json.dumps({'scope': 'Pair-only launcher geometry, real clicks, exclusive source lifecycle, tagged shared cursor and parent-close cleanup. No addon gear/effect test.', 'checks': checks}, indent=2) + '\n')
        assert passed, label
        print('PASS', label, flush=True)
    act('command', command='gamemode survival @a', ticks=5)
    act('regression-character', ticks=10)
    info=json.loads((profile/'regression-character.json').read_text())
    bounds={r['type'].split('.')[-1]:tuple(map(int,re.search(r'x=(-?\d+), y=(-?\d+), width=(\d+), height=(\d+)',r['regions']).groups())) for r in info['attachments']}
    check('Curios and Accessories buttons occupy opposite portrait corners', bounds['CuriosCharacterButton'][0]==27 and bounds['AccessoriesClient$Button'][0]==64)
    act('regression-seed-player', items=[{'slot':12,'item':'minecraft:diamond','count':7,'snbt':'{siu_regression_marker:42}'}], ticks=4)
    signature=next(s['signature'] for s in snapshot()['inventory'] if s['index']==12)
    act('regression-player-click', slot=12, ticks=4)
    for _ in range(3):
        for attachment, model in [('.curios.CuriosCharacterButton','.CuriosDesktopMenu'),('.accessories.AccessoriesClient$Button','.AccessoriesDesktopMenu')]:
            act('regression-attachment-click', attachment=attachment, ticks=14)
            state=snapshot()
            check('one live equipment source after switching to '+model, len(state['menus'])==1 and state['menus'][0]['type'].endswith(model))
            cursor=state['cursor']
            check('switch preserves the tagged carried stack', cursor['item']=='minecraft:diamond' and cursor['count']==7 and cursor['signature']==signature)
    act('regression-attachment-click', attachment='.accessories.AccessoriesClient$Button', ticks=14)
    check('same button closes the selected Accessories panel', not snapshot()['menus'])
    act('regression-attachment-click', attachment='.curios.CuriosCharacterButton', ticks=14)
    act('regression-character-control', control='CLOSE', ticks=14)
    state=snapshot()
    check('Character close clears equipment and keeps the carried stack', not state['menus'] and state['cursor']['count']==7 and state['cursor']['signature']==signature)
    act('regression-player-click', slot=15, ticks=4)
    state=snapshot()
    check('seven carried items return to inventory exactly once', not state['cursor']['count'] and sum(s['count'] for s in state['inventory'] if s['item']=='minecraft:diamond')==7)
    print('Focused pair checks passed:',len(checks),flush=True)


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('profile',type=Path);run(p.parse_args().profile)
