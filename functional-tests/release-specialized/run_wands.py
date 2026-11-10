"""Serial exact-publisher native tool-selector qualification, without UI control."""
from pathlib import Path
import argparse, datetime, hashlib, importlib.util, json, msvcrt, subprocess, sys, traceback

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
from run_mod_matrix import FIXTURES
from run_release_publishers import PARTITIONS
from subject_artifacts import current_subjects,subjects_match
from run_toms_all import named
from wands_features import run as features
from wands_observer import enhance


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--run',required=True)
    parser.add_argument('--target',choices=list(FIXTURES),required=True);parser.add_argument('--plan-only',action='store_true')
    args=parser.parse_args();assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-'for c in args.run)
    candidates=[];mc,loader=args.target.split('/')
    for cohort,name in enumerate(FIXTURES[args.target]):
        manifest=ROOT/'build/all-target-rollout/fixtures'/name/'fixture.json'
        if any('buildingwands' in a['filename'].lower()or a['filename'].lower().startswith('wands-')for a in json.loads(manifest.read_text())['artifacts']):
            candidates.append((cohort,name,manifest))
    assert candidates,'No exact supported BuildingWands publisher fixture'
    cohort,fixture,manifest=candidates[0]
    if fixture in PARTITIONS:manifest=ROOT/'build/blocked-setup-recovery/fixtures'/PARTITIONS[fixture]
    if args.plan_only:
        print(json.dumps(dict(target=args.target,fixture=fixture,cohort=cohort,manifest=str(manifest.relative_to(ROOT)),
            scope='Native tool-source selector; no physical-transfer or building-gameplay claim'),indent=2));return
    out=ROOT/'output/release-readiness'/args.run;assert not out.exists();out.mkdir()
    inputs=[Path(__file__).with_name(v)for v in ['run_wands.py','wands_features.py','wands_observer.py','prepare_wands.py',
        'mowzie_features.py','run_toms_all.py','toms_all_features.py','toms_all_observer.py']]
    inputs += [p for p in (ROOT/'functional-tests/project-everything-regression').iterdir()if p.is_file()and p.suffix in ['.py','.txt','.inc','.java','.gradle']]
    inputs += [manifest,ROOT/'functional-tests/project-everything-ten/send.py',ROOT/'functional-tests/all-target-rollout/evidence.py']
    if loader=='neoforge':
        inputs += list((ROOT/'functional-tests/src/main/java').rglob('*.java'))
        inputs += list((ROOT/('functional-tests/all-target-rollout/java-'+mc)).rglob('*.java'))
    inputs += [Path(__file__).with_name('native_item_serialization.py')]
    hashes={str(p.relative_to(ROOT)):sha(p)for p in inputs}
    record=dict(status='running',target=args.target,fixture=fixture,subjects=current_subjects(args.target),qa_inputs=hashes,
        scope='Native key-request entry, source selection/deselection, read-only authoritative Tools state, exact item conservation, moved input and persistence; no OS shortcut or world-building claim.',
        started_at=datetime.datetime.now(datetime.timezone.utc).isoformat())
    def save():(out/'summary.json').write_text(json.dumps(record,indent=2)+'\n')
    save()
    with (ROOT/'build/project-everything-regression/runtime.lock').open('a+b')as lock:
        lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            if loader in ['fabric','forge']:
                prepare=[sys.executable,str(Path(__file__).with_name('prepare_wands.py')),'--minecraft',mc,'--loader',loader,
                    '--fixture',str(manifest),'--run',args.run+'-'+str(cohort)]
                record['prepare_command']=prepare;save()
                with(out/'prepare.log').open('x')as stream:subprocess.run(prepare,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,check=True)
                script=ROOT/'functional-tests/project-everything-regression/run_production_matrix.py'
                spec=importlib.util.spec_from_file_location('wands_raw_launcher',script)
                launcher=importlib.util.module_from_spec(spec);spec.loader.exec_module(launcher);launcher.check=features
                report=ROOT/'output/project-everything-regression'/(args.run+'-production-mod-matrix.json');assert not report.exists()
                previous=sys.argv;sys.argv=[str(script),'--run',args.run,'--target',args.target,'--cohort',str(cohort),'--prepared']
                if fixture in PARTITIONS:sys.argv+=['--fixture-manifest',str(manifest)]
                try:launcher.main()
                finally:sys.argv=previous
                results=json.loads(report.read_text())
            else:results=named(args.run,args.target,manifest,out,check=features,enhance=enhance)
            assert len(results)==1
            row=results[0];assert row['status']=='passed'and row['exit_code']==0 and row['results']['wands_features_checked']and row['results']['cases']==1
            profile=ROOT/row['profile'];assert subjects_match(json.loads((profile/'staged-artifacts.json').read_text())['artifacts'],current_subjects(args.target))
            journal=profile/'wands-release/summary.json';data=json.loads(journal.read_text())
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
