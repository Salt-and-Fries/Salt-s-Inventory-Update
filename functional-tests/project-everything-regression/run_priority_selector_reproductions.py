"""Finish the active owned client, then reproduce selectors and held bags serially."""
from pathlib import Path
import argparse,datetime,json,subprocess,sys
import psutil
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'output/project-everything-regression'

def main():
    p=argparse.ArgumentParser();p.add_argument('--coordinator',type=int,required=True);p.add_argument('--driver',type=int,required=True);p.add_argument('--run',required=True);p.add_argument('--target',action='append');p.add_argument('--scenario',choices=['selectors','themed','themed-requester','input-debug','ae2-filter','harness-fixtures','closed-stock','native-approach','empty-churn','native-enum','revision-stack','isolated-charm'],default='selectors');a=p.parse_args()
    parent=psutil.Process(a.coordinator);child=psutil.Process(a.driver)
    assert 'run_followups.py' in ' '.join(parent.cmdline()) and 'project-everything-retries-01' in parent.cmdline()
    assert child.ppid()==parent.pid and any(script in ' '.join(child.cmdline()) for script in ['run_mod_matrix.py','run_production_matrix.py'])
    created=parent.create_time();report=OUT/(a.run+'-integration.json');assert not report.exists()
    reservation={'coordinator':parent.pid,'created':created,'active_driver':child.pid,
                 'reason':'Finish active client, reproduce native browser and held-item interactions, resume broad queue in finally.',
                 'started_at':datetime.datetime.now(datetime.timezone.utc).isoformat(),'resumed':False}
    proof=OUT/(a.run+'-queue-reservation.json');rows=[]
    parent.suspend()
    try:
        proof.write_text(json.dumps(reservation,indent=2)+'\n')
        assert all(c.pid==child.pid for c in parent.children()),'Coordinator advanced before reservation'
        child.wait()
        jobs=[('1.21.1/fabric',0,['bb-polymer-creative-browser','item-wands-magic-bag-1','sophisticated-backpack','travelers-backpack','atlas-aircraft-airship','atlas-ae2-terminal']),
              ('1.21.1/neoforge',0,['sophisticated-backpack','travelers-backpack','atlas-aircraft-airship','atlas-ae2-terminal']),
              ('1.20.1/forge',0,['travelers-backpack','atlas-travelers-settings']),
              ('1.20.1/forge',3,['registry-projecte-relay_mk1','registry-projecte-relay_mk2','registry-projecte-relay_mk3'])]
        if a.scenario=='themed':
            jobs=[('1.20.1/forge',1,['farm-and-charm-cooking-pot','candlelight-oak-drawer','registry-aether-altar']),
                  ('1.21.1/neoforge',1,['spawn-pigment-shifter','exposure-camera'])]
        if a.scenario=='themed-requester':
            from run_mod_matrix import FIXTURES
            jobs=[('1.20.1/forge',1,['spawn-pigment-shifter','exposure-camera']),
                  ('1.21.1/neoforge',1,['spawn-pigment-shifter','exposure-camera'])]
            for target,fixture in [('1.21.1/neoforge','regression-recent-06-1211-neoforge'),
                                   ('26.1.2/neoforge','regression-recent-07-2612-neoforge')]:
                jobs.append((target,FIXTURES[target].index(fixture),['registry-merequester-requester','atlas-merequester-wireless-terminal']))
        if a.scenario=='input-debug':
            from run_mod_matrix import FIXTURES
            cohort=FIXTURES['26.1.2/neoforge'].index('regression-recent-07-2612-neoforge')
            jobs=[('1.21.1/neoforge',1,['spawn-pigment-shifter']),
                  ('26.1.2/neoforge',cohort,['registry-merequester-requester','atlas-merequester-wireless-terminal']),
                  ('26.1.2/neoforge',cohort,['registry-merequester-requester','atlas-merequester-wireless-terminal'])]
        if a.scenario=='ae2-filter':
            jobs=[('26.3/neoforge',0,['atlas-ae2-terminal']),
                  ('1.21.1/neoforge',0,['atlas-ae2-terminal'])]
        if a.scenario=='harness-fixtures':
            jobs=[('1.20.1/fabric',1,['atlas-bountiful','atlas-seasons-chiller']),
                  ('1.20.1/forge',1,['spawn-pigment-shifter','shared-ag-bakery-small-pot'])]
        if a.scenario=='closed-stock':
            jobs=[('1.20.1/fabric',1,['atlas-bountiful','atlas-seasons-chiller']),
                  ('1.21.1/fabric',1,['atlas-bountiful','atlas-seasons-chiller'])]
        if a.scenario=='native-approach':
            jobs=[('1.21.1/neoforge',1,['farm-and-charm-cooking-pot','farm-and-charm-roaster','farm-and-charm-stove',
                'registry-smallships-dockyard','atlas-vinery-apple-press','atlas-vinery-fermentation-barrel'])]
        if a.scenario=='empty-churn':
            # Keep the first failing publisher and its next two cases in their
            # actual order; a fresh control omits that first publisher menu.
            jobs=[('1.20.1/fabric',2,['registry-galosphere-combustion_table','registry-meadow-cheese_form','registry-meadow-cooking_cauldron']),
                  ('1.20.1/fabric',2,['registry-meadow-cheese_form','registry-meadow-cooking_cauldron'])]
        if a.scenario=='native-enum':
            jobs=[('1.21.1/fabric',2,['charm-kiln'])]
        if a.scenario=='revision-stack':
            jobs=[('1.20.1/fabric',2,['registry-galosphere-combustion_table','registry-meadow-cheese_form','registry-meadow-cooking_cauldron'])]
        if a.scenario=='isolated-charm':jobs=[('1.21.1/fabric',2,[])]
        for index,(target,cohort,only) in enumerate(jobs):
            if a.target and target not in a.target:continue
            from run_mod_matrix import FIXTURES
            fixture=FIXTURES[target][cohort];name=a.run+'-j'+str(index)
            if a.scenario=='isolated-charm':fixture='1211-fabric-charm-trinkets-tail-v1';cohort=0
            log=ROOT/'build/project-everything-regression'/(name+'.log')
            row={'target':target,'fixture':fixture,'cohort':cohort,'scope':'smoke','only':only,'status':'running','log':log.relative_to(ROOT).as_posix()};rows.append(row)
            report.write_text(json.dumps(rows,indent=2)+'\n');print('PRIORITY',target,fixture,flush=True)
            production=target.endswith(('/fabric','/forge'))
            script='run_production_matrix.py' if production else 'run_mod_matrix.py'
            command=[sys.executable,str(Path(__file__).with_name(script)),'--run',name,'--target',target,'--cohort',str(cohort)]
            for case in only:command+=['--only',case]
            if a.scenario=='native-enum':command=[sys.executable,str(Path(__file__).with_name('run_native_charm_probe.py')),'--run',name]
            if a.scenario=='revision-stack':command+=['--revision-stack-agent']
            if a.scenario=='isolated-charm':command=[sys.executable,str(Path(__file__).with_name('run_isolated_charm_control.py')),'--run',name]
            with log.open('x') as stream:result=subprocess.run(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT)
            row.update(status='passed' if result.returncode==0 else 'failed',exit_code=result.returncode)
            matrix=OUT/(name+('-production-mod-matrix.json' if production else '-mod-matrix.json'))
            if a.scenario=='native-enum':matrix=OUT/(name+'-native-mod-matrix.json')
            if matrix.exists():row['results']=json.loads(matrix.read_text())
            report.write_text(json.dumps(rows,indent=2)+'\n');print('PRIORITY END',target,row['status'],flush=True)
    finally:
        if parent.is_running() and parent.create_time()==created:parent.resume();reservation['resumed']=True
        reservation['finished_at']=datetime.datetime.now(datetime.timezone.utc).isoformat();proof.write_text(json.dumps(reservation,indent=2)+'\n')

if __name__=='__main__':main()
