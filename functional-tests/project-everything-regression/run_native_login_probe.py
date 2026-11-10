"""Serial, exact-publisher SIU-absent control for a native login failure.

Diagnostic evidence only: a failed login is never a menu functional pass.
"""
from pathlib import Path
import argparse,datetime,hashlib,json,msvcrt,os,subprocess,sys,time,traceback
from run_mod_matrix import ROOT,finish
from run_native_charm_probe import pins


def bumblezone_login_failure(log):
    return all(v in log for v in ('queen_main_trades_sync_packet',
        'java.lang.IllegalStateException: Stack must be non-empty',
        'Client disconnected with reason:'))


def main():
    p=argparse.ArgumentParser();p.add_argument('--run',required=True);a=p.parse_args()
    mc='26.1.2';fixture='regression-fill-2612-fabric-inventory-batch'
    baseline=ROOT/'build/remaining-ui-audit/gameplay/26.1.2-fabric-siu-project-everything-fill-01-c4-4/staged-artifacts.json'
    profile=ROOT/'build/remaining-ui-audit/gameplay'/(mc+'-fabric-native-'+a.run)
    report=ROOT/'output/project-everything-regression'/(a.run+'-native-login-control.json')
    assert not profile.exists() and not report.exists(),'Preserve previous evidence'
    row={'target':mc+'/fabric','fixture':fixture,'profile':profile.relative_to(ROOT).as_posix(),
         'scope':'SIU-absent-native-login-diagnostic','status':'waiting-for-serial-runtime-lock',
         'baseline':baseline.relative_to(ROOT).as_posix(),'checks':[]}
    def save():report.write_text(json.dumps(row,indent=2)+'\n')
    save()
    with (ROOT/'build/project-everything-regression/runtime.lock').open('a+b') as lock:
        while True:
            try:lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1);break
            except OSError:time.sleep(2)
        try:
            row.update(status='preparing',started_at=datetime.datetime.now(datetime.timezone.utc).isoformat());save()
            prep=ROOT/'build/project-everything-regression'/(a.run+'-prepare.log')
            with prep.open('x') as log:
                native_source=ROOT/'build/all-target-rollout/native-observers'/('java-'+mc)/'com/siu_reference/NativeUiReference.java'
                text=native_source.read_text()
                assert 'com.salts_inventory_update' not in text,'Use the SIU-free native observer'
                command=[sys.executable,'-c',
                    'import pathlib,sys;sys.path.insert(0,"functional-tests/project-everything-regression");'
                    'from prepare_production import prepare_named;'
                    'source=pathlib.Path(sys.argv[4]);'
                    'prepare_named(sys.argv[1],pathlib.Path(sys.argv[2]),sys.argv[3],pathlib.Path(sys.argv[5]),lambda side:(source,source.read_text()),side="native")',
                    mc,str(ROOT/'build/all-target-rollout/fixtures'/fixture/'fixture.json'),a.run,str(native_source),
                    str(ROOT/'build/project-everything-regression/production'/(mc+'-fabric-classpath.json'))]
                subprocess.run(command,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,check=True)
            staged=json.loads((profile/'staged-artifacts.json').read_text())
            assert staged['siu_loaded'] is False and not any(v.get('family') in {'core','compatibility'} for v in staged['artifacts'])
            assert pins(staged)==pins(json.loads(baseline.read_text())),'Publisher/API pin mismatch'
            row['checks'].append({'check':'exact-publisher-and-runtime-api-binaries-with-SIU-absent','passed':True})
            launch=json.loads((profile/'launch.json').read_text());logpath=Path(launch['log'])
            from display_fixture import configure
            configure(Path(launch['cwd']),profile)
            with logpath.open('w') as log:
                process=subprocess.Popen([launch['java'],'@'+launch['arguments']],cwd=launch['cwd'],
                    env={**os.environ,**launch['environment']},stdout=log,stderr=subprocess.STDOUT,
                    creationflags=subprocess.CREATE_NO_WINDOW)
                row.update(status='starting',pid=process.pid);save()
                try:
                    deadline=time.monotonic()+600
                    while True:
                        observed=logpath.read_text(encoding='utf-8',errors='replace')
                        if bumblezone_login_failure(observed):
                            row.update(status='failed-native-login',same_native_packet_failure=True)
                            break
                        if (profile/'ready.json').exists():
                            row.update(status='native-login-ready',same_native_packet_failure=False)
                            break
                        if process.poll() is not None:raise RuntimeError('Native login client exited '+str(process.returncode))
                        if time.monotonic()>deadline:raise TimeoutError('Native login control did not become ready')
                        time.sleep(1)
                finally:
                    finish(profile)
                    if not (profile/'ready.json').exists() and process.poll() is None:
                        # This is our Popen handle, after a terminal failure or
                        # timeout. Do not wait for an observer in a closed world.
                        process.terminate();row['cleanup']='terminated-owned-unready-diagnostic-client'
                    try:row['exit_code']=process.wait(timeout=120)
                    except subprocess.TimeoutExpired:
                        subprocess.run(['taskkill','/PID',str(process.pid),'/T','/F'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
                        row.update(exit_code='terminated-owned-diagnostic-after-timeout',cleanup='owned-tree-timeout')
            row['log']=logpath.relative_to(ROOT).as_posix()
            row['log_sha256']=hashlib.sha256(logpath.read_bytes()).hexdigest()
        except Exception as error:row.update(status='diagnostic-error',error=repr(error),trace=traceback.format_exc())
        finally:
            row['finished_at']=datetime.datetime.now(datetime.timezone.utc).isoformat();save()
            msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)
    print(json.dumps(row),flush=True)


if __name__=='__main__':main()
