package com.salts_inventory_update.server;

import com.salts_inventory_update.internal.desktop.AutomaticContainerSupport;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeConfigurationEligibilityTest {
    public static final class ErasedType extends MenuType<AbstractContainerMenu> {
        int openingByte;
        ErasedType(){super((id,inventory)->{throw new AssertionError("Lost extended opening data");},net.minecraft.world.flag.FeatureFlags.VANILLA_SET);}
        public Object create(int id,net.minecraft.world.entity.player.Inventory inventory,net.minecraft.network.FriendlyByteBuf data){
            openingByte=data.readUnsignedByte();
            return new AbstractContainerMenu(this,id){
                @Override public boolean stillValid(Player player){return true;}
                @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
            };
        }
    }
    @BeforeAll static void bootstrap(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}
    @Test void aSlotlessInventoryToolRequiresExplicitNativeTemplateCapability() {
        var menu=new AbstractContainerMenu(MenuType.GENERIC_9x3,1) {
            @Override public boolean stillValid(Player player){return true;}
            @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
        };
        assertFalse(AutomaticContainerSupport.candidate(null,menu,false));
        assertTrue(AutomaticContainerSupport.candidate(null,menu,true));
    }
    @Test void extendedFactoryWithAnErasedReturnTypeReceivesItsNativeOpeningBytes(){
        var type=new ErasedType();
        var menu=AutomaticContainerSupport.create(type,17,null,new byte[]{42});
        assertEquals(17,menu.containerId);assertEquals(42,type.openingByte);
    }
}
