"""Native Powah6/7 workflows; preserve the original74 legacy control assertions.

The frozen legacy module is loaded privately. Only its item representation is
adapted; its settings/card/solar interaction methods and assertions are reused
unchanged. No original module globals or files are modified.
"""
from pathlib import Path
import collections, hashlib, importlib.util, json, uuid
import nbtlib
from ae2_quantum_features import expected_item as component_item
from mowzie_features import typed

LEGACY = Path(__file__).with_name('powah_legacy_features.py')
LEGACY_SHA256 = 'c7b66ba0298a2cf5cdfdbdd1f774ba7b32024026b1154db935b298b99c3e03eb'
TARGETS = {'1.21.1/neoforge': ('Powah-6.2.10.jar', 'Container', 110),
           '26.1.2/neoforge': ('Powah-7.0.4-alpha.jar', 'Menu', 74)}
BATTERY_DATA = '{release_powah_battery:197,typed:[B;6b,3b],nested:{check:11L}}'
ENERGY = 1000
SCOPE = ('Original74 cable/hopper/transmitter/solar pointer assertions on the exact '
         'modern publishers, with typed components and native player-card binding. '
         '1.21.1 additionally checks all seven discharger cells, refusal, native '
         'redstone pause, exact item recovery, persistence and balanced real discharge. '
         'Not arbitrary power networks, endurance, transmitter charging or all Powah gameplay.')


def item(text):
    value = nbtlib.parse_nbt(text)
    assert isinstance(value, nbtlib.Compound)
    if not value:
        return 0, None
    assert 'Count' not in value and 'tag' not in value
    assert isinstance(value.get('count'), nbtlib.Int) and isinstance(value.get('id'), nbtlib.String)
    count = int(value['count'])
    assert count > 0
    value['count'] = nbtlib.Int(1)
    return count, typed(value)


def bound_card(player_id, name):
    value = nbtlib.parse_nbt(component_item('1.21.1', 'powah:binding_card', 1, base.CARD))
    number = uuid.UUID(player_id).int
    words = [(number >> shift) & 0xffffffff for shift in [96, 64, 32, 0]]
    value['components']['powah:bound_player'] = nbtlib.Compound({
        'gameProfileId': nbtlib.IntArray([v if v < 0x80000000 else v - 0x100000000 for v in words]),
        'name': nbtlib.String(name)})
    return value.snbt()


def battery(energy):
    assert type(energy) is int and 0 <= energy <= ENERGY
    value = nbtlib.parse_nbt(component_item('1.21.1', 'powah:battery_starter', 1, BATTERY_DATA))
    value['components']['powah:energy_stored'] = nbtlib.Long(energy)
    return value.snbt()


def command_components(text):
    """Translate only the three known frozen fixture item literals."""
    for tag in [base.DIAMOND, base.CARD, base.BAD]:
        if tag in text:
            assert text.count(tag) == 1 and 'item replace entity @a ' in text
            text = text.replace(tag, '[minecraft:custom_data=' + tag + ']')
    return text


assert hashlib.sha256(LEGACY.read_bytes()).hexdigest() == LEGACY_SHA256
spec = importlib.util.spec_from_file_location('_release_powah_modern_private_legacy', LEGACY)
base = importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)
base.item = item
base.bound_card = bound_card


def _fixture_item(original_mc, item_id, count, data):
    assert original_mc == '1.20.1', 'Only the declared frozen fixture literal is adapted'
    return component_item('1.21.1', item_id, count, data)


base.expected_item = _fixture_item


def next_faces(before, index=None):
    """Both pinned modern SideConfig.isAllEquals skip unavailable faces1..5."""
    faces, available, transfer = before['faces'], before['available'], before['transfer']
    assert len(faces) == len(available) == 6 and all(type(v) is bool for v in available)
    assert transfer in base.TRANSFERS and all(v in base.TRANSFERS for v in faces)
    if index is not None:
        assert type(index) is int and 0 <= index < 6
        return [base.next_transfer(v, transfer) if i == index and available[i] and transfer != 'NONE' else v
                for i, v in enumerate(faces)]
    same = len({faces[i] for i in range(1, 6) if available[i]}) <= 1
    return [(base.next_transfer(v, transfer) if same else 'ALL') if available[i] and transfer != 'NONE' else v
            for i, v in enumerate(faces)]


base.next_faces = next_faces


