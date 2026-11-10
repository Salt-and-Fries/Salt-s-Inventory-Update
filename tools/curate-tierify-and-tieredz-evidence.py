"""Keep the two distinct publishers sharing tiered:reforge as separate UI pairs."""
from pathlib import Path
import hashlib,json,sys
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'functional-tests/all-target-rollout'))
from evidence import mutate_ledger
restored=[]
for side,suffix in [('native','f3909e43'),('siu','9853971e')]:
 run=ROOT/f'build/remaining-ui-audit/gameplay/1.20.1-fabric-{side}-rollout-production-remaining-tail-01'
 history=json.loads((run/'observations.json').read_text());row=next(r for r in history if r.get('command',{}).get('capture','').endswith(suffix))
 path=run/'screenshots'/(row['command']['capture']+'.png')
 state=row['client'];state['siu_loaded']=side=='siu'
 if side=='native':assert state['screen']=='draylar.tiered.reforge.ReforgeScreen'
 else:assert state['windows'][0]['nativeScreen']=='draylar.tiered.reforge.ReforgeScreen'
 restored.append({'minecraft':'1.20.1','loader':'fabric','ui':'tieredz-reforge','side':side,'screenshot':str(path),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'state':state,'run':str(run),'staged_artifacts':json.loads((run/'staged-artifacts.json').read_text()),'note':'Original TieredZ tab entry, retained from its original observation history. Tierify is a separate publisher/fixture despite sharing the tiered menu ID.'})
def update(rows):
 for r in rows:
  if r['minecraft']=='1.20.1' and r['loader']=='fabric' and r['ui']=='tieredz-reforge' and 'tierify-final' in r['run']:r['ui']='tierify-reforge'
 rows=[r for r in rows if (r['minecraft'],r['loader'],r['ui'])!=('1.20.1','fabric','tieredz-reforge')]
 return rows+restored
mutate_ledger(update)
print('Restored two original TieredZ observations; retained Tierify as its own exact-publisher pair.')
