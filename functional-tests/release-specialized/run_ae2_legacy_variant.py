"""Run unchanged native feature drivers on the separately recorded 15.4.11 pin."""
from pathlib import Path
import argparse,datetime,hashlib,importlib,importlib.util,json,msvcrt,subprocess,sys,traceback
from ae2_legacy_publisher_variant import FIXTURE,FILENAME,SHA512,native_artifact,verify_runtime_artifacts

ROOT=Path(__file__).resolve().parents[2]
sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'functional-tests/project-everything-regression')]
from subject_artifacts import current_subjects,subjects_match
from focused_release_evidence import reference
from run_mod_matrix import FIXTURES

FAMILIES={
    'settings':dict(cases=['priority','spatial-anchor','energy-level-emitter'],checks=20),
    'quantum':dict(cases=['quantum-link'],checks=12),
    'network':dict(cases=['network-status'],checks=10),
    'pattern':dict(cases=['pattern-access'],checks=19),
}


def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--run',required=True)
    p.add_argument('--target',choices=['1.20.1/fabric'],required=True);p.add_argument('--family',choices=FAMILIES,required=True)
    p.add_argument('--plan-only',action='store_true');a=p.parse_args();assert a.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-'for c in a.run)
    artifact=native_artifact();manifest=ROOT/FIXTURE
    fixture=Path(FIXTURE).parent.name;cohort=FIXTURES[a.target].index(fixture);specification=FAMILIES[a.family]
    if a.plan_only:
        print(json.dumps(dict(target=a.target,family=a.family,fixture=FIXTURE,cohort=cohort,publisher=FILENAME,sha512=SHA512,
            planned_checks=specification['checks'],scope='Original unchanged native feature assertions on this separate publisher pin. No gameplay executed.'),indent=2));return
    with(ROOT/'build/project-everything-regression/runtime.lock').open('a+b')as lock:
        lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            out=ROOT/'output/release-readiness'/a.run;assert not out.exists();out.mkdir()
            files=[*sorted(Path(__file__).parent.glob('*.py')),*[q for q in(ROOT/'functional-tests/project-everything-regression').iterdir()if q.is_file()and q.suffix in['.py','.txt','.inc','.java','.gradle']],
                manifest,ROOT/'functional-tests/project-everything-ten/send.py',ROOT/'functional-tests/all-target-rollout/evidence.py',ROOT/'output/project-everything-regression/native-entry-cases.json']
            hashes={str(q.relative_to(ROOT)):sha(q)for q in files}
            record=dict(status='running',target=a.target,fixture=fixture,subjects=current_subjects(a.target),qa_inputs=hashes,
                publisher_variant=dict(filename=FILENAME,sha512=SHA512,family=a.family,original_fixture=reference(ROOT,manifest)),
                scope='Unchanged original '+a.family+' feature driver and observer on the real audited15.4.11 publisher; no new feature scope, inferred patch equivalence or input retry.',started_at=datetime.datetime.now(datetime.timezone.utc).isoformat())
            def save():(out/'summary.json').write_text(json.dumps(record,indent=2)+'\n')
            save()
            try:
                prepare=[sys.executable,str(Path(__file__).with_name('prepare_ae2_'+a.family+'.py')),'--minecraft','1.20.1','--loader','fabric','--fixture',str(manifest),'--run',a.run+'-'+str(cohort)]
                record['prepare_command']=prepare;save()
                with(out/'prepare.log').open('x')as stream:subprocess.run(prepare,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,check=True)
                script=ROOT/'functional-tests/project-everything-regression/run_production_matrix.py'
                spec=importlib.util.spec_from_file_location('legacy_ae2_native_feature_launcher',script);launcher=importlib.util.module_from_spec(spec);spec.loader.exec_module(launcher)
                launcher.check=importlib.import_module('ae2_'+a.family+'_features').run
                report=ROOT/'output/project-everything-regression'/(a.run+'-production-mod-matrix.json');assert not report.exists()
                previous=sys.argv;sys.argv=[str(script),'--run',a.run,'--target',a.target,'--cohort',str(cohort),'--prepared','--fixture-manifest',str(manifest)]
                record['command']=list(sys.argv);save()
                try:launcher.main()
                finally:sys.argv=previous
                results=json.loads(report.read_text());assert len(results)==1
                row=results[0];assert row['status']=='passed'and type(row['exit_code'])is int and row['exit_code']==0
                assert row['results']['ae2_'+a.family+'_features_checked']is True and row['results']['cases']==len(specification['cases'])
                profile=ROOT/row['profile'];staged=json.loads((profile/'staged-artifacts.json').read_text())['artifacts']
                assert subjects_match(staged,record['subjects']);native=verify_runtime_artifacts(ROOT,staged)
                journal=profile/('ae2-'+a.family+'-release')/'summary.json';data=json.loads(journal.read_text())
                assert data['complete']is True and data['cases']==specification['cases']and len(data['checks'])==specification['checks']and all(v['passed']is True for v in data['checks'])
                assert hashes=={str(q.relative_to(ROOT)):sha(q)for q in files}and current_subjects(a.target)==record['subjects']
                record.update(status='passed',results=results,acceptance=reference(ROOT,journal),actual_native_publisher=native)
            except BaseException as error:
                record.update(status='failed',error=repr(error),trace=traceback.format_exc());raise
            finally:record['finished_at']=datetime.datetime.now(datetime.timezone.utc).isoformat();save()
        finally:lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)


if __name__=='__main__':main()
