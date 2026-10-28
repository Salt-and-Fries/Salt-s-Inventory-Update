package com.salts_inventory_update.compat.barteringstation;
import com.salts_inventory_update.compat.nativewindows.*;
/** Native item/button rendering is captured by the host on these releases. */
public final class BarteringStationNativeUi {
    private BarteringStationNativeUi() { }
    public static void initialize() {
        NativeWindowPolish.install("barteringstation","barteringstation:bartering_station",NativeMachineLayouts::bartering);
    }
}
