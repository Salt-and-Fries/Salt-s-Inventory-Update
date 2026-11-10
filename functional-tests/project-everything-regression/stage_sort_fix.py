"""Stage the verified empty-slot sorting correction into all twelve Core jars."""
import json
from stage_runtime_fixes import main, ROOT, OUT, SHA
baseline = OUT / 'release-before-sort.json'
assert not (OUT / 'staged-sort-fix.json').exists(), 'Already staged; preserve the original evidence'
if not baseline.exists():
    baseline.write_text(json.dumps({p.relative_to(ROOT).as_posix(): SHA(p.read_bytes())
        for family in ['core', 'compatibility'] for p in (ROOT / 'dist' / family).glob('*.jar')}, indent=2))
changes = [('core', mc, loader, 'com/salts_inventory_update/server/DesktopContainerSessions.class')
           for mc in ['1.20.1', '1.21.1', '1.21.11', '26.1.2', '26.2', '26.3']
           for loader in ['Fabric', 'Forge' if mc == '1.20.1' else 'NeoForge']]
main(changes, baseline.name, 'staged-sort-fix.json', 'release-before-sort')
