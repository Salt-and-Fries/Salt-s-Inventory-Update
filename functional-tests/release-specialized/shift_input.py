"""Excluded synthetic Shift query fixture; no window/action callback overrides."""
from pathlib import Path
import hashlib,json,subprocess,zipfile

ROOT=Path(__file__).resolve().parents[2]


def prepare(folder,java):
    folder=Path(folder);assert not folder.exists();folder.mkdir(parents=True)
    source=Path(__file__).with_name('NativeInputShiftAgent.java')
    classes=folder/'classes';classes.mkdir()
    cache=Path.home()/'.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm/9.10.1'
    asm=next(p for p in cache.glob('*/*.jar')if not p.name.endswith(('-sources.jar','-javadoc.jar')))
    with(folder/'compile.log').open('x')as stream:
        subprocess.run([str(Path(java).with_name('javac.exe')),'--release','17','-proc:none','-cp',str(asm),
            '-d',str(classes),str(source)],stdout=stream,stderr=subprocess.STDOUT,check=True)
    jar=folder/'synthetic-shift-input.jar'
    with zipfile.ZipFile(jar,'x',zipfile.ZIP_DEFLATED)as z:
        z.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\nPremain-Class: NativeInputShiftAgent\n\n')
        for p in classes.glob('*.class'):z.write(p,p.name)
        # Keep this QA agent independent of loader-specific ASM exposure.
        with zipfile.ZipFile(asm)as library:
            for name in library.namelist():
                if name.startswith('org/objectweb/asm/')and name.endswith('.class'):z.writestr(name,library.read(name))
    sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
    (folder/'lineage.json').write_text(json.dumps(dict(scope='Synthetic Shift keyboard-query fixture only. While its explicit property is true, left/right Shift queries are pressed. Original queries execute for every other key and when false. No SIU/upstream menu, callback, inventory or packet implementation is changed.',
        source=str(source.relative_to(ROOT)),source_sha256=sha(source),agent_sha256=sha(jar),asm=str(asm),asm_sha256=sha(asm),
        shipping_jars_changed=False,requires_live_attestation=True),indent=2)+'\n')
    return jar


def install_raw(profile,out):
    profile=Path(profile);launch=json.loads((profile/'launch.json').read_text())
    agent=prepare(Path(out)/'synthetic-shift-input',launch['java'])
    arguments=Path(launch['arguments']);raw=arguments.read_bytes()
    assert b'-javaagent:'not in raw
    arguments.write_text('"-javaagent:'+agent.as_posix()+'"\n'+raw.decode())
    sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
    (Path(out)/'synthetic-input-launch.json').write_text(json.dumps(dict(
        scope='QA-only JVM input fixture, declared separately from ordinary uninstrumented runs.',
        arguments_file=str(arguments.relative_to(ROOT)),before_sha256=hashlib.sha256(raw).hexdigest(),after_sha256=sha(arguments),
        agent=str(agent.relative_to(ROOT)),agent_sha256=sha(agent)),indent=2)+'\n')
    return agent
