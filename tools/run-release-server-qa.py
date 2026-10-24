"""Boot and cleanly stop a loopback-only dedicated server from shipping mod archives."""
import argparse,importlib.util,json,re,shutil,socket,subprocess,sys,time
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('rcon',ROOT/'tools/verify-spatial-server.py')
native=importlib.util.module_from_spec(spec);spec.loader.exec_module(native)
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True)
p.add_argument('--scenario',choices=['core','extension-empty','ae2','toms','sophisticated'],required=True);p.add_argument('--run-id',required=True)
p.add_argument('--accessory',choices=['trinkets','curios'])
p.add_argument('--upstream-only',action='store_true',help='Diagnostic baseline without either Salts archive')
p.add_argument('--production',action='store_true',help='Use the official production Forge installation')
a=p.parse_args()
if a.upstream_only and a.scenario=='core':raise ValueError('Upstream-only requires an integration scenario')
if a.production:
    if (a.minecraft,a.loader)!=('1.20.1','forge'):raise ValueError('Production backend currently supports Forge 1.20.1')
    command=[sys.executable,str(ROOT/'tools/run-forge-production-server-qa.py'),'--scenario',a.scenario,'--run-id',a.run_id]
    if a.accessory:command+=['--accessory',a.accessory]
    if a.upstream_only:command+=['--upstream-only']
    raise SystemExit(subprocess.run(command,cwd=ROOT).returncode)
if not re.fullmatch('[a-z0-9-]+',a.run_id):raise ValueError('Invalid run ID')
with socket.socket() as check_port:
    check_port.bind(('127.0.0.1',25589))
output=ROOT/f'build/gameplay-qa/packaged-native/{a.minecraft}-{a.loader}-{a.run_id}'
output.mkdir(parents=True,exist_ok=True)
command=[shutil.which('pwsh'),'-NoProfile','-File',str(ROOT/'functional-tests/scripts/Run-ReleaseGameplayQa.ps1'),
    '-Minecraft',a.minecraft,'-Loader',a.loader,'-Scenario',a.scenario,'-DedicatedServer','-RunId',a.run_id]
if a.accessory: command+=['-Accessory',a.accessory]
if a.upstream_only: command+=['-UpstreamOnly']
with (output/'supervisor.log').open('w',encoding='utf-8') as stream:
    child=subprocess.Popen(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
    deadline=time.monotonic()+600
    try:
        while True:
            log=(output/'launch.log').read_text(encoding='utf-8',errors='replace') if (output/'launch.log').exists() else ''
            if re.search(r'Done \([^)]+\)!',log):break
            if child.poll() is not None:raise RuntimeError('Server exited before ready: '+str(output))
            if time.monotonic()>deadline:raise TimeoutError('Server readiness: '+str(output))
            time.sleep(1)
        rcon=native.Rcon();rcon.port=25589;rcon.password='release-local-qa-only';rcon.command_timeout=120
        with rcon as r:
            commands={name:r.command(value) for name,value in [('list','list'),('save','save-all flush'),('stop','stop')]}
        code=child.wait(timeout=180)
        if code:raise RuntimeError('Server did not stop cleanly: '+str(output))
        final=(output/'launch.log').read_text(encoding='utf-8',errors='replace')
        assert 'Unable to initialize optional integration' not in final
        assert 'Mixin apply failed salts_inventory' not in final
        assert not re.search(r"Couldn.t load (mod/)?salts_inventory_(update|compat|controlify) pack metadata", final)
        assert 'Attempted to load class' not in final
        if 'DEDICATED_RELEASE_SERVER_STOPPED' not in final:raise AssertionError('Missing clean completion')
        if a.scenario=='ae2' and not a.upstream_only and not re.search(r'Verified \d+ (?:native )?AE2 desktop hooks',final):
            raise AssertionError('AE2 server booted without activating its verified native contract')
        mods=ROOT/f'versions/{a.minecraft}/{a.loader}/build/runtime-smoke/chest-close-{a.run_id}/mods'
        names=sorted(f.name for f in mods.glob('*.jar'))
        if any('qa-' in n or 'controlify' in n for n in names):raise AssertionError(names)
        fixture=json.loads((output/'upstream-fixtures.json').read_text())
        if a.upstream_only and any(n.startswith('salts_inventory_') for n in names):raise AssertionError(names)
        expected=(0 if a.upstream_only else 1 if a.scenario=='core' else 2)+len([f for f in fixture['artifacts'] if f.get('coordinate')])
        if len(names)!=expected:raise AssertionError(dict(expected=expected,actual=names))
        for artifact in fixture['artifacts']:
            if artifact.get('coordinate') and Path(artifact['filename']).name not in names:
                raise AssertionError('Missing upstream server archive: '+artifact['filename'])
        (output/'server-results.json').write_text(json.dumps(dict(passed=True,minecraft=a.minecraft,loader=a.loader,
            scenario=a.scenario,accessory=a.accessory,upstream_only=a.upstream_only,mods=names,commands=commands),indent=2)+'\n')
        print('DEDICATED RELEASE SERVER PASS',a.minecraft,a.loader,a.scenario,output)
    finally:
        if child.poll() is None:
            try:
                rcon=native.Rcon();rcon.port=25589;rcon.password='release-local-qa-only';rcon.command_timeout=120
                with rcon as r:r.command('stop')
                child.wait(timeout=60)
            except Exception:
                subprocess.run(['taskkill','/PID',str(child.pid),'/T','/F'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
