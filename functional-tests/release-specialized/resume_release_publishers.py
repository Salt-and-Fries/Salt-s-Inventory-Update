"""Continue the frozen publisher plan using complete named observations.

Existing attempts and frozen regression sources are retained. New NeoForge
attempts add actual menu/template/scroll fields required by the unchanged bee
care driver; all discovered cases and original assertions still execute.
"""
from pathlib import Path
import argparse
import datetime
import hashlib
import json
import msvcrt
import subprocess
import sys
import traceback

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'functional-tests/project-everything-regression'))
from run_release_publishers import plan
from subject_artifacts import current_subjects
from run_toms_all import named
from native_observation_schema import enhance
from check_mod_interactions import run as check


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def inputs_for(target):
    inputs = [p for p in (ROOT/'functional-tests/project-everything-regression').iterdir()
              if p.is_file() and p.suffix in {'.py', '.txt', '.inc', '.java', '.gradle'}]
    inputs += [Path(__file__), Path(__file__).with_name('native_observation_schema.py')]
    if target.endswith('/neoforge'):
        inputs += [Path(__file__).with_name(v) for v in ['run_toms_all.py', 'toms_all_observer.py']]
        inputs += list((ROOT/'functional-tests/src/main/java').rglob('*.java'))
        inputs += list((ROOT/('functional-tests/all-target-rollout/java-'+target.split('/')[0])).rglob('*.java'))
    return {str(p.relative_to(ROOT)): sha(p) for p in inputs}


def previous_accepted(attempt, target):
    assert attempt['exit_code'] == 0 and attempt['qa_inputs_unchanged'] and attempt['subjects_unchanged']
    assert attempt['subjects'] == {k: list(v) for k, v in current_subjects(target).items()}
    assert all(sha(ROOT/p) == h for p, h in attempt['qa_inputs'].items()), 'Previously passed input changed'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', required=True)
    parser.add_argument('--plan-only', action='store_true')
    args = parser.parse_args()
    assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.run)
    folder = ROOT/'output/release-readiness'/args.run
    journal = folder/'publishers.json'
    record = json.loads(journal.read_text())
    jobs = plan()
    assert record['plan'] == jobs, 'Frozen exact-publisher plan changed'
    for job in jobs:
        previous = [v for v in record['attempts'] if v['job'] == job['id']]
        if previous and previous[-1]['status'] == 'passed':
            previous_accepted(previous[-1], job['target'])
    if args.plan_only:
        print('Verified unchanged', len(jobs), 'job plan and all previously accepted subjects/inputs')
        return

    def save():
        journal.write_text(json.dumps(record, indent=2)+'\n')

    with (ROOT/'build/project-everything-regression/runtime.lock').open('a+b') as lock:
        lock.seek(0); lock.write(b'0'); lock.flush(); lock.seek(0)
        msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
        try:
            record['status'] = 'running'; save()
            for job in jobs:
                previous = [v for v in record['attempts'] if v['job'] == job['id']]
                if previous and previous[-1]['status'] == 'passed':
                    previous_accepted(previous[-1], job['target']); continue
                name = args.run+'-'+job['id']+'-a'+str(len(previous)+1)
                for key in ['manifest', 'original_manifest']:
                    assert sha(ROOT/job[key]) == job[key+'_sha256']
                production = job['target'].endswith(('/fabric', '/forge'))
                script = ROOT/'functional-tests/project-everything-regression'/('run_production_matrix.py' if production else 'run_mod_matrix.py')
                command = [sys.executable, str(script), '--run', name, '--target', job['target'], '--cohort', str(job['cohort'])]
                if job['partition']:
                    assert production
                    command += ['--fixture-manifest', str(ROOT/job['manifest'])]
                log = ROOT/'build/release-readiness'/(name+'.log')
                assert not log.exists(), 'Retain earlier log'
                inputs = inputs_for(job['target'])
                for key in ['manifest', 'original_manifest']:
                    inputs[job[key]] = sha(ROOT/job[key])
                attempt = dict(job=job['id'], run=name, status='running', arguments=command[1:],
                    log=str(log.relative_to(ROOT)), subjects=current_subjects(job['target']), qa_inputs=inputs,
                    started=datetime.datetime.now(datetime.timezone.utc).isoformat(),
                    continuation_runner=str(Path(__file__).relative_to(ROOT)),
                    observation_adapter=None if production else 'native_observation_schema.py: actual read-only identity/layout/scroll fields; no assertion/action substitutions')
                record['attempts'].append(attempt); save(); print('START', name, flush=True)
                exit_code = None
                try:
                    if production:
                        with log.open('x') as stream:
                            completed = subprocess.run(command, cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT)
                        exit_code = completed.returncode
                        matrix = ROOT/'output/project-everything-regression'/(name+'-production-mod-matrix.json')
                        if matrix.exists():
                            attempt['results'] = json.loads(matrix.read_text())
                    else:
                        private = folder/(name+'-native-attempt')
                        assert not private.exists(); private.mkdir()
                        attempt['private_evidence'] = str(private.relative_to(ROOT)); save()
                        # named() owns a fresh runtime, verifies actual staged copies,
                        # and records the generated Java/init and original observer.
                        with log.open('x') as stream:
                            import contextlib
                            with contextlib.redirect_stdout(stream), contextlib.redirect_stderr(stream):
                                attempt['results'] = named(name, job['target'], ROOT/job['manifest'], private,
                                                           check=check, enhance=enhance)
                        exit_code = attempt['results'][0]['exit_code']
                        matrix = ROOT/'output/project-everything-regression'/(name+'-mod-matrix.json')
                        with matrix.open('x') as stream:
                            json.dump(attempt['results'], stream, indent=2); stream.write('\n')
                    assert attempt.get('results') and all(v['status']=='passed' and v['exit_code']==0 for v in attempt['results'])
                except BaseException as error:
                    attempt.update(error=repr(error), trace=traceback.format_exc())
                    if not production:
                        actual = private/'native-attempt.json'
                        if actual.exists():
                            attempt['results'] = [json.loads(actual.read_text())]
                            exit_code = attempt['results'][0]['exit_code']
                    if exit_code is None:
                        exit_code = 'launcher-error'
                unchanged = all(sha(ROOT/p)==h for p,h in inputs.items())
                same = current_subjects(job['target']) == attempt['subjects']
                passed = exit_code==0 and unchanged and same and not attempt.get('error')
                attempt.update(status='passed' if passed else 'failed', exit_code=exit_code,
                    qa_inputs_unchanged=unchanged, subjects_unchanged=same,
                    finished=datetime.datetime.now(datetime.timezone.utc).isoformat())
                save(); print(attempt['status'], name, flush=True)
                if not passed:
                    record['status']='failed'; save()
                    raise RuntimeError('Preserved failed publisher job '+job['id'])
            record['status']='complete-discovery-results-need-case-disposition'; save()
        finally:
            lock.seek(0); msvcrt.locking(lock.fileno(), msvcrt.LK_UNLCK, 1)


if __name__ == '__main__':
    main()
