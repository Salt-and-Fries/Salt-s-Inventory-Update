package com.salts_inventory_update.compat.alloyed;
import com.salts_inventory_update.compat.nativewindows.*;
/** Standalone Alloyed 3.x forge: nine inputs, fuel, result and native progress. */
public final class AlloyedNativeUi {
    private AlloyedNativeUi() { }
    public static void initialize() {
        NativeWindowPolish.install("alloyed","alloyed:forge",NativeMachineLayouts::alloyed);
        NativeWindowPolish.install("alloyed","alloyed:oven",NativeMachineLayouts::alloyed);
    }
}
