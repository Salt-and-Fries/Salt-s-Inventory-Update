"""Count actual paired states only against the exact original publisher binary."""
from pathlib import Path
import csv,hashlib,importlib.util,json
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'output/all-target-rollout'
spec=importlib.util.spec_from_file_location('atlas',ROOT/'tools/build-rollout-ui-pdf.py');atlas=importlib.util.module_from_spec(spec);spec.loader.exec_module(atlas)
pairs=atlas.collect();matrix=json.loads((OUT/'availability-and-registration.json').read_text());plan=json.loads((OUT/'ui-evidence-plan.json').read_text())
digests={};inputs={}
def native_inputs(row):
 run=Path(row['run'])
 if run not in inputs:
  staged=row.get('staged_artifacts')
  if not staged:
   file=run/'staged-artifacts.json';staged=json.loads(file.read_text(encoding='utf-8-sig')) if file.exists() else {}
  values=[]
  for artifact in staged.get('artifacts',[]) if isinstance(staged,dict) else staged:
   path=Path(artifact.get('file',artifact.get('path','')))
   if not path.is_file():continue
   if path not in digests:digests[path]=hashlib.sha512(path.read_bytes()).hexdigest()
   values.append(digests[path])
  inputs[run]=set(values)
 return inputs[run]
counts={};pictured={};matched=0
for candidate in plan:
 # A later retake may replace an earlier UI key with a different publisher.
 # Recompute coverage instead of retaining a stale previous match.
 for key in ['captured_states','native_capture','siu_capture']:
  candidate.pop(key,None)
 states=[]
 release=next(r for r in matrix if (r['minecraft'],r['loader'],r['slug'])==(candidate['minecraft'],candidate['loader'],candidate['slug']))
 for (mc,loader,ui),sides in pairs.items():
  if (mc,loader)!=(candidate['minecraft'],candidate['loader']):continue
  if sides['native']['state'].get('screen') not in [candidate['screen'],*candidate.get('native_runtime_aliases',[])]:continue
  if any(release['upstream_sha512'] not in native_inputs(sides[side]) for side in ['native','siu']):continue
  states.append({'ui':ui,'native':sides['native']['screenshot'],'siu':sides['siu']['screenshot']})
 if states:
  candidate.update(captured_states=states,native_capture=states[0]['native'],siu_capture=states[0]['siu'],status='paired real captures; pictured states only, exhaustive feature review pending')
  key=(candidate['minecraft'],candidate['loader'],candidate['slug']);pictured.setdefault(key,set()).update(s['ui'] for s in states);matched+=1
def owners(ui):
 if ui=='visual-workbench':return {'visual-workbench'}
 if ui=='tierify-reforge':return {'tierify'}
 if ui=='tieredz-reforge':return {'tieredz'}
 if ui=='bountiful':return {'bountiful'}
 if ui.startswith('minecells-'):return {'minecells'}
 if ui.startswith('ad-astra-giselle-addon-'):return {'ad-astra-giselle-addon'}
 if ui=='alloyed-forge':return {'create-alloyed'}
 if ui.startswith('appbot-'):return {'applied-botanics'}
 if ui.startswith(('item-megacells-','megacells-')):return {'mega'}
 if ui in ['merequester-requester','own-ae-part-requester-terminal']:return {'merequester'}
 if ui.startswith('nethervinery-'):return {'lets-do-nethervinery'}
 if ui=='shared-ag-bakery-small-pot':return {'lets-do-bakery-farmcharm-compat'}
 if ui=='universal-sawmill':return {'universal-sawmill'}
 if ui.startswith('spawn-') or ui=='bb-spawn-hamster':return {'spawn-mod'}
 if ui=='bf-wolf-isolated':return {'revampedwolf'}
 if ui=='bf-gallery-painting-selector':return {'gallery'}
 if ui=='curios':return {'curios'}
 if ui=='trinkets-equipment':return {'trinkets'}
 if ui.startswith('sophisticated-') and ui.endswith('-settings'):
  consumers=({'sophisticated-backpacks','sophisticated-backpacks-unoffical-fabric-port'} if 'backpack' in ui else {'sophisticated-storage','sophisticated-storage-unofficial-fabric-port'})
  return consumers|{'sophisticated-core','sophisticated-core-unofficial-fabric-port','sophisticated-core-(unofficial-fabric-port)'}
 if ui=='waystones-modifiers':return {'waystones'}
 if ui=='guard-villager':return {'guard-villagers','guard-villagers-fabricquilt'}
 if ui.startswith('niftycarts-'):return {'niftycarts'}
 if ui=='alloy-forge':return {'alloy-forgery'}
 if ui.startswith('universal-grave'):return {'universal-graves'}
 if ui.startswith('ae2-'):return {'ae2'}
 if ui.startswith('aircraft-'):return {'immersive-aircraft'}
 if ui.startswith('bumblezone-'):return {'the-bumblezone','the-bumblezone-fabric'}
 if ui.startswith('wands-'):return {'building-wands'}
 if ui.startswith('travelers-'):return {'travelersbackpack'}
 if ui in ['flower-pouch','petal-pouch','bauble-box','botania-hand-of-ender']:return {'botania'}
 return set()
# A library can supply the screen class (e.g. SGUI's vanilla ContainerScreen),
# so an empty publisher class inventory must not erase a verified native entry.
for (mc,loader,ui),sides in pairs.items():
 for row in matrix:
  if (row['minecraft'],row['loader'])!=(mc,loader) or row['slug'] not in owners(ui):continue
  if any(row['upstream_sha512'] not in native_inputs(sides[side]) for side in ['native','siu']):continue
  pictured.setdefault((mc,loader,row['slug']),set()).add(ui)
counts={key:len(values) for key,values in pictured.items()}
for row in matrix:
 count=counts.get((row['minecraft'],row['loader'],row['slug']),0);row['paired_states_captured']=count
 if row.get('ui_scope','').startswith('appendix:'):
  row['runtime_status']='appendix: outside inventory scope'
 elif row['runtime_status']!='verified-all-in-scope-interfaces':row['runtime_status']=f'partial: {count} pictured states' if count else 'pending focused validation'
(OUT/'ui-evidence-plan.json').write_text(json.dumps(plan,indent=2))
(OUT/'availability-and-registration.json').write_text(json.dumps(matrix,indent=2))
with (OUT/'availability-and-registration.csv').open('w',newline='',encoding='utf-8-sig') as f:
 writer=csv.DictWriter(f,fieldnames=list(dict.fromkeys(k for r in matrix for k in r)));writer.writeheader();writer.writerows(matrix)
print(json.dumps({'pairs':len(pairs),'matched_screen_candidates':matched,'project_targets_with_pictures':len(counts),'scope':'Only exact original input binaries; screenshots do not certify exhaustive functionality'},indent=2))
