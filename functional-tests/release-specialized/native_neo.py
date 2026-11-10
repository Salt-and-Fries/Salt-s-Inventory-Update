"""Private named-Neo native observer for scoped, original-screen feature replays.

This backend keeps the richer NativeUiReference protocol required by the
retained Create driver. It does not replace or modify the publisher matrix's
EverythingAuditQa observer, nor any production archive.
"""
from pathlib import Path
import hashlib, importlib.util, json, os, shutil, subprocess, sys, time, traceback
from native_neo_provenance import private_init

ROOT=Path(__file__).resolve().parents[2]


def load(name,path):
    spec=importlib.util.spec_from_file_location(name,path)
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module


def source_inputs():
    return [ROOT/p for p in [
        'tools/prepare-rollout-production-modern.py',
        'tools/prepare-rollout-production-fabric.py',
        'functional-tests/remaining-ui-audit/native/java/com/siu_reference/NativeUiReference.java',
        'functional-tests/aether-windows/prepare_modern.py',
        'functional-tests/aether-windows/prepare.py',
        'functional-tests/aether-windows/RuntimeNames.java',
        'functional-tests/aether-windows/native.init.gradle',
        'functional-tests/release-specialized/native_neo_provenance.py']]


def prepare(run,enhance=None):
    folder=ROOT/'build/release-readiness'/run/'native-observer'
    assert not folder.exists(),'Preserve generated observer inputs per attempt'
    prep=load('specialized_neo_source',ROOT/'tools/prepare-rollout-production-modern.py');prep.MC='1.21.1'
    sys.path.insert(0,str(ROOT/'functional-tests/aether-windows'))
    aether=load('specialized_neo_adapter',ROOT/'functional-tests/aether-windows/prepare_modern.py')
    _,text=prep.qa_source('siu')
    source=folder/'java/com/siu_reference';source.mkdir(parents=True)
    # The established adapter adds read-only optional Aether observations and
    # updates the native 1.21.1 registry-aware save methods. Its optional hooks
    # remain guarded and no Aether publisher is required by this fixture.
    text=aether.source(text).replace('net.minecraftforge.fml.ModList','net.neoforged.fml.ModList')
    if enhance:text=enhance(text,'1.21.1')
    (source/'NativeUiReference.java').write_text(text)
    shutil.copy2(ROOT/'functional-tests/aether-windows/RuntimeNames.java',source/'RuntimeNames.java')
    init=(ROOT/'functional-tests/aether-windows/native.init.gradle').read_text()
    assert 'build/aether-ports/observer' in init
    init=init.replace('build/aether-ports/observer',str(folder.relative_to(ROOT)/'java').replace('\\','/'))
    init=private_init(init,run)
    init_path=folder/'native.init.gradle';init_path.write_text(init)
    return init_path


