package com.salts_inventory_update.compat.alloyforgery;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Observe the original button's state, including keyboard activation, without owning its UI. */
public final class AlloySlotModes {
    public interface Source { boolean salts$lockingEnabled(); }
    private static final Map<AbstractContainerMenu,WeakReference<Source>> SOURCES=new WeakHashMap<>();
    private AlloySlotModes() { }
    public static void remember(AbstractContainerMenu menu,Source source){SOURCES.put(menu,new WeakReference<>(source));}
    public static boolean locking(AbstractContainerMenu menu){
        var reference=SOURCES.get(menu);var source=reference==null?null:reference.get();
        return source!=null&&source.salts$lockingEnabled();
    }
}
