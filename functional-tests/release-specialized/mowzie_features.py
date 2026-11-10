"""Ordinary Mowzie entity entry, actual native payment and exact results.

Operator commands create the native world prerequisites/items. Trade actions
are normal mouse clicks; no handler callbacks or action packets are injected.
Sculptor coverage stops at starting its test, not challenge gameplay.
"""
from pathlib import Path
import copy, hashlib, json, sys
import nbtlib

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'functional-tests/project-everything-ten'))
from send import send

ENTRIES = ROOT / 'build/project-everything-56-implementation/bf-final-cases.json'
CASES = [('bf-sculptor-clean', 'sculptor', 'Sculptor'),
         ('own-bf-umvuthi-aligned', 'umvuthi', 'Umvuthi'),
         ('own-bf-minion-trade-real', 'umvuthana', 'Umvuthana')]


def typed(value):
    if isinstance(value, dict): return type(value).__name__, tuple((k, typed(v)) for k, v in sorted(value.items()))
    if isinstance(value, list): return type(value).__name__, tuple(typed(v) for v in value)
    return type(value).__name__, str(value)


def stack(text, count=None):
    value = nbtlib.parse_nbt(text)
    assert isinstance(value, nbtlib.Compound)
    if count is not None:
        key = 'Count' if 'Count' in value else 'count'
        assert key in value
        value[key] = type(value[key])(count)
    return typed(value)


def item_count(text):
    value = nbtlib.parse_nbt(text)
    if not value: return 'minecraft:air', 0
    assert isinstance(value.get('id'), nbtlib.String)
    key = 'Count' if 'Count' in value else 'count'
    assert isinstance(value.get(key), nbtlib.Byte if key == 'Count' else nbtlib.Int)
    return str(value['id']), int(value[key])


