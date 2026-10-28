package com.salts_inventory_update.compat.inmis;

import com.salts_inventory_update.compat.nativewindows.NativeWindowPolish;

public final class InmisNativeUi {
    private InmisNativeUi() { }
    public static void initialize() {
        InmisIntegration.register();
        NativeWindowPolish.install("inmis", "inmis:backpack", NativeWindowPolish::slots);
    }
}
