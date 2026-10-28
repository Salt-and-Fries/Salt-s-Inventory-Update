package com.salts_inventory_update.compat.expandeddelight;

import com.salts_inventory_update.compat.nativewindows.NativeWindowPolish;

public final class ExpandedDelightNativeUi {
    private ExpandedDelightNativeUi() { }
    public static void initialize() {
        NativeWindowPolish.install("expandeddelight", "expandeddelight:juicer", NativeWindowPolish::juicer);
    }
}
