"""Serialize AE2 19 pictured states and the named shared-cursor regression."""
from pathlib import Path
import argparse,json,subprocess,sys
ROOT=Path(__file__).resolve().parents[2];HERE=Path(__file__).parent
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--loader',required=True);p.add_argument('--minecraft',default='1.21.1');p.add_argument('--start',choices=['parts','configuration','priority','items','crafting'],default='parts');a=p.parse_args()
common=[str(a.run),'--side',a.side,'--loader',a.loader];navigation=['--minecraft',a.minecraft]
names=['network-tool','quartz-cutting-knife','controller-network-status','network-tool-status','crafting-cpu','quantum-network-bridge','priority','terminal-settings','pattern-processing-patterns','pattern-stonecutting-patterns','pattern-smithing-table-patterns','crafting-status','stock-amount']
selected=[value for name in names for value in ['--only',name]]
jobs=[('parts','capture-ae2-parts.py',common+navigation),('configuration','capture-ae2-legacy-workflows.py',common+navigation+selected),('priority','capture-ae2-legacy-workflows.py',common+navigation+['--only','priority'])]
if a.side=='siu' and a.minecraft=='1.21.1':jobs.append(('priority','focused-ae2-transition-cursor.py',[str(a.run)]))
jobs += [('items','capture-ae2-modern-items.py',common+navigation),('crafting','capture-ae2-modern-crafting.py',common+navigation)]
order=['parts','configuration','priority','items','crafting'];rows=[]
for stage,name,args in jobs:
 if order.index(stage)<order.index(a.start):continue
 log=a.run/(name.removesuffix('.py')+'-modern-cohort-'+stage+'.log');print('START',stage,name,flush=True)
 with log.open('w',encoding='utf-8') as out:r=subprocess.run([sys.executable,'-X','utf8',str(HERE/name),*args],cwd=ROOT,stdout=out,stderr=subprocess.STDOUT)
 rows.append({'stage':stage,'driver':name,'exit_code':r.returncode,'log':str(log),'scope':'Named AE2 native UI entries/navigation and shared-cursor regression only; no full gameplay suite'})
 (a.run/'ae2-modern-cohort.json').write_text(json.dumps(rows,indent=2));print('END',stage,name,r.returncode,flush=True)
 if r.returncode:raise SystemExit(r.returncode)
