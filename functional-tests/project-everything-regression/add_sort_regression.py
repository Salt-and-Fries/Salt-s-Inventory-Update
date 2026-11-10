"""Install the same behavior regression into each mapped-game test source."""
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
TESTS = '''
    @Test void emptyStorageCellsNeedNoPickupPermissionToReceiveSortedItems() throws Throwable {
        var m = menu(4).put(2, Items.DIRT, 10).put(3, Items.IRON_INGOT, 5);
        for (int i = 0; i < 4; i++) {
            m.slots.set(i, new Slot(m.storage, i, 0, 0) {
                @Override public boolean mayPickup(Player player) { return hasItem(); }
            });
        }
        assertTrue(execute(m, m.slots));
        assertTrue(m.slots.get(0).hasItem()); assertTrue(m.slots.get(1).hasItem());
        assertFalse(m.slots.get(2).hasItem()); assertFalse(m.slots.get(3).hasItem());
        assertEquals(15, m.total()); assertEquals(5, m.count(Items.IRON_INGOT));
    }

    @Test void occupiedCellsWithoutPickupPermissionStillRejectSorting() {
        var m = menu(2).put(0, Items.DIRT, 10).put(1, Items.IRON_INGOT, 5);
        m.slots.set(0, new Slot(m.storage, 0, 0, 0) {
            @Override public boolean mayPickup(Player player) { return false; }
        });
        assertThrows(IllegalStateException.class, () -> execute(m, m.slots));
        assertEquals(10, m.slots.get(0).getItem().getCount());
        assertTrue(m.slots.get(0).getItem().is(Items.DIRT));
        assertEquals(15, m.total());
    }

'''
for mc in ['1.20.1', '1.21.1', '1.21.11', '26.1.2', '26.2', '26.3']:
    path = ROOT / f'versions/{mc}/tests/com/salts_inventory_update/server/InventorySortTest.java'
    text = path.read_text()
    if 'emptyStorageCellsNeedNoPickupPermission' not in text:
        at = text.index('    private static void withTags(')
        path.write_text(text[:at] + TESTS + text[at:])

    source = ROOT / f'versions/{mc}/fabric/src/main/java/com/salts_inventory_update/server/DesktopContainerSessions.java'
    text = source.read_text()
    start = text.index('    private static void sortInventory(')
    end = text.index('    private static void verifyInventorySort(', start)
    section = text[start:end]
    section = section.replace('&& slot.mayPickup(player)', '&& (!slot.hasItem() || slot.mayPickup(player))')
    section = section.replace('|| !slot.mayPickup(player) || !next.isEmpty()', '|| slot.hasItem() && !slot.mayPickup(player) || !next.isEmpty()')
    source.write_text(text[:start] + section + text[end:])
