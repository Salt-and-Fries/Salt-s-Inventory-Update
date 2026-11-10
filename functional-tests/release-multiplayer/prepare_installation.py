"""Disposable one-sided installation fixtures; never mutate production jars."""
from pathlib import Path
import argparse,importlib.util,json,shutil,subprocess,zipfile
from prepare import prepare,sha,argument,ROOT

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--run',required=True);p.add_argument('--mode',required=True,choices=['client-only','server-only','protocol-mismatch']);a=p.parse_args()
    output=prepare(a.run);manifest=json.loads((output/'manifest.json').read_text());source=Path(__file__).with_name('InstallationClient.java')
    classes=output/'installation-classes';classes.mkdir()
    compile_cp=json.loads((ROOT/'build/project-everything-regression/production/26.3-fabric-classpath.json').read_text())['classpath']
    compile_cp=[v for v in compile_cp if not ('build' in v and 'classes' in v)]
    args=['--release','25','-proc:none','-encoding','UTF-8','-cp',';'.join(compile_cp),'-d',classes,source,Path(__file__).with_name('AtomicObservation.java')]
    argfile=output/'installation-javac.args';argfile.write_text('\n'.join(map(argument,args)))
    java=Path(manifest['launches']['a']['java'])
    with (output/'installation-compile.log').open('x') as stream:subprocess.run([str(java.with_name('javac.exe')),'@'+str(argfile)],stdout=stream,stderr=subprocess.STDOUT,check=True)
    observer=output/'release-installation-client-qa.jar'
    with zipfile.ZipFile(observer,'w',zipfile.ZIP_DEFLATED) as archive:
        for path in classes.rglob('*.class'):archive.write(path,path.relative_to(classes).as_posix())
        archive.writestr('fabric.mod.json',json.dumps(dict(schemaVersion=1,id='siu_release_installation_qa',version='1.0',environment='client',
            entrypoints={'client':['com.siu_release_remote.InstallationClient']},depends={'minecraft':'26.3','fabric-api':'*'})))
    retained=[]
    for subject in manifest['subjects']:
        remove=(a.mode=='client-only' and subject['profile']=='server' or a.mode in {'server-only','protocol-mismatch'} and subject['profile']!='server') and subject['family'] in {'core','compatibility'}
        if remove:
            path=Path(manifest['launches'][subject['profile']]['cwd'])/'mods'/subject['filename'];assert sha(path)==subject['sha256'];path.unlink()
        else:retained.append(subject)
    for name,launch in manifest['launches'].items():
        if name=='server':continue
        mods=Path(launch['cwd'])/'mods';old=mods/'release-remote-client-qa.jar';assert sha(old)==manifest['observer_sha256'];old.unlink();shutil.copy2(observer,mods/observer.name)
        launch['environment']['SIU_INSTALL_MODE']=a.mode
    manifest.update(mode=a.mode,subjects=retained,source_sha256=sha(source),observer_sha256=sha(observer),
        scope='Real one-sided installation/native fallback or explicit protocol mismatch, official dedicated server; no server QA mod; exact retained production artifacts.')
    (output/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n');print(a.mode,output)

if __name__=='__main__':main()
