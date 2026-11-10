"""Real native XP buttons, exact item data and resource conservation."""
from pathlib import Path
import copy, json, sys
import nbtlib

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'functional-tests/project-everything-ten'))
from send import send


def typed(value):
    if isinstance(value, dict): return (type(value).__name__, tuple((k, typed(v)) for k, v in sorted(value.items())))
    if isinstance(value, list): return (type(value).__name__, tuple(typed(v) for v in value))
    return (type(value).__name__, str(value))


def pouch_item(signature):
    value = nbtlib.parse_nbt(signature)
    assert str(value['id']) in ['archon:experience_pouch', 'archon:super_experience_pouch']
    assert isinstance(value['Count'], nbtlib.Byte) and int(value['Count']) == 1
    xp = value['tag']['archon']['xp']; assert isinstance(xp, nbtlib.Int) and int(xp) >= 0
    other = copy.deepcopy(value); del other['tag']['archon']['xp']
    return int(xp), typed(other)


class Pouch:
    def __init__(self, profile):
        self.profile = Path(profile); self.out = self.profile / 'pouch-release'; self.out.mkdir()
        self.state = {}; self.checks = []; self.completed = []
    def act(self, kind='wait', **kw):
        self.state = send(self.profile, kind, **kw)['client']; return self.state
    def save(self, complete=False):
        (self.out / 'summary.json').write_text(json.dumps(dict(complete=complete, checks=self.checks,
            variants=self.completed, scope='Two native Archon XP pouches, all six real buttons, empty/full limits, native XP ledger, metadata, moved input and close/reopen. No direct packet/callback invocation or physical keyboard hardware claim.'), indent=2) + '\n')
    def check(self, name, condition, **details):
        self.checks.append(dict(name=name, passed=bool(condition), **details)); self.save()
        assert condition, name
        print('PASS', name, flush=True)
    def snapshot(self, label):
        self.act('pouch-resource-state', ticks=3)
        result = json.loads((self.profile / 'pouch-resource-state.json').read_text())
        result['stored'], result['other'] = pouch_item(result['held'])
        self.check(label + '-native-capacity-and-item', result['capacity'] > 0 and result['stored'] <= result['capacity'])
        synced=self.window()['nativeValues']
        self.check(label + '-client-readout-matches-native-server',
                   synced['getExperience']==result['stored'] and synced['getMaxExperience']==result['capacity'])
        # Keep the unmodified native signature and client layout for review.
        raw = {k: v for k, v in result.items() if k != 'other'}
        label = str(len(self.checks)).zfill(4) + '-' + label
        path = self.out / (label + '.json'); assert not path.exists()
        path.write_text(json.dumps(dict(server=raw, client=self.state), indent=2) + '\n')
        return result
    def window(self):
        rows = [v for v in self.state['allWindows'] if v.get('menuId') == 'archon:experience_pouch']
        assert len(rows) == 1
        row = rows[0]
        assert row.get('templateClass', '').endswith('NativeTemplateWindow'), row
        return row
    def widgets(self):
        self.act('regression-native-widget-catalog', ticks=2)
        values = json.loads((self.profile / 'regression-native-widget-catalog.json').read_text())
        return [v for v in values if v.get('source_session') == self.session
                and v['type'] == 'safro.archon.client.screen.ExperienceButtonWidget' and v.get('visible', True)]
    def open(self):
        self.act('regression-use-held-item', shift=False, ticks=12)
        self.act('wait', ticks=3)
        self.session = self.window()['sessionId']
        rows = self.widgets()
        self.check('all-six-native-buttons-are-exposed', len(rows) == 6
                   and sorted(v.get('message') for v in rows) == ['50', '50', '500', '500', '5000', '5000'])
        if self.window().get('locked') in [True, 'true']:
            self.act('regression-control', session=self.session, control='LOCK', ticks=3)
        before = self.window()['bounds']
        self.act('regression-move', session=self.session, dx=18, dy=12, ticks=4)
        self.act('wait', ticks=3)
        self.check('pouch-moves-before-button-actions', self.window()['bounds'] != before)
    def button(self, amount, add, label):
        before = self.snapshot(label + '-before')
        # Native init adds three deposit buttons followed by three withdrawal
        # buttons; message is the actual native integer, even when rendered 5k.
        rows = [v for v in self.widgets() if v.get('message') == str(amount)]
        self.check(label + '-two-native-buttons', len(rows) == 2)
        self.act('regression-template-widget-tap', session=self.session,
                 widget_type=rows[0]['type'], message=str(amount), ordinal=0 if add else 1, ticks=10)
        after = self.snapshot(label + '-after')
        change = min(amount, before['capacity'] - before['stored'], before['nativePlayerXp']) if add else -min(amount, before['stored'])
        self.check(label + '-exact-native-transfer', after['stored'] == before['stored'] + change
                   and after['nativePlayerXp'] == before['nativePlayerXp'] - change, before=before['stored'], after=after['stored'], expected_change=change)
        self.check(label + '-full-unrelated-item-data', after['other'] == before['other'])
        self.check(label + '-xp-conserved', after['stored'] + after['nativePlayerXp'] == before['stored'] + before['nativePlayerXp'])
        return after
    def scenario(self, item):
        self.act('close', ticks=3); self.act('fixture', block='minecraft:air', ticks=3)
        self.act('command', command='gamemode survival @p', ticks=3)
        self.act('command', command='experience set @p 0 levels', ticks=3)
        self.act('command', command='experience set @p 0 points', ticks=3)
        self.act('command', command='item replace entity @p hotbar.0 with '+item+'{archon:{xp:0},release_marker:{name:"pouch-owner",typed:7b}} 1', ticks=3)
        self.open()
        empty = self.snapshot(item.split(':')[1] + '-empty')
        self.check('empty-pouch-and-player-start-at-zero', empty['stored'] == 0 and empty['nativePlayerXp'] == 0)
        self.button(50, True, 'empty-player-deposit-refused')
        self.button(50, False, 'empty-pouch-withdraw-refused')
        self.check('native-capacity-fits-bounded-test-fixture', 0 < empty['capacity'] <= 100000)
        self.act('command', command='experience add @p '+str(empty['capacity'] + 5000)+' points', ticks=3)
        funded = self.snapshot(item.split(':')[1] + '-funded')
        self.check('ordinary-xp-fixture-funds-real-player', funded['nativePlayerXp'] > funded['capacity'])
        for amount in [50, 500, 5000]: self.button(amount, True, 'deposit-' + str(amount))
        # Ensure both capacities reach their actual limit using native buttons.
        while self.snapshot('capacity-progress-' + str(len(self.checks)))['stored'] < funded['capacity']:
            self.button(5000, True, 'fill-' + str(len(self.checks)))
        self.button(50, True, 'full-pouch-deposit-refused')
        full = self.snapshot('full-persist-before-' + item.split(':')[1])
        self.act('regression-control', session=self.session, control='CLOSE', ticks=4)
        self.check('close-releases-pouch-session', not self.state.get('allWindows'))
        self.open(); reopened = self.snapshot('full-persist-after-' + item.split(':')[1])
        self.check('native-xp-and-item-data-persist-after-reopen', reopened['held'] == full['held'] and reopened['nativePlayerXp'] == full['nativePlayerXp'])
        for amount in [50, 500, 5000]: self.button(amount, False, 'withdraw-' + str(amount))
        while self.snapshot('drain-progress-' + str(len(self.checks)))['stored'] > 0:
            self.button(5000, False, 'drain-' + str(len(self.checks)))
        self.button(5000, False, 'drained-pouch-refused')
        self.act('close', ticks=3)
        self.completed.append(item); self.save()


def run(profile, mc, loader, *args, **kwargs):
    assert (mc, loader) == ('1.20.1', 'fabric')
    suite = Pouch(profile); suite.act('gui-scale', scale=2, width=1280, height=900, ticks=3)
    for item in ['archon:experience_pouch', 'archon:super_experience_pouch']: suite.scenario(item)
    assert all(v['passed'] for v in suite.checks) and len(suite.completed) == 2
    suite.save(True)
    return dict(cases=2, failed=0, checks=len(suite.checks), pouch_features_checked=True)
