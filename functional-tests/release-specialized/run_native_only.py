"""Serial final-artifact qualification of the six retained native-only mods."""
from pathlib import Path
import argparse,datetime,hashlib,importlib.util,json,msvcrt,subprocess,sys,traceback

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
from subject_artifacts import current_subjects,subjects_match
from run_toms_all import named
from native_only_features import run as features
from native_only_observer import enhance

TARGETS=['1.20.1/fabric','1.20.1/forge','1.21.1/neoforge']
FIXTURE_INDEX=ROOT/'build/release-readiness/native-only-fixtures-01.json'


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()


def selection(target):
    values=[v for v in json.loads(FIXTURE_INDEX.read_text())['records'] if v['target']==target];assert len(values)==1
    record=values[0];manifest=ROOT/record['manifest'];original=ROOT/record['original_fixture']
    assert sha(manifest)==record['manifest_sha256'] and sha(original)==record['original_fixture_sha256']
    data=json.loads(manifest.read_text());assert data['scenario']=='extension-empty'
    assert data['minecraft']+'/'+data['loader']==target
    assert data['artifacts']==json.loads(original.read_text())['artifacts'],'No native publisher substitutions'
    for a in data['artifacts']:assert hashlib.sha512((ROOT/a['path']).read_bytes()).hexdigest()==a['sha512']
    return record,manifest,original


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--run',required=True)
    parser.add_argument('--target',choices=TARGETS,required=True);parser.add_argument('--plan-only',action='store_true')
    args=parser.parse_args();assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.run)
    selection_record,manifest,original=selection(args.target);mc,loader=args.target.split('/')
    groups=['dungeonz','sortilege','things','wraith-waystones'] if loader=='fabric' else ['elevatormod'] if loader=='forge' else ['armor-statues']
    if args.plan_only:
        print(json.dumps(dict(**selection_record,groups=groups,expected_screen_types=9 if loader=='fabric' else 1 if loader=='forge' else 6,
            scope='Current native UI ownership/input and cleanup only; not whole upstream gameplay or untested releases.'),indent=2));return
    out=ROOT/'output/release-readiness'/args.run;assert not out.exists();out.mkdir()
    inputs=[Path(__file__).with_name(v) for v in ['run_native_only.py','native_only_features.py','native_only_observer.py','prepare_native_only.py',
        'native_item_serialization.py','mowzie_features.py','run_toms_all.py','toms_all_features.py','toms_all_observer.py']]
    inputs += [p for p in (ROOT/'functional-tests/project-everything-regression').iterdir() if p.is_file() and p.suffix in ['.py','.txt','.inc','.java','.gradle']]
    inputs += [FIXTURE_INDEX,manifest,original,ROOT/'functional-tests/project-everything-ten/send.py',ROOT/'functional-tests/all-target-rollout/evidence.py']
    if loader=='neoforge':
        inputs += list((ROOT/'functional-tests/src/main/java').rglob('*.java'))
        inputs += list((ROOT/('functional-tests/all-target-rollout/java-'+mc)).rglob('*.java'))
    hashes={str(p.relative_to(ROOT)):sha(p) for p in inputs}
    record=dict(status='running',target=args.target,fixture=selection_record,subjects=current_subjects(args.target),qa_inputs=hashes,
        scope='Six original native-only mods on their exact retained publisher releases. Real native entry/navigation/input/settings, zero SIU source ownership, typed foreign inventory preservation, Escape/reopening and ordinary SIU inventory return. No whole upstream gameplay, additional releases or conditional Vanilla Tweaks datapack claim.',
        started_at=datetime.datetime.now(datetime.timezone.utc).isoformat())
    def save():(out/'summary.json').write_text(json.dumps(record,indent=2)+'\n')
    save()
    with (ROOT/'build/project-everything-regression/runtime.lock').open('a+b') as lock:
        lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            if loader in ['fabric','forge']:
                prepare=[sys.executable,str(Path(__file__).with_name('prepare_native_only.py')),'--minecraft',mc,'--loader',loader,
                    '--fixture',str(manifest),'--run',args.run+'-0']
                record['prepare_command']=prepare;save()
                with (out/'prepare.log').open('x') as stream:subprocess.run(prepare,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,check=True)
                script=ROOT/'functional-tests/project-everything-regression/run_production_matrix.py'
                spec=importlib.util.spec_from_file_location('native_only_raw_launcher',script);launcher=importlib.util.module_from_spec(spec);spec.loader.exec_module(launcher);launcher.check=features
                report=ROOT/'output/project-everything-regression'/(args.run+'-production-mod-matrix.json');assert not report.exists()
                previous=sys.argv;sys.argv=[str(script),'--run',args.run,'--target',args.target,'--cohort','0','--prepared','--fixture-manifest',str(manifest)]
                try:launcher.main()
                finally:sys.argv=previous
                results=json.loads(report.read_text())
            else:results=named(args.run,args.target,manifest,out,check=features,enhance=enhance)
            assert len(results)==1
            row=results[0];assert row['status']=='passed' and row['exit_code']==0 and row['results']['cases']==len(groups) and row['results']['native_only_checked']
            assert len(row['results']['screens'])==(9 if loader=='fabric' else 1 if loader=='forge' else 6)
            profile=ROOT/row['profile'];assert subjects_match(json.loads((profile/'staged-artifacts.json').read_text())['artifacts'],current_subjects(args.target))
            journal=profile/'native-only-release/summary.json';data=json.loads(journal.read_text())
            assert data['complete'] and data['cases']==groups and data['checks'] and all(v['passed'] for v in data['checks'])
            assert hashes=={str(p.relative_to(ROOT)):sha(p) for p in inputs} and current_subjects(args.target)==record['subjects']
            record.update(status='passed',results=results,acceptance=dict(file=str(journal.relative_to(ROOT)),sha256=sha(journal)))
        except BaseException as error:
            record.update(status='failed',error=repr(error),trace=traceback.format_exc());raise
        finally:
            record['finished_at']=datetime.datetime.now(datetime.timezone.utc).isoformat();save()
            lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)


if __name__=='__main__':main()
