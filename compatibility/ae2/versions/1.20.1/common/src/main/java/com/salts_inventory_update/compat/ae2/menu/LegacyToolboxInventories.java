package com.salts_inventory_update.compat.ae2.menu;

import appeng.util.inv.AppEngInternalInventory;
import com.google.common.collect.MapMaker;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.world.item.ItemStack;

/** The fork's weak identity sharing, using AE2 15's original mutable inventory. */
public final class LegacyToolboxInventories {
    private static final ConcurrentMap<ItemStack,AppEngInternalInventory> INVENTORIES =
            new MapMaker().weakKeys().weakValues().makeMap();
    private LegacyToolboxInventories() { }
    public static AppEngInternalInventory share(ItemStack source, AppEngInternalInventory created) {
        var previous=INVENTORIES.putIfAbsent(source,created);
        return previous==null?created:previous;
    }
}
