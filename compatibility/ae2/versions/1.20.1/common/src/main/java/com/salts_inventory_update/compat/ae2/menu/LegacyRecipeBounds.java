package com.salts_inventory_update.compat.ae2.menu;

import net.minecraft.network.FriendlyByteBuf;

/** The fork's crafting-grid bound, retaining AE2 15's original integer encoding. */
public final class LegacyRecipeBounds {
    private LegacyRecipeBounds() { }
    public static int readSize(FriendlyByteBuf buffer) {
        int count=buffer.readInt();
        if (count<0||count>9) throw new IllegalArgumentException("Invalid crafting grid size: "+count);
        return count;
    }
}
