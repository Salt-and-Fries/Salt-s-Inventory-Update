package com.salts_inventory_update.compat.ae2.nativeview;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/** Retains Forge's active conflict-context and modifier checks for hosted keyboard actions. */
public final class LegacyKeyBinding {
    private LegacyKeyBinding() { }
    public static boolean isActiveAndMatches(KeyMapping mapping, InputConstants.Key key) { return mapping.isActiveAndMatches(key); }
}
