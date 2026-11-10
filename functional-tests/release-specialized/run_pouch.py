"""Current-artifact native Archon XP-pouch resource accounting acceptance."""
from pathlib import Path
import argparse, datetime, hashlib, importlib.util, json, msvcrt, subprocess, sys, traceback

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'functional-tests/project-everything-regression'))
from run_mod_matrix import FIXTURES
from subject_artifacts import current_subjects, subjects_match
from pouch_features import run as pouch
from native_neo import source_inputs as native_inputs


def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', required=True)
    parser.add_argument('--target', choices=['1.20.1/fabric'], required=True)
    parser.add_argument('--plan-only', action='store_true'); args = parser.parse_args()
    assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.run)
    candidates = []
    for i, name in enumerate(FIXTURES[args.target]):
        path = ROOT / 'build/all-target-rollout/fixtures' / name / 'fixture.json'
        if any(v['filename'].lower().startswith('archon') for v in json.loads(path.read_text())['artifacts']):
            candidates.append((i, name, path))
    assert candidates
    cohort, fixture, manifest = candidates[0]
    if args.plan_only:
        print(json.dumps(dict(target=args.target, fixture=fixture, cohort=cohort,
            items=['experience_pouch', 'super_experience_pouch'], scope='Six native buttons on two pouch variants; no complete-cohort claim'), indent=2)); return
    out = ROOT / 'output/release-readiness' / args.run; assert not out.exists(); out.mkdir()
    report = ROOT / 'output/project-everything-regression' / (args.run + '-production-mod-matrix.json'); assert not report.exists()
    helpers = [Path(__file__).with_name(v) for v in ['run_pouch.py', 'pouch_features.py', 'pouch_observer.py', 'prepare_pouch.py']]
    helpers += [p for p in (ROOT / 'functional-tests/project-everything-regression').iterdir()
                if p.is_file() and p.suffix in ['.py', '.txt', '.inc', '.java', '.gradle']]
    helpers += [ROOT / 'functional-tests/project-everything-ten/send.py', manifest, *native_inputs()]
    fingerprints = {str(p.relative_to(ROOT)): sha(p) for p in helpers}
    record = dict(status='running', target=args.target, fixture=fixture, subjects=current_subjects(args.target),
                  qa_inputs=fingerprints, scope='Two native XP pouches, real deposit/withdraw controls, refusal and metadata/XP conservation',
                  started_at=datetime.datetime.now(datetime.timezone.utc).isoformat())
    def save(): (out / 'summary.json').write_text(json.dumps(record, indent=2) + '\n')
    save()
    with (ROOT / 'build/project-everything-regression/runtime.lock').open('a+b') as lock:
        lock.seek(0); lock.write(b'0'); lock.flush(); lock.seek(0); msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
        try:
            command = [sys.executable, str(Path(__file__).with_name('prepare_pouch.py')), '--minecraft', '1.20.1',
                       '--loader', 'fabric', '--fixture', str(manifest), '--run', args.run + '-' + str(cohort)]
            record['prepare_command'] = command; save()
            with (out / 'prepare.log').open('x') as stream:
                subprocess.run(command, cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT, check=True)
            spec = importlib.util.spec_from_file_location('pouch_release_launcher',
                ROOT / 'functional-tests/project-everything-regression/run_production_matrix.py')
            launcher = importlib.util.module_from_spec(spec); spec.loader.exec_module(launcher); launcher.check = pouch
            previous = sys.argv
            sys.argv = ['run_production_matrix.py', '--run', args.run, '--target', args.target, '--cohort', str(cohort), '--prepared']
            try: launcher.main()
            finally: sys.argv = previous
            results = json.loads(report.read_text()); assert len(results) == 1
            result = results[0]; assert result['status'] == 'passed' and result['exit_code'] == 0
            assert result['results']['cases'] == 2 and result['results']['pouch_features_checked']
            profile = ROOT / result['profile']
            assert subjects_match(json.loads((profile / 'staged-artifacts.json').read_text())['artifacts'], current_subjects(args.target))
            journal = profile / 'pouch-release/summary.json'; data = json.loads(journal.read_text())
            assert data['complete'] and data['variants'] == ['archon:experience_pouch', 'archon:super_experience_pouch']
            assert len(data['checks']) >= 50 and all(v['passed'] for v in data['checks'])
            assert all(sha(ROOT / p) == h for p, h in fingerprints.items())
            assert current_subjects(args.target) == record['subjects']
            record.update(status='passed', results=results, acceptance=dict(file=str(journal.relative_to(ROOT)), sha256=sha(journal)))
        except BaseException as error:
            record.update(status='failed', error=repr(error), trace=traceback.format_exc()); raise
        finally:
            record['finished_at'] = datetime.datetime.now(datetime.timezone.utc).isoformat(); save()
            lock.seek(0); msvcrt.locking(lock.fileno(), msvcrt.LK_UNLCK, 1)


if __name__ == '__main__': main()
