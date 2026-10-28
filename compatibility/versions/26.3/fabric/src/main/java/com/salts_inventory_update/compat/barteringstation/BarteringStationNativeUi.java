package com.salts_inventory_update.compat.barteringstation;

import com.salts_inventory_update.compat.nativewindows.*;
import com.salts_inventory_update.api.client.desktop.*;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Native head/count artwork, with its highlight-and-close action scoped to this window. */
public final class BarteringStationNativeUi {
    private BarteringStationNativeUi() { }
    public static void initialize() {
        NativeWindowPolish.installTemplate("barteringstation","barteringstation:bartering_station",Template::new);
    }
    private static final class Template implements DesktopNativeTemplate<AbstractContainerMenu,Void> {
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,Void> c) {
            return NativeMachineLayouts.bartering(c.nativeView());
        }
        @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,Void> c,MouseButtonEvent e,boolean twice) {
            if(e.button()!=com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT)return false;
            var view=((DesktopNativeContext<?,?>)c).nativeView();
            var point=NativeMachineLayouts.bartering(view).sourceAt(e.x()-c.contentX(),e.y()-c.contentY());
            if(point.isEmpty())return false;
            for(var widget:view.widgets()) {
                var b=widget.bounds();
                if(widget.visible() && widget.active() && point.get().x()>=b.x() && point.get().x()<b.x()+b.width()
                        && point.get().y()>=b.y() && point.get().y()<b.y()+b.height()) {
                    // Upstream calls the vanilla screen's onClose(). For a detached
                    // window, keep that action local and preserve other desktop sessions.
                    if(c.sendMenuButton(0)) {
                        c.minecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK,1));
                        c.windowHandle().close();
                    }
                    return true;
                }
            }
            return false;
        }
    }
}
