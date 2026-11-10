"""Serial named residual UI entries, not the functional suite."""
from pathlib import Path
import argparse,json,subprocess,sys
HERE=Path(__file__).parent
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);a=p.parse_args()
sys.path.insert(0,str(HERE));from evidence import send,capture
base=[str(a.run),'--side',a.side,'--minecraft','1.21.1','--loader','neoforge']
manifest=a.run/'vinery-final-workflows.json';source=Path('build/all-target-rollout/themed-reviewed-cases-1211-neoforge.json')
manifest.write_text(json.dumps({'minecraft':'1.21.1','loader':'neoforge','source_manifest':str(source),'cases':[r for r in json.loads(source.read_text())['cases'] if r['id'].startswith('vinery-')]}))
jobs=[('capture-reviewed-manifests.py',base+['--manifest',str(manifest),'--record','vinery-final-captures']),
 ('capture-oldest-extra.py',base+['--blocks-only']),
 ('capture-older-publisher-entries.py',base+['--only','bountiful-decree-analyzer']),
 ('capture-requester-wireless.py',base),('capture-ae2-submit-error.py',base)]
rows=[]
for name,args in jobs:
 log=a.run/(name.removesuffix('.py')+'-final-extras.log');print('START',name,flush=True)
 with log.open('w',encoding='utf-8') as stream:r=subprocess.run([sys.executable,'-X','utf8',str(HERE/name),*args],stdout=stream,stderr=subprocess.STDOUT)
 rows.append({'driver':name,'exit_code':r.returncode,'log':str(log)});(a.run/'named-final-extras.json').write_text(json.dumps(rows,indent=2));print('END',name,r.returncode,flush=True)
send(a.run,'close',ticks=3);send(a.run,'held',item='minecraft:air',ticks=3);send(a.run,'fixture',block='crabbersdelight:tackle_box',ticks=8)
s=send(a.run,'client-use-block' if a.side=='siu' else 'open',ticks=20)['client']
capture(a.run,a.side,'1.21.1','neoforge','crabbers-tackle-box','Original tackle-box block, ordinary right-click. Displayed inventory only.')
