"""Keep intermittent failures and opening-only coverage visible across trials."""
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path
import hashlib
import json

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'output/ae2-click-investigation'


def measure(trials):
    statuses, failed, assertions, executions = Counter(), [], 0, 0
    for trial in trials:
        statuses.update(c['status'] for c in trial['cases'])
        executions += len(trial['cases'])
        assertions += len(trial['checks'])
        failed.extend(c for c in trial['checks'] if c['passed'] is not True)
    failed_cases = statuses.get('failed', 0)
    return {'case_executions': executions, 'case_statuses': dict(statuses),
            'assertions': assertions, 'failed_assertions': failed,
            'outcome': 'failure-reproduced' if failed or failed_cases else 'not-reproduced',
            'resolved': False,
            'interpretation': 'A later pass never clears an earlier failure. Passing repetitions alone establish neither a cause nor a fix.'}


def main():
    runs, quiet, traced = [], [], []
    for name in ['legacy-baseline-01', 'requester-baseline-01',
                 'legacy-context-quiet-01', 'requester-context-quiet-01']:
        folder = OUT / name
        record = json.loads((folder / 'run.json').read_text())
        assert record.get('finished_at') and record.get('exit_code') == 0, name
        assert len(record['trials']) == record['planned_trials'], name
        trials = []
        for i in range(record['planned_trials']):
            evidence = folder / f'trial-{i+1:02d}' / 'mod-interaction-checks.json'
            trial = json.loads(evidence.read_text())
            assert set(trial['selected_case_ids']) == set(record['cases']), name
            trials.append(trial)
        target = quiet if record.get('quiet_capture') else traced
        target.extend(trials)
        runs.append({'run': name, 'target': record['target'], 'quiet_capture': record.get('quiet_capture', False),
                     'trace_logging': record['trace_logging'], 'profile': record['profile'],
                     'result': measure(trials), 'record': str((folder / 'run.json').relative_to(ROOT))})
    before = json.loads((ROOT / 'output/blocked-setup-recovery/shipping-subjects-before.json').read_text())
    integrity = [{'file': file, 'sha256': hashlib.sha256((ROOT / file).read_bytes()).hexdigest(),
                  'expected': sha} for file, sha in before.items()]
    assert len(integrity) == 24 and all(r['sha256'] == r['expected'] for r in integrity)
    historical = []
    for profile, case, failed_check in [
        ('build/remaining-ui-audit/gameplay/1.20.1-fabric-siu-project-everything-retries-02-j32-3', 'appbot-portable-mana-cell', 'round trip returns items once'),
        ('build/remaining-ui-audit/gameplay/1.20.1-fabric-siu-project-everything-retries-02-j32-3', 'megacells-portable-mana-cell', 'additional native input accepts tagged stack'),
        ('build/all-target-rollout/gameplay/26.1.2-neoforge-project-everything-priority-11-j3-1', 'atlas-merequester-wireless-terminal', 'two-menu round trip conserves one shared cursor and all physical items'),
    ]:
        folder = ROOT / profile / 'regression-failures' / case
        state = json.loads((folder / 'regression-state.json').read_text())
        pointers = folder / 'regression-click-inputs.json'
        historical.append({'case': case, 'failed_check': failed_check,
                           'evidence': str(folder.relative_to(ROOT)), 'cursor': state['cursor'],
                           'occupied_player_cells': [s for s in state['inventory'] if s['count']],
                           'last_click': json.loads(pointers.read_text())[-1] if pointers.exists() else None,
                           'finding': 'The tagged card remains intact. Legacy captures show the intended slot hit. No original server rejection reason was captured, so stale-state rejection remains a hypothesis.'})
    result = {'generated_at': datetime.now(timezone.utc).isoformat(), 'runs': runs,
              'all_trials': measure(quiet + traced), 'quiet_functional_trials': measure(quiet),
              'traced_diagnostic_trials': measure(traced), 'historical_failures': historical,
              'unchanged_shipping_jars': integrity,
              'focused_unit_checks': {'single_click_cadence': 5, 'failure_accounting': 3},
              'conclusion': 'No causal fix is claimed. Original failed clicks remain unresolved unless reproduced with a captured rejection path.',
              'work_done': ['Bounded serial independent trials with immutable per-trial results.',
                            'Quiet repeats include the immediately preceding related native-menu workflows.',
                            'Optional diagnostic logging override; normal clicks, native packet paths, server ticks, stale-state guards and assertions remain unchanged.'],
              'limits': ['Focused 1.20.1 Fabric portable mana cells and 26.1.2 NeoForge Requester, with relevant entry context only; no global matrix rerun.',
                         'Trace logging affects timing and is reported separately from quiet functional trials.',
                         'Preceding ghost-only Requester entries retain opening/policy coverage.',
                         'No shipping Core, API, compatibility or publisher binaries changed.']}
    (OUT / 'summary.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps({k: result[k] for k in ['all_trials', 'quiet_functional_trials', 'conclusion']}))


if __name__ == '__main__':
    main()
