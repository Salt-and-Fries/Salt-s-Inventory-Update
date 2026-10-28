package com.salts_inventory_update.compat.minersdelight;

import com.salts_inventory_update.compat.nativewindows.NativeMachineLayouts;
import com.salts_inventory_update.compat.nativewindows.NativeWindowPolish;

public final class MinersDelightNativeUi {
    private MinersDelightNativeUi() { }
    public static void initialize() {
        NativeWindowPolish.install("minersdelight", "minersdelight:copper_pot", NativeMachineLayouts::copperPot);
    }
}
