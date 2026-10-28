package com.salts_inventory_update.compat.sawmill;

import com.salts_inventory_update.compat.nativewindows.NativeMachineLayouts;
import com.salts_inventory_update.compat.nativewindows.NativeWindowPolish;

public final class SawmillNativeUi {
    private SawmillNativeUi() { }
    public static void initialize() {
        NativeWindowPolish.install("sawmill", "sawmill:sawmill", NativeMachineLayouts::sawmill);
    }
}
