"""Named remaining 1.20.1 dialogs; UI entry/capture only, no full suite."""
from pathlib import Path
import argparse,json,subprocess,sys,time
HERE=Path(__file__).parent
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',required=True);p.add_argument('--minecraft',default='1.20.1');p.add_argument('--loader',default='forge');a=p.parse_args()
base=[str(a.run),'--side',a.side,'--minecraft',a.minecraft,'--loader',a.loader]
deadline=time.monotonic()+300
while not (a.run/'ready.json').exists():
 if time.monotonic()>deadline:raise TimeoutError('Reference world not ready')
 time.sleep(1)
sys.path.insert(0,str(HERE));from evidence import send
send(a.run,'command',command='fill 5 64 5 12 64 14 minecraft:stone',ticks=3)
jobs=[('capture-wands-tools.py',base),
      ('capture-container-batch.py',['--siu',str(a.run),'--native',str(a.run),*base[1:],'--only','betterarcheology-fossil-inventory']),
      ('capture-sophisticated-settings.py',base),('capture-travelers-settings.py',base),
      ('capture-equipment-panel.py',base+['--mod','curios']),
      ('capture-ae2-submit-error.py',base),('capture-ae2-client-subviews.py',base)]
rows=[]
for name,args in jobs:
 log=a.run/(name.removesuffix('.py')+'-final-oldest.log');print('START',name,flush=True)
 with log.open('w',encoding='utf-8') as stream:r=subprocess.run([sys.executable,'-X','utf8',str(HERE/name),*args],stdout=stream,stderr=subprocess.STDOUT)
 rows.append({'driver':name,'exit_code':r.returncode,'log':str(log),'scope':'Named remaining UI entries/screenshots only; no full gameplay suite'})
 (a.run/'final-oldest-tail-captures.json').write_text(json.dumps(rows,indent=2));print('END',name,r.returncode,flush=True)
