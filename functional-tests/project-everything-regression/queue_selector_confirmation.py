"""Reserve one fresh Fabric confirmation after the existing reservation ends."""
from pathlib import Path
import argparse,json,subprocess,sys,time
import psutil
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'output/project-everything-regression'

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--run',required=True)
    parser.add_argument('--after',default='project-everything-priority-07');parser.add_argument('--scenario',default='selectors')
    parser.add_argument('--target',action='append');args=parser.parse_args()
    coordinator=psutil.Process(30584)
    created=coordinator.create_time()
    proof=OUT/(args.after+'-queue-reservation.json')
    while True:
        assert coordinator.is_running() and coordinator.create_time()==created
        try:finished=json.loads(proof.read_text()).get('resumed',False)
        except (OSError,ValueError):finished=False
        children=[c for c in coordinator.children() if any('run_mod_matrix.py' in x or 'run_production_matrix.py' in x for x in c.cmdline())]
        if finished and len(children)==1:break
        time.sleep(1)
    command=[sys.executable,str(Path(__file__).with_name('run_priority_selector_reproductions.py')),
          '--coordinator',str(coordinator.pid),'--driver',str(children[0].pid),
          '--run',args.run,'--scenario',args.scenario]
    for target in args.target or ['1.21.1/fabric']:command+=['--target',target]
    raise SystemExit(subprocess.run(command,cwd=ROOT).returncode)

if __name__=='__main__':main()
