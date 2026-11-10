"""Replay remaining failures/current-subject gaps after the broad serial queue."""
from pathlib import Path
import argparse,json,subprocess,sys,time
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'output/project-everything-regression'

def read(name):
    try:return json.loads((OUT/name).read_text())
    except (OSError,ValueError):return None

def finished(name,expected):
    rows=read(name+'-integration.json')
    return rows is not None and len(rows)==expected and all(r.get('status') in {'passed','failed'} for r in rows)

def latest_whole(matrices):
    latest={}
    for rows in matrices:
        for original in rows:
            if original.get('scope') in {'smoke','case-reproduction'}:continue
            row={**original,'fixture':{'1201-fabric-regression-inventory-no-guard':'1201-fabric-inventory-batch-v2'}.get(original['fixture'],original['fixture'])}
            latest[(row['target'],row['fixture'])]=row
    return list(latest.values())

def main():
    p=argparse.ArgumentParser();p.add_argument('--original',required=True);p.add_argument('--after',required=True);p.add_argument('--fill',required=True);p.add_argument('--run',required=True);a=p.parse_args()
    expected_fill=len(read('coverage-fill-fixtures.json'))
    while True:
        plan=read(a.after+'-plan.json')
        if plan is not None and finished(a.after,len(plan)) and finished(a.fill,expected_fill):break
        time.sleep(2)
    latest=latest_whole([read(name+'-integration.json') for name in [a.original,a.after,a.fill]])
    # Do not replace a whole cohort with a partial replay. This may repeat an
    # already corrected small case, but cannot hide another failed interface.
    source=a.run+'-source';path=OUT/(source+'-integration.json');assert not path.exists()
    path.write_text(json.dumps(latest,indent=2)+'\n')
    command=[sys.executable,str(Path(__file__).with_name('run_followups.py')),'--source',source,'--run',a.run]
    raise SystemExit(subprocess.run(command,cwd=ROOT).returncode)

if __name__=='__main__':main()
