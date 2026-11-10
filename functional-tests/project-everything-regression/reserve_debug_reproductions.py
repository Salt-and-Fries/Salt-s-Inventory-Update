"""Hold a pending priority boundary for detailed held-menu and ordered AE2 traces."""
from pathlib import Path
import argparse,datetime,json,subprocess,sys
import psutil
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'output/project-everything-regression'

def main():
    p=argparse.ArgumentParser();p.add_argument('--coordinator',type=int,required=True);p.add_argument('--driver',type=int,required=True);p.add_argument('--run',required=True);a=p.parse_args()
    parent=psutil.Process(a.coordinator);child=psutil.Process(a.driver)
    owned_priority=any(v.endswith('run_priority_selector_reproductions.py') for v in parent.cmdline()) and 'project-everything-priority-04' in parent.cmdline()
    owned_broad=any(v.endswith('run_followups.py') for v in parent.cmdline()) and 'project-everything-retries-01' in parent.cmdline()
    assert owned_priority or owned_broad
    assert child.ppid()==parent.pid and any(v.endswith('run_production_matrix.py') for v in child.cmdline())
    report=OUT/(a.run+'-integration.json');assert not report.exists()
    created=parent.create_time();rows=[]
    proof=OUT/(a.run+'-queue-reservation.json')
    reserve={'coordinator':parent.pid,'active_driver':child.pid,'resumed':False,
        'reason':'Finish the active owned client, collect detailed traces serially, resume the same coordinator in finally.',
        'started_at':datetime.datetime.now(datetime.timezone.utc).isoformat()}
    parent.suspend()
    try:
        proof.write_text(json.dumps(reserve,indent=2)+'\n')
        assert all(c.pid==child.pid for c in parent.children())
        child.wait()
        jobs=[('1.21.1/fabric',0,['bb-polymer-creative-browser','item-wands-magic-bag-1','sophisticated-backpack','travelers-backpack']),
              ('1.21.1/neoforge',0,['registry-ae2-condenser','registry-ae2-drive','registry-ae2-inscriber','registry-ae2-interface','registry-ae2-molecular_assembler',
                'atlas-ae2-cell-workbench','atlas-ae2-io-port','atlas-ae2-vibration-chamber','atlas-ae2-sky-stone-chest','atlas-ae2-spatial-io-port','atlas-ae2-spatial-anchor','atlas-ae2-terminal'])]
        from run_mod_matrix import FIXTURES
        for index,(target,cohort,only) in enumerate(jobs):
            name=a.run+'-j'+str(index);production=target.endswith('/fabric');script='run_production_matrix.py' if production else 'run_mod_matrix.py'
            log=ROOT/'build/project-everything-regression'/(name+'.log')
            row={'target':target,'fixture':FIXTURES[target][cohort],'cohort':cohort,'scope':'smoke','only':only,'status':'running','log':log.relative_to(ROOT).as_posix()};rows.append(row)
            report.write_text(json.dumps(rows,indent=2)+'\n');print('DEBUG',target,flush=True)
            args=[sys.executable,str(Path(__file__).with_name(script)),'--run',name,'--target',target,'--cohort',str(cohort)]
            if production:args.append('--desktop-debug')
            for case in only:args+=['--only',case]
            with log.open('x') as stream:result=subprocess.run(args,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT)
            row.update(status='passed' if result.returncode==0 else 'failed',exit_code=result.returncode)
            matrix=OUT/(name+('-production-mod-matrix.json' if production else '-mod-matrix.json'))
            if matrix.exists():row['results']=json.loads(matrix.read_text())
            report.write_text(json.dumps(rows,indent=2)+'\n');print('DEBUG END',target,row['status'],flush=True)
    finally:
        if parent.is_running() and parent.create_time()==created:parent.resume();reserve['resumed']=True
        reserve['finished_at']=datetime.datetime.now(datetime.timezone.utc).isoformat();proof.write_text(json.dumps(reserve,indent=2)+'\n')

if __name__=='__main__':main()
