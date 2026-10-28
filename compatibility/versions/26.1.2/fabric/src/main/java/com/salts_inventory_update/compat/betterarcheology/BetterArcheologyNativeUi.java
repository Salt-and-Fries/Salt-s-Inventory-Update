package com.salts_inventory_update.compat.betterarcheology;

import com.salts_inventory_update.compat.nativewindows.NativeMachineLayouts;
import com.salts_inventory_update.compat.nativewindows.NativeWindowPolish;
import com.salts_inventory_update.api.client.desktop.*;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class BetterArcheologyNativeUi {
    private BetterArcheologyNativeUi() { }
    public static void initialize() {
        NativeWindowPolish.installTemplate("betterarcheology", "betterarcheology:archeology_table", IdentifyingTemplate::new);
        NativeWindowPolish.installTemplate("betterarcheology", "betterarcheology:identifying", IdentifyingTemplate::new);
        NativeWindowPolish.install("betterarcheology", "betterarcheology:fossil", NativeMachineLayouts::fossil);
    }

    private static final class IdentifyingTemplate implements DesktopNativeTemplate<AbstractContainerMenu,Void> {
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,Void> context){
            return NativeMachineLayouts.identifying(context.nativeView());
        }
        @Override public DesktopWindowDecoration<AbstractContainerMenu,Void> decoration(DesktopWindowContext<AbstractContainerMenu,Void> context){
            return BetterArcheologyDecoration.INSTANCE;
        }
    }
}
