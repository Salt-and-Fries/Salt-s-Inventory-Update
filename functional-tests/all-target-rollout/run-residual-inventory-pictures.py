"""Named residual entry/layout checks only. Never the full functional suite."""
from pathlib import Path
import argparse,json,subprocess,sys,time
HERE=Path(__file__).parent
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);p.add_argument('--case',choices=['tools','12111-fabric','262-neoforge','263-neoforge','wireless','1211-fabric'],required=True);p.add_argument('--finish',action='store_true');a=p.parse_args()
deadline=time.monotonic()+600
while not (a.run/'ready.json').exists():
 if time.monotonic()>deadline:raise TimeoutError('Disposable client did not become ready')
 time.sleep(1)
base=[str(a.run),'--side',a.side,'--minecraft',a.minecraft,'--loader',a.loader];jobs=[]
if a.case in ['12111-fabric','263-neoforge']:
 jobs.append(('capture-container-batch.py',['--native',str(a.run),'--siu',str(a.run),*base[1:],'--only','betterarcheology-fossil-inventory']))
if a.case in ['tools','12111-fabric','262-neoforge','wireless']:jobs.append(('capture-wands-tools.py',base))
if a.case in ['12111-fabric','263-neoforge']:jobs.append(('capture-travelers-settings.py',base))
if a.case=='263-neoforge':
 jobs.append(('capture-ae2-block-batch.py',base+['--only','me_chest']))
 if a.side=='native':jobs.append(('capture-equipment-panel.py',base+['--mod','curios']))
if a.case=='wireless':jobs.append(('capture-requester-wireless.py',base))
if a.case in ['wireless','262-neoforge','263-neoforge']:jobs.append(('capture-ae2-submit-error.py',base))
if a.case=='1211-fabric':
 jobs += [('capture-niftycarts-wagons.py',base),('capture-sophisticated-settings.py',base),('capture-older-publisher-entries.py',base+['--only','bountiful-decree-analyzer'])]
rows=[]
for script,args in jobs:
 log=a.run/(script.removesuffix('.py')+'-residual-focused.log');print('START',script,flush=True)
 with log.open('w',encoding='utf-8') as stream:r=subprocess.run([sys.executable,'-X','utf8',str(HERE/script),*args],stdout=stream,stderr=subprocess.STDOUT)
 rows.append({'driver':script,'exit_code':r.returncode,'log':str(log),'scope':'Named original menu entry and pictured layout only'})
 (a.run/'residual-focused-capture-results.json').write_text(json.dumps(rows,indent=2));print('END',script,r.returncode,flush=True)
 if r.returncode:raise RuntimeError('Focused driver failed; preserve state for investigation: '+script)
if a.finish:
 seq=max(json.loads((a.run/n).read_text()).get('sequence',0) for n in ['command.json','reply.json'])+1
 temporary=a.run/'finish-command.tmp';temporary.write_text(json.dumps({'sequence':seq,'type':'finish','ticks':1}));temporary.replace(a.run/'command.json');print('FIN',seq,flush=True)
