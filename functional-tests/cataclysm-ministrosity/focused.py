"""Focused Ministrosity inventory, portrait and native chest checks only."""
from pathlib import Path
import json, os, re, sys
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'functional-tests/project-everything-ten'))
from send import send

RUN = ROOT / 'build/remaining-ui-audit/gameplay' / os.environ.get('MINI_RUN', '1.20.1-forge-integrated-ministrosity04')
OUT = ROOT / 'build/cataclysm-ministrosity' / os.environ.get('MINI_EVIDENCE', 'evidence04')
OUT.mkdir(parents=True, exist_ok=True)
checks = []

def act(kind='capture', **kw): return send(RUN, kind, ticks=kw.pop('ticks', 5), **kw)['client']
def cmd(text, **kw): return act('command', command=text, **kw)

def click(x, y, button=0):
    act('click', x=x, y=y, button=button, ticks=1)
    return act('release', x=x, y=y, button=button)

def mini(s=None): return next(w for w in (s or act())['allWindows'] if w.get('menuId') == 'salts_inventory_compat:ministrosity')
def workspace(w, s=None): return next(v for v in (s or act())['workspaceWindows'] if v['bounds'] == w['bounds'])

def control(w, name):
    controls = workspace(w)['controls']
    if name in controls: return click(**controls[name])
    click(**controls['ELLIPSIS'])
    return click(**workspace(w)['popupControls'][name])

def drag(w, x, y):
    if workspace(w)['locked'] == 'true': control(w, 'LOCK')
    b = w['bounds']; sx, sy = b['x'] + 12, b['y'] + 7; dx, dy = x - b['x'], y - b['y']
    act('click', x=sx, y=sy, button=0, ticks=1)
    act('drag', x=sx + dx, y=sy + dy, dx=dx, dy=dy, button=0, ticks=1)
    act('release', x=sx + dx, y=sy + dy, button=0)

def inventory(index):
    w = next(w for w in act()['workspaceWindows'] if w['kind'] == 'INVENTORY')
    p = next(p for p in w['inventoryCells'] if p['inventorySlot'] == index)
    return click(p['x'], p['y'])

def cell(index, button=0):
    w = mini(); c = w['ministrosityContent']; p = w['slots'][index]; r = w['composition']['regions'][0]
    return click(c['x'] + p['x'] - r['source']['x'] + r['x'] + 8,
                 c['y'] + p['y'] - r['source']['y'] + r['y'] + 8, button)

def check(name, passed, details=None):
    checks.append(dict(name=name, passed=bool(passed), details=details))
    (OUT / 'checks.json').write_text(json.dumps(checks, indent=2))
    print(('PASS ' if passed else 'FAIL ') + name, flush=True)
    assert passed, (name, details)

def capture(name):
    s = act(capture=name)
    (OUT / (name + '.json')).write_text(json.dumps(s, indent=2))
    return s

def snapshot(name):
    act('snapshot-server', ticks=1); s = json.loads((RUN / 'server-state.json').read_text())
    (OUT / (name + '.json')).write_text(json.dumps(s, indent=2)); return s

def pet_probe(name):
    act('reviewed-entity-probe', ticks=1); s = json.loads((RUN / 'reviewed-entity-probe.json').read_text())
    (OUT / (name + '.json')).write_text(json.dumps(s, indent=2))
    return next(e for e in s['entities'] if e['type'] == 'cataclysm:netherite_ministrosity')

def menu(s): return next(m for m in s['menus'] if m['class'].endswith('.MinistrostiyMenu'))
def item(nbt): return re.search(r'id:"([^"]+)"', nbt).group(1)
def count(nbt): return int(re.search(r'Count:(\d+)b', nbt).group(1))
def cargo(s, name): return sum(count(p['item']) for p in menu(s)['slots'][:15] if item(p['item']) == name)

