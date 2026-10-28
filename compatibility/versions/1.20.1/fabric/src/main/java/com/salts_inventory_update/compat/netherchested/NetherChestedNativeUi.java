package com.salts_inventory_update.compat.netherchested;

import com.salts_inventory_update.compat.nativewindows.NativeWindowPolish;

public final class NetherChestedNativeUi {
    private NetherChestedNativeUi() { }
    public static void initialize() {
        NativeWindowPolish.install("netherchested", "netherchested:nether_chest", NativeWindowPolish::slots, false, 0);
    }
}
