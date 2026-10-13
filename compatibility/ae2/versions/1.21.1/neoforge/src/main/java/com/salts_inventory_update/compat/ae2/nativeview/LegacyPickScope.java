package com.salts_inventory_update.compat.ae2.nativeview;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.world.inventory.Slot;

/** Restores a temporary native pick selection even when its callback fails. */
public final class LegacyPickScope {
    private record Selection(GuiEventListener owner, Slot slot) { }
    private static final ThreadLocal<Selection> CURRENT = new ThreadLocal<>();
    private LegacyPickScope() { }

    public static boolean matches(KeyMapping mapping, int button) {
        return CURRENT.get() != null || mapping.matchesMouse(button);
    }

    public static Slot slot(GuiEventListener owner) {
        var selected = CURRENT.get(); return selected != null && selected.owner() == owner ? selected.slot() : null;
    }

    public static boolean invoke(GuiEventListener owner, Slot slot, double x, double y) {
        if (slot == null || CURRENT.get() != null) return false;
        CURRENT.set(new Selection(owner, slot));
        try { return owner.mouseClicked(x, y, 2); }
        finally { CURRENT.remove(); }
    }

}
