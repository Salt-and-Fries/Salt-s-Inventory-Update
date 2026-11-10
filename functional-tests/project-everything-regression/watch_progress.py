"""Read-only serial-queue progress; yield on a new failure for investigation."""
from pathlib import Path
import argparse,datetime,json,time
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'output/project-everything-regression'

def read(path,default=None):
    try:return json.loads(path.read_text())
    except (OSError,ValueError):return default

def profile_for(run,row):
    target=row['target'];mc,loader=target.split('/');index=row['cohort']
    job=Path(row['log']).stem if '-j' in Path(row['log']).stem else run+'-c'+str(index)
    return ROOT/('build/remaining-ui-audit/gameplay' if loader in {'fabric','forge'} else 'build/all-target-rollout/gameplay')/(target.replace('/','-')+('-siu-' if loader in {'fabric','forge'} else '-')+job+'-'+str(index))

def failure_alerts(profile,row,checks,seen):
    """Catch final-case and startup failures as well as actively running cases."""
    alerts=[]
    for case in checks.get('cases',[]):
        key=profile.as_posix()+'#'+case['id']
        if case.get('status')=='failed' and key not in seen:
            seen.add(key);alerts.append({'case':case['id'],'error':case.get('error',''),
                'profile':profile.relative_to(ROOT).as_posix(),'target':row['target']})
    # A client can crash before a case is written, or after its final assertion.
    # Neither may disappear simply because its coordinator already marked it done.
    key=profile.as_posix()+'#cohort-runtime'
    if (row.get('status')=='failed' and key not in seen
            and not any(c.get('status')=='failed' for c in checks.get('cases',[]))):
        seen.add(key);alerts.append({'case':'cohort-runtime','error':row.get('error') or
            'Cohort exited unsuccessfully; inspect its startup/exit log and incomplete selections.',
            'log':row.get('log'),'profile':profile.relative_to(ROOT).as_posix(),'target':row['target']})
    return alerts

def main():
    p=argparse.ArgumentParser();p.add_argument('--integration',action='append',required=True);a=p.parse_args()
    seen_path=OUT/'progress-seen-failures.json';seen=set(read(seen_path,[]));last=0;last_checks={}
    while True:
        active=[];totals={};alerts=[]
        for run in a.integration:
            rows=read(OUT/(run+'-integration.json'),[])
            totals[run]={'finished':sum(r.get('status') in {'passed','failed'} for r in rows),'started':len(rows)}
            for row in rows:
                if row.get('status') not in {'running','failed'}:continue
                profile=profile_for(run,row);checks=read(profile/'mod-interaction-checks.json')
                # Writers save a growing JSON report in place. A read during
                # that write must not reset progress to a misleading 0/0.
                if checks is not None:last_checks[profile]=checks
                else:checks=last_checks.get(profile,{})
                alerts.extend(failure_alerts(profile,row,checks,seen))
                if row.get('status')!='running':continue
                counts={s:sum(c.get('status')==s for c in checks.get('cases',[])) for s in ['passed','failed','entry-and-policy-only','running']}
                active.append({'run':run,'target':row['target'],'fixture':row['fixture'],'profile':profile.relative_to(ROOT).as_posix(),
                    'cases':len(checks.get('cases',[])),'selected':len(checks.get('selected_case_ids',[])),
                    'current_case':next((c['id'] for c in reversed(checks.get('cases',[])) if c.get('status')=='running'),None),
                    'assertions':len(checks.get('checks',[])),'counts':counts})
        finished=read(OUT/'queue-completion.json')
        status={'at':datetime.datetime.now(datetime.timezone.utc).isoformat(),'active':active,'groups':totals,'new_failures':alerts,'completion':finished}
        (OUT/'live-progress.json').write_text(json.dumps(status,indent=2)+'\n')
        if alerts:
            seen_path.write_text(json.dumps(sorted(seen),indent=2)+'\n');print('NEW FAILURE '+json.dumps(alerts),flush=True);return
        if finished:print('QUEUE FINISHED '+json.dumps(finished),flush=True);return
        if time.monotonic()-last>=50:
            if active:
                for r in active:print(f"LIVE {r['target']} / {r['fixture']}: {r['cases']}/{r['selected']} cases, {r['assertions']} assertions; {r['counts']}; active={r['current_case']}",flush=True)
            else:print('LIVE Between serial cohorts; waiting for the next launch.',flush=True)
            last=time.monotonic()
        time.sleep(3)

if __name__=='__main__':main()
