"""Run exact-publisher integration cohorts serially, across every release target."""
from pathlib import Path
import argparse, json, msvcrt, os, subprocess, sys, time
from run_mod_matrix import FIXTURES
ROOT = Path(__file__).resolve().parents[2]

def main():
    p = argparse.ArgumentParser(); p.add_argument('--run', required=True); p.add_argument('--target', action='append'); p.add_argument('--round', type=int);p.add_argument('--max-cases',type=int);p.add_argument('--fixture',action='append'); a = p.parse_args()
    fill=ROOT/'output/project-everything-regression/coverage-fill-fixtures.json'
    mod_selection={r['fixture']:r['mods'] for r in json.loads(fill.read_text())} if fill.exists() else {}
    folder = ROOT / 'build/project-everything-regression'; folder.mkdir(exist_ok=True)
    with (folder / 'runtime.lock').open('a+b') as lock:
        lock.seek(0); lock.write(b'0'); lock.flush(); lock.seek(0)
        msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
        rows = []; out = ROOT / 'output/project-everything-regression' / (a.run + '-integration.json')
        assert not out.exists(), 'Use a new run ID to preserve previous evidence'
        for index in range(max(map(len, FIXTURES.values()))):
            if a.round is not None and index != a.round: continue
            for target, cohorts in FIXTURES.items():
                if index >= len(cohorts) or a.target and target not in a.target: continue
                if a.fixture and cohorts[index] not in a.fixture:continue
                script = 'run_production_matrix.py' if target.endswith(('/fabric','/forge')) else 'run_mod_matrix.py'
                name = a.run + '-c' + str(index)
                log = folder / (name + '-' + target.replace('/', '-') + '.log')
                args = [sys.executable, str(Path(__file__).with_name(script)), '--run', name, '--target', target, '--cohort', str(index)]
                if script.startswith('run_production'):
                    prepared=ROOT/'build/remaining-ui-audit/gameplay'/(target.replace('/','-')+'-siu-'+name+'-'+str(index))
                    if (prepared/'launch.json').exists() and not (prepared/'pid.txt').exists():args.append('--prepared')
                if a.max_cases:args+=['--max-cases',str(a.max_cases)]
                mods=mod_selection.get(cohorts[index],[])
                for mod in mods:args+=['--mod',mod]
                row = {'target': target, 'cohort': index, 'fixture': cohorts[index], 'status': 'running', 'scope':'smoke' if a.max_cases else 'reviewed-mod-selection' if mods else 'complete-discovery-pass','mods':mods,'log': str(log.relative_to(ROOT))}; rows.append(row)
                out.write_text(json.dumps(rows, indent=2)); started = time.monotonic(); print('START', target, index, flush=True)
                with log.open('w') as stream:
                    result = subprocess.run(args, cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT)
                row.update(status='passed' if result.returncode == 0 else 'failed', exit_code=result.returncode, seconds=round(time.monotonic()-started,1))
                suffix = '-production-mod-matrix.json' if script.startswith('run_production') else '-mod-matrix.json'
                matrix = ROOT / 'output/project-everything-regression' / (name + suffix)
                # Per-target drivers share run labels, so retain each immediately.
                if matrix.exists(): row['results'] = json.loads(matrix.read_text())
                out.write_text(json.dumps(rows, indent=2)); print('END', target, index, row['status'], flush=True)
        msvcrt.locking(lock.fileno(), msvcrt.LK_UNLCK, 1)
    if any(r['status'] != 'passed' for r in rows): raise SystemExit(1)

if __name__ == '__main__': main()