def pixels(name, s, scale):
    image = Image.open(RUN / 'screenshots' / (name + '.png')).convert('RGB')
    atlas = Image.open(ROOT / 'build/project-everything-small-design/assets/346/assets/cataclysm/textures/gui/ministrosity2.png').convert('RGBA')
    c = mini(s)['ministrosityContent']; sample = lambda x, y: image.getpixel((int(x * scale), int(y * scale)))
    mismatches = []
    for y in range(76):
        for x in range(123):
            src = atlas.getpixel((53 + x, y))
            if 17 <= y < 71 and 17 <= x < 107: src = atlas.getpixel((x - 17, y - 17 + 166))
            expected = src[:3] if src[3] else (59, 51, 45)
            if y >= 28 and (x < 9 or x >= 115): expected = (59, 51, 45)
            if (y >= 28 and x in (9, 114)) or (y == 75 and 9 <= x <= 114): expected = (0, 0, 0)
            actual = sample(c['x'] + 64 + x, c['y'] + 4 + y)
            if expected != actual: mismatches.append((x, y, expected, actual))
    check('clean native chest and all gold cells match original texture at scale ' + str(scale), not mismatches,
          {'mismatches': len(mismatches), 'examples': mismatches[:8]})
    perimeter = [(x, y) for y in range(75) for x in range(55) if x in (0, 54) or y in (0, 74)]
    check('portrait has a continuous dark rim without white lines at scale ' + str(scale),
          all(sample(c['x'] + 4 + x, c['y'] + 6 + y) == (41, 37, 33) for x, y in perimeter))
    points = [(x, y) for y in range(73) for x in range(53) if sample(c['x'] + 5 + x, c['y'] + 7 + y) != (24, 22, 23)]
    bbox = [min(x for x, y in points), min(y for x, y in points), max(x for x, y in points), max(y for x, y in points)] if points else []
    check('live portrait renders fully inside its viewport at scale ' + str(scale),
          len(points) > 100 and bbox[0] > 0 and bbox[1] > 0 and bbox[2] < 52 and bbox[3] < 72, {'pixels': len(points), 'bounds': bbox})

cmd('gamemode survival @a'); act('fixture', block='minecraft:air', ticks=10)
cmd('summon cataclysm:netherite_ministrosity 8.5 65 10.5 {NoAI:1b,PersistenceRequired:1b}', ticks=10)
act('held', item='cataclysm:lava_power_cell'); act('aim-entity', entity='cataclysm:netherite_ministrosity', ticks=8)
act('use-entity-ray', entity='cataclysm:netherite_ministrosity', ticks=30)
act('held', item='minecraft:air'); act('aim-entity', entity='cataclysm:netherite_ministrosity', ticks=8)
act('use-entity-ray', entity='cataclysm:netherite_ministrosity', ticks=25)
w = mini(); owner = w['ministrosityOwner']['uuid']
check('ordinary awakened pet interaction opens native 15-slot companion',
      w.get('template', '').endswith('MinistrosityWindows$Template') and len([p for p in w['slots'] if not p['player']]) == 15)
check('original owned entity remains in native open animation', pet_probe('opened-owner')['owned'] and w['ministrosityOwner']['attackState'] in (3, 4))
c = w['ministrosityContent']; act('move', x=c['x'] + 31, y=c['y'] + 43, ticks=10)
s = capture('ministrosity-empty-scale2'); pixels('ministrosity-empty-scale2', s, 2)
act('inventory'); iw = next(w for w in act()['workspaceWindows'] if w['kind'] == 'INVENTORY'); drag(iw, 230, 320)
act('gui-scale', scale=3, ticks=15); drag(mini(), 110, 35)
iw = next(w for w in act()['workspaceWindows'] if w['kind'] == 'INVENTORY'); drag(iw, 120, 200)
c = mini()['ministrosityContent']; act('move', x=c['x'] + 31, y=c['y'] + 43, ticks=10)
s = capture('ministrosity-empty-scale3'); pixels('ministrosity-empty-scale3', s, 3)
act('gui-scale', scale=2, ticks=15); drag(mini(), 219, 62)
iw = next(w for w in act()['workspaceWindows'] if w['kind'] == 'INVENTORY'); drag(iw, 230, 320)

