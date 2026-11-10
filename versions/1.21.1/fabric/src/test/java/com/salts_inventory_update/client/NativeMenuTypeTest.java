package com.salts_inventory_update.client;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeMenuTypeTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    private static class TestMenu extends AbstractContainerMenu {
        TestMenu(MenuType<?> type) { super(type, 0); }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    }

    @Test void unregisteredPlayerStyleMenuDoesNotCrashPayloadLookup() {
        var menu = new TestMenu(null);
        assertThrows(UnsupportedOperationException.class, menu::getType);
        assertNull(NativeContainerView.registeredMenuType(menu));
    }

    @Test void registeredContainerKeepsItsExactHandlerKey() {
        var menu = new TestMenu(MenuType.GENERIC_9x3);
        assertSame(MenuType.GENERIC_9x3, NativeContainerView.registeredMenuType(menu));
    }

    @Test void unrelatedMenuFailureIsNotSilentlySwallowed() {
        var menu = new TestMenu(null) {
            @Override public MenuType<?> getType() { throw new IllegalStateException("broken menu"); }
        };
        assertThrows(IllegalStateException.class, () -> NativeContainerView.registeredMenuType(menu));
    }
}
