"""Keep the complete native name field in its frame, plus Easy Anvils' guide."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
for version in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']:
    folder=ROOT/'compatibility/versions'/version
    path=folder/'common/src/main/resources/salts_inventory_compat/native-windows/project-everything.json'
    rows=json.loads(path.read_text());profile=next(r for r in rows if r['id']=='minecraft-anvil')
    # These are left/top/right/bottom coordinates. Native EditBox is y24..36;
    # the old bottom35 missed one pixel and caused a second-column relocation.
    profile['regions'][0]['source']=[59,19,168,37]
    path.write_text(json.dumps(rows,indent=2)+'\n')
    path=folder/'fabric/src/main/java/com/salts_inventory_update/compat/nativewindows/ReviewedNativeWindows.java'
    text=path.read_text();anchor='        for (int[] hole:p.clean) b.hide(box(hole));'
    addition='''        // The native guide is render-only, so widget discovery cannot retain it.
        // Its hover coordinates still go through the ordinary native region map.
        if(p.id.equals("minecraft-anvil")&&view.screenType().contains("easyanvils"))
            b.keep("formatting-guide",new DesktopWindowBounds(155,6,15,12),115,0);
'''
    if addition not in text:
        assert anchor in text,version
        text=text.replace(anchor,addition+anchor)
        path.write_text(text)
    print(version,'complete anvil input and native guide')
