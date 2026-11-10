"""Correct the five reviewed native-artwork defects without replacing publisher art."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
WORK = ROOT / 'build/window-artifact-fixes'
changed = []


def write(path, value):
    old = path.read_text(encoding='utf-8')
    if old == value:
        return
    backup = WORK / 'before' / path.relative_to(ROOT)
    backup.parent.mkdir(parents=True, exist_ok=True)
    if not backup.exists():
        backup.write_bytes(path.read_bytes())
    path.write_text(value, encoding='utf-8')
    changed.append(str(path.relative_to(ROOT)))


versions = [('1.20.1', 'fabric'), ('1.20.1', 'forge'), ('1.21.1', 'neoforge'),
            ('26.1.2', 'neoforge'), ('26.2', 'neoforge'), ('26.3', 'neoforge')]
for mc, loader in versions:
    resources = ROOT / f'compatibility/ae2/versions/{mc}/{loader}/src/main/resources'
    for folder in ['ae2-native-patches', 'ae2-native-patches-runtime']:
        root = resources / folder
        if not root.exists():
            continue
        # Keep AE2's original three-column, five-row crafting report. The
        # inherited renderer also uses this policy for hover and scroll math.
        path = root / 'appeng/client/gui/me/crafting/CraftConfirmTableRenderer.json'
        data = json.loads(path.read_text())
        method = next(m for m in data['methods'] if m['name'] == 'readableRows')
        method.update(instructions=[{'type': 0, 'op': 3}, {'type': 0, 'op': 172}],
                      handlers=[], maxStack=1, maxLocals=1)
        write(path, json.dumps(data, separators=(',', ':')))
        path = root / 'appeng/client/gui/me/crafting/CraftConfirmScreen.json'
        data = json.loads(path.read_text())
        method = next(m for m in data['methods'] if m['name'] == 'init')
        # Its original table constructor supplies the publisher's own geometry.
        # Retain the matching mapped superclass-init call from this contract.
        native_init = next(n for n in method['instructions']
                           if n.get('op') == 183 and n.get('owner') == 'appeng/client/gui/AEBaseScreen')
        method.update(instructions=[{'type': 2, 'op': 25, 'var': 0}, native_init,
                                    {'type': 0, 'op': 177}],
                      handlers=[], maxStack=1, maxLocals=1)
        write(path, json.dumps(data, separators=(',', ':')))
        for name in ['me/common/MEStorageScreen', 'me/patternaccess/PatternAccessTermScreen']:
            path = root / f'appeng/client/gui/{name}.json'
            data = json.loads(path.read_text())
            matches = 0
            for method in [*data['methods'], *data['changes']]:
                arrays = [method['instructions']] if 'instructions' in method else [e.get('insert', []) for e in method['edits']]
                for ops in arrays:
                    for index in range(len(ops)-1, -1, -1):
                        node = ops[index]
                        if node.get('name') != 'catalogBottom':
                            continue
                        matches += 1
                        if node['desc'].endswith('IIII)V'):
                            continue
                        assert node['desc'].endswith('III)V'), node
                        node['desc'] = node['desc'][:-2] + 'I)V'
                        ops[index:index] = [
                            {'type': 2, 'op': 25, 'var': 0},
                            {'type': 5, 'op': 182, 'owner': 'appeng/client/gui/AEBaseScreen',
                             'name': 'desktopInventoryCut', 'desc': '()I', 'interface': False}
                        ]
                        method['maxStack'] += 1
            assert matches == 1, (path, matches)
            write(path, json.dumps(data, separators=(',', ':')))

# The installed drive.json already places its slot and empty-cell icon together.
path = ROOT / 'compatibility/ae2/src/main/resources/assets/ae2/screens/desktop/drive.json'
data = json.loads(path.read_text())
data.pop('slots', None)
write(path, json.dumps(data, indent=2) + '\n')

for branch in ['1.20.1/common', '1.21.1/neoforge', '26.1.2/neoforge', '26.2/neoforge', '26.3/neoforge']:
    root = ROOT / f'compatibility/ae2/versions/{branch}/src/main/java/com/salts_inventory_update/compat/ae2/nativeview'
    path = root / 'style/DesktopFrameEdges.java'
    text = path.read_text()
    text = text.replace('int x, int y, int width) {\n        header.copy()',
                        'int x, int y, int width, int inventoryCut) {\n        header.copy()')
    graphics = 'graphics.unwrap()' if 'graphics.unwrap()' in text else 'graphics'
    old = f'''        header.copy().src(header.getSrcX(), header.getSrcY() + 4, width, 4)
                .dest(x, y - 4).blit({graphics});'''
    new = f'''        // Only the blank area after the final catalog row may be repainted.
        // AE2 15 leaves six footer pixels; newer art leaves eight. A fixed
        // four-row splice overwrote the last two slot-border pixels on AE2 15.
        int joinHeight = Math.max(0, Math.min(4, bottom.getSrcHeight() - inventoryCut - 4));
        if (joinHeight > 0)
            header.copy().src(header.getSrcX(), header.getSrcY() + 4, width, joinHeight)
                    .dest(x, y - joinHeight).blit({graphics});'''
    assert old in text or new in text, path
    text = text.replace(old, new)
    write(path, text)

    path = root / 'AEWindow.java'
    text = path.read_text()
    start = text.index('        boolean intersects = false, fitsAbove = true;')
    end = text.index('        for (var slot : slots) {', text.index('top -= 2;', start))
    text = text[:start] + '''        // Preserve native slot/art positions. Find the nearest clear title
        // band above them instead of moving the Drive's slots away from its art.
        for (int candidate = top; candidate >= 0; candidate--) {
            boolean clear = true;
            for (var slot : slots) {
                if (slot.x - 1 < right && slot.x + 17 > right - 100
                        && slot.y - 1 < candidate + 11 && slot.y + 17 > candidate) {
                    clear = false;
                    break;
                }
            }
            if (clear) { top = candidate; break; }
        }
''' + text[end:]
    write(path, text)

for mc in ['1.20.1', '1.21.1']:
    path = ROOT / f'compatibility/versions/{mc}/fabric/src/main/java/com/salts_inventory_update/compat/create/CreateNativeFrame.java'
    text = path.read_text()
    assert 'AMMO("createbigcannons","backgrounds1",0,166,' in text
    write(path, text.replace('AMMO("createbigcannons","backgrounds1",0,166,',
                             'AMMO("createbigcannons","backgrounds1",0,168,'))

for mc in ['1.20.1', '1.21.1', '1.21.11', '26.1.2', '26.2', '26.3']:
    path = ROOT / f'compatibility/versions/{mc}/fabric/src/main/java/com/salts_inventory_update/compat/nativewindows/NativeMachineLayouts.java'
    text = path.read_text()
    assert '.exterior("lava-reservoir",box(57,0,66,18),55,-16)' in text
    write(path, text.replace('.exterior("lava-reservoir",box(57,0,66,18),55,-16)',
                             '.exterior("lava-reservoir",box(53,0,70,16),53,-16)'))

(WORK / 'layout-source-changes.json').write_text(json.dumps(changed, indent=2))
print('Updated', len(changed), 'owned layout/contracts files; native publisher binaries untouched.')
