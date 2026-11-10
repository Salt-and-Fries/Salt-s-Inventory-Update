package com.salts_inventory_update.compat.aether;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.salts_inventory_update.api.client.desktop.DesktopWindowBounds;
import static org.junit.jupiter.api.Assertions.*;
class AetherCharacterLayoutTest {
    @Test void playerSlotsMatchNativeAetherAndStayInsideExistingCharacter(){
        var expected=Map.of(5,new int[]{59,8},6,new int[]{59,26},7,new int[]{59,44},8,new int[]{59,62},
            1,new int[]{116,18},2,new int[]{134,18},3,new int[]{116,36},4,new int[]{134,36},0,new int[]{154,28},45,new int[]{116,62});
        for(var e:expected.entrySet()){var r=AetherCharacterLayout.playerSlot(e.getKey());assertEquals(e.getValue()[0],r.x());assertEquals(e.getValue()[1],r.y());assertTrue(r.x()-1>=0&&r.x()+17<=176&&r.y()-1>=0&&r.y()+17<=82);}
        assertNull(AetherCharacterLayout.playerSlot(9));
    }
    @Test void allEightEquipmentFramesFitWithoutOverlappingArmorOrCrafting(){
        var occupied=new ArrayList<DesktopWindowBounds>();
        for(int id:new int[]{0,1,2,3,4,5,6,7,8,45})occupied.add(AetherCharacterLayout.playerSlot(id));
        for(int[] cell:new int[][]{{0,0},{0,18},{0,36},{18,0},{18,18},{18,36},{0,54},{18,54}}){
            var r=AetherCharacterLayout.equipment(cell[0],cell[1]);assertTrue(r.x()+17<=176&&r.y()+17<=82);
            for(var old:occupied)assertFalse(r.x()<old.x()+16&&r.x()+16>old.x()&&r.y()<old.y()+16&&r.y()+16>old.y());occupied.add(r);
        }
    }
}
