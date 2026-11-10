"""Use remapped native field accessors instead of removed NeoForge GUI methods."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
template=(ROOT/'tools/rollout-templates/NativeScreenGeometryAccess.java').read_text()
for version in ['1.21.1','1.21.11','26.1.2','26.2','26.3']:
    base=ROOT/'compatibility/versions'/version
    src=base/'fabric/src/main/java/com/salts_inventory_update/compat'
    (src/'nativewindows/mixin/NativeScreenGeometryAccess.java').write_text(template)
    path=src/'travelersbackpack/TravelersWindows.java'
    path.write_text(path.read_text().replace('number(s,"getGuiLeft")','left(s)').replace('number(s,"getGuiTop")','top(s)'))
    path=src/'travelersbackpack/TravelersAccess.java';text=path.read_text()
    if 'static int left(Object screen)' not in text:
        text=text.replace('    static int number(Object o,String method)','''    static int left(Object screen){return ((com.salts_inventory_update.compat.nativewindows.mixin.NativeScreenGeometryAccess)screen).salts_inventory_compat$left();}
    static int top(Object screen){return ((com.salts_inventory_update.compat.nativewindows.mixin.NativeScreenGeometryAccess)screen).salts_inventory_compat$top();}
    static int number(Object o,String method)''')
    path.write_text(text)
    path=base/'common/src/main/resources/salts_inventory_compat.reviewed-native.mixins.json'
    data=json.loads(path.read_text());data['client']=list(dict.fromkeys(data['client']+['NativeScreenGeometryAccess']))
    path.write_text(json.dumps(data,indent=2)+'\n')
print('Replaced loader-specific Traveler screen-origin calls on five branches')
