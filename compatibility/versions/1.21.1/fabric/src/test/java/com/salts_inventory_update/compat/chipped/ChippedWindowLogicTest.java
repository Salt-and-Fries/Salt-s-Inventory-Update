package com.salts_inventory_update.compat.chipped;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class ChippedWindowLogicTest {
    @Test void everyInventoryReferenceMapsToTheNativeSelectionSlot(){
        var slots=new HashSet<Integer>();
        for(int inventory=0;inventory<36;inventory++){
            int slot=ChippedNative.menuSlot(inventory);assertTrue(slots.add(slot));
            assertEquals(inventory,slot<27?slot+9:slot-27);
        }
        assertEquals(27,ChippedNative.menuSlot(0));assertEquals(0,ChippedNative.menuSlot(9));
        assertThrows(IllegalArgumentException.class,()->ChippedNative.menuSlot(-1));
        assertThrows(IllegalArgumentException.class,()->ChippedNative.menuSlot(36));
    }
    @Test void theEntireSixthRowHasTheSameVisibleAndClickableCells(){
        for(int col=0;col<9;col++){
            assertEquals(45+col,ChippedWindow.catalogIndex(col*18+9,92,0));
            assertEquals(45+col,ChippedWindow.catalogIndex(col*18+9,107,0));
        }
        assertEquals(-1,ChippedWindow.catalogIndex(162,90,0));
        assertEquals(-1,ChippedWindow.catalogIndex(10,108,0));
        assertEquals(54,ChippedWindow.catalogIndex(1,100,8));
        assertEquals(0,ChippedWindow.maxScroll(54));assertEquals(18,ChippedWindow.maxScroll(55));
    }
}
