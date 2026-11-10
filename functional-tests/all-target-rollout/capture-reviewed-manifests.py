"""Re-use reviewed ordinary entry workflows against exact rollout publishers.

Missing content and non-container screens remain explicit outcomes. This does
not force providers and does not run the complete gameplay/functional suite.
"""
from pathlib import Path
import argparse,hashlib,json,re,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import capture,send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True)
p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True)
p.add_argument('--manifest',type=Path,action='append',required=True);p.add_argument('--only-case',action='append')
p.add_argument('--record',default='reviewed-manifest-captures');a=p.parse_args()
assert re.fullmatch('[a-z0-9-]+',a.record)
send(a.run,'catalog',ticks=1);catalog=json.loads((a.run/'catalog.json').read_text())
blocks=set(catalog['blocks']);items=set(catalog['items']);namespaces={x.split(':')[0] for x in blocks|items}
cases={}
for path in a.manifest:
 data=json.loads(path.read_text())
 if data.get('loader')!=a.loader:continue
 for case in data['cases']:
  cases[case['id']]={**case,'source_manifest':str(path)}
rows=[];actions_key='core_actions' if a.side=='siu' else 'native_actions'
# These mods extend a Minecraft item/entity and add no namespaced registry
# reference to the opening action. Require their exact installed publisher JAR.
vanilla_entry_owners={'RevampedWolf':'revampedwolf','Gallery':'gallery','AdditionZ':'additionz'}
installed_publisher_hashes=None
def vanilla_owner_present(family):
 global installed_publisher_hashes
 slug=vanilla_entry_owners.get(family)
 if slug is None:return False
 if installed_publisher_hashes is None:
  staged=json.loads((a.run/'staged-artifacts.json').read_text(encoding='utf-8-sig'))
  installed_publisher_hashes={hashlib.sha512(Path(x['file']).read_bytes()).hexdigest() for x in staged['artifacts']}
  lock=json.loads((Path(__file__).resolve().parents[2]/'build/all-target-rollout/availability.json').read_text())
  vanilla_owner_present.releases=lock['projects']
 release=vanilla_owner_present.releases.get(slug,{}).get('targets',{}).get(a.minecraft+'/'+a.loader,{})
 return release.get('sha512') in installed_publisher_hashes
for case in cases.values():
 if a.only_case and case['id'] not in a.only_case:continue
 actions=case.get(actions_key,[])
 required=[(k,action[k]) for action in actions for k in ['block','item'] if k in action]
 if any(value not in (blocks if kind=='block' else items) for kind,value in required):continue
 # Item data is not a registry reference: e.g. amount:250000L is valid
 # native SNBT, not a dependency namespace named "amount".
 reference_actions=[{k:v for k,v in action.items() if k not in ['snbt','nbt']} for action in actions]
 refs=({value.split(':',1)[0] for _,value in required} |
       set(re.findall(r'\b([a-z][a-z0-9_]*):[a-z_][a-z0-9_/]*',json.dumps(reference_actions))))-{'minecraft'}
 # An inherited vanilla entry needs a separate explicit owner check. Do not
 # treat a plain vanilla workstation as proof for an absent optional mod.
 if not refs and not vanilla_owner_present(case.get('family')):continue
 if not refs.issubset(namespaces):continue
 row={'ui':case['id'],'family':case.get('family'),'manifest':case['source_manifest'],'side':a.side};rows.append(row)
 try:
  # Remove prior-case combat exposure before clearing armor or closing a mob
  # inventory. Each reviewed action list still chooses its required game mode.
  send(a.run,'command',command='gamemode creative @a',ticks=3)
  send(a.run,'close',ticks=3)
  send(a.run,'command',command='kill @e[type=!minecraft:player]',ticks=3)
  send(a.run,'command',command='clear @a',ticks=3)
  send(a.run,'command',command='fill 5 65 5 12 69 12 minecraft:air',ticks=3)
  for action in actions:
   args=dict(action);kind=args.pop('type')
   if kind in ['provider','native-template-probe','profile','reload-profiles']:raise ValueError('Forced menu/provider is not comparison evidence')
   kind={'client-use-block-ray':'open-ray','client-use-any-block-ray':'use-block-ray','client-use-entity-ray':'use-entity-ray'}.get(kind,kind)
   if a.side=='siu' and kind=='open' and a.loader=='neoforge' and not args.get('realUse'):
    kind='client-use-block'
   if kind=='fixture' and args.get('block')=='minecraft:air' and a.run.parent.parent.name=='all-target-rollout':
    # The older excluded QA observer rejects AIR as its prototype block.
    # Build its ordinary platform first, then clear that prototype with the
    # normal world command before the publisher's part-placement workflow.
    send(a.run,'fixture',**{**args,'block':'minecraft:stone'})
    x,y,z=map(int,catalog.get('origin','8,65,8').split(','))
    result=send(a.run,'command',command=f'setblock {x} {y} {z} minecraft:air',ticks=3)
   else:result=send(a.run,kind,**args)
   if result.get('client',{}).get('worldLoaded') is False:raise RuntimeError('Client left the disposable world')
  state=send(a.run,'wait',ticks=3)['client']
  if state.get('screen','').endswith('.DeathScreen'):raise RuntimeError('Disposable fixture player died; restore its actor before accepting UI evidence')
  if state.get('screen') in ['none',None]:row.update(status='no-screen-opened; investigate scope/entry')
  elif a.side=='native' and not state.get('menuClass'):
   row.update(status='non-container screen; appendix review required',screen=state['screen'])
  elif a.side=='siu' and not state['screen'].endswith('InventoryDesktopScreen'):
   row.update(status='native full-screen retained; appendix review required',screen=state['screen'])
  else:
   send(a.run,'move',x=5,y=5,ticks=3)
   capture(a.run,a.side,a.minecraft,a.loader,case['id'],
           case.get('family','Reviewed mod')+': original reviewed entry workflow, re-run against this exact installed publisher release. Pictured layout only; feature checks are separate.')
   row.update(status='ordinary-entry-and-picture-pass',screen=state['screen'])
 except Exception as error:
  row.update(status='pending',reason=str(error)[:350]);print('PENDING',case['id'],str(error)[:100],flush=True)
  (a.run/(a.record+'.json')).write_text(json.dumps(rows,indent=2))
  if isinstance(error,TimeoutError) or 'left the disposable world' in str(error) or 'fixture player died' in str(error):raise
 (a.run/(a.record+'.json')).write_text(json.dumps(rows,indent=2))
 print(case['id'],row['status'],flush=True)
