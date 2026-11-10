"""Serial current-artifact NiftyCarts native equipment-cell qualification."""
from pathlib import Path
import argparse,datetime,hashlib,importlib.util,json,msvcrt,subprocess,sys,traceback

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
from run_mod_matrix import FIXTURES
from run_release_publishers import PARTITIONS
from subject_artifacts import current_subjects,subjects_match
from nifty_features import run as nifty,CASES
from native_neo import execute as native_neo,source_inputs as native_inputs
from nifty_observer import enhance


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--run',required=True)
    p.add_argument('--target',choices=['1.20.1/fabric','1.21.1/fabric','1.21.11/fabric'],required=True)
    p.add_argument('--plan-only',action='store_true');args=p.parse_args()
    assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.run)
    candidates=[]
    for i,name in enumerate(FIXTURES[args.target]):
        manifest=ROOT/'build/all-target-rollout/fixtures'/name/'fixture.json'
        if any(v['filename'].lower().startswith('niftycarts-') for v in json.loads(manifest.read_text())['artifacts']):candidates.append((i,name))
    assert candidates,'No exact native NiftyCarts publisher fixture'
    cohort,fixture=candidates[0];mc,loader=args.target.split('/')
    production=loader in ['fabric','forge'];script='run_production_matrix.py'
    command=['--run',args.run,'--target',args.target,'--cohort',str(cohort),'--prepared']
    fixture_path=ROOT/'build/all-target-rollout/fixtures'/fixture/'fixture.json'
    if fixture in PARTITIONS:
        assert production
        fixture_path=ROOT/'build/blocked-setup-recovery/fixtures'/PARTITIONS[fixture]
        command+=['--fixture-manifest',str(fixture_path)]
    if args.plan_only:
        print(json.dumps(dict(target=args.target,fixture=fixture,cohort=cohort,
            launcher=script if production else 'private named NativeUiReference',
            fixture_manifest=str(fixture_path.relative_to(ROOT)),menus=[v[0]for v in CASES],
            scope='Two native cart inventories; no complete-cohort claim'),indent=2));return
    out=ROOT/'output/release-readiness'/args.run;assert not out.exists();out.mkdir(parents=True)
    suffix='-production-mod-matrix.json' if production else '-mod-matrix.json'
    result_path=ROOT/'output/project-everything-regression'/(args.run+suffix);assert not result_path.exists()
    helpers=[Path(__file__).with_name(v) for v in ['run_nifty.py','nifty_features.py','nifty_observer.py','prepare_nifty.py','native_neo.py','mowzie_features.py']]
    helpers += [v for folder in [ROOT/'functional-tests/project-everything-regression']
        for v in folder.iterdir()if v.is_file()and v.suffix in ['.py','.txt','.inc','.java','.gradle']]
    helpers+=[ROOT/'functional-tests/project-everything-ten/send.py',fixture_path,*native_inputs()]
    helpers += [Path(__file__).with_name('native_item_serialization.py')]
    fingerprints={str(v.relative_to(ROOT)):sha(v)for v in helpers}
    record=dict(status='running',scope='Two real NiftyCarts inventories: all three native hoe cells and nine seed cells, complete data, refused foreign seed input, moved geometry, ordinary persistence and source invalidation. Shipping jars and action callbacks remain untouched; not agricultural automation or cart driving.',
        target=args.target,fixture=fixture,subjects=current_subjects(args.target),qa_inputs=fingerprints,
        started_at=datetime.datetime.now(datetime.timezone.utc).isoformat())
    def save():(out/'summary.json').write_text(json.dumps(record,indent=2)+'\n')
    save()
    with (ROOT/'build/project-everything-regression/runtime.lock').open('a+b')as lock:
        lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            if production:
                prepare=[sys.executable,str(Path(__file__).with_name('prepare_nifty.py')),'--minecraft',mc,'--loader',loader,
                    '--fixture',str(fixture_path),'--run',args.run+'-'+str(cohort)]
                record['prepare_command']=prepare;save()
                with(out/'prepare.log').open('x')as stream:subprocess.run(prepare,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,check=True)
                spec=importlib.util.spec_from_file_location('nifty_launcher',ROOT/'functional-tests/project-everything-regression'/script)
                launcher=importlib.util.module_from_spec(spec);spec.loader.exec_module(launcher)
                launcher.check=nifty;previous=sys.argv;sys.argv=[script,*command]
                try:launcher.main()
                finally:sys.argv=previous
                results=json.loads(result_path.read_text())
            else:
                results=native_neo(args.run,args.target,fixture_path,out,lambda profile:nifty(profile,mc,loader),enhance=enhance)
                result_path.write_text(json.dumps(results,indent=2)+'\n')
            assert len(results)==1
            for result in results:
                assert result['status']=='passed'and result['exit_code']==0
                assert result['results'].get('nifty_features_checked')and result['results']['cases']==len(CASES)
                profile=ROOT/result['profile'];staged=json.loads((profile/'staged-artifacts.json').read_text())['artifacts']
                assert subjects_match(staged,current_subjects(args.target))
                acceptance=profile/'nifty-release/summary.json';journal=json.loads(acceptance.read_text())
                assert journal['complete']and journal['cases']==[v[0]for v in CASES]and all(c['passed']for c in journal['checks'])
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
