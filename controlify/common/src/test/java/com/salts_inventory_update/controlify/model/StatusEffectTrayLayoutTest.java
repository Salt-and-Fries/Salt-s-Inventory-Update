package com.salts_inventory_update.controlify.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StatusEffectTrayLayoutTest {
    @Test void ordinaryTrayFitsUnderCharacter() {
        var tray=StatusEffectTrayLayout.place(640,420,8,96,306,230,6,30);
        assertNotNull(tray);assertEquals(6,tray.capacity());
        assertTrue(tray.y()>=96+230);assertTrue(tray.y()+tray.height()<=414);
    }
    @Test void screenEdgesAndLongTimersKeepEveryCellWithinTheFrame() {
        for (int width:new int[]{160,320,480,640}) for (int height:new int[]{100,240,420})
            for (int count:new int[]{1,6,30,100}) {
                var tray=StatusEffectTrayLayout.place(width,height,width-35,height-65,140,60,count,52);
                assertTrue(tray.x()>=0 && tray.y()>=0);
                assertTrue(tray.x()+tray.width()<=width && tray.y()+tray.height()<=height);
                for (int i=0;i<tray.capacity();i++) {
                    assertTrue(tray.cellX(i)>=tray.x());assertTrue(tray.cellX(i)+tray.cellWidth()<=tray.x()+tray.width());
                    assertTrue(tray.cellY(i)>=tray.y());assertTrue(tray.cellY(i)+StatusEffectTrayLayout.ROW_HEIGHT<=tray.y()+tray.height());
                }
            }
    }
    @Test void spaceAboveIsUsedWhenBelowCannotFitARow() {
        var tray=StatusEffectTrayLayout.place(640,420,320,300,306,110,6,30);
        assertNotNull(tray);assertTrue(tray.y()+tray.height()<300);
    }
    @Test void CrowdedTraysReserveBoundedCapacityAndEmptyEffectsNeedNoTray() {
        var tray=StatusEffectTrayLayout.place(160,100,6,10,140,75,100,30);
        assertTrue(tray.capacity()>0 && tray.capacity()<100);
        assertNull(StatusEffectTrayLayout.place(640,420,8,96,306,230,0,30));
    }
}
