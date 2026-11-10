"""Keep the caller-only writer for a loader-native fallback opening."""
import json
from stage_runtime_fixes import main,ROOT,OUT,SHA
baseline=OUT/'release-before-provider-fallback.json'
report='staged-provider-fallback-fix.json'
assert not (OUT/report).exists() and not baseline.exists(),'Preserve staging evidence'
baseline.write_text(json.dumps({p.relative_to(ROOT).as_posix():SHA(p.read_bytes())
    for family in ['core','compatibility'] for p in (ROOT/'dist'/family).glob('*.jar')},indent=2))
changes=[('core',mc,loader,'com/salts_inventory_update/server/DesktopContainerSessions.class')
    for mc in ['1.21.1','1.21.11','26.1.2','26.2','26.3'] for loader in ['Fabric','NeoForge']]
main(changes,baseline.name,report,'release-before-provider-fallback')
