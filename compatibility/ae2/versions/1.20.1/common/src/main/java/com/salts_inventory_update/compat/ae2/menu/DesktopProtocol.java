package com.salts_inventory_update.compat.ae2.menu;

import net.minecraft.resources.ResourceLocation;

/** Shared channel identifiers for authenticated desktop companion associations. */
public final class DesktopProtocol {
    public static final ResourceLocation TOOLBOX_CHANNEL =
            new ResourceLocation("ae2", "desktop_toolbox_association");
    public static final ResourceLocation CPU_STATE_CHANNEL =
            new ResourceLocation("ae2", "desktop_cpu_status");
    private DesktopProtocol() { }
}
