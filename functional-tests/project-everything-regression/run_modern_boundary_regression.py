"""Focused named-loader Aether/Quark regression with exact shipping candidates."""
from pathlib import Path
import argparse
import datetime
import hashlib
import importlib.util
import json
import msvcrt
import os
import shutil
import subprocess
import sys
import time

ROOT=Path(__file__).resolve().parents[2]
from run_mod_matrix import finish


def load(name,path):
    spec=importlib.util.spec_from_file_location(name,path);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module


def prepare():
    folder=ROOT/'build/release-readiness/modern-boundary';folder.mkdir(parents=True,exist_ok=True)
    fixture=json.loads((ROOT/'build/aether-ports/fixture-neoforge-final.json').read_text())
    baseline=json.loads((ROOT/'build/all-target-rollout/fixtures/1211-neoforge-inventory-batch-v1/fixture.json').read_text())
    fixture['artifacts'] += [a for a in baseline['artifacts'] if a['filename'].startswith('curios-')]
    fixture.update(scenario='extension-empty',runtime=baseline['runtime'])
    fixture_path=folder/'fixture.json'
    if fixture_path.exists():assert json.loads(fixture_path.read_text())==fixture
    else:fixture_path.write_text(json.dumps(fixture,indent=2)+'\n')
    prep=load('modern_boundary_source',ROOT/'tools/prepare-rollout-production-modern.py');prep.MC='1.21.1'
    sys.path.insert(0,str(ROOT/'functional-tests/aether-windows'))
    aether=load('modern_boundary_aether',ROOT/'functional-tests/aether-windows/prepare_modern.py')
    _,text=prep.qa_source('siu')
    source=folder/'observer/com/siu_reference';source.mkdir(parents=True,exist_ok=True)
    (source/'NativeUiReference.java').write_text(aether.source(text).replace('net.minecraftforge.fml.ModList','net.neoforged.fml.ModList'))
    shutil.copy2(ROOT/'functional-tests/aether-windows/RuntimeNames.java',source/'RuntimeNames.java')
    init=(ROOT/'functional-tests/aether-windows/native.init.gradle').read_text()
    init=init.replace('build/aether-ports/observer','build/release-readiness/modern-boundary/observer')
    init_path=folder/'native.init.gradle';init_path.write_text(init)
    return fixture_path,init_path


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--run',required=True);args=parser.parse_args()
    assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.run)
    output=ROOT/'output/release-readiness'/args.run;assert not output.exists();output.mkdir(parents=True)
    profile=ROOT/'build/remaining-ui-audit/gameplay'/('1.21.1-neoforge-integrated-'+args.run)
    record=dict(status='running',target='1.21.1/neoforge',profile=str(profile.relative_to(ROOT)),
                started_at=datetime.datetime.now(datetime.timezone.utc).isoformat())
    def save(): (output/'summary.json').write_text(json.dumps(record,indent=2)+'\n')
    save()
    with (ROOT/'build/project-everything-regression/runtime.lock').open('a+b') as lock:
        lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            fixture,init=prepare()
            core=ROOT/'versions/1.21.1/neoforge/build/libs/salts_inventory_update-1.21.1-NeoForge-0.1.6.jar'
            compat=ROOT/'compatibility/versions/1.21.1/neoforge/build/libs/salts_inventory_compat-1.21.1-NeoForge-0.1.3.jar'
            command=[str(ROOT/'gradlew.bat'),':1.21.1:neoforge:runNativeReference','-I',str(init),
                '-PnativeMinecraft=1.21.1','-PnativeLoader=neoforge','-PnativeRun='+args.run,'-PnativeIntegrated=true',
                '-PnativeFixture='+str(fixture),'-PcompatibilityQaManifest='+str(fixture),
                '-PnativeCoreJar='+str(core),'-PnativeCompatJar='+str(compat),
                '-PnativeRuntimeRoot=C:/Users/texca/.cache/siu-regression-runtime','-PnativeHeap=3g','--console=plain','--max-workers=3']
            record.update(command=command,source_sha256={str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest()
                for p in [fixture,init,*init.parent.glob('observer/com/siu_reference/*.java')]});save()
            with (output/'launch.log').open('w') as log:
                proc=subprocess.Popen(command,cwd=ROOT,env={**os.environ,'JAVA_HOME':'C:/Program Files/Java/jdk-26'},
                    stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
                record['pid']=proc.pid;save()
                try:
                    deadline=time.monotonic()+900
                    while not (profile/'ready.json').exists():
                        if proc.poll() is not None:raise RuntimeError('Client launch exited '+str(proc.returncode))
                        if 'Error during pre-loading phase' in (output/'launch.log').read_text(errors='replace'):
                            raise RuntimeError('Publisher/loader pre-loading failure; see preserved launch.log')
                        if time.monotonic()>deadline:raise TimeoutError('Native client readiness')
                        time.sleep(1)
                    record['subjects']=json.loads((profile/'staged-artifacts.json').read_text())['artifacts'];save()
                    assert {a['filename'] for a in json.loads(fixture.read_text())['artifacts']} <= {a['filename'] for a in record['subjects']}
                    check=[sys.executable,str(ROOT/'functional-tests/aether-windows/character.py'),str(profile),'--loader','forge','--controller']
                    with (output/'aether.log').open('w') as log:
                        result=subprocess.run(check,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT)
                    record['checks_exit_code']=result.returncode;save()
                    assert result.returncode==0,'Aether/Quark modern focused checks failed'
                finally:
                    finish(profile)
                    try:record['exit_code']=proc.wait(timeout=120)
                    except subprocess.TimeoutExpired:
                        subprocess.run(['taskkill','/PID',str(proc.pid),'/T','/F'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
                        record['exit_code']='terminated-owned-runtime-after-timeout'
            assert record['exit_code']==0
            record['status']='passed'
        except Exception as error:
            record.update(status='failed',error=repr(error));raise
        finally:
            record['finished_at']=datetime.datetime.now(datetime.timezone.utc).isoformat();save()
            lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)


if __name__=='__main__':main()
