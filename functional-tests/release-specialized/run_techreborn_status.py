"""Serial exact-publisher Tech Reborn zero-item control/status qualification."""
from pathlib import Path
import argparse,datetime,hashlib,importlib.util,json,msvcrt,subprocess,sys,traceback

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
from run_mod_matrix import FIXTURES
from run_release_publishers import PARTITIONS
from subject_artifacts import current_subjects,subjects_match
from techreborn_status_features import run as features

TARGETS=['1.20.1/fabric','1.21.1/fabric','26.1.2/fabric','26.2/fabric','26.3/fabric']


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()


def selection(target,fixture_name=None):
    assert target in TARGETS
    candidates=[]
    for cohort,name in enumerate(FIXTURES[target]):
        manifest=ROOT/'build/all-target-rollout/fixtures'/name/'fixture.json'
        if any(a['filename'].lower().startswith('techreborn-')for a in json.loads(manifest.read_text())['artifacts']):
            candidates.append((cohort,name,manifest))
    assert candidates,'No exact supported Tech Reborn publisher fixture'
    if fixture_name is not None:
        candidates=[v for v in candidates if v[1]==fixture_name]
        assert len(candidates)==1,'Explicit fixture must be a reviewed Tech Reborn cohort for this exact target'
    cohort,fixture,manifest=candidates[0]
    if fixture in PARTITIONS:manifest=ROOT/'build/blocked-setup-recovery/fixtures'/PARTITIONS[fixture]
    return cohort,fixture,manifest


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--run',required=True)
    parser.add_argument('--target',choices=TARGETS,required=True);parser.add_argument('--plan-only',action='store_true')
    parser.add_argument('--fixture-name',help='Select an existing reviewed cohort without changing its original index or prerequisites')
    args=parser.parse_args();assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-'for c in args.run)
    mc,loader=args.target.split('/');cohort,fixture,manifest=selection(args.target,args.fixture_name)
    if args.plan_only:
        print(json.dumps(dict(target=args.target,fixture=fixture,cohort=cohort,manifest=str(manifest.relative_to(ROOT)),
            groups=['elevator','launchpad','player_detector','basic_solar_panel'],
            scope='Native zero-item settings/status; no teleportation, launching or detector automation claim'),indent=2));return
    out=ROOT/'output/release-readiness'/args.run;assert not out.exists();out.mkdir()
    inputs=[Path(__file__).with_name(v)for v in ['run_techreborn_status.py','techreborn_status_features.py','techreborn_status_observer.py','prepare_techreborn_status.py',
        'mowzie_features.py','native_item_serialization.py']]
    inputs += [p for p in (ROOT/'functional-tests/project-everything-regression').iterdir()if p.is_file()and p.suffix in ['.py','.txt','.inc','.java','.gradle']]
    inputs += [manifest,ROOT/'functional-tests/project-everything-ten/send.py',ROOT/'functional-tests/all-target-rollout/evidence.py']
    entrypoint=Path(sys.argv[0]).resolve()
    assert entrypoint.is_relative_to(ROOT/'functional-tests/release-specialized')and entrypoint.is_file()
    inputs.append(entrypoint)
    hashes={str(p.relative_to(ROOT)):sha(p)for p in inputs}
    record=dict(status='running',target=args.target,fixture=fixture,subjects=current_subjects(args.target),qa_inputs=hashes,
        scope='Four native zero-item status/settings panels; exact selector buttons and persistence, live native solar/energy observation, movement and source cleanup. Not a complete Tech Reborn gameplay suite.',
        started_at=datetime.datetime.now(datetime.timezone.utc).isoformat())
    def save():(out/'summary.json').write_text(json.dumps(record,indent=2)+'\n')
    save()
    with(ROOT/'build/project-everything-regression/runtime.lock').open('a+b')as lock:
        lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            prepare=[sys.executable,str(Path(__file__).with_name('prepare_techreborn_status.py')),'--minecraft',mc,'--loader',loader,
                '--fixture',str(manifest),'--run',args.run+'-'+str(cohort)]
            record['prepare_command']=prepare;save()
            with(out/'prepare.log').open('x')as stream:subprocess.run(prepare,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,check=True)
            script=ROOT/'functional-tests/project-everything-regression/run_production_matrix.py'
            spec=importlib.util.spec_from_file_location('tr_status_raw_launcher',script)
            launcher=importlib.util.module_from_spec(spec);spec.loader.exec_module(launcher);launcher.check=features
            report=ROOT/'output/project-everything-regression'/(args.run+'-production-mod-matrix.json');assert not report.exists()
            previous=sys.argv;sys.argv=[str(script),'--run',args.run,'--target',args.target,'--cohort',str(cohort),'--prepared']
            if fixture in PARTITIONS:sys.argv+=['--fixture-manifest',str(manifest)]
            try:launcher.main()
            finally:sys.argv=previous
            results=json.loads(report.read_text());assert len(results)==1
            row=results[0];assert row['status']=='passed'and type(row['exit_code'])is int and row['exit_code']==0
            assert row['results']['techreborn_status_features_checked']and row['results']['cases']==4
            profile=ROOT/row['profile'];assert subjects_match(json.loads((profile/'staged-artifacts.json').read_text())['artifacts'],current_subjects(args.target))
            journal=profile/'techreborn-status-release/summary.json';data=json.loads(journal.read_text())
            assert data['complete']is True and len(data['cases'])==4 and data['checks']and all(v['passed']is True for v in data['checks'])
            assert hashes=={str(p.relative_to(ROOT)):sha(p)for p in inputs}
            assert current_subjects(args.target)==record['subjects']
            record.update(status='passed',results=results,acceptance=dict(file=str(journal.relative_to(ROOT)),sha256=sha(journal)))
        except BaseException as error:
            record.update(status='failed',error=repr(error),trace=traceback.format_exc());raise
        finally:
            record['finished_at']=datetime.datetime.now(datetime.timezone.utc).isoformat();save()
            lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)


if __name__=='__main__':main()