def execute(run,target,fixture,out,check,enhance=None,jvm_agent=None):
    assert target=='1.21.1/neoforge'
    from run_mod_matrix import finish
    from subject_artifacts import current_subjects
    init=prepare(run,enhance)
    if jvm_agent:
        assert Path(jvm_agent).is_file()
        text=init.read_text();anchor="minHeapSize='512m';"
        assert anchor in text
        init.write_text(text.replace(anchor,"jvmArgs('-javaagent:'+root.providers.gradleProperty('nativeShiftAgent').get());"+anchor,1))
    subjects=current_subjects(target)
    core=ROOT/'dist/core'/subjects['core'][0]
    compat=ROOT/'dist/compatibility'/subjects['compatibility'][0]
    profile=ROOT/'build/remaining-ui-audit/gameplay'/('1.21.1-neoforge-integrated-'+run)
    assert not profile.exists()
    row=dict(target=target,profile=str(profile.relative_to(ROOT)),status='starting',fixture=str(fixture.relative_to(ROOT)))
    command=[str(ROOT/'gradlew.bat'),':1.21.1:neoforge:runNativeReference','-I',str(init),
        '-PnativeMinecraft=1.21.1','-PnativeLoader=neoforge','-PnativeRun='+run,'-PnativeIntegrated=true',
        '-PnativeFixture='+str(fixture),'-PcompatibilityQaManifest='+str(fixture),
        '-PnativeCoreJar='+str(core),'-PnativeCompatJar='+str(compat),
        '-PnativeRuntimeRoot=C:/Users/texca/.cache/siu-regression-runtime','-PnativeHeap=3g',
        '--console=plain','--max-workers=3']
    if jvm_agent:command.append('-PnativeShiftAgent='+str(jvm_agent))
    generated=[init,*init.parent.glob('java/com/siu_reference/*.java')]
    row.update(command=command,generated_inputs={str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in generated})
    log_path=out/'launch.log'
    with log_path.open('x') as log:
        process=subprocess.Popen(command,cwd=ROOT,env={**os.environ,'JAVA_HOME':'C:/Program Files/Java/jdk-26'},
            stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
        row['pid']=process.pid
        try:
            deadline=time.monotonic()+900
            while not (profile/'ready.json').exists():
                if process.poll() is not None:raise RuntimeError('Named native client exited '+str(process.returncode))
                if 'Error during pre-loading phase' in log_path.read_text(errors='replace'):
                    raise RuntimeError('Publisher pre-loading failure; preserve launch.log')
                if time.monotonic()>deadline:raise TimeoutError('Named native client readiness')
                time.sleep(1)
            staging=profile/'staged-artifacts.json'
            raw=staging.read_bytes();metadata=json.loads(raw)
            original_staging=profile/'native-staged-artifacts-original.json';assert not original_staging.exists()
            original_staging.write_bytes(raw)
            # The native Gradle task records filenames/hashes without family
            # labels. Add QA metadata roles only after verifying the exact
            # known subject names/bytes, retaining the untouched native ledger.
            subject_names={name:(family,digest)for family,(name,digest)in subjects.items()}
            qa_path=(init.parent/'artifact/native-ui-reference.jar').resolve()
            assert qa_path.is_file()
            assert len({a['filename']for a in metadata['artifacts']})==len(metadata['artifacts'])
            for artifact in metadata['artifacts']:
                original=Path(artifact['file']);assert hashlib.sha256(original.read_bytes()).hexdigest()==artifact['sha256']
                if artifact['filename']in subject_names:
                    family,digest=subject_names[artifact['filename']];assert artifact['sha256']==digest
                elif artifact['filename']=='native-ui-reference.jar':
                    assert original.resolve()==qa_path,'The actual private compiled observer is required'
                    family='qa'
                else:family='upstream'
                assert artifact.get('family',family)==family
                artifact['family']=family
            assert {a['family']for a in metadata['artifacts']if a['family']in ['core','compatibility']}=={'core','compatibility'}
            assert len([a for a in metadata['artifacts']if a['family']=='qa'])==1
            for generated_path in sorted((init.parent/'generated-metadata').rglob('*')):
                if generated_path.is_file():row['generated_inputs'][str(generated_path.relative_to(ROOT))]=hashlib.sha256(generated_path.read_bytes()).hexdigest()
            assert any(p.endswith('NativeReferenceNeoForge.java')for p in row['generated_inputs']),'Actual generated entrypoint source is required'
            staging.write_text(json.dumps(metadata,indent=2)+'\n')
            row['staging_role_annotation']=dict(original=str(original_staging.relative_to(ROOT)),
                original_sha256=hashlib.sha256(raw).hexdigest(),annotated_sha256=hashlib.sha256(staging.read_bytes()).hexdigest(),
                scope='QA ledger roles inferred from verified exact Core/Compatibility identities and per-run compiled observer path/hash; binary inputs unchanged')
            runtime=Path('C:/Users/texca/.cache/siu-regression-runtime')/('1.21.1-neoforge-'+run)
            assert runtime.is_dir()
            row['runtime_artifact_copies']=[]
            for artifact in metadata['artifacts']:
                copied=runtime/'mods'/artifact['filename']
                actual_hash=hashlib.sha256(copied.read_bytes()).hexdigest()
                assert actual_hash==artifact['sha256'],('Runtime copy differs',artifact['filename'])
                row['runtime_artifact_copies'].append(dict(file=str(copied),sha256=actual_hash,family=artifact['family']))
            runtime_record=profile/'native-runtime.json';assert not runtime_record.exists()
            runtime_record.write_text(json.dumps(dict(cwd=str(runtime),
                source='Explicit nativeRuntimeRoot property and the reviewed Gradle task run-name rule'),indent=2)+'\n')
            row['runtime_record']=dict(file=str(runtime_record.relative_to(ROOT)),sha256=hashlib.sha256(runtime_record.read_bytes()).hexdigest())
            staged=metadata['artifacts']
            actual={v['filename']:v['sha256'] for v in staged}
            for original in json.loads(fixture.read_text())['artifacts']:
                native=Path(original['path'])
                if not native.is_absolute():native=ROOT/native
                assert actual[original['filename']]==hashlib.sha256(native.read_bytes()).hexdigest(),original['filename']
            row['status']='testing';row['results']=check(profile)
            assert row['results']['cases'] and row['results']['failed']==0
            row['status']='passed'
        except BaseException as error:
            row.update(status='failed',error=repr(error),trace=traceback.format_exc());raise
        finally:
            finish(profile)
            try:row['exit_code']=process.wait(timeout=120)
            except subprocess.TimeoutExpired:
                subprocess.run(['taskkill','/PID',str(process.pid),'/T','/F'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
                row['exit_code']='terminated-owned-runtime-after-timeout'
            if row['exit_code']!=0:row['status']='failed'
            (out/'native-attempt.json').write_text(json.dumps(row,indent=2)+'\n')
    assert row['exit_code']==0
    assert all(hashlib.sha256((ROOT/p).read_bytes()).hexdigest()==sha for p,sha in row['generated_inputs'].items())
    return [row]
