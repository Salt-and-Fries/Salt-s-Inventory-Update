"""Finalize evidence after the serial runtime queue; never equate completion with pass."""
from pathlib import Path
import argparse,datetime,hashlib,json,subprocess,sys,time
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'output/project-everything-regression'

def read(path):
    try:return json.loads(path.read_text())
    except (OSError,ValueError):return None

def replay_finished(rows,plan):
    return (rows is not None and plan is not None and len(rows)==len(plan)
            and all(r.get('status') in {'passed','failed'} for r in rows))

def main():
    p=argparse.ArgumentParser();p.add_argument('--core-run',required=True);p.add_argument('--integration',action='append',required=True)
    p.add_argument('--wait-integration',action='append',default=[])
    p.add_argument('--native-control',action='append',default=[])
    a=p.parse_args()
    core=OUT/(a.core_run+'-core-matrix.json')
    while True:
        try:rows=json.loads(core.read_text())
        except (OSError,ValueError):rows=[]
        replays=all(replay_finished(read(OUT/(run+'-integration.json')),read(OUT/(run+'-plan.json'))) for run in a.wait_integration)
        controls=[read(OUT/(run+'-native-login-control.json')) for run in a.native_control]
        if (len(rows)==12 and all(r.get('status') in {'passed','failed'} for r in rows)
                and replays and all(r and r.get('finished_at') for r in controls)):break
        time.sleep(2)
    here=Path(__file__).parent
    commands=[[sys.executable,str(here/'verify_release.py')],[sys.executable,str(here/'collect_headless.py')],
              [sys.executable,str(here/'collect_results.py'),'--core',str(core),*[v for name in a.integration for v in ['--integration',name]]]]
    for command in commands:subprocess.run(command,cwd=ROOT,check=True)
    files=sorted(p for p in here.rglob('*') if p.is_file() and p.suffix in {'.py','.java','.txt','.gradle'} and '__pycache__' not in p.parts)
    snapshot={'recorded_at':datetime.datetime.now(datetime.timezone.utc).isoformat(),
              'note':'Current source snapshot only. Earlier live attempts retain their own driver/QA/helper identities and are not claimed to have run later edits.',
              'files':[{'file':p.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in files]}
    (OUT/'test-source-snapshot.json').write_text(json.dumps(snapshot,indent=2)+'\n')
    summary=json.loads((OUT/'summary.json').read_text())
    completion={'queue_finished':True,'audit_status':summary['status'],'totals':summary['totals'],
                'completed_at':datetime.datetime.now(datetime.timezone.utc).isoformat(),
                'note':'Queue completion is not a clean functional result. See failures, entry-only coverage and repository checks.'}
    completion['native_login_controls']=[{'run':run,'status':read(OUT/(run+'-native-login-control.json'))['status']}
                                         for run in a.native_control]
    (OUT/'queue-completion.json').write_text(json.dumps(completion,indent=2)+'\n')
    print(json.dumps(completion),flush=True)

if __name__=='__main__':main()
