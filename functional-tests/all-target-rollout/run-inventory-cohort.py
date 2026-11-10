"""Serialize the affected container-entry captures in an already running client."""
from pathlib import Path
import argparse,json,subprocess,sys,time

ROOT=Path(__file__).resolve().parents[2]
HERE=Path(__file__).parent
p=argparse.ArgumentParser();p.add_argument('run',type=Path)
p.add_argument('--side',choices=['native','siu'],required=True)
p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True)
a=p.parse_args();deadline=time.monotonic()+300
while True:
    try:
        if json.loads((a.run/'ready.json').read_text()).get('ready'):break
    except (FileNotFoundError,json.JSONDecodeError):pass
    if time.monotonic()>deadline:raise TimeoutError('Disposable client did not reach its ready world')
    time.sleep(1)
common=['--side',a.side,'--minecraft',a.minecraft,'--loader',a.loader]
jobs=[('capture-container-batch.py',['--siu',str(a.run),'--native',str(a.run),*common]),
      ('capture-item-batch.py',[str(a.run),*common,'--capture-only']),
      ('run-capture-sequence.py',[str(a.run),*common,'--profile','inventory-rest'])]
rows=[]
for name,args in jobs:
    log=a.run/(name.removesuffix('.py')+'-cohort.log');print('START',name,flush=True)
    with log.open('w',encoding='utf-8') as out:
        result=subprocess.run([sys.executable,'-X','utf8',str(HERE/name),*args],cwd=ROOT,stdout=out,stderr=subprocess.STDOUT)
    rows.append({'driver':name,'exit_code':result.returncode,'log':str(log),
                 'scope':'Focused natural entry, pictured states and explicitly named feature checks; not a complete gameplay suite'})
    (a.run/'inventory-cohort.json').write_text(json.dumps(rows,indent=2))
    print('END',name,result.returncode,flush=True)
    if result.returncode:raise SystemExit(result.returncode)
