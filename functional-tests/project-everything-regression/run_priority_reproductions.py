"""Reserve a boundary in an owned serial queue for focused defect reproductions."""
from pathlib import Path
import argparse,datetime,json,subprocess,sys
import psutil
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'output/project-everything-regression'

def main():
    p=argparse.ArgumentParser();p.add_argument('--coordinator',type=int,required=True);p.add_argument('--driver',type=int,required=True);p.add_argument('--run',required=True);a=p.parse_args()
    parent=psutil.Process(a.coordinator);child=psutil.Process(a.driver)
    assert 'run_followups.py' in ' '.join(parent.cmdline()) and 'project-everything-retries-01' in parent.cmdline()
    assert child.ppid()==parent.pid and 'run_mod_matrix.py' in ' '.join(child.cmdline())
    created=parent.create_time();report=OUT/(a.run+'-integration.json');assert not report.exists()
    reservation={'coordinator':parent.pid,'created':created,'active_driver':child.pid,
                 'reason':'Let the active client finish, verify newly corrected defect paths serially, then resume the broad queue.',
                 'started_at':datetime.datetime.now(datetime.timezone.utc).isoformat(),'resumed':False}
    proof=OUT/(a.run+'-queue-reservation.json');rows=[]
    parent.suspend()
    try:
        proof.write_text(json.dumps(reservation,indent=2)+'\n')
        assert all(p.pid==child.pid for p in parent.children()),'Coordinator advanced to another driver before the reservation'
        child.wait()
        jobs=[('1.21.1/neoforge','regression-recent-20-1211-neoforge',9,['registry-powah-energy_cable_starter','registry-powah-furnator_starter']),
              ('26.1.2/neoforge','regression-recent-21-2612-neoforge',2,['registry-powah-energy_cable_starter','registry-powah-furnator_starter']),
              ('1.21.1/neoforge','regression-1.21.1-neoforge-integrated-ssn-implemented-04',11,['registry-storagenetwork-inventory','registry-storagenetwork-request','registry-storagenetwork-request_expanded'])]
        for index,(target,fixture,cohort,only) in enumerate(jobs):
            name=a.run+'-j'+str(index);log=ROOT/'build/project-everything-regression'/(name+'.log')
            row={'target':target,'fixture':fixture,'cohort':cohort,'scope':'smoke','only':only,'status':'running','log':log.relative_to(ROOT).as_posix()};rows.append(row)
            report.write_text(json.dumps(rows,indent=2)+'\n');print('PRIORITY',target,fixture,flush=True)
            args=[sys.executable,str(Path(__file__).with_name('run_mod_matrix.py')),'--run',name,'--target',target,'--cohort',str(cohort)]
            for case in only:args+=['--only',case]
            with log.open('w') as stream:result=subprocess.run(args,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT)
            row.update(status='passed' if result.returncode==0 else 'failed',exit_code=result.returncode)
            matrix=OUT/(name+'-mod-matrix.json')
            if matrix.exists():row['results']=json.loads(matrix.read_text())
            report.write_text(json.dumps(rows,indent=2)+'\n');print('PRIORITY END',target,row['status'],flush=True)
    finally:
        if parent.is_running() and parent.create_time()==created:
            parent.resume();reservation['resumed']=True
        reservation['finished_at']=datetime.datetime.now(datetime.timezone.utc).isoformat();proof.write_text(json.dumps(reservation,indent=2)+'\n')

if __name__=='__main__':main()
