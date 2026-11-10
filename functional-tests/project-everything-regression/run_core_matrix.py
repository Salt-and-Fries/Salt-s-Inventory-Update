"""Run the complete opt-in Core suite on all twelve shipping targets, unattended."""
from pathlib import Path
import argparse,datetime,hashlib,json,os,subprocess,time

ROOT=Path(__file__).resolve().parents[2]
TARGETS=[(v,l)for v in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']for l in ['fabric','forge'if v=='1.20.1'else'neoforge']]

def main():
    p=argparse.ArgumentParser();p.add_argument('--run',default='project-everything-full-01');p.add_argument('--target',action='append');a=p.parse_args()
    out=ROOT/'output/project-everything-regression';out.mkdir(exist_ok=True)
    build=ROOT/'build/project-everything-regression';build.mkdir(exist_ok=True)
    rows=[]
    for mc,loader in TARGETS:
        if a.target and mc+'/'+loader not in a.target:continue
        key=mc+'-'+loader;run=ROOT/'build/all-target-rollout/gameplay'/f'{key}-{a.run}'
        log=build/(key+'-'+a.run+'.log')
        assert not log.exists()and not run.exists(),'Use a fresh run id'
        args=[str(ROOT/'gradlew.bat'),f':{mc}:{loader}:runNativePortQa','-I','functional-tests/project-everything-regression/core.init.gradle',f'-PqaMinecraft={mc}',f'-PqaLoader={loader}','-PqaFullGameplay=true','-PqaFullCore=true','-PqaScenario=core',f'-PqaRunId={a.run}','-PqaRuntimeRoot=C:/Users/texca/.cache/siu-regression-runtime','--console=plain','--max-workers=3']
        row={'minecraft':mc,'loader':loader,'log':str(log.relative_to(ROOT)),'runtime':str(run.relative_to(ROOT)),'status':'running','started_at':datetime.datetime.now(datetime.timezone.utc).isoformat()};rows.append(row)
        report=out/(a.run+'-core-matrix.json')
        report.write_text(json.dumps(rows,indent=2)+'\n')
        print('START',key,flush=True);started=time.monotonic()
        try:
            with log.open('w')as stream:
                process=subprocess.Popen(args,cwd=ROOT,env={**os.environ,'JAVA_HOME':'C:/Program Files/Java/jdk-26'},stdout=stream,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
                try:exit_code=process.wait(timeout=2700)
                except subprocess.TimeoutExpired:
                    # Killing only gradlew's wrapper leaves a native client
                    # alive and contaminates the next focus-sensitive target.
                    subprocess.run(['taskkill','/PID',str(process.pid),'/T','/F'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
                    process.wait(timeout=30)
                    raise
            row['exit_code']=exit_code
            path=run/'results.json';data=json.loads(path.read_text())if path.exists()else{}
            row['results']=data
            row['status']='passed'if exit_code==0 and 'SIU_GAMEPLAY_SUMMARY status=PASS'in log.read_text(errors='replace')and data.get('failures')==0 else'failed'
            if (run/'staged-artifacts.json').exists():
                staged=json.loads((run/'staged-artifacts.json').read_text());row['staged']=staged
                dist=ROOT/f'dist/core/salts_inventory_update-{mc}-{loader.title()if loader!="neoforge"else"NeoForge"}-0.1.6.jar'
                expected=hashlib.sha256(dist.read_bytes()).hexdigest();row['shipping_sha256']=expected
                assert staged['sha256']==expected,'Did not run shipping Core bytes'
        except Exception as error:row.update(status='failed',error=repr(error))
        row['seconds']=round(time.monotonic()-started,1);report.write_text(json.dumps(rows,indent=2)+'\n')
        print('END',key,row['status'],flush=True)
    if any(r['status']!='passed'for r in rows):raise SystemExit(1)

if __name__=='__main__':main()
