package com.salts_inventory_update.compat.crabbersdelight;

import com.salts_inventory_update.compat.nativewindows.NativeWindowPolish;

public final class CrabbersDelightNativeUi {
    private CrabbersDelightNativeUi() { }
    public static void initialize() {
        NativeWindowPolish.install("crabbersdelight", "crabbersdelight:crab_trap_menu", NativeWindowPolish::slots);
    }
}
