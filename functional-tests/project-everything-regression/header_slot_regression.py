"""Install the native header-slot routing regression on every Core target."""
from pathlib import Path
import argparse
ROOT=Path(__file__).resolve().parents[2]
TEST='''package com.salts_inventory_update.client;
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
'''
def main():
    p=argparse.ArgumentParser();p.add_argument('--fix',action='store_true');a=p.parse_args()
    for mc in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']:
        path=ROOT/f'versions/{mc}/fabric/src/main/java/com/salts_inventory_update/client/InventoryDesktopScreen.java';text=path.read_text()
        if 'static boolean canStartFallbackTitleDrag(' not in text:
            at=text.index('    private boolean beginSurfaceInteraction(')
            helper='    static boolean canStartFallbackTitleDrag(DesktopHitRole role, boolean slotHit) {\n        return true;\n    }\n\n'
            text=text[:at]+helper+text[at:]
            old='&& window.isTopBar(event.x(), event.y()) && !creativeTitleWidgetClick'
            new='&& window.isTopBar(event.x(), event.y()) && canStartFallbackTitleDrag(window.apiSurface.hitRole(window.x, window.y, window.width, window.height, event.x(), event.y()), window.slotAt(this, event.x(), event.y()) != null) && !creativeTitleWidgetClick'
            assert text.count(old)==1,mc;text=text.replace(old,new)
        if a.fix:text=text.replace('static boolean canStartFallbackTitleDrag(DesktopHitRole role, boolean slotHit) {\n        return true;','static boolean canStartFallbackTitleDrag(DesktopHitRole role, boolean slotHit) {\n        return role != DesktopHitRole.SLOT && !slotHit;')
        path.write_text(text)
    target=ROOT/'functional-tests/src/test/java/com/salts_inventory_update/client/NativeHeaderSlotInputTest.java';target.write_text(TEST)

if __name__=='__main__':main()
