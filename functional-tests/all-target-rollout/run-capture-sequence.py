"""Serialize focused picture drivers in one disposable client; never a full suite."""
from pathlib import Path
import argparse,json,subprocess,sys
ROOT=Path(__file__).resolve().parents[2];HERE=Path(__file__).parent
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);p.add_argument('--profile',choices=['inventory1201-rest','inventory1201-tail','inventory-rest','themed1201'],required=True);a=p.parse_args()
common=[str(a.run),'--side',a.side,'--minecraft',a.minecraft,'--loader',a.loader]
if a.profile=='inventory1201-rest':
 assert a.minecraft=='1.20.1'
 jobs=[('capture-ae2-legacy-workflows.py',[str(a.run),'--side',a.side,'--loader',a.loader]),('capture-ae2-legacy-crafting.py',[str(a.run),'--side',a.side,'--loader',a.loader])]
 jobs += [(name,common) for name in ['capture-aircraft-batch.py','capture-bumblezone-modern.py','capture-small-entity-batch.py','focused-tradingpost-modern.py','capture-toms-special.py','capture-toms-customer.py','capture-waystones-modifiers.py']]
elif a.profile=='inventory1201-tail':
 jobs=[('capture-ae2-legacy-crafting.py',[str(a.run),'--side',a.side,'--loader',a.loader])]
 jobs += [(name,common) for name in ['focused-tradingpost-modern.py','capture-toms-special.py','capture-toms-customer.py','capture-waystones-modifiers.py']]
elif a.profile=='inventory-rest':
 catalog=json.loads((a.run/'catalog.json').read_text());blocks=set(catalog['blocks']);items=set(catalog['items']);namespaces={x.split(':')[0] for x in blocks|items};names=[]
 if 'immersive_aircraft' in namespaces:names.append('capture-aircraft-batch.py')
 if 'the_bumblezone' in namespaces:names.append('capture-bumblezone-modern.py')
 if namespaces&{'guardvillagers','niftycarts'}:names.append('capture-small-entity-batch.py')
 if 'tradingpost:trading_post' in blocks:names.append('focused-tradingpost-modern.py')
 if 'toms_storage' in namespaces:names.append('capture-toms-special.py')
 if 'toms_trading_network:vending_machine' in blocks:names.append('capture-toms-customer.py')
 if 'waystones:warp_plate' in items:names.append('capture-placed-warp-plate.py')
 staged=json.loads((a.run/'staged-artifacts.json').read_text(encoding='utf-8-sig'))
 if any('universal-graves' in r['filename'].lower() for r in staged['artifacts']):names.append('capture-graves-production.py')
 jobs=[(name,common) for name in names]
else:
 jobs=[('capture-reviewed-manifests.py',common+['--manifest',str(ROOT/f'build/all-target-rollout/themed-reviewed-cases-1201-{a.loader}.json'),'--record','themed-inventory-captures']),('capture-smallships.py',common)]
rows=[]
for name,args in jobs:
 log=a.run/(name.removesuffix('.py')+'-sequence.log');print('START',name,flush=True)
 with log.open('w',encoding='utf-8') as out:r=subprocess.run([sys.executable,'-X','utf8',str(HERE/name),*args],cwd=ROOT,stdout=out,stderr=subprocess.STDOUT)
 rows.append({'driver':name,'exit_code':r.returncode,'log':str(log),'scope':'Focused entry and pictured states; any feature checks are named in the individual journal'})
 (a.run/('capture-sequence-'+a.profile+'.json')).write_text(json.dumps(rows,indent=2));print('END',name,r.returncode,flush=True)
 if r.returncode:raise SystemExit(r.returncode)
