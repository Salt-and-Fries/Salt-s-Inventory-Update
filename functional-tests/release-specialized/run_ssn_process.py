"""Serial exact-publisher native SSN process-configuration qualification."""
from pathlib import Path
import argparse, datetime, hashlib, importlib.util, json, msvcrt, subprocess, sys, traceback

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
from run_mod_matrix import FIXTURES
from run_release_publishers import PARTITIONS
from subject_artifacts import current_subjects,subjects_match
from run_toms_all import named
from ssn_process_features import run as features
from ssn_process_observer import enhance


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--run',required=True)
    parser.add_argument('--target',choices=['1.21.1/neoforge','26.1.2/neoforge'],required=True);parser.add_argument('--plan-only',action='store_true')
    args=parser.parse_args();assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-'for c in args.run)
    candidates=[];mc,loader=args.target.split('/')
    for cohort,name in enumerate(FIXTURES[args.target]):
        manifest=ROOT/'build/all-target-rollout/fixtures'/name/'fixture.json'
        if any(a['filename'].lower().startswith(('simplestoragenetwork-', 'storagenetwork-')) for a in json.loads(manifest.read_text())['artifacts']):
            candidates.append((cohort,name,manifest))
    assert candidates,'No exact supported SSN publisher fixture'
    cohort,fixture,manifest=candidates[0]
    if fixture in PARTITIONS:manifest=ROOT/'build/blocked-setup-recovery/fixtures'/PARTITIONS[fixture]
    if args.plan_only:
        print(json.dumps(dict(target=args.target,fixture=fixture,cohort=cohort,manifest=str(manifest.relative_to(ROOT)),
            scope='Native process ghost recipes and exact configuration; no formed-network, production reset or adjacent-machine import claim'),indent=2));return
    out=ROOT/'output/release-readiness'/args.run;assert not out.exists();out.mkdir()
    inputs=[Path(__file__).with_name(v)for v in ['run_ssn_process.py','ssn_process_features.py','ssn_process_observer.py','ssn_filters_features.py','ssn_filters_observer.py',
        'mowzie_features.py','run_toms_all.py','toms_all_features.py','toms_all_observer.py']]
    inputs += [p for p in (ROOT/'functional-tests/project-everything-regression').iterdir()if p.is_file()and p.suffix in ['.py','.txt','.inc','.java','.gradle']]
    inputs += [manifest,ROOT/'functional-tests/project-everything-ten/send.py',ROOT/'functional-tests/all-target-rollout/evidence.py']
    if loader=='neoforge':
        inputs += list((ROOT/'functional-tests/src/main/java').rglob('*.java'))
        inputs += list((ROOT/('functional-tests/all-target-rollout/java-'+mc)).rglob('*.java'))
    inputs += [Path(__file__).with_name('native_item_serialization.py')]
    hashes={str(p.relative_to(ROOT)):sha(p)for p in inputs}
    record=dict(status='running',target=args.target,fixture=fixture,subjects=current_subjects(args.target),qa_inputs=hashes,
        scope='Native process ghost recipes, count/always/redstone/faces, exact physical-item preservation, moved input, persistence and source invalidation; not formed production, reset-during-production or adjacent-machine import acceptance.',
        started_at=datetime.datetime.now(datetime.timezone.utc).isoformat())
    def save():(out/'summary.json').write_text(json.dumps(record,indent=2)+'\n')
    save()
    with (ROOT/'build/project-everything-regression/runtime.lock').open('a+b')as lock:
        lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            results=named(args.run,args.target,manifest,out,check=features,enhance=enhance)
            assert len(results)==1
            row=results[0];assert row['status']=='passed'and row['exit_code']==0 and row['results']['ssn_process_features_checked']and row['results']['cases']==1
            profile=ROOT/row['profile'];assert subjects_match(json.loads((profile/'staged-artifacts.json').read_text())['artifacts'],current_subjects(args.target))
            journal=profile/'ssn-process-release/summary.json';data=json.loads(journal.read_text())
            assert data['complete']and data['checks']and all(v['passed']for v in data['checks'])
            assert hashes=={str(p.relative_to(ROOT)):sha(p)for p in inputs}
            assert current_subjects(args.target)==record['subjects']
            record.update(status='passed',results=results,acceptance=dict(file=str(journal.relative_to(ROOT)),sha256=sha(journal)))
        except BaseException as error:
            record.update(status='failed',error=repr(error),trace=traceback.format_exc());raise
        finally:
            record['finished_at']=datetime.datetime.now(datetime.timezone.utc).isoformat();save()
            lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)


if __name__=='__main__':main()
