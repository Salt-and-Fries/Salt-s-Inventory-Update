package com.salts_inventory_update.compat.oxidized;

import com.salts_inventory_update.compat.nativewindows.NativeWindowPolish;

public final class OxidizedNativeUi {
    private OxidizedNativeUi() { }
    public static void initialize() {
        NativeWindowPolish.install("oxidized", "oxidized:copper_kiln_screen_handler", NativeWindowPolish::oxidized);
    }
}
