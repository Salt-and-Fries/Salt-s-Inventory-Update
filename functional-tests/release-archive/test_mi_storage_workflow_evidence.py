"""Reject incomplete or corrupt MI storage evidence; these are offline fixtures."""
from pathlib import Path
import copy
import importlib.util
import sys
import unittest

import nbtlib

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
spec = importlib.util.spec_from_file_location('review_mi_storage_workflow', ROOT / 'tools/reconcile-mi-storage-release-evidence.py')
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
DRIVER, CONTROLS = 'a' * 64, 'b' * 64


def fixture(target='1.21.1/neoforge'):
    mc = target.split('/')[0]
    classes = m.native_classes(target)
    prefix = classes[2].removesuffix('gui.MachineScreen')
    seed = m.physical_item(mc, 'minecraft:diamond', 7, '{release_mi_status_marker:41,typed:[B;3b,6b]}')
    metadata = '{release_mi_output_marker:19,typed:[B;4b,8b]}'
    journal = dict(complete=True, cases=list(m.CASES), control_cases=list(m.CASES),
        driver_sha256=DRIVER, controls_driver_sha256=CONTROLS, probed_cells={}, checks=[],
        control_frames=0, control_capabilities={}, control_counts={})
    transfers, controls = {}, {}
    for case in m.CASES:
        slug = case.removeprefix(m.PREFIX)
        kind = 'fluids' if slug in m.FLUIDS else 'items'
        count = 4 if kind == 'items' else (1 if slug == 'nuclear_fluid_hatch' else 2)
        inserts = kind == 'fluids' and 'output' not in slug
        lockable = slug != 'nuclear_fluid_hatch'
        initial = nbtlib.parse_nbt('[' + ','.join('{amount:0L,key:{},playerInsert:' + str(int(inserts)) + 'b,playerExtract:1b,playerLockable:' + str(int(lockable)) + 'b,playerLocked:0b}' for _ in range(count)) + ']')
        indices = [0, count - 1] if count > 1 else [0]
        caps = dict(lock=True, eject=not (mc == '1.20.1' and slug == 'nuclear_fluid_hatch'), kind=kind)
        initial_extract = slug == 'configurable_tank'
        inventory = [dict(index=i, item=seed if i == 12 else '{}') for i in range(41)]

        def frame(cells, inv=None, cursor='{}', locking=False, extract=initial_extract):
            inv = copy.deepcopy(inventory if inv is None else inv)
            options = [dict(type=prefix + 'guicomponents.AutoExtractClient', hasExtractItems=kind == 'items',
                hasExtractFluids=kind == 'fluids', extractItems=extract if kind == 'items' else False,
                extractFluids=extract if kind == 'fluids' else False)] if caps['eject'] else []
            widgets = [dict(type=prefix + 'gui.MachineScreen$MachineButton', x=x, y=10, width=20, height=20,
                active=True, visible=True, message='') for x in ([120, 100] if caps['eject'] else [120])]
            tag = nbtlib.Compound(dict(id=nbtlib.String('modern_industrialization:' + slug),
                x=nbtlib.Int(8), y=nbtlib.Int(65), z=nbtlib.Int(8)))
            tag[kind] = copy.deepcopy(cells)
            tag['extractItems' if kind == 'items' else 'extractFluids'] = nbtlib.Byte(extract)
            visible = dict(items=[], fluids=[])
            visible[kind] = [dict(index=i, x=40 + 20 * i, y=50, nbt=v.snbt()) for i, v in enumerate(cells)]
            window = dict(definition='com.salts_inventory_update.client.automatic.NativeTemplateWindow',
                menuId='modern_industrialization:machine', nativeSlots=[], nativeWidgets=widgets,
                nativeBounds=dict(x=0, y=0, width=176, height=166), nativeValues={},
                template=dict(regions=[dict(source=dict(x=0, y=0, width=176, height=166))]))
            return dict(client=dict(windows=[window]), server=dict(fixture=tag.snbt(), inventory=inv,
                menus=[dict(**{'class': 'player.Menu'}, carried=cursor), dict(**{'class': m.SERVER}, carried=cursor)]),
                observed=dict(menu=classes[1], screen=classes[2], cells=visible,
                    controls=dict(hasLockButton=caps['lock'], lockingMode=locking, autoExtract=options)))

        def check(name):
            journal['checks'].append(dict(case=case, name=name, passed=True))

        frames = []
        if kind == 'fluids':
            for i in indices:
                filled = copy.deepcopy(initial)
                filled[i]['amount'] = nbtlib.Long(81000 if mc == '1.20.1' else 1000)
                filled[i]['key'] = nbtlib.parse_nbt('{fluid:"minecraft:water"}' if mc == '1.20.1' else '{id:"minecraft:water"}')
                inv = copy.deepcopy(inventory)
                inv[9]['item'] = m.physical_item(mc, 'minecraft:water_bucket' if inserts else 'minecraft:bucket', 1)
                final_inv = copy.deepcopy(inventory)
                final_inv[9]['item'] = m.physical_item(mc, 'minecraft:water_bucket', 1)
                frames += [('empty', frame(initial)), ('prerequisites', frame(initial if inserts else filled, inv)),
                    ('native-first-transfer', frame(filled if inserts else initial, cursor=m.physical_item(mc, 'minecraft:bucket' if inserts else 'minecraft:water_bucket', 1))),
                    ('water-recovered' if inserts else 'reinsertion-refused', frame(initial, cursor=m.physical_item(mc, 'minecraft:water_bucket', 1))),
                    ('reopened', frame(initial, final_inv))]
                for name in ['exact foreign typed fixture and empty cursor', 'native flags and declared water prerequisite',
                    'actual native bucket transfer accounts for water and container',
                    'native inverse transfer recovers exactly one water bucket' if inserts else 'output policy refuses water reinsertion without loss',
                    'fluid result and full physical ledger persist']:
                    check(name)
            check('native first and last fluid cells both completed')
        else:
            filled = copy.deepcopy(initial)
            examples = [(0, 'minecraft:stone', 7, 9), (count - 1, 'minecraft:iron_ingot', 9, 10)]
            for i, item, number, _ in examples:
                filled[i]['amount'] = nbtlib.Long(number)
                filled[i]['key'] = m.item_variant(mc, item, metadata)
            frames = [('empty', frame(initial)), ('declared-stored-output', frame(filled))]
            check('exact foreign typed fixture and empty cursor')
            check('stored output metadata and all native policy flags are exact')
            final_inv = copy.deepcopy(inventory)
            for i, item, number, dest in examples:
                filled[i]['amount'] = nbtlib.Long(0)
                expected = m.physical_item(mc, item, number, metadata)
                frames += [('picked-' + str(i), frame(filled, final_inv, expected)), ('refused-' + str(i), frame(filled, final_inv, expected))]
                final_inv[dest]['item'] = expected
                frames.append(('reopened-' + str(i), frame(filled, final_inv)))
                for name in ['native pickup preserves exact output ', 'native output-only reinsertion is refused ', 'recovered full output persists ']:
                    check(name + str(i))
            initial = filled
            check('all declared output recovered and first and last cells completed')
        transfers[case] = frames
        locked = copy.deepcopy(initial)
        if lockable:
            locked[0]['playerLocked'] = nbtlib.Byte(1)
            locked[0]['locked'] = nbtlib.String('minecraft:air')
        active_extract = not initial_extract if caps['eject'] else initial_extract
        steps = [('initial', initial, False, initial_extract), ('lock-editor-enabled', initial, True, initial_extract),
            ('first-cell-lock-policy', locked, True, initial_extract)]
        names = ['native storage exposes exactly its declared controls', 'native lock editor enables without changing resources',
            'native first storage cell respects its player-lock policy']
        if caps['eject']:
            steps.append(('auto-eject-toggled', locked, True, active_extract))
            names.append('native auto-eject changes exact server option conservatively')
        steps += [('locked-option-reopened', locked, False, active_extract), ('restoring-lock-editor-enabled', locked, True, active_extract),
            ('first-cell-lock-restored', initial, True, active_extract), ('lock-editor-disabled', initial, False, active_extract)]
        names += ['native available storage controls persist on reopening', 'native lock editor restores only the selected empty cell',
            'native lock editor disables without changing storage']
        if caps['eject']:
            steps.append(('auto-eject-restored', initial, False, initial_extract))
            names.append('native auto-eject restores original option and full item ledger')
        steps.append(('controls-restored-reopened', initial, False, initial_extract))
        names.append('native storage controls remain restored after reopening')
        controls[case] = [(label, frame(cells, final_inv, locking=locking, extract=extract)) for label, cells, locking, extract in steps]
        for name in names:
            check(name)
        journal['probed_cells'][case] = dict(kind=kind, native_count=count, indices=indices)
        journal['control_capabilities'][case] = caps
        journal['control_counts'][case] = dict(checks=len(names), frames=len(steps))
        journal['control_frames'] += len(steps)
    check('all native MI storage sessions close')
    return journal, transfers, controls


