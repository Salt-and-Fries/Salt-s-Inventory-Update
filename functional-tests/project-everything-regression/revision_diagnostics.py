"""Build an excluded, bounded native stack-logging agent; no release staging."""
from pathlib import Path
import hashlib,json,subprocess,zipfile
ROOT=Path(__file__).resolve().parents[2]

def prepare(profile,java):
    source=Path(__file__).with_name('NativeRevisionAgent.java')
    folder=profile/'native-revision-agent';classes=folder/'classes';classes.mkdir(parents=True)
    cache=Path.home()/'.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm/9.10.1'
    asm=next(p for p in cache.glob('*/*.jar') if not p.name.endswith(('-sources.jar','-javadoc.jar')))
    with (folder/'compile.log').open('w') as log:
        subprocess.run([str(Path(java).with_name('javac.exe')),'--release','17','-proc:none','-cp',str(asm),'-d',str(classes),str(source)],stdout=log,stderr=subprocess.STDOUT,check=True)
    jar=folder/'native-revision-agent.jar'
    with zipfile.ZipFile(jar,'x',zipfile.ZIP_DEFLATED) as z:
        z.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\nPremain-Class: NativeRevisionAgent\n\n')
        for p in classes.glob('*.class'):z.write(p,p.name)
    sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
    (folder/'lineage.json').write_text(json.dumps({'scope':'Diagnostic only: adds at most 32 stack logs at native 1.20.1 Fabric player-menu revision increments above 200. Original method executes unchanged. Timing affected; never functional pass evidence.',
        'source':str(source.relative_to(ROOT)),'source_sha256':sha(source),'agent_sha256':sha(jar),'asm':str(asm),'asm_sha256':sha(asm),'shipping_jars_changed':False},indent=2)+'\n')
    return jar,asm

def self_test():
    folder=ROOT/'build/project-everything-regression/revision-agent-selftest-02';assert not folder.exists()
    folder.mkdir();java=Path('C:/Program Files/Microsoft/jdk-21.0.10.7-hotspot/bin/java.exe')
    jar,asm=prepare(folder,java);src=folder/'class_1703.java'
    src.write_text('package net.minecraft; public class class_1703 {public int field_7763=0; private int field_34024;private class_2371 field_29206=new class_2371(); public int method_37422(){return ++field_34024;} private void method_34253(int i,class_1799 s,java.util.function.Supplier<class_1799> copy){if(!class_1799.method_7973((class_1799)field_29206.get(i),s)){field_29206.set(i,copy.get());method_37422();}}public static void main(String[] x){var m=new class_1703();for(int i=1;i<=240;i++)if(m.method_37422()!=i)throw new AssertionError();var s=new class_1799(2);m.method_34253(0,s,()->s);if(m.field_34024!=241||m.field_29206.get(0)!=s)throw new AssertionError();System.out.println("original method preserved through all 240 calls and cache replacement");}}')
    (folder/'class_1799.java').write_text('package net.minecraft; public class class_1799 {int count;class_1799(int c){count=c;}public static boolean method_7973(class_1799 a,class_1799 b){return a.count==b.count;}public int method_7947(){return count;}public boolean method_7960(){return count==0;}public class_2487 method_7969(){return null;}public String toString(){return "fixture-stack";}}')
    (folder/'class_2487.java').write_text('package net.minecraft; public class class_2487 {}')
    (folder/'class_2371.java').write_text('package net.minecraft; public class class_2371 extends java.util.ArrayList<Object>{class_2371(){add(new class_1799(1));}}')
    classes=folder/'fixture-classes';classes.mkdir()
    subprocess.run([str(java.with_name('javac.exe')),'--release','17','-d',str(classes),*map(str,folder.glob('class_*.java'))],check=True,capture_output=True,text=True)
    run=subprocess.run([str(java),'-javaagent:'+str(jar),'-cp',str(classes)+';'+str(asm),'net.minecraft.class_1703'],capture_output=True,text=True,check=True)
    (folder/'stdout.log').write_text(run.stdout);(folder/'stderr.log').write_text(run.stderr)
    assert run.stderr.count('java.lang.Throwable: SIU QA native player-menu revision caller')==32,run.stderr[:2000]
    assert 'original method preserved through all 240 calls' in run.stdout
    assert run.stderr.count('SIU QA native cache mismatch slot=')==1
    print('Diagnostic bytecode agent self-check passed: 240 original returns, original cache replacement, exactly 32 bounded traces and one observed mismatch. This is not a Minecraft functional result.')

if __name__=='__main__':self_test()