cmd('item replace entity @a inventory.1 with minecraft:black_shulker_box')
inventory(10); cell(0); blocked = snapshot('blocked-item')
check('native blacklist rejects a shulker box in an empty gold cell', item(menu(blocked)['slots'][0]['item']) == 'minecraft:air')
inventory(10); returned = snapshot('blocked-item-returned')
check('rejected item can be returned intact to player inventory', item(returned['inventory'][10]['item']) == 'minecraft:black_shulker_box')

cmd('item replace entity @a inventory.0 with minecraft:stone 15'); inventory(9)
for index in range(15): cell(index, button=1)
filled = snapshot('all-fifteen-cells')
check('all fifteen visual cell hit positions store their own item',
      all(item(p['item']) == 'minecraft:stone' and count(p['item']) == 1 for p in menu(filled)['slots'][:15]))
capture('ministrosity-all-fifteen-cells')
control(mini(), 'SORT_INVENTORY'); act(ticks=15); sorted_state = snapshot('sorted-cargo')
check('SIU container sort merges original storage without losing items',
      cargo(sorted_state, 'minecraft:stone') == 15 and len([p for p in menu(sorted_state)['slots'][:15] if count(p['item'])]) == 1)
cmd('item replace entity @a inventory.0 with minecraft:stone 7')
iw = next(w for w in act()['workspaceWindows'] if w['kind'] == 'INVENTORY'); control(iw, 'SORT_WINDOWS')
act(ticks=15)
sorted_in = snapshot('sort-into-companion')
check('SIU sort into companion transfers matching items and respects blacklist',
      cargo(sorted_in, 'minecraft:stone') == 22 and item(sorted_in['inventory'][10]['item']) == 'minecraft:black_shulker_box')

b = mini()['bounds']; drag(mini(), b['x'] + 27, b['y'] + 18)
cmd('item replace entity @a inventory.2 with minecraft:diamond'); inventory(11); cell(14)
moved = snapshot('moved-last-cell')
check('dragged final cell keeps its correct native hit position', item(menu(moved)['slots'][14]['item']) == 'minecraft:diamond')
control(mini(), 'CLOSE'); act(ticks=15)
pet = pet_probe('closed-owner')
check('closing preserves pet cargo and uses native close animation',
      'minecraft:stone' in pet['nbt'] and 'minecraft:diamond' in pet['nbt'] and pet['attackState'] in (0, 5))
act('close'); act(ticks=30); act('aim-entity', entity='cataclysm:netherite_ministrosity', ticks=8)
act('use-entity-ray', entity='cataclysm:netherite_ministrosity', ticks=25)
reopened = snapshot('reopened-cargo')
check('ordinary reopening restores the same owner and all stored items',
      mini()['ministrosityOwner']['uuid'] == owner and cargo(reopened, 'minecraft:stone') == 22 and cargo(reopened, 'minecraft:diamond') == 1)
cmd('fill 0 64 21 20 64 40 minecraft:stone')
cmd('tp @a 8.5 65 30.5 180 8', ticks=15)
check('native eight-block validity closes an out-of-range companion', not any(w.get('menuId') == 'salts_inventory_compat:ministrosity' for w in act()['allWindows']))
act('close'); cmd('tp @a 8.5 65 12.5 180 8', ticks=30)
act('aim-entity', entity='cataclysm:netherite_ministrosity', ticks=8); act('use-entity-ray', entity='cataclysm:netherite_ministrosity', ticks=25)
act('inventory'); act('gui-scale', scale=3, ticks=15); drag(mini(), 110, 35)
iw = next(w for w in act()['workspaceWindows'] if w['kind'] == 'INVENTORY'); drag(iw, 120, 200)
c = mini()['ministrosityContent']; act('move', x=c['x'] + 31, y=c['y'] + 43, ticks=10)
act('held', item='minecraft:air')
capture('ministrosity-final-fullscreen')
print('Focused Ministrosity checks completed.', flush=True)
