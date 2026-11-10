package com.salts_inventory_update.compat.nativewindows.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Native screen origin, independent of loader-specific extension methods. */
@Mixin(AbstractContainerScreen.class)
public interface NativeScreenGeometryAccess {
    @Accessor("leftPos") int salts_inventory_compat$left();
    @Accessor("topPos") int salts_inventory_compat$top();
}
