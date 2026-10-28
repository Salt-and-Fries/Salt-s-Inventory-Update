package com.salts_inventory_update.compat.illagerinvasion;

import com.salts_inventory_update.compat.nativewindows.NativeMachineLayouts;
import com.salts_inventory_update.compat.nativewindows.NativeWindowPolish;

public final class IllagerInvasionNativeUi {
    private IllagerInvasionNativeUi() { }
    public static void initialize() {
        NativeWindowPolish.install("illagerinvasion", "illagerinvasion:imbuing", NativeMachineLayouts::imbuing);
    }
}
