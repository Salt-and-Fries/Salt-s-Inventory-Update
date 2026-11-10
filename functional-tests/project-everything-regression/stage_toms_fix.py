"""Stage only the verified vertical-sidebar correction into compatibility jars."""
import json
from stage_runtime_fixes import main, ROOT, OUT, SHA
baseline=OUT/'release-before-toms-sidebar.json'
assert not (OUT/'staged-toms-sidebar-fix.json').exists(),'Already staged; retain original evidence'
if not baseline.exists():
    baseline.write_text(json.dumps({p.relative_to(ROOT).as_posix():SHA(p.read_bytes())
        for family in ['core','compatibility'] for p in (ROOT/'dist'/family).glob('*.jar')},indent=2))
changes=[('compatibility',mc,loader,'com/salts_inventory_update/compat/toms_storage/client/TomsStorageDesktopWindows.class')
    for mc in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']
    for loader in ['Fabric','Forge' if mc=='1.20.1' else 'NeoForge']]
main(changes,baseline.name,'staged-toms-sidebar-fix.json','release-before-toms-sidebar')
