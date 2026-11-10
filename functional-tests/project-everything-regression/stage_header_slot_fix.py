"""Stage the tested native-header input-routing correction into every Core jar."""
import json
from stage_runtime_fixes import main,ROOT,OUT,SHA
baseline=OUT/'release-before-header-slot.json'
assert not (OUT/'staged-header-slot-fix.json').exists(),'Already staged; preserve evidence'
if not baseline.exists():
    baseline.write_text(json.dumps({p.relative_to(ROOT).as_posix():SHA(p.read_bytes()) for family in ['core','compatibility'] for p in (ROOT/'dist'/family).glob('*.jar')},indent=2))
changes=[('core',mc,loader,'com/salts_inventory_update/client/InventoryDesktopScreen.class')
    for mc in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3'] for loader in ['Fabric','Forge' if mc=='1.20.1' else 'NeoForge']]
main(changes,baseline.name,'staged-header-slot-fix.json','release-before-header-slot')
