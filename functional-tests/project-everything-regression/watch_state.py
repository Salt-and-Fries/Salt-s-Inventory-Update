"""Read-only diagnostic capture for an already-running disposable QA profile."""
from pathlib import Path
import argparse,gzip,json,time

def main():
    p=argparse.ArgumentParser();p.add_argument('profile',type=Path);p.add_argument('--seconds',type=int,default=900);a=p.parse_args()
    target=a.profile/'regression-state.json';output=a.profile/'watched-regression-states.jsonl.gz'
    deadline=time.monotonic()+a.seconds;stamp=None;count=0
    while time.monotonic()<deadline:
        try:
            updated=target.stat().st_mtime_ns
            if updated!=stamp:
                state=json.loads(target.read_text());command=json.loads((a.profile/'command.json').read_text())
                with gzip.open(output,'at',encoding='utf-8') as out:out.write(json.dumps({'mtime_ns':updated,'command_at_capture':command,'state':state})+'\n')
                stamp=updated;count+=1
        except (OSError,ValueError):pass
        time.sleep(.025)
    print('Captured',count,'read-only native-state snapshots into',output,flush=True)

if __name__=='__main__':main()
