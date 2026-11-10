"""Keep VillagersPlus' four machine slots and complete native furnace graphics."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
for version in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']:
 path=ROOT/f'compatibility/versions/{version}/common/src/main/resources/salts_inventory_compat/native-windows/project-everything.json'
 profiles=json.loads(path.read_text())
 if not any(p['id']=='villagersplus-ore-grinder' for p in profiles):
  profiles.append({'id':'villagersplus-ore-grinder','mods':['villagersplus'],
   'screen':'com.lion.villagersplus.client.screen.OreGrinderScreen','menu':'villagersplus:ore_grinder_screen_handler',
   'regions':[{'source':[20,15,138,71],'at':[0,0],'cutouts':[]}],
   'canvas':[0,0],'title':'Ore Grinder','color':[198,198,198],'darkFrame':False,
   'clean':[],'recipeBook':False,'machineUtility':False,'menus':['villagersplus:ore_grinder_screen_handler']})
 path.write_text(json.dumps(profiles,indent=2)+'\n')
