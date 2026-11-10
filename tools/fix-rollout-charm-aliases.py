"""Explicit Charm 7 class relocations; do not guess from the generic name Screen."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
for version in ['1.21.1','1.21.11','26.1.2','26.2','26.3']:
 path=ROOT/f'compatibility/versions/{version}/common/src/main/resources/salts_inventory_compat/native-windows/project-everything.json'
 profiles=json.loads(path.read_text())
 for p in profiles:
  if p['id']=='charm-kiln':
   p['screens']=[s for s in p.get('screens',[]) if s.startswith('svenhjol.charm.')]
   p['screens']=sorted(set(p['screens']+['svenhjol.charm.feature.kilns.client.Screen']))
  elif p['id']=='charm-woodcutter':
   p['screens']=sorted(set(p.get('screens',[])+['svenhjol.charm.feature.woodcutters.client.Screen']))
   # Include the complete native recipe-grid top edge (at y=14).
   p['regions'][0]['source'][1]=13
 path.write_text(json.dumps(profiles,indent=2)+'\n')
