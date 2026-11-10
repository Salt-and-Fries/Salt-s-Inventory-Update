"""Serial, named reference entries and pictures; not a gameplay suite."""
from pathlib import Path
import argparse,json,subprocess,sys
HERE=Path(__file__).parent
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',required=True);p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);a=p.parse_args()
base=[str(a.run),'--side',a.side,'--minecraft',a.minecraft,'--loader',a.loader]
jobs=[('capture-sophisticated-settings.py',base),
      ('capture-older-publisher-entries.py',base),
      ('run-older-extra-entries.py',base),
      ('capture-ae2-submit-error.py',base)]
rows=[]
for name,args in jobs:
 log=a.run/(name.removesuffix('.py')+'-selected-followup.log');print('START',name,flush=True)
 with log.open('w',encoding='utf-8') as stream:r=subprocess.run([sys.executable,'-X','utf8',str(HERE/name),*args],stdout=stream,stderr=subprocess.STDOUT)
 rows.append({'driver':name,'exit_code':r.returncode,'log':str(log),'scope':'Named entry/navigation and screenshots; no complete gameplay suite'})
 (a.run/'selected-old-followups.json').write_text(json.dumps(rows,indent=2));print('END',name,r.returncode,flush=True)
