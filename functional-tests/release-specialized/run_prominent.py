"""Serial current-artifact Prominent forge, armor and shop replay."""
from pathlib import Path
import argparse, datetime, hashlib, importlib.util, json, msvcrt, subprocess, sys, traceback

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'functional-tests/project-everything-regression'))
from run_mod_matrix import FIXTURES
from subject_artifacts import current_subjects, subjects_match
from prominent_features import run as prominent
from prominent_observer import INPUTS
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
        if any(v['filename'].lower().startswith('prominent') for v in json.loads(path.read_text())['artifacts']):
            candidates.append((i, name, path))
    assert len(candidates) == 1
    cohort, fixture, manifest = candidates[0]
    if args.plan_only:
        print(json.dumps(dict(target=args.target, fixture=fixture, cohort=cohort,
            groups=['upgrade', 'armor', 'visuals', 'shop'], scope='Three native menus and four named feature groups; no complete-cohort claim'), indent=2)); return
    out = ROOT / 'output/release-readiness' / args.run; assert not out.exists(); out.mkdir()
    report = ROOT / 'output/project-everything-regression' / (args.run + '-production-mod-matrix.json'); assert not report.exists()
    helpers = [Path(__file__).with_name(v) for v in ['run_prominent.py', 'prominent_features.py',
               'prominent_observer.py', 'prepare_prominent.py', 'minecells_observer.py']]
    helpers += [p for p in (ROOT / 'functional-tests/project-everything-regression').iterdir()
                if p.is_file() and p.suffix in ['.py', '.txt', '.inc', '.java', '.gradle']]
    helpers += [*INPUTS, ROOT / 'functional-tests/prominent-forge/check.py', ROOT / 'functional-tests/prominent-shop/check.py',
                ROOT / 'functional-tests/project-everything-ten/send.py', manifest, *native_inputs()]
    fingerprints = {str(p.relative_to(ROOT)): sha(p) for p in helpers}
    record = dict(status='running', target=args.target, fixture=fixture, subjects=current_subjects(args.target),
                  qa_inputs=fingerprints, scope='Native Prominent upgrade, armor and shop actions only',
                  started_at=datetime.datetime.now(datetime.timezone.utc).isoformat())
    def save(): (out / 'summary.json').write_text(json.dumps(record, indent=2) + '\n')
    save()
    with (ROOT / 'build/project-everything-regression/runtime.lock').open('a+b') as lock:
        lock.seek(0); lock.write(b'0'); lock.flush(); lock.seek(0); msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
        try:
            command = [sys.executable, str(Path(__file__).with_name('prepare_prominent.py')), '--minecraft', '1.20.1',
                       '--loader', 'fabric', '--fixture', str(manifest), '--run', args.run + '-' + str(cohort)]
            record['prepare_command'] = command; save()
            with (out / 'prepare.log').open('x') as stream:
                subprocess.run(command, cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT, check=True)
            spec = importlib.util.spec_from_file_location('prominent_release_launcher',
                ROOT / 'functional-tests/project-everything-regression/run_production_matrix.py')
            launcher = importlib.util.module_from_spec(spec); spec.loader.exec_module(launcher); launcher.check = prominent
            previous = sys.argv
            sys.argv = ['run_production_matrix.py', '--run', args.run, '--target', args.target, '--cohort', str(cohort), '--prepared']
            try: launcher.main()
            finally: sys.argv = previous
            results = json.loads(report.read_text()); assert len(results) == 1
            result = results[0]; assert result['status'] == 'passed' and result['exit_code'] == 0
            assert result['results']['cases'] == 3 and result['results']['prominent_features_checked']
            profile = ROOT / result['profile']
            assert subjects_match(json.loads((profile / 'staged-artifacts.json').read_text())['artifacts'], current_subjects(args.target))
            journal = profile / 'prominent-release/summary.json'; data = json.loads(journal.read_text())
            assert data['complete'] and [v['group'] for v in data['groups']] == ['upgrade', 'armor', 'visuals', 'shop']
            for group in data['groups']:
                source = ROOT / group['journal']; assert sha(source) == group['sha256']
                assert all(v['passed'] for v in json.loads(source.read_text()))
            assert all(sha(ROOT / p) == h for p, h in fingerprints.items())
            assert current_subjects(args.target) == record['subjects']
            record.update(status='passed', results=results, acceptance=dict(file=str(journal.relative_to(ROOT)), sha256=sha(journal)))
        except BaseException as error:
            record.update(status='failed', error=repr(error), trace=traceback.format_exc()); raise
        finally:
            record['finished_at'] = datetime.datetime.now(datetime.timezone.utc).isoformat(); save()
            lock.seek(0); msvcrt.locking(lock.fileno(), msvcrt.LK_UNLCK, 1)


if __name__ == '__main__': main()
