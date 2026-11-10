"""Controlled SIU-absent opening probe using the failing mixed publisher cohort.

Run only inside the serial priority reservation. This is diagnostic evidence,
not a replacement for a shipping-SIU functional cohort.
"""
from pathlib import Path
import argparse,hashlib,json,os,subprocess,sys,time,traceback
from run_mod_matrix import ROOT,finish
sys.path.insert(0,str(ROOT/'functional-tests/all-target-rollout'))
from evidence import send

def pins(data):
    return sorted({(r['filename'],r['sha256']) for r in data['artifacts']
                   if r.get('family') not in {'qa','core','compatibility'}})

def main():
    p=argparse.ArgumentParser();p.add_argument('--run',required=True);a=p.parse_args()
    fixture=ROOT/'build/all-target-rollout/fixtures/1211-fabric-themed-v1/fixture.json'
    baseline=ROOT/'build/remaining-ui-audit/gameplay/1.21.1-fabric-siu-project-everything-retries-01-j27-2/staged-artifacts.json'
    profile=ROOT/'build/remaining-ui-audit/gameplay'/('1.21.1-fabric-native-'+a.run)
    report=ROOT/'output/project-everything-regression'/(a.run+'-native-mod-matrix.json')
    assert not report.exists() and not profile.exists(),'Preserve earlier evidence'
    row={'target':'1.21.1/fabric','fixture':'1211-fabric-themed-v1','profile':str(profile.relative_to(ROOT)),
         'scope':'SIU-absent-diagnostic','status':'preparing','checks':[]}
    def save():report.write_text(json.dumps([row],indent=2)+'\n')
    save()
    try:
        prep=ROOT/'build/project-everything-regression'/(a.run+'-prepare.log')
        with prep.open('x') as log:
            subprocess.run([sys.executable,str(ROOT/'tools/prepare-rollout-production-modern.py'),
                '--minecraft','1.21.1','--side','native','--run',a.run,'--fixture',str(fixture),
                '--classpath-from',str(ROOT/'build/project-everything-regression/production/1.21.1-fabric-classpath.json'),
                '--heap','4g'],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,check=True)
        staged=json.loads((profile/'staged-artifacts.json').read_text());expected=json.loads(baseline.read_text())
        assert staged['siu_loaded'] is False
        assert not any(r.get('family') in {'core','compatibility'} for r in staged['artifacts'])
        assert pins(staged)==pins(expected),'The native probe must use the exact mixed publisher/API binaries'
        row['checks'].append({'check':'same-publishers-and-runtime-api-with-SIU-absent','passed':True})
        launch=json.loads((profile/'launch.json').read_text());row['log']=str(Path(launch['log']).relative_to(ROOT))
        from display_fixture import configure
        configure(Path(launch['cwd']),profile)
        with Path(launch['log']).open('w') as log:
            process=subprocess.Popen([launch['java'],'@'+launch['arguments']],cwd=launch['cwd'],
                env={**os.environ,**launch['environment']},stdout=log,stderr=subprocess.STDOUT,
                creationflags=subprocess.CREATE_NO_WINDOW)
            row.update(status='starting',pid=process.pid);save()
            try:
                deadline=time.monotonic()+600
                while not (profile/'ready.json').exists():
                    if process.poll() is not None:raise RuntimeError('Native probe exited '+str(process.returncode))
                    if time.monotonic()>deadline:raise TimeoutError('Native probe did not become ready')
                    time.sleep(1)
                row['status']='testing';save()
                send(profile,'fixture',block='charm:kiln',ticks=12)
                result=send(profile,'open',ticks=20);row['opening_reply']=result
                result=send(profile,'catalog',ticks=4);state=result['client'];row['native_state']=state
                okay=state.get('screen')=='svenhjol.charm.feature.kilns.client.Screen' and state.get('menuClass')=='svenhjol.charm.feature.kilns.common.Menu'
                row['checks'].append({'check':'ordinary-native-kiln-opens','passed':okay})
                row['status']='passed' if okay else 'failed'
            finally:
                finish(profile)
                try:row['exit_code']=process.wait(timeout=120)
                except subprocess.TimeoutExpired:
                    subprocess.run(['taskkill','/PID',str(process.pid),'/T','/F'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
                    row.update(status='failed',exit_code='terminated-owned-native-probe-after-timeout')
                if row.get('exit_code')!=0:row['status']='failed'
        row['missing_kiln_enum_in_native_log']='No enum constant net.minecraft.class_5421.KILN' in Path(launch['log']).read_text(encoding='utf-8',errors='replace')
        row['log_sha256']=hashlib.sha256(Path(launch['log']).read_bytes()).hexdigest()
    except Exception as error:row.update(status='failed',error=repr(error),trace=traceback.format_exc())
    save();print(json.dumps({k:row.get(k) for k in ('status','profile','missing_kiln_enum_in_native_log','error')}),flush=True)
    if row['status']!='passed':raise SystemExit(1)

if __name__=='__main__':main()
