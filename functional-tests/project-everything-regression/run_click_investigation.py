"""Bounded serial AE2 click reproductions; every failed trial stays failed."""
from pathlib import Path
import argparse, contextlib, datetime, hashlib, json, msvcrt, os, shutil, subprocess, sys, time

ROOT = Path(__file__).resolve().parents[2]
from check_mod_interactions import run as check
from run_mod_matrix import finish
from display_fixture import configure


def configure_source_visibility(runtime, output):
    """Expose the held source through an ordinary user preference for click QA."""
    config = Path(runtime) / 'config/salts_inventory_update/config.json'
    config.parent.mkdir(parents=True, exist_ok=True)
    old = config.read_bytes() if config.exists() else b''
    settings = json.loads(old) if old else {}
    settings['returnHotbarToInventory'] = True
    config.write_text(json.dumps(settings, indent=2) + '\n')
    proof = {'scope': 'Disposable QA user configuration; no production code or menu state changes.',
             'setting': {'returnHotbarToInventory': True},
             'reason': 'The lifecycle probe clicks the held source through its real visible hotbar slot. Default detached-hotbar behavior is covered separately by Core qualification.',
             'before_sha256': hashlib.sha256(old).hexdigest(),
             'after_sha256': hashlib.sha256(config.read_bytes()).hexdigest()}
    (output / 'qa-source-visibility.json').write_text(json.dumps(proof, indent=2) + '\n')


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--run', required=True)
    p.add_argument('--target', choices=['1.20.1/fabric','1.20.1/forge','1.21.1/neoforge','26.1.2/neoforge','26.2/neoforge','26.3/neoforge'], required=True)
    p.add_argument('--repeat', type=int, default=1)
    p.add_argument('--trace', action='store_true')
    p.add_argument('--quiet', action='store_true', help='Disable diagnostic capture and per-frame logs')
    p.add_argument('--context', action='store_true', help='Include the immediately preceding related menu workflows')
    p.add_argument('--probe', action='store_true', help='Use excluded, bounded click/authorization instrumentation')
    p.add_argument('--timing', choices=['normal','fast','varied'], default='normal')
    p.add_argument('--latency',type=int,default=0,help='Client outbound native-packet delay in ms')
    p.add_argument('--jitter',type=int,default=0,help='Deterministic +/- latency variation in ms')
    p.add_argument('--core-candidate',type=Path,help='Use a disclosed class-scoped candidate only in this disposable runtime')
    p.add_argument('--compat-candidate',type=Path,help='Use a disclosed class-scoped compatibility candidate in this disposable runtime')
    p.add_argument('--require-portable-lock',action='store_true',help='Also verify native host registration and a refused source pickup')
    a = p.parse_args()
    assert 1 <= a.repeat <= 12
    assert 0<=a.latency<=200 and 0<=a.jitter<=100
    assert not (a.latency or a.jitter) or a.probe,'A delivery/decision fence requires the excluded probe'
    assert a.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in a.run)
    mc, loader = a.target.split('/')
    legacy = mc == '1.20.1'
    fixture = {'1.20.1/fabric':'1201-fabric-create-ae2-v1','1.20.1/forge':'1201-forge-create-ae2-v1',
        '1.21.1/neoforge':'regression-recent-06-1211-neoforge','26.1.2/neoforge':'regression-recent-07-2612-neoforge',
        '26.2/neoforge':'262-neoforge-inventory-tail-v1','26.3/neoforge':'263-neoforge-inventory-batch'}[a.target]
    cases = ['appbot-portable-mana-cell', 'megacells-portable-mana-cell'] if legacy else (
        ['ae2-wireless-crafting-terminal'] if mc in ['26.2','26.3'] else ['atlas-merequester-wireless-terminal'])
    portable_cases=list(cases)
    if a.context:
        cases = (['flower-pouch', 'bauble-box'] if legacy else ['registry-merequester-requester']) + cases
    profile = ROOT / ('build/remaining-ui-audit/gameplay' if legacy else 'build/all-target-rollout/gameplay') / (
        f'{mc}-{loader}-siu-{a.run}' if legacy else f'{mc}-{loader}-{a.run}')
    out = ROOT / 'output/ae2-click-investigation' / a.run
    assert not out.exists() and not profile.exists(), 'Preserve evidence; choose a new run ID'
    out.mkdir(parents=True)
    record = {'target': a.target, 'fixture': fixture, 'profile': str(profile.relative_to(ROOT)),
              'cases': cases, 'planned_trials': a.repeat, 'trace_logging': a.trace, 'quiet_capture': a.quiet,
              'click_probe':a.probe,'timing':a.timing,
              'latency_ms':a.latency,'jitter_ms':a.jitter,
              'core_candidate':str(a.core_candidate) if a.core_candidate else None,
              'compat_candidate':str(a.compat_candidate) if a.compat_candidate else None,
              'require_portable_lock':a.require_portable_lock,
              'scope': 'Focused independent trials; no failed click is retried, and a later pass cannot erase a failed trial.',
              'started_at': datetime.datetime.now(datetime.timezone.utc).isoformat(), 'trials': []}
    def save():
        (out / 'run.json').write_text(json.dumps(record, indent=2) + '\n')
    save()
    with (ROOT / 'build/project-everything-regression/runtime.lock').open('a+b') as lock:
        lock.seek(0); lock.write(b'0'); lock.flush(); lock.seek(0)
        deadline = time.monotonic() + 3600
        while True:
            try:
                lock.seek(0); msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1); break
            except OSError:
                if time.monotonic() > deadline: raise TimeoutError('Serial runtime remains occupied')
                time.sleep(2)
        try:
            if legacy:
                with (out / 'prepare.log').open('w') as log:
                    subprocess.run([sys.executable, str(Path(__file__).with_name('prepare_production.py')),
                        '--minecraft', mc, '--loader',loader,'--fixture', str(ROOT / 'build/all-target-rollout/fixtures' / fixture / 'fixture.json'),
                        '--run', a.run], cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, check=True)
                launch = json.loads((profile / 'launch.json').read_text())
                if a.core_candidate or a.compat_candidate:
                    staged_path=profile/'staged-artifacts.json';staged=json.loads(staged_path.read_text())
                    for family,candidate in [('core',a.core_candidate),('compatibility',a.compat_candidate)]:
                        if not candidate: continue
                        subject=next(r for r in staged['artifacts'] if r.get('family')==family)
                        assert candidate.name==subject['filename']
                        shutil.copy2(candidate,Path(launch['cwd'])/'mods'/subject['filename'])
                        subject.update(file=str(candidate.resolve()),sha256=hashlib.sha256(candidate.read_bytes()).hexdigest(),
                            sha512=hashlib.sha512(candidate.read_bytes()).hexdigest(),scope='Class-scoped candidate; shipping jars unchanged')
                    staged_path.write_text(json.dumps(staged,indent=2)+'\n')
                configure(Path(launch['cwd']), profile)
                if a.require_portable_lock:
                    configure_source_visibility(launch['cwd'], out)
                probe=[]
                if a.probe:
                    from click_probe import prepare
                    probe=[prepare(out,launch['java'])]
                args = [launch['java']] + probe + (['-Dsalts_inventory_update.desktopTrace=true'] if a.trace else []) + ['@' + launch['arguments']]
                cwd, env = launch['cwd'], {**os.environ, **launch['environment']}
            else:
                probe=[]
                if a.probe:
                    from click_probe import prepare
                    probe=['-PqaClickProbeAgent='+prepare(out,'C:/Program Files/Java/jdk-26/bin/java.exe')]
                args = [str(ROOT / 'gradlew.bat'), f':{mc}:{loader}:runNativePortQa', '-I', 'functional-tests/project-everything-regression/core.init.gradle',
                    f'-PqaMinecraft={mc}', f'-PqaLoader={loader}', '-PqaFullGameplay=true', '-PqaScenario=extension-empty',
                    f'-PqaRunId={a.run}', '-PqaRuntimeRoot=C:/Users/texca/.cache/siu-regression-runtime',
                    '-PcompatibilityQaManifest=' + str(Path('build/all-target-rollout/fixtures') / fixture / 'fixture.json'),
                    '-PqaTrace=' + str(a.trace).lower(), *probe,
                    *(['-PqaCoreArtifact='+str(a.core_candidate.resolve())] if a.core_candidate else []), '--console=plain', '--max-workers=3']
                if a.compat_candidate: args.insert(-2,'-PqaCompatArtifact='+str(a.compat_candidate.resolve()))
                cwd, env = ROOT, {**os.environ, 'JAVA_HOME': 'C:/Program Files/Java/jdk-26'}
                if a.require_portable_lock:
                    configure_source_visibility(Path('C:/Users/texca/.cache/siu-regression-runtime') / f'{mc}-{loader}-{a.run}', out)
            with (out / 'launch.log').open('w') as log:
                proc = subprocess.Popen(args, cwd=cwd, env=env, stdout=log, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
                record['pid'] = proc.pid; save()
                try:
                    end = time.monotonic() + 900
                    while not (profile / 'ready.json').exists():
                        if proc.poll() is not None: raise RuntimeError('Client exited ' + str(proc.returncode))
                        if time.monotonic() > end: raise TimeoutError('Client readiness timed out')
                        time.sleep(1)
                    # The Gradle launch configures display options before starting.
                    record['subjects'] = json.loads((profile / 'staged-artifacts.json').read_text())['artifacts']
                    if a.latency or a.jitter:
                        from evidence import send
                        send(profile,'regression-network-delay',millis=a.latency,jitter=a.jitter,ticks=2)
                    for i in range(a.repeat):
                        trial = out / f'trial-{i+1:02d}'; trial.mkdir()
                        print('TRIAL', a.run, i+1, flush=True)
                        with (trial / 'checks.log').open('w') as stream, contextlib.redirect_stdout(stream):
                            result = check(profile, mc, loader, only=cases, capture_debug=False if a.quiet else None,
                                click_ticks={'normal':(),'fast':(1,),'varied':(1,2,4,1,6,3,2,5)}[a.timing],
                                network_fence=bool(a.latency or a.jitter),require_portable_lock=a.require_portable_lock)
                        if a.require_portable_lock:
                            proof=json.loads((profile/'mod-interaction-checks.json').read_text())
                            completed={row['id'] for row in proof['cases'] if row.get('portable_source_lifecycle_checked')}
                            result['portable_lifecycle_missing']=sorted(set(portable_cases)-completed)
                        for name in ['mod-interaction-checks.json', 'regression-state.json', 'regression-click-inputs.json', 'regression-mutation-state.json']:
                            if (profile / name).exists(): shutil.copy2(profile / name, trial / name)
                        failures = profile / 'regression-failures'
                        if failures.exists():
                            shutil.copytree(failures, trial / 'regression-failures')
                            assert failures.resolve().is_relative_to((ROOT / 'build').resolve())
                            shutil.rmtree(failures)
                        record['trials'].append(result); save()
                        print('RESULT', result, flush=True)
                finally:
                    finish(profile)
                    try: record['exit_code'] = proc.wait(timeout=120)
                    except subprocess.TimeoutExpired:
                        subprocess.run(['taskkill', '/PID', str(proc.pid), '/T', '/F'], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                        record['exit_code'] = 'terminated-owned-client-after-timeout'
        finally:
            record['finished_at'] = datetime.datetime.now(datetime.timezone.utc).isoformat(); save()
            lock.seek(0); msvcrt.locking(lock.fileno(), msvcrt.LK_UNLCK, 1)
    assert record.get('exit_code') == 0 and len(record['trials']) == a.repeat
    if any(t['failed'] or t['entry_only'] or t['passed']!=t['cases'] or t['cases']!=len(cases) or t.get('portable_lifecycle_missing')
           for t in record['trials']): raise SystemExit(1)


if __name__ == '__main__':
    main()
