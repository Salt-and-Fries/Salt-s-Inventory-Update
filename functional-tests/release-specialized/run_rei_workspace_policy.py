"""Run the original two native REI mod-compat profiles on final release jars."""
from pathlib import Path
import argparse,datetime,importlib.util,json,msvcrt,subprocess,sys,traceback,zipfile
from urllib.parse import unquote,urlparse

ROOT=Path(__file__).resolve().parents[2]
sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'functional-tests/project-everything-regression')]
from focused_release_evidence import checked,reference
from full_suite_provenance import sources,bundle,verify_bundle,sha
from release_candidate_identity import current
from rei_workspace_policy_acceptance import TARGETS,verify_observation

spec=importlib.util.spec_from_file_location('original_REI_native_controller_validator',ROOT/'tools/verify-controlify-compat-runtime.py')
validator=importlib.util.module_from_spec(spec);spec.loader.exec_module(validator)


def key(target,run):return target.replace('/','-')+'-mod-compat-extended-rei-'+run


def subjects_for(target):
    mc,loader=target.split('/')
    ledger=json.loads((ROOT/'output/release-readiness/candidate-integrity.json').read_text())
    assert ledger['status']=='passed'and len(ledger['artifacts'])==36
    rows=[v for v in ledger['artifacts']if v['minecraft']==mc and v['loader']==loader]
    assert len(rows)==3 and {v['family']for v in rows}=={'core','compatibility','controlify'}
    result={}
    for row in rows:
        path=ROOT/row['file'];assert sha(path)==row['sha256']
        result[row['family']]=(path.name,row['sha256'])
    return result


def command(target,run):
    mc,loader=target.split('/')
    return ['cmd.exe','/d','/c','gradlew.bat','-I','controlify/functional-tests/controlify-qa.init.gradle',
        f':controlify:{mc}:{loader}:runPackagedControlifyQa','-PcontrolifyQaScope=mod-compat','-PcontrolifyQaExtension=true',
        '-PcontrolifyQaRunId='+run,'-PcontrolifyQaJvmVendor=Microsoft','-PcontrolifyQaFixtures=rei',
        '--no-parallel','--max-workers=2','--console=plain']


def observe(root,record,artifacts=None):
    root=Path(root).resolve();folder=root/'build/controlify-qa'/key(record['target'],record['run'])
    original=validator.validate(folder,root/'dist'if artifacts is None else Path(artifacts))
    report_path=folder/'results.json';report=json.loads(report_path.read_text())
    stage=folder/'staged-artifacts.json';staged=json.loads(stage.read_text())
    native=root/original['runtime_profile'];config=native/'config/roughlyenoughitems/config.json5'
    extra=verify_observation(report,checked(root,record['launch_log']).read_text(errors='replace'),config.read_text())
    loaded={}
    for field in ['coreCodeSource','companionCodeSource','controlifyCodeSource']:
        p=Path(unquote(urlparse(report[field]).path).lstrip('/')if sys.platform=='win32'else unquote(urlparse(report[field]).path)).resolve()
        assert p.is_relative_to(native.resolve())and p.is_file();loaded[field]=reference(root,p)
    matches=[v for v in staged['artifacts']if v['file']==record['subjects']['compatibility'][0]]
    assert len(matches)==1
    compat=matches[0]
    actual=native/'mods'/compat['file'];assert sha(actual)==compat['sha256']==record['subjects']['compatibility'][1]
    with zipfile.ZipFile(actual)as z:
        policy=z.read('com/salts_inventory_update/compat/rei/WorkspaceReiOverlayPolicy.class')
        assert b'com/salts_inventory_update/api/client/recipebrowser/DesktopRecipeBrowserScreen'in policy
    return dict(target=record['target'],original_validator=original,additional_observation=extra,
        original_report=reference(root,report_path),original_staging=reference(root,stage),actual_REI_config=reference(root,config),
        actual_loaded_code_sources=loaded,compatibility_loaded_code_source='not separately measured by the original native report; exact staged runtime jar and loaded mod ID are independently checked',
        actual_runtime_compatibility=reference(root,actual),scope=__doc__)


def verify_run(root,record,expected):
    root=Path(root).resolve();assert record['target']in TARGETS and record['status']=='passed'and type(record['exit_code'])is int and record['exit_code']==0
    assert record['subjects']=={k:list(v)for k,v in expected.items()}and set(expected)=={'core','compatibility','controlify'}
    assert subjects_for(record['target'])=={k:tuple(v)for k,v in expected.items()}
    assert record['command']==command(record['target'],record['run'])
    verify_bundle(root,record['qa_source_archive'],record['qa_inputs'])
    checked(root,record['launch_log']);verify_bundle(root,record['runtime_archive'],record['runtime_files'])
    assert observe(root,record)==json.loads(checked(root,record['acceptance']).read_text())
    return True