class Mowzie:
    def __init__(self, profile, mc):
        self.profile = Path(profile); self.mc = mc
        self.out = self.profile / 'mowzie-release'; self.out.mkdir()
        self.state = {}; self.checks = []; self.completed = []; self.name = ''
    def act(self, kind='wait', **kw):
        self.state = send(self.profile, kind, **kw)['client']; return self.state
    def save(self, complete=False):
        (self.out / 'summary.json').write_text(json.dumps(dict(complete=complete, checks=self.checks,
            cases=self.completed, scope='Three ordinary native mob menus, exact offer/payment, refusal, output/blessing or test start, moved inputs and source lifetime. No Sculptor challenge gameplay or every minion offer.',
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()), indent=2) + '\n')
    def check(self, name, value, **details):
        self.checks.append(dict(name=self.name + ':' + name, passed=bool(value), **details)); self.save()
        assert value, name
        print('PASS', self.name, name, flush=True)
    def w(self):
        rows = [v for v in self.state['allWindows'] if v.get('menu', '').endswith('.Container' + self.kind + 'Trade')]
        assert len(rows) == 1, rows
        assert rows[0].get('nativeScreen', '').endswith('.Gui' + self.kind + 'Trade')
        assert rows[0].get('mowzieInputRegions'), rows[0]
        return rows[0]
    def tap(self, x, y):
        self.act('move', x=x, y=y, ticks=1); self.act('click', x=x, y=y, button=0, ticks=1)
        self.act('release', x=x, y=y, button=0, ticks=10)
    def point(self, x, y):
        r = next(v for v in self.w()['mowzieInputRegions'] if v['interactive'] and
                 v['source']['x'] <= x < v['source']['x'] + v['source']['width'] and
                 v['source']['y'] <= y < v['source']['y'] + v['source']['height'])
        return r['x'] + x - r['source']['x'], r['y'] + y - r['source']['y']
    def slot(self, index):
        cell = next(v for v in self.w()['slots'] if v['index'] == index)
        self.tap(*self.point(cell['x'] + 8, cell['y'] + 8))
    def inventory(self, index):
        inv = [v for v in self.state['workspaceWindows'] if v['kind'] == 'INVENTORY']
        if not inv:
            self.act('inventory', ticks=6); inv = [v for v in self.state['workspaceWindows'] if v['kind'] == 'INVENTORY']
        assert len(inv) == 1
        cell = next(v for v in inv[0]['inventoryCells'] if v['inventorySlot'] == index)
        self.tap(cell['x'], cell['y'])
    def button(self):
        w = self.w(); buttons = [v for v in w['nativeWidgets'] if v.get('visible') and v.get('active')]
        self.check('one-active-original-action-button', len(buttons) == 1)
        b = buttons[0]; origin = w['nativeBounds']
        self.tap(*self.point(b['x'] + b['width'] / 2 - origin['x'], b['y'] + b['height'] / 2 - origin['y']))
    def snapshot(self, label):
        self.act('snapshot-server', ticks=3); server = json.loads((self.profile / 'server-state.json').read_text())
        self.act('mowzie-native-state', ticks=2); native = json.loads((self.profile / 'mowzie-native-state.json').read_text())
        label = str(len(self.checks)).zfill(4) + '-' + self.name + '-' + label
        path = self.out / (label + '.json'); assert not path.exists()
        path.write_text(json.dumps(dict(server=server, native=native, client=self.state), indent=2) + '\n')
        return server, native
    def menu(self, data): return next(v for v in data['menus'] if v['class'].endswith('.Container' + self.kind + 'Trade'))
    def owner(self, data): return next(v for v in data['entities'] if v['type'] == 'mowziesmobs:' + self.mob)
    def seed(self, index, item, count):
        self.act('command', command=f'item replace entity @p container.{index} with {item} {count}', ticks=4)
    def open(self, case):
        self.act('close', ticks=3)
        for original in case['core_actions']:
            action = copy.deepcopy(original); kind = action.pop('type'); action['ticks'] = int(action.get('ticks', 5))
            self.act(kind, **action)
        self.w(); self.check('real-native-screen-and-owner-regions', not self.w().get('layoutError'))
        self.act('command', command='gamemode survival @p', ticks=3)
        before = self.w()['bounds']
        workspace = next(v for v in self.state['workspaceWindows'] if v['kind'] == 'CONTAINER' and v['bounds'] == before)
        if workspace.get('locked') in ['true', True]: self.tap(**workspace['controls']['LOCK'])
        b = self.w()['bounds']; x, y = b['x'] + 18, b['y'] + 8
        self.act('click', x=x, y=y, button=0, ticks=1)
        self.act('drag', x=x+16, y=y+10, dx=16, dy=10, button=0, ticks=1)
        self.act('release', x=x+16, y=y+10, button=0, ticks=4)
        self.check('window-moves-before-native-actions', self.w()['bounds'] != before)
    def payment(self, desired, maximum):
        item, cost = item_count(desired); assert item != 'minecraft:air' and 0 < cost <= maximum
        # Respect the observed native stack limit for every offer. Offers with
        # spare capacity additionally exercise an overpayment remainder.
        extra = min(5, maximum - cost)
        self.seed(9, item, cost + extra); before, _ = self.snapshot('payment-in-inventory')
        expected = stack(before['inventory'][9]['item']); self.inventory(9); self.slot(0)
        after, _ = self.snapshot('payment-in-native-slot')
        self.check('whole-payment-data-reaches-native-input', stack(self.menu(after)['slots'][0]['item']) == expected)
        return item, cost, before, extra
    @staticmethod
    def remainder(actual, original, count):
        return stack(actual) == stack(original, count) if count else item_count(actual)[1] == 0
    def scenario(self, case):
        self.open(case); initial, native = self.snapshot('empty')
        owner = self.owner(native)
        self.check('native-owner-recognizes-customer', owner.get('getCustomer') is True)
        if self.mob == 'umvuthana':
            self.check('native-output-is-empty-without-payment', item_count(self.menu(initial)['slots'][1]['item'])[1] == 0)
            desired, output = owner['input'], owner['output']
            self.seed(9, 'minecraft:bedrock', 1); self.inventory(9); self.slot(0)
            refused, _ = self.snapshot('wrong-input')
            self.check('foreign-item-does-not-create-result', item_count(self.menu(refused)['slots'][1]['item'])[1] == 0)
            self.slot(0); self.inventory(9)
            item, cost, before, extra = self.payment(desired, owner['inputMaxCount'])
            paid, _ = self.snapshot('offered-result')
            self.check('original-offer-exposes-exact-result-data', stack(self.menu(paid)['slots'][1]['item']) == stack(output))
            self.slot(1); result, _ = self.snapshot('result-taken')
            carried = self.menu(result)['carried']
            self.check('result-created-exactly-on-shared-cursor', stack(carried) == stack(output))
            self.check('native-cost-removed-and-exact-remainder-remains', self.remainder(self.menu(result)['slots'][0]['item'], before['inventory'][9]['item'], extra))
            self.inventory(10); settled, _ = self.snapshot('result-deposited')
            self.check('result-deposits-once-with-complete-native-data', stack(settled['inventory'][10]['item']) == stack(output)
                       and item_count(self.menu(settled)['carried'])[1] == 0)
            if extra: self.slot(0); self.inventory(9)
            returned, _ = self.snapshot('remainder-returned')
            self.check('remainder-returns-once-and-preview-clears', self.remainder(returned['inventory'][9]['item'], before['inventory'][9]['item'], extra)
                       and item_count(self.menu(returned)['slots'][1]['item'])[1] == 0)
        else:
            if any(v.get('active') and v.get('visible') for v in self.w()['nativeWidgets']):
                self.button(); empty, state = self.snapshot('empty-action')
                self.check('empty-action-does-not-start-test-or-grant-blessing',
                    not self.owner(state).get('isTesting', False) and 'mowziesmobs:suns_blessing' not in state['effects'])
            item, cost, before, extra = self.payment(owner['desired'], owner['desiredMaxCount']); self.button()
            after, native = self.snapshot('native-action')
            self.check('exact-price-consumed-and-remainder-returned-once',
                sum(item_count(v['item'])[1] for v in after['inventory'] if item_count(v['item'])[0] == item) == extra
                and item_count(self.menu(after)['slots'][0]['item'])[1] == 0
                and all(item_count(v['carried'])[1] == 0 for v in after['menus']))
            if self.mob == 'sculptor':
                self.check('native-begin-starts-sculptor-test', self.owner(native)['isTesting'] is True)
            else:
                self.button(); _, native = self.snapshot('blessing')
                self.check('original-grant-action-applies-native-blessing', 'mowziesmobs:suns_blessing' in native['effects'])
        self.act('capture', capture='release-mowzie-' + self.mob, ticks=2)
        self.act('command', command='kill @e[type=mowziesmobs:' + self.mob + ']', ticks=16)
        self.check('invalidated-entity-closes-its-owner-window', not any(v.get('menu', '').endswith('.Container' + self.kind + 'Trade') for v in self.state.get('allWindows', [])))
        self.completed.append(self.name); self.save()
    def run(self):
        self.act('gui-scale', scale=2, width=1280, height=900, ticks=4)
        cases = json.loads(ENTRIES.read_text())['cases']
        for self.name, self.mob, self.kind in CASES:
            self.scenario(next(v for v in cases if v['id'] == self.name))
        self.act('close', ticks=3); self.save(True)
        return dict(cases=3, failed=0, checks=len(self.checks), mowzie_features_checked=True)


def run(profile, mc, loader, *args, **kwargs): return Mowzie(profile, mc).run()
