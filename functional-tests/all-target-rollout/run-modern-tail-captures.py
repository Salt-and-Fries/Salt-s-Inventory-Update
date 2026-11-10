"""Bounded, serial inventory-entry screenshots in one already launched client.

Only the named interface drivers run; this is not the functional/gameplay suite.
The client stays open for focused follow-up and visual inspection.
"""
from pathlib import Path
import argparse,json,subprocess,sys,time
HERE=Path(__file__).parent
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True)
p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);p.add_argument('--ae2',action='store_true');a=p.parse_args()
deadline=time.monotonic()+300
while not (a.run/'ready.json').exists():
 if time.monotonic()>deadline:raise TimeoutError('Disposable world has not reached ready state')
 time.sleep(1)
sys.path.insert(0,str(HERE));from evidence import send
send(a.run,'command',command='difficulty peaceful',ticks=2)
send(a.run,'command',command='fill 5 64 5 12 64 14 minecraft:stone',ticks=3)
send(a.run,'catalog',ticks=1);catalog=json.loads((a.run/'catalog.json').read_text())
base=['--side',a.side,'--minecraft',a.minecraft,'--loader',a.loader]
blocks=['rune-crafting','betterend-smelter','betterend-forge','alloyed-forge','alloyed-oven',
        'alloy-forge','illager-imbuing','universal-sawmill','betterarcheology-fossil-inventory','deep-aether-combiner']
jobs=[('capture-container-batch.py',['--siu',str(a.run),'--native',str(a.run),*base,*[v for ui in blocks for v in ['--only',ui]]])]
if any('guardvillagers' in item for item in catalog['items']):jobs.append(('capture-small-entity-batch.py',[str(a.run),*base,'--only','guard-villager']))
if any('curios' in row.get('filename','').lower() for row in json.loads((a.run/'staged-artifacts.json').read_text(encoding='utf-8-sig'))['artifacts']):
 jobs.append(('capture-equipment-panel.py',[str(a.run),*base,'--mod','curios']))
if 'tradingpost:trading_post' in catalog['blocks']:jobs.append(('focused-tradingpost-modern.py',[str(a.run),*base]))
if 'waystones:waystone_modifiers' in catalog['menus']:jobs.append(('capture-waystones-modifiers.py',[str(a.run),*base]))
if any(item.startswith('sophisticated') for item in catalog['items']):jobs.append(('capture-sophisticated-settings.py',[str(a.run),*base]))
if any(item.startswith('toms_storage:') for item in catalog['items']):jobs.append(('capture-toms-special.py',[str(a.run),*base]))
if 'toms_trading_network:vending_machine' in catalog['blocks']:jobs.append(('capture-toms-customer.py',[str(a.run),*base]))
jobs.append(('capture-item-batch.py',[str(a.run),*base,'--capture-only']))
if 'travelersbackpack:standard' in catalog['items']:jobs.append(('capture-travelers-settings.py',[str(a.run),*base]))
if 'wands:diamond_wand' in catalog['items']:jobs.append(('capture-wands-tools.py',[str(a.run),*base]))
if 'merequester:requester' in catalog['blocks']:
 jobs.append(('capture-reviewed-manifests.py',[str(a.run),*base,'--manifest','build/all-target-rollout/themed-reviewed-cases-1211-neoforge.json','--only-case','merequester-requester','--only-case','own-ae-part-requester-terminal','--record','current-requester-entry-captures']))
if a.ae2 and 'ae2:me_chest' in catalog['blocks']:
 jobs.append(('capture-ae2-block-batch.py',[str(a.run),*base,'--only','me_chest']))
 jobs.append(('run-ae2-modern-cohort.py',[str(a.run),*base]))
 jobs.append(('capture-ae2-client-subviews.py',[str(a.run),*base]))
rows=[]
for name,args in jobs:
 log=a.run/(name.removesuffix('.py')+'-tail-cohort.log');print('START',name,flush=True)
 with log.open('w',encoding='utf-8') as stream:
  result=subprocess.run([sys.executable,'-X','utf8',str(HERE/name),*args],stdout=stream,stderr=subprocess.STDOUT)
 rows.append({'driver':name,'exit_code':result.returncode,'log':str(log),'scope':'Named native entry/navigation and actual screenshots only; feature checks recorded separately'})
 (a.run/'modern-tail-captures.json').write_text(json.dumps(rows,indent=2));print('END',name,result.returncode,flush=True)
 # Entry failures remain visible for follow-up. A lost world stops the batch.
 state=send(a.run,'wait',ticks=1)['client']
 if state.get('worldLoaded') is False or state.get('screen','').endswith(('DisconnectedScreen','DeathScreen')):
  raise RuntimeError('Disposable actor left its valid world; do not continue accepting screenshots')
