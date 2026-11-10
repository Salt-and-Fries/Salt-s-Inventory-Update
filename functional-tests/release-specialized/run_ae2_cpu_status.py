"""One exact-publisher native Crafting Status selector workflow."""
from pathlib import Path
import argparse,datetime,hashlib,json,msvcrt,sys,traceback,zipfile

ROOT=Path(__file__).resolve().parents[2]
sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'functional-tests/project-everything-regression')]
from run_mod_matrix import FIXTURES
from run_release_publishers import PARTITIONS
from subject_artifacts import current_subjects,subjects_match
from run_toms_all import named
from ae2_cpu_status_features import run as features,NAMES,CASE,SCOPE
from ae2_cpu_status_observer import enhance
from full_suite_provenance import bundle


def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--run',required=True)
    parser.add_argument('--target',choices=['1.21.1/neoforge'],required=True);parser.add_argument('--plan-only',action='store_true');args=parser.parse_args()
    assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-'for c in args.run)
    mc,loader=args.target.split('/');found=[]
    for cohort,name in enumerate(FIXTURES[args.target]):
        manifest=ROOT/'build/all-target-rollout/fixtures'/name/'fixture.json'
        artifacts=json.loads(manifest.read_text())['artifacts']
        if any(a['filename']=='appliedenergistics2-19.2.18.jar'for a in artifacts):found.append((cohort,name,manifest))
    assert found;cohort,fixture,manifest=found[0]
    if fixture in PARTITIONS:manifest=ROOT/'build/blocked-setup-recovery/fixtures'/PARTITIONS[fixture]
    pinned=[a for a in json.loads(manifest.read_text())['artifacts']if a['filename']=='appliedenergistics2-19.2.18.jar'];assert len(pinned)==1
    native=Path(pinned[0]['path']);native=native if native.is_absolute()else ROOT/native
    assert hashlib.sha512(native.read_bytes()).hexdigest()==pinned[0]['sha512']
    with zipfile.ZipFile(native)as jar:assert 'assets/ae2/models/item/fluix_smart_dense_cable.json'in jar.namelist()
    if args.plan_only:
        print(json.dumps(dict(target=args.target,fixture=fixture,manifest=manifest.relative_to(ROOT).as_posix(),checks=len(NAMES),scope=SCOPE,
            limits='Plan only: Java/Gradle/game were not launched and no native acceptance is assigned.'),indent=2));return
    with(ROOT/'build/project-everything-regression/runtime.lock').open('a+b')as lock:
        lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            out=ROOT/'output/release-readiness'/args.run;assert not out.exists();out.mkdir()
            files=[Path(__file__).with_name(v)for v in ['run_ae2_cpu_status.py','ae2_cpu_status_features.py','ae2_cpu_status_observer.py',
                'ae2_settings_features.py','ae2_child_observer.py','mowzie_features.py','run_toms_all.py','toms_all_features.py','toms_all_observer.py','native_item_serialization.py']]
            files += [p for p in(ROOT/'functional-tests/project-everything-regression').iterdir()if p.is_file()and p.suffix in ['.py','.txt','.inc','.java','.gradle']]
            files += list((ROOT/'functional-tests/src/main/java').rglob('*.java'))+list((ROOT/('functional-tests/all-target-rollout/java-'+mc)).rglob('*.java'))
            files += [manifest,ROOT/'functional-tests/project-everything-ten/send.py',ROOT/'functional-tests/all-target-rollout/evidence.py',ROOT/'output/project-everything-regression/native-entry-cases.json']
            inputs={p.relative_to(ROOT).as_posix():sha(p)for p in files}
            record=dict(status='running',target=args.target,fixture=fixture,subjects=current_subjects(args.target),qa_inputs=inputs,
                qa_source_archive=bundle(ROOT,inputs),scope=SCOPE,started_at=datetime.datetime.now(datetime.timezone.utc).isoformat())
            def save():(out/'summary.json').write_text(json.dumps(record,indent=2)+'\n')
            save()
            try:
                results=named(args.run,args.target,manifest,out,check=features,enhance=enhance);assert len(results)==1
                row=results[0];assert row['status']=='passed'and type(row['exit_code'])is int and row['exit_code']==0
                assert row['results']['ae2_cpu_status_features_checked']is True and row['results']['cases']==1 and row['results']['failed']==0 and row['results']['checks']==len(NAMES)
                profile=ROOT/row['profile'];assert subjects_match(json.loads((profile/'staged-artifacts.json').read_text())['artifacts'],current_subjects(args.target))
                journal=profile/'ae2-cpu-status-release/summary.json';data=json.loads(journal.read_text())
                assert data['complete']is True and data['cases']==[CASE]and [(v['name'],v['passed'])for v in data['checks']]==[(v,True)for v in NAMES]
                assert inputs=={p:sha(ROOT/p)for p in inputs}and current_subjects(args.target)==record['subjects']
                record.update(status='passed',results=results,acceptance=dict(file=journal.relative_to(ROOT).as_posix(),sha256=sha(journal)))
            except BaseException as error:
                record.update(status='failed',error=repr(error),trace=traceback.format_exc());raise
            finally:record['finished_at']=datetime.datetime.now(datetime.timezone.utc).isoformat();save()
        finally:lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)


if __name__=='__main__':main()
