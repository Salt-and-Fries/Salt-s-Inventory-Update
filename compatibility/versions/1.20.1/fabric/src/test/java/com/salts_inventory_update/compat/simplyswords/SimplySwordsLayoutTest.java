package com.salts_inventory_update.compat.simplyswords;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SimplySwordsLayoutTest {
    @Test void everyCompleteCellFitsTheVisibleBodyAndThePreviewCannotBecomeAnOutput() {
        for(int i=0;i<11;i++){
            int x=SimplySwordsLayout.slotX(i),y=SimplySwordsLayout.slotY(i);
            assertTrue(x-1>=0);assertTrue(x+17<=200);
            assertTrue(y-1>=16);assertTrue(y+17<=74);
        }
        assertEquals(92,SimplySwordsLayout.slotX(0));assertEquals(22,SimplySwordsLayout.slotY(0));
        assertEquals(155,SimplySwordsLayout.slotX(10));assertEquals(56,SimplySwordsLayout.slotY(10));
        assertThrows(IllegalArgumentException.class,()->SimplySwordsLayout.slotX(47));
        assertThrows(IllegalArgumentException.class,()->SimplySwordsLayout.slotY(-1));
    }
    @Test void cleanupRetainsGoldOnlyInTheActualTabletArtwork() {
        int gold=0xFF22BBDD;
        assertTrue(SimplySwordsLayout.keepTabletPixel(30,gold));
        assertTrue(SimplySwordsLayout.keepTabletPixel(170,gold));
        assertFalse(SimplySwordsLayout.keepTabletPixel(28,gold));
        assertFalse(SimplySwordsLayout.keepTabletPixel(45,gold));
        assertFalse(SimplySwordsLayout.keepTabletPixel(199,gold));
        assertFalse(SimplySwordsLayout.keepTabletPixel(30,0xFF183568));
        assertFalse(SimplySwordsLayout.keepTabletPixel(30,0));
    }
}
