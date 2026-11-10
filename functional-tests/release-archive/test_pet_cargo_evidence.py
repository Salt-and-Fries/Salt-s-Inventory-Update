"""Offline corruption controls for pet evidence; these do not run the native UI."""
from pathlib import Path
import copy
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import focused_release_evidence_pet_cargo as reader
DRIVER = 'a' * 64


def observations(kind, target='1.21.1/neoforge'):
    cfg = reader.PETS[kind]
    def item(identifier, amount, marker, damage=None):
        data = '{' + ('release_pet_marker' if kind == 'ministrosity' else 'release_moa_marker') + ':' + str(marker)
        data += ',typed:[B;' + ('1b,7b' if kind == 'ministrosity' else '3b,9b') + ']}'
        if target.startswith('1.20.1/'):
            if damage is not None: data = data[:-1] + ',Damage:' + str(damage) + '}'
            return '{id:"' + identifier + '",Count:' + str(amount) + 'b,tag:' + data + '}'
        components = '{"minecraft:custom_data":' + data
        if damage is not None: components += ',"minecraft:damage":' + str(damage)
        return '{id:"' + identifier + '",count:' + str(amount) + ',components:' + components + '}}'

    base = dict(client=dict(menu=cfg['menu'], screen=cfg['screen'], session=17, locked=False,
                            bounds=dict(x=40, y=40, width=220, height=140)),
                owner='10000000-0000-0000-0000-000000000001',
                cells=[dict(index=i, item='{}') for i in range(cfg['cells'])],
                inventory=[dict(index=i, item='{}') for i in range(41)],
                cursors=[dict(menu=cfg['menu'], item='{}')])
    if kind == 'moa': base.update(saddled=False, armor='{}')
    states = [copy.deepcopy(base) for _ in cfg['labels']]
    if kind == 'ministrosity':
        stone = item('minecraft:stone', 15, 51)
        black = item('minecraft:black_shulker_box', 1, 52)
        diamond = item('minecraft:diamond', 3, 53)
        for state in states[1:]: state['inventory'][10]['item'] = black
        states[1]['inventory'][9]['item'] = stone
        states[2]['inventory'][9]['item'] = stone
        states[2]['inventory'][10]['item'] = '{}'
        states[2]['cursors'][0]['item'] = black
        for cell in states[3]['cells']: cell['item'] = item('minecraft:stone', 1, 51)
        states[4]['cells'][0]['item'] = stone
        for state in states[5:10]: state['cells'][0]['item'] = item('minecraft:stone', 22, 51)
        states[7]['inventory'][11]['item'] = diamond
        for state in states[8:10]: state['cells'][-1]['item'] = diamond
        states[10]['inventory'][9]['item'] = item('minecraft:stone', 22, 51)
        states[10]['inventory'][11]['item'] = diamond
    else:
        saddle = item('minecraft:saddle', 1, 61)
        armor = item('aether_protect_your_moa:iron_moa_armor', 1, 62, damage=2)
        diamond = item('minecraft:diamond', 3, 63)
        stone = item('minecraft:stone', 15, 64)
        for state in states[1:]:
            for idx, value in [(9, saddle), (10, armor), (11, diamond), (12, stone)]:
                state['inventory'][idx]['item'] = value
        for state in states[2:4]:
            state['inventory'][11]['item'] = '{}'
            state['cursors'][0]['item'] = diamond
        for state in states[4:10]:
            state['cells'][0]['item'] = saddle; state['cells'][1]['item'] = armor
            state['inventory'][9]['item'] = state['inventory'][10]['item'] = '{}'
            state.update(saddled=True, armor=armor)
        for state in states[5:10]:
            state['inventory'][12]['item'] = '{}'
            for cell in state['cells'][2:]: cell['item'] = item('minecraft:stone', 1, 64)
        states[7]['cells'][-1]['item'] = '{}'
        states[7]['inventory'][13]['item'] = item('minecraft:stone', 1, 64)
    for state in states[6:]:
        state['client']['bounds']['x'] += 17
        state['client']['bounds']['y'] += 12 if kind == 'ministrosity' else 11
    for state in states[9:]: state['client']['session'] = 18
    journal = dict(complete=True, cases=[cfg['case']], driver_sha256=DRIVER,
                   checks=[dict(name=n, passed=True) for n in cfg['checks']])
    return journal, states, dict(menus=[])


