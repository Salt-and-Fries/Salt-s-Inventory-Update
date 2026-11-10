"""Serial, disposable setup recovery; original publisher and SIU jars stay intact."""
from pathlib import Path
import argparse, contextlib, datetime, hashlib, json, msvcrt, os, subprocess, sys, time, traceback
import importlib.util
from collections import Counter

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
from run_mod_matrix import finish
from check_mod_interactions import run as check
from display_fixture import configure
from run_native_login_probe import bumblezone_login_failure


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--minecraft',required=True)
    p.add_argument('--fixture',required=True,type=Path)
    p.add_argument('--run',required=True)
    p.add_argument('--only',action='append',default=[])
    p.add_argument('--mod',action='append',default=[])
    p.add_argument('--affected-cohort',action='store_true')
    p.add_argument('--after',type=Path,help='Additional focused action scenario after the recorded entry cases')
    a=p.parse_args()
    assert a.only or a.mod or a.affected_cohort,'Select the previously blocked cases or the affected cohort explicitly'
    folder=ROOT/'output/blocked-setup-recovery';folder.mkdir(exist_ok=True)
    build=ROOT/'build/blocked-setup-recovery';build.mkdir(exist_ok=True)
    profile=ROOT/'build/remaining-ui-audit/gameplay'/f'{a.minecraft}-fabric-siu-{a.run}'
    report=folder/(a.run+'.json')
    assert not report.exists() and not profile.exists(),'Preserve historical evidence; use a fresh run ID'
    fixture=json.loads(a.fixture.read_text())
    row={'target':a.minecraft+'/fabric','fixture':str(a.fixture),'recovery':fixture.get('setup_recovery'),
         'profile':str(profile.relative_to(ROOT)),'selection':{'only':a.only,'mods':a.mod,'affected_cohort':a.affected_cohort},
         'scope':'Affected setup/cases only; original full-cohort failure remains preserved.',
         'status':'waiting-for-serial-runtime-lock','started_at':datetime.datetime.now(datetime.timezone.utc).isoformat()}
    def save():report.write_text(json.dumps(row,indent=2)+'\n')
    save()
    with (ROOT/'build/project-everything-regression/runtime.lock').open('a+b') as lock:
        lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0)
        deadline=time.monotonic()+3600
        while True:
            try:
                lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1);break
            except OSError:
                if time.monotonic()>deadline:raise TimeoutError('Another serial test still owns the runtime')
                time.sleep(2)
        try:
            row['status']='preparing';save();print('PREPARING',a.run,flush=True)
            with (build/(a.run+'-prepare.log')).open('x') as log:
                subprocess.run([sys.executable,str(ROOT/'functional-tests/project-everything-regression/prepare_production.py'),
                    '--minecraft',a.minecraft,'--fixture',str(a.fixture.resolve()),'--run',a.run],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,check=True)
            launch=json.loads((profile/'launch.json').read_text())
            configure(Path(launch['cwd']),profile)
            staged=json.loads((profile/'staged-artifacts.json').read_text())
            row['subjects']=[r for r in staged['artifacts'] if r.get('family') in {'core','compatibility'}]
            for artifact in staged['artifacts']:
                source=Path(artifact['file']);actual=Path(launch['cwd'])/'mods'/artifact['filename']
                assert hashlib.sha256(source.read_bytes()).hexdigest()==artifact['sha256']
                assert hashlib.sha256(actual.read_bytes()).hexdigest()==artifact['sha256']
            with Path(launch['log']).open('w') as log:
                process=subprocess.Popen([launch['java'],'@'+launch['arguments']],cwd=launch['cwd'],
                    env={**os.environ,**launch['environment']},stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
                row.update(status='starting',pid=process.pid);save()
                try:
                    end=time.monotonic()+600
                    while not (profile/'ready.json').exists():
                        text=Path(launch['log']).read_text(encoding='utf-8',errors='replace')
                        if bumblezone_login_failure(text):
                            row['native_failure']='Bumblezone queen-trade empty-stack packet'
                            raise RuntimeError(row['native_failure'])
                        if process.poll() is not None:raise RuntimeError('Client exited '+str(process.returncode))
                        if time.monotonic()>end:raise TimeoutError('Client did not reach test readiness')
                        time.sleep(1)
                    row['status']='testing';save();print('TESTING',a.run,flush=True)
                    with (build/(a.run+'-checks.log')).open('x') as log,contextlib.redirect_stdout(log):
                        result=check(profile,a.minecraft,'fabric',only=a.only,mods=a.mod)
                        if a.after:
                            spec=importlib.util.spec_from_file_location('focused_recovery_actions',a.after)
                            module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
                            row['additional_scenario']={'file':str(a.after),'sha256':hashlib.sha256(a.after.read_bytes()).hexdigest()}
                            module.run(profile)
                    row['results']=result
                    measured=json.loads((profile/'mod-interaction-checks.json').read_text())
                    row['case_coverage']=dict(Counter(case['status'] for case in measured['cases']))
                    row['assertions']=len(measured['checks'])
                    row['status']=('completed-with-coverage-limits' if any(status!='passed' for status in row['case_coverage']) else 'passed') if result['cases'] and result['failed']==0 else 'failed'
                finally:
                    finish(profile)
                    if not (profile/'ready.json').exists() and process.poll() is None:process.terminate()
                    try:row['exit_code']=process.wait(timeout=120)
                    except subprocess.TimeoutExpired:
                        subprocess.run(['taskkill','/PID',str(process.pid),'/T','/F'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
                        row['exit_code']='terminated-owned-client-after-timeout';row['status']='failed'
                    if row.get('exit_code')!=0:row['status']='failed'
            row['log']=launch['log']
        except Exception as error:
            row.update(status='failed',error=repr(error),trace=traceback.format_exc())
        finally:
            row['finished_at']=datetime.datetime.now(datetime.timezone.utc).isoformat();save()
            lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)
    print('FINISHED',a.run,row['status'],flush=True)
    if row['status'] not in {'passed','completed-with-coverage-limits'}:raise SystemExit(1)


if __name__=='__main__':main()
