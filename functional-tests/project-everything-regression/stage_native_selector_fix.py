"""Stage the explicit native selector veto once, keeping public API byte-identical."""
import json
from stage_runtime_fixes import main,ROOT,OUT,SHA
baseline=OUT/'release-before-native-selector.json'
report='staged-native-selector-fix.json'
assert not baseline.exists() and not (OUT/report).exists(),'Preserve prior staging evidence'
baseline.write_text(json.dumps({p.relative_to(ROOT).as_posix():SHA(p.read_bytes())
    for family in ['core','compatibility'] for p in (ROOT/'dist'/family).glob('*.jar')},indent=2))
changes=[]
for mc in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']:
    for loader in ['Fabric','Forge' if mc=='1.20.1' else 'NeoForge']:
        changes.append(('core',mc,loader,'com/salts_inventory_update/server/DesktopContainerSessions.class'))
        for cls in ['ReviewedNativeWindows.class','ReviewedNativeWindows$1.class']:
            changes.append(('compatibility',mc,loader,'com/salts_inventory_update/compat/nativewindows/'+cls))
main(changes,baseline.name,report,'release-before-native-selector')
