"""Focused in-game Cataclysm fusion checks; not a complete gameplay suite."""
from pathlib import Path
import json, os, re, sys
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'functional-tests/project-everything-ten'))
from send import send

RUN = ROOT / 'build/remaining-ui-audit/gameplay' / os.environ.get('FUSION_RUN', '1.20.1-forge-integrated-fusion-recessed03')
OUT = ROOT / 'build/weapon-fusion-recessed' / os.environ.get('FUSION_EVIDENCE', 'evidence-inset')
OUT.mkdir(parents=True, exist_ok=True)
checks = []

def act(kind='capture', **kw):
    return send(RUN, kind, ticks=kw.pop('ticks', 5), **kw)['client']

def cmd(text): return act('command', command=text)

def click(x, y, button=0):
    act('click', x=x, y=y, button=button, ticks=1)
    return act('release', x=x, y=y, button=button)

def table(state=None):
    return next(w for w in (state or act())['allWindows'] if w.get('menuId') == 'cataclysm:weapon_fusion')

def workspace(w, state=None):
    return next(v for v in (state or act())['workspaceWindows'] if v['bounds'] == w['bounds'])

def check(name, passed, details=None):
    checks.append(dict(name=name, passed=bool(passed), details=details))
    (OUT / 'checks.json').write_text(json.dumps(checks, indent=2))
    print(('PASS ' if passed else 'FAIL ') + name, flush=True)
    assert passed, (name, details)

def capture(name):
    s = act(capture=name)
    (OUT / (name + '.json')).write_text(json.dumps(s, indent=2))
    return s

def inventory(index):
    w = next(w for w in act()['workspaceWindows'] if w['kind'] == 'INVENTORY')
    p = next(p for p in w['inventoryCells'] if p['inventorySlot'] == index)
    return click(p['x'], p['y'])

def cell(index):
    c = table()['fusionContent']
    return click(c['x'] + (35, 84, 142)[index], c['y'] + 55)

def snapshot(name):
    act('snapshot-server', ticks=1)
    s = json.loads((RUN / 'server-state.json').read_text())
    (OUT / (name + '.json')).write_text(json.dumps(s, indent=2))
    return s

def menu(s): return next(m for m in s['menus'] if m['class'].endswith('.WeaponfusionMenu'))
def item(nbt): return re.search(r'id:"([^"]+)"', nbt).group(1)
def amount(nbt): return int(re.search(r'Count:(\d+)b', nbt).group(1))
def total(s, name): return sum(amount(v['item']) for v in s['inventory'] if item(v['item']) == name)

def drag_window(w, x, y):
    s = act(); ww = workspace(w, s)
    if ww['locked'] == 'true': click(**ww['controls']['LOCK'])
    b = w['bounds']; sx, sy = b['x'] + 12, b['y'] + 7
    dx, dy = x - b['x'], y - b['y']
    act('click', x=sx, y=sy, button=0, ticks=1)
    act('drag', x=sx + dx, y=sy + dy, dx=dx, dy=dy, button=0, ticks=1)
    act('release', x=sx + dx, y=sy + dy, button=0)

def pixels(name, state, scale):
    image = Image.open(RUN / 'screenshots' / (name + '.png')).convert('RGB')
    atlas = Image.open(ROOT / 'build/project-everything-small-design/assets/346/assets/cataclysm/textures/gui/fusion.png').convert('RGBA')
    w = table(state); c = w['fusionContent']; b = w['bounds']
    bad = []
    for y in range(5, 66):
        for x in range(5, 171):
            r, g, blue, alpha = atlas.getpixel((x, y))
            if alpha == 255:
                actual = image.getpixel((int((c['x'] + x) * scale), int((c['y'] + y) * scale)))
                if actual != (r, g, blue): bad.append((x, y, actual, (r, g, blue)))
    check('visible native empty panel matches original artwork at scale ' + str(scale), not bad, {'mismatches': len(bad), 'examples': bad[:6]})
    sample = lambda x, y: image.getpixel((int(x * scale), int(y * scale)))
    check('shell and model-preview rim share gray palette at scale ' + str(scale),
          sample(b['x'] + 6, b['y'] + 18) == (70, 73, 79) and
          sample(c['x'] + 20, c['y'] + 4) == (19, 20, 22) and
          sample(c['x'] + 20, c['y'] + 66) == (127, 130, 136))
    check('original frame is covered on all four sides at scale ' + str(scale),
          all(sample(c['x'] + x, c['y'] + y) == (70, 73, 79)
              for x, y in ((1, 30), (174, 30), (100, 1), (100, 70))))

