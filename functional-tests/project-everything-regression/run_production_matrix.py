"""Run mixed publisher cohorts using original Fabric release jars, supervised."""
from pathlib import Path
import argparse, json, os, subprocess, sys, time, traceback
from check_mod_interactions import run as check
from run_mod_matrix import FIXTURES, finish
ROOT = Path(__file__).resolve().parents[2]

def initialize_default_cache(profile,launch,loader):
    if loader!='forge':return
    import hashlib,shutil
    folder=Path(__file__).with_name('fixtures')
    proof=json.loads((folder/'forge-sophisticated-default-cache.json').read_text())
    staged=json.loads((profile/'staged-artifacts.json').read_text())['artifacts']
    installed={a['sha256'] for a in staged}
    if not any('sophisticatedcore-' in a.get('filename','') for a in staged):return
    known={a['sha256'] for a in proof['artifacts']}
    assert all(a['sha256'] in known for a in staged if a.get('family')=='upstream' and any(
        s in a.get('filename','') for s in ['sophisticatedcore-','sophisticatedstorage-','sophisticatedbackpacks-'])), 'Default cache requires the exact Sophisticated publisher cohort'
    source=folder/'forge-sophisticatedcore-common.toml'
    assert hashlib.sha256(source.read_bytes()).hexdigest()==proof['config_sha256']
    target=Path(launch['cwd'])/'config/sophisticatedcore-common.toml';target.parent.mkdir(exist_ok=True)
    assert not target.exists(),'Do not replace existing settings'
    shutil.copy2(source,target)
    (profile/'publisher-default-cache.json').write_text(json.dumps(proof,indent=2))

