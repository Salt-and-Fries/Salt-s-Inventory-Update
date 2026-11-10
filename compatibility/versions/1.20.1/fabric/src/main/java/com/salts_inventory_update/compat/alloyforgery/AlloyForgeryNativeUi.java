package com.salts_inventory_update.compat.alloyforgery;

import com.salts_inventory_update.compat.nativewindows.NativeMachineLayouts;
import com.salts_inventory_update.compat.nativewindows.NativeWindowPolish;
import com.salts_inventory_update.api.client.desktop.*;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class AlloyForgeryNativeUi {
    private AlloyForgeryNativeUi() { }
    public static void initialize() {
        NativeWindowPolish.installTemplate("alloy_forgery", "alloy_forgery:alloy_forge", Template::new);
    }
    private static final class Template implements DesktopNativeTemplate<AbstractContainerMenu, Void> {
        @Override public void prepareLayout(DesktopWindowContext<AbstractContainerMenu, Void> context) {
            AlloyScreens.prepare(context.model());
        }
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu, Void> context) {
            return NativeMachineLayouts.alloyForge(context.nativeView());
        }
    }
}
