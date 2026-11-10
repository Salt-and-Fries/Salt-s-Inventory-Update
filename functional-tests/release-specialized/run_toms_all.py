"""Serialize native Tom workflows against each exact supported publisher set."""
from pathlib import Path
import argparse, datetime, hashlib, importlib.util, json, msvcrt, os, subprocess, sys, time, traceback

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'functional-tests/project-everything-regression'))
from run_mod_matrix import FIXTURES, finish
from run_release_publishers import PARTITIONS
from subject_artifacts import current_subjects, subjects_match
from toms_all_features import run as features, SOURCE
from toms_all_observer import prepare_named, SNIPPET
from native_neo import source_inputs as native_inputs


def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()


def named(run, target, fixture, out, check=features, enhance=None):
    mc, loader = target.split('/'); assert loader == 'neoforge'
    init, original = prepare_named(mc, run, enhance=enhance)
    profile = ROOT / 'build/all-target-rollout/gameplay' / (mc + '-' + loader + '-' + run)
    assert not profile.exists()
    command = [str(ROOT / 'gradlew.bat'), f':{mc}:{loader}:runNativePortQa', '-I', str(init),
        '-PqaMinecraft=' + mc, '-PqaLoader=' + loader, '-PqaFullGameplay=true', '-PqaScenario=extension-empty',
        '-PqaRunId=' + run, '-PqaRuntimeRoot=C:/Users/texca/.cache/siu-regression-runtime',
        '-PcompatibilityQaManifest=' + str(fixture), '--console=plain', '--max-workers=3']
    subjects = current_subjects(target)
    command += ['-PqaCoreArtifact=' + str(ROOT/'dist/core'/subjects['core'][0]),
                '-PqaCompatArtifact=' + str(ROOT/'dist/compatibility'/subjects['compatibility'][0])]
    inputs = [init, *init.parent.glob('java/com/salts_inventory_update/functionaltest/*.java')]
    row = dict(target=target, profile=str(profile.relative_to(ROOT)), status='starting', command=command,
        original_observer=dict(file=str(original.relative_to(ROOT)), sha256=sha(original)),
        generated_inputs={str(p.relative_to(ROOT)):sha(p) for p in inputs})
    with (out / 'launch.log').open('x') as log:
        process = subprocess.Popen(command, cwd=ROOT, env={**os.environ, 'JAVA_HOME':'C:/Program Files/Java/jdk-26'},
            stdout=log, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
        row['pid'] = process.pid
        try:
            deadline = time.monotonic() + 900
            while not (profile / 'ready.json').exists():
                if process.poll() is not None: raise RuntimeError('Native client exited ' + str(process.returncode))
                if time.monotonic() > deadline: raise TimeoutError('Native client readiness')
                time.sleep(1)
            generated_metadata=init.parent/'generated-metadata'
            metadata_inputs=[p for p in generated_metadata.rglob('*') if p.is_file()]
            assert any(p.suffix=='.java' for p in metadata_inputs), 'Generated named entrypoint source is missing'
            for p in metadata_inputs: row['generated_inputs'][str(p.relative_to(ROOT))]=sha(p)
            staged = json.loads((profile/'staged-artifacts.json').read_text())['artifacts']
            assert subjects_match(staged, subjects)
            qa=[v for v in staged if v.get('family')=='qa']
            assert len(qa)==1 and Path(qa[0]['file']).parent==init.parent/'artifact'
            runtime = Path('C:/Users/texca/.cache/siu-regression-runtime')/(mc+'-'+loader+'-'+run)
            row['runtime_artifact_copies'] = []
            for artifact in staged:
                copied = runtime/'mods'/artifact['filename']
                assert sha(copied) == artifact['sha256'], ('Runtime artifact changed', artifact['filename'])
                row['runtime_artifact_copies'].append(dict(file=str(copied),sha256=sha(copied),family=artifact['family']))
            row['status'] = 'testing'; row['results'] = check(profile, mc, loader)
            assert row['results']['cases'] and row['results']['failed'] == 0
            row['status'] = 'passed'
        except BaseException as error:
            row.update(status='failed', error=repr(error), trace=traceback.format_exc()); raise
        finally:
            finish(profile)
            try: row['exit_code'] = process.wait(timeout=120)
            except subprocess.TimeoutExpired:
                subprocess.run(['taskkill','/PID',str(process.pid),'/T','/F'], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                row['exit_code'] = 'terminated-owned-runtime-after-timeout'
            if row['exit_code'] != 0: row['status'] = 'failed'
            (out / 'native-attempt.json').write_text(json.dumps(row, indent=2) + '\n')
    assert row['exit_code'] == 0
    assert all(sha(ROOT/p) == h for p, h in row['generated_inputs'].items())
    return [row]


def main():
    parser = argparse.ArgumentParser(description=__doc__); parser.add_argument('--run', required=True)
    parser.add_argument('--target', choices=list(FIXTURES), required=True); parser.add_argument('--plan-only', action='store_true')
    args = parser.parse_args(); assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.run)
    mc, loader = args.target.split('/'); candidates = []
    for i, name in enumerate(FIXTURES[args.target]):
        manifest = ROOT / 'build/all-target-rollout/fixtures' / name / 'fixture.json'
        files = [v['filename'].lower() for v in json.loads(manifest.read_text())['artifacts']]
        if any(v.startswith(('toms_storage-', 'toms_storage_')) for v in files) and any('toms_trading_network' in v for v in files):
            candidates.append((i, name, manifest))
    assert candidates, 'No dependency-complete native Tom pair'
    cohort, fixture, manifest = candidates[0]
    if fixture in PARTITIONS: manifest = ROOT / 'build/blocked-setup-recovery/fixtures' / PARTITIONS[fixture]
    groups = ['terminal','link','vending'] if mc == '1.20.1' else ['terminal','configurator','link','vending']
    if args.plan_only:
        print(json.dumps(dict(target=args.target, cohort=cohort, fixture=fixture, manifest=str(manifest.relative_to(ROOT)),
            groups=groups, scope='Named Tom feature workflows, not complete-cohort acceptance'), indent=2)); return
    out = ROOT / 'output/release-readiness' / args.run; assert not out.exists(); out.mkdir()
    inputs = [Path(__file__).with_name(v) for v in ['run_toms_all.py','toms_all_features.py','toms_all_observer.py','prepare_toms_all.py']]
    inputs += [p for p in (ROOT / 'functional-tests/project-everything-regression').iterdir()
               if p.is_file() and p.suffix in ['.py','.txt','.inc','.java','.gradle']]
    inputs += [SOURCE, SNIPPET, manifest, ROOT/'functional-tests/all-target-rollout/evidence.py', *native_inputs()]
    if loader == 'neoforge':
        inputs += list((ROOT/'functional-tests/src/main/java').rglob('*.java'))
        inputs += list((ROOT/('functional-tests/all-target-rollout/java-'+mc)).rglob('*.java'))
    hashes = {str(p.relative_to(ROOT)):sha(p) for p in inputs}
    record = dict(status='running', target=args.target, fixture=fixture, subjects=current_subjects(args.target), qa_inputs=hashes,
        started_at=datetime.datetime.now(datetime.timezone.utc).isoformat(), scope='Named native Tom workflows; legacy lacks configurator. Not arbitrary topologies or full modpack acceptance.')
    def save(): (out/'summary.json').write_text(json.dumps(record, indent=2)+'\n')
    save()
    with (ROOT/'build/project-everything-regression/runtime.lock').open('a+b') as lock:
        lock.seek(0); lock.write(b'0'); lock.flush(); lock.seek(0); msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            if loader in ['fabric','forge']:
                command = [sys.executable,str(Path(__file__).with_name('prepare_toms_all.py')),'--minecraft',mc,'--loader',loader,
                    '--fixture',str(manifest),'--run',args.run+'-'+str(cohort)]
                record['prepare_command'] = command; save()
                with (out/'prepare.log').open('x') as stream: subprocess.run(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,check=True)
                script = ROOT/'functional-tests/project-everything-regression/run_production_matrix.py'
                spec = importlib.util.spec_from_file_location('toms_all_raw_launcher',script)
                launcher = importlib.util.module_from_spec(spec); spec.loader.exec_module(launcher); launcher.check = features
                report = ROOT/'output/project-everything-regression'/(args.run+'-production-mod-matrix.json'); assert not report.exists()
                previous = sys.argv; sys.argv = [str(script),'--run',args.run,'--target',args.target,'--cohort',str(cohort),'--prepared']
                if fixture in PARTITIONS: sys.argv += ['--fixture-manifest',str(manifest)]
                try: launcher.main()
                finally: sys.argv = previous
                results = json.loads(report.read_text())
            else: results = named(args.run,args.target,manifest,out)
            assert len(results) == 1
            result = results[0]; assert result['status'] == 'passed' and result['exit_code'] == 0
            assert result['results']['groups'] == groups and result['results']['toms_all_features_checked']
            profile = ROOT/result['profile']; staging = profile/'staged-artifacts.json'
            assert subjects_match(json.loads(staging.read_text())['artifacts'], current_subjects(args.target))
            journal = profile/'toms-all-release/summary.json'; data = json.loads(journal.read_text())
            assert data['complete'] and data['groups'] == groups and all(v['passed'] for v in data['checks'])
            assert {str(p.relative_to(ROOT)):sha(p) for p in inputs} == hashes
            assert current_subjects(args.target) == record['subjects']
            record.update(status='passed', results=results, acceptance=dict(file=str(journal.relative_to(ROOT)),sha256=sha(journal)))
        except BaseException as error:
            record.update(status='failed',error=repr(error),trace=traceback.format_exc()); raise
        finally:
            record['finished_at'] = datetime.datetime.now(datetime.timezone.utc).isoformat(); save()
            lock.seek(0); msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)


if __name__ == '__main__': main()
