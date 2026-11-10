"""Prepare exact production Fabric clients/server; compile only excluded client QA."""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import shutil
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
from display_fixture import configure

def module(name,path):
    spec=importlib.util.spec_from_file_location(name,path);value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value

def sha(path,algorithm='sha256'):return hashlib.new(algorithm,Path(path).read_bytes()).hexdigest()
def argument(value):return '"'+str(value).replace('\\','/').replace('"','\\"')+'"'

def prepare(run,java_path=None,vm_arguments=()):
    mc='26.3';output=ROOT/'output/release-readiness'/run
    output.mkdir(parents=True,exist_ok=False)
    cache=ROOT/'build/release-readiness/remote-production-cache';cache.mkdir(exist_ok=True)
    base=module('remote_production_cache',ROOT/'tools/prepare-rollout-production-fabric.py');base.CACHE=cache
    info_path=Path.home()/'.gradle/caches/fabric-loom'/mc/'mojang_minecraft_info.json'
    info=base.read(info_path)
    profile_path=cache/'fabric-loader-0.19.5.json'
    base.fetch(f'https://meta.fabricmc.net/v2/versions/loader/{mc}/0.19.5/profile/json',profile_path)
    profile=base.read(profile_path)
    loader_libraries=[base.library(row,True) for row in profile['libraries']]
    client_libraries=[base.library(row) for row in info['libraries'] if base.allowed(row) and 'artifact' in row.get('downloads',{})]
    client=Path.home()/'.gradle/caches/fabric-loom'/mc/'minecraft-client.jar'
    assert sha(client,'sha1')==info['downloads']['client']['sha1']
    bundle=base.fetch(info['downloads']['server']['url'],cache/'official-server-bundle.jar',info['downloads']['server']['sha1'])
    server_libraries=[];server=None
    with zipfile.ZipFile(bundle) as archive:
        for family in ['versions','libraries']:
            for line in archive.read(f'META-INF/{family}.list').decode().splitlines():
                digest,coordinate,name=line.split('\t')
                relative=Path(name)
                assert not relative.is_absolute() and '..' not in relative.parts
                destination=cache/family/relative;destination.parent.mkdir(parents=True,exist_ok=True)
                data=archive.read(f'META-INF/{family}/{name}');assert hashlib.sha256(data).hexdigest()==digest
                if not destination.exists() or sha(destination)!=digest:destination.write_bytes(data)
                if family=='versions':assert server is None;server=destination
                else:server_libraries.append(destination)
    assert server and 'net/minecraft/server/Main.class' in zipfile.ZipFile(server).namelist()
    with zipfile.ZipFile(server) as archive:
        assert 'net/minecraft/client/Minecraft.class' not in archive.namelist(), 'Use a real dedicated server jar'
    api=ROOT/'build/project-everything-regression/production/26.3/fabric-api-0.165.0+26.3.jar';assert api.is_file()
    fixture=json.loads((ROOT/'build/all-target-rollout/fixtures/263-fabric-inventory-tail-v2/fixture.json').read_text())
    assert fixture['runtime']['fabric_api']=='0.165.0+26.3'
    java=Path(java_path or 'C:/Program Files/Java/jdk-25.0.3/bin/java.exe');assert java.is_file()
    subjects=[('core',ROOT/f'dist/core/salts_inventory_update-{mc}-Fabric-0.1.6.jar'),
              ('compatibility',ROOT/f'dist/compatibility/salts_inventory_compat-{mc}-Fabric-0.1.3.jar'),('runtime-api',api)]
    compile_cp=json.loads((ROOT/'build/project-everything-regression/production/26.3-fabric-classpath.json').read_text())['classpath']
    compile_cp=[p for p in compile_cp if not ('build' in p and 'classes' in p)]
    compile_cp += [str(path) for kind,path in subjects if kind=='core']
    classes=output/'qa-classes';classes.mkdir()
    source=Path(__file__).with_name('RemoteClient.java')
    args=['--release','25','-proc:none','-encoding','UTF-8','-cp',';'.join(compile_cp),'-d',classes,source,Path(__file__).with_name('AtomicObservation.java')]
    argfile=output/'javac.args';argfile.write_text('\n'.join(map(argument,args)))
    with (output/'compile.log').open('w') as log:
        subprocess.run([str(java.with_name('javac.exe')),'@'+str(argfile)],stdout=log,stderr=subprocess.STDOUT,check=True)
    observer=output/'release-remote-client-qa.jar'
    with zipfile.ZipFile(observer,'w',zipfile.ZIP_DEFLATED) as archive:
        for path in classes.rglob('*.class'):archive.write(path,path.relative_to(classes).as_posix())
        archive.writestr('fabric.mod.json',json.dumps(dict(schemaVersion=1,id='siu_release_remote_qa',version='1.0',environment='client',
            entrypoints={'client':['com.siu_release_remote.RemoteClient']},depends={'minecraft':mc,'fabric-api':'*','salts_inventory_update':'*'})))
    assets=Path.home()/'.gradle/caches/fabric-loom/assets';index=f'regression-{mc}-'+info['assetIndex']['id']
    base.fetch(info['assetIndex']['url'],assets/'indexes'/(index+'.json'),info['assetIndex']['sha1'])
    launches={};staged=[]
    for name in ['server','a','b','a-reconnect']:
        folder=output/name;folder.mkdir();runtime=ROOT/'build/release-readiness/remote-runtimes'/run/name
        assert not runtime.exists();(runtime/'mods').mkdir(parents=True)
        for kind,path in subjects:
            shutil.copy2(path,runtime/'mods'/path.name)
            staged.append(dict(profile=name,family=kind,file=str(path),filename=path.name,sha256=sha(path)))
        if name=='server':
            (runtime/'eula.txt').write_text('eula=true\n')
            flat=json.dumps(dict(layers=[dict(height=1,block='minecraft:bedrock'),dict(height=2,block='minecraft:dirt'),dict(height=1,block='minecraft:grass_block')],biome='minecraft:plains'))
            (runtime/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25587\nonline-mode=false\nspawn-protection=0\n'
                'view-distance=2\nsimulation-distance=3\nlevel-type=minecraft:flat\ngenerator-settings='+flat+'\ngenerate-structures=false\n'
                'difficulty=peaceful\nallow-flight=true\nenable-rcon=true\nrcon.port=25588\nrcon.password=release-local-qa-only\n'
                'broadcast-rcon-to-ops=false\nwhite-list=false\n')
            cp=loader_libraries+server_libraries+[server]
            values=['-Xms512m','-Xmx2g','-XX:ActiveProcessorCount=4','--enable-native-access=ALL-UNNAMED','--sun-misc-unsafe-memory-access=allow',
                '-Dfabric.development=false','-Dfabric.noGui=true','-Dfabric.gameJarPath='+str(server),'-cp',';'.join(map(str,cp)),
                'net.fabricmc.loader.impl.launch.knot.KnotServer','nogui']
            env={}
        else:
            shutil.copy2(observer,runtime/'mods'/observer.name)
            (runtime/'options.txt').write_text('tutorialStep:none\nrenderDistance:2\nsimulationDistance:5\nguiScale:2\npauseOnLostFocus:false\nautoJump:false\n')
            configure(runtime,folder)
            cp=loader_libraries+client_libraries+[client]
            values=['-Xms512m','-Xmx4g','-XX:ActiveProcessorCount=4','--enable-native-access=ALL-UNNAMED','--sun-misc-unsafe-memory-access=allow',
                '-Dfabric.development=false','-Dfabric.noGui=true','-Dfabric.gameJarPath='+str(client),'-cp',';'.join(map(str,cp)),profile['mainClass'],
                '--username','SIUPlayerB' if name=='b' else 'SIUPlayerA','--version',mc,'--gameDir',str(runtime),'--assetsDir',str(assets),'--assetIndex',index,
                '--uuid','14ea713fe12d3d8bb7b6baf52d52d598' if name!='b' else '25ea713fe12d3d8bb7b6baf52d52d598',
                '--accessToken','0','--userType','legacy','--versionType','release','--width','1000','--height','700']
            env={'SIU_REMOTE_OUTPUT':str(folder),'SIU_REMOTE_SERVER':'127.0.0.1:25587'}
        values[:0]=list(vm_arguments)
        values.insert(0,'-XX:ErrorFile='+str(folder/'hs_err_pid%p.log'))
        file=folder/'java.args';file.write_text('\n'.join(map(argument,values)))
        launches[name]=dict(java=str(java),arguments=str(file),cwd=str(runtime),environment=env,log=str(folder/'launch.log'))
    manifest=dict(status='prepared-not-run',minecraft=mc,loader='fabric',launches=launches,subjects=staged,vm_arguments=list(vm_arguments),
        official_server=dict(bundle_sha1=sha(bundle,'sha1'),dedicated_sha256=sha(server),no_client_main=True),
        source_sha256=sha(source),observer_sha256=sha(observer),scope='Real dedicated server and separate JVM clients; no server QA mod; vanilla RCON setup and authoritative reads.')
    (output/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n');print(output)
    return output

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--run',required=True);parser.add_argument('--java',help='Explicit installed supported Java runtime; compiler still targets Java 25');parser.add_argument('--vm-argument',action='append',default=[],help='Explicit diagnostic VM setting, retained in the evidence manifest');args=parser.parse_args()
    assert all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.run)
    prepare(args.run,args.java,args.vm_argument)
