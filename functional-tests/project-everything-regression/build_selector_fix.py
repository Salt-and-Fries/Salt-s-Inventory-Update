"""Compile/package the selector veto across all shipping targets; no clients."""
from pathlib import Path
import os,subprocess
ROOT=Path(__file__).resolve().parents[2]
tasks=[]
for mc in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']:
    tasks += [f':{mc}:fabric:test',f':compatibility:{mc}:fabric:test']
    for loader in ['fabric','forge' if mc=='1.20.1' else 'neoforge']:
        archive='remapJar' if loader=='fabric' and not mc.startswith('26.') else 'reobfJar' if loader=='forge' else 'jar'
        tasks += [f':{mc}:{loader}:{archive}',f':compatibility:{mc}:{loader}:{archive}']
with (ROOT/'build/project-everything-regression/native-selector-build-03.log').open('x') as stream:
    result=subprocess.run([str(ROOT/'gradlew.bat'),*tasks,'--console=plain','--max-workers=3'],cwd=ROOT,env={**os.environ,'JAVA_HOME':'C:/Program Files/Java/jdk-26'},stdout=stream,stderr=subprocess.STDOUT)
print('Selector production build exit',result.returncode,flush=True)
raise SystemExit(result.returncode)
