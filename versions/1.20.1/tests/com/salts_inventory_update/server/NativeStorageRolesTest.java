package com.salts_inventory_update.server;

import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.integration.DesktopRegistrationScope;
import com.salts_inventory_update.internal.desktop.DesktopSortSlots;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeStorageRolesTest {
    @BeforeAll static void bootstrap(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}
    private static final class Menu extends AbstractContainerMenu {
        Menu(){super(MenuType.GENERIC_3x3,1);addSlot(new Slot(new SimpleContainer(1),0,8,18));}
        @Override public ItemStack quickMoveStack(Player p,int i){return ItemStack.EMPTY;}
        @Override public boolean stillValid(Player p){return true;}
    }
    private static final class Rollback extends RuntimeException { }
    @Test void explicitNoStorageSuppressesSortEvenWhenAnAdapterExists() {
        assertThrows(Rollback.class,()->DesktopRegistrationScope.run("test-native-roles",()->{
            DesktopSortSlots.registerStorage(MenuType.GENERIC_3x3,(m,s)->false);
            assertFalse(DesktopSortSlots.isStorage(new Menu()));
            assertFalse(DesktopSortSlots.isSource(new Menu()));
            throw new Rollback();
        }));
    }
    @Test void ghostRuleCellsCannotMakeAnExplicitStorageSortTarget() {
        assertThrows(Rollback.class,()->DesktopRegistrationScope.run("test-native-roles",()->{
            DesktopMenuBindings.register(MenuType.GENERIC_3x3,new DesktopMenuBindings.SlotAccess(){
                @Override public int size(AbstractContainerMenu m){return m.slots.size();}
                @Override public boolean isVirtualSlot(AbstractContainerMenu m,Slot s){return true;}
            },false);
            DesktopSortSlots.registerStorage(MenuType.GENERIC_3x3,(m,s)->true);
            assertFalse(DesktopSortSlots.isStorage(new Menu()));
            throw new Rollback();
        }));
    }
    @Test void realExplicitStorageRemainsSortable() {
        assertThrows(Rollback.class,()->DesktopRegistrationScope.run("test-native-roles",()->{
            DesktopSortSlots.registerStorage(MenuType.GENERIC_3x3,(m,s)->true);
            var m=new Menu();assertTrue(DesktopSortSlots.isStorage(m));assertTrue(DesktopSortSlots.storageSlot(m,m.getSlot(0)));
            throw new Rollback();
        }));
    }
}
