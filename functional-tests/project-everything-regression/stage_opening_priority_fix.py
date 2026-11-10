"""Preserve registered menu wire formats while retaining native provider bytes."""
import json
from stage_runtime_fixes import main,ROOT,OUT,SHA

baseline=OUT/'release-before-opening-priority.json'
report='staged-opening-priority-fix.json'
assert not baseline.exists() and not (OUT/report).exists(),'Preserve prior staging evidence'
baseline.write_text(json.dumps({p.relative_to(ROOT).as_posix():SHA(p.read_bytes())
    for family in ['core','compatibility'] for p in (ROOT/'dist'/family).glob('*.jar')},indent=2))
changes=[('core',mc,loader,entry)
    for mc in ['1.21.1','1.21.11','26.1.2','26.2','26.3'] for loader in ['Fabric','NeoForge']
    for entry in ['com/salts_inventory_update/internal/desktop/AutomaticContainerSupport.class',
                  'com/salts_inventory_update/server/DesktopContainerSessions.class']]
main(changes,baseline.name,report,'release-before-opening-priority')
