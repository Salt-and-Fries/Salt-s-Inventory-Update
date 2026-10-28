package com.salts_inventory_update.compat.betterend;

import com.salts_inventory_update.compat.nativewindows.*;

public final class BetterEndNativeUi {
    private BetterEndNativeUi() { }
    public static void initialize() {
        NativeWindowPolish.installTemplate("betterend","betterend:end_stone_smelter",()->new MockupNativeTemplate<>(NativeBatchLayouts::smelter));
        // Ordinary BetterEnd furnaces retain Core's existing vanilla furnace/recipe-book support.
    }
}
