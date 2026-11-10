"""Selected remaining ordinary UI entries, separate from gameplay testing."""
from pathlib import Path
import argparse,json,subprocess,sys
HERE=Path(__file__).parent
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);a=p.parse_args()
base=[str(a.run),'--side',a.side,'--minecraft',a.minecraft,'--loader',a.loader]
source=Path('build/all-target-rollout/themed-reviewed-cases-1211-neoforge.json');data=json.loads(source.read_text())
ids={'own-supp-red-merchant','ad-astra-giselle-addon-automation-nasa-workbench','ad-astra-giselle-addon-fuel-loader','ad-astra-giselle-addon-gravity-normalizer','ad-astra-giselle-addon-rocket-sensor'}
manifest=a.run/'selected-publisher-entry-workflows.json'
manifest.write_text(json.dumps({'loader':a.loader,'minecraft':a.minecraft,'source_manifest':str(source),'scope':'Reused ordinary workflows, with actual installed registries and publisher hashes checked separately','cases':[r for r in data['cases'] if r['id'] in ids]},indent=2))
jobs=[('capture-specialized-extra.py',base+['--only','tieredz-reforge']),
      ('capture-remaining-reviewed-extra.py',base+['--only','galosphere-combustion-table']),
      ('capture-container-batch.py',['--siu',str(a.run),'--native',str(a.run),*base[1:],'--only','visual-workbench']),
      ('capture-reviewed-manifests.py',base+['--manifest',str(manifest),'--record','selected-publisher-entry-captures'])]
rows=[]
for name,args in jobs:
 log=a.run/(name.removesuffix('.py')+'-older-extra.log');print('START',name,flush=True)
 with log.open('w',encoding='utf-8') as stream:r=subprocess.run([sys.executable,'-X','utf8',str(HERE/name),*args],stdout=stream,stderr=subprocess.STDOUT)
 rows.append({'driver':name,'exit_code':r.returncode,'log':str(log),'scope':'Only named interface entries and pictures; not a gameplay suite'})
 (a.run/'older-extra-captures.json').write_text(json.dumps(rows,indent=2));print('END',name,r.returncode,flush=True)
