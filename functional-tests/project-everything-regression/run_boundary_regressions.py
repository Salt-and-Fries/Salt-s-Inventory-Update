"""Serial, shipping-JAR checks for the integrations migrated onto public APIs."""
from pathlib import Path
import argparse
import datetime
import hashlib
import json
import msvcrt
import os
import shutil
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[2]
from display_fixture import configure
from run_mod_matrix import finish


def fixture(loader='fabric',minecraft='1.20.1'):
    # Use the already reviewed exact releases; do not download newer substitutes.
    folder = ROOT/'build/release-readiness/fixtures'; folder.mkdir(parents=True, exist_ok=True)
    if minecraft=='1.21.1':
        assert loader=='fabric'
        return ROOT/'build/aether-ports/fixture-fabric.json'
    if loader=='forge':
        base=json.loads((ROOT/'build/aether-implementation/fixture-forge-without-aether.json').read_text())
        themed=json.loads((ROOT/'build/all-target-rollout/fixtures/1201-forge-themed-v1/fixture.json').read_text())
        base['artifacts'] += [a for a in themed['artifacts'] if a['filename'].startswith('aether-')]
        base['artifacts'] += json.loads((ROOT/'build/quark-window-design/fixture-oddities.json').read_text())['artifacts']
        base.update(scenario='extension-empty',runtime=themed['runtime'])
        target=folder/'1201-forge-boundary-v1.json'
        if target.exists():assert json.loads(target.read_text())==base,'Pinned boundary fixture changed'
        else:target.write_text(json.dumps(base,indent=2)+'\n')
        return target
    base = json.loads((ROOT/'build/aether-implementation/fixture-fabric-sodium-indium.json').read_text())
    inventory = json.loads((ROOT/'build/all-target-rollout/fixtures/1201-fabric-inventory-batch-v2/fixture.json').read_text())
    selected = [a for a in inventory['artifacts'] if a['filename'].startswith((
        'immersive_aircraft-', 'travelersbackpack-', 'cardinal-components-api-'))]
    base.update(runtime=inventory['runtime'], scenario='extension-empty')
    base['artifacts'] += selected
    # Historical capture manifests did not give these two real publisher
    # artifacts Maven coordinates. Mark explicit roots rather than inventing
    # coordinates or relying on the preparer's implicit coordinate filter.
    for artifact in base['artifacts']:
        if artifact['filename'].startswith(('sodium-', 'indium-')):
            artifact['stage_as_root']=True
    target = folder/'1201-fabric-boundary-render-complete-v3.json'
    if target.exists(): assert json.loads(target.read_text()) == base, 'Pinned boundary fixture changed'
    else: target.write_text(json.dumps(base, indent=2)+'\n')
    return target


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', required=True)
    parser.add_argument('--target', choices=['1.20.1/fabric','1.20.1/forge','1.21.1/fabric'], default='1.20.1/fabric')
    parser.add_argument('--core-candidate', type=Path, help='Normal mapped production jar; staged only in this disposable runtime')
    parser.add_argument('--compat-candidate', type=Path, help='Normal mapped compatibility candidate in this disposable runtime')
    args = parser.parse_args()
    minecraft,loader=args.target.split('/')
    assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.run)
    output = ROOT/'output/release-readiness'/args.run
    assert not output.exists(), 'Preserve every attempt: choose a fresh run ID'
    output.mkdir(parents=True)
    profile = ROOT/'build/remaining-ui-audit/gameplay'/(minecraft+'-'+loader+'-siu-'+args.run)
    record = dict(status='running', started_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),
                  target=args.target, profile=str(profile.relative_to(ROOT)), checks=[])
    def save(): (output/'summary.json').write_text(json.dumps(record, indent=2)+'\n')
    save()
    lock_path = ROOT/'build/project-everything-regression/runtime.lock'
    with lock_path.open('a+b') as lock:
        lock.seek(0); lock.write(b'0'); lock.flush(); lock.seek(0)
        msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
        try:
            command = [sys.executable, str(Path(__file__).with_name('prepare_production.py')),
                       '--minecraft',minecraft,'--loader',loader,'--fixture',str(fixture(loader,minecraft)),'--run',args.run,'--boundary-observer']
            record['prepare_command'] = command; save()
            with (output/'prepare.log').open('w') as log:
                subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, check=True)
            launch = json.loads((profile/'launch.json').read_text())
            for family,candidate in [('core',args.core_candidate),('compatibility',args.compat_candidate)]:
                if not candidate:continue
                staged_path=profile/'staged-artifacts.json';staged=json.loads(staged_path.read_text())
                artifact=next(r for r in staged['artifacts'] if r.get('family')==family)
                assert candidate.name==artifact['filename']
                shutil.copy2(candidate,Path(launch['cwd'])/'mods'/artifact['filename'])
                artifact.update(file=str(candidate.resolve()),sha256=hashlib.sha256(candidate.read_bytes()).hexdigest(),
                    sha512=hashlib.sha512(candidate.read_bytes()).hexdigest(),scope='Normal mapped production candidate; distribution unchanged')
                staged_path.write_text(json.dumps(staged,indent=2)+'\n')
            record['subjects'] = json.loads((profile/'staged-artifacts.json').read_text())['artifacts']; save()
            pinned=json.loads(fixture(loader,minecraft).read_text())['artifacts']
            assert {r['filename'] for r in pinned} <= {r['filename'] for r in record['subjects']}, 'Required root publisher was omitted during staging'
            for artifact in record['subjects']:
                runtime = Path(launch['cwd'])/'mods'/artifact['filename']
                if artifact.get('family') in ['core','compatibility']:
                    assert hashlib.sha256(runtime.read_bytes()).hexdigest() == artifact['sha256']
            configure(Path(launch['cwd']), profile)
            with (output/'launch.log').open('w') as log:
                proc = subprocess.Popen([launch['java'],'@'+launch['arguments']], cwd=launch['cwd'],
                    env={**os.environ,**launch['environment']}, stdout=log, stderr=subprocess.STDOUT,
                    creationflags=subprocess.CREATE_NO_WINDOW)
                record['pid'] = proc.pid; save()
                try:
                    deadline = time.monotonic()+600
                    while not (profile/'ready.json').exists():
                        if proc.poll() is not None: raise RuntimeError('Client exited '+str(proc.returncode))
                        if time.monotonic()>deadline: raise TimeoutError('Client readiness')
                        time.sleep(1)
                    jobs = [
                        ('aether', [sys.executable,str(ROOT/'functional-tests/aether-windows/character.py'),str(profile),'--loader',loader,'--controller'], {}),
                        ('aircraft', [sys.executable,str(ROOT/'functional-tests/aircraft-window/side_controls.py')],
                         {'AIRCRAFT_RUN':profile.name,'AIRCRAFT_EVIDENCE':str(output.relative_to(ROOT)/'aircraft')}),
                        ('travelers', [sys.executable,str(Path(__file__).with_name('check_mod_interactions.py')),str(profile),
                         '--minecraft','1.20.1','--loader','fabric','--only','travelers-backpack','--only','travelers-block'], {})]
                    if loader=='forge' or minecraft=='1.21.1':jobs=jobs[:1] # Forge also checks Quark ownership/movement.
                    for name, command, extra in jobs:
                        print('START',name,flush=True)
                        with (output/(name+'.log')).open('w') as checks:
                            result = subprocess.run(command,cwd=ROOT,env={**os.environ,**extra},stdout=checks,stderr=subprocess.STDOUT)
                        record['checks'].append(dict(name=name,command=command,exit_code=result.returncode)); save()
                        if result.returncode: raise RuntimeError('Focused '+name+' checks failed')
                        if name=='travelers':
                            proof=json.loads((profile/'mod-interaction-checks.json').read_text())
                            assert proof['cases'] and all(r['status']=='passed' for r in proof['cases']), 'Traveler workflow must actually execute transfers'
                finally:
                    finish(profile)
                    try: record['exit_code'] = proc.wait(timeout=120)
                    except subprocess.TimeoutExpired:
                        subprocess.run(['taskkill','/PID',str(proc.pid),'/T','/F'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
                        record['exit_code']='terminated-owned-runtime-after-timeout'
            assert record['exit_code']==0
            record['status']='passed'
        except Exception as error:
            record.update(status='failed',error=repr(error)); raise
        finally:
            record['finished_at']=datetime.datetime.now(datetime.timezone.utc).isoformat(); save()
            lock.seek(0); msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)


if __name__=='__main__': main()
