"""Affected AE2WTLib views and native input, using shipping JARs in a disposable world."""
from pathlib import Path
import sys, json, shutil, time
ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'functional-tests/ae2-wireless-1.20.1'))
import focused as f

version, run = sys.argv[1:3]
f.RUN = Path(run).resolve()
f.OUT = ROOT / 'build/ae2-wireless-all-targets' / version
f.OUT.mkdir(exist_ok=True)
f.LOG = []
def observed_send(kind, **args):
    response=f.send(f.RUN,kind,**args)
    f.LOG.append({'command':kind,'args':args,'client':response['client']})
    return response['client']
f.S=observed_send
checks = []
def check(name, value):
    assert value, name
    checks.append({'name': name, 'pass': True})
    (f.OUT / 'checks.json').write_text(json.dumps(checks, indent=2))
    print('PASS', name, flush=True)
def state(): return f.S('wait', ticks=12)
def window(s): return next(w for w in s['allWindows'] if w.get('definition', '').endswith('.AEWindow'))
def native(s, suffix):
    w = window(s)
    check('Native ' + suffix, w.get('nativeScreen', '').endswith(suffix))
    check('Geometry ' + suffix, bool(w.get('wirelessGeometry')) and not w.get('wirelessObserverError'))
    return w
def widgets(s): return window(s).get('nativeWidgets', [])
def button(s, name): return next(a for a in widgets(s) if a.get('visible') and a.get('message', '').split('\n')[0] == name)
def press(s, name):
    a = button(s, name)
    return f.tap(a['x'] + a['width']/2, a['y'] + a['height']/2)
def slots(s, semantic):
    w = window(s)
    return [w['slots'][x['index']] for x in w['slotVisibility']
            if x['visible'] and ('id=' + semantic + ',') in x['semantic']]
def index(s, semantic): return slots(s, semantic)[0]['index']
def inventory(s):
    if not any(w['kind'] == 'INVENTORY' for w in s['workspaceWindows']):
        s = f.S('inventory', ticks=30)
    return s
def picture(name, s):
    f.save(name, s)
    source=f.RUN/'screenshots'/(name+'.png')
    for attempt in range(100):
        if source.exists():break
        time.sleep(.05)
    shutil.copyfile(source, f.OUT / (name + '.png'))
def open_item(item):
    f.S('close', ticks=5)
    f.cmd('item replace entity @a weapon.mainhand with ' + item + '[ae2:stored_energy=1000000.0d,ae2:wireless_link_target={dimension:"minecraft:overworld",pos:[I;8,65,8]}]', 12)
    return f.S('use-item', ticks=30)

f.S('close', ticks=3)
f.cmd('gamemode survival @a'); f.cmd('clear @a')
f.cmd('fill 6 65 6 11 68 10 minecraft:air')
f.cmd('fill 4 64 4 14 64 14 minecraft:stone')
f.cmd('setblock 8 65 8 ae2:wireless_access_point')
f.cmd('setblock 8 65 9 ae2:creative_energy_cell', 35)
f.cmd('tp @a 8.5 65 12.5 180 8', 20)
s = open_item('ae2:wireless_crafting_terminal')
native(s, 'WCTScreen')
check('Magnet filter hidden without card', not any(a.get('visible') and a['message'] == 'Magnet Filter' for a in widgets(s)))
check('Equipment delegated to Character window', all(not p['visible'] for p in window(s)['slotVisibility'] if 'AE2WTLIB_' in p['semantic']))
s = inventory(s)
f.cmd('item replace entity @a inventory.0 with minecraft:oak_log 2', 12)
s = state(); s = f.itap(s, 9); s = f.slot(window(s), index(s, 'CRAFTING_GRID'))
if not slots(s, 'CRAFTING_GRID')[0]['item'].endswith('oak_log'):
    s = f.itap(s, 9); s = f.slot(window(s), index(s, 'CRAFTING_GRID'))
check('Native crafting result', slots(s, 'CRAFTING_RESULT')[0]['item'] == 'minecraft:oak_planks')
s = f.slot(window(s), index(s, 'CRAFTING_RESULT')); s = f.itap(s, 11)
check('Craft output reaches Inventory', any(p['index']==11 and p['item']=='minecraft:oak_planks' for p in json.loads((f.RUN/'reply.json').read_text())['server']['playerInventory']))
f.cmd('item replace entity @a inventory.1 with ae2wtlib:magnet_card', 12)
s = state(); s = f.itap(s, 10); s = f.slot(window(s), index(s, 'UPGRADE')); s = state()
check('Native magnet card installation', slots(s, 'UPGRADE')[0]['item'] == 'ae2wtlib:magnet_card')
labels = ['Wireless Terminal Settings', 'Magnet Filter', 'Trash']
actions = [button(s, n) for n in labels]
check('Native footer action order', sorted(actions, key=lambda a:a['x']) == actions)
check('All actions below crafting output', all(a['y'] >= window(s)['bounds']['y'] + slots(s, 'CRAFTING_RESULT')[0]['y'] + 16 for a in actions))
picture('wireless-crafting', s)
s = press(s, 'Wireless Terminal Settings'); native(s, 'WirelessTerminalSettingsScreen')
check('Five native wireless preferences', sum(a.get('visible') and a['type'].endswith('Checkbox') for a in widgets(s)) == 5)
picture('wireless-settings', s)
s = press(s, 'Wireless Crafting Terminal'); native(s, 'WCTScreen')
s = press(s, 'Magnet Filter'); w = native(s, 'MagnetScreen')
check('All 54 native filter slots', len(slots(s, 'AE2WTLIB_PICKUP_CONFIG')) == 27 and len(slots(s, 'AE2WTLIB_INSERT_CONFIG')) == 27)
check('Return tab stays top-right', button(s, 'Wireless Crafting Terminal')['x'] >= w['bounds']['x'] + 150)
picture('magnet-filter', s)
s = press(s, 'Wireless Crafting Terminal'); native(s, 'WCTScreen')
s = press(s, 'Trash'); w = native(s, 'TrashScreen')
check('All 27 Trash slots', len(slots(s, 'AE2WTLIB_TRASH')) == 27)
check('Trash return tab stays top-right', button(s, 'Wireless Crafting Terminal')['x'] >= w['bounds']['x'] + 150)
picture('trash', s)
s = press(s, 'Wireless Crafting Terminal'); native(s, 'WCTScreen')
s = open_item('ae2wtlib:wireless_pattern_encoding_terminal'); native(s, 'WETScreen')
picture('pattern-encoding', s)
for label in ['Processing Patterns', 'Smithing Table Patterns', 'Stonecutting Patterns', 'Crafting Patterns']:
    s = press(s, label)
    check('Native mode: ' + label, window(s)['nativeScreen'].endswith('WETScreen'))
    picture('encoding-' + label.split()[0].lower(), s)
s = open_item('ae2wtlib:wireless_pattern_access_terminal'); native(s, 'WATScreen')
picture('pattern-access', s)
check('No automatic native fallback', all(w['definition'].endswith('.AEWindow') for w in s['allWindows']))
(f.OUT / 'observations.json').write_text(json.dumps(f.LOG, indent=2))
print(version, 'PASS', len(checks), 'focused checks', flush=True)
