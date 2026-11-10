"""Match Small Ships' Forge/NeoForge namespace alongside its legacy Fabric ID."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
for mc in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']:
 catalog=ROOT/f'compatibility/versions/{mc}/common/src/main/resources/salts_inventory_compat/native-windows/project-everything.json'
 data=json.loads(catalog.read_text());profiles=[p for p in data if 'smallships' in p['mods']];assert len(profiles)==1
 p=profiles[0];p['menus']=list(dict.fromkeys([p['menu'],*p.get('menus',[]),'smallships:ship_container']))
 catalog.write_text(json.dumps(data,indent=2)+'\n')
 path=ROOT/f'compatibility/versions/{mc}/fabric/src/main/java/com/salts_inventory_update/compat/nativewindows/SmallShipsActions.java';text=path.read_text()
 old='var type=NativeMenuSupport.type("minecraft:ship_container");if(type==null)return;'
 new='var nativeType=NativeMenuSupport.type("smallships:ship_container");\n            var type=nativeType!=null?nativeType:NativeMenuSupport.type("minecraft:ship_container");if(type==null)return;'
 assert old in text or new in text,path
 path.write_text(text.replace(old,new));print(mc)
