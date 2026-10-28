package com.salts_inventory_update.compat.brickmortar;

import com.salts_inventory_update.compat.nativewindows.NativeWindowPolish;

public final class BrickMortarNativeUi {
    private BrickMortarNativeUi() { }
    public static void initialize() {
        NativeWindowPolish.install("brick_and_mortar", "brick_and_mortar:kiln", NativeWindowPolish::kiln, true, 45);
    }
}