class ModernPowah(base.LegacyPowah):
    def __init__(self, profile, mc, loader):
        assert mc + '/' + loader in TARGETS
        self.profile = Path(profile)
        self.mc, self.loader, self.command = mc, loader, 'server-command'
        self.checks, self.cases, self.case, self.n, self.expected = [], [], '', 0, None
        self.out = self.profile / 'powah-modern-release'
        self.out.mkdir()
        self.act('regression-catalog', ticks=2)
        self.origin = json.loads((self.profile / 'regression-catalog.json').read_text())['origin']

    def cmd(self, text):
        return super().cmd(command_components(text))

    def save(self, complete=False):
        record = dict(complete=complete, target=self.mc + '/' + self.loader,
                      cases=self.cases, checks=self.checks, scope=SCOPE,
                      driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                      original_assertions_source_sha256=LEGACY_SHA256)
        (self.out / 'summary.json').write_text(json.dumps(record, indent=2) + '\n')

    def frame(self, label):
        self.act('regression-powah-modern-state', ticks=4)
        state = json.loads((self.profile / 'release-powah-modern-state.json').read_text())
        self.n += 1
        (self.out / (str(self.n).zfill(3) + '-' + self.case + '-' + label + '.json')).write_text(json.dumps(state, indent=2) + '\n')
        if self.expected is not None:
            assert base.conserved(state['server'], self.expected), 'Complete modern native item ownership changed'
        self.state = state
        return state

    def discharger(self):
        assert self.mc == '1.21.1'
        self.begin('energy_discharger', 'discharger', 'DischargerContainer', 7)
        self.settings()
        # Pause by clicking the real redstone control, before adding the charged
        # fixture. No machine power or menu setting is directly assigned.
        before = self.source(self.state)[1]['values']['redstone']
        for _ in range((base.REDSTONE.index('ON') - base.REDSTONE.index(before)) % 3):
            self.point('native-redstone', 182, 38)
            self.frame('discharger-native-pause-click')
        self.check('discharger native ON mode pauses without a powered neighbor',
                   self.source(self.state)[1]['values']['redstone'] == 'ON')
        self.expected += base.items([battery(ENERGY)])
        self.cmd('item replace entity @a container.9 with powah:battery_starter'
                 '[minecraft:custom_data=' + BATTERY_DATA + ',powah:energy_stored=1000L] 1')
        state = self.frame('declared-charged-native-battery-prerequisite')
        self.check('declared native battery contains exact typed components and1000FE',
                   item(state['server']['inventory'][9]['item']) == item(battery(ENERGY)))
        self.act('regression-player-click', slot=12, ticks=6)
        self.act('regression-slot-click', session=self.sid, slot=0, ticks=6)
        state = self.frame('discharger-refuses-foreign-item')
        self.check('discharger refuses foreign typed item without changing cursor or real cells',
                   item(state['server']['cursor']) == item(component_item(self.mc, 'minecraft:diamond', 7, base.DIAMOND))
                   and not base.items([v['item'] for v in state['server']['world']['items']]))
        self.act('regression-player-click', slot=12, ticks=6)
        self.frame('discharger-foreign-item-restored')
        for index in range(7):
            self.act('regression-player-click', slot=9, ticks=6)
            self.act('regression-slot-click', session=self.sid, slot=index, ticks=6)
            state = self.frame('native-discharger-cell' + str(index))
            _, menu = self.source(state)
            self.check('discharger cell' + str(index) + ' owns exact charged battery in native world and menu',
                       item(menu['slots'][index]['item']) == item(battery(ENERGY))
                       and item(state['server']['world']['items'][index]['item']) == item(battery(ENERGY))
                       and item(state['server']['cursor'])[0] == 0)
            self.act('regression-slot-click', session=self.sid, slot=index, ticks=6)
            self.act('regression-player-click', slot=9, ticks=6)
            state = self.frame('native-discharger-cell' + str(index) + '-recovered')
            self.check('discharger cell' + str(index) + ' recovers the entire charged item exactly once',
                       item(state['server']['inventory'][9]['item']) == item(battery(ENERGY))
                       and not base.items([v['item'] for v in state['server']['world']['items']])
                       and item(state['server']['cursor'])[0] == 0)
        # Deliberately cross the native energy transition once. The expected
        # item ledger changes only from the independently observed energy delta,
        # and all unrelated typed components must still match our declared item.
        self.act('regression-player-click', slot=9, ticks=6)
        self.act('regression-slot-click', session=self.sid, slot=6, ticks=6)
        state = self.frame('paused-battery-for-real-discharge')
        initial = self.source(state)[1]['values']['stored']
        self.expected = None
        self.point('native-redstone', 182, 38)  # ON -> OFF, unpowered = enabled.
        self.act('wait', ticks=60)
        state = self.frame('actual-native-battery-discharge')
        client, menu = self.source(state)
        gained = menu['values']['stored'] - initial
        assert type(gained) is int and 0 < gained <= ENERGY
        remaining = ENERGY - gained
        self.expected = base.items([component_item(self.mc, 'minecraft:diamond', 7, base.DIAMOND), battery(remaining)])
        self.check('native discharger energy gain balances exact item energy loss with every other component preserved',
                   menu['values']['redstone'] == 'OFF' and client['values']['stored'] == menu['values']['stored']
                   and item(state['server']['world']['items'][6]['item']) == item(battery(remaining))
                   and base.conserved(state['server'], self.expected))
        # Stop further discharge before asserting persistence and recovery.
        for _ in range(2):
            self.point('native-redstone', 182, 38)
        state = self.frame('native-discharge-paused-again')
        self.act('close', ticks=6)
        self.frame('discharger-closed-native-world-retains-battery')
        state = self.enter()
        self.sid = self.source(state)[1]['session']
        self.act('regression-slot-click', session=self.sid, slot=6, ticks=6)
        self.act('regression-player-click', slot=9, ticks=6)
        state = self.frame('discharger-post-discharge-item-recovered')
        self.check('discharged native item persists across closure and recovers without loss or duplication',
                   item(state['server']['inventory'][9]['item']) == item(battery(remaining))
                   and not base.items([v['item'] for v in state['server']['world']['items']])
                   and item(state['server']['cursor'])[0] == 0)
        self.finish()

    def run(self):
        suffix = TARGETS[self.mc + '/' + self.loader][1]
        for block, path, menu, count in base.CASES:
            self.begin(block, path, menu.replace('Container', suffix), count)
            self.settings()
            if path == 'player_transmitter':
                self.cards()
            if path == 'solar':
                self.solar()
            self.finish()
        assert len(self.checks) == base.EXPECTED_CHECKS == 74
        if self.mc == '1.21.1':
            self.discharger()
        assert len(self.checks) == TARGETS[self.mc + '/' + self.loader][2]
        self.save(True)
        return dict(cases=len(self.cases), failed=0, checks=len(self.checks), powah_modern_features_checked=True)


def run(profile, mc, loader, *args, **kwargs):
    return ModernPowah(profile, mc, loader).run()
