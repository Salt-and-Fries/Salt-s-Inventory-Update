"""Named remaining inventory-entry pictures only; never a gameplay suite."""
from pathlib import Path
import argparse,json,subprocess,sys,time
HERE=Path(__file__).parent
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);a=p.parse_args()
deadline=time.monotonic()+300
while not (a.run/'ready.json').exists():
 if time.monotonic()>deadline:raise TimeoutError('World did not reach ready state')
 time.sleep(1)
common=[str(a.run),'--side',a.side,'--minecraft',a.minecraft,'--loader',a.loader]
manifest=f"build/all-target-rollout/themed-reviewed-cases-{a.minecraft.replace('.','')}-{a.loader}.json"
jobs=[('capture-reviewed-manifests.py',common+['--manifest',manifest,'--only-case','bf-wolf-isolated','--only-case','bf-gallery-painting-selector','--only-case','bf-moa-clean','--record','remaining-reviewed-tail']),
      ('capture-remaining-reviewed-extra.py',common),
      ('capture-equipment-panel.py',common+['--mod','curios'])]
rows=[]
for name,args in jobs:
 log=a.run/(name.removesuffix('.py')+'-remaining-tail.log');print('START',name,flush=True)
 with log.open('w',encoding='utf-8') as stream:r=subprocess.run([sys.executable,'-X','utf8',str(HERE/name),*args],stdout=stream,stderr=subprocess.STDOUT)
 rows.append({'driver':name,'exit_code':r.returncode,'log':str(log),'scope':'Named ordinary UI entry and pictured default states; not a functional suite'})
 (a.run/'remaining-reviewed-tail-captures.json').write_text(json.dumps(rows,indent=2));print('END',name,r.returncode,flush=True)
