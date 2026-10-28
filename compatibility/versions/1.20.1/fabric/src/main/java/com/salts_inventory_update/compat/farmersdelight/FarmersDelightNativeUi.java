package com.salts_inventory_update.compat.farmersdelight;

import com.salts_inventory_update.compat.nativewindows.NativeMachineLayouts;
import com.salts_inventory_update.compat.nativewindows.NativeWindowPolish;

public final class FarmersDelightNativeUi {
    private FarmersDelightNativeUi() { }
    public static void initialize() {
        NativeWindowPolish.install("farmersdelight", "farmersdelight:cooking_pot", NativeMachineLayouts::cooking);
    }
}
