"""Keep an unsupported Curios ABI out of the Aether launcher path."""
import json
from stage_runtime_fixes import main,ROOT,OUT,SHA
baseline=OUT/'release-before-aether-launcher.json'
assert not baseline.exists() and not (OUT/'staged-aether-launcher-fix.json').exists(),'Preserve original staging evidence'
baseline.write_text(json.dumps({p.relative_to(ROOT).as_posix():SHA(p.read_bytes())
    for family in ['core','compatibility'] for p in (ROOT/'dist'/family).glob('*.jar')},indent=2))
changes=[('compatibility','1.20.1',loader,'com/salts_inventory_update/compat/aether/AetherEquipmentClient.class') for loader in ['Fabric','Forge']]
changes.append(('compatibility','1.20.1','Forge','com/salts_inventory_update/compat/curios/CuriosCompatBootstrap.class'))
main(changes,baseline.name,'staged-aether-launcher-fix.json','release-before-aether-launcher')
