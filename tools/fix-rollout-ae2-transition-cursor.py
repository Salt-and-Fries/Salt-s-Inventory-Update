"""Do not use an expired payload context after AE2 replaces its native menu."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
paths=[ROOT/'compatibility/ae2/versions/1.20.1/common/src/main/java/com/salts_inventory_update/compat/ae2/server/AE2ServerIntegration.java']
paths += [ROOT/f'compatibility/ae2/versions/{mc}/neoforge/src/main/java/com/salts_inventory_update/compat/ae2/server/AE2ServerIntegration.java' for mc in ['1.21.1','26.1.2','26.2','26.3']]
needle='request.setCarriedStack(menu.getCarried());'
for path in paths:
 text=path.read_text()
 if 'A replaced native menu no longer owns this payload context.' in text:continue
 assert text.count(needle)==1,path
 line=next(line for line in text.splitlines() if needle in line);indent=line[:len(line)-len(line.lstrip())]
 replacement='\n'.join(indent+line for line in [
  'if (DesktopSessions.hasOpenSessionMatching(player, candidate -> candidate == menu)) {',
  '    request.setCarriedStack(menu.getCarried());',
  '} else {',
  '    // A replaced native menu no longer owns this payload context.',
  '    // Keep the canonical cursor through Core\'s enclosing callback;',
  '    // native removed() has already cleared the old menu mirror.',
  '    menu.setCarried(player.inventoryMenu.getCarried().copy());',
  '}'])
 text=text.replace(line,replacement);path.write_text(text);print(path.relative_to(ROOT))
