package com.salts_inventory_update.compat.ae2.nativeview;

import net.minecraft.resources.ResourceLocation;

/** Minecraft 1.20's original identifier constructor for the desktop's vanilla art names. */
public final class LegacyIdentifiers {
    private LegacyIdentifiers() { }
    public static ResourceLocation withDefaultNamespace(String path) { return new ResourceLocation("minecraft", path); }
}
