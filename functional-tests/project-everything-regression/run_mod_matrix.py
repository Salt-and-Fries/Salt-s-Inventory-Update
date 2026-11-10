"""Supervise mixed-mod regression clients, one at a time, preserving failures."""
from pathlib import Path
import argparse, datetime, json, os, subprocess, sys, time, traceback
from check_mod_interactions import run as check
ROOT = Path(__file__).resolve().parents[2]
FIXTURES = {
    '1.20.1/fabric': ['1201-fabric-inventory-batch-v2', '1201-fabric-specialized-v1', '1201-fabric-themed-v1', '1201-fabric-create-ae2-v1'],
    '1.20.1/forge': ['1201-forge-inventory-batch-v2', '1201-forge-themed-v1', '1201-forge-create-ae2-v1'],
    '1.21.1/fabric': ['1211-fabric-inventory-batch-v1', '1211-fabric-specialized-v3', '1211-fabric-themed-v1'],
    '1.21.1/neoforge': ['1211-neoforge-inventory-batch-v1', '1211-neoforge-themed-v3'],
    '1.21.11/fabric': ['12111-fabric-inventory-batch-no-guard-v3'],
    '1.21.11/neoforge': ['12111-neoforge-inventory-batch-v2'],
    '26.1.2/fabric': ['2612-fabric-inventory-batch-no-bumblezone-v2', '2612-fabric-inventory-tail-v1', '2612-fabric-native-machines'],
    '26.1.2/neoforge': ['2612-neoforge-inventory-tail-v1'],
    '26.2/fabric': ['262-fabric-inventory-tail-v1'],
    '26.2/neoforge': ['262-neoforge-inventory-tail-v1'],
    '26.3/fabric': ['263-fabric-inventory-tail-v2', '263-fabric-actions'],
    '26.3/neoforge': ['263-neoforge-inventory-batch'],
}
for extra in json.loads((ROOT / 'output/project-everything-regression/additional-fixtures.json').read_text()):
    cohorts = FIXTURES.setdefault(extra['target'], [])
    if extra['fixture'] not in cohorts: cohorts.append(extra['fixture'])
fill=ROOT/'output/project-everything-regression/coverage-fill-fixtures.json'
if fill.exists():
    for extra in json.loads(fill.read_text()):
        cohorts=FIXTURES.setdefault(extra['target'],[])
        if extra['fixture'] not in cohorts:cohorts.append(extra['fixture'])

def finish(profile):
    if not (profile / 'ready.json').exists(): return
    seq = max((json.loads(p.read_text()).get('sequence',0) for p in [profile/'reply.json',profile/'command.json'] if p.exists()),default=0)+1
    temp = profile / 'finish.tmp'; temp.write_text(json.dumps({'sequence': seq, 'type': 'finish', 'ticks': 2})); temp.replace(profile / 'command.json')

def main():
    p = argparse.ArgumentParser(); p.add_argument('--run', required=True); p.add_argument('--target', action='append'); p.add_argument('--cohort', type=int); p.add_argument('--only', action='append', default=[]);p.add_argument('--max-cases',type=int);p.add_argument('--mod',action='append',default=[]); a = p.parse_args()
    out = ROOT / 'output/project-everything-regression'; out.mkdir(exist_ok=True)
    rows = []; report = out / (a.run + '-mod-matrix.json')
    def save(): report.write_text(json.dumps(rows, indent=2) + '\n')
    for target, cohorts in FIXTURES.items():
        if a.target and target not in a.target: continue
        mc, loader = target.split('/')
        for i, fixture in enumerate(cohorts):
            if a.cohort is not None and i != a.cohort: continue
            manifest = ROOT / 'build/all-target-rollout/fixtures' / fixture / 'fixture.json'
            if not manifest.exists():
                rows.append({'target': target, 'fixture': fixture, 'status': 'missing-fixture'}); save(); continue
            key = f'{mc}-{loader}-{a.run}-{i}'
            profile = ROOT / 'build/all-target-rollout/gameplay' / key
            log = ROOT / 'build/project-everything-regression' / (key + '.log')
            assert not profile.exists() and not log.exists(), 'Preserve evidence: choose a new run ID'
            row = {'target': target, 'fixture': fixture, 'profile': str(profile.relative_to(ROOT)), 'log': str(log.relative_to(ROOT)), 'status': 'starting'}; rows.append(row); save()
            args = [str(ROOT / 'gradlew.bat'), f':{mc}:{loader}:runNativePortQa', '-I', 'functional-tests/project-everything-regression/core.init.gradle',
                f'-PqaMinecraft={mc}', f'-PqaLoader={loader}', '-PqaFullGameplay=true', '-PqaScenario=extension-empty',
                f'-PqaRunId={a.run}-{i}', '-PqaRuntimeRoot=C:/Users/texca/.cache/siu-regression-runtime',
                '-PcompatibilityQaManifest=' + str(manifest.relative_to(ROOT)), '--console=plain', '--max-workers=3']
            print('START', target, fixture, flush=True); started = time.monotonic()
            with log.open('w') as stream:
                process = subprocess.Popen(args, cwd=ROOT, env={**os.environ, 'JAVA_HOME': 'C:/Program Files/Java/jdk-26'}, stdout=stream, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
                try:
                    deadline = time.monotonic() + 900
                    while not (profile / 'ready.json').exists():
                        if process.poll() is not None: raise RuntimeError('Launch exited ' + str(process.returncode))
                        if time.monotonic() > deadline: raise TimeoutError('No ready marker within 15 minutes')
                        time.sleep(1)
                    row['status'] = 'testing'; save()
                    row['results'] = check(profile, mc, loader, a.only,a.max_cases,a.mod)
                    row['status'] = ('coverage-gap' if not row['results']['cases'] else
                                     'passed' if row['results']['failed'] == 0 else 'failed')
                except Exception as error:
                    row.update(status='failed', error=repr(error), trace=traceback.format_exc())
                finally:
                    finish(profile)
                    try: row['exit_code'] = process.wait(timeout=120)
                    except subprocess.TimeoutExpired:
                        subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                        row['exit_code'] = 'terminated-owned-launch-after-timeout'; row['status'] = 'failed'
                    if row['exit_code'] != 0: row['status'] = 'failed'
            row['seconds'] = round(time.monotonic() - started, 1); save(); print('END', target, fixture, row['status'], flush=True)
    if any(r['status'] != 'passed' for r in rows): raise SystemExit(1)

if __name__ == '__main__': main()