s = capture('fusion-recessed-empty')
w = table(s)
check('dedicated recessed template selected with only three machine slots',
      w.get('template', '').endswith('WeaponFusionWindows$Template') and len([p for p in w['slots'] if not p['player']]) == 3)
rim = w['fusionSkin']['rim']
check('inset uses transparent center rather than black portrait backing', rim['pixels'][4]['a'] == 0, rim)
pixels('fusion-recessed-empty', s, 2)

act('inventory')
iw = next(w for w in act()['workspaceWindows'] if w['kind'] == 'INVENTORY')
drag_window(iw, 230, 320)
cmd('item replace entity @a inventory.0 with minecraft:stone')
inventory(9); cell(0)
invalid = snapshot('invalid-input-server')
check('invalid input accepted without producing an invalid recipe result', item(menu(invalid)['slots'][0]['item']) == 'minecraft:stone' and item(menu(invalid)['slots'][2]['item']) == 'minecraft:air')
invalid_view = capture('fusion-recessed-invalid-input')
c = table(invalid_view)['fusionContent']
invalid_image = Image.open(RUN / 'screenshots/fusion-recessed-invalid-input.png').convert('RGB')
atlas = Image.open(ROOT / 'build/project-everything-small-design/assets/346/assets/cataclysm/textures/gui/fusion.png').convert('RGBA')
marker_pixels = [(x, y, atlas.getpixel((176 + x, y))) for y in range(21) for x in range(28) if atlas.getpixel((176 + x, y))[3] == 255]
check('native invalid-recipe cross retains complete visible marker',
      all(invalid_image.getpixel(((c['x'] + 99 + x) * 2, (c['y'] + 45 + y) * 2)) == rgba[:3] for x, y, rgba in marker_pixels),
      {'opaque_pixels': len(marker_pixels)})
cell(0); inventory(9)

before = table()['fusionContent']
b = table()['bounds']; drag_window(table(), b['x'] + 29, b['y'] + 17)
after = table()['fusionContent']
check('dragging header moves native artwork and slot hit positions together', after['x'] == before['x'] + 29 and after['y'] == before['y'] + 17)

for label, first, second, result in (
    ('weapon', 'infernal_forge', 'void_core', 'void_forge'),
    ('armor', 'ignitium_chestplate', 'minecraft:elytra', 'ignitium_elytra_chestplate'),
):
    qualify = lambda name: name if ':' in name else 'cataclysm:' + name
    first, second, result = map(qualify, (first, second, result))
    cmd('item replace entity @a inventory.0 with ' + first)
    cmd('item replace entity @a inventory.1 with ' + second)
    inventory(9); cell(0); inventory(10); cell(1)
    ready = snapshot(label + '-recipe-ready-server')
    check(label + ' recipe synchronizes native result', item(menu(ready)['slots'][2]['item']) == result)
    capture('fusion-recessed-' + label + '-ready')
    cell(2); inventory(11)
    done = snapshot(label + '-recipe-taken-server')
    check(label + ' result click consumes both inputs and transfers original result',
          all(item(p['item']) == 'minecraft:air' for p in menu(done)['slots'][:3]) and total(done, result) == total(ready, result) + 1)
    cmd('item replace entity @a inventory.2 with minecraft:air')

s = capture('fusion-recessed-with-inventory')
act('gui-scale', scale=3, ticks=15)
drag_window(table(), 110, 40)
iw = next(w for w in act()['workspaceWindows'] if w['kind'] == 'INVENTORY')
drag_window(iw, 120, 200)
s = capture('fusion-recessed-scale3')
pixels('fusion-recessed-scale3', s, 3)
act('gui-scale', scale=2, ticks=15)

cmd('item replace entity @a inventory.0 with cataclysm:infernal_forge')
inventory(9); cell(0)
before = snapshot('before-close')
click(**workspace(table())['controls']['CLOSE'])
after = snapshot('after-close')
check('closing window returns unconsumed native input',
      not any(w.get('menuId') == 'cataclysm:weapon_fusion' for w in act()['allWindows']) and total(after, 'cataclysm:infernal_forge') == total(before, 'cataclysm:infernal_forge') + 1)
act('close'); act('open', ticks=20)
check('ordinary block reopen creates a clean functional panel', table()['slots'][0]['count'] == 0 and table()['fusionSkin']['rim']['pixels'][4]['a'] == 0)
act('inventory')
iw = next(w for w in act()['workspaceWindows'] if w['kind'] == 'INVENTORY')
drag_window(iw, 230, 310)
capture('fusion-recessed-final-fullscreen')
print('Focused Weapon Fusion checks completed.', flush=True)