class StorageWorkflowEvidence(unittest.TestCase):
    def accept(self, j, t, c, target='1.21.1/neoforge'):
        return m.validate(target, j, t, c, DRIVER, CONTROLS)

    def reject(self, mutate):
        j, t, c = fixture()
        mutate(j, t, c)
        with self.assertRaises((AssertionError, KeyError, ValueError, TypeError)):
            self.accept(j, t, c)

    def test_vintage_modern_exact_storage_contract_and_capability_counts(self):
        for target in m.TARGETS:
            j, t, c = fixture(target)
            result = self.accept(j, t, c, target)
            self.assertEqual(result['assertions'], len(j['checks']))
            self.assertEqual(result['native_variants'], 17)
            self.assertEqual(result['control_states'], 168 if target == '1.20.1/fabric' else 170)
            self.assertEqual(result['contract'], 'native-storage-transfers-and-available-controls-not-production')

    def test_completed_exact_original_check_set_and_two_source_hashes_required(self):
        for mutate in [lambda j,t,c:j.update(complete=1), lambda j,t,c:j.update(driver_sha256='c'*64),
            lambda j,t,c:j.update(controls_driver_sha256='c'*64), lambda j,t,c:j['checks'].pop(),
            lambda j,t,c:j['checks'].append(copy.deepcopy(j['checks'][0])), lambda j,t,c:j['checks'][0].update(passed=1)]:
            self.reject(mutate)

    def test_exact_case_native_screen_and_fixture_required(self):
        self.reject(lambda j,t,c:t[m.CASES[0]][0][1]['observed'].update(screen='other.Screen'))
        self.reject(lambda j,t,c:t[m.CASES[0]][0][1]['server'].update(fixture=t[m.CASES[0]][0][1]['server']['fixture'].replace('bronze_fluid_input_hatch','other_machine')))
        self.reject(lambda j,t,c:t[m.CASES[0]][0][1]['server']['menus'][1].update({'class':'other.Menu'}))
        with self.assertRaises(AssertionError):
            m.specification('1.21.1/fabric',m.CASES[0],[m.SERVER])

    def test_first_and_last_cell_observations_cannot_be_omitted_or_renamed(self):
        self.reject(lambda j,t,c:t[m.CASES[0]].pop())
        self.reject(lambda j,t,c:j['probed_cells'][m.CASES[0]].update(indices=[0]))
        self.reject(lambda j,t,c:t[m.CASES[0]].__setitem__(2,('reopened',t[m.CASES[0]][2][1])))
        self.reject(lambda j,t,c:j['probed_cells'][m.CASES[0]].update(native_count=True))

    def test_wrong_fluid_amount_and_client_server_desynchronization_rejected(self):
        self.reject(lambda j,t,c:t[m.CASES[0]][2][1]['server'].update(fixture=t[m.CASES[0]][2][1]['server']['fixture'].replace('1000L','999L')))
        self.reject(lambda j,t,c:t[m.CASES[0]][2][1]['observed']['cells']['fluids'][0].update(nbt=t[m.CASES[0]][2][1]['observed']['cells']['fluids'][0]['nbt'].replace('1000L','1000')))

    def test_refused_transfer_and_reopened_full_output_metadata_required(self):
        case = m.PREFIX + m.OUTPUTS[0]
        # A single native owner may legitimately be empty. Loss means every
        # owner is empty even though the output left the storage cell.
        self.reject(lambda j,t,c:[v.update(carried='{}') for v in t[case][3][1]['server']['menus']])
        self.reject(lambda j,t,c:t[case][4][1]['server']['inventory'][9].update(item=t[case][4][1]['server']['inventory'][9]['item'].replace('[B;','[I;')))
        self.reject(lambda j,t,c:t[case][7][1]['server']['inventory'][10].update(item='{}'))

    def test_hidden_inventory_cursor_and_missing_menu_cannot_pass(self):
        self.reject(lambda j,t,c:t[m.CASES[0]][4][1]['server']['inventory'][13].update(item=m.physical_item('1.21.1','minecraft:stone',1)))
        self.reject(lambda j,t,c:t[m.CASES[0]][4][1]['server']['menus'][1].update(carried=m.physical_item('1.21.1','minecraft:stone',1)))
        self.reject(lambda j,t,c:t[m.CASES[0]][4][1]['server'].update(menus=[]))
        self.reject(lambda j,t,c:t[m.CASES[0]][0][1]['server']['inventory'][12].update(item=t[m.CASES[0]][0][1]['server']['inventory'][12]['item'].replace('[B;','[I;')))

    def test_native_option_change_restore_and_reopening_required(self):
        self.reject(lambda j,t,c:c[m.CASES[0]][3][1]['observed']['controls']['autoExtract'][0].update(extractFluids=False))
        self.reject(lambda j,t,c:c[m.CASES[0]][-1][1]['server'].update(fixture=c[m.CASES[0]][-1][1]['server']['fixture'].replace('extractFluids: 0b','extractFluids: 1b')))
        self.reject(lambda j,t,c:c[m.CASES[0]].pop())
        self.reject(lambda j,t,c:j['control_counts'][m.CASES[0]].update(frames=9))

    def test_changed_native_capabilities_or_hidden_buttons_rejected(self):
        self.reject(lambda j,t,c:c[m.CASES[0]][0][1]['observed']['controls'].update(hasLockButton=False))
        self.reject(lambda j,t,c:c[m.CASES[0]][0][1]['client']['windows'][0]['nativeWidgets'][0].update(visible=False))
        self.reject(lambda j,t,c:j['control_capabilities'][m.CASES[0]].update(eject=1))

    def test_cut_virtual_cell_and_control_geometry_rejected(self):
        self.reject(lambda j,t,c:t[m.CASES[0]][0][1]['observed']['cells']['fluids'][0].update(x=0))
        self.reject(lambda j,t,c:c[m.CASES[0]][0][1]['client']['windows'][0]['template']['regions'][0]['source'].update(height=25))

    def test_lock_state_preservation_and_other_cell_policy_required(self):
        self.reject(lambda j,t,c:c[m.CASES[0]][2][1]['observed']['cells']['fluids'][0].update(nbt=c[m.CASES[0]][2][1]['observed']['cells']['fluids'][0]['nbt'].replace('playerLocked: 1b','playerLocked: 0b')))
        self.reject(lambda j,t,c:c[m.CASES[0]][-1][1]['server']['inventory'][9].update(item='{}'))


if __name__ == '__main__':
    unittest.main(verbosity=2)
