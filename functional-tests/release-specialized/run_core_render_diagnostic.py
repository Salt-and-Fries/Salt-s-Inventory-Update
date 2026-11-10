"""Focused ordinary Core smoke with real detailed logging, preserving original QA."""
from pathlib import Path
import argparse,datetime,json,msvcrt,subprocess,sys,traceback,zipfile

ROOT=Path(__file__).resolve().parents[2]
sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'functional-tests/project-everything-regression')]
import full_suite_provenance as provenance
from focused_release_evidence import reference,checked
from release_candidate_identity import current
from subject_artifacts import current_subjects
from core_render_diagnostic import BASE,CONFIG,WANTED,script,init_source,prepare,validate_log,loaded_literal,sha

TARGETS=[v['minecraft']+'/'+v['loader']for v in json.loads((ROOT/'compatibility/coverage-targets.json').read_text())['targets']]


def descriptor(target,subjects):
    mc,loader=target.split('/')
    return dict(name='focused-core-detailed-render',minecraft=mc,loader=loader,script=BASE,
        args=['-Minecraft',mc,'-Loader',loader,'-Scenario','core'],artifacts={'dist/core/'+subjects['core'][0]:subjects['core'][1]})


def observe(root,summary):
    root=Path(root).resolve();case=summary['base_case'];folder,scenario=provenance.ordinary_report(root,case,summary)
    report_path=folder/'results.json';report=json.loads(report_path.read_text());runtime=Path(report['runDirectory']).resolve()
    assert(report['minecraft'],report['loader'])==tuple(summary['target'].split('/'))
    assert scenario=='core'and report['scenario']=='core'and type(report['failures'])is int and report['failures']==0
    assert report.get('completeCoreFeatureSuite',False)is False,'This is focused ordinary smoke, not a second complete Core suite'
    assert report['results']and any(v['status']=='PASS'for v in report['results'])and not any(v['status']in {'FAIL','FAILED','ERROR'}for v in report['results'])
    receipt=json.loads(checked(root,summary['initial_config_receipt']).read_text())
    assert Path(receipt['runtime']).resolve()==runtime and runtime.is_relative_to(root)
    config=(runtime/CONFIG).resolve();assert config.is_relative_to(runtime)and Path(receipt['config']).resolve()==config
    assert receipt['existingConfigOverwritten']is False and receipt['expected']==WANTED
    snapshot=checked(root,summary['initial_config_snapshot']);assert sha(snapshot)==receipt['initialSha256']and json.loads(snapshot.read_text())==WANTED
    final=json.loads(config.read_text());assert all(final.get(k)is v for k,v in WANTED.items())
    log=folder/'launch.log';text=log.read_text(errors='replace');assert 'DETAILED_RENDER_CONFIG_WRITTEN '+str(config)in text
    assert 'SIU_GAMEPLAY_SUMMARY status=PASS'in text
    rendering=validate_log(text)
    loaded=summary['runtime_evidence']['loaded_code_sources']['coreCodeSource'];loaded_path=checked(root,loaded)
    loaded_literal(loaded_path)
    return dict(target=summary['target'],original_smoke_pass_records=sum(v['status']=='PASS'for v in report['results']),original_result_records=len(report['results']),
        final_config=reference(root,config),inner_log=reference(root,log),rendering=rendering,
        loaded_corrected_class=reference(root,loaded_path),original_report=reference(root,report_path),
        scope='Original ordinary Core smoke assertions plus actual detailed-log configuration/rows and loaded corrected class. No complete functional/gameplay claim or automatic old/new acceptance.')


