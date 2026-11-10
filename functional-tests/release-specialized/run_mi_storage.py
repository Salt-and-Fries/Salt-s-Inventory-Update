"""Serial exact native storage replays; not recipe/production qualification."""
from pathlib import Path
import argparse,datetime,hashlib,importlib.util,json,msvcrt,subprocess,sys,traceback

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
from run_mod_matrix import FIXTURES
from subject_artifacts import current_subjects,subjects_match
from mi_storage_features import INPUTS,FLUIDS,OUTPUTS
from mi_storage_controls import run as mi,expected_counts
from mi_storage_observer import enhance
from native_neo import execute as native_neo,source_inputs as native_inputs


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--run',required=True)
    p.add_argument('--target',choices=['1.20.1/fabric','1.21.1/neoforge'],required=True)
    p.add_argument('--plan-only',action='store_true');args=p.parse_args()
    assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-'for c in args.run)
    candidates=[]
    for i,name in enumerate(FIXTURES[args.target]):
        manifest=ROOT/'build/all-target-rollout/fixtures'/name/'fixture.json'
        if any(v['filename'].lower().startswith('modern-industrialization-')for v in json.loads(manifest.read_text())['artifacts']):candidates.append((i,name,manifest))
    assert candidates
    cohort,fixture,manifest=candidates[0];mc,loader=args.target.split('/');production=loader=='fabric'
    if args.plan_only:
        print(json.dumps(dict(target=args.target,fixture=fixture,cohort=cohort,
            launcher='run_production_matrix.py'if production else'private named NativeUiReference',
            scope='Seventeen exact native storage panels, original transfers plus native lock-editor/auto-eject controls; no full-cohort/recipe or natural generation claim'),indent=2));return
    out=ROOT/'output/release-readiness'/args.run;assert not out.exists();out.mkdir()
    suffix='-production-mod-matrix.json'if production else'-mod-matrix.json'
    report=ROOT/'output/project-everything-regression'/(args.run+suffix);assert not report.exists()
    helpers=[Path(__file__),*[Path(__file__).with_name(v) for v in ['mi_storage_features.py','mi_storage_controls.py','mi_storage_observer.py','prepare_mi_storage.py','native_neo.py','mowzie_features.py','mi_energy_features.py']],*INPUTS,*native_inputs(),manifest]
    helpers += [v for v in (ROOT/'functional-tests/project-everything-regression').iterdir()
                if v.is_file()and v.suffix in ['.py','.txt','.inc','.java','.gradle']]
    helpers += [ROOT/'functional-tests/project-everything-ten/send.py']
    fingerprints={str(p.relative_to(ROOT)):sha(p)for p in helpers}
    record=dict(status='running',target=args.target,fixture=fixture,subjects=current_subjects(args.target),qa_inputs=fingerprints,
                scope='Seventeen exact native MI storage panels; original bucket/typed output transfers, first/last cells and full resource/cursor ledgers, plus native lock-editor/empty-cell policy/auto-eject controls and restoration/persistence; not generation/recipe, adjacent-container automation or full-cohort acceptance',
                started_at=datetime.datetime.now(datetime.timezone.utc).isoformat())
    def save():(out/'summary.json').write_text(json.dumps(record,indent=2)+'\n')
    save()
    with(ROOT/'build/project-everything-regression/runtime.lock').open('a+b')as lock:
        lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            if production:
                prepare=[sys.executable,str(Path(__file__).with_name('prepare_mi_storage.py')),'--minecraft',mc,'--loader',loader,'--fixture',str(manifest),'--run',args.run+'-'+str(cohort)]
                record['prepare_command']=prepare;save()
                with(out/'prepare.log').open('x')as stream:subprocess.run(prepare,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,check=True)
                script='run_production_matrix.py';spec=importlib.util.spec_from_file_location('mi_release_launcher',ROOT/'functional-tests/project-everything-regression'/script)
                launcher=importlib.util.module_from_spec(spec);spec.loader.exec_module(launcher);launcher.check=mi
                previous=sys.argv;sys.argv=[script,'--run',args.run,'--target',args.target,'--cohort',str(cohort),'--prepared']
                try:launcher.main()
                finally:sys.argv=previous
                results=json.loads(report.read_text())
            else:
                results=native_neo(args.run,args.target,manifest,out,lambda profile:mi(profile,mc,loader),enhance=enhance)
                report.write_text(json.dumps(results,indent=2)+'\n')
            assert len(results)==1
            r=results[0];assert r['status']=='passed'and r['exit_code']==0 and r['results']['mi_storage_features_checked']and r['results']['mi_storage_controls_checked']
            profile=ROOT/r['profile'];assert subjects_match(json.loads((profile/'staged-artifacts.json').read_text())['artifacts'],current_subjects(args.target))
            journal=profile/'mi-storage-release/summary.json';data=json.loads(journal.read_text())
            assert data['complete']and len(data['cases'])==17 and set(data['cases'])=={'machine-modern_industrialization-'+name for name in FLUIDS+OUTPUTS}
            probes=data['probed_cells'];assert set(probes)==set(data['cases'])
            for case,probe in probes.items():
                assert type(probe['native_count'])is int and probe['native_count']>0
                assert probe['indices']==([0,probe['native_count']-1]if probe['native_count']>1 else[0])
                assert probe['kind']==('fluids'if case.removeprefix('machine-modern_industrialization-')in FLUIDS else'items')
            expected_checks=len(FLUIDS)+5*sum(len(v['indices'])for v in probes.values()if v['kind']=='fluids')+9*len(OUTPUTS)+1
            assert set(data['control_capabilities'])==set(data['cases'])
            assert set(data['control_counts'])==set(data['cases'])
            for case,caps in data['control_capabilities'].items():
                assert caps['kind']==('fluids'if case.removeprefix('machine-modern_industrialization-')in FLUIDS else'items')
                expected=expected_counts(caps)
                assert data['control_counts'][case]==dict(checks=expected[0],frames=expected[1])
            counts=[expected_counts(v)for v in data['control_capabilities'].values()]
            expected_checks+=sum(v[0]for v in counts)
            assert data['control_cases']==data['cases']and data['control_frames']==sum(v[1]for v in counts)
            assert data['controls_driver_sha256']==sha(Path(__file__).with_name('mi_storage_controls.py'))
            assert len(data['checks'])==expected_checks and all(v['passed']is True for v in data['checks'])
            assert fingerprints=={str(p.relative_to(ROOT)):sha(p)for p in helpers}
            assert current_subjects(args.target)==record['subjects']
            record.update(status='passed',results=results,acceptance=dict(file=str(journal.relative_to(ROOT)),sha256=sha(journal)))
        except BaseException as error:
            record.update(status='failed',error=repr(error),trace=traceback.format_exc());raise
        finally:
            record['finished_at']=datetime.datetime.now(datetime.timezone.utc).isoformat();save()
            lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)


if __name__=='__main__':main()
