"""Serial exact-publisher Powah5 control qualification on both legacy loaders."""
from pathlib import Path
import argparse,datetime,hashlib,importlib.util,json,msvcrt,subprocess,sys,traceback

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'));sys.path.insert(0,str(ROOT/'tools'))
from run_mod_matrix import FIXTURES
from subject_artifacts import current_subjects,subjects_match
from full_suite_provenance import sources
from powah_legacy_features import run as features,EXPECTED_CHECKS,CASES


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--run',required=True)
    parser.add_argument('--target',choices=['1.20.1/fabric','1.20.1/forge'],required=True);parser.add_argument('--plan-only',action='store_true');args=parser.parse_args()
    assert args.run and all(c in'abcdefghijklmnopqrstuvwxyz0123456789-'for c in args.run)
    mc,loader=args.target.split('/')
    if loader=='forge':
        cohort=0;fixture='publisher-extra-powah-1201-forge';manifest=ROOT/'build/release-readiness/fixtures/publisher-extra-powah-1201-forge.json'
    else:
        selected=[]
        for index,name in enumerate(FIXTURES[args.target]):
            path=ROOT/'build/all-target-rollout/fixtures'/name/'fixture.json'
            if any(v['filename'].lower().startswith('powah-')for v in json.loads(path.read_text())['artifacts']):selected.append((index,name,path))
        assert len(selected)==1;cohort,fixture,manifest=selected[0]
    if args.plan_only:
        print(json.dumps(dict(target=args.target,fixture=fixture,cohort=cohort,manifest=str(manifest.relative_to(ROOT)),
            scope='74 strict checks on four legacy native Powah control/status/card routes; no complete publisher or automation claim'),indent=2));return
    out=ROOT/'output/release-readiness'/args.run;assert not out.exists();out.mkdir()
    hashes=sources(ROOT,dict(script=str(Path(__file__).relative_to(ROOT))))
    inputs=[manifest,*[Path(__file__).with_name(v)for v in ['run_powah_legacy.py','powah_legacy_features.py','powah_legacy_observer.py','prepare_powah_legacy.py','test_powah_legacy_data.py',
        'ae2_settings_features.py','ae2_quantum_features.py','mowzie_features.py']]]
    hashes.update({p.relative_to(ROOT).as_posix():sha(p)for p in inputs})
    record=dict(status='running',target=args.target,fixture=fixture,subjects=current_subjects(args.target),qa_inputs=hashes,
        scope='Four native Powah5 cable/energy-hopper/player-transmitter/solar controls and actual server effects. Entire frozen original QA closure, exact typed actor/cursor/world inventory conservation, native card binding/refusal/recovery, ray-specific face, moved exterior controls, original mode cycles and real solar generation/obstruction; not every energy network or transmitter charging.',
        started_at=datetime.datetime.now(datetime.timezone.utc).isoformat())
    def save():(out/'summary.json').write_text(json.dumps(record,indent=2)+'\n')
    save()
    with(ROOT/'build/project-everything-regression/runtime.lock').open('a+b')as lock:
        lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            prepare=[sys.executable,'functional-tests/release-specialized/prepare_powah_legacy.py','--minecraft',mc,'--loader',loader,'--fixture',str(manifest),'--run',args.run+'-'+str(cohort)]
            record['prepare_command']=prepare;save()
            with(out/'prepare.log').open('x')as stream:subprocess.run(prepare,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,check=True)
            script=ROOT/'functional-tests/project-everything-regression/run_production_matrix.py'
            spec=importlib.util.spec_from_file_location('powah_legacy_raw_launcher',script);launcher=importlib.util.module_from_spec(spec);spec.loader.exec_module(launcher);launcher.check=features
            report=ROOT/'output/project-everything-regression'/(args.run+'-production-mod-matrix.json');assert not report.exists()
            previous=sys.argv;sys.argv=[str(script),'--run',args.run,'--target',args.target,'--cohort',str(cohort),'--prepared','--fixture-manifest',str(manifest)]
            try:launcher.main()
            finally:sys.argv=previous
            results=json.loads(report.read_text());assert len(results)==1;row=results[0]
            assert row['status']=='passed'and row['exit_code']==0 and row['results']['cases']==4 and row['results']['powah_legacy_features_checked']is True
            profile=ROOT/row['profile'];assert subjects_match(json.loads((profile/'staged-artifacts.json').read_text())['artifacts'],current_subjects(args.target))
            journal=profile/'powah-legacy-release/summary.json';data=json.loads(journal.read_text())
            assert data['complete']is True and data['cases']==[v[0]for v in CASES]and len(data['checks'])==EXPECTED_CHECKS and all(v['passed']is True for v in data['checks'])
            assert hashes=={p:sha(ROOT/p)for p in hashes}and current_subjects(args.target)==record['subjects']
            record.update(status='passed',results=results,acceptance=dict(file=str(journal.relative_to(ROOT)),sha256=sha(journal)))
        except BaseException as error:record.update(status='failed',error=repr(error),trace=traceback.format_exc());raise
        finally:
            record['finished_at']=datetime.datetime.now(datetime.timezone.utc).isoformat();save();lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)


if __name__=='__main__':main()
