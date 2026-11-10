"""Serial focused film scenario; reuse launchers, never the broad pass label."""
from pathlib import Path
import argparse,datetime,hashlib,importlib.util,json,msvcrt,sys,traceback

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
from run_mod_matrix import FIXTURES
from run_release_publishers import PARTITIONS
from subject_artifacts import current_subjects,subjects_match
from film_rename import run as film
from native_neo import source_inputs as native_inputs


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--run',required=True)
    p.add_argument('--target',choices=['1.20.1/fabric','1.20.1/forge','1.21.1/fabric','1.21.1/neoforge'],required=True)
    p.add_argument('--plan-only',action='store_true');args=p.parse_args()
    assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.run)
    candidates=[]
    for i,name in enumerate(FIXTURES[args.target]):
        manifest=ROOT/'build/all-target-rollout/fixtures'/name/'fixture.json'
        data=json.loads(manifest.read_text())
        if any(v['filename'].lower().startswith('exposure-') for v in data['artifacts']):candidates.append((i,name))
    assert candidates,'No exact publisher fixture'
    cohort,fixture=candidates[0]
    production=args.target.endswith(('/fabric','/forge'))
    script='run_production_matrix.py' if production else 'run_mod_matrix.py'
    command=['--run',args.run,'--target',args.target,'--cohort',str(cohort)]
    fixture_path=ROOT/'build/all-target-rollout/fixtures'/fixture/'fixture.json'
    if fixture in PARTITIONS:
        assert production
        fixture_path=ROOT/'build/blocked-setup-recovery/fixtures'/PARTITIONS[fixture]
        command+=['--fixture-manifest',str(fixture_path)]
    if args.plan_only:print(json.dumps(dict(target=args.target,fixture=fixture,cohort=cohort,launcher=script,arguments=command,scope='Focused native film rename only'),indent=2));return
    out=ROOT/'output/release-readiness'/args.run;assert not out.exists();out.mkdir(parents=True)
    suffix='-production-mod-matrix.json' if production else '-mod-matrix.json'
    result_path=ROOT/'output/project-everything-regression'/(args.run+suffix);assert not result_path.exists()
    helpers=[Path(__file__).with_name(v) for v in ['run_film.py','film_rename.py','native_neo.py']]
    helpers += [v for folder in [ROOT/'functional-tests/project-everything-regression']
             for v in folder.iterdir() if v.is_file() and v.suffix in ['.py','.txt','.inc','.java','.gradle']]
    fingerprints={str(v.relative_to(ROOT)):sha(v) for v in helpers}
    fingerprints.update({str(v.relative_to(ROOT)):sha(v) for v in [*native_inputs(),ROOT/'functional-tests/project-everything-ten/send.py']})
    fingerprints[str(fixture_path.relative_to(ROOT))]=sha(fixture_path)
    record=dict(status='running',scope='Focused film rename only; launcher reuse is not full-cohort coverage',target=args.target,
                fixture=fixture,fixture_manifest=str(fixture_path.relative_to(ROOT)),subjects=current_subjects(args.target),qa_inputs=fingerprints,
                started_at=datetime.datetime.now(datetime.timezone.utc).isoformat())
    def save(): (out/'summary.json').write_text(json.dumps(record,indent=2)+'\n')
    save()
    with (ROOT/'build/project-everything-regression/runtime.lock').open('a+b') as lock:
        lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            spec=importlib.util.spec_from_file_location('film_launcher',ROOT/'functional-tests/project-everything-regression'/script)
            launcher=importlib.util.module_from_spec(spec);spec.loader.exec_module(launcher)
            launcher.check=film;previous=sys.argv;sys.argv=[script,*command]
            try:launcher.main()
            finally:sys.argv=previous
            results=json.loads(result_path.read_text());assert len(results)==1
            for result in results:
                assert result['status']=='passed' and result['exit_code']==0
                assert result['results'].get('film_rename_checked') and result['results']['cases']==1
                profile=ROOT/result['profile'];staged=json.loads((profile/'staged-artifacts.json').read_text())['artifacts']
                assert subjects_match(staged,current_subjects(args.target))
                acceptance=profile/'film-rename-release/summary.json'
                record['acceptance']=dict(file=str(acceptance.relative_to(ROOT)),sha256=sha(acceptance))
            assert all(sha(ROOT/file)==value for file,value in fingerprints.items())
            assert current_subjects(args.target)==record['subjects']
            record.update(status='passed',results=results)
        except BaseException as error:
            record.update(status='failed',error=repr(error),trace=traceback.format_exc());raise
        finally:
            record['finished_at']=datetime.datetime.now(datetime.timezone.utc).isoformat();save()
            lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)


if __name__=='__main__':main()
