"""Qualify exact publisher binaries omitted by the older complete cohort plan."""
from pathlib import Path
import argparse, datetime, hashlib, importlib.util, json, msvcrt, subprocess, sys, traceback

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT/'functional-tests/project-everything-regression'))
from check_mod_interactions import run as check
from subject_artifacts import current_subjects, subjects_match
from run_toms_all import named

PLAN = ROOT/'build/release-readiness/publisher-extra-plan-01.json'


def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__); parser.add_argument('--run',required=True)
    jobs = json.loads(PLAN.read_text())['jobs']
    parser.add_argument('--target',choices=[v['target']for v in jobs],required=True)
    parser.add_argument('--plan-only',action='store_true');args=parser.parse_args()
    assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-'for c in args.run)
    job = next(v for v in jobs if v['target']==args.target); fixture = ROOT/job['fixture']
    mc,loader = args.target.split('/')
    if args.plan_only: print(json.dumps({**job,'scope':'Complete discovery and all selected interactions for this additional publisher fixture; no filters or case limits'},indent=2));return
    out = ROOT/'output/release-readiness'/args.run;assert not out.exists();out.mkdir()
    inputs = [PLAN,fixture,Path(__file__),Path(__file__).with_name('run_toms_all.py'),
              Path(__file__).with_name('toms_all_features.py'),Path(__file__).with_name('toms_all_observer.py')]
    inputs += [p for p in (ROOT/'functional-tests/project-everything-regression').iterdir()if p.is_file()and p.suffix in ['.py','.txt','.inc','.java','.gradle']]
    inputs += list((ROOT/'functional-tests/src/main/java').rglob('*.java'))
    inputs += list((ROOT/('functional-tests/all-target-rollout/java-'+mc)).rglob('*.java'))
    inputs += [ROOT/'functional-tests/all-target-rollout/evidence.py']
    hashes = {str(p.relative_to(ROOT)):sha(p)for p in inputs}
    record = dict(status='running',target=args.target,fixture=job['fixture'],family=job['family'],
        subjects=current_subjects(args.target),qa_inputs=hashes,scope='Additional exact publisher full discovery, strict native entry and interactions; no silent case limits.',
        started_at=datetime.datetime.now(datetime.timezone.utc).isoformat())
    def save():(out/'summary.json').write_text(json.dumps(record,indent=2)+'\n')
    save()
    with (ROOT/'build/project-everything-regression/runtime.lock').open('a+b')as lock:
        lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            if loader=='forge':
                script=ROOT/'functional-tests/project-everything-regression/run_production_matrix.py'
                spec=importlib.util.spec_from_file_location('additional_publisher_raw_launcher',script)
                launcher=importlib.util.module_from_spec(spec);spec.loader.exec_module(launcher)
                report=ROOT/'output/project-everything-regression'/(args.run+'-production-mod-matrix.json');assert not report.exists()
                previous=sys.argv;sys.argv=[str(script),'--run',args.run,'--target',args.target,'--cohort','0','--fixture-manifest',str(fixture)]
                try:launcher.main()
                finally:sys.argv=previous
                results=json.loads(report.read_text())
            else:results=named(args.run,args.target,fixture,out,check=check)
            assert len(results)==1
            row=results[0];assert row['status']=='passed'and row['exit_code']==0 and row['results']['failed']==0 and row['results']['cases']>0
            profile=ROOT/row['profile'];staged=json.loads((profile/'staged-artifacts.json').read_text())['artifacts']
            assert subjects_match(staged,current_subjects(args.target))
            actual={v['filename']:v['sha256']for v in staged}
            for artifact in json.loads(fixture.read_text())['artifacts']:
                file=Path(artifact['path']);file=file if file.is_absolute()else ROOT/file
                assert hashlib.sha512(file.read_bytes()).hexdigest()==artifact['sha512']
                if artifact.get('coordinate')or artifact.get('stage_as_root'):assert actual[artifact['filename']]==sha(file)
            path=profile/'mod-interaction-checks.json';data=json.loads(path.read_text())
            assert data['selection']==dict(only=[],max_cases=None,mods=[])
            assert data['checks']and all(v['passed']for v in data['checks'])
            assert not any(v['status']=='failed'for v in data['cases'])
            if job['family']=='ae2wtlib':
                relevant=[v for v in data['cases']if any('ae2wtlib'in str(c).lower()or 'de.mari_023' in str(c).lower()for c in v.get('native_menus',[]))]
            else:relevant=[v for v in data['cases']if any('owmii.powah' in str(c)for c in v.get('native_menus',[]))]
            assert relevant, 'The added publisher must have real native menu observations, not merely be loaded alongside base AE2'
            record['additional_publisher_cases']=[dict(id=v['id'],status=v['status'],native_menus=v.get('native_menus',[]))for v in relevant]
            assert hashes=={str(p.relative_to(ROOT)):sha(p)for p in inputs}
            assert current_subjects(args.target)==record['subjects']
            record.update(status='passed',results=results,acceptance=dict(file=str(path.relative_to(ROOT)),sha256=sha(path)))
        except BaseException as error:
            record.update(status='failed',error=repr(error),trace=traceback.format_exc());raise
        finally:
            record['finished_at']=datetime.datetime.now(datetime.timezone.utc).isoformat();save()
            lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)


if __name__=='__main__':main()