class PetCargoEvidence(unittest.TestCase):
    def rejected(self, kind, change, target='1.21.1/neoforge'):
        journal, states, removed = observations(kind, target)
        change(journal, states, removed)
        with self.assertRaises((AssertionError, KeyError, ValueError, TypeError)):
            reader.validate(target, kind, journal, states, removed, DRIVER)

    def test_original_five_target_contracts_keep_actual_assertion_and_state_counts(self):
        for kind, cfg in reader.PETS.items():
            for target in cfg['targets']:
                journal, states, removed = observations(kind, target)
                result = reader.validate(target, kind, journal, states, removed, DRIVER)
                self.assertEqual(result['assertions'], 10 if kind == 'ministrosity' else 11)
                self.assertEqual(result['states'], 11)
                self.assertEqual(result['cargo_cells'], 15)

    def test_missing_reordered_false_or_incomplete_original_checks_are_rejected(self):
        for kind in reader.PETS:
            for change in [lambda j,s,r:j['checks'].pop(),
                           lambda j,s,r:j['checks'].reverse(),
                           lambda j,s,r:j['checks'][0].update(passed=1),
                           lambda j,s,r:j.update(complete=1),
                           lambda j,s,r:j.update(driver_sha256='b'*64),
                           lambda j,s,r:s.pop()]:
                self.rejected(kind, change)

    def test_native_classes_full_cell_count_owner_and_session_cannot_be_substituted(self):
        for kind in reader.PETS:
            for change in [lambda j,s,r:s[0]['client'].update(screen='fallback.Screen'),
                           lambda j,s,r:s[3]['cells'].pop(),
                           lambda j,s,r:s[4]['cells'][1].update(index=0),
                           lambda j,s,r:s[6].update(owner='20000000-0000-0000-0000-000000000002'),
                           lambda j,s,r:s[9]['client'].update(session=True)]:
                self.rejected(kind, change)

    def test_all_fifteen_cells_and_the_declared_sort_transfer_are_checked(self):
        self.rejected('ministrosity', lambda j,s,r:s[3]['cells'][-1].update(item='{}'))
        self.rejected('ministrosity', lambda j,s,r:s[5]['cells'][0].update(item=s[5]['cells'][0]['item'].replace('count:22','count:23')))
        self.rejected('ministrosity', lambda j,s,r:s[4]['cells'][1].update(item=s[4]['cells'][0]['item']))
        self.rejected('moa', lambda j,s,r:s[5]['cells'][-1].update(item='{}'))

    def test_full_typed_payload_and_native_count_types_are_preserved(self):
        self.rejected('ministrosity', lambda j,s,r:s[3]['cells'][7].update(item=s[3]['cells'][7]['item'].replace('[B;1b,7b]','[I;1,7]')))
        self.rejected('moa', lambda j,s,r:s[4]['cells'][1].update(item=s[4]['cells'][1]['item'].replace('damage":2','damage":2b')))
        self.rejected('moa', lambda j,s,r:s[1]['inventory'][11].update(item=s[1]['inventory'][11]['item'].replace('count:3','count:3b')))
        self.rejected('ministrosity', lambda j,s,r:s[1]['inventory'][9].update(item=s[1]['inventory'][9]['item'].replace('Count:15b','Count:15')), '1.20.1/forge')

    def test_refusal_cannot_silently_insert_or_destroy_the_held_item(self):
        self.rejected('ministrosity', lambda j,s,r:s[2]['cells'][0].update(item=s[1]['inventory'][10]['item']))
        self.rejected('ministrosity', lambda j,s,r:s[2]['cursors'][0].update(item='{}'))
        self.rejected('moa', lambda j,s,r:s[3]['cells'][1].update(item=s[1]['inventory'][11]['item']))
        self.rejected('moa', lambda j,s,r:s[2]['cursors'][0].update(item='{}'))

    def test_native_saddle_and_damaged_armor_effects_must_match_the_cells(self):
        self.rejected('moa', lambda j,s,r:s[4].update(saddled=False))
        self.rejected('moa', lambda j,s,r:s[9].update(armor='{}'))
        self.rejected('moa', lambda j,s,r:s[10].update(saddled=True))
        self.rejected('moa', lambda j,s,r:s[4]['inventory'][9].update(item=s[1]['inventory'][9]['item']))

    def test_last_cell_movement_reopening_and_exact_recovery_are_required(self):
        self.rejected('ministrosity', lambda j,s,r:s[6]['client'].update(bounds=copy.deepcopy(s[5]['client']['bounds'])))
        self.rejected('ministrosity', lambda j,s,r:s[8]['cells'][-1].update(item='{}'))
        self.rejected('ministrosity', lambda j,s,r:s[9]['cells'].reverse())
        self.rejected('moa', lambda j,s,r:s[7]['inventory'][13].update(item='{}'))
        self.rejected('moa', lambda j,s,r:s[8]['cells'][-1].update(item='{}'))
        self.rejected('moa', lambda j,s,r:s[10]['inventory'][12].update(item='{}'))

    def test_unrelated_inventory_shared_cursor_and_final_range_closure_are_checked(self):
        for kind in reader.PETS:
            for change in [lambda j,s,r:s[6]['inventory'][30].update(item=s[1]['inventory'][9]['item']),
                           lambda j,s,r:s[9]['cursors'][0].update(item=s[1]['inventory'][9]['item']),
                           lambda j,s,r:r['menus'].append(dict(session=18))]:
                self.rejected(kind, change)

    def test_unreviewed_targets_aliases_and_classes_do_not_gain_acceptance(self):
        for kind, cfg in reader.PETS.items():
            with self.assertRaises(AssertionError): reader.specification('future/fabric', cfg['case'], [cfg['screen']])
            with self.assertRaises(AssertionError): reader.specification(cfg['targets'][0], cfg['case']+'-alias', [cfg['screen']])
            with self.assertRaises(AssertionError): reader.specification(cfg['targets'][0], cfg['case'], ['other.Screen'])


if __name__ == '__main__':
    unittest.main(verbosity=2)
