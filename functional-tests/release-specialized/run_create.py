"""Serial current-subject Create feature replay, using the existing raw/native launchers."""
from pathlib import Path
import argparse,datetime,hashlib,importlib.util,json,msvcrt,subprocess,sys,traceback

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
from run_mod_matrix import FIXTURES
from subject_artifacts import current_subjects,subjects_match
from create_features import run as features,GROUPS
from native_neo import execute as native_neo,source_inputs as native_inputs
from create_observer import enhance


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--run',required=True)
    p.add_argument('--target',choices=['1.20.1/fabric','1.20.1/forge','1.21.1/neoforge'],required=True)
    p.add_argument('--plan-only',action='store_true');args=p.parse_args()
    assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.run)
    candidates=[]
    for i,name in enumerate(FIXTURES[args.target]):
        manifest=ROOT/'build/all-target-rollout/fixtures'/name/'fixture.json'
        files=[v['filename'].lower() for v in json.loads(manifest.read_text())['artifacts']]
        if any(n.startswith(('create-','create-fabric-')) for n in files) and any(n.startswith('createbigcannons-') for n in files):
            candidates.append((i,name,manifest,files))
    assert candidates,'No exact Create/CBC fixture'
    cohort,fixture,fixture_path,files=candidates[0]
    # Mounted Toolbox must not silently disappear from a supposedly complete
    # named-group replay if the native Steam n Rails dependency is absent.
    assert any(n.startswith(('steam_rails-','railways-')) for n in files),'The mounted-toolbox group needs the actual Railways publisher fixture'
    production=args.target.endswith(('/fabric','/forge'))
    script='run_production_matrix.py' if production else 'private named NativeUiReference'
    command=['--run',args.run,'--target',args.target,'--cohort',str(cohort)]
    if production:command+=['--prepared']
    if args.plan_only:print(json.dumps(dict(target=args.target,fixture=fixture,cohort=cohort,launcher=script,groups=GROUPS,scope='Named Create feature groups only'),indent=2));return
    out=ROOT/'output/release-readiness'/args.run;assert not out.exists();out.mkdir(parents=True)
    suffix='-production-mod-matrix.json' if production else '-mod-matrix.json'
    result_path=ROOT/'output/project-everything-regression'/(args.run+suffix);assert not result_path.exists()
    helpers=[Path(__file__).with_name(v) for v in ['run_create.py','create_features.py','native_neo.py',
        'create_processing.py','create_observer.py','prepare_create.py']]
    helpers += [v for folder in [ROOT/'functional-tests/project-everything-regression',ROOT/'functional-tests/create-window-overhaul']
             for v in folder.iterdir() if v.is_file() and v.suffix in ['.py','.txt','.inc','.java','.gradle']]
    helpers+=[ROOT/'functional-tests/project-everything-ten/send.py',fixture_path]
    if not production:helpers += [Path(__file__).with_name(v)for v in ['create_modern.py','create_codec_1211.py']]
    helpers+=native_inputs()
    fingerprints={str(v.relative_to(ROOT)):sha(v) for v in helpers}
    record=dict(status='running',scope=f'Native Create feature replay; one aggregated scenario, {len(GROUPS)} named feature groups; not full-cohort or complete Create gameplay coverage',
                target=args.target,fixture=fixture,subjects=current_subjects(args.target),qa_inputs=fingerprints,
                started_at=datetime.datetime.now(datetime.timezone.utc).isoformat())
    def save(): (out/'summary.json').write_text(json.dumps(record,indent=2)+'\n')
    save()
    with (ROOT/'build/project-everything-regression/runtime.lock').open('a+b') as lock:
        lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            if production:
                mc,loader=args.target.split('/')
                prepare=[sys.executable,str(Path(__file__).with_name('prepare_create.py')),'--minecraft',mc,'--loader',loader,
                    '--fixture',str(fixture_path),'--run',args.run+'-'+str(cohort)]
                record['prepare_command']=prepare;save()
                with(out/'prepare.log').open('x')as stream:subprocess.run(prepare,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,check=True)
                spec=importlib.util.spec_from_file_location('create_launcher',ROOT/'functional-tests/project-everything-regression'/script)
                launcher=importlib.util.module_from_spec(spec);spec.loader.exec_module(launcher)
                launcher.check=features;previous=sys.argv;sys.argv=[script,*command]
                try:launcher.main()
                finally:sys.argv=previous
                results=json.loads(result_path.read_text())
            else:
                results=native_neo(args.run,args.target,fixture_path,out,features,enhance=enhance)
                result_path.write_text(json.dumps(results,indent=2)+'\n')
            assert len(results)==1
            for result in results:
                assert result['status']=='passed' and result['exit_code']==0
                assert result['results'].get('create_features_checked') and result['results']['feature_groups']==GROUPS
                profile=ROOT/result['profile'];staged=json.loads((profile/'staged-artifacts.json').read_text())['artifacts']
                assert subjects_match(staged,current_subjects(args.target))
                acceptance=profile/'features/journal.json';journal=json.loads(acceptance.read_text())
                assert journal['complete'] and journal['completed_groups']==GROUPS and all(v['passed'] for v in journal['checks'])
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
