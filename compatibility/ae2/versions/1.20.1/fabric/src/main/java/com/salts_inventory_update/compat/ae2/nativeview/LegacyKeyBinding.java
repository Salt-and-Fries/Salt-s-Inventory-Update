package com.salts_inventory_update.compat.ae2.nativeview;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/** Fabric uses the original key/scancode/mouse matchers; it has no Forge conflict-context API. */
public final class LegacyKeyBinding {
    private LegacyKeyBinding() { }
    public static boolean isActiveAndMatches(KeyMapping mapping, InputConstants.Key key) {
        if (key.getType() == InputConstants.Type.MOUSE) return mapping.matchesMouse(key.getValue());
        return key.getType() == InputConstants.Type.SCANCODE
                ? mapping.matches(InputConstants.UNKNOWN.getValue(), key.getValue())
                : mapping.matches(key.getValue(), 0);
    }
}
