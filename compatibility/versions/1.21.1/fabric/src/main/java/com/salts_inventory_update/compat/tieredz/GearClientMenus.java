package com.salts_inventory_update.compat.tieredz;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Own integration lifetime, populated only by this integration's public API callbacks. */
public final class GearClientMenus {
    private static final Set<AbstractContainerMenu> RENAMING=Collections.newSetFromMap(new WeakHashMap<>());
    private GearClientMenus() { }
    public static void opened(AbstractContainerMenu menu){RENAMING.add(menu);}
    public static void closed(AbstractContainerMenu menu){RENAMING.remove(menu);}
    public static boolean ownsRename(AbstractContainerMenu menu){return RENAMING.contains(menu);}
}