def verify_retained_run(root,record,expected,artifacts):
    """Validate actual historical execution against exact preserved build15 jars."""
    root=Path(root).resolve();artifacts=Path(artifacts).resolve()
    assert root==ROOT and artifacts==root/'output/release-readiness/normal-build15-subjects-preserved-01/dist'
    assert record['target']in TARGETS and record['status']=='passed'and type(record['exit_code'])is int and record['exit_code']==0
    assert record['subjects']=={k:list(v)for k,v in expected.items()}and set(expected)=={'core','compatibility','controlify'}
    ledger=json.loads((artifacts.parent/'candidate-integrity.json').read_text())
    assert ledger['status']=='passed'and len(ledger['artifacts'])==36
    mc,loader=record['target'].split('/')
    rows=[v for v in ledger['artifacts']if v['minecraft']==mc and v['loader']==loader]
    assert len(rows)==3 and {v['family']for v in rows}==set(expected)
    for item in rows:
        actual=artifacts/item['family']/Path(item['file']).name
        assert sha(actual)==item['sha256']and (actual.name,item['sha256'])==tuple(expected[item['family']])
    assert record['command']==command(record['target'],record['run'])
    verify_bundle(root,record['qa_source_archive'],record['qa_inputs'])
    checked(root,record['launch_log']);verify_bundle(root,record['runtime_archive'],record['runtime_files'])
    assert observe(root,record,artifacts)==json.loads(checked(root,record['acceptance']).read_text())
    return True


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--target',choices=TARGETS,required=True);p.add_argument('--run',required=True);args=p.parse_args()
    assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-'for c in args.run)
    with(ROOT/'build/project-everything-regression/runtime.lock').open('a+b')as lock:
        lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            out=ROOT/'output/release-readiness'/args.run;out.mkdir();destination=out/'summary.json'
            subjects=subjects_for(args.target)
            inputs=sources(ROOT,dict(script='tools/Run-ControlifyReleaseQa.ps1'))
            for f in [Path(__file__),Path(__file__).with_name('rei_workspace_policy_acceptance.py')]:inputs[f.relative_to(ROOT).as_posix()]=sha(f)
            record=dict(status='prepared',target=args.target,run=args.run,subjects=subjects,command=command(args.target,args.run),
                qa_inputs=inputs,qa_source_archive=bundle(ROOT,inputs),candidate=current(ROOT,verify_worktree=True),started=datetime.datetime.now(datetime.timezone.utc).isoformat())
            def save():destination.write_text(json.dumps(record,indent=2)+'\n')
            save()
            try:
                log=out/'native-launch.log';record['status']='running';save()
                with log.open('x')as stream:process=subprocess.run(record['command'],cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT)
                record.update(exit_code=process.returncode,launch_log=reference(ROOT,log));save()
                assert process.returncode==0,'Actual native REI workflow failure; preserve and inspect'
                folder=ROOT/'build/controlify-qa'/key(args.target,args.run)
                acceptance=observe(ROOT,record)
                profile=ROOT/acceptance['original_validator']['runtime_profile']
                files={f.relative_to(ROOT).as_posix():sha(f)for f in folder.rglob('*')if f.is_file()}
                for directory in [profile/'mods',profile/'config',profile/'logs',profile/'.fabric/processedMods']:
                    if directory.is_dir():files.update({f.relative_to(ROOT).as_posix():sha(f)for f in directory.rglob('*')if f.is_file()})
                # Original prelaunch staging records the QA classpath; copy its
                # actual outputs after this run, without claiming a pre-run hash.
                staged=json.loads((folder/'staged-artifacts.json').read_text())
                for directory in staged['projectClasspath']:
                    path=Path(directory).resolve();assert path.is_relative_to(ROOT)and path.is_dir()
                    files.update({f.relative_to(ROOT).as_posix():sha(f)for f in path.rglob('*')if f.is_file()})
                record.update(runtime_files=files,runtime_archive=bundle(ROOT,files,'qa-binaries'),
                    QA_binary_scope='Original prelaunch classpath declaration and post-run file bytes; no pre-run binary checksum claim')
                path=out/'acceptance.json';path.write_text(json.dumps(acceptance,indent=2)+'\n');record.update(acceptance=reference(ROOT,path),status='passed')
                assert all(sha(ROOT/n)==h for n,h in inputs.items())and subjects_for(args.target)=={k:tuple(v)for k,v in subjects.items()}
                save();verify_run(ROOT,json.loads(destination.read_text()),subjects)
            except BaseException as error:record.update(status='failed',error=repr(error),trace=traceback.format_exc());raise
            finally:record['finished']=datetime.datetime.now(datetime.timezone.utc).isoformat();save()
        finally:lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)


if __name__=='__main__':main()
