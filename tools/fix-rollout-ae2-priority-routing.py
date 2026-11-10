"""Keep reusable priority tabs on their owning authenticated desktop menu."""
from pathlib import Path
import copy,importlib,json,sys
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))
g=importlib.import_module('generate-ae2-native-deltas')
SCOPE='com/salts_inventory_update/compat/ae2/nativeview/NativeMenuPacketScope'
def calls(patch):
 for method in patch['changes']:
  if method['name']=='openPriorityGui':
   for edit in method['edits']:
    yield from (op for op in edit.get('insert',[]) if op.get('type')==5)
rows=[]
for path in (ROOT/'compatibility/ae2/versions/1.20.1').glob('*/src/main/resources/ae2-native-patches*/appeng/client/gui/WidgetContainer.json'):
 patch=json.loads(path.read_text());changed=False
 for op in calls(patch):
  if op.get('owner','').endswith('/DesktopNativeTransport') and op.get('name')=='sendDefault':
   op.update(owner=SCOPE,name='send');changed=True
 assert any(op.get('owner')==SCOPE and op.get('name')=='send' for op in calls(patch)),path
 if changed:path.write_text(g.canonical(patch))
 rows.append({'file':str(path.relative_to(ROOT)),'route':'owning menu scope, ordinary native fallback outside SIU'})
path=ROOT/'compatibility/ae2/versions/1.21.1/neoforge/src/main/resources/ae2-native-patches/appeng/client/gui/WidgetContainer.json'
patch=json.loads(path.read_text())
if not any(method['name']=='openPriorityGui' for method in patch['changes']):
 original=json.loads((ROOT/'build/ae2-port/patch-tools/1.21.1-neoforge/appeng/client/gui/WidgetContainer.json').read_text())
 merged=copy.deepcopy(original);method=next(m for m in merged['methods'] if m['name']=='openPriorityGui');ops=method['instructions']
 index=next(i for i,op in enumerate(ops) if op.get('owner')=='net/neoforged/neoforge/network/PacketDistributor' and op.get('name')=='sendToServer')
 assert ops[index-2]['op']==3 and ops[index-1]['op']==189
 ops[index].update(owner=SCOPE,name='send',desc='(Lappeng/core/network/ServerboundPacket;)V')
 del ops[index-2:index]
 delta=g.generate(original,merged);assert len(delta['changes'])==1
 patch['changes'].append(delta['changes'][0]);path.write_text(g.canonical(patch))
manifest=path.parents[3]/'manifest.json'
if manifest.exists():
 data=json.loads(manifest.read_text())
 next(c for c in data['classes'] if c['class']==patch['class'])['changes']=len(patch['changes'])
 manifest.write_text(json.dumps(data,indent=2)+'\n')
assert any(op.get('owner')==SCOPE and op.get('name')=='send' for op in calls(patch))
rows.append({'file':str(path.relative_to(ROOT)),'route':'owning menu scope, ordinary native fallback outside SIU'})
for version in ['26.1.2','26.2','26.3']:
 path=ROOT/f'compatibility/ae2/versions/{version}/neoforge/src/main/resources/ae2-native-patches/appeng/client/gui/WidgetContainer.json';patch=json.loads(path.read_text())
 assert any(op.get('owner')==SCOPE and op.get('name')=='send' for op in calls(patch)),path
 rows.append({'file':str(path.relative_to(ROOT)),'route':'existing owning menu scope retained'})
(ROOT/'build/all-target-rollout/ae2-priority-routes.json').write_text(json.dumps(rows,indent=2))
print('PASS scoped priority routing in',len(rows),'version/loader/resource variants')
