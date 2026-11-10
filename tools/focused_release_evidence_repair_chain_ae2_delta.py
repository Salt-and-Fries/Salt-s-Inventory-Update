"""Freshly compile the exact scoped AE2 inspectors against immutable artifact pairs."""
from pathlib import Path
import os,shutil,subprocess
from focused_release_evidence import checked,reference


def compile_inspector(root,folder,edge):
    root=Path(root).resolve();folder=Path(folder).resolve();assert folder.is_relative_to(root)and not folder.exists();folder.mkdir(parents=True)
    sources=[];libraries=[]
    for ref in edge['java_sources']:
        p=checked(root,ref);q=folder/p.name;shutil.copy2(p,q);sources.append(q)
    for ref in edge['libraries']:
        p=checked(root,ref);q=folder/p.name;shutil.copy2(p,q);libraries.append(q)
    assert len(libraries)==3 and {p.name for p in libraries}=={'asm-9.11.jar','asm-tree-9.11.jar','asm-analysis-9.11.jar'}
    if edge['inspector_class']=='AeNativeQueueDelta':
        assert len(sources)==1
        for ref in edge['tested_queue_classes']:checked(root,ref)
    else:assert edge['inspector_class']in {'AePendingCloseDelta','RequesterPayloadDelta','LayoutTransitionDelta','RequesterLegacyScopeDelta_1266','ForgeRequesterBridgeDelta_1277'}and len(sources)==2
    classes=folder/'classes';classes.mkdir();cp=os.pathsep.join(map(str,libraries));jdk=Path('C:/Program Files/Java/jdk-26/bin')
    q=subprocess.run([str(jdk/'javac.exe'),'--release','17','-cp',cp,'-d',str(classes),*map(str,sources)],capture_output=True,text=True)
    (folder/'compile.log').write_text(q.stdout+q.stderr);assert q.returncode==0,q.stderr
    return dict(folder=folder,classpath=os.pathsep.join([str(classes),cp]),java=jdk/'java.exe',source=reference(root,sources[0]),binary=reference(root,classes/(edge['inspector_class']+'.class')))


def inspect(root,tool,edge,before,after,log):
    args=[str(tool['java']),'-cp',tool['classpath'],edge['inspector_class']]
    if edge['inspector_class']=='AeNativeQueueDelta':
        classes=checked(root,edge['tested_queue_classes'][0]).parents[5]
        assert classes.name=='test-classes'
        args+=['audit',str(before),str(after),str(classes)]
    else:args+=[str(before),str(after)]
    q=subprocess.run(args,capture_output=True,text=True);Path(log).write_text(q.stdout+q.stderr);assert q.returncode==0,q.stderr
    return reference(root,log)
