package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.client.desktop.*;
import java.util.List;
import java.util.function.Function;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Compact native composition using exactly the same API available to other extensions. */
public class CompactNativeTemplate<S> implements DesktopNativeTemplate<AbstractContainerMenu,S> {
    private static final DesktopWindowSurface SURFACE = new DesktopWindowSurface(DesktopSurfaceMode.SALT,
        new DesktopInsets(6,20,6,6),true,List.of(),DesktopViewportPolicy.CLAMP);
    private final Function<DesktopNativeView,DesktopNativeLayout> layout;
    public CompactNativeTemplate(Function<DesktopNativeView,DesktopNativeLayout> layout) { this.layout=layout; }
    @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,S> c) { return SURFACE; }
    @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,S> c) {
        return layout.apply(c.nativeView());
    }
}
