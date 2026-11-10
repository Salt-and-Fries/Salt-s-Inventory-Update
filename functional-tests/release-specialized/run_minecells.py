"""Serial exact-publisher Mine Cells crafter feature acceptance."""
from pathlib import Path
import argparse, datetime, hashlib, importlib.util, json, msvcrt, subprocess, sys, traceback

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'functional-tests/project-everything-regression'))
from run_mod_matrix import FIXTURES
from subject_artifacts import current_subjects, subjects_match
from minecells_features import run as minecells
from minecells_observer import INPUTS
from native_neo import source_inputs as native_inputs


def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', required=True)
    parser.add_argument('--target', choices=['1.20.1/fabric', '1.21.1/fabric'], required=True)
    parser.add_argument('--plan-only', action='store_true'); args = parser.parse_args()
    assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.run)
    candidates = []
    for i, name in enumerate(FIXTURES[args.target]):
        manifest = ROOT / 'build/all-target-rollout/fixtures' / name / 'fixture.json'
        if any(v['filename'].lower().startswith('minecells-') for v in json.loads(manifest.read_text())['artifacts']):
            candidates.append((i, name, manifest))
    assert candidates
    cohort, fixture, manifest = candidates[0]
    mc, loader = args.target.split('/')
    if args.plan_only:
        print(json.dumps(dict(target=args.target, fixture=fixture, cohort=cohort,
            menus=['cell_crafter', 'unbreakable_cell_crafter'], scope='Two native crafting routes, not full-cohort acceptance'), indent=2)); return
    out = ROOT / 'output/release-readiness' / args.run; assert not out.exists(); out.mkdir()
    report = ROOT / 'output/project-everything-regression' / (args.run + '-production-mod-matrix.json')
    assert not report.exists()
    helpers = [Path(__file__).with_name(v) for v in
        ['run_minecells.py', 'minecells_features.py', 'minecells_observer.py', 'prepare_minecells.py']]
    helpers += [p for p in (ROOT / 'functional-tests/project-everything-regression').iterdir()
                if p.is_file() and p.suffix in ['.py', '.txt', '.inc', '.java', '.gradle']]
    helpers += [*INPUTS, ROOT / 'functional-tests/minecells-implemented/check.py',
                ROOT / 'functional-tests/project-everything-ten/send.py', manifest, *native_inputs()]
    fingerprints = {str(p.relative_to(ROOT)): sha(p) for p in helpers}
    record = dict(status='running', target=args.target, fixture=fixture, subjects=current_subjects(args.target),
                  qa_inputs=fingerprints, scope='Retained native Mine Cells crafter actions on final subjects; no complete-cohort claim',
                  started_at=datetime.datetime.now(datetime.timezone.utc).isoformat())
    def save(): (out / 'summary.json').write_text(json.dumps(record, indent=2) + '\n')
    save()
    with (ROOT / 'build/project-everything-regression/runtime.lock').open('a+b') as lock:
        lock.seek(0); lock.write(b'0'); lock.flush(); lock.seek(0); msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
        try:
            command = [sys.executable, str(Path(__file__).with_name('prepare_minecells.py')),
                       '--minecraft', mc, '--loader', loader, '--fixture', str(manifest),
                       '--run', args.run + '-' + str(cohort)]
            record['prepare_command'] = command; save()
            with (out / 'prepare.log').open('x') as stream:
                subprocess.run(command, cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT, check=True)
            spec = importlib.util.spec_from_file_location('minecells_release_launcher',
                ROOT / 'functional-tests/project-everything-regression/run_production_matrix.py')
            launcher = importlib.util.module_from_spec(spec); spec.loader.exec_module(launcher)
            launcher.check = minecells
            previous = sys.argv
            sys.argv = ['run_production_matrix.py', '--run', args.run, '--target', args.target,
                        '--cohort', str(cohort), '--prepared']
            try: launcher.main()
            finally: sys.argv = previous
            results = json.loads(report.read_text()); assert len(results) == 1
            result = results[0]; assert result['status'] == 'passed' and result['exit_code'] == 0
            assert result['results']['cases'] == 2 and result['results']['minecells_features_checked']
            profile = ROOT / result['profile']
            assert subjects_match(json.loads((profile / 'staged-artifacts.json').read_text())['artifacts'], current_subjects(args.target))
            journal = profile / 'minecells-release/focused-checks.json'; data = json.loads(journal.read_text())
            assert data['complete'] and all(v['passed'] for v in data['checks'])
            assert all(sha(ROOT / p) == h for p, h in fingerprints.items())
            assert current_subjects(args.target) == record['subjects']
            record.update(status='passed', results=results, acceptance=dict(file=str(journal.relative_to(ROOT)), sha256=sha(journal)))
        except BaseException as error:
            record.update(status='failed', error=repr(error), trace=traceback.format_exc()); raise
        finally:
            record['finished_at'] = datetime.datetime.now(datetime.timezone.utc).isoformat(); save()
            lock.seek(0); msvcrt.locking(lock.fileno(), msvcrt.LK_UNLCK, 1)


if __name__ == '__main__': main()
