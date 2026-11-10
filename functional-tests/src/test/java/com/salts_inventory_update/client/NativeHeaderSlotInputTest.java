package com.salts_inventory_update.client;
import com.salts_inventory_update.api.client.desktop.*;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class NativeHeaderSlotInputTest {
    @BeforeAll static void bootstrap(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}
    private static DesktopWindowSurface workbench(){return new DesktopWindowSurface(DesktopSurfaceMode.SALT,DesktopInsets.ZERO,false,List.of(
        DesktopWindowRegion.visualAndInteractive(0,0,176,162),
        DesktopWindowRegion.interactive(4,2,72,12).withRole(DesktopHitRole.MOVE),
        DesktopWindowRegion.interactive(151,7,18,18).withRole(DesktopHitRole.SLOT)),DesktopViewportPolicy.PRESERVE);}
    @ParameterizedTest @ValueSource(booleans={true,false}) void nativeWorkbenchCellRemainsClickableWhenUnlocked(boolean physicalHit){
        var role=workbench().hitRole(234,10,176,162,394,26);
        assertEquals(DesktopHitRole.SLOT,role);
        assertFalse(InventoryDesktopScreen.canStartFallbackTitleDrag(role,physicalHit),"Native physical and virtual header cells must not become window drags");
    }
    @Test void movingTheWindowDoesNotChangeHeaderSlotOwnership(){
        for(int[] origin:List.of(new int[]{0,0},new int[]{234,10},new int[]{41,173})){
            var role=workbench().hitRole(origin[0],origin[1],176,162,origin[0]+160,origin[1]+16);
            assertFalse(InventoryDesktopScreen.canStartFallbackTitleDrag(role,true));
        }
    }
    @Test void physicalSlotAlsoPreventsDragWhenNoExplicitSlotRegionWasDeclared(){
        var role=DesktopWindowSurface.SALT.hitRole(100,50,176,162,260,66);
        assertFalse(InventoryDesktopScreen.canStartFallbackTitleDrag(role,true));
    }
    @Test void unoccupiedNativeAndDefaultTitleAreasStillDrag(){
        assertTrue(InventoryDesktopScreen.canStartFallbackTitleDrag(workbench().hitRole(234,10,176,162,244,17),false));
        assertTrue(InventoryDesktopScreen.canStartFallbackTitleDrag(DesktopHitRole.NONE,false));
        assertTrue(InventoryDesktopScreen.canStartFallbackTitleDrag(DesktopHitRole.CONTENT,false));
    }
}
