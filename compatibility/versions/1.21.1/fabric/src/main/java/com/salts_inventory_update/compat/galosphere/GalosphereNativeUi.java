package com.salts_inventory_update.compat.galosphere;

import com.salts_inventory_update.compat.nativewindows.NativeMachineLayouts;
import com.salts_inventory_update.compat.nativewindows.NativeWindowPolish;

public final class GalosphereNativeUi {
    private GalosphereNativeUi() { }
    public static void initialize() {
        NativeWindowPolish.install("galosphere", "galosphere:combustion_table", NativeMachineLayouts::combustion);
    }
}
