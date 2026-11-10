"""Launch a bounded affected-menu observer with the actual shipping jars."""
from pathlib import Path
import subprocess,os,sys
ROOT=Path(__file__).resolve().parents[2]
mc,run=sys.argv[1:3]
assert mc in ['1.21.1','26.1.2']
args=['cmd','/c',str(ROOT/'gradlew.bat'),f':{mc}:neoforge:runNativePortQa','-I','functional-tests/all-target-rollout/qa.init.gradle',f'-PqaMinecraft={mc}','-PqaLoader=neoforge','-PqaFullGameplay=true','-PqaIntegrationOnly=true','-PqaScenario=extension-empty',f'-PqaRunId={run}',f'-PcompatibilityQaManifest=build/extended-ae-all-targets/fixtures/{mc}/fixture.json','-PqaRuntimeRoot=C:/Users/texca/.cache/siu-rollout-runtime','--console=plain']
with(ROOT/f'build/extended-ae-all-targets/{mc}-{run}.log').open('w')as log:
    p=subprocess.Popen(args,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,env={**os.environ,'JAVA_HOME':'C:/Program Files/Java/jdk-26'},creationflags=subprocess.CREATE_NO_WINDOW)
print(mc,run,p.pid)
