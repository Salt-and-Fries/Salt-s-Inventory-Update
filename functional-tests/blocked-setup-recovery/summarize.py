"""Summarize measured recovery runs without upgrading limited case coverage."""
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path
import hashlib
import json

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'output/blocked-setup-recovery'
RUNS = [
    'charm-themed-recovery-01',
    'charm-tail-recovery-01',
    'bumblezone-recovery-01',
    'polymer-cohort-recovery-01',
    'briefcase-actions-recovery-01',
    'waystone-entry-recovery-01',
]


def read(path):
    return json.loads(path.read_text(encoding='utf-8'))


def main():
    runs, latest, checks, case_executions = [], {}, 0, 0
    for name in RUNS:
        record = read(OUT / (name + '.json'))
        assert record.get('finished_at') and record.get('exit_code') == 0, name
        assert record['status'] in {'passed', 'completed-with-coverage-limits'}, name
        profile = ROOT / record['profile']
        measured_path = profile / 'mod-interaction-checks.json'
        measured = read(measured_path)
        assert measured['checks'] and all(c['passed'] is True for c in measured['checks']), name
        assert not any(c['status'] == 'failed' for c in measured['cases']), name
        checks += len(measured['checks'])
        case_executions += len(measured['cases'])
        statuses = dict(Counter(c['status'] for c in measured['cases']))
        for case in measured['cases']:
            key = (record['recovery']['source_fixture'], case['id'])
            latest[key] = {'case': case['id'], 'source_fixture': key[0],
                           'status': case['status'], 'run': name,
                           'reason': case.get('reason')}
        row = {'run': name, 'target': record['target'], 'fixture': record['fixture'],
               'cases': len(measured['cases']), 'case_statuses': statuses,
               'assertions': len(measured['checks']), 'failed_assertions': 0,
               'process_exit_code': record['exit_code'], 'finished_at': record['finished_at'],
               'record': str((OUT / (name + '.json')).relative_to(ROOT)),
               'case_evidence': str(measured_path.relative_to(ROOT))}
        if name == 'briefcase-actions-recovery-01':
            extra_path = profile / 'briefcase-action-checks.json'
            extra = read(extra_path)
            assert len(extra['checks']) == 15 and all(c['passed'] is True for c in extra['checks'])
            checks += len(extra['checks'])
            row['additional_native_action_checks'] = len(extra['checks'])
            row['additional_action_evidence'] = str(extra_path.relative_to(ROOT))
            row['additional_action_scope'] = extra['scope']
        runs.append(row)

    baseline = read(OUT / 'shipping-subjects-before.json')
    assert len(baseline) == 24
    integrity = [{'file': name, 'sha256': hashlib.sha256((ROOT / name).read_bytes()).hexdigest(),
                  'expected_sha256': expected} for name, expected in baseline.items()]
    assert all(r['sha256'] == r['expected_sha256'] for r in integrity)
    assert checks == 1266 and case_executions == 44
    assert Counter(r['status'] for r in latest.values()) == {'passed': 37, 'entry-and-policy-only': 5}
    waystone = next(c for c in latest.values() if c['case'] == 'atlas-waystones-modifiers')
    assert waystone['run'] == 'waystone-entry-recovery-01' and waystone['status'] == 'passed'
    opening_only = [c for c in latest.values() if c['status'] == 'entry-and-policy-only'
                    and c['case'] != 'atlas-bumblezone-briefcase']
    assert len(opening_only) == 4

    summary = {
        'generated_at': datetime.now(timezone.utc).isoformat(),
        'scope': 'Only previously blocked setups/cases and the explicitly selected startup-blocked cohort. No complete global functional suite rerun.',
        'outcome': 'Affected tests completed in disclosed, dependency-complete partitions of the original setups. Original incompatible combinations are not repaired or certified.',
        'root_causes': [
            {'target': '1.21.1/fabric', 'combination': 'Charm Forked + Aether',
             'native_failure': 'No enum constant net.minecraft.class_5421.KILN',
             'finding': 'Native SIU-absent control reproduced the failure. Charm and Aether both extend the recipe-book enum; removing Aether allows the kiln tests to run. Exact initialization ordering is inferred from bytecode and the isolation result.',
             'setup_change': 'Two disposable Charm profiles omit Aether and its required dependent Protect Your Moa. All retained publisher jars are byte-identical.',
             'original_native_control': 'output/project-everything-regression/charm-fill-native-signature.json'},
            {'target': '26.1.2/fabric', 'combination': 'Bumblezone + Polymer',
             'native_failure': 'queen_main_trades_sync_packet -> IllegalStateException: Stack must be non-empty',
             'finding': 'Native SIU-absent control reproduced the login disconnect. Bumblezone supplies a reward stack of two totems, above its max stack size of one; Polymer codec validation rejects the trade data.',
             'setup_change': 'One disposable profile omits Polymer and Universal Graves, which requires Polymer. Another omits only Bumblezone. Together they retain every original publisher. No publisher data or jar is patched.',
             'original_native_control': 'output/project-everything-regression/project-everything-native-login-02-native-login-control.json'},
        ],
        'runs': runs,
        'live_assertions': checks,
        'failed_live_assertions': 0,
        'case_executions_including_replays': case_executions,
        'latest_cases_by_source_fixture': list(latest.values()),
        'latest_case_statuses': dict(Counter(c['status'] for c in latest.values())),
        'remaining_opening_and_policy_only_cases': opening_only,
        'briefcase_coverage': 'The base case retains its entry-and-policy-only classification. Fifteen separate live checks additionally exercise capture, mapped mouse controls, healing, stinger restoration, growing, pollen, resource consumption, completed actions, close/reopen, release and recapture.',
        'fixture_correction': {
            'case': 'atlas-waystones-modifiers',
            'previous_problem': 'The startup-blocked cohort used unregistered waystones:waystone as its entry fixture.',
            'correction': 'Only when the exact 26.1.2 Fabric modifier workflow lacks the old registry entry and contains the replacement, place waystones:andesite_waystone and use the normal native modifier button.',
            'validation': 'Fresh replay reached the publisher menu and passed 47 assertions. Historical invalid-fixture-workflow evidence remains preserved.',
            'source': 'functional-tests/project-everything-regression/registry_entry_adaptations.py',
        },
        'focused_setup_and_harness_tests': {
            'passed': 8,
            'commands': ['python functional-tests/blocked-setup-recovery/test_partitions.py',
                         'python functional-tests/project-everything-regression/test_registry_entry_adaptations.py'],
            'observed_result': 'Each command exited 0 and reported four tests OK.',
            'coverage': 'Original artifact and platform pins, required dependency closure, union of split publisher profiles, all 24 shipping hashes, and exact-scoped immutable registry-entry adaptation.',
        },
        'shipping_integrity': {'unchanged_jars': 24, 'subjects': integrity},
        'limits': [
            'Partitioned passes do not certify the original Charm/Aether or Bumblezone/Polymer co-installations.',
            'Four listed Toms Storage cases have opening/policy checks only; specialized inventory or network interactions are not claimed.',
            'Passing assertions are not a claim that every feature in every mod/version/loader was exercised.',
            'No Core/API, Compatibility, publisher jar or publisher trade-data changes were required in this recovery task.',
        ],
    }
    destination = OUT / 'summary.json'
    destination.write_text(json.dumps(summary, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'summary': str(destination.relative_to(ROOT)), 'live_assertions': checks,
                      'failed_assertions': 0, 'latest_cases': len(latest),
                      'remaining_opening_only_cases': len(opening_only), 'unchanged_shipping_jars': 24}))


if __name__ == '__main__':
    main()
