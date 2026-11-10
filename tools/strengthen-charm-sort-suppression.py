"""Block Charm's redundant sorting controls before any native screen adds them."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'build/redundant-sort-suppression'
WORK.mkdir(exist_ok=True)
changes=[]
for version in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']:
    path=ROOT/f'versions/{version}/fabric/src/main/java/com/salts_inventory_update/mixin/client/compat/CharmInventoryTidyingMixin.java'
    text=path.read_text()
    backup=WORK/'before'/path.relative_to(ROOT)
    if not backup.exists():
        backup.parent.mkdir(parents=True,exist_ok=True)
        backup.write_bytes(path.read_bytes())
    text=text.replace('''/** SIU already provides sorting for player and container inventories.
 * Suppress Charm's extra buttons at their common creation point, including
 * menus that keep their original screen. No Charm config is rewritten. */''','''/** SIU supplies sorting for both player and container inventories.
 * Suppress only Charm's tidying feature, for every native screen rather than
 * a per-menu whitelist. Covers original full-screen menus, extracted native
 * windows and rebuilt/resized screens, without changing Charm configuration. */''')
    text=text.replace('''    @Inject(method="addSortingButton",at=@At("HEAD"),cancellable=true,require=0)''','''    // Legacy Charm 6 and modern Charm 7 name the same feature entry point
    // differently. Cancel setup before it can scan slots or attach widgets;
    // retain the factory guard for other callers of addSortingButton.
    @Inject(method={"handleScreenSetup","screenSetup","addSortingButton"},
        at=@At("HEAD"),cancellable=true,require=0)''')
    path.write_text(text)
    changes.append(str(path.relative_to(ROOT)))
(WORK/'changed-files.json').write_text(json.dumps(changes,indent=2))
print('Updated six shared Core branches; applies to all twelve loader/version JARs')
