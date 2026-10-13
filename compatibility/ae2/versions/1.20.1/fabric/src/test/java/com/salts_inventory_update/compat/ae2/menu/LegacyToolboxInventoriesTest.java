package com.salts_inventory_update.compat.ae2.menu;

import appeng.util.inv.AppEngInternalInventory;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegacyToolboxInventoriesTest {
    @BeforeAll static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
    }
    @Test void twoWindowsShareMutableStorageOnlyForTheExactSameToolStack() {
        var source=new ItemStack(Items.STICK);
        var first=new AppEngInternalInventory(null,9);
        var second=new AppEngInternalInventory(null,9);
        assertSame(first,LegacyToolboxInventories.share(source,first));
        var shared=LegacyToolboxInventories.share(source,second);
        assertSame(first,shared);
        first.setItemDirect(0,new ItemStack(Items.DIAMOND,3));
        assertEquals(3,shared.getStackInSlot(0).getCount());
        assertSame(second,LegacyToolboxInventories.share(source.copy(),second));
    }
}
