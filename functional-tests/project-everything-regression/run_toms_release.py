"""Serial native Tom-family qualification on untouched production artifacts."""
from pathlib import Path
import argparse, datetime, hashlib, json, msvcrt, os, subprocess, sys, time

ROOT = Path(__file__).resolve().parents[2]
from display_fixture import configure
from run_mod_matrix import finish


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', required=True)
    parser.add_argument('--only', choices=['vending','configurator','link','terminal'], required=True)
    parser.add_argument('--prepared', action='store_true', help='Launch a fresh, already compiled disposable observer profile')
    args = parser.parse_args()
    assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.run)
    fixture = ROOT / 'build/blocked-setup-recovery/fixtures/polymer-without-bumblezone.json'
    profile = ROOT / 'build/remaining-ui-audit/gameplay' / ('26.1.2-fabric-siu-' + args.run)
    output = ROOT / 'output/release-readiness' / args.run
    assert profile.exists()==args.prepared and not output.exists(), 'Preserve evidence; choose a fresh run ID'
    if args.prepared:assert not (profile/'ready.json').exists() and not (profile/'reply.json').exists(), 'Prepared profile must never have run'
    output.mkdir(parents=True)
    record = dict(status='running', target='26.1.2/fabric', workflow=args.only,
                  fixture=str(fixture.relative_to(ROOT)), profile=str(profile.relative_to(ROOT)),
                  started_at=datetime.datetime.now(datetime.timezone.utc).isoformat())
    def save(): (output / 'summary.json').write_text(json.dumps(record, indent=2) + '\n')
    save()
    with (ROOT / 'build/project-everything-regression/runtime.lock').open('a+b') as lock:
        lock.seek(0); lock.write(b'0'); lock.flush(); lock.seek(0)
        msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
        try:
            if not args.prepared:
                with (output / 'prepare.log').open('w') as log:
                    subprocess.run([sys.executable, str(Path(__file__).with_name('prepare_production.py')),
                                    '--minecraft', '26.1.2', '--fixture', str(fixture), '--run', args.run,'--release-toms'],
                                   cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, check=True)
            launch = json.loads((profile / 'launch.json').read_text())
            record['subjects'] = json.loads((profile / 'staged-artifacts.json').read_text())['artifacts']
            for subject in record['subjects']:
                if subject['family'] not in ['core','compatibility']:continue
                assert hashlib.sha256(Path(subject['file']).read_bytes()).hexdigest()==subject['sha256'], 'Prepared production subject changed'
                assert hashlib.sha256((Path(launch['cwd'])/'mods'/subject['filename']).read_bytes()).hexdigest()==subject['sha256'], 'Staged production subject changed'
            roots = {r['filename'] for r in json.loads(fixture.read_text())['artifacts']
                     if r.get('coordinate') or r.get('stage_as_root', False)}
            assert roots <= {r['filename'] for r in record['subjects']}, 'Every publisher dependency root must be staged'
            record['qa_inputs'] = {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest()
                                   for p in [Path(__file__), Path(__file__).with_name('toms_release_workflows.py'),
                                             Path(__file__).with_name('observer-fragment.java.txt'),
                                             Path(__file__).with_name('release-toms-observer.java.inc'),
                                             Path(__file__).with_name('prepare_production.py'), fixture]}
            configure(Path(launch['cwd']), profile)
            with (output / 'launch.log').open('w') as log:
                proc = subprocess.Popen([launch['java'], '@' + launch['arguments']], cwd=launch['cwd'],
                                        env={**os.environ, **launch['environment']}, stdout=log,
                                        stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
                record['pid'] = proc.pid; save()
                try:
                    deadline = time.monotonic() + 900
                    while not (profile / 'ready.json').exists():
                        if proc.poll() is not None: raise RuntimeError('Client exited ' + str(proc.returncode))
                        if time.monotonic() > deadline: raise TimeoutError('Native client readiness')
                        time.sleep(1)
                    with (output / 'checks.log').open('w') as checks:
                        result = subprocess.run([sys.executable, str(Path(__file__).with_name('toms_release_workflows.py')),
                                                 str(profile), '--only', args.only], cwd=ROOT,
                                                stdout=checks, stderr=subprocess.STDOUT)
                    record['checks_exit_code'] = result.returncode; save()
                    assert result.returncode == 0, 'Native Tom workflow failed; inspect preserved checks'
                finally:
                    finish(profile)
                    try: record['exit_code'] = proc.wait(timeout=120)
                    except subprocess.TimeoutExpired:
                        subprocess.run(['taskkill', '/PID', str(proc.pid), '/T', '/F'], stdout=subprocess.DEVNULL,
                                       stderr=subprocess.DEVNULL)
                        record['exit_code'] = 'terminated-owned-client-after-timeout'
            assert record['exit_code'] == 0
            for relative, fingerprint in record['qa_inputs'].items():
                assert hashlib.sha256((ROOT / relative).read_bytes()).hexdigest() == fingerprint, 'QA input changed during workflow'
            record['status'] = 'passed'
        except Exception as error:
            record.update(status='failed', error=repr(error)); raise
        finally:
            record['finished_at'] = datetime.datetime.now(datetime.timezone.utc).isoformat(); save()
            lock.seek(0); msvcrt.locking(lock.fileno(), msvcrt.LK_UNLCK, 1)


if __name__ == '__main__': main()
