"""Resumable, sequential release/gameplay matrix. Stops on failure and preserves attempts."""
from pathlib import Path
import argparse, hashlib, json, shutil, subprocess, sys
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[1]
PHASES = ['core', 'compatibility', 'toms-pickup', 'mouse-tweaks', 'raised', 'spatial', 'controller', 'ae2-atlas', 'config', 'ancient-remnants', 'startup', 'accessories', 'dedicated-server']
def now(): return datetime.now(timezone.utc).isoformat()
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()

def plan(phase, run_id):
    targets = json.loads((ROOT/'compatibility/coverage-targets.json').read_text())['targets']
    rows = []
    def add(target, name, script, args, kinds=('core',), gradle=False):
        mc, loader = target['minecraft'], target['loader']
        rows.append(dict(key=f'{mc}:{loader}:{name}', minecraft=mc, loader=loader,
                         name=name, script=script, args=args, kinds=list(kinds), gradle=gradle))
    for t in targets:
        mc, loader = t['minecraft'], t['loader']
        select = ['-Minecraft', mc, '-Loader', loader]
        if phase == 'core':
            add(t, 'complete-core', 'functional-tests/scripts/Run-ReleaseGameplayQa.ps1',
                select+['-Scenario','core','-CompleteCore','-RunId','{runid}'])
        elif phase == 'compatibility':
            providers={'browser-jei':'jei','browser-rei':'rei','browser-emi':'emi','toms':'toms-storage','sophisticated':'sophisticated-core','ae2':'ae2'}
            for scenario in ['extension-empty',*providers]:
                if scenario in providers and not t['integrations'][providers[scenario]]['available']: continue
                add(t, scenario, 'functional-tests/scripts/Run-ReleaseGameplayQa.ps1',
                    select+['-Scenario',scenario,'-IntegrationOnly','-RunId','{runid}'],('core','compatibility'))
        elif phase == 'toms-pickup' and t['integrations']['toms-storage']['available']:
            add(t,'toms-pickup-both-terminals','functional-tests/scripts/Run-ReleaseGameplayQa.ps1',
                select+['-Scenario','toms','-TomsPickupOnly','-RunId','{runid}'],('core','compatibility'))
        elif phase == 'mouse-tweaks':
            add(t, 'mouse-tweaks', 'functional-tests/scripts/Run-MouseTweaksQa.ps1',select+['-RunId','{runid}'])
        elif phase == 'raised':
            add(t, 'raised-with-mouse-tweaks', 'functional-tests/scripts/Run-RaisedHudQa.ps1',
                select+['-Mode','raised','-MouseTweaks','-RunId','{runid}'],('core','compatibility'))
        elif phase == 'spatial':
            add(t, 'complete-spatial', 'functional-tests/scripts/Run-SpatialGuiQa.ps1',
                select+['-Mode','present','-Complete','-UseDistribution','-RunId','{runid}'])
        elif phase == 'config':
            add(t, 'config-entrypoint', 'functional-tests/scripts/Run-ConfigMenuQa.ps1',select+['-UseDistribution','-RunId','{runid}'])
        elif phase == 'dedicated-server':
            production_flags=['--production'] if (mc,loader)==('1.20.1','forge') else []
            servers=['core','extension-empty']
            for scenario,project in [('ae2','ae2'),('toms','toms-storage'),('sophisticated','sophisticated-core')]:
                if t['integrations'][project]['available']: servers.append(scenario)
            for scenario in servers:
                add(t,'server-'+scenario,'tools/run-release-server-qa.py',
                    ['--minecraft',mc,'--loader',loader,'--scenario',scenario,'--run-id','{runid}']+production_flags,
                    ('core',) if scenario=='core' else ('core','compatibility'))
            accessory='trinkets' if loader=='fabric' and mc in ['1.20.1','1.21.1'] else 'curios' if loader!='fabric' else None
            if accessory:
                add(t,'server-'+accessory,'tools/run-release-server-qa.py',
                    ['--minecraft',mc,'--loader',loader,'--scenario','extension-empty','--accessory',accessory,'--run-id','{runid}']+production_flags,('core','compatibility'))
        elif phase == 'accessories':
            accessory='trinkets' if loader=='fabric' and mc in ['1.20.1','1.21.1'] else 'curios' if loader!='fabric' else None
            if accessory:
                add(t,accessory,'functional-tests/scripts/Run-ReleaseGameplayQa.ps1',
                    select+['-Scenario','extension-empty','-IntegrationOnly','-Accessory',accessory,'-RunId','{runid}']+
                    (['-Trace'] if accessory=='curios' else []),('core','compatibility'))
        elif phase == 'ancient-remnants' and mc in ['1.20.1','1.21.1','26.1.2']:
            browsers=['rei']
            if (mc,loader) in [('1.21.1','fabric'),('26.1.2','neoforge')]: browsers.append('jei')
            for browser in browsers:
                add(t,'ancient-'+browser,'functional-tests/scripts/Run-AncientRemnantsQa.ps1',
                    select+['-Browser',browser,'-RunId','{runid}'],('core','compatibility'))
        elif phase == 'ae2-atlas' and t['integrations']['ae2']['available']:
            add(t, 'ae2-atlas', 'functional-tests/scripts/Run-Ae2ReleaseQa.ps1',
                select+['-RunId','{runid}'],('core','compatibility'))
            add(t, 'ae2-cpu-lifecycle', 'functional-tests/scripts/Run-Ae2ReleaseQa.ps1',
                select+['-CpuLifecycle','-RunId','{runid}'],('core','compatibility'))
    if phase == 'controller':
        rows.append(dict(key='all:controller', minecraft=None, loader=None, name='controller',
            script='tools/Run-ControlifyReleaseQa.ps1',args=['-RunId',run_id,'-Phase','all','-Resume'],
            kinds=['core','compatibility','controlify'],gradle=False))
    if phase == 'startup':
        rows.append(dict(key='all:startup',minecraft=None,loader=None,name='startup',
            script='functional-tests/scripts/Run-FunctionalMatrix.ps1',
            args=['-Runtime','-TimeoutSeconds','600','-ResultsDir',str(ROOT/'build/full-validation-20261023'/run_id/'startup/logs'),'-RunId',run_id],
            kinds=['core'],gradle=False))
    return rows