def main():
    p = argparse.ArgumentParser(); p.add_argument('--run', required=True); p.add_argument('--target', action='append'); p.add_argument('--cohort', type=int); p.add_argument('--prepared', action='store_true'); p.add_argument('--only', action='append', default=[]);p.add_argument('--max-cases',type=int);p.add_argument('--mod',action='append',default=[]);p.add_argument('--desktop-debug',action='store_true');p.add_argument('--revision-debug-port',type=int);p.add_argument('--revision-stack-agent',action='store_true');p.add_argument('--fixture-manifest',type=Path,help='Explicit preserved dependency-complete partition; requires one target/cohort'); a = p.parse_args()
    if a.fixture_manifest:assert a.target and len(a.target)==1 and a.cohort is not None and a.fixture_manifest.is_file()
    report = ROOT / 'output/project-everything-regression' / (a.run + '-production-mod-matrix.json'); rows = []
    def save(): report.write_text(json.dumps(rows, indent=2) + '\n')
    for target, fixtures in FIXTURES.items():
        mc, loader = target.split('/')
        if loader not in {'fabric','forge'} or a.target and target not in a.target: continue
        for i, fixture in enumerate(fixtures):
            if a.cohort is not None and i != a.cohort: continue
            if fixture == '1201-fabric-regression-inventory-no-guard': fixture = '1201-fabric-inventory-batch-v2'
            manifest=a.fixture_manifest.resolve() if a.fixture_manifest else ROOT / 'build/all-target-rollout/fixtures' / fixture / 'fixture.json'
            run = f'{a.run}-{i}'; profile = ROOT / 'build/remaining-ui-audit/gameplay' / f'{mc}-{loader}-siu-{run}'
            row = {'target': target, 'fixture': fixture, 'profile': str(profile.relative_to(ROOT)), 'status': 'preparing'}; rows.append(row); save(); print('START', target, fixture, flush=True)
            if a.fixture_manifest:
                import hashlib
                row['fixture_partition']={'file':str(manifest.relative_to(ROOT)),'sha256':hashlib.sha256(manifest.read_bytes()).hexdigest(),
                    'recovery':json.loads(manifest.read_text()).get('setup_recovery')};save()
            prep_log = ROOT / 'build/project-everything-regression' / f'{mc}-{loader}-{run}-prepare.log'
            try:
                if not a.prepared:
                    with prep_log.open('w') as log:
                        subprocess.run([sys.executable, str(ROOT / 'functional-tests/project-everything-regression/prepare_production.py'), '--minecraft', mc,'--loader',loader,
                            '--fixture', str(manifest), '--run', run], cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, check=True)
                launch = json.loads((profile / 'launch.json').read_text()); assert not (profile / 'pid.txt').exists()
                import hashlib
                for artifact in json.loads((profile/'staged-artifacts.json').read_text())['artifacts']:
                    if artifact.get('family') in ['core','compatibility']:
                        original=Path(artifact['file']);runtime=Path(launch['cwd'])/'mods'/artifact['filename']
                        assert hashlib.sha256(original.read_bytes()).hexdigest()==artifact['sha256'],'Prepared shipping artifact changed'
                        assert hashlib.sha256(runtime.read_bytes()).hexdigest()==artifact['sha256'],'Runtime shipping artifact changed'
                initialize_default_cache(profile,launch,loader)
                from display_fixture import configure
                configure(Path(launch['cwd']),profile)
                with Path(launch['log']).open('w') as log:
                    row['desktop_debug']=a.desktop_debug
                    row['jvm_error_file']=str(profile/'hs_err_pid%p.log')
                    revision_args=[]
                    if a.revision_stack_agent:
                        assert target=='1.20.1/fabric' and not a.revision_debug_port
                        from revision_diagnostics import prepare
                        agent,_=prepare(profile,launch['java']);revision_args=['-javaagent:'+str(agent)]
                        row['debugger_scope']='Bounded native revision stack logging agent. Diagnostic only; not functional pass evidence.'
                    if a.revision_debug_port:
                        assert target=='1.20.1/fabric' and 1024<=a.revision_debug_port<=65535
                        revision_args=['-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:'+str(a.revision_debug_port)]
                        row['debugger_scope']='Read-only native revision stack diagnosis; breakpoints alter timing. This run is diagnostic only.'
                    java_args=[launch['java'],'-XX:ErrorFile='+row['jvm_error_file']]+revision_args+(['-Dsalts_inventory_update.desktopDebug=true'] if a.desktop_debug else [])+['@'+launch['arguments']]
                    process = subprocess.Popen(java_args, cwd=launch['cwd'], env={**os.environ, **launch['environment']}, stdout=log, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
                    (profile / 'pid.txt').write_text(str(process.pid)); row['pid'] = process.pid; row['status'] = 'starting'; save()
                    revision_trace=None
                    try:
                        deadline = time.monotonic() + 600
                        while not (profile / 'ready.json').exists():
                            if process.poll() is not None: raise RuntimeError('Production client exited ' + str(process.returncode))
                            from run_native_login_probe import bumblezone_login_failure
                            if bumblezone_login_failure(Path(launch['log']).read_text(encoding='utf-8',errors='replace')):
                                row['startup_failure']='Bumblezone queen-trade packet rejected an empty stack and the native client disconnected before QA readiness.'
                                process.terminate()
                                row['cleanup']='terminated-owned-client-after-terminal-native-login-failure'
                                raise RuntimeError(row['startup_failure'])
                            if time.monotonic() > deadline: raise TimeoutError('No ready marker after ten minutes')
                            time.sleep(1)
                        row['status'] = 'testing'; save()
                        if a.revision_debug_port:
                            trace_path=profile/'native-revision-stacks.json'
                            with (profile/'native-revision-trace.log').open('w') as trace_log:
                                revision_trace=subprocess.Popen([launch['java'],'--add-modules','jdk.jdi',str(ROOT/'functional-tests/project-everything-regression/MenuRevisionTrace.java'),str(a.revision_debug_port),str(trace_path)],cwd=ROOT,stdout=trace_log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
                            attach_deadline=time.monotonic()+30
                            while not Path(str(trace_path)+'.ready').exists():
                                if revision_trace.poll() is not None:raise RuntimeError('Revision debugger failed to attach; see native-revision-trace.log')
                                if time.monotonic()>attach_deadline:raise TimeoutError('Revision debugger attachment timed out')
                                time.sleep(.2)
                        row['results'] = check(profile, mc, loader, a.only,a.max_cases,a.mod)
                        row['status'] = ('coverage-gap' if not row['results']['cases'] else
                                         'passed' if row['results']['failed'] == 0 else 'failed')
                    finally:
                        if revision_trace is not None:
                            try:revision_trace.wait(timeout=65)
                            except subprocess.TimeoutExpired:revision_trace.kill();revision_trace.wait()
                        finish(profile)
                        try: row['exit_code'] = process.wait(timeout=120)
                        except subprocess.TimeoutExpired:
                            subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                            row.update(status='failed', exit_code='terminated-owned-launch-after-timeout')
                        if row.get('exit_code') != 0: row['status'] = 'failed'
            except Exception as error: row.update(status='failed', error=repr(error), trace=traceback.format_exc())
            save(); print('END', target, fixture, row['status'], flush=True)
    if any(r['status'] != 'passed' for r in rows): raise SystemExit(1)

if __name__ == '__main__': main()
