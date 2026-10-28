package com.salts_inventory_update.compat.alloyforgery;

import com.salts_inventory_update.compat.nativewindows.NativeMachineLayouts;
import com.salts_inventory_update.compat.nativewindows.NativeWindowPolish;

public final class AlloyForgeryNativeUi {
    private AlloyForgeryNativeUi() { }
    public static void initialize() {
        NativeWindowPolish.install("alloy_forgery", "alloy_forgery:alloy_forge", NativeMachineLayouts::alloyForge);
    }
}