def fingerprints(case):
    result={}
    for kind in case['kinds']:
        for p in sorted((ROOT/'dist'/kind).glob('*.jar')):
            if case['minecraft'] and not f"-{case['minecraft']}-{case['loader'].lower()}-" in p.name.lower(): continue
            result[p.relative_to(ROOT).as_posix()]=sha(p)
    if not result: raise RuntimeError('No production inputs for '+case['key'])
    return result

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--phase',choices=PHASES,required=True)
    parser.add_argument('--run-id',default='fullval-20261023')
    parser.add_argument('--resume',action='store_true')
    parser.add_argument('--exclude-target',action='append',default=[])
    parser.add_argument('--case',action='append',default=[],help='Run selected case keys while retaining the complete phase plan')
    parser.add_argument('--plan-only',action='store_true')
    args=parser.parse_args()
    if not all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.run_id): parser.error('Use a lowercase run ID')
    rows=[r for r in plan(args.phase,args.run_id) if f"{r['minecraft']}:{r['loader']}" not in args.exclude_target]
    if set(args.case)-{r['key'] for r in rows}: parser.error('Unknown selected case key')
    if args.plan_only:
        print(json.dumps(rows,indent=2));return
    output=ROOT/'build/full-validation-20261023'/args.run_id/args.phase
    output.mkdir(parents=True,exist_ok=True)
    manifest=output/'results.json'
    previous=json.loads(manifest.read_text())['cases'] if manifest.exists() and args.resume else []
    if manifest.exists() and not args.resume: raise RuntimeError('Use --resume or a new run ID')
    old={r['key']:r for r in previous}
    for r in rows:
        r.update(status='PENDING',attempts=old.get(r['key'],{}).get('attempts',[]))
        if r['key'] in old and args.resume and old[r['key']].get('status')=='PASS' and old[r['key']].get('artifacts')==fingerprints(r):
            r.update(status='PASS',artifacts=old[r['key']]['artifacts'])
    def save():
        manifest.write_text(json.dumps(dict(run_id=args.run_id,phase=args.phase,updated_at=now(),cases=rows),indent=2)+'\n')
    save()
    pwsh=shutil.which('pwsh')
    if not pwsh: raise RuntimeError('PowerShell 7 is required')
    if args.phase=='mouse-tweaks':
        subprocess.run([sys.executable,str(ROOT/'tools/prepare-mouse-tweaks-qa.py')],cwd=ROOT,check=True)
    for r in rows:
        if args.case and r['key'] not in args.case: continue
        if r['status']=='PASS': print('VERIFIED PASS',r['key'],flush=True);continue
        if (output/'stop.request').exists(): raise RuntimeError('Stopped at case boundary')
        slug=(args.run_id+'-'+r['name']+'-'+str(r['minecraft'])+'-'+str(r['loader'])).replace('.','-')
        attempt=len(r['attempts'])+1
        run_id=slug+f'-a{attempt}'
        log=output/(run_id+'.log')
        values=[v.replace('{runid}',run_id) for v in r['args']]
        command=[pwsh,'-NoProfile','-File',str(ROOT/r['script']),*values]
        if r['script'].endswith('.py'): command=[sys.executable,str(ROOT/r['script']),*values]
        if r['gradle']:
            # Static task/flag arguments only; use PowerShell to execute the batch wrapper.
            command=[pwsh,'-NoProfile','-Command','& $args[0] @($args | Select-Object -Skip 1); exit $LASTEXITCODE',str(ROOT/'gradlew.bat'),*values]
        qa_sources=[ROOT/'functional-tests/src/main/java/com/salts_inventory_update/functionaltest/InWorldGameplayHarness.java',ROOT/r['script']]
        entry=dict(run_id=run_id,started_at=now(),log=log.relative_to(ROOT).as_posix(),command=command,
                   qa_inputs={p.relative_to(ROOT).as_posix():sha(p) for p in qa_sources})
        r['attempts'].append(entry);r.update(status='RUNNING',artifacts=fingerprints(r));save()
        print('START',r['key'],run_id,flush=True)
        with log.open('w',encoding='utf-8') as stream:
            result=subprocess.run(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,
                                  creationflags=subprocess.CREATE_NO_WINDOW if sys.platform=='win32' else 0)
        entry.update(finished_at=now(),exit_code=result.returncode)
        r['status']='PASS' if result.returncode==0 else 'FAIL';save()
        print(r['status'],r['key'],str(log),flush=True)
        if result.returncode: raise RuntimeError('Case failed; inspect evidence before resuming: '+r['key'])
    print(f"Completed {sum(r['status']=='PASS' for r in rows)}/{len(rows)} current-artifact cases: {manifest}",flush=True)

if __name__=='__main__': main()
