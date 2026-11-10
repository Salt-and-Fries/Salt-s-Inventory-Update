"""Replay the retained ship-page workflow on exact shipping subjects, serially."""
from pathlib import Path
import argparse, datetime, hashlib, importlib.util, json, msvcrt, sys, traceback

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'functional-tests/project-everything-regression'))
from run_mod_matrix import FIXTURES
from run_release_publishers import PARTITIONS
from subject_artifacts import current_subjects, subjects_match
from run_toms_all import named
from native_neo import source_inputs as native_inputs
from smallships_paging_features import ORIGINAL, derive, run as features

TARGETS = ['1.20.1/fabric', '1.20.1/forge', '1.21.1/fabric', '1.21.1/neoforge']


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', required=True)
    parser.add_argument('--target', choices=TARGETS, required=True)
    parser.add_argument('--plan-only', action='store_true')
    args = parser.parse_args()
    assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.run)
    candidates = []
    for cohort, fixture in enumerate(FIXTURES[args.target]):
        manifest = ROOT / 'build/all-target-rollout/fixtures' / fixture / 'fixture.json'
        if any(a['filename'].startswith('smallships') for a in json.loads(manifest.read_text())['artifacts']):
            candidates.append((cohort, fixture, manifest))
    assert candidates
    cohort, fixture, manifest = candidates[0]
    if fixture in PARTITIONS:
        manifest = ROOT / 'build/blocked-setup-recovery/fixtures' / PARTITIONS[fixture]
    publisher = [a for a in json.loads(manifest.read_text())['artifacts'] if a['filename'].startswith('smallships')]
    assert len(publisher) == 1
    if args.plan_only:
        print(json.dumps(dict(target=args.target, fixture=fixture, cohort=cohort,
            manifest=str(manifest.relative_to(ROOT)), publisher=publisher[0],
            scope='All six original Galleon cargo/page/shared-cursor assertions; only the two public ground-height prerequisites translate on 1.21.1.'), indent=2))
        return
    out = ROOT / 'output/release-readiness' / args.run
    assert not out.exists()
    out.mkdir()
    inputs = [Path(__file__), ORIGINAL, Path(__file__).with_name('smallships_paging_features.py'),
              Path(__file__).with_name('run_toms_all.py'), Path(__file__).with_name('toms_all_features.py'),
              Path(__file__).with_name('toms_all_observer.py'), manifest,
              ROOT / 'functional-tests/all-target-rollout/evidence.py',
              ROOT / 'functional-tests/project-everything-ten/send.py', *native_inputs()]
    inputs += [p for p in (ROOT / 'functional-tests/project-everything-regression').iterdir()
               if p.is_file() and p.suffix in {'.py', '.txt', '.inc', '.java', '.gradle'}]
    inputs += list((ROOT / 'functional-tests/src/main/java').rglob('*.java'))
    inputs += list((ROOT / ('functional-tests/all-target-rollout/java-' + args.target.split('/')[0])).rglob('*.java'))
    fingerprints = {str(p.relative_to(ROOT)): sha(p) for p in inputs}
    record = dict(status='running', target=args.target, fixture=fixture, subjects=current_subjects(args.target),
        qa_inputs=fingerprints, original_driver=next(v for k, v in fingerprints.items() if k == str(ORIGINAL.relative_to(ROOT))),
        scope='Retained native Galleon page transition and shared-cursor workflow; not a complete publisher-cohort rerun.',
        started_at=datetime.datetime.now(datetime.timezone.utc).isoformat())

    def save():
        (out / 'summary.json').write_text(json.dumps(record, indent=2) + '\n')

    save()
    with (ROOT / 'build/project-everything-regression/runtime.lock').open('a+b') as lock:
        lock.seek(0)
        lock.write(b'0')
        lock.flush()
        lock.seek(0)
        msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
        try:
            if args.target.endswith('/neoforge'):
                results = named(args.run, args.target, manifest, out, check=features)
            else:
                source = ROOT / 'functional-tests/project-everything-regression/run_production_matrix.py'
                spec = importlib.util.spec_from_file_location('ship_paging_production', source)
                launcher = importlib.util.module_from_spec(spec)
                spec.loader.exec_module(launcher)
                launcher.check = features
                old = sys.argv
                sys.argv = [str(source), '--run', args.run, '--target', args.target, '--cohort', str(cohort)]
                if fixture in PARTITIONS:
                    sys.argv += ['--fixture-manifest', str(manifest)]
                try:
                    launcher.main()
                finally:
                    sys.argv = old
                path = ROOT / 'output/project-everything-regression' / (args.run + '-production-mod-matrix.json')
                results = json.loads(path.read_text())
            assert len(results) == 1
            result = results[0]
            assert result['status'] == 'passed' and result['exit_code'] == 0
            assert result['results']['smallships_paging_checked'] and result['results']['checks'] == 6
            profile = ROOT / result['profile']
            assert subjects_match(json.loads((profile / 'staged-artifacts.json').read_text())['artifacts'], record['subjects'])
            accepted = profile / 'smallships-paging-release/summary.json'
            value = json.loads(accepted.read_text())
            assert value['complete'] and len(value['checks']) == 6 and all(c['passed'] for c in value['checks'])
            derived = ROOT / value['derived']['file']
            assert sha(derived) == value['derived']['sha256']
            assert derived.read_text() == derive(ORIGINAL.read_text(), args.target.split('/')[0])
            result.setdefault('generated_inputs', {})[str(derived.relative_to(ROOT))] = sha(derived)
            original_report = out / 'native-attempt.json' if args.target.endswith('/neoforge') else path
            assert original_report.is_file()
            assert current_subjects(args.target) == record['subjects']
            assert fingerprints == {str(p.relative_to(ROOT)): sha(p) for p in inputs}
            record.update(status='passed', results=results,
                          acceptance=dict(file=str(accepted.relative_to(ROOT)), sha256=sha(accepted)),
                          generated_checker_inputs=[value['derived']],
                          supporting_evidence=[dict(file=str(original_report.relative_to(ROOT)), sha256=sha(original_report))])
        except BaseException as error:
            record.update(status='failed', error=repr(error), trace=traceback.format_exc())
            raise
        finally:
            record['finished_at'] = datetime.datetime.now(datetime.timezone.utc).isoformat()
            save()
            lock.seek(0)
            msvcrt.locking(lock.fileno(), msvcrt.LK_UNLCK, 1)


if __name__ == '__main__':
    main()
