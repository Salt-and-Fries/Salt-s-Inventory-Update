"""Focused rendering checks for the shared right upgrade-tray surround."""
from pathlib import Path
import hashlib
import importlib.util
import json
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'output/tech-reborn-tray-fix'
RUN = ROOT / 'build/remaining-ui-audit/gameplay/1.20.1-fabric-integrated-tech-reborn-tray-fix-01'
spec = importlib.util.spec_from_file_location('native_driver', ROOT / 'functional-tests/tech-reborn-design/capture.py')
d = importlib.util.module_from_spec(spec)
spec.loader.exec_module(d)
d.RUN = RUN


def region(image, state, x, y, width, height):
    scale = image.width / state['guiWidth']
    return image.crop(tuple(round(v * scale) for v in (x, y, x + width, y + height)))


def main():
    previous = json.loads((ROOT / 'output/tech-reborn-implementation/captures.json').read_text())
    cases = [r for r in previous if r.get('variant') in ('idle', 'formed-idle')
             and r['state']['allWindows'][-1].get('trUpgrades')]
    assert len(cases) == 28
    checks = []
    OUT.mkdir(exist_ok=True, parents=True)
    for case in cases:
        d.open_block(case['block'])
        d.call('move', x=2, y=2, ticks=2)
        state = d.call('capture', capture=case['id'], ticks=5)
        window = state['allWindows'][-1]
        assert window['sameModel'] and window['trUpgrades']
        bounds = window['bounds']
        x, y = bounds['x'] + bounds['width'] - 4, bounds['y'] + 24
        path = RUN / 'screenshots' / (case['id'] + '.png')
        image = Image.open(path).convert('RGB')
        # These gutters were sky-colored. They must now be the frame's exact
        # opaque center color, including the corners adjacent to native cells.
        for rect in [(x + 22, y + 3, 3, 74), (x + 4, y + 77, 20, 2)]:
            assert set(region(image, state, *rect).getdata()) == {(198, 198, 198)}, (case['id'], rect)
        assert set(region(image, state, x + 24, y + 78, 1, 1).getdata()) == {(85, 85, 85)}
        mapped = next(r for r in window['composition']['regions'] if r['id'] == 'native-upgrade-cells')
        old_window = case['state']['allWindows'][-1]
        old_mapped = next(r for r in old_window['composition']['regions'] if r['id'] == 'native-upgrade-cells')
        before = region(Image.open(ROOT / case['capture']).convert('RGB'), case['state'],
                        old_window['bounds']['x'] + old_mapped['x'], old_window['bounds']['y'] + old_mapped['y'], 20, 74)
        after = region(image, state, bounds['x'] + mapped['x'], bounds['y'] + mapped['y'], 20, 74)
        assert before.size == after.size and before.tobytes() == after.tobytes(), (case['id'], 'native cell artwork changed')
        checks.append(dict(id=case['id'], solid_right_and_bottom_gutters=True, native_cells_identical=True,
                           capture=str(path.relative_to(ROOT)), sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
                           state=state))
        (OUT / 'captures.json').write_text(json.dumps(checks, indent=2))
        print('PASS', case['id'], flush=True)
    d.call('close')
    report = dict(status='passed', screens=len(checks), opaque_gutters=True, native_cell_art_unchanged=True,
                  runtime=str(RUN.relative_to(ROOT)), scope='Shared upgrade-tray rendering only; no complete gameplay suite.')
    (OUT / 'checks.json').write_text(json.dumps(report, indent=2))
    print(json.dumps(report), flush=True)


if __name__ == '__main__':
    main()
