package com.salts_inventory_update.internal.desktop;

import static org.junit.jupiter.api.Assertions.*;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class VirtualRecipeSourceContractTest {
    @BeforeAll static void bootstrap(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}
    private static final class Menu extends AbstractContainerMenu {
        private Menu(MenuType<?> type){super(type,1);addSlot(new Slot(new SimpleContainer(1),0,0,0));}
        @Override public boolean stillValid(Player player){return true;}
        @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
    }
    @Test void phantomAuthorityIsDeclaredByItsOwnerWithoutChangingNativeSlotClickability(){
        MenuType<Menu> type=new MenuType<>((id,inventory)->null,FeatureFlags.DEFAULT_FLAGS);
        Menu menu=new Menu(type);Slot phantom=menu.getSlot(0);
        DesktopMenuBindings.register(type,new DesktopMenuBindings.SlotAccess(){
            @Override public int size(AbstractContainerMenu candidate){return candidate.slots.size();}
            @Override public boolean isVirtualSlot(AbstractContainerMenu candidate,Slot slot){return slot==phantom;}
        },false);
        assertTrue(DesktopMenuBindings.isVirtualSlot(menu,phantom));
        assertSame(phantom,DesktopMenuBindings.slot(menu,0),"The native owner still receives its configuration clicks");
    }
    @Test void ordinaryUnregisteredSlotsRemainReal(){
        MenuType<Menu> type=new MenuType<>((id,inventory)->null,FeatureFlags.DEFAULT_FLAGS);
        Menu menu=new Menu(type);
        assertFalse(DesktopMenuBindings.isVirtualSlot(menu,menu.getSlot(0)));
    }
}
