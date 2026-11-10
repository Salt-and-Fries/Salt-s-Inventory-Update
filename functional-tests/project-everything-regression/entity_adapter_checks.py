"""Validate disjoint aircraft panes against their actual native source slots."""
from pathlib import Path
import hashlib
SOURCE_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
TYPE='salts_inventory_compat:aircraft_inventory'
CLASS='com.salts_inventory_update.compat.aircraft.AircraftDesktopMenu'
NATIVE='immersive_aircraft.screen.VehicleScreenHandler'

def aircraft_source_preserved(menus,expected,count):
    panes=[m for m in menus if m.get('menuType')==TYPE and m.get('type')==CLASS]
    if expected!=NATIVE or not panes or count<1:return False
    sources=[m.get('nativeAdapter',{}) for m in panes]
    if any(s.get('originalMenuClass')!=expected for s in sources):return False
    identities={s.get('vehicle') for s in sources}
    if len(identities)!=1 or None in identities:return False
    indices=[i for s in sources for i in s.get('nativeSlotIndices',[])]
    return sorted(indices)==list(range(count)) and all(
        len(s.get('nativeSlotIndices',[]))==sum(not v['player'] for v in m['slots'])
        and s.get('originalNonplayerSlots')==count for m,s in zip(panes,sources))
