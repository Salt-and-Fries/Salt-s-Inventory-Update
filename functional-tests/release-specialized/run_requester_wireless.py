"""Exact supported handheld Survival wireless Requester qualification."""
from pathlib import Path
import argparse,datetime,hashlib,json,msvcrt,sys,traceback

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
from subject_artifacts import current_subjects,subjects_match
from run_mod_matrix import FIXTURES
from run_toms_all import named
from requester_wireless_features import run as features
from requester_wireless_observer import enhance


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--run',required=True);parser.add_argument('--target',choices=['1.21.1/neoforge','26.1.2/neoforge'],required=True);parser.add_argument('--plan-only',action='store_true');args=parser.parse_args()
    assert args.run and all(c in'abcdefghijklmnopqrstuvwxyz0123456789-'for c in args.run)
    candidates=[]
    for cohort,name in enumerate(FIXTURES[args.target]):
        p=ROOT/'build/all-target-rollout/fixtures'/name/'fixture.json'
        artifacts=json.loads(p.read_text())['artifacts']
        if any('merequester'in a['filename'].lower()for a in artifacts)and any('ae2wtlib'in a['filename'].lower()for a in artifacts):candidates.append((cohort,name,p))
    assert len(candidates)==1,'Exact dependency-complete Requester/WTLib publisher fixture must be unique';cohort,fixture,manifest=candidates[0]
    if args.plan_only:
        print(json.dumps(dict(target=args.target,fixture=fixture,cohort=cohort,manifest=str(manifest.relative_to(ROOT)),scope='Original Survival wireless requester, actual shared row controls and same backing manager, source lock/passive power/moved input/persistence'),indent=2));return
    out=ROOT/'output/release-readiness'/args.run;assert not out.exists();out.mkdir()
    inputs=[Path(__file__).with_name(n)for n in ['run_requester_wireless.py','requester_wireless_observer.py','requester_wireless_features.py','test_requester_wireless_data.py','requester_features.py','requester_observer.py','ae2_crafting_features.py','ae2_crafting_observer.py','ae2_child_observer.py','ae2_settings_features.py','ae2_quantum_features.py','mowzie_features.py','run_toms_all.py','toms_all_features.py','toms_all_observer.py','native_item_serialization.py']]
    inputs += [p for p in(ROOT/'functional-tests/project-everything-regression').iterdir()if p.is_file()and p.suffix in['.py','.txt','.inc','.java','.gradle']]
    inputs += list((ROOT/'functional-tests/src/main/java').rglob('*.java'))+list((ROOT/('functional-tests/all-target-rollout/java-'+args.target.split('/')[0])).rglob('*.java'))
    inputs += [manifest,ROOT/'functional-tests/project-everything-ten/send.py',ROOT/'functional-tests/all-target-rollout/evidence.py']
    hashes={str(p.relative_to(ROOT)):sha(p)for p in inputs};record=dict(status='running',target=args.target,fixture=fixture,subjects=current_subjects(args.target),qa_inputs=hashes,started_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),scope='Original Survival wireless Requester:27 named checks including genuine world recipe-network prerequisites, actual native row controls/backing manager/search/Missing/moved input/persistence and selected-source protection. Passive power remains active with strict full nonenergy source metadata and actual native debit batches. No creative exemption, assigned model/status, every WTLib upgrade or quantum transport claim.')
    def save():(out/'summary.json').write_text(json.dumps(record,indent=2)+'\n')
    save()
    with(ROOT/'build/project-everything-regression/runtime.lock').open('a+b')as lock:
        lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            results=named(args.run,args.target,manifest,out,check=features,enhance=enhance);assert len(results)==1
            row=results[0];assert row['status']=='passed'and row['exit_code']==0 and row['results']['cases']==1 and row['results']['requester_wireless_features_checked']is True
            profile=ROOT/row['profile'];assert subjects_match(json.loads((profile/'staged-artifacts.json').read_text())['artifacts'],current_subjects(args.target))
            journal=profile/'requester-wireless-release/summary.json';data=json.loads(journal.read_text());assert data['complete']is True and data['cases']==['wireless-requester']and len(data['checks'])==27 and all(v['passed']is True for v in data['checks'])
            assert hashes=={str(p.relative_to(ROOT)):sha(p)for p in inputs}and current_subjects(args.target)==record['subjects']
            record.update(status='passed',results=results,acceptance=dict(file=str(journal.relative_to(ROOT)),sha256=sha(journal)))
        except BaseException as error:
            record.update(status='failed',error=repr(error),trace=traceback.format_exc());raise
        finally:
            record['finished_at']=datetime.datetime.now(datetime.timezone.utc).isoformat();save();lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)


if __name__=='__main__':main()
