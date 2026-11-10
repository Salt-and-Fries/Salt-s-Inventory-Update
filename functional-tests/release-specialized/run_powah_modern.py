"""Two additional exact-publisher Powah6/7 workflows; leave the original174 intact."""
from pathlib import Path
import argparse, datetime, hashlib, json, msvcrt, sys, traceback

ROOT = Path(__file__).resolve().parents[2]
sys.path[:0] = [str(ROOT / 'tools'), str(ROOT / 'functional-tests/project-everything-regression')]
from run_mod_matrix import FIXTURES
from run_release_publishers import PARTITIONS
from subject_artifacts import current_subjects, subjects_match
from full_suite_provenance import bundle
from run_toms_all import named
from powah_modern_features import run as features, TARGETS, SCOPE
from powah_modern_observer import enhance

PINS = {'1.21.1/neoforge': '41d616d3d944faaee2cde4d15b3fc35cdd154690169f43d8436e6432df50db90355ca5fef78bd0f7864d97d9a14039280c8fb78d8c3e1d407618c7ccff47d97f',
        '26.1.2/neoforge': 'a5d1fcaa70529a0286783a49eac992a5d3cb15fa5e2aee5c7d080e86d3c501b3bce79adcf51b86c019f2119a377cdb20dd4f896c8ae468a30b01d8cdf4bd0d01'}


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def select(target):
    candidates = []
    for cohort, name in enumerate(FIXTURES[target]):
        manifest = ROOT / 'build/all-target-rollout/fixtures' / name / 'fixture.json'
        if name in PARTITIONS:
            manifest = ROOT / 'build/blocked-setup-recovery/fixtures' / PARTITIONS[name]
        artifacts = json.loads(manifest.read_text())['artifacts']
        pins = [a for a in artifacts if a['filename'] == TARGETS[target][0]]
        if pins:
            assert len(pins) == 1
            native = Path(pins[0]['path'])
            if not native.is_absolute():
                native = ROOT / native
            assert hashlib.sha512(native.read_bytes()).hexdigest() == pins[0]['sha512'] == PINS[target]
            candidates.append((cohort, name, manifest))
    assert candidates, 'No dependency-complete fixture with the exact reviewed Powah publisher'
    return candidates[0]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', required=True)
    parser.add_argument('--target', choices=list(TARGETS), required=True)
    parser.add_argument('--plan-only', action='store_true')
    args = parser.parse_args()
    assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.run)
    mc, loader = args.target.split('/')
    cohort, fixture, manifest = select(args.target)
    expected_cases = ['energy_cable', 'energy_hopper', 'player_transmitter', 'solar_panel']
    if mc == '1.21.1':
        expected_cases.append('energy_discharger')
    if args.plan_only:
        print(json.dumps(dict(target=args.target, fixture=fixture, cohort=cohort, manifest=manifest.relative_to(ROOT).as_posix(),
                              publisher=TARGETS[args.target][0], publisher_sha512=PINS[args.target],
                              cases=expected_cases, checks=TARGETS[args.target][2], scope=SCOPE,
                              limits='Plan only; no native execution or gameplay acceptance.'), indent=2))
        return
    with (ROOT / 'build/project-everything-regression/runtime.lock').open('a+b') as lock:
        lock.seek(0)
        msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
        try:
            out = ROOT / 'output/release-readiness' / args.run
            assert not out.exists()
            out.mkdir()
            files = [Path(__file__).with_name(v) for v in [
                'run_powah_modern.py', 'powah_modern_features.py', 'powah_modern_observer.py', 'test_powah_modern_data.py',
                'powah_legacy_features.py', 'powah_legacy_observer.py', 'ae2_settings_features.py',
                'ae2_quantum_features.py', 'mowzie_features.py', 'run_toms_all.py', 'toms_all_observer.py', 'toms_all_features.py']]
            files += [p for p in (ROOT / 'functional-tests/project-everything-regression').iterdir()
                      if p.is_file() and p.suffix in ['.py', '.txt', '.inc', '.java', '.gradle']]
            files += list((ROOT / 'functional-tests/src/main/java').rglob('*.java'))
            files += list((ROOT / ('functional-tests/all-target-rollout/java-' + mc)).rglob('*.java'))
            files += [manifest, ROOT / 'functional-tests/project-everything-ten/send.py', ROOT / 'functional-tests/all-target-rollout/evidence.py']
            inputs = {p.relative_to(ROOT).as_posix(): sha(p) for p in files}
            record = dict(status='running', target=args.target, fixture=fixture, subjects=current_subjects(args.target),
                          publisher=TARGETS[args.target][0], publisher_sha512=PINS[args.target], qa_inputs=inputs,
                          qa_source_archive=bundle(ROOT, inputs), scope=SCOPE,
                          started_at=datetime.datetime.now(datetime.timezone.utc).isoformat())
            def save():
                (out / 'summary.json').write_text(json.dumps(record, indent=2) + '\n')
            save()
            try:
                results = named(args.run, args.target, manifest, out, check=features, enhance=enhance)
                assert len(results) == 1
                row = results[0]
                assert row['status'] == 'passed' and type(row['exit_code']) is int and row['exit_code'] == 0
                assert row['results'] == dict(cases=len(expected_cases), failed=0, checks=TARGETS[args.target][2], powah_modern_features_checked=True)
                profile = ROOT / row['profile']
                staged = json.loads((profile / 'staged-artifacts.json').read_text())['artifacts']
                assert subjects_match(staged, current_subjects(args.target))
                native = [a for a in staged if a['filename'] == TARGETS[args.target][0]]
                assert len(native) == 1 and hashlib.sha512(Path(native[0]['file']).read_bytes()).hexdigest() == PINS[args.target]
                journal = profile / 'powah-modern-release/summary.json'
                data = json.loads(journal.read_text())
                assert data['complete'] is True and data['cases'] == expected_cases
                assert len(data['checks']) == TARGETS[args.target][2] and all(v['passed'] is True for v in data['checks'])
                assert inputs == {p: sha(ROOT / p) for p in inputs}
                assert current_subjects(args.target) == record['subjects']
                record.update(status='passed', results=results, acceptance=dict(file=journal.relative_to(ROOT).as_posix(), sha256=sha(journal)))
            except BaseException as error:
                record.update(status='failed', error=repr(error), trace=traceback.format_exc())
                raise
            finally:
                record['finished_at'] = datetime.datetime.now(datetime.timezone.utc).isoformat()
                save()
        finally:
            lock.seek(0)
            msvcrt.locking(lock.fileno(), msvcrt.LK_UNLCK, 1)


if __name__ == '__main__':
    main()
