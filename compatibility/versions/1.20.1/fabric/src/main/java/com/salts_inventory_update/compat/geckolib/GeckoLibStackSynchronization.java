package com.salts_inventory_update.compat.geckolib;

import net.minecraft.nbt.CompoundTag;

/** Keep GeckoLib's optional identity check neutral when neither tag has an ID. */
public final class GeckoLibStackSynchronization {
    private GeckoLibStackSynchronization() {}

    /** Null delegates the comparison of two real IDs to GeckoLib unchanged. */
    public static Boolean compareMissingIdentities(CompoundTag first, CompoundTag second) {
        boolean firstHasId = first != null && first.contains("GeckoLibID");
        boolean secondHasId = second != null && second.contains("GeckoLibID");
        if (firstHasId && secondHasId) return null;
        return firstHasId == secondHasId;
    }
}
