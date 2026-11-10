"""Serial paired-equipment qualification on six exact Accessories releases."""
from pathlib import Path
import argparse,datetime,hashlib,importlib.util,json,msvcrt,subprocess,sys,traceback

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
from run_mod_matrix import FIXTURES
from subject_artifacts import current_subjects,subjects_match
from run_toms_all import named
from accessories_equipment_features import run as features
from accessories_equipment_observer import enhance

TARGETS=['1.20.1/fabric','1.20.1/forge','1.21.1/fabric','1.21.1/neoforge']


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()


def selection(target,older=False):
    if older:
        assert target in ['1.20.1/fabric','1.21.1/fabric']
        cohort=6 if target=='1.20.1/fabric' else 2
        expected='beta.48' if target=='1.20.1/fabric' else 'beta.16'
    else:
        cohort={'1.20.1/fabric':1,'1.20.1/forge':7,'1.21.1/fabric':1,'1.21.1/neoforge':14}[target]
        expected='beta.49' if target.startswith('1.20.1/') else 'beta.54'
    fixture=FIXTURES[target][cohort]
    manifest=ROOT/'build/all-target-rollout/fixtures'/fixture/'fixture.json'
    if older and target=='1.21.1/fabric':
        manifest=ROOT/'build/blocked-setup-recovery/fixtures/aether-themed-without-charm.json'
    data=json.loads(manifest.read_text());publishers=[a for a in data['artifacts'] if a['filename'].lower().startswith('accessories-')]
    assert len(publishers)==1 and expected in publishers[0]['filename']
    return cohort,fixture,manifest,publishers[0]


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--run',required=True)
    parser.add_argument('--target',choices=TARGETS,required=True);parser.add_argument('--older-release',action='store_true');parser.add_argument('--plan-only',action='store_true')
    args=parser.parse_args();assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.run)
    cohort,fixture,manifest,publisher=selection(args.target,args.older_release);mc,loader=args.target.split('/')
    if args.plan_only:
        print(json.dumps(dict(target=args.target,cohort=cohort,fixture=fixture,manifest=str(manifest.relative_to(ROOT)),publisher=publisher,
            scope='Native normal/cosmetic inventory edits and actual visibility/ownership controls; no accessory effects or Compat Layer claim.'),indent=2));return
    out=ROOT/'output/release-readiness'/args.run;assert not out.exists();out.mkdir()
    inputs=[Path(__file__).with_name(v) for v in ['run_accessories_equipment.py','run_accessories_equipment_older.py','accessories_equipment_features.py',
        'accessories_equipment_observer.py','prepare_accessories_equipment.py','native_item_serialization.py','mowzie_features.py','run_toms_all.py','toms_all_features.py','toms_all_observer.py']]
    inputs += [p for p in (ROOT/'functional-tests/project-everything-regression').iterdir() if p.is_file() and p.suffix in ['.py','.txt','.inc','.java','.gradle']]
    inputs += [manifest,ROOT/'functional-tests/project-everything-ten/send.py',ROOT/'functional-tests/all-target-rollout/evidence.py']
    if loader=='neoforge':
        inputs += list((ROOT/'functional-tests/src/main/java').rglob('*.java'))
        inputs += list((ROOT/('functional-tests/all-target-rollout/java-'+mc)).rglob('*.java'))
    hashes={str(p.relative_to(ROOT)):sha(p) for p in inputs}
    record=dict(status='running',target=args.target,fixture=fixture,publisher=publisher,subjects=current_subjects(args.target),qa_inputs=hashes,
        scope='Original Accessories capability normal/cosmetic edits using native supported validation-component item fixtures; real visibility, moved parent, available Curios/Aether exclusive swaps, refusal and recovery. No addon effects or Compat Layer acceptance.',
        started_at=datetime.datetime.now(datetime.timezone.utc).isoformat())
    def save():(out/'summary.json').write_text(json.dumps(record,indent=2)+'\n')
    save()
    with (ROOT/'build/project-everything-regression/runtime.lock').open('a+b') as lock:
        lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            if loader in ['fabric','forge']:
                prepare=[sys.executable,str(Path(__file__).with_name('prepare_accessories_equipment.py')),'--minecraft',mc,'--loader',loader,
                    '--fixture',str(manifest),'--run',args.run+'-'+str(cohort)]
                record['prepare_command']=prepare;save()
                with (out/'prepare.log').open('x') as stream:subprocess.run(prepare,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,check=True)
                script=ROOT/'functional-tests/project-everything-regression/run_production_matrix.py'
                spec=importlib.util.spec_from_file_location('accessories_raw_launcher',script);launcher=importlib.util.module_from_spec(spec);spec.loader.exec_module(launcher);launcher.check=features
                report=ROOT/'output/project-everything-regression'/(args.run+'-production-mod-matrix.json');assert not report.exists()
                previous=sys.argv;sys.argv=[str(script),'--run',args.run,'--target',args.target,'--cohort',str(cohort),'--prepared','--fixture-manifest',str(manifest)]
                try:launcher.main()
                finally:sys.argv=previous
                results=json.loads(report.read_text())
            else:results=named(args.run,args.target,manifest,out,check=features,enhance=enhance)
            assert len(results)==1
            row=results[0];assert row['status']=='passed' and row['exit_code']==0 and row['results']['cases']==1 and row['results']['accessories_equipment_checked']
            profile=ROOT/row['profile'];assert subjects_match(json.loads((profile/'staged-artifacts.json').read_text())['artifacts'],current_subjects(args.target))
            journal=profile/'accessories-equipment-release/summary.json';data=json.loads(journal.read_text())
            assert data['complete'] and data['checks'] and all(v['passed'] for v in data['checks'])
            assert hashes=={str(p.relative_to(ROOT)):sha(p) for p in inputs} and current_subjects(args.target)==record['subjects']
            record.update(status='passed',results=results,acceptance=dict(file=str(journal.relative_to(ROOT)),sha256=sha(journal)))
        except BaseException as error:
            record.update(status='failed',error=repr(error),trace=traceback.format_exc());raise
        finally:
            record['finished_at']=datetime.datetime.now(datetime.timezone.utc).isoformat();save()
            lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)


if __name__=='__main__':main()