def verify_run(root,summary,expected):
    root=Path(root).resolve();assert root==ROOT and summary['status']=='passed'and type(summary['exit_code'])is int and summary['exit_code']==0
    assert summary['target']in TARGETS
    assert summary['subjects']=={k:list(v)for k,v in expected.items()}and set(expected)=={'core'}
    assert summary['base_case']==descriptor(summary['target'],expected)
    provenance.verify_attempt(root,summary,summary['base_case'])
    with zipfile.ZipFile(checked(root,summary['qa_source_archive']))as archive:
        before=archive.read(BASE).decode()
        init_path=checked(root,summary['derived_init']);wrapper=checked(root,summary['executed_wrapper'])
        assert archive.read(init_path.relative_to(root).as_posix()).decode().replace('\r\n','\n')==init_source(root,wrapper.parent)
        assert archive.read(wrapper.relative_to(root).as_posix()).decode().replace('\r\n','\n')==script(before,root,init_path)
    assert observe(root,summary)==json.loads(checked(root,summary['acceptance']).read_text())
    return True


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--target',required=True,choices=TARGETS);p.add_argument('--run',required=True);p.add_argument('--plan-only',action='store_true');a=p.parse_args()
    assert a.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-'for c in a.run)
    mc,loader=a.target.split('/')
    if a.plan_only:
        print(json.dumps(dict(target=a.target,base_wrapper=BASE,complete_core=False,config=WANTED,
            scope='Plan only. Ordinary Core smoke plus actual detailed-render evidence; Java/game not executed.'),indent=2));return
    with(ROOT/'build/project-everything-regression/runtime.lock').open('a+b')as lock:
        lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            expected={'core':current_subjects(a.target)['core']}
            loaded_literal(ROOT/'dist/core'/expected['core'][0])
            out=ROOT/'output/release-readiness'/a.run;assert not out.exists();out.mkdir()
            case=descriptor(a.target,expected)
            record=dict(status='preparing',run_id=a.run,target=a.target,subjects=expected,base_case=case,candidate_identity=current(ROOT,verify_worktree=True),started=datetime.datetime.now(datetime.timezone.utc).isoformat())
            def save():(out/'summary.json').write_text(json.dumps(record,indent=2)+'\n')
            save()
            try:
                wrapper,init=prepare(ROOT,out);inputs=provenance.sources(ROOT,case)
                for path in [Path(__file__),Path(__file__).with_name('core_render_diagnostic.py'),wrapper,init]:inputs[path.relative_to(ROOT).as_posix()]=sha(path)
                record.update(status='running',qa_provenance_schema=provenance.SCHEMA,qa_inputs=inputs,qa_source_archive=provenance.bundle(ROOT,inputs),
                    executed_wrapper=reference(ROOT,wrapper),derived_init=reference(ROOT,init),
                    wrapper_derivation='Canonical original assertions and launch flags retained. Only absolute repo location and additional isolated config init differ.')
                args=['pwsh','-NoProfile','-File',str(wrapper),'-Minecraft',mc,'-Loader',loader,'-Scenario','core','-RunId',a.run]
                record['command']=args;save();log=out/'driver.log'
                with log.open('x')as stream:result=subprocess.run(args,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT)
                record.update(exit_code=result.returncode,log=log.relative_to(ROOT).as_posix(),log_sha256=sha(log));save();assert result.returncode==0,'Preserved detailed-render smoke failure'
                record.update(initial_config_receipt=reference(ROOT,out/'initial-config-receipt.json'),initial_config_snapshot=reference(ROOT,out/'initial-config.json'),
                    runtime_evidence=provenance.ordinary_gameplay_evidence(ROOT,case,record))
                acceptance=out/'render-log-acceptance.json';acceptance.write_text(json.dumps(observe(ROOT,record),indent=2)+'\n')
                assert inputs=={n:sha(ROOT/n)for n in inputs}and current_subjects(a.target)['core']==expected['core']
                record.update(status='passed',acceptance=reference(ROOT,acceptance));save()
                verify_run(ROOT,json.loads((out/'summary.json').read_text()),expected)
            except BaseException as error:record.update(status='failed',error=repr(error),trace=traceback.format_exc());raise
            finally:record['finished']=datetime.datetime.now(datetime.timezone.utc).isoformat();save()
        finally:lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)


if __name__=='__main__':main()
