"""Explicit native management screen contracts require real menu evidence."""
import hashlib
from pathlib import Path
SOURCE_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()

def dockyard_management(screen,menu):
    return (screen=='com.talhanation.smallships.client.gui.screens.inventory.DockyardScreen'
            and menu.get('class')=='com.talhanation.smallships.world.inventory.DockyardMenu'
            and menu.get('menuType')=='smallships:dockyard'
            and menu.get('slotCount')==0)
