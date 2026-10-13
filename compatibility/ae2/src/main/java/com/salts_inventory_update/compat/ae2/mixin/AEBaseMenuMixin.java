package com.salts_inventory_update.compat.ae2.mixin;

import com.salts_inventory_update.compat.ae2.menu.DesktopMenuState;
import com.salts_inventory_update.compat.ae2.menu.NativeDesktopMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

/** Adds only extension-owned desktop state; native AE2 menu behavior stays native. */
@Pseudo
@Mixin(targets = "appeng.menu.AEBaseMenu", remap = false)
public abstract class AEBaseMenuMixin implements NativeDesktopMenu {
    @Unique private final DesktopMenuState<Object> salts$desktop = new DesktopMenuState<>();

    @Override
    public DesktopMenuState<Object> salts$desktopState() {
        return salts$desktop;
    }
}
